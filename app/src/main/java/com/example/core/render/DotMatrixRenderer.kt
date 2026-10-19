package com.example.core.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.theme.BackgroundMode
import com.example.core.time.DotStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance Canvas renderer for monochrome dot-matrix visuals.
 */
object DotMatrixRenderer {

    /**
     * Dot style for the widget currently being drawn. Set per render with [withDotStyle]; a
     * ThreadLocal because several widgets can render at once on different threads.
     */
    private val dotStyle = ThreadLocal.withInitial { DotStyle.ROUND }

    fun <T> withDotStyle(style: DotStyle, block: () -> T): T {
        val previous = dotStyle.get()
        dotStyle.set(style)
        try {
            return block()
        } finally {
            dotStyle.set(previous)
        }
    }

    /** Draws one lit or unlit dot in the current [DotStyle]. */
    private fun dot(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint, lit: Boolean) {
        when (dotStyle.get()) {
            DotStyle.MINIMAL -> if (lit) canvas.drawCircle(cx, cy, radius, paint)
            DotStyle.SQUARE -> {
                val half = radius * 0.92f
                canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
            }
            DotStyle.BOLD -> canvas.drawCircle(cx, cy, if (lit) radius * 1.22f else radius, paint)
            else -> canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    private val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * Draws a line of dot-matrix text using the 5x7 glyph table.
     * Always renders the OFF ghost dots as required by design specification.
     */
    fun drawText(
        canvas: Canvas,
        text: String,
        startX: Float,
        startY: Float,
        dotRadius: Float,
        dotGap: Float,
        onColor: Int,
        offColor: Int,
        accentIndices: Set<Int> = emptySet(),
        accentColor: Int = onColor
    ) {
        paintOn.color = onColor
        paintOff.color = offColor

        val charStepX = (GlyphTable.WIDTH * dotGap) + (dotGap * 1.5f)
        var currentX = startX

        for (charIndex in text.indices) {
            val char = text[charIndex]
            val glyph = GlyphTable.getGlyph(char)
            val isAccent = accentIndices.contains(charIndex)

            for (row in 0 until GlyphTable.HEIGHT) {
                for (col in 0 until GlyphTable.WIDTH) {
                    val cx = currentX + (col * dotGap)
                    val cy = startY + (row * dotGap)
                    val isOn = glyph[row * GlyphTable.WIDTH + col]

                    if (isOn) {
                        paintOn.color = if (isAccent) accentColor else onColor
                        dot(canvas, cx, cy, dotRadius, paintOn, lit = true)
                    } else {
                        dot(canvas, cx, cy, dotRadius * 0.85f, paintOff, lit = false)
                    }
                }
            }
            currentX += charStepX
        }
    }

    /**
     * Calculates width of dot-matrix text for centering or layout.
     */
    fun measureTextWidth(textLength: Int, dotGap: Float): Float {
        if (textLength <= 0) return 0f
        val charStepX = (GlyphTable.WIDTH * dotGap) + (dotGap * 1.5f)
        return (textLength * charStepX) - (dotGap * 1.5f)
    }

    /**
     * Automatically calculates optimal dotGap so dot text fits cleanly within both maxAllowedWidth and maxAllowedHeight.
     * Prevents any clipping or off-screen drawing across arbitrary widget shapes and sizes.
     */
    fun fitDotGap(
        textLength: Int,
        maxAllowedWidth: Float,
        maxAllowedHeight: Float = Float.MAX_VALUE,
        preferredGap: Float
    ): Float {
        if (textLength <= 0) return preferredGap
        val factor = (GlyphTable.WIDTH * textLength) + ((textLength - 1) * 1.5f)
        val maxGapWidth = if (factor > 0) maxAllowedWidth / factor else preferredGap
        val maxGapHeight = maxAllowedHeight / GlyphTable.HEIGHT.toFloat()
        val fitted = minOf(preferredGap, maxGapWidth, maxGapHeight)
        return fitted.coerceAtLeast(3.0f)
    }

    /**
     * Draws text with auto-fitting and intelligent ellipsizing so labels never clip on any launcher.
     */
    fun drawFittedLabel(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        if (maxWidth <= 0f) return
        val originalSize = paint.textSize
        var currentSize = originalSize
        while (paint.measureText(text) > maxWidth && currentSize > 9f) {
            currentSize -= 0.5f
            paint.textSize = currentSize
        }
        var finalText = text
        if (paint.measureText(finalText) > maxWidth) {
            while (finalText.isNotEmpty() && paint.measureText("$finalText…") > maxWidth) {
                finalText = finalText.dropLast(1)
            }
            finalText = if (finalText.isNotEmpty()) "$finalText…" else ""
        }
        canvas.drawText(finalText, x, y, paint)
        paint.textSize = originalSize
    }

    /**
     * Draws horizontal run of dots for progress bars, percentage, or volume levels.
     */
    fun drawDotRun(
        canvas: Canvas,
        current: Int,
        max: Int,
        totalDots: Int,
        startX: Float,
        startY: Float,
        dotRadius: Float,
        dotGap: Float,
        onColor: Int,
        offColor: Int,
        accentColor: Int = onColor,
        useAccentOnActive: Boolean = false
    ) {
        val filled = if (max > 0) ((current.toFloat() / max) * totalDots).toInt().coerceIn(0, totalDots) else 0

        for (i in 0 until totalDots) {
            val cx = startX + (i * dotGap)
            val isFilled = i < filled
            val isLastActive = (i == filled - 1) && filled > 0

            val paint = if (isFilled) {
                paintOn.color = if (useAccentOnActive && isLastActive) accentColor else onColor
                paintOn
            } else {
                paintOff.color = offColor
                paintOff
            }

            dot(canvas, cx, startY, if (isFilled) dotRadius else dotRadius * 0.8f, paint, isFilled)
        }
    }

    /**
     * Draws a circular ring of dots (e.g. for battery ring, analog dial, seconds countdown).
     */
    fun drawDotRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        totalDots: Int,
        filledDots: Int,
        dotRadius: Float,
        onColor: Int,
        offColor: Int,
        accentColor: Int,
        startAngleDeg: Float = -90f
    ) {
        val angleStep = 360f / totalDots

        for (i in 0 until totalDots) {
            val angleDeg = startAngleDeg + (i * angleStep)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cx = centerX + (radius * cos(angleRad)).toFloat()
            val cy = centerY + (radius * sin(angleRad)).toFloat()

            val isFilled = i < filledDots
            val isLeadingDot = (i == filledDots - 1) && filledDots > 0

            val paint = if (isFilled) {
                paintOn.color = if (isLeadingDot) accentColor else onColor
                paintOn
            } else {
                paintOff.color = offColor
                paintOff
            }

            dot(canvas, cx, cy, if (isFilled) dotRadius else dotRadius * 0.8f, paint, isFilled)
        }
    }

    /**
     * Draws an analog clock face with hour & minute dots and center pin.
     */
    fun drawAnalogClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        hours: Int,
        minutes: Int,
        seconds: Int,
        accentColor: Int,
        onColor: Int,
        offColor: Int
    ) {
        // Draw 12 hour dial dots
        drawDotRing(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            totalDots = 12,
            filledDots = 12,
            dotRadius = radius * 0.07f,
            onColor = offColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Draw Hour hand as a line of dots
        val hour12 = hours % 12
        val hourAngle = Math.toRadians((-90f + (hour12 * 30f) + (minutes * 0.5f)).toDouble())
        val hourDots = 4
        val hourStep = (radius * 0.52f) / hourDots
        paintOn.color = onColor
        for (i in 1..hourDots) {
            val hx = centerX + (i * hourStep * cos(hourAngle)).toFloat()
            val hy = centerY + (i * hourStep * sin(hourAngle)).toFloat()
            canvas.drawCircle(hx, hy, radius * 0.08f, paintOn)
        }

        // Draw Minute hand as a line of dots
        val minuteAngle = Math.toRadians((-90f + (minutes * 6f)).toDouble())
        val minuteDots = 6
        val minuteStep = (radius * 0.78f) / minuteDots
        paintOn.color = onColor
        for (i in 1..minuteDots) {
            val mx = centerX + (i * minuteStep * cos(minuteAngle)).toFloat()
            val my = centerY + (i * minuteStep * sin(minuteAngle)).toFloat()
            canvas.drawCircle(mx, my, radius * 0.06f, paintOn)
        }

        // Second indicator dot on outer edge (pass seconds < 0 to hide it, e.g. on home-screen
        // widgets that only redraw once a minute)
        if (seconds >= 0) {
            val secondAngle = Math.toRadians((-90f + (seconds * 6f)).toDouble())
            val sx = centerX + (radius * 0.92f * cos(secondAngle)).toFloat()
            val sy = centerY + (radius * 0.92f * sin(secondAngle)).toFloat()
            paintOn.color = accentColor
            canvas.drawCircle(sx, sy, radius * 0.09f, paintOn)
        }

        // Center pivot dot
        paintOn.color = accentColor
        canvas.drawCircle(centerX, centerY, radius * 0.09f, paintOn)
    }

    /**
     * Draws dot-matrix weather icon (Sun, Cloud, Rain, Thunder, Snow).
     */
    fun drawWeatherIcon(
        canvas: Canvas,
        weatherCode: Int,
        centerX: Float,
        centerY: Float,
        dotGap: Float,
        dotRadius: Float,
        onColor: Int,
        offColor: Int,
        accentColor: Int
    ) {
        val iconPattern = when (weatherCode) {
            in 0..1 -> WEATHER_SUN
            in 2..3 -> WEATHER_PARTLY_CLOUDY
            in 51..67 -> WEATHER_RAIN
            in 71..77 -> WEATHER_SNOW
            in 95..99 -> WEATHER_STORM
            else -> WEATHER_CLOUD
        }

        val rows = 7
        val cols = 7
        val startX = centerX - ((cols - 1) * dotGap / 2f)
        val startY = centerY - ((rows - 1) * dotGap / 2f)

        paintOn.color = onColor
        paintOff.color = offColor

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = startX + (c * dotGap)
                val cy = startY + (r * dotGap)
                val bit = iconPattern[r * cols + c]

                if (bit == 1) {
                    paintOn.color = if (weatherCode in 0..1 || (r == 0 && c == 6)) accentColor else onColor
                    dot(canvas, cx, cy, dotRadius, paintOn, lit = true)
                } else {
                    dot(canvas, cx, cy, dotRadius * 0.7f, paintOff, lit = false)
                }
            }
        }
    }

    /**
     * Draws widget background and hairline border according to background mode and corner radius.
     */
    fun drawBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        cornerRadius: Float,
        bgMode: BackgroundMode,
        isDark: Boolean,
        accentColor: Int,
        /** Fill opacity, 0 = see-through, 1 = fully opaque. Ignored when [bgMode] is TRANSPARENT. */
        bgAlpha: Float = 1f,
        showAccentDot: Boolean = true
    ) {
        val rect = RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f)
        val alpha = bgAlpha.coerceIn(0f, 1f)

        // Fill
        val bgColor = when (bgMode) {
            BackgroundMode.SOLID -> {
                val a = (alpha * 255).toInt()
                if (isDark) Color.argb(a, 0, 0, 0) else Color.argb(a, 242, 242, 242)
            }
            BackgroundMode.TRANSPARENT -> Color.TRANSPARENT
            BackgroundMode.SCRIM -> {
                val base = if (isDark) 120 else 140
                val a = (base * alpha).toInt()
                if (isDark) Color.argb(a, 10, 10, 10) else Color.argb(a, 240, 240, 240)
            }
        }

        if (bgColor != Color.TRANSPARENT) {
            paintFill.color = bgColor
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintFill)
        }

        // Hairline border: 18% alpha of foreground
        val strokeColor = if (isDark) Color.argb(46, 255, 255, 255) else Color.argb(46, 10, 10, 10)
        paintStroke.color = strokeColor
        paintStroke.strokeWidth = 2f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintStroke)

        // Accent indicator dot in top-right corner
        if (showAccentDot) {
            val dotInset = (cornerRadius * 0.72f).coerceIn(16f, 24f)
            paintOn.color = accentColor
            canvas.drawCircle(width - dotInset, dotInset, 4.0f, paintOn)
        }
    }

    // 7x7 Icon Bitmaps
    private val WEATHER_SUN = intArrayOf(
        0,0,1,0,0,0,0,
        1,0,0,0,1,0,0,
        0,1,1,1,0,0,0,
        1,1,1,1,1,0,1,
        0,1,1,1,0,0,0,
        1,0,0,0,1,0,0,
        0,0,1,0,0,0,0
    )

    private val WEATHER_CLOUD = intArrayOf(
        0,0,0,0,0,0,0,
        0,0,1,1,0,0,0,
        0,1,0,0,1,1,0,
        1,0,0,0,0,0,1,
        1,1,1,1,1,1,1,
        0,0,0,0,0,0,0,
        0,0,0,0,0,0,0
    )

    private val WEATHER_PARTLY_CLOUDY = intArrayOf(
        0,0,0,0,1,0,1,
        0,0,1,1,0,1,0,
        0,1,0,0,1,1,0,
        1,0,0,0,0,0,1,
        1,1,1,1,1,1,1,
        0,0,0,0,0,0,0,
        0,0,0,0,0,0,0
    )

    private val WEATHER_RAIN = intArrayOf(
        0,0,1,1,0,0,0,
        0,1,0,0,1,1,0,
        1,0,0,0,0,0,1,
        1,1,1,1,1,1,1,
        0,1,0,1,0,1,0,
        1,0,1,0,1,0,0,
        0,1,0,1,0,0,0
    )

    private val WEATHER_SNOW = intArrayOf(
        0,0,1,1,0,0,0,
        0,1,0,0,1,1,0,
        1,0,0,0,0,0,1,
        1,1,1,1,1,1,1,
        0,1,0,0,1,0,0,
        0,0,1,0,0,1,0,
        0,1,0,0,1,0,0
    )

    private val WEATHER_STORM = intArrayOf(
        0,0,1,1,0,0,0,
        0,1,0,0,1,1,0,
        1,0,0,0,0,0,1,
        1,1,1,1,1,1,1,
        0,0,1,1,0,0,0,
        0,1,1,0,0,0,0,
        0,0,1,0,0,0,0
    )
}
