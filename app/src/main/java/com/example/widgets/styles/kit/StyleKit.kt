package com.example.widgets.styles.kit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.core.render.Skin
import com.example.core.theme.BackgroundMode
import com.example.core.render.WidgetLayoutRecorder
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.StyleFamily

/**
 * Drawing helpers for the mockup design families. [StyleFamily.SOFT] follows the widget's light/dark
 * theme; [StyleFamily.BOLD] is always a black high-contrast tile. Both still use the widget's
 * background mode, opacity, shape, accent colour, text-size and corner-dot settings, because the card
 * is drawn by the same [RenderKit.background] every classic widget uses.
 *
 * Sizes are in `u` = 1% of the widget's short side, so the same layout scales across S/M/L.
 */
class StyleKit(
    val context: Context,
    spec: WidgetSpec,
    val canvas: Canvas,
    val family: StyleFamily = StyleFamily.SOFT
) {
    val bold = family == StyleFamily.BOLD
    val spec: WidgetSpec = if (bold) spec.copy(isDark = true) else spec
    val kit = RenderKit(this.spec, canvas)
    val dark = this.spec.isDark
    /**
     * In dark theme a soft card is a raised graphite card, so it never looks the same as the pure
     * black bold tile. A chosen fill, a see-through background or a skin with its own paper wins.
     */
    private val softDark = dark && !bold && spec.bgMode == BackgroundMode.SOLID &&
        !DotMatrixRenderer.hasFillOverride && DotMatrixRenderer.currentSkin !in OWN_BACKGROUND_SKINS

    val width = kit.width
    val height = kit.height
    val isSmall = spec.sizeBucket == SizeBucket.S
    val isWide = spec.sizeBucket == SizeBucket.M
    val isLarge = spec.sizeBucket == SizeBucket.L
    /**
     * 1% of the short side for S and M. Large (4×4) has the same width as Medium, so its unit tracks
     * Medium's (slightly bigger) instead of doubling, keeping row layouts readable rather than oversized.
     */
    val u = if (isLarge) width / 200f * 1.1f else minOf(width, height) / 100f

    val pad = 9f * u
    val left = pad
    val right = width - pad
    val top = pad
    val bottom = height - pad
    val contentW = right - left
    val cx = width / 2f
    val cy = height / 2f

    // Colours
    val ink = if (dark) 0xFFFFFFFF.toInt() else 0xFF0B0B0C.toInt()
    val sub = if (dark) 0xFF9A9A9E.toInt() else 0xFF6B6B70.toInt()
    val faint = if (dark) 0xFF5C5C60.toInt() else 0xFFA9A9AE.toInt()
    val track = if (!dark) 0xFFDCDCE0.toInt() else if (softDark) 0xFF3A3A3C.toInt() else 0xFF2C2C2E.toInt()
    /** A raised inner surface (icon buttons, segments) one step off the card colour. */
    val surface = if (!dark) 0xFFE6E6EA.toInt() else if (softDark) 0xFF2C2C2E.toInt() else 0xFF1C1C1E.toInt()
    val accent = this.spec.accentColor
    val positive = 0xFF30D158.toInt()
    val negative = 0xFFFF453A.toInt()

    private val textScale = DotMatrixRenderer.currentTextScale
    /** True when the red corner status dot is drawn (top-right); layouts keep that corner clear. */
    val hasCornerDot = DotMatrixRenderer.hasCornerDot
    /** Horizontal room to leave free at the top-right for the corner dot. */
    val cornerReserve: Float get() = if (hasCornerDot) 8f * u else 0f

    enum class Weight { REGULAR, MEDIUM, SEMIBOLD }
    enum class Align { LEFT, CENTER, RIGHT }

    /** The card itself (same background code path as every classic widget). */
    fun card() {
        if (!softDark) return kit.background()
        val alpha = (spec.bgAlpha.coerceIn(0f, 1f) * 255).toInt() shl 24
        DotMatrixRenderer.withCardFill(alpha or 0x1C1C1E) { kit.background() }
    }

    /** Where content can start below a [title]. */
    val contentTop: Float get() = if (spec.showLabel) top + 13f * u else top

    /** Title-case label top-left (the mockups' "Hydration", "Screen Time"...). Respects "show label". */
    fun title(text: String, color: Int = ink) {
        if (!spec.showLabel) return
        val reserve = if (hasCornerDot) 10f * u else 0f
        text(text, left, top + 8.5f * u, 9.5f, color, Weight.MEDIUM, maxWidth = contentW - reserve)
    }

    // ------------------------------------------------------------------ text

    fun paint(sizeU: Float, color: Int, weight: Weight = Weight.MEDIUM): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizeU * u * textScale
        typeface = DotMatrixRenderer.skinTypeface() ?: typeface(context, weight)
    }

    /**
     * Draws [text] at [baseline], shrunk to fit [maxWidth] (ellipsized only if it still can't fit).
     * Returns the drawn width.
     */
    fun text(
        text: String,
        x: Float,
        baseline: Float,
        sizeU: Float,
        color: Int = ink,
        weight: Weight = Weight.MEDIUM,
        align: Align = Align.LEFT,
        maxWidth: Float = contentW
    ): Float {
        if (text.isEmpty() || maxWidth <= 0f) return 0f
        val p = paint(sizeU, color, weight)
        val measured = p.measureText(text)
        if (measured > maxWidth) {
            // Never grows text, and steps past the rounding that would leave it a hair too wide.
            val floor = minOf(p.textSize, MIN_TEXT_DP * spec.density)
            p.textSize = (p.textSize * maxWidth / measured).coerceAtLeast(floor)
            while (p.measureText(text) > maxWidth && p.textSize > floor) p.textSize = (p.textSize - 0.25f).coerceAtLeast(floor)
        }
        var shown = text
        if (p.measureText(shown) > maxWidth) {
            while (shown.isNotEmpty() && p.measureText("$shown…") > maxWidth) shown = shown.dropLast(1)
            shown = if (shown.isEmpty()) "" else "$shown…"
        }
        val w = p.measureText(shown)
        val drawX = when (align) {
            Align.LEFT -> x
            Align.CENTER -> x - w / 2f
            Align.RIGHT -> x - w
        }
        WidgetLayoutRecorder.recordLabel(shown, drawX, baseline, p, shown != text)
        canvas.drawText(shown, drawX, baseline, p)
        return w
    }

    /** Width [text] would take at [sizeU] (before fitting). */
    fun measure(text: String, sizeU: Float, weight: Weight = Weight.MEDIUM): Float = paint(sizeU, ink, weight).measureText(text)

    // ---------------------------------------------------------------- shapes

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    fun stroke(color: Int, widthPx: Float, round: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = widthPx
        if (round) strokeCap = Paint.Cap.ROUND
    }

    fun circle(x: Float, y: Float, r: Float, color: Int) = canvas.drawCircle(x, y, r, fill(color))

    fun roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) =
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, fill(color))

    /** Horizontal pill progress bar (the mockups' "Storage 64%" bar). */
    fun pill(l: Float, t: Float, r: Float, b: Float, fraction: Float, color: Int = ink, trackColor: Int = track) {
        val h = b - t
        roundRect(l, t, r, b, h / 2f, trackColor)
        val f = fraction.coerceIn(0f, 1f)
        if (f <= 0f) return
        val fillR = (l + (r - l) * f).coerceAtLeast(l + h)
        roundRect(l, t, fillR, b, h / 2f, color)
    }

    /** Circular progress ring starting at 12 o'clock. */
    fun ring(x: Float, y: Float, radius: Float, strokePx: Float, fraction: Float, color: Int = ink, trackColor: Int = track) {
        val rect = RectF(x - radius, y - radius, x + radius, y + radius)
        canvas.drawArc(rect, 0f, 360f, false, stroke(trackColor, strokePx))
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) canvas.drawArc(rect, -90f, 360f * f, false, stroke(color, strokePx))
    }

    /** Open arc gauge (default 270° opening at the bottom) like the UV / AQI / calorie gauges. */
    fun arcGauge(
        x: Float, y: Float, radius: Float, strokePx: Float, fraction: Float,
        color: Int = ink, trackColor: Int = track, startDeg: Float = 135f, sweepDeg: Float = 270f
    ) {
        val rect = RectF(x - radius, y - radius, x + radius, y + radius)
        canvas.drawArc(rect, startDeg, sweepDeg, false, stroke(trackColor, strokePx))
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) canvas.drawArc(rect, startDeg, sweepDeg * f, false, stroke(color, strokePx))
    }

    /** Tick marks round a dial; every [majorEvery]-th tick is longer. */
    fun ticks(x: Float, y: Float, outer: Float, count: Int, minorLen: Float, majorLen: Float, majorEvery: Int, color: Int, widthPx: Float) {
        val p = stroke(color, widthPx)
        for (i in 0 until count) {
            val a = Math.toRadians(i * 360.0 / count - 90.0)
            val len = if (i % majorEvery == 0) majorLen else minorLen
            val cos = Math.cos(a).toFloat()
            val sin = Math.sin(a).toFloat()
            canvas.drawLine(x + cos * (outer - len), y + sin * (outer - len), x + cos * outer, y + sin * outer, p)
        }
    }

    /** A clock hand from the centre at [angleDeg] (0 = 12 o'clock). */
    fun hand(x: Float, y: Float, angleDeg: Float, length: Float, widthPx: Float, color: Int, tail: Float = 0f) {
        val a = Math.toRadians(angleDeg - 90.0)
        val cos = Math.cos(a).toFloat()
        val sin = Math.sin(a).toFloat()
        canvas.drawLine(x - cos * tail, y - sin * tail, x + cos * length, y + sin * length, stroke(color, widthPx))
    }

    /** Line chart through [values] inside the rect; optional soft fill underneath. */
    fun spark(values: List<Float>, l: Float, t: Float, r: Float, b: Float, color: Int, strokePx: Float = 1.6f * u, fillUnder: Boolean = false) {
        if (values.size < 2) return
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        values.forEachIndexed { i, v ->
            val px = l + (r - l) * i / (values.size - 1)
            val py = b - (b - t) * (v - min) / range
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        if (fillUnder) {
            val area = Path(path).apply { lineTo(r, b); lineTo(l, b); close() }
            canvas.drawPath(area, fill((color and 0x00FFFFFF) or 0x33000000))
        }
        canvas.drawPath(path, stroke(color, strokePx).apply { strokeJoin = Paint.Join.ROUND })
    }

    /** Vertical bars for [values] 0..1, rounded tops, evenly spaced across the rect. */
    fun bars(values: List<Float>, l: Float, t: Float, r: Float, b: Float, color: Int = ink, trackColor: Int? = null, gapFraction: Float = 0.45f, highlight: Int = -1, highlightColor: Int = accent) {
        if (values.isEmpty()) return
        val slot = (r - l) / values.size
        val barW = slot * (1f - gapFraction)
        values.forEachIndexed { i, v ->
            val x0 = l + i * slot + (slot - barW) / 2f
            if (trackColor != null) roundRect(x0, t, x0 + barW, b, barW / 2f, trackColor)
            val h = ((b - t) * v.coerceIn(0f, 1f)).coerceAtLeast(barW)
            roundRect(x0, b - h, x0 + barW, b, barW / 2f, if (i == highlight) highlightColor else color)
        }
    }

    fun icon(vector: ImageVector, x: Float, y: Float, size: Float, color: Int = ink) =
        StyleIcons.draw(canvas, vector, x, y, size, color)

    /** Icon inside a filled circle (the mockups' round toggle buttons). */
    fun iconButton(vector: ImageVector, x: Float, y: Float, radius: Float, bg: Int, fg: Int) {
        circle(x, y, radius, bg)
        icon(vector, x, y, radius * 1.05f, fg)
    }

    /** A small dot-matrix run (keeps a hint of the Dot language inside soft cards). */
    fun dotRun(l: Float, y: Float, r: Float, count: Int, filled: Int, dotR: Float, on: Int = ink, off: Int = track) {
        if (count < 2) return
        val step = (r - l) / (count - 1)
        for (i in 0 until count) circle(l + i * step, y, dotR, if (i < filled) on else off)
    }

    companion object {
        /** Smallest text the fitting step may shrink to. */
        private const val MIN_TEXT_DP = 7f

        private val OWN_BACKGROUND_SKINS = setOf(Skin.PAPER, Skin.TERMINAL, Skin.SEGMENT)

        @Volatile private var fonts: Map<Weight, Typeface>? = null

        fun typeface(context: Context, weight: Weight): Typeface {
            val loaded = fonts ?: synchronized(this) {
                fonts ?: mapOf(
                    Weight.REGULAR to load(context, R.font.geist_regular, Typeface.NORMAL),
                    Weight.MEDIUM to load(context, R.font.geist_medium, Typeface.NORMAL),
                    Weight.SEMIBOLD to load(context, R.font.geist_semibold, Typeface.BOLD)
                ).also { fonts = it }
            }
            return loaded.getValue(weight)
        }

        private fun load(context: Context, id: Int, fallbackStyle: Int): Typeface =
            runCatching { ResourcesCompat.getFont(context, id) }.getOrNull()
                ?: Typeface.create(Typeface.SANS_SERIF, fallbackStyle)
    }
}
