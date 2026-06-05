package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.icons.MonoIcon
import com.mono.signal.ui.theme.MonoColors

/** A circular, hairline-bordered icon button using the bespoke thin-line glyphs. */
@Composable
fun MonoIconButton(
    glyph: MonoGlyph,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color = MonoColors.Fg2,
    bordered: Boolean = false,
    active: Boolean = false,
    accent: Color = MonoColors.Turquoise,
) {
    val resolvedTint = if (active) accent else tint
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (bordered) Modifier.border(
                    1.dp,
                    if (active) accent else MonoColors.BorderSoft,
                    CircleShape,
                ) else Modifier,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MonoIcon(glyph = glyph, tint = resolvedTint, size = iconSize)
    }
}

/** The large accent play/pause button with a soft bloom. */
@Composable
fun MonoPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .circleGlow(accent)
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MonoIcon(
            glyph = if (isPlaying) MonoGlyph.PAUSE else MonoGlyph.PLAY,
            tint = MonoColors.Void,
            size = size * 0.42f,
        )
    }
}
