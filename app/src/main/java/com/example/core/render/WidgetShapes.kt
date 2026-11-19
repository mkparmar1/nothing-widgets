package com.example.core.render

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.WidgetShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

object WidgetShapes {

    const val CUT_FRACTION = 0.16f
    private const val BEVEL_FRACTION = 0.28f

    fun path(
        shape: WidgetShape,
        width: Float,
        height: Float,
        cornerRadius: Float,
        inset: Float = 0f,
        customPoints: List<ShapePoint> = emptyList()
    ): Path {
        val left = inset
        val top = inset
        val right = (width - inset).coerceAtLeast(left + 1f)
        val bottom = (height - inset).coerceAtLeast(top + 1f)
        val rect = RectF(left, top, right, bottom)
        val w = rect.width()
        val h = rect.height()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = min(w, h) / 2f

        return Path().apply {
            when (shape) {
                WidgetShape.SQUARE -> addRect(rect, Path.Direction.CW)
                WidgetShape.ROUNDED -> addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
                WidgetShape.PILL -> addRoundRect(rect, r, r, Path.Direction.CW)
                WidgetShape.CIRCLE -> {
                    addOval(RectF(cx - r, cy - r, cx + r, cy + r), Path.Direction.CW)
                }
                WidgetShape.ELLIPSE -> addOval(rect, Path.Direction.CW)
                WidgetShape.SQUIRCLE -> superellipse(rect)
                WidgetShape.CUT -> chamfer(rect, angleFraction(cornerRadius, w, h, 0.02f, 0.30f))
                WidgetShape.BEVEL -> chamfer(rect, angleFraction(cornerRadius, w, h, 0.05f, 0.42f))
                WidgetShape.DIAMOND -> polygon(rect, 4, -90f)
                WidgetShape.SOFT_DIAMOND -> softDiamond(rect)
                WidgetShape.TRIANGLE -> polygon(rect, 3, -90f)
                WidgetShape.TRIANGLE_DOWN -> polygon(rect, 3, 90f)
                WidgetShape.PENTAGON -> polygon(rect, 5, -90f)
                WidgetShape.HEXAGON -> polygon(rect, 6, 0f)
                WidgetShape.OCTAGON -> polygon(rect, 8, 22.5f)
                WidgetShape.DECAGON -> polygon(rect, 10, -90f)
                WidgetShape.STAR_5 -> star(rect, 5, 0.46f, -90f)
                WidgetShape.STAR_6 -> star(rect, 6, 0.5f, -90f)
                WidgetShape.STAR_8 -> star(rect, 8, 0.58f, -90f)
                WidgetShape.BURST -> star(rect, 16, 0.82f, -90f)
                WidgetShape.GEAR -> star(rect, 12, 0.74f, 0f)
                WidgetShape.HEART -> heart(rect)
                WidgetShape.SHIELD -> shield(rect)
                WidgetShape.ARROW -> arrow(rect)
                WidgetShape.CHEVRON -> chevron(rect)
                WidgetShape.CROSS -> cross(rect, false)
                WidgetShape.PLUS -> cross(rect, true)
                WidgetShape.FLOWER_4 -> flower(rect, 4)
                WidgetShape.FLOWER_6 -> flower(rect, 6)
                WidgetShape.FLOWER_8 -> flower(rect, 8)
                WidgetShape.BLOB -> radial(rect, 128) { t ->
                    0.86f + 0.07f * sin(3 * t).toFloat() + 0.05f * cos(5 * t).toFloat()
                }
                WidgetShape.SPEECH -> speech(rect)
                WidgetShape.TICKET -> ticket(rect)
                WidgetShape.LEAF -> leaf(rect)
                WidgetShape.DROP -> drop(rect)
                WidgetShape.CRESCENT -> crescent(rect)
                WidgetShape.LENS -> lens(rect)
                WidgetShape.RIBBON -> ribbon(rect)
                WidgetShape.CUSTOM -> custom(rect, customPoints, cornerRadius)
            }
        }
    }

    fun radiusFor(shape: WidgetShape, width: Float, height: Float, cornerRadius: Float): Float =
        when (shape) {
            WidgetShape.SQUARE, WidgetShape.DIAMOND, WidgetShape.SOFT_DIAMOND,
            WidgetShape.TRIANGLE, WidgetShape.TRIANGLE_DOWN, WidgetShape.PENTAGON,
            WidgetShape.HEXAGON, WidgetShape.OCTAGON, WidgetShape.DECAGON,
            WidgetShape.STAR_5, WidgetShape.STAR_6, WidgetShape.STAR_8, WidgetShape.BURST,
            WidgetShape.GEAR, WidgetShape.HEART, WidgetShape.SHIELD, WidgetShape.ARROW,
            WidgetShape.CHEVRON, WidgetShape.CROSS, WidgetShape.PLUS, WidgetShape.FLOWER_4,
            WidgetShape.FLOWER_6, WidgetShape.FLOWER_8, WidgetShape.BLOB, WidgetShape.SPEECH,
            WidgetShape.TICKET, WidgetShape.LEAF,
            WidgetShape.DROP, WidgetShape.CRESCENT, WidgetShape.LENS, WidgetShape.RIBBON,
            WidgetShape.CUSTOM -> 0f
            WidgetShape.ROUNDED -> cornerRadius
            WidgetShape.PILL, WidgetShape.CIRCLE -> min(width, height) / 2f
            WidgetShape.ELLIPSE -> min(width, height) / 2f
            WidgetShape.SQUIRCLE -> min(width, height) * 0.42f
            WidgetShape.CUT -> min(width, height) * angleFraction(cornerRadius, width, height, 0.02f, 0.30f)
            WidgetShape.BEVEL -> min(width, height) * angleFraction(cornerRadius, width, height, 0.05f, 0.42f)
        }

    private fun angleFraction(radius: Float, width: Float, height: Float, min: Float, max: Float): Float =
        (radius / minOf(width, height).coerceAtLeast(1f)).coerceIn(min, max)

    private fun Path.chamfer(rect: RectF, fraction: Float) {
        val cut = min(rect.width(), rect.height()) * fraction
        moveTo(rect.left + cut, rect.top)
        lineTo(rect.right - cut, rect.top)
        lineTo(rect.right, rect.top + cut)
        lineTo(rect.right, rect.bottom - cut)
        lineTo(rect.right - cut, rect.bottom)
        lineTo(rect.left + cut, rect.bottom)
        lineTo(rect.left, rect.bottom - cut)
        lineTo(rect.left, rect.top + cut)
        close()
    }

    private fun Path.polygon(rect: RectF, sides: Int, rotationDeg: Float, fraction: Float = 1f) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val a = rect.width() / 2f * fraction
        val b = rect.height() / 2f * fraction
        for (i in 0 until sides) {
            val angle = Math.toRadians(rotationDeg.toDouble() + 360.0 * i / sides)
            val x = cx + a * cos(angle).toFloat()
            val y = cy + b * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun Path.star(rect: RectF, points: Int, inner: Float, rotationDeg: Float) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val a = rect.width() / 2f
        val b = rect.height() / 2f
        for (i in 0 until points * 2) {
            val angle = Math.toRadians(rotationDeg.toDouble() + 180.0 * i / points)
            val f = if (i % 2 == 0) 1f else inner
            val x = cx + a * f * cos(angle).toFloat()
            val y = cy + b * f * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun Path.radial(rect: RectF, steps: Int, radius: (Double) -> Float) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val a = rect.width() / 2f
        val b = rect.height() / 2f
        for (i in 0..steps) {
            val t = 2.0 * PI * i / steps
            val f = radius(t)
            val x = cx + a * f * cos(t).toFloat()
            val y = cy + b * f * sin(t).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun Path.superellipse(rect: RectF, steps: Int = 96) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val a = rect.width() / 2f
        val b = rect.height() / 2f
        for (i in 0..steps) {
            val t = 2.0 * PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = cx + a * sign(c) * abs(c).pow(0.5)
            val y = cy + b * sign(s) * abs(s).pow(0.5)
            if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
        }
        close()
    }

    private fun Path.heart(rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val sx = rect.width() / 34f
        val sy = rect.height() / 34f
        for (i in 0..96) {
            val t = 2.0 * PI * i / 96
            val s = sin(t)
            val x = cx + (16 * s * s * s).toFloat() * sx
            val y = cy - (13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)).toFloat() * sy
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun Path.flower(rect: RectF, petals: Int) {
        radial(rect, 128) { t -> 0.72f + 0.28f * cos(petals * t).toFloat() }
    }

    private fun Path.softDiamond(rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val a = rect.width() / 2f
        val b = rect.height() / 2f
        val k = 0.18f
        moveTo(cx, cy - b)
        lineTo(cx + a * (1 - k), cy - b * (1 - k))
        quadTo(cx + a, cy - b, cx + a, cy - b * (1 - k))
        lineTo(cx + a, cy + b * (1 - k))
        quadTo(cx + a, cy + b, cx + a * (1 - k), cy + b)
        lineTo(cx, cy + b)
        quadTo(cx - a * (1 - k), cy + b, cx - a, cy + b * (1 - k))
        lineTo(cx - a, cy - b * (1 - k))
        quadTo(cx - a, cy - b, cx - a * (1 - k), cy - b)
        close()
    }

    private fun Path.shield(rect: RectF) {
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.top + rect.height() * 0.42f)
        quadTo(rect.right, rect.bottom, rect.centerX(), rect.bottom)
        quadTo(rect.left, rect.bottom, rect.left, rect.top + rect.height() * 0.42f)
        close()
    }

    private fun Path.arrow(rect: RectF) {
        val h = rect.height()
        moveTo(rect.left, rect.top + h * 0.3f)
        lineTo(rect.left + rect.width() * 0.55f, rect.top + h * 0.3f)
        lineTo(rect.left + rect.width() * 0.55f, rect.top)
        lineTo(rect.right, rect.centerY())
        lineTo(rect.left + rect.width() * 0.55f, rect.bottom)
        lineTo(rect.left + rect.width() * 0.55f, rect.top + h * 0.7f)
        lineTo(rect.left, rect.top + h * 0.7f)
        close()
    }

    private fun Path.chevron(rect: RectF) {
        moveTo(rect.left, rect.top)
        lineTo(rect.left + rect.width() * 0.55f, rect.top)
        lineTo(rect.right, rect.centerY())
        lineTo(rect.left + rect.width() * 0.55f, rect.bottom)
        lineTo(rect.left, rect.bottom)
        lineTo(rect.left + rect.width() * 0.45f, rect.centerY())
        close()
    }

    private fun Path.cross(rect: RectF, plus: Boolean) {
        val w = rect.width()
        val h = rect.height()
        val t = if (plus) h * 0.28f else h * 0.32f
        val s = if (plus) w * 0.28f else w * 0.32f
        moveTo(rect.left + (w - s) / 2f, rect.top)
        lineTo(rect.left + (w + s) / 2f, rect.top)
        lineTo(rect.left + (w + s) / 2f, rect.top + t)
        lineTo(rect.right, rect.top + t)
        lineTo(rect.right, rect.bottom - t)
        lineTo(rect.left + (w + s) / 2f, rect.bottom - t)
        lineTo(rect.left + (w + s) / 2f, rect.bottom)
        lineTo(rect.left + (w - s) / 2f, rect.bottom)
        lineTo(rect.left + (w - s) / 2f, rect.bottom - t)
        lineTo(rect.left, rect.bottom - t)
        lineTo(rect.left, rect.top + t)
        lineTo(rect.left + (w - s) / 2f, rect.top + t)
        close()
    }

    private fun Path.speech(rect: RectF) {
        val tail = rect.height() * 0.22f
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.bottom - tail)
        lineTo(rect.left + rect.width() * 0.34f, rect.bottom - tail)
        lineTo(rect.left + rect.width() * 0.2f, rect.bottom)
        lineTo(rect.left + rect.width() * 0.06f, rect.bottom - tail)
        lineTo(rect.left, rect.bottom - tail)
        close()
    }

    private fun Path.ticket(rect: RectF) {
        val r = min(rect.width(), rect.height()) * 0.13f
        val cy = rect.centerY()
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, cy - r)
        cubicTo(rect.right - r * 1.6f, cy - r, rect.right - r * 1.6f, cy + r, rect.right, cy + r)
        lineTo(rect.right, rect.bottom)
        lineTo(rect.left, rect.bottom)
        lineTo(rect.left, cy + r)
        cubicTo(rect.left + r * 1.6f, cy + r, rect.left + r * 1.6f, cy - r, rect.left, cy - r)
        close()
    }

    private fun Path.leaf(rect: RectF) {
        moveTo(rect.left, rect.bottom)
        quadTo(rect.left, rect.top, rect.right, rect.top)
        quadTo(rect.right, rect.bottom, rect.left, rect.bottom)
        close()
    }

    private fun Path.drop(rect: RectF) {
        val cx = rect.centerX()
        moveTo(cx, rect.top)
        cubicTo(
            cx + rect.width() * 0.14f, rect.top + rect.height() * 0.42f,
            rect.right, rect.centerY() + rect.height() * 0.18f, cx, rect.bottom
        )
        cubicTo(
            rect.left, rect.centerY() + rect.height() * 0.18f,
            cx - rect.width() * 0.14f, rect.top + rect.height() * 0.42f, cx, rect.top
        )
        close()
    }

    private fun Path.crescent(rect: RectF) {
        val cx = rect.centerX()
        val r = min(rect.width(), rect.height()) / 2f
        moveTo(cx, rect.centerY() - r)
        cubicTo(cx - r * 1.6f, rect.centerY() - r, cx - r * 1.6f, rect.centerY() + r, cx, rect.centerY() + r)
        cubicTo(cx - r * 0.35f, rect.centerY() + r * 0.7f, cx - r * 0.35f, rect.centerY() - r * 0.7f, cx, rect.centerY() - r)
        close()
    }

    private fun Path.lens(rect: RectF) {
        val cy = rect.centerY()
        moveTo(rect.centerX(), rect.top)
        quadTo(rect.right, cy, rect.centerX(), rect.bottom)
        quadTo(rect.left, cy, rect.centerX(), rect.top)
        close()
    }

    private fun Path.ribbon(rect: RectF) {
        val notch = rect.width() * 0.18f
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right - notch, rect.centerY())
        lineTo(rect.right, rect.bottom)
        lineTo(rect.left, rect.bottom)
        lineTo(rect.left + notch, rect.centerY())
        close()
    }

    private fun Path.custom(rect: RectF, points: List<ShapePoint>, radius: Float) {
        val maxRadius = min(rect.width(), rect.height()) / 2f
        val corner = radius.coerceIn(0f, maxRadius)
        if (points.size < 3) {
            addRoundRect(rect, corner, corner, Path.Direction.CW)
            return
        }
        roundedPolygon(
            points.map {
                PointF(
                    rect.left + it.x.coerceIn(0f, 1f) * rect.width(),
                    rect.top + it.y.coerceIn(0f, 1f) * rect.height()
                )
            },
            corner
        )
    }

    private fun Path.roundedPolygon(points: List<PointF>, radius: Float) {
        val size = points.size
        val starts = ArrayList<PointF>(size)
        val ends = ArrayList<PointF>(size)
        for (i in 0 until size) {
            val point = points[i]
            val previous = points[(i - 1 + size) % size]
            val next = points[(i + 1) % size]
            val incoming = hypot(previous.x - point.x, previous.y - point.y)
            val outgoing = hypot(next.x - point.x, next.y - point.y)
            val inCut = min(radius, incoming / 2f)
            val outCut = min(radius, outgoing / 2f)
            starts += PointF(
                point.x + (previous.x - point.x) / incoming.coerceAtLeast(0.001f) * inCut,
                point.y + (previous.y - point.y) / incoming.coerceAtLeast(0.001f) * inCut
            )
            ends += PointF(
                point.x + (next.x - point.x) / outgoing.coerceAtLeast(0.001f) * outCut,
                point.y + (next.y - point.y) / outgoing.coerceAtLeast(0.001f) * outCut
            )
        }
        moveTo(starts[0].x, starts[0].y)
        for (i in 0 until size) {
            val nextStart = starts[(i + 1) % size]
            lineTo(starts[i].x, starts[i].y)
            quadTo(points[i].x, points[i].y, ends[i].x, ends[i].y)
            lineTo(nextStart.x, nextStart.y)
        }
        close()
    }
}
