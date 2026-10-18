package com.example.widgets.catalogue

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.widgets.base.WidgetSpec

object MediaAndFunRenderers {

    fun renderNowPlaying(spec: WidgetSpec, canvas: Canvas, isPlaying: Boolean = true) {
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
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "NOW PLAYING", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Track title and artist
        val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (13f * spec.density).coerceAtMost(17f)
            isFakeBoldText = true
        }
        val paintArtist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }

        val trackY = pad + (36f * spec.density)
        val artistY = trackY + (18f * spec.density)
        DotMatrixRenderer.drawFittedLabel(canvas, "MIDNIGHT CITY", pad, trackY, maxW, paintTrack)
        DotMatrixRenderer.drawFittedLabel(canvas, "M83 • HURRY UP, WE'RE DREAMING", pad, artistY, maxW, paintArtist)

        // 20-dot progress bar
        val totalDots = 20
        val segGap = maxW / (totalDots - 1)
        val segY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = 13,
            max = 20,
            totalDots = totalDots,
            startX = pad,
            startY = segY,
            dotRadius = 2.6f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

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

    fun renderRetroSnake(spec: WidgetSpec, canvas: Canvas) {
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
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "SNAKE • SCORE: 42", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Dot matrix playfield: 15 columns x 8 rows
        val cols = 15
        val rows = 8
        val dotGap = (maxW / (cols - 1)).coerceAtMost((spec.heightPx - pad - 42f * spec.density) / rows)
        val dotRadius = dotGap * 0.34f

        val startX = (spec.widthPx - ((cols - 1) * dotGap)) / 2f
        val startY = pad + (30f * spec.density)

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintFood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        // Snake coordinates
        val snakeBody = setOf(
            3 to 2, 4 to 2, 5 to 2, 5 to 3, 5 to 4, 6 to 4, 7 to 4, 8 to 4, 9 to 4, 9 to 5, 9 to 6
        )
        val foodCoord = 12 to 3

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = startX + (c * dotGap)
                val cy = startY + (r * dotGap)

                if (c == foodCoord.first && r == foodCoord.second) {
                    canvas.drawCircle(cx, cy, dotRadius * 1.3f, paintFood)
                } else if (snakeBody.contains(c to r)) {
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

        val quoteLines = listOf(
            "\"SIMPLICITY IS",
            "THE ULTIMATE",
            "SOPHISTICATION.\""
        )
        val author = "— LEONARDO DA VINCI"

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (12f * spec.density).coerceAtMost(16f)
            isFakeBoldText = true
        }
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

    fun renderDiceRoll(spec: WidgetSpec, canvas: Canvas, die1: Int = 4, die2: Int = 6) {
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
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "ROLL: ${die1 + die2}", pad, pad + 12f * spec.density, maxW, paintLabel)

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
