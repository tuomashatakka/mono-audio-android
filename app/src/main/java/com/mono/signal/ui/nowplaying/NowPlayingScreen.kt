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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onEnableVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    val playback = state.playback
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
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
            MonoIconButton(MonoGlyph.MORE, "More", {}, bordered = true, accent = palette.accent)
        }

        Spacer(Modifier.height(32.dp))

        GraphicArea(
            state = state,
            audioGranted = audioGranted,
            onTap = onCycleGraphic,
            onEnableVisualizer = onEnableVisualizer,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )

        Spacer(Modifier.height(36.dp))

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
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        )
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
            .background(palette.panel)
            .cornerBrackets(palette.accent)
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
                    modifier = Modifier.fillMaxSize(),
                )
                NowPlayingGraphic.WAVE_3D -> Waveform3DGraph(
                    waveform = state.frame.waveform,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
                NowPlayingGraphic.FFT -> FftGraph(
                    bands = state.frame.fftBands,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
            }
        }

        // Only show the mic prompt when the permission is actually missing.
        if (state.graphic != NowPlayingGraphic.ALBUM_ART && !audioGranted) {
            VisualizerHint(onEnableVisualizer, palette.accent, Modifier.align(Alignment.Center))
        }

        MonoLabel(
            text = "— ${state.graphic.label}",
            color = palette.accent,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
    }
}

@Composable
private fun VisualizerHint(onEnable: () -> Unit, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoIcon(MonoGlyph.NAV_SCN, tint = MonoColors.Fg3, size = 28.dp)
        Text(
            "Live visuals need microphone access to read the audio output.",
            style = MonoTypography.bodySmall,
            color = MonoColors.Fg3,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onEnable) { Text("ENABLE", color = accent) }
    }
}

/** Holographic L-marks at each corner, per the mockup's "corner brackets" chrome. */
private fun Modifier.cornerBrackets(color: androidx.compose.ui.graphics.Color): Modifier = drawBehind {
    val len = 18.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val w = size.width
    val h = size.height
    drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(w - len, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(w, len), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, h), Offset(len, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, h), Offset(0f, h - len), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, h), Offset(w - len, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, h), Offset(w, h - len), stroke, StrokeCap.Round)
}
