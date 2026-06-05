package com.mono.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mono.audio.ui.theme.Cyan
import com.mono.audio.ui.theme.Magenta
import com.mono.audio.ui.theme.Muted
import com.mono.audio.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun FftGraph(bands: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(168.dp)) {
        val safeBands = bands.ifEmpty { List(36) { 0.06f } }
        val barWidth = size.width / (safeBands.size * 1.7f)
        safeBands.forEachIndexed { index, value ->
            val x = index * size.width / safeBands.size + barWidth / 2f
            val height = (value.coerceIn(0f, 1f) * size.height).coerceAtLeast(5f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Magenta, Violet, Cyan)),
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
            )
        }
        drawLine(Cyan.copy(alpha = 0.45f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
    }
}

@Composable
fun WaveformProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek(offset.x / size.width) }
            },
    ) {
        val bars = 72
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2f)
        repeat(bars) { index ->
            val normalized = index / (bars - 1f)
            val wave = abs(sin(normalized * PI * 5.5)).toFloat()
            val jitter = abs(sin(index * 1.73f)).toFloat()
            val barHeight = (0.18f + wave * 0.55f + jitter * 0.27f) * size.height
            val x = index * (barWidth + gap)
            val color = if (normalized <= progress) Cyan else Muted.copy(alpha = 0.25f)
            drawLine(
                color = color,
                start = Offset(x + barWidth / 2, (size.height - barHeight) / 2),
                end = Offset(x + barWidth / 2, (size.height + barHeight) / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Square,
            )
        }
        drawRect(
            brush = Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.22f), Color.Transparent, Magenta.copy(alpha = 0.18f))),
            size = size,
        )
    }
}
