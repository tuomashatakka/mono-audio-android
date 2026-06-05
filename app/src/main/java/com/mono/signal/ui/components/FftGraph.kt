package com.mono.signal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTokens

/**
 * Log-spaced frequency-spectrum bars with a vertical neon gradient and a soft glow pass.
 * Purely a function of the incoming [bands] (already normalized + smoothed upstream).
 */
@Composable
fun FftGraph(
    bands: FloatArray,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val count = bands.size.coerceAtLeast(1)
        val gap = size.width * 0.012f
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
        val baseline = size.height

        for (i in 0 until count) {
            val v = if (bands.isNotEmpty()) bands[i] else 0f
            val barHeight = (v * size.height * 0.92f).coerceAtLeast(2f)
            val x = i * (barWidth + gap) + barWidth / 2f

            // Glow underlay.
            drawLine(
                color = MonoColors.Turquoise.copy(alpha = 0.18f),
                start = Offset(x, baseline),
                end = Offset(x, baseline - barHeight),
                strokeWidth = barWidth * 2.2f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus,
            )
            // Crisp bar.
            drawLine(
                brush = MonoTokens.gradSpectrum,
                start = Offset(x, baseline),
                end = Offset(x, baseline - barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }

        // Faint baseline edge.
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(MonoColors.Turquoise.copy(alpha = 0.6f), MonoColors.Crimson.copy(alpha = 0.6f)),
            ),
            start = Offset(0f, baseline - 1f),
            end = Offset(size.width, baseline - 1f),
            strokeWidth = 1.5f,
        )
    }
}
