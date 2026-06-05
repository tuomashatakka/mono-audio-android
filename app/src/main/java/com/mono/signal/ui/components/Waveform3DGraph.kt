package com.mono.signal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.mono.signal.ui.theme.LocalMonoPalette

private const val FRAMES = 56

/**
 * Serum-style wavetable terrain, split per channel. Each captured frame is pushed into a depth
 * stack and every row is drawn twice — left channel in one colour, right in another — so the
 * phase difference between channels reads directly off the divergence of the two coloured lines.
 *
 * Wave history is refreshed at ~60Hz with short tweens; a faint mesh under the left channel keeps
 * the 3D terrain feel without burying the per-channel traces.
 */
@Composable
fun Waveform3DGraph(
    waveformLeft: FloatArray,
    waveformRight: FloatArray,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    // Left/right take the two ends of the signature sweep so they stay distinct on every accent.
    val leftColor = palette.sweep.firstOrNull() ?: palette.accent
    val rightColor = palette.sweep.lastOrNull() ?: palette.accent
    val meshColor = lerp(leftColor, rightColor, 0.5f)

    val history = remember { mutableStateListOf<Pair<FloatArray, FloatArray>>() }
    val frameTween = remember { Animatable(1f) }
    LaunchedEffect(waveformLeft, waveformRight) {
        history.add(0, waveformLeft.copyOf() to waveformRight.copyOf())
        while (history.size > FRAMES) history.removeAt(history.lastIndex)
        frameTween.snapTo(0f)
        frameTween.animateTo(1f, tween(16, easing = LinearEasing))
    }

    val tween = frameTween.value
    Canvas(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) return@Canvas
        // Terrain mesh follows the left channel so the surface stays coherent behind both traces.
        for (idx in history.lastIndex - 1 downTo 0) {
            drawMeshBand(
                history[idx + 1].first,
                history[idx].first,
                (idx + 1) / history.size.toFloat(),
                idx / history.size.toFloat(),
                meshColor,
            )
        }
        for (idx in history.lastIndex downTo 0) {
            val depth = idx / (history.size - 1f).coerceAtLeast(1f)
            // Draw the rear channel first so the front (most recent) rows stay legible on top.
            drawFrame(history[idx].second, depth, rightColor, tween)
            drawFrame(history[idx].first, depth, leftColor, tween)
        }
    }
}

private fun DrawScope.drawMeshBand(back: FloatArray, front: FloatArray, backDepth: Float, frontDepth: Float, color: Color) {
    if (back.isEmpty() || front.isEmpty()) return
    val samples = minOf(back.size, front.size).coerceAtLeast(2)
    val frontPoints = pointsFor(front, frontDepth, samples)
    val backPoints = pointsFor(back, backDepth, samples)
    val bandColor = color.copy(alpha = 0.05f)
    for (i in 0 until samples - 1) {
        val path = Path().apply {
            moveTo(frontPoints[i].x, frontPoints[i].y)
            lineTo(frontPoints[i + 1].x, frontPoints[i + 1].y)
            lineTo(backPoints[i + 1].x, backPoints[i + 1].y)
            lineTo(backPoints[i].x, backPoints[i].y)
            close()
        }
        drawPath(path, bandColor)
    }
}

private fun DrawScope.drawFrame(frame: FloatArray, depth: Float, color: Color, tween: Float) {
    if (frame.isEmpty()) return
    val points = pointsFor(frame, depth, frame.size)
    val traceColor = color.copy(alpha = ((0.92f - 0.82f * depth) * (0.7f + tween * 0.3f)).coerceIn(0.08f, 0.92f))
    val path = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }
    drawPath(path, traceColor, style = Stroke(width = (2.2f - 1.4f * depth).coerceAtLeast(0.8f), cap = StrokeCap.Round))
}

private data class MeshPoint(val x: Float, val y: Float)

private fun DrawScope.pointsFor(frame: FloatArray, depth: Float, samples: Int): List<MeshPoint> {
    val frontY = size.height * 0.82f
    val backY = size.height * 0.16f
    val baseY = frontY + (backY - frontY) * depth
    val widthScale = 1f - 0.55f * depth
    val ampScale = (1f - 0.6f * depth) * size.height * 0.16f
    val drawWidth = size.width * widthScale
    val left = (size.width - drawWidth) / 2f
    val stepX = drawWidth / (samples - 1).coerceAtLeast(1)
    return List(samples) { i ->
        val src = (i * (frame.size - 1f) / (samples - 1).coerceAtLeast(1)).toInt().coerceIn(0, frame.lastIndex)
        MeshPoint(left + i * stepX, baseY - frame[src] * ampScale)
    }
}
