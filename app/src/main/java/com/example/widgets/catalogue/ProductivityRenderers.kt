package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.render.DotMatrixRenderer
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec

object ProductivityRenderers {

    fun renderNotesPin(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val note = PreferencesManager.getInstance(context).getNoteText()
        val lines = note.lines().take(4)

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
        DotMatrixRenderer.drawFittedLabel(canvas, "QUICK NOTE", pad, pad + 12f * spec.density, maxW, paintLabel)

        val startY = pad + (32f * spec.density)
        val availableH = spec.heightPx - startY - pad
        val lineSpacing = (availableH / lines.size.coerceAtLeast(1)).coerceIn(18f, 28f)

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }

        val textStartX = pad + (16f * spec.density)
        val textMaxW = spec.widthPx - pad - textStartX

        lines.forEachIndexed { i, line ->
            val y = startY + (i * lineSpacing) + (10f * spec.density)
            canvas.drawCircle(pad + 4f * spec.density, y - 4f * spec.density, 2.5f * spec.density, paintText)
            DotMatrixRenderer.drawFittedLabel(canvas, line.uppercase(), textStartX, y, textMaxW, paintText)
        }
    }

    fun renderTodoList(spec: WidgetSpec, canvas: Canvas, maxItems: Int = 3) {
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
        DotMatrixRenderer.drawFittedLabel(canvas, "DAILY TASKS", pad, pad + 12f * spec.density, maxW, paintLabel)

        val tasks = listOf(
            true to "FINISH CODEBASE MIGRATION",
            true to "AUDIT BATTERY USAGE",
            false to "SHIP RELEASE BUILD v1.0",
            false to "RECORD DEMO TEASER",
            false to "UPDATE CHANGELOG"
        ).take(maxItems)

        val startY = pad + (32f * spec.density)
        val availableH = spec.heightPx - startY - pad
        val lineSpacing = (availableH / tasks.size).coerceIn(20f, 32f)

        val paintDone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        val paintPending = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        val paintCheck = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
        }

        val textStartX = pad + (24f * spec.density)
        val textMaxW = spec.widthPx - pad - textStartX

        tasks.forEachIndexed { i, (isDone, title) ->
            val y = startY + (i * lineSpacing) + (12f * spec.density)
            val cx = pad + (6f * spec.density)
            val cy = y - 4f * spec.density

            if (isDone) {
                // Completed dot
                canvas.drawCircle(cx, cy, 4.0f * spec.density, paintCheck)
                DotMatrixRenderer.drawFittedLabel(canvas, title, textStartX, y, textMaxW, paintDone)
            } else {
                // Ghost dot circle
                val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = onColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * spec.density
                }
                canvas.drawCircle(cx, cy, 4.0f * spec.density, paintRing)
                DotMatrixRenderer.drawFittedLabel(canvas, title, textStartX, y, textMaxW, paintPending)
            }
        }
    }

    fun renderStepCounter(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val steps = PreferencesManager.getInstance(context).getStepCount()
        val goal = 10000

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

        // Title
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "DAILY STEPS", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Step number in dot matrix
        val stepStr = String.format("%,d", steps)
        val maxStepW = maxW * 0.50f
        val maxStepH = spec.heightPx * 0.38f
        val dotGap = DotMatrixRenderer.fitDotGap(stepStr.length, maxStepW, maxStepH, 8.5f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textW = DotMatrixRenderer.measureTextWidth(stepStr.length, dotGap)

        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = stepStr,
            startX = startX,
            startY = startY,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor
        )

        // Goal percentage
        val pct = ((steps.toFloat() / goal) * 100).toInt()
        val paintPct = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val pctLabel = "$pct% OF GOAL"
        val pctStartX = (startX + textW + (14f * spec.density)).coerceAtLeast(spec.widthPx * 0.52f)
        DotMatrixRenderer.drawFittedLabel(canvas, pctLabel, pctStartX, startY + (3.5f * dotGap), spec.widthPx - pad - pctStartX, paintPct)

        // 20-dot progress run across bottom
        val totalDots = 20
        val segGap = maxW / (totalDots - 1)
        val segY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = steps,
            max = goal,
            totalDots = totalDots,
            startX = pad,
            startY = segY,
            dotRadius = 2.8f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

    fun renderWaterTracker(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val glasses = PreferencesManager.getInstance(context).getWaterCount()
        val total = 8

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
        DotMatrixRenderer.drawFittedLabel(canvas, "HYDRATION TRACKER", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Glass count
        val countStr = "$glasses/$total"
        val maxTextW = maxW * 0.45f
        val maxTextH = spec.heightPx * 0.38f
        val dotGap = DotMatrixRenderer.fitDotGap(countStr.length, maxTextW, maxTextH, 9f * spec.density)
        val dotRadius = dotGap * 0.38f

        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = countStr,
            startX = startX,
            startY = startY,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(0),
            accentColor = accentColor
        )

        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        val subStartX = (spec.widthPx * 0.52f).coerceAtLeast(startX + DotMatrixRenderer.measureTextWidth(countStr.length, dotGap) + 12f * spec.density)
        DotMatrixRenderer.drawFittedLabel(canvas, "${glasses * 250} ML LOGGED", subStartX, startY + (3.5f * dotGap), spec.widthPx - pad - subStartX, paintSub)

        // 8 water drop dots
        val segGap = maxW / (total - 1)
        val segY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = glasses,
            max = total,
            totalDots = total,
            startX = pad,
            startY = segY,
            dotRadius = 4.0f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }
}
