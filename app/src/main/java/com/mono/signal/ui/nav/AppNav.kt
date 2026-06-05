package com.mono.signal.ui.nav

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mono.signal.ui.components.MiniPlayer
import com.mono.signal.ui.library.LibraryScreen
import com.mono.signal.ui.nowplaying.NowPlayingScreen
import com.mono.signal.viewmodel.LibraryViewModel
import com.mono.signal.viewmodel.NowPlayingViewModel

private object Routes {
    const val LIBRARY = "library"
    const val NOW_PLAYING = "now_playing"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val nowPlayingState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()

    // --- Permissions ---
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        libraryViewModel.onPermissionResult(result[Manifest.permission.READ_MEDIA_AUDIO] == true)
    }
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            libraryViewModel.onPermissionResult(true)
        } else {
            launcher.launch(REQUIRED_PERMISSIONS)
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.LIBRARY) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    state = libraryState,
                    onTrackClick = { index ->
                        libraryViewModel.onTrackClick(index)
                        navController.navigate(Routes.NOW_PLAYING)
                    },
                    onRequestPermission = { launcher.launch(REQUIRED_PERMISSIONS) },
                )
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    state = nowPlayingState,
                    onBack = { navController.popBackStack() },
                    onCycleGraphic = nowPlayingViewModel::cycleGraphic,
                    onPlayPause = nowPlayingViewModel::togglePlayPause,
                    onNext = nowPlayingViewModel::next,
                    onPrevious = nowPlayingViewModel::previous,
                    onSeek = nowPlayingViewModel::seek,
                    onShuffle = nowPlayingViewModel::toggleShuffle,
                    onRepeat = nowPlayingViewModel::cycleRepeat,
                )
            }
        }

        // Mini-player overlay — only on the library screen, when something is loaded.
        if (currentRoute == Routes.LIBRARY && libraryState.activeTrackId != null) {
            MiniPlayer(
                state = nowPlayingState.playback,
                onClick = { navController.navigate(Routes.NOW_PLAYING) },
                onPlayPause = nowPlayingViewModel::togglePlayPause,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS,
)
