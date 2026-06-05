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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mono.signal.model.PlaybackState
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MonoColors.Ink)
            .clickable(onClick = onClick),
    ) {
        ProgressLine(progress = state.progress)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlbumArt(
                track = track,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(2.dp)),
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
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .circleGlow(MonoColors.Turquoise)
                    .clip(CircleShape)
                    .background(MonoColors.Turquoise),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MonoColors.Void,
                )
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MonoColors.Graphite),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(MonoColors.Turquoise, MonoColors.Crimson))),
        )
    }
}
