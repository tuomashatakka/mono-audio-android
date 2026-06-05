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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.TransportControls
import com.mono.signal.ui.components.Waveform3DGraph
import com.mono.signal.ui.components.WaveformSeekBar
import com.mono.signal.ui.components.formatDuration
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography

@Composable
fun NowPlayingScreen(
    state: com.mono.signal.viewmodel.NowPlayingUiState,
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
    val playback = state.playback
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MonoColors.Fg2)
            }
            Spacer(Modifier.weight(1f))
            MonoLabel("— NOW PLAYING")
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp)) // balances the back button
        }

        Spacer(Modifier.height(28.dp))

        // Tap-to-cycle graphic area
        GraphicArea(
            state = state,
            onTap = onCycleGraphic,
            onEnableVisualizer = onEnableVisualizer,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )

        Spacer(Modifier.height(36.dp))

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

        Spacer(Modifier.height(28.dp))

        WaveformSeekBar(
            envelope = state.waveformEnvelope,
            progress = playback.progress,
            onSeek = onSeek,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MonoTypography.bodySmall, color = MonoColors.Turquoise)
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
    state: com.mono.signal.viewmodel.NowPlayingUiState,
    onTap: () -> Unit,
    onEnableVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MonoColors.Ink)
            .cornerBrackets()
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
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
                NowPlayingGraphic.FFT -> FftGraph(
                    bands = state.frame.fftBands,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
        }

        // Hint when the live graphs can't draw because audio capture isn't available.
        if (state.graphic != NowPlayingGraphic.ALBUM_ART && !state.visualizerActive) {
            VisualizerHint(onEnableVisualizer, Modifier.align(Alignment.Center))
        }

        MonoLabel(
            text = "— ${state.graphic.label}",
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        )
    }
}

@Composable
private fun VisualizerHint(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Live visuals need microphone access to read the audio output.",
            style = MonoTypography.bodySmall,
            color = MonoColors.Fg3,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onEnable) {
            Text("ENABLE", color = MonoColors.Turquoise)
        }
    }
}

/** Holographic L-marks at each corner, per the mockup's "corner brackets" chrome. */
private fun Modifier.cornerBrackets(): Modifier = drawBehind {
    val len = 18.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val color = MonoColors.Turquoise
    val w = size.width
    val h = size.height
    // top-left
    drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke, StrokeCap.Round)
    // top-right
    drawLine(color, Offset(w, 0f), Offset(w - len, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(w, len), stroke, StrokeCap.Round)
    // bottom-left
    drawLine(color, Offset(0f, h), Offset(len, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, h), Offset(0f, h - len), stroke, StrokeCap.Round)
    // bottom-right
    drawLine(color, Offset(w, h), Offset(w - len, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, h), Offset(w, h - len), stroke, StrokeCap.Round)
}
