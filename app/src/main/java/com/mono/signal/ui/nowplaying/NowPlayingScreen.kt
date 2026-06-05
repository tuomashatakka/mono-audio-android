package com.mono.signal.ui.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mono.signal.model.NowPlayingGraphic
import com.mono.signal.ui.components.AlbumArt
import com.mono.signal.ui.components.FftGraph
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.TransportControls
import com.mono.signal.ui.components.Waveform3DGraph
import com.mono.signal.ui.components.WaveformSeekBar
import com.mono.signal.ui.components.formatDuration
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.icons.MonoIcon
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.viewmodel.NowPlayingUiState

@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    audioGranted: Boolean,
    onBack: () -> Unit,
    onCycleGraphic: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onScrub: (Float, Float) -> Unit,
    onScrubEnd: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onEnableVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    val playback = state.playback
    var showTags by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        AlbumArt(
            track = playback.currentTrack,
            modifier = Modifier
                .fillMaxSize()
                .blur(54.dp)
                .alpha(0.14f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 68.dp, start = 28.dp, end = 28.dp),
        ) {
        // Header: caret back · NOW PLAYING · more
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack, bordered = true, accent = palette.accent)
            Spacer(Modifier.weight(1f))
            MonoLabel("— NOW PLAYING", color = palette.accent)
            Spacer(Modifier.weight(1f))
            MonoIconButton(MonoGlyph.MORE, "More", { showTags = true }, bordered = true, accent = palette.accent)
        }

        Spacer(Modifier.height(32.dp))

        GraphicArea(
            state = state,
            audioGranted = audioGranted,
            onTap = onCycleGraphic,
            onEnableVisualizer = onEnableVisualizer,
            modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f).align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(24.dp))

        // Title + favourite
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playback.currentTrack?.title ?: "Nothing playing",
                    style = MonoTypography.displaySmall,
                    color = MonoColors.Fg1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playback.currentTrack?.artist ?: "—",
                    style = MonoTypography.bodyMedium,
                    color = MonoColors.Fg3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            var favourite by remember { mutableStateOf(false) }
            MonoIconButton(
                if (favourite) MonoGlyph.HEART_FILLED else MonoGlyph.HEART,
                "Favourite", { favourite = !favourite },
                bordered = true, active = favourite, accent = palette.accent,
            )
        }

        Spacer(Modifier.height(28.dp))

        WaveformSeekBar(
            envelope = state.waveformEnvelope,
            progress = playback.progress,
            onSeek = onSeek,
            onScrub = onScrub,
            onScrubEnd = onScrubEnd,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MonoTypography.bodySmall, color = palette.accent)
            val remaining = (playback.durationMs - playback.positionMs).coerceAtLeast(0L)
            Text("-${formatDuration(remaining)}", style = MonoTypography.bodySmall, color = MonoColors.Fg3)
        }

        Spacer(Modifier.weight(1f))

            TransportControls(
            isPlaying = playback.isPlaying,
            shuffle = playback.shuffle,
            repeatMode = playback.repeatMode,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onShuffle = onShuffle,
            onRepeat = onRepeat,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        )
        }
        if (showTags) {
            FileTagsDialog(
                title = playback.currentTrack?.title.orEmpty(),
                artist = playback.currentTrack?.artist.orEmpty(),
                album = playback.currentTrack?.album.orEmpty(),
                path = playback.currentTrack?.filePath.orEmpty(),
                onDismiss = { showTags = false },
            )
        }
    }
}

@Composable
private fun GraphicArea(
    state: NowPlayingUiState,
    audioGranted: Boolean,
    onTap: () -> Unit,
    onEnableVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Box(
        modifier = modifier
            .background(palette.panel.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    ) {
        Crossfade(targetState = state.graphic, animationSpec = tween(320), label = "graphic") { graphic ->
            when (graphic) {
                NowPlayingGraphic.ALBUM_ART -> AlbumArt(
                    track = state.playback.currentTrack,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
                NowPlayingGraphic.WAVE_3D -> Waveform3DGraph(
                    waveformLeft = state.frame.waveformLeft,
                    waveformRight = state.frame.waveformRight,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
                NowPlayingGraphic.FFT -> FftGraph(
                    peakBands = state.frame.fftPeakBands,
                    rmsBands = state.frame.fftRmsBands,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
            }
        }

        // Live visuals are read straight from the player's PCM output (no mic permission needed),
        // so the only time there's nothing to show is when no track is loaded.
        if (state.graphic != NowPlayingGraphic.ALBUM_ART && state.playback.currentTrack == null) {
            VisualizerHint(Modifier.align(Alignment.Center))
        }

        MonoLabel(
            text = "— ${state.graphic.label}",
            color = palette.accent,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
    }
}

@Composable
private fun VisualizerHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoIcon(MonoGlyph.NAV_SCN, tint = MonoColors.Fg3, size = 28.dp)
        Text(
            "Play a track to see the live waveform and spectrum.",
            style = MonoTypography.bodySmall,
            color = MonoColors.Fg3,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FileTagsDialog(
    title: String,
    artist: String,
    album: String,
    path: String,
    onDismiss: () -> Unit,
) {
    var editTitle by remember(title) { mutableStateOf(title) }
    var editArtist by remember(artist) { mutableStateOf(artist) }
    var editAlbum by remember(album) { mutableStateOf(album) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("SAVE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        title = { Text("File meta tags", color = MonoColors.Fg1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
                OutlinedTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artist") })
                OutlinedTextField(value = editAlbum, onValueChange = { editAlbum = it }, label = { Text("Album") })
                Text(path.ifBlank { "No file path available" }, style = MonoTypography.bodySmall, color = MonoColors.Fg3)
            }
        },
    )
}
