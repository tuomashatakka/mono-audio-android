package com.mono.signal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import kotlin.math.abs

@Composable
fun StereoWidthGraph(waveform: FloatArray, modifier: Modifier = Modifier) {
    val palette = LocalMonoPalette.current
    Canvas(modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = size.minDimension * 0.44f
        drawCircle(MonoColors.Fg4.copy(alpha = 0.14f), maxRadius, Offset(centerX, centerY), style = Stroke(1.dp.toPx()))
        drawLine(MonoColors.Fg4.copy(alpha = 0.16f), Offset(centerX, 0f), Offset(centerX, size.height), 1f)
        drawLine(MonoColors.Fg4.copy(alpha = 0.16f), Offset(0f, centerY), Offset(size.width, centerY), 1f)
        val width = stereoWidth(waveform)
        val sideRadius = maxRadius * (0.18f + width * 0.82f)
        drawCircle(Brush.radialGradient(palette.sweep, Offset(centerX, centerY), sideRadius), sideRadius, Offset(centerX, centerY), alpha = 0.38f, blendMode = BlendMode.Plus)
        drawCircle(palette.accent, sideRadius, Offset(centerX, centerY), style = Stroke(3.dp.toPx()))
        val label = "${(width * 100).toInt()}% WIDTH"
        drawContext.canvas.nativeCanvas.drawText(
            label,
            centerX - 52.dp.toPx(),
            centerY + 5.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(220, 234, 242, 245)
                textSize = 14.dp.toPx()
                isAntiAlias = true
            },
        )
        drawContext.canvas.nativeCanvas.drawText(
            "MONO        WIDE",
            10.dp.toPx(),
            size.height - 8.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(130, 234, 242, 245)
                textSize = 10.dp.toPx(); isAntiAlias = true
            },
        )
    }
}

private fun stereoWidth(waveform: FloatArray): Float {
    if (waveform.size < 4) return 0f
    var diff = 0f
    var sum = 0f
    var count = 0
    var i = 0
    while (i + 1 < waveform.size) {
        val left = waveform[i]
        val right = waveform[i + 1]
        diff += abs(left - right)
        sum += abs(left) + abs(right)
        count++
        i += 2
    }
    return if (sum <= 0f || count == 0) 0f else (diff / sum).coerceIn(0f, 1f)
}
