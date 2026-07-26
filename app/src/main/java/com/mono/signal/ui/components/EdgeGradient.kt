package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoSpacing

/** The thin neon edge line used as a section divider / header underline in the mockup. */
@Composable
fun EdgeGradientLine(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    val sweep = LocalMonoPalette.current.sweep
    val edge = Brush.horizontalGradient(
        0.0f to Color.Transparent,
        0.22f to sweep.first(),
        0.78f to sweep.last(),
        1.0f to Color.Transparent,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(edge),
    )
}

/** A mono "— LABEL" eyebrow with a trailing neon edge line, as seen above each section. */
@Composable
fun SectionEyebrow(
    label: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        MonoLabel(text = "— $label")
        androidx.compose.foundation.layout.Spacer(Modifier.height(MonoSpacing.xxs))
        EdgeGradientLine()
    }
}
