package com.mono.signal.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.signal.ui.components.MiniPlayer
import com.mono.signal.ui.components.MonoBottomNav
import com.mono.signal.ui.components.NavTab
import com.mono.signal.ui.library.LibraryScreen
import com.mono.signal.ui.nowplaying.NowPlayingScreen
import com.mono.signal.ui.settings.SettingsScreen
import com.mono.signal.ui.theme.MonoSpacing
import com.mono.signal.viewmodel.DspViewModel
import com.mono.signal.viewmodel.LibraryViewModel
import com.mono.signal.viewmodel.NowPlayingViewModel
import com.mono.signal.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/** Left-to-right order of the swipeable section tabs, mirrored by the bottom nav. */
private val TABS = listOf(NavTab.LIB, NavTab.DSP, NavTab.QUE, NavTab.CFG)

@Composable
fun AppNav() {
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val dspViewModel: DspViewModel = hiltViewModel()

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val nowPlayingState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
    val themeConfig by settingsViewModel.theme.collectAsStateWithLifecycle()
    val dspConfig by dspViewModel.config.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    // Now Playing is a full-screen overlay (no bottom toolbar), opened from a track tap or the
    // mini-player rather than being one of the swipeable tabs.
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }

    fun goToTab(tab: NavTab) {
        val page = TABS.indexOf(tab)
        if (page >= 0) scope.launch { pagerState.animateScrollToPage(page) }
    }

    // The bottom nav is an overlay, so the pages reserve its measured height (which includes the
    // system navigation-bar inset) to keep the mini-player above it.
    val density = LocalDensity.current
    var navBarHeightPx by remember { mutableStateOf(0) }
    val navBarHeight = with(density) { navBarHeightPx.toDp() }

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

    // Back: close Now Playing first, then step back to the Library tab; otherwise let the system
    // handle it (finishing the activity).
    BackHandler(enabled = nowPlayingOpen || pagerState.currentPage != 0) {
        when {
            nowPlayingOpen -> nowPlayingOpen = false
            else -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Section pages hold no cross-page drag gestures, so full-width swiping moves cleanly
            // between tabs. (Library rows keep their own horizontal swipe-to-queue action.)
            key = { TABS[it] },
        ) { page ->
            when (TABS[page]) {
                NavTab.LIB -> LibraryScreen(
                    state = libraryState,
                    onTrackClick = { track ->
                        libraryViewModel.onTrackClick(track)
                        nowPlayingOpen = true
                    },
                    onRequestPermission = { launcher.launch(REQUIRED_PERMISSIONS) },
                    onSortBy = libraryViewModel::setSortBy,
                    onGroupBy = libraryViewModel::setGroupBy,
                    onSearch = libraryViewModel::setSearch,
                    onAddToQueue = libraryViewModel::addToQueue,
                    onPlayNext = libraryViewModel::playNext,
                    onOpenSettings = { goToTab(NavTab.CFG) },
                    contentPadding = PaddingValues(top = MonoSpacing.xs, bottom = navBarHeight + MonoSpacing.playerClearance),
                )
                NavTab.DSP -> AudioProcessingScreen(
                    config = dspConfig,
                    onEnabled = dspViewModel::setEnabled,
                    onEqBand = dspViewModel::setEqBand,
                    onPreset = dspViewModel::setEqBands,
                    onFlat = dspViewModel::resetEq,
                    onCompressor = dspViewModel::setCompressor,
                    onLimiter = dspViewModel::setLimiter,
                    onBack = { goToTab(NavTab.LIB) },
                    bottomInset = navBarHeight,
                )
                NavTab.QUE -> QueueScreen(
                    playback = nowPlayingState.playback,
                    onBack = { goToTab(NavTab.LIB) },
                    onRemove = nowPlayingViewModel::removeFromQueue,
                    onOpenTrack = { nowPlayingOpen = true },
                    bottomInset = navBarHeight,
                )
                NavTab.CFG -> SettingsScreen(
                    theme = themeConfig,
                    onBack = { goToTab(NavTab.LIB) },
                    onAccent = settingsViewModel::setAccent,
                    onBackground = settingsViewModel::setBackground,
                    onFftBlockSize = settingsViewModel::setFftBlockSize,
                    bottomInset = navBarHeight,
                )
            }
        }

        if (nowPlayingState.playback.currentTrack != null && !nowPlayingOpen) {
            MiniPlayer(
                state = nowPlayingState.playback,
                onClick = { nowPlayingOpen = true },
                onPlayPause = nowPlayingViewModel::togglePlayPause,
                onPrevious = nowPlayingViewModel::previous,
                onNext = nowPlayingViewModel::next,
                compact = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBarHeight),
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).onSizeChanged { navBarHeightPx = it.height },
        ) {
            MonoBottomNav(
                selected = TABS[pagerState.currentPage],
                onSelect = ::goToTab,
            )
        }

        // Drawn last so it sits above the mini-player and the bottom nav, hiding both.
        AnimatedVisibility(
            visible = nowPlayingOpen,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutVertically(tween(280)) { it } + fadeOut(tween(240)),
        ) {
            NowPlayingScreen(
                state = nowPlayingState,
                audioGranted = audioGranted,
                onBack = { nowPlayingOpen = false },
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
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS,
)
