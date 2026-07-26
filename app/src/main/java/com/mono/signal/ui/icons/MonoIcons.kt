package com.mono.signal.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bespoke thin-line glyphs, drawn on a normalized 24×24 grid. Matches the hairline,
 * monospace-adjacent iconography of the MONO Signal reference rather than Material's filled set.
 */
enum class MonoGlyph {
    CARET_LEFT, CARET_RIGHT, CARET_UP, CARET_DOWN,
    PLAY, PAUSE, PREV, NEXT, SHUFFLE, REPEAT, REPEAT_ONE,
    SETTINGS, SORT, GROUP, HEART, HEART_FILLED, SEARCH, MORE,
    NAV_LIB, NAV_SCN, NAV_NOW, NAV_QUE, NAV_CFG,
}

@Composable
fun MonoIcon(
    glyph: MonoGlyph,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 1.5.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val sw = strokeWidth.toPx()
        draw(glyph, tint, s, sw)
    }
}

private fun DrawScope.draw(glyph: MonoGlyph, c: Color, s: Float, sw: Float) {
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(c, p(x1, y1), p(x2, y2), sw, StrokeCap.Round)
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (glyph) {
        MonoGlyph.CARET_LEFT -> { line(15f, 5f, 9f, 12f); line(9f, 12f, 15f, 19f) }
        MonoGlyph.CARET_RIGHT -> { line(9f, 5f, 15f, 12f); line(15f, 12f, 9f, 19f) }
        MonoGlyph.CARET_UP -> { line(5f, 15f, 12f, 9f); line(12f, 9f, 19f, 15f) }
        MonoGlyph.CARET_DOWN -> { line(5f, 9f, 12f, 15f); line(12f, 15f, 19f, 9f) }

        MonoGlyph.PLAY -> {
            val path = Path().apply {
                moveTo(8f * s, 6f * s); lineTo(18f * s, 12f * s); lineTo(8f * s, 18f * s); close()
            }
            drawPath(path, c)
        }
        MonoGlyph.PAUSE -> {
            drawRoundRectFill(c, 8f, 6f, 2.6f, 12f, s)
            drawRoundRectFill(c, 13.4f, 6f, 2.6f, 12f, s)
        }
        MonoGlyph.PREV -> {
            line(8f, 6f, 8f, 18f)
            val path = Path().apply {
                moveTo(18f * s, 6f * s); lineTo(9.5f * s, 12f * s); lineTo(18f * s, 18f * s); close()
            }
            drawPath(path, c)
        }
        MonoGlyph.NEXT -> {
            line(16f, 6f, 16f, 18f)
            val path = Path().apply {
                moveTo(6f * s, 6f * s); lineTo(14.5f * s, 12f * s); lineTo(6f * s, 18f * s); close()
            }
            drawPath(path, c)
        }
        MonoGlyph.SHUFFLE -> {
            line(3f, 7f, 8f, 7f); line(8f, 7f, 20f, 17f)
            line(16f, 17f, 20f, 17f); line(20f, 17f, 20f, 13f) // arrowhead
            line(3f, 17f, 8f, 17f); line(8f, 17f, 12f, 13.5f)
            line(15.5f, 9.5f, 20f, 7f)
            line(16f, 7f, 20f, 7f); line(20f, 7f, 20f, 11f) // arrowhead
        }
        MonoGlyph.REPEAT -> drawRepeat(c, s, sw)
        MonoGlyph.REPEAT_ONE -> {
            drawRepeat(c, s, sw)
            line(11.4f, 10.5f, 12.6f, 9.8f); line(12.6f, 9.8f, 12.6f, 14f)
        }
        MonoGlyph.SETTINGS -> { // sliders
            line(4f, 8f, 20f, 8f); line(4f, 16f, 20f, 16f)
            drawCircleStroke(c, 9f, 8f, 2.2f, s, sw)
            drawCircleStroke(c, 15f, 16f, 2.2f, s, sw)
        }
        MonoGlyph.SORT -> {
            line(5f, 7f, 16f, 7f); line(5f, 12f, 13f, 12f); line(5f, 17f, 10f, 17f)
            line(18f, 7f, 18f, 18f); line(15.5f, 15.5f, 18f, 18f); line(18f, 18f, 20.5f, 15.5f)
        }
        MonoGlyph.GROUP -> {
            drawRectStroke(c, 4f, 5f, 16f, 4f, s, sw)
            drawRectStroke(c, 4f, 14f, 16f, 4f, s, sw)
        }
        MonoGlyph.HEART, MonoGlyph.HEART_FILLED -> {
            val path = heartPath(s)
            if (glyph == MonoGlyph.HEART_FILLED) drawPath(path, c)
            else drawPath(path, c, style = stroke)
        }
        MonoGlyph.SEARCH -> {
            drawCircleStroke(c, 10.5f, 10.5f, 5f, s, sw)
            line(14.5f, 14.5f, 20f, 20f)
        }
        MonoGlyph.MORE -> {
            drawDot(c, 6f, 12f, s); drawDot(c, 12f, 12f, s); drawDot(c, 18f, 12f, s)
        }
        MonoGlyph.NAV_LIB -> { // diamond
            val path = Path().apply {
                moveTo(12f * s, 4f * s); lineTo(20f * s, 12f * s)
                lineTo(12f * s, 20f * s); lineTo(4f * s, 12f * s); close()
            }
            drawPath(path, c, style = stroke)
        }
        MonoGlyph.NAV_SCN -> { drawCircleStroke(c, 10.5f, 10.5f, 5f, s, sw); line(14.5f, 14.5f, 20f, 20f) }
        MonoGlyph.NAV_NOW -> {
            val path = Path().apply {
                moveTo(9f * s, 6f * s); lineTo(18f * s, 12f * s); lineTo(9f * s, 18f * s); close()
            }
            drawPath(path, c, style = stroke)
        }
        MonoGlyph.NAV_QUE -> { line(5f, 8f, 19f, 8f); line(5f, 12f, 19f, 12f); line(5f, 16f, 14f, 16f) }
        MonoGlyph.NAV_CFG -> { // sparkle / 4-point star
            line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f)
            line(12f, 4f, 14f, 10f); line(12f, 20f, 10f, 14f)
        }
    }
}

private fun DrawScope.drawRepeat(c: Color, s: Float, sw: Float) {
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val path = Path().apply {
        // top edge with arrow on the right
        moveTo(7f * s, 8f * s); lineTo(15f * s, 8f * s)
        // right corner down
        moveTo(7f * s, 16f * s); lineTo(17f * s, 16f * s)
    }
    drawPath(path, c, style = stroke)
    // simple rounded loop sides
    drawLine(c, Offset(7f * s, 8f * s), Offset(5f * s, 10f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(5f * s, 10f * s), Offset(5f * s, 14f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(5f * s, 14f * s), Offset(7f * s, 16f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(17f * s, 16f * s), Offset(19f * s, 14f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(19f * s, 14f * s), Offset(19f * s, 10f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(19f * s, 10f * s), Offset(17f * s, 8f * s), sw, StrokeCap.Round)
    // arrowhead at top-right
    drawLine(c, Offset(15f * s, 8f * s), Offset(13.5f * s, 6.5f * s), sw, StrokeCap.Round)
    drawLine(c, Offset(15f * s, 8f * s), Offset(13.5f * s, 9.5f * s), sw, StrokeCap.Round)
}

private fun DrawScope.drawCircleStroke(c: Color, cx: Float, cy: Float, r: Float, s: Float, sw: Float) =
    drawCircle(c, r * s, Offset(cx * s, cy * s), style = Stroke(width = sw))

private fun DrawScope.drawRectStroke(c: Color, x: Float, y: Float, w: Float, h: Float, s: Float, sw: Float) =
    drawRect(c, Offset(x * s, y * s), Size(w * s, h * s), style = Stroke(width = sw))

private fun DrawScope.drawRoundRectFill(c: Color, x: Float, y: Float, w: Float, h: Float, s: Float) =
    drawRect(c, Offset(x * s, y * s), Size(w * s, h * s))

private fun DrawScope.drawDot(c: Color, cx: Float, cy: Float, s: Float) =
    drawCircle(c, 1.4f * s, Offset(cx * s, cy * s))

private fun heartPath(s: Float): Path = Path().apply {
    moveTo(12f * s, 18f * s)
    cubicTo(4f * s, 12.5f * s, 6f * s, 5f * s, 12f * s, 8.5f * s)
    cubicTo(18f * s, 5f * s, 20f * s, 12.5f * s, 12f * s, 18f * s)
    close()
}
