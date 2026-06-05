package com.mono.signal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors

/**
 * Flat, square-corner amplitude waveform. Tapping seeks; dragging emits continuous scrub
 * updates plus signed drag rate so the player can stretch playback instead of clicking.
 */
@Composable
fun WaveformSeekBar(
    envelope: FloatArray,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onScrub: (fraction: Float, rate: Float) -> Unit = { _, _ -> },
    onScrubEnd: () -> Unit = {},
) {
    val palette = LocalMonoPalette.current
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    var lastDragFraction by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragFraction >= 0f) dragFraction else progress

    val grow = remember(envelope) { Animatable(0f) }
    val loaded = remember(envelope) { envelope.any { it > 0f } }
    LaunchedEffect(envelope) {
        if (loaded) grow.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
    }

    val fill = remember(palette) { Brush.horizontalGradient(palette.sweep) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragFraction = (it.x / size.width).coerceIn(0f, 1f)
                        lastDragFraction = dragFraction
                        onScrub(dragFraction, 0f)
                    },
                    onHorizontalDrag = { change, _ ->
                        val next = (change.position.x / size.width).coerceIn(0f, 1f)
                        val rate = ((next - lastDragFraction) * 180f).coerceIn(-3f, 3f)
                        dragFraction = next
                        lastDragFraction = next
                        onScrub(next, rate)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) onSeek(dragFraction)
                        dragFraction = -1f
                        lastDragFraction = -1f
                        onScrubEnd()
                    },
                    onDragCancel = {
                        dragFraction = -1f
                        lastDragFraction = -1f
                        onScrubEnd()
                    },
                )
            },
    ) {
        val barWidth = 3.dp.toPx()
        val gap = 2.dp.toPx()
        val pitch = barWidth + gap
        val barCount = (size.width / pitch).toInt().coerceIn(1, 512)
        val bars = WaveformReducer.resample(envelope, barCount)

        val midY = size.height / 2f
        val maxBarHeight = size.height - 2.dp.toPx()
        val minBarHeight = 2.dp.toPx()
        val playedX = shown * size.width
        val g = grow.value

        for (i in 0 until barCount) {
            val amp = if (bars.isNotEmpty()) bars[i.coerceIn(0, bars.size - 1)] else 0f
            val barHeight = (amp * maxBarHeight * g).coerceAtLeast(minBarHeight)
            val x = i * pitch + barWidth / 2f
            drawLine(
                brush = if (x <= playedX) fill else dimBrush,
                start = Offset(x, midY - barHeight / 2f),
                end = Offset(x, midY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Butt,
            )
        }
    }
}

private val dimBrush = Brush.horizontalGradient(listOf(MonoColors.Graphite, MonoColors.Graphite))
