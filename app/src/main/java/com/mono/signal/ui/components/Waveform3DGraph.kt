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
 * Serum-style wavetable terrain. Wave history is refreshed with short tweens and adjacent
 * wave rows are connected by translucent mesh planes instead of disconnected guide lines.
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
    val frameTween = remember { Animatable(1f) }
    LaunchedEffect(waveform) {
        history.add(0, waveform.copyOf())
        while (history.size > FRAMES) history.removeAt(history.lastIndex)
        frameTween.snapTo(0f)
        frameTween.animateTo(1f, tween(33, easing = LinearEasing))
    }

    val tween = frameTween.value
    Canvas(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) return@Canvas
        for (idx in history.lastIndex - 1 downTo 0) {
            drawMeshBand(history[idx + 1], history[idx], (idx + 1) / history.size.toFloat(), idx / history.size.toFloat(), near, mid, far)
        }
        for (idx in history.lastIndex downTo 0) {
            drawFrame(history[idx], idx / (history.size - 1f).coerceAtLeast(1f), near, mid, far, tween)
        }
    }
}

private fun DrawScope.drawMeshBand(back: FloatArray, front: FloatArray, backDepth: Float, frontDepth: Float, near: Color, mid: Color, far: Color) {
    if (back.isEmpty() || front.isEmpty()) return
    val samples = minOf(back.size, front.size).coerceAtLeast(2)
    val frontPoints = pointsFor(front, frontDepth, samples)
    val backPoints = pointsFor(back, backDepth, samples)
    val color = colorForDepth((frontDepth + backDepth) / 2f, near, mid, far).copy(alpha = 0.055f)
    for (i in 0 until samples - 1) {
        val path = Path().apply {
            moveTo(frontPoints[i].x, frontPoints[i].y)
            lineTo(frontPoints[i + 1].x, frontPoints[i + 1].y)
            lineTo(backPoints[i + 1].x, backPoints[i + 1].y)
            lineTo(backPoints[i].x, backPoints[i].y)
            close()
        }
        drawPath(path, color)
    }
}

private fun DrawScope.drawFrame(frame: FloatArray, depth: Float, near: Color, mid: Color, far: Color, tween: Float) {
    if (frame.isEmpty()) return
    val points = pointsFor(frame, depth, frame.size)
    val color = colorForDepth(depth, near, mid, far).copy(alpha = ((0.92f - 0.82f * depth) * (0.7f + tween * 0.3f)).coerceIn(0.08f, 0.92f))
    val path = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }
    drawPath(path, color, style = Stroke(width = (2.4f - 1.45f * depth).coerceAtLeast(0.8f), cap = StrokeCap.Round))
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

private fun colorForDepth(depth: Float, near: Color, mid: Color, far: Color): Color = when {
    depth < 0.5f -> lerp(near, mid, depth * 2f)
    else -> lerp(mid, far, (depth - 0.5f) * 2f)
}
