package com.mono.signal.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mono.signal.ui.components.MiniPlayer
import com.mono.signal.ui.components.MonoBottomNav
import com.mono.signal.ui.components.NavTab
import com.mono.signal.ui.library.LibraryScreen
import com.mono.signal.ui.nowplaying.NowPlayingScreen
import com.mono.signal.ui.settings.SettingsScreen
import com.mono.signal.viewmodel.LibraryViewModel
import com.mono.signal.viewmodel.NowPlayingViewModel
import com.mono.signal.viewmodel.SettingsViewModel

private object Routes {
    const val LIBRARY = "library"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
    const val AUDIO = "audio_processing"
    const val QUEUE = "queue"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val nowPlayingState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
    val themeConfig by settingsViewModel.theme.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        libraryViewModel.onPermissionResult(result[Manifest.permission.READ_MEDIA_AUDIO] == true)
        if (result[Manifest.permission.RECORD_AUDIO] == true) {
            audioGranted = true
            nowPlayingViewModel.retryVisualizer()
        }
    }
    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (readGranted) libraryViewModel.onPermissionResult(true)
        if (!readGranted || !audioGranted) launcher.launch(REQUIRED_PERMISSIONS)
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    BackHandler {
        if (currentRoute != Routes.LIBRARY) {
            navController.navigate(Routes.LIBRARY) { popUpTo(Routes.LIBRARY) { inclusive = false } }
        } else {
            (context as? Activity)?.finish()
        }
    }

    fun switchTab(route: String) {
        if (route == currentRoute) return
        navController.navigate(route) {
            popUpTo(Routes.LIBRARY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            enterTransition = { slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = {
                slideOutHorizontally(tween(260)) { it / 6 } + fadeOut(tween(220))
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    state = libraryState,
                    onTrackClick = { track ->
                        libraryViewModel.onTrackClick(track)
                        navController.navigate(Routes.NOW_PLAYING)
                    },
                    onRequestPermission = { launcher.launch(REQUIRED_PERMISSIONS) },
                    onSortBy = libraryViewModel::setSortBy,
                    onGroupBy = libraryViewModel::setGroupBy,
                    onSearch = libraryViewModel::setSearch,
                    onAddToQueue = libraryViewModel::addToQueue,
                    onPlayNext = libraryViewModel::playNext,
                    onOpenSettings = { switchTab(Routes.SETTINGS) },
                    contentPadding = PaddingValues(top = 8.dp, bottom = 170.dp),
                )
            }
            composable(
                Routes.NOW_PLAYING,
                enterTransition = { slideInHorizontally(tween(320)) { it } + fadeIn(tween(320)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(260)) },
            ) {
                NowPlayingScreen(
                    state = nowPlayingState,
                    audioGranted = audioGranted,
                    onBack = { switchTab(Routes.LIBRARY) },
                    onCycleGraphic = nowPlayingViewModel::cycleGraphic,
                    onPlayPause = nowPlayingViewModel::togglePlayPause,
                    onNext = nowPlayingViewModel::next,
                    onPrevious = nowPlayingViewModel::previous,
                    onSeek = nowPlayingViewModel::seek,
                    onScrub = nowPlayingViewModel::scrub,
                    onScrubEnd = nowPlayingViewModel::endScrub,
                    onShuffle = nowPlayingViewModel::toggleShuffle,
                    onRepeat = nowPlayingViewModel::cycleRepeat,
                    onEnableVisualizer = { launcher.launch(REQUIRED_PERMISSIONS) },
                )
            }
            composable(Routes.AUDIO) {
                AudioProcessingScreen(onBack = { switchTab(Routes.LIBRARY) })
            }
            composable(Routes.QUEUE) {
                QueueScreen(
                    playback = nowPlayingState.playback,
                    onBack = { switchTab(Routes.LIBRARY) },
                    onRemove = nowPlayingViewModel::removeFromQueue,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    theme = themeConfig,
                    onBack = { switchTab(Routes.LIBRARY) },
                    onAccent = settingsViewModel::setAccent,
                    onBackground = settingsViewModel::setBackground,
                    onFftBlockSize = settingsViewModel::setFftBlockSize,
                )
            }
        }

        Column(Modifier.align(Alignment.BottomCenter)) {
            MonoBottomNav(
                selected = when (currentRoute) {
                    Routes.SETTINGS -> NavTab.CFG
                    Routes.AUDIO -> NavTab.DSP
                    Routes.QUEUE -> NavTab.QUE
                    Routes.NOW_PLAYING -> NavTab.NOW
                    else -> NavTab.LIB
                },
                onSelect = { tab ->
                    when (tab) {
                        NavTab.LIB -> switchTab(Routes.LIBRARY)
                        NavTab.DSP -> switchTab(Routes.AUDIO)
                        NavTab.NOW -> navController.navigate(Routes.NOW_PLAYING)
                        NavTab.QUE -> switchTab(Routes.QUEUE)
                        NavTab.CFG -> switchTab(Routes.SETTINGS)
                    }
                },
            )
        }
        if (nowPlayingState.playback.currentTrack != null && currentRoute != Routes.NOW_PLAYING) {
            MiniPlayer(
                state = nowPlayingState.playback,
                onClick = { navController.navigate(Routes.NOW_PLAYING) },
                onPlayPause = nowPlayingViewModel::togglePlayPause,
                onPrevious = nowPlayingViewModel::previous,
                onNext = nowPlayingViewModel::next,
                compact = currentRoute != Routes.NOW_PLAYING,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
            )
        }
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS,
)
