package com.mono.signal.ui.components

import androidx.compose.foundation.Canvas
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
import com.mono.signal.data.waveform.WaveformReducer
import com.mono.signal.ui.theme.MonoColors

/**
 * Mirror-style amplitude waveform with a neon gradient fill up to [progress]. Tapping or
 * dragging reports a 0..1 [onSeek] fraction. Stateless — envelope + progress come in,
 * seek intents go out.
 *
 * Bars are sized to the available width (a fixed dp pitch), and the stored envelope is
 * resampled to exactly that many bars, so the track never overflows the canvas regardless
 * of how many samples the decoder produced.
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
            .height(64.dp)
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
        val barWidth = 3.dp.toPx()
        val gap = 2.dp.toPx()
        val pitch = barWidth + gap
        val barCount = (size.width / pitch).toInt().coerceIn(1, 512)

        val bars = WaveformReducer.resample(envelope, barCount)
        val midY = size.height / 2f
        val maxBarHeight = size.height - 4.dp.toPx()
        val minBarHeight = 3.dp.toPx()
        val playedX = shown * size.width

        for (i in 0 until barCount) {
            val amp = if (bars.isNotEmpty()) bars[i.coerceIn(0, bars.size - 1)] else 0f
            val barHeight = (amp * maxBarHeight).coerceAtLeast(minBarHeight)
            val x = i * pitch + barWidth / 2f
            drawLine(
                brush = if (x <= playedX) fillBrush else dimBrush,
                start = Offset(x, midY - barHeight / 2f),
                end = Offset(x, midY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val fillBrush = Brush.horizontalGradient(
    listOf(MonoColors.Turquoise, MonoColors.Violet, MonoColors.Crimson),
)

private val dimBrush = Brush.verticalGradient(
    listOf(MonoColors.Fog, MonoColors.Slate),
)
