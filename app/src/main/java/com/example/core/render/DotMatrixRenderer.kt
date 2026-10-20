package com.example.core.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.core.theme.BackgroundMode
import com.example.core.time.DotStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance Canvas renderer for monochrome dot-matrix visuals.
 */
object DotMatrixRenderer {

    /**
     * Dot style and skin for the widget currently being drawn. Set per render with [withStyle];
     * ThreadLocals because several widgets can render at once on different threads.
     */
    private val dotStyle = ThreadLocal.withInitial { DotStyle.ROUND }
    private val skin = ThreadLocal.withInitial { Skin.DOT }
    private val frame = ThreadLocal.withInitial { Frame.HAIRLINE }

    /** Skin of the widget being drawn, for renderers that draw their own shapes. */
    val currentSkin: Skin get() = skin.get() ?: Skin.DOT

    fun <T> withStyle(style: DotStyle, widgetSkin: Skin, widgetFrame: Frame = Frame.HAIRLINE, block: () -> T): T {
        val previousStyle = dotStyle.get()
        val previousSkin = skin.get()
        val previousFrame = frame.get()
        dotStyle.set(style)
        skin.set(widgetSkin)
        frame.set(widgetFrame)
        try {
            return block()
        } finally {
            dotStyle.set(previousStyle)
            skin.set(previousSkin)
            frame.set(previousFrame)
        }
    }

    fun <T> withDotStyle(style: DotStyle, block: () -> T): T = withStyle(style, currentSkin, frame.get() ?: Frame.HAIRLINE, block)

    /** Draws one lit or unlit dot in the current skin and [DotStyle]. */
    private fun dot(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint, lit: Boolean) {
        when (currentSkin) {
            Skin.PIXEL -> {
                if (!lit) return
                canvas.drawRect(cx - radius * 1.3f, cy - radius * 1.3f, cx + radius * 1.3f, cy + radius * 1.3f, paint)
                return
            }
            Skin.SEGMENT, Skin.TERMINAL -> {
                val half = radius * 0.9f
                canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, half * 0.3f, half * 0.3f, paint)
                return
            }
            Skin.GLYPH -> {
                if (!lit) {
                    canvas.drawCircle(cx, cy, radius * 0.5f, paint)
                    return
                }
                // Short light strip with a soft glow
                drawGlow(canvas, paint.color, radius * 1.4f) { p -> canvas.drawRoundRect(cx - radius * 1.2f, cy - radius * 0.55f, cx + radius * 1.2f, cy + radius * 0.55f, radius, radius, p) }
                canvas.drawRoundRect(cx - radius * 1.2f, cy - radius * 0.55f, cx + radius * 1.2f, cy + radius * 0.55f, radius, radius, paint)
                return
            }
            Skin.NEON -> if (lit) drawGlow(canvas, paint.color, radius * 1.6f) { p -> canvas.drawCircle(cx, cy, radius, p) }
            else -> Unit
        }
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

    private val glowPaint = ThreadLocal.withInitial { Paint(Paint.ANTI_ALIAS_FLAG) }

    /** Draws [shape] blurred in [color] behind the crisp shape, for glowing skins. */
    private inline fun drawGlow(canvas: Canvas, color: Int, blur: Float, shape: (Paint) -> Unit) {
        val p = glowPaint.get()!!
        p.color = color
        p.alpha = 170
        p.maskFilter = BlurMaskFilter(blur.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        shape(p)
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
        val activeSkin = currentSkin

        for (charIndex in text.indices) {
            val char = text[charIndex]
            val isAccent = accentIndices.contains(charIndex)
            val litColor = if (isAccent) accentColor else onColor

            // Skins with their own letterforms; characters they can't draw fall back to the grid.
            val drawn = when (activeSkin) {
                Skin.SEGMENT, Skin.GLYPH -> drawSegmentChar(canvas, char, currentX, startY, dotGap, litColor, offColor, activeSkin == Skin.GLYPH)
                Skin.TERMINAL -> drawTerminalChar(canvas, char, currentX, startY, dotGap, litColor)
                Skin.NOTHING -> NothingFonts.ndot?.let { drawFontChar(canvas, char, currentX, startY, dotGap, litColor, it) } ?: false
                else -> false
            }
            if (!drawn) {
                val glyph = GlyphTable.getGlyph(char)
                for (row in 0 until GlyphTable.HEIGHT) {
                    for (col in 0 until GlyphTable.WIDTH) {
                        val cx = currentX + (col * dotGap)
                        val cy = startY + (row * dotGap)
                        if (glyph[row * GlyphTable.WIDTH + col]) {
                            paintOn.color = litColor
                            dot(canvas, cx, cy, dotRadius, paintOn, lit = true)
                        } else {
                            paintOff.color = offColor
                            dot(canvas, cx, cy, dotRadius * 0.85f, paintOff, lit = false)
                        }
                    }
                }
            }
            currentX += charStepX
        }
    }

    // Segment bits: a=top, b=top-right, c=bottom-right, d=bottom, e=bottom-left, f=top-left, g=middle
    private const val SA = 1; private const val SB = 2; private const val SC = 4; private const val SD = 8
    private const val SE = 16; private const val SF = 32; private const val SG = 64

    private val SEGMENTS: Map<Char, Int> = mapOf(
        '0' to (SA or SB or SC or SD or SE or SF), '1' to (SB or SC),
        '2' to (SA or SB or SG or SE or SD), '3' to (SA or SB or SG or SC or SD),
        '4' to (SF or SG or SB or SC), '5' to (SA or SF or SG or SC or SD),
        '6' to (SA or SF or SG or SE or SD or SC), '7' to (SA or SB or SC),
        '8' to 127, '9' to (SA or SB or SC or SD or SF or SG),
        'A' to (SA or SB or SC or SE or SF or SG), 'B' to (SC or SD or SE or SF or SG),
        'C' to (SA or SD or SE or SF), 'D' to (SB or SC or SD or SE or SG),
        'E' to (SA or SD or SE or SF or SG), 'F' to (SA or SE or SF or SG),
        'G' to (SA or SC or SD or SE or SF), 'H' to (SB or SC or SE or SF or SG),
        'I' to (SE or SF), 'J' to (SB or SC or SD or SE), 'L' to (SD or SE or SF),
        'N' to (SC or SE or SG), 'O' to (SA or SB or SC or SD or SE or SF),
        'P' to (SA or SB or SE or SF or SG), 'R' to (SE or SG), 'S' to (SA or SF or SG or SC or SD),
        'T' to (SD or SE or SF or SG), 'U' to (SB or SC or SD or SE or SF),
        'Y' to (SB or SC or SD or SF or SG), '-' to SG, ' ' to 0,
        // Usual 7-segment stand-ins for letters with no true segment form
        'V' to (SB or SC or SD or SE or SF), 'X' to (SB or SC or SE or SF or SG)
    )

    private val segPaint = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    }

    /**
     * 7-segment letterform inside the same 5×7 cell the dot grid uses, so layouts don't move.
     * [glyph] draws thin glowing strips and hides unlit segments; otherwise chunky LCD bars with
     * faint unlit segments. Returns false for characters with no segment form.
     */
    private fun drawSegmentChar(
        canvas: Canvas, char: Char, x0: Float, y0: Float, gap: Float,
        litColor: Int, offColor: Int, glyph: Boolean
    ): Boolean {
        val c = char.uppercaseChar()
        val x1 = x0 + 4 * gap
        val y1 = y0 + 6 * gap
        val ym = y0 + 3 * gap
        val p = segPaint.get()!!
        val t = if (glyph) gap * 0.42f else gap * 0.9f
        p.strokeWidth = t
        p.strokeCap = Paint.Cap.ROUND

        if (c == ':' || c == '.') {
            val r = t * 0.62f
            val cx = x0 + 2 * gap
            val dots = if (c == ':') listOf(y0 + 2 * gap, y0 + 4 * gap) else listOf(y1)
            dots.forEach { cy ->
                if (glyph) drawGlow(canvas, litColor, t * 1.6f) { g -> canvas.drawCircle(cx, cy, r, g) }
                paintOn.color = litColor
                canvas.drawCircle(cx, cy, r, paintOn)
            }
            return true
        }
        val bits = SEGMENTS[c] ?: return false

        val i = t / 2f + gap * 0.15f // keep segments from touching at the corners
        val segs = listOf(
            SA to floatArrayOf(x0 + i, y0, x1 - i, y0),
            SB to floatArrayOf(x1, y0 + i, x1, ym - i),
            SC to floatArrayOf(x1, ym + i, x1, y1 - i),
            SD to floatArrayOf(x0 + i, y1, x1 - i, y1),
            SE to floatArrayOf(x0, ym + i, x0, y1 - i),
            SF to floatArrayOf(x0, y0 + i, x0, ym - i),
            SG to floatArrayOf(x0 + i, ym, x1 - i, ym)
        )
        for ((bit, line) in segs) {
            val lit = bits and bit != 0
            if (!lit && glyph) continue
            p.color = if (lit) litColor else offColor
            if (lit && glyph) {
                drawGlow(canvas, litColor, t * 1.8f) { g ->
                    g.style = Paint.Style.STROKE
                    g.strokeWidth = t
                    g.strokeCap = Paint.Cap.ROUND
                    canvas.drawLine(line[0], line[1], line[2], line[3], g)
                    g.style = Paint.Style.FILL
                }
            }
            canvas.drawLine(line[0], line[1], line[2], line[3], p)
        }
        return true
    }

    private val terminalPaint = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    /** Monospace character filling the dot cell. */
    private val nothingPaint = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    }

    /** Character in Nothing's NDot font, sized to fill the same 5×7 cell as the dot grid. */
    private fun drawFontChar(canvas: Canvas, char: Char, x0: Float, y0: Float, gap: Float, color: Int, typeface: Typeface): Boolean {
        if (char == ' ') return true
        val p = nothingPaint.get()!!
        p.typeface = typeface
        p.color = color
        p.textSize = gap * 8.6f
        canvas.drawText(char.uppercaseChar().toString(), x0 + 2 * gap, y0 + 6.3f * gap, p)
        return true
    }

    private fun drawTerminalChar(canvas: Canvas, char: Char, x0: Float, y0: Float, gap: Float, color: Int): Boolean {
        if (char == ' ') return true
        val p = terminalPaint.get()!!
        p.color = color
        p.textSize = gap * 8.4f
        canvas.drawText(char.toString(), x0 + 2 * gap, y0 + 6.2f * gap, p)
        return true
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
        val originalTypeface = paint.typeface
        skinTypeface()?.let { paint.typeface = it }
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
        paint.typeface = originalTypeface
    }

    /** Label typeface for the current skin, or null to keep the renderer's own. */
    fun skinTypeface(): Typeface? = when (currentSkin) {
        Skin.NOTHING -> NothingFonts.lettera
        Skin.TERMINAL, Skin.SEGMENT -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        Skin.PAPER -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        else -> null
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

        val activeSkin = currentSkin

        // Fill
        val bgColor = when (bgMode) {
            BackgroundMode.SOLID -> {
                val a = (alpha * 255).toInt()
                when {
                    activeSkin == Skin.PAPER -> Color.argb(a, 239, 233, 221)
                    activeSkin == Skin.TERMINAL -> Color.argb(a, 6, 10, 6)
                    activeSkin == Skin.SEGMENT && isDark -> Color.argb(a, 18, 22, 18)
                    activeSkin == Skin.SEGMENT -> Color.argb(a, 201, 212, 180)
                    isDark -> Color.argb(a, 0, 0, 0)
                    else -> Color.argb(a, 242, 242, 242)
                }
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
            if (activeSkin == Skin.TERMINAL) {
                // CRT scan lines
                canvas.save()
                canvas.clipRect(rect)
                paintFill.color = Color.argb(14, 255, 255, 255)
                var y = rect.top
                while (y < rect.bottom) {
                    canvas.drawRect(rect.left, y, rect.right, y + 1f, paintFill)
                    y += 4f
                }
                canvas.restore()
            }
        }

        if (activeSkin == Skin.NEON) {
            drawGlow(canvas, accentColor, 10f) { p ->
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, p)
                p.style = Paint.Style.FILL
            }
        }

        // Edge: 18%-alpha hairline by default, or the widget's chosen frame
        val edgeColor = if (isDark) Color.argb(46, 255, 255, 255) else Color.argb(46, 10, 10, 10)
        when (frame.get() ?: Frame.HAIRLINE) {
            Frame.HAIRLINE -> {
                paintStroke.color = edgeColor
                paintStroke.strokeWidth = 2f
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintStroke)
            }
            Frame.NONE -> Unit
            Frame.DOTTED -> {
                paintStroke.color = if (isDark) Color.argb(140, 255, 255, 255) else Color.argb(140, 10, 10, 10)
                paintStroke.strokeWidth = 3f
                paintStroke.strokeCap = Paint.Cap.ROUND
                paintStroke.pathEffect = android.graphics.DashPathEffect(floatArrayOf(0.1f, 9f), 0f)
                val inset = RectF(rect.left + 3f, rect.top + 3f, rect.right - 3f, rect.bottom - 3f)
                canvas.drawRoundRect(inset, cornerRadius, cornerRadius, paintStroke)
                paintStroke.pathEffect = null
                paintStroke.strokeCap = Paint.Cap.BUTT
            }
            Frame.GLOW -> drawGlow(canvas, accentColor, 9f) { p ->
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, p)
                p.style = Paint.Style.FILL
            }
        }

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
