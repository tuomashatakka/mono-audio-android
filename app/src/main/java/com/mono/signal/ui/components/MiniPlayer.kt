package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mono.signal.model.PlaybackState
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography

/** Persistent bottom bar: thumb · title/artist · play-pause, with a thin neon progress line. */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    val palette = LocalMonoPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.panel)
            .clickable(onClick = onClick),
    ) {
        ProgressLine(progress = state.progress, palette.sweep)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlbumArt(
                track = track,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(2.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MonoTypography.bodyMedium,
                    color = MonoColors.Fg1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MonoTypography.bodySmall,
                    color = MonoColors.Fg3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MonoPlayButton(
                isPlaying = state.isPlaying,
                onClick = onPlayPause,
                accent = palette.accent,
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, sweep: List<androidx.compose.ui.graphics.Color>) {
    Box(
        Modifier.fillMaxWidth().height(2.dp).background(MonoColors.Graphite),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(Brush.horizontalGradient(sweep)),
        )
    }
}
