package com.example.core.render

import android.graphics.Canvas
import android.graphics.Paint
import com.example.widgets.base.WidgetSpec

/**
 * Shared setup for widget renderers: theme colours, padding, background and the small
 * grey header label that most widgets draw in their top-left corner.
 */
class RenderKit(val spec: WidgetSpec, val canvas: Canvas) {
    val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
    val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
    val accentColor = spec.accentColor
    // Grey captions need more contrast on a light background to stay legible.
    val greyColor = if (spec.isDark) 0xFF8A8A8A.toInt() else 0xFF6A6A6A.toInt()

    val pad = spec.widthPx * 0.08f
    val maxW = spec.widthPx - (2 * pad)
    val width = spec.widthPx.toFloat()
    val height = spec.heightPx.toFloat()
    val d = spec.density

    /** 1 u = 1% of the widget's short side. Text sized in u scales with the widget. */
    val u = minOf(width, height) / 100f

    fun background() {
        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = width,
            height = height,
            cornerRadius = spec.cornerRadiusDp * d,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor,
            bgAlpha = spec.bgAlpha
        )
    }

    /** Small header label in the top-left, respecting the "show label" option. */
    fun header(text: String, color: Int = greyColor, widthFraction: Float = 0.75f) {
        if (!spec.showLabel) return
        DotMatrixRenderer.drawFittedLabel(canvas, text, pad, pad + 12f * d, maxW * widthFraction, textPaint(color, 10.5f, 14f))
    }

    fun textPaint(color: Int, sizeDp: Float, maxPx: Float, bold: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = (sizeDp * d).coerceAtMost(maxPx)
        isFakeBoldText = bold
    }

    /** Text sized in [u]; scales evenly with the widget instead of being capped in px. */
    fun textPaint(color: Int, sizeU: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizeU * u
        isFakeBoldText = true
    }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    /** Draws dot-matrix text centred horizontally, fitted inside [maxWidth] x [maxHeight]. Returns the dot gap used. */
    fun dotTextCentered(
        text: String,
        centerY: Float,
        maxHeight: Float,
        preferredGap: Float,
        maxWidth: Float = maxW,
        accentIndices: Set<Int> = emptySet(),
        color: Int = onColor
    ): Float {
        val gap = DotMatrixRenderer.fitDotGap(text.length, maxWidth, maxHeight, preferredGap)
        val textW = DotMatrixRenderer.measureTextWidth(text.length, gap)
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = text,
            startX = (width - textW) / 2f,
            startY = centerY - (GlyphTable.HEIGHT - 1) * gap / 2f,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = color,
            offColor = offColor,
            accentIndices = accentIndices,
            accentColor = accentColor
        )
        return gap
    }

    /** Plain text centred horizontally at baseline [y], shrunk to fit [maxWidth]. */
    fun labelCentered(text: String, y: Float, paint: Paint, maxWidth: Float = maxW) {
        val w = paint.measureText(text).coerceAtMost(maxWidth)
        DotMatrixRenderer.drawFittedLabel(canvas, text, ((width - w) / 2f).coerceAtLeast(pad), y, maxWidth, paint)
    }

    companion object {
        fun colonIndices(text: String): Set<Int> = text.indices.filter { text[it] == ':' }.toSet()

        /** Whole-widget message, e.g. when a permission the widget needs isn't granted. */
        fun message(spec: WidgetSpec, canvas: Canvas, title: String, message: String) {
            val kit = RenderKit(spec, canvas)
            kit.background()
            kit.header(title)
            kit.labelCentered(message, kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
        }
    }
}
