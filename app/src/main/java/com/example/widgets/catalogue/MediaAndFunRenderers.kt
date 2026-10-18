package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.widgets.base.WidgetSpec
import java.util.Calendar

object MediaAndFunRenderers {

    fun renderRetroSnake(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val snakeState = com.example.widgets.base.WidgetStateManager.getSnakeState(context)
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "SNAKE • SCORE: ${snakeState.score} • TAP TO STEP", pad, pad + 12f * spec.density, maxW, paintLabel)

        val cols = 15
        val rows = 8
        val dotGap = (maxW / (cols - 1)).coerceAtMost((spec.heightPx - pad - 42f * spec.density) / rows)
        val dotRadius = dotGap * 0.34f

        val startX = (spec.widthPx - ((cols - 1) * dotGap)) / 2f
        val startY = pad + (30f * spec.density)

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintFood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        val bodySet = snakeState.body.toSet()
        val foodCoord = snakeState.food

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = startX + (c * dotGap)
                val cy = startY + (r * dotGap)

                if (c == foodCoord.first && r == foodCoord.second) {
                    canvas.drawCircle(cx, cy, dotRadius * 1.3f, paintFood)
                } else if (bodySet.contains(c to r)) {
                    canvas.drawCircle(cx, cy, dotRadius, paintOn)
                } else {
                    canvas.drawCircle(cx, cy, dotRadius * 0.65f, paintOff)
                }
            }
        }
    }

    fun renderDailyQuote(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "DAILY THOUGHT", pad, pad + 12f * spec.density, maxW, paintLabel)

        // A different quote each day of the year.
        val (quote, by) = QUOTES[(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1) % QUOTES.size]
        val author = "— $by"

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (12f * spec.density).coerceAtMost(16f)
            isFakeBoldText = true
        }
        val maxLines = if (spec.heightPx > spec.widthPx * 0.8f) 6 else 3
        val quoteLines = wrapText("\"$quote\"", paintText, maxW, maxLines)
        val paintAuthor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(13f)
            isFakeBoldText = true
        }

        val startY = pad + (36f * spec.density)
        val lineSpacing = 20f * spec.density

        quoteLines.forEachIndexed { i, line ->
            DotMatrixRenderer.drawFittedLabel(canvas, line, pad, startY + (i * lineSpacing), maxW, paintText)
        }
        val authorY = startY + (quoteLines.size * lineSpacing) + (6f * spec.density)
        DotMatrixRenderer.drawFittedLabel(canvas, author, pad, authorY, maxW, paintAuthor)
    }

    /** Greedy word wrap; the last line is ellipsised if the text doesn't fit in [maxLines]. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        for (word in text.split(" ")) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        if (lines.size <= maxLines) return lines
        return lines.take(maxLines - 1) + (lines[maxLines - 1] + "…")
    }

    private val QUOTES = listOf(
        "SIMPLICITY IS THE ULTIMATE SOPHISTICATION." to "LEONARDO DA VINCI",
        "WELL DONE IS BETTER THAN WELL SAID." to "BENJAMIN FRANKLIN",
        "THE BEST WAY OUT IS ALWAYS THROUGH." to "ROBERT FROST",
        "WHAT WE THINK, WE BECOME." to "BUDDHA",
        "LESS IS MORE." to "LUDWIG MIES VAN DER ROHE",
        "KNOWING IS NOT ENOUGH; WE MUST APPLY." to "GOETHE",
        "THE ONLY WAY TO DO GREAT WORK IS TO LOVE WHAT YOU DO." to "STEVE JOBS",
        "IT ALWAYS SEEMS IMPOSSIBLE UNTIL IT IS DONE." to "NELSON MANDELA",
        "ACTION IS THE FOUNDATIONAL KEY TO ALL SUCCESS." to "PABLO PICASSO",
        "DO WHAT YOU CAN, WITH WHAT YOU HAVE, WHERE YOU ARE." to "THEODORE ROOSEVELT",
        "WHEREVER YOU GO, GO WITH ALL YOUR HEART." to "CONFUCIUS",
        "THE FUTURE DEPENDS ON WHAT YOU DO TODAY." to "MAHATMA GANDHI",
        "ARISE, AWAKE, AND STOP NOT TILL THE GOAL IS REACHED." to "SWAMI VIVEKANANDA",
        "DREAM, DREAM, DREAM. DREAMS TRANSFORM INTO THOUGHTS." to "A. P. J. ABDUL KALAM",
        "WE ARE WHAT WE REPEATEDLY DO." to "WILL DURANT",
        "WELL BEGUN IS HALF DONE." to "ARISTOTLE",
        "STAY HUNGRY. STAY FOOLISH." to "STEWART BRAND",
        "THE MIND IS EVERYTHING. WHAT YOU THINK YOU BECOME." to "BUDDHA",
        "GOOD DESIGN IS AS LITTLE DESIGN AS POSSIBLE." to "DIETER RAMS",
        "FORM FOLLOWS FUNCTION." to "LOUIS SULLIVAN",
        "YOU MISS 100% OF THE SHOTS YOU DON'T TAKE." to "WAYNE GRETZKY",
        "THE SECRET OF GETTING AHEAD IS GETTING STARTED." to "MARK TWAIN",
        "LIFE IS REALLY SIMPLE, BUT WE INSIST ON MAKING IT COMPLICATED." to "CONFUCIUS",
        "BE YOURSELF; EVERYONE ELSE IS ALREADY TAKEN." to "OSCAR WILDE",
        "IN THE MIDDLE OF DIFFICULTY LIES OPPORTUNITY." to "ALBERT EINSTEIN",
        "SMALL DEEDS DONE ARE BETTER THAN GREAT DEEDS PLANNED." to "PETER MARSHALL",
        "FOCUS ON BEING PRODUCTIVE INSTEAD OF BUSY." to "TIM FERRISS",
        "SIMPLE CAN BE HARDER THAN COMPLEX." to "STEVE JOBS",
        "THE JOURNEY OF A THOUSAND MILES BEGINS WITH ONE STEP." to "LAO TZU",
        "NOTHING WILL WORK UNLESS YOU DO." to "MAYA ANGELOU",
        "TIME IS WHAT WE WANT MOST, BUT WHAT WE USE WORST." to "WILLIAM PENN"
    )

    fun renderDiceRoll(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val (die1, die2) = com.example.widgets.base.WidgetStateManager.getDiceRoll(context)
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "DICE ROLL: ${die1 + die2} • TAP TO ROLL", pad, pad + 12f * spec.density, maxW, paintLabel)

        val maxAvailableH = spec.heightPx - pad - (40f * spec.density)
        val diceSize = minOf(52f * spec.density, maxAvailableH * 0.75f, maxW * 0.40f)
        val gap = 16f * spec.density
        val startX = (spec.widthPx - (2 * diceSize + gap)) / 2f
        val startY = (spec.heightPx - diceSize) / 2f + (8f * spec.density)

        drawDie(canvas, startX, startY, diceSize, die1, onColor, offColor, accentColor, spec.density)
        drawDie(canvas, startX + diceSize + gap, startY, diceSize, die2, onColor, offColor, accentColor, spec.density)
    }

    private fun drawDie(canvas: Canvas, x: Float, y: Float, size: Float, value: Int, onColor: Int, offColor: Int, accentColor: Int, density: Float) {
        val rect = RectF(x, y, x + size, y + size)
        val paintFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (onColor == 0xFFFFFFFF.toInt()) 0x33FFFFFF.toInt() else 0x33000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawRoundRect(rect, 8f * density, 8f * density, paintFrame)

        val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        val r = (size * 0.08f).coerceAtLeast(2.5f)

        val x1 = x + (size * 0.25f); val x2 = x + (size * 0.5f); val x3 = x + (size * 0.75f)
        val y1 = y + (size * 0.25f); val y2 = y + (size * 0.5f); val y3 = y + (size * 0.75f)

        when (value) {
            1 -> canvas.drawCircle(x2, y2, r * 1.3f, paintAccent)
            2 -> {
                canvas.drawCircle(x1, y1, r, paintDot)
                canvas.drawCircle(x3, y3, r, paintDot)
            }
            3 -> {
                canvas.drawCircle(x1, y1, r, paintDot)
                canvas.drawCircle(x2, y2, r, paintAccent)
                canvas.drawCircle(x3, y3, r, paintDot)
            }
            4 -> {
                canvas.drawCircle(x1, y1, r, paintDot)
                canvas.drawCircle(x3, y1, r, paintDot)
                canvas.drawCircle(x1, y3, r, paintDot)
                canvas.drawCircle(x3, y3, r, paintDot)
            }
            5 -> {
                canvas.drawCircle(x1, y1, r, paintDot)
                canvas.drawCircle(x3, y1, r, paintDot)
                canvas.drawCircle(x2, y2, r, paintAccent)
                canvas.drawCircle(x1, y3, r, paintDot)
                canvas.drawCircle(x3, y3, r, paintDot)
            }
            6 -> {
                canvas.drawCircle(x1, y1, r, paintDot)
                canvas.drawCircle(x3, y1, r, paintDot)
                canvas.drawCircle(x1, y2, r, paintDot)
                canvas.drawCircle(x3, y2, r, paintDot)
                canvas.drawCircle(x1, y3, r, paintDot)
                canvas.drawCircle(x3, y3, r, paintDot)
            }
        }
    }
}
