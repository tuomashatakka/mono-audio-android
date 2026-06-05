package com.mono.signal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.mono.signal.ui.theme.LocalMonoPalette
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private const val FRAMES = 48

/**
 * Serum-style wavetable terrain: a stack of recent waveform captures receding toward a
 * vanishing point. Newest frame is large/bright at the front; older frames shrink, rise,
 * fade, and shift hue turquoise → violet → crimson, producing a scrolling 3D oscilloscope.
 *
 * Keeps its own short ring buffer of frames; [waveform] drives it (each emit is a new array).
 */
@Composable
fun Waveform3DGraph(
    waveform: FloatArray,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    val near = palette.sweep.firstOrNull() ?: palette.accent
    val mid = palette.sweep.getOrElse(1) { palette.accent }
    val far = palette.sweep.lastOrNull() ?: palette.accent

    val history = remember { mutableStateListOf<FloatArray>() }
    LaunchedEffect(waveform) {
        history.add(0, waveform)
        while (history.size > FRAMES) history.removeAt(history.lastIndex)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) return@Canvas
        val n = history.size
        // Draw back-to-front so the newest frame sits on top.
        for (idx in history.lastIndex downTo 0) {
            val frame = history[idx]
            val depth = idx / (n - 1f).coerceAtLeast(1f) // 0 = front, 1 = back
            drawFrame(frame, depth, near, mid, far)
        }
    }
}

private fun DrawScope.drawFrame(frame: FloatArray, depth: Float, near: Color, mid: Color, far: Color) {
    if (frame.isEmpty()) return

    val frontY = size.height * 0.82f
    val backY = size.height * 0.18f
    val baseY = frontY + (backY - frontY) * depth

    val widthScale = 1f - 0.55f * depth                 // back frames are narrower
    val ampScale = (1f - 0.6f * depth) * size.height * 0.16f
    val alpha = (0.95f - 0.9f * depth).coerceIn(0.04f, 0.95f)

    val drawWidth = size.width * widthScale
    val left = (size.width - drawWidth) / 2f
    val stepX = drawWidth / (frame.size - 1).coerceAtLeast(1)

    val color = when {
        depth < 0.5f -> lerp(near, mid, depth * 2f)
        else -> lerp(mid, far, (depth - 0.5f) * 2f)
    }.copy(alpha = alpha)

    val path = Path().apply {
        frame.forEachIndexed { i, sample ->
            val x = left + i * stepX
            val y = baseY - sample * ampScale
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = (2.5f - 1.6f * depth).coerceAtLeast(0.8f), cap = StrokeCap.Round),
    )
}
