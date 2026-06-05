package com.mono.signal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoLabelStyle

/** Soft accent bloom behind a composable — the "accent glow" toggle from the mockup. */
fun Modifier.neonGlow(color: Color = MonoColors.Turquoise, radius: Dp = 24.dp): Modifier =
    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = size.minDimension / 2f + radius.toPx(),
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }

/** Drop-shadow style glow for circular controls (e.g. the play button). */
fun Modifier.circleGlow(color: Color = MonoColors.Turquoise): Modifier =
    this.shadow(elevation = 24.dp, shape = CircleShape, ambientColor = color, spotColor = color)

/** The tracked, uppercase, monospace label used for every section eyebrow. */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MonoColors.Turquoise,
) {
    Box(modifier) {
        Text(
            text = text.uppercase(),
            style = MonoLabelStyle,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
