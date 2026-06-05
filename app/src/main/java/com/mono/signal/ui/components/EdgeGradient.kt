package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.theme.MonoTokens

/** The thin neon edge line used as a section divider / header underline in the mockup. */
@Composable
fun EdgeGradientLine(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(MonoTokens.gradEdge),
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
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        EdgeGradientLine()
    }
}
