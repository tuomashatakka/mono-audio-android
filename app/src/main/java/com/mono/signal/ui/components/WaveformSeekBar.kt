package com.mono.signal.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.theme.MonoColors
import androidx.compose.foundation.Canvas
import kotlin.math.roundToInt

/**
 * Mirror-style amplitude waveform with a neon gradient fill up to [progress]. Tapping or
 * dragging reports a 0..1 [onSeek] fraction. Stateless — envelope + progress come in,
 * seek intents go out.
 */
@Composable
fun WaveformSeekBar(
    envelope: FloatArray,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragFraction >= 0f) dragFraction else progress

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragFraction = (it.x / size.width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) onSeek(dragFraction)
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                )
            },
    ) {
        val bars = if (envelope.isNotEmpty()) envelope.size else 1
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(1f)
        val midY = size.height / 2f
        val playedBars = (shown * bars).roundToInt()
        val fill = Brush.horizontalGradient(
            listOf(MonoColors.Turquoise, MonoColors.Violet, MonoColors.Crimson),
        )

        for (i in 0 until bars) {
            val amp = if (envelope.isNotEmpty()) envelope[i] else 0.05f
            val barHeight = (amp * size.height).coerceAtLeast(2f)
            val x = i * (barWidth + gap) + barWidth / 2f
            val played = i <= playedBars
            drawLine(
                brush = if (played) fill else dimBrush,
                start = Offset(x, midY - barHeight / 2f),
                end = Offset(x, midY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val dimBrush = Brush.verticalGradient(
    listOf(MonoColors.Fog, MonoColors.Slate),
)
