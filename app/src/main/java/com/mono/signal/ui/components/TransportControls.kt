package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mono.signal.model.RepeatMode
import com.mono.signal.ui.theme.MonoColors

/** Transport row from the mockup: shuffle · prev · [neon play] · next · repeat. */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GhostButton(Icons.Filled.Shuffle, "Shuffle", onShuffle, active = shuffle)
        GhostButton(Icons.Filled.SkipPrevious, "Previous", onPrevious)
        PlayButton(isPlaying = isPlaying, onClick = onPlayPause)
        GhostButton(Icons.Filled.SkipNext, "Next", onNext)
        GhostButton(
            icon = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = "Repeat",
            onClick = onRepeat,
            active = repeatMode != RepeatMode.OFF,
        )
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .circleGlow(MonoColors.Turquoise)
            .clip(CircleShape)
            .background(MonoColors.Turquoise),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = MonoColors.Void,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun GhostButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val tint = if (active) MonoColors.Turquoise else MonoColors.Fg2
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, if (active) MonoColors.Turquoise else MonoColors.BorderSoft, CircleShape)
            .background(Color.Transparent),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}
