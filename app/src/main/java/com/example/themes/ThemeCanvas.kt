package com.example.themes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.config.UniversalOptions
import com.example.widgets.base.WidgetSpec

/** Draws a pack's tile images with live text and icons on top. */
class ThemeCanvas(private val context: Context, val spec: WidgetSpec, val canvas: Canvas, val pack: ThemePack) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textScale = UniversalOptions.textScale(spec.options)

    val width = spec.widthPx.toFloat()
    val height = spec.heightPx.toFloat()

    /** Draws an image fitted (not stretched) and centred in [area]; returns where it landed. */
    fun tile(name: String, area: RectF = RectF(0f, 0f, width, height)): RectF {
        val image = ThemeRepository.image(context, pack, name) ?: return area
        val scale = minOf(area.width() / image.width, area.height() / image.height)
        val w = image.width * scale
        val h = image.height * scale
        val dest = RectF(area.centerX() - w / 2, area.centerY() - h / 2, area.centerX() + w / 2, area.centerY() + h / 2)
        canvas.drawBitmap(image, null, dest, paint)
        return dest
    }

    /** The optional small accent dot in a tile's top-right corner. */
    fun cornerDot(tile: RectF, inset: Float = 0.14f) {
        if (!UniversalOptions.cornerDot(spec.options)) return
        val short = minOf(tile.width(), tile.height())
        paint.color = pack.accent
        canvas.drawCircle(tile.right - short * inset, tile.top + short * inset, short * 0.022f, paint)
    }

    fun button(on: Boolean, cx: Float, cy: Float, size: Float) {
        val r = size / 2
        tile(if (on) "button_on" else "button_off", RectF(cx - r, cy - r, cx + r, cy + r))
    }

    fun icon(vector: ImageVector, cx: Float, cy: Float, size: Float, color: Int) {
        val scale = size / vector.viewportWidth
        paint.color = color
        canvas.save()
        canvas.translate(cx - size / 2, cy - size / 2)
        canvas.scale(scale, scale)
        ThemeIcons.paths(vector).forEach { canvas.drawPath(it, paint) }
        canvas.restore()
    }

    fun text(
        value: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        align: Paint.Align = Paint.Align.LEFT,
        bold: Boolean = false,
        mono: Boolean = false,
        maxWidth: Float = Float.MAX_VALUE
    ) {
        paint.color = color
        paint.textAlign = align
        paint.typeface = ThemeFonts.get(context, mono, bold)
        paint.textSize = size * textScale
        var shown = value
        if (paint.measureText(shown) > maxWidth) {
            while (shown.length > 1 && paint.measureText("$shown…") > maxWidth) shown = shown.dropLast(1)
            shown = shown.trimEnd() + "…"
        }
        canvas.drawText(shown, x, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /** A progress arc in the pack's accent, starting at the top. */
    fun arc(cx: Float, cy: Float, radius: Float, stroke: Float, fraction: Float) {
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = pack.muted and 0x33FFFFFF
        canvas.drawArc(oval, 0f, 360f, false, paint)
        paint.color = pack.accent
        canvas.drawArc(oval, -90f, 360f * fraction.coerceIn(0f, 1f), false, paint)
        paint.style = Paint.Style.FILL
    }
}

private object ThemeFonts {
    private val cache = HashMap<String, Typeface?>()

    fun get(context: Context, mono: Boolean, bold: Boolean): Typeface? = synchronized(cache) {
        cache.getOrPut("$mono/$bold") {
            val base = runCatching {
                ResourcesCompat.getFont(context, if (mono) R.font.geist_mono_medium else R.font.geist_medium)
            }.getOrNull()
            Typeface.create(base ?: Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}

/** Material icons as Android paths, so they can be drawn into widget bitmaps. */
private object ThemeIcons {
    private val cache = HashMap<ImageVector, List<Path>>()

    fun paths(vector: ImageVector): List<Path> = synchronized(cache) {
        cache.getOrPut(vector) { collect(vector.root) }
    }

    private fun collect(group: VectorGroup): List<Path> = group.flatMap { node: VectorNode ->
        when (node) {
            is VectorGroup -> collect(node)
            is VectorPath -> listOf(
                node.pathData.toPath().asAndroidPath().apply {
                    fillType = if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                }
            )
        }
    }
}
