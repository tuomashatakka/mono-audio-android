package com.mono.signal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.mono.signal.data.waveform.WaveformReducer
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors

private val FrequencyLabels = listOf(20, 65, 110, 220, 440, 880, 1760, 3600, 7200, 20000)
private const val PeakHistoryFrames = 90

/**
 * Log-spaced spectrum as continuous peak/RMS traces. Incoming capture buffers are tweened at
 * frame rate so the graph refreshes above 30Hz even when Android Visualizer callbacks arrive
 * more slowly. A third trace keeps a slowly degrading sliding peak history.
 */
@Composable
fun FftGraph(
    peakBands: FloatArray,
    modifier: Modifier = Modifier,
    rmsBands: FloatArray = peakBands,
) {
    val palette = LocalMonoPalette.current
    val peakAnim = remember { Animatable(1f) }
    val peakHistory = remember { mutableStateListOf<FloatArray>() }
    val previousPeak = remember { mutableStateOf(peakBands.copyOf()) }
    val previousRms = remember { mutableStateOf(rmsBands.copyOf()) }
    LaunchedEffect(peakBands, rmsBands) {
        previousPeak.value = lerpArrays(previousPeak.value, peakBands, peakAnim.value)
        previousRms.value = lerpArrays(previousRms.value, rmsBands, peakAnim.value)
        peakHistory.add(0, peakBands.copyOf())
        while (peakHistory.size > PeakHistoryFrames) peakHistory.removeAt(peakHistory.lastIndex)
        peakAnim.snapTo(0f)
        peakAnim.animateTo(1f, tween(33, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val count = maxOf(peakBands.size, rmsBands.size, 2)
        val peak = WaveformReducer.resample(lerpArrays(previousPeak.value, peakBands, peakAnim.value), count)
        val rms = WaveformReducer.resample(lerpArrays(previousRms.value, rmsBands, peakAnim.value), count)
        val history = FloatArray(count)
        peakHistory.forEachIndexed { idx, frame ->
            val degraded = 1f - idx / PeakHistoryFrames.toFloat()
            WaveformReducer.resample(frame, count).forEachIndexed { i, v ->
                history[i] = maxOf(history[i], v * degraded)
            }
        }

        drawAreaTrace(history, palette.accent.copy(alpha = 0.16f), closeToBottom = true)
        drawTrace(history, palette.accent.copy(alpha = 0.28f), strokeWidth = 1.5f)
        drawAreaTrace(peak, palette.accent.copy(alpha = 0.18f), closeToBottom = true)
        drawTrace(peak, palette.accent, strokeWidth = 3f)
        drawTrace(rms, palette.sweep.getOrElse(1) { palette.accent }.copy(alpha = 0.9f), strokeWidth = 2f)

        listOf(-60, -30, 0).forEach { db ->
            val normalized = ((db + 60) / 60f).coerceIn(0f, 1f)
            val y = size.height - (normalized * size.height * 0.9f) - size.height * 0.04f
            drawLine(
                color = MonoColors.Fg4.copy(alpha = 0.14f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$db dB",
                4f,
                y - 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(130, 234, 242, 245)
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                },
            )
        }

        FrequencyLabels.forEach { hz ->
            val x = frequencyToX(hz.toFloat(), size.width)
            drawLine(
                color = MonoColors.Fg4.copy(alpha = 0.16f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                hz.toString(),
                x + 3f,
                size.height - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 234, 242, 245)
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                },
            )
        }

        drawLine(
            brush = Brush.horizontalGradient(palette.sweep),
            start = Offset(0f, size.height - 1f),
            end = Offset(size.width, size.height - 1f),
            strokeWidth = 1.5f,
            blendMode = BlendMode.Plus,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrace(
    values: FloatArray,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
) {
    if (values.isEmpty()) return
    val path = values.toPath(size.width, size.height)
    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAreaTrace(
    values: FloatArray,
    color: androidx.compose.ui.graphics.Color,
    closeToBottom: Boolean,
) {
    if (values.isEmpty()) return
    val path = values.toPath(size.width, size.height)
    if (closeToBottom) {
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
    }
    drawPath(path, color)
}

private fun FloatArray.toPath(width: Float, height: Float): Path = Path().apply {
    val step = width / (size - 1).coerceAtLeast(1)
    forEachIndexed { i, v ->
        val x = i * step
        val y = height - (v.coerceIn(0f, 1f) * height * 0.9f) - height * 0.04f
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
}

private fun frequencyToX(freq: Float, width: Float): Float {
    val lo = kotlin.math.ln(20f)
    val hi = kotlin.math.ln(20_000f)
    return ((kotlin.math.ln(freq.coerceIn(20f, 20_000f)) - lo) / (hi - lo) * width).coerceIn(0f, width)
}

private fun lerpArrays(from: FloatArray, to: FloatArray, t: Float): FloatArray {
    val count = maxOf(from.size, to.size)
    return FloatArray(count) { i ->
        val a = from.getOrElse(i) { 0f }
        val b = to.getOrElse(i) { 0f }
        a + (b - a) * t.coerceIn(0f, 1f)
    }
}
