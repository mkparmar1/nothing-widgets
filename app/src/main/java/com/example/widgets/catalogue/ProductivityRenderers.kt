package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.activity.StepRepository
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetStateManager

object ProductivityRenderers {

    fun renderNotesPin(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val note = PreferencesManager.getInstance(context).getNoteText()
        val isLarge = spec.sizeBucket == SizeBucket.L
        val lines = note.lines().filter { it.isNotBlank() }
            .take(if (isLarge) 8 else 4)
            .ifEmpty { listOf("TAP TO WRITE A NOTE") }

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
        val lineSpacing = (availableH / lines.size.coerceAtLeast(1))
            .coerceIn(if (isLarge) 24f else 18f, if (isLarge) 44f else 28f)

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = if (isLarge) (14f * spec.density).coerceAtMost(20f) else (11f * spec.density).coerceAtMost(14f)
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

    fun renderTodoList(context: Context, spec: WidgetSpec, canvas: Canvas, maxItems: Int = 3) {
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
        val allTasks = PreferencesManager.getInstance(context).getTodos()
        val doneCount = allTasks.count { it.done }
        val header = if (allTasks.isEmpty()) "DAILY TASKS" else "DAILY TASKS • $doneCount/${allTasks.size} DONE"
        DotMatrixRenderer.drawFittedLabel(canvas, header, pad, pad + 12f * spec.density, maxW, paintLabel)

        // Unfinished tasks first so the widget shows what's left to do.
        val isLarge = spec.sizeBucket == SizeBucket.L
        val limit = if (isLarge) 8 else maxItems
        val tasks = (allTasks.filter { !it.done } + allTasks.filter { it.done })
            .take(limit)
            .map { it.done to it.text.uppercase() }
        if (tasks.isEmpty()) {
            DotMatrixRenderer.drawFittedLabel(canvas, "ADD TASKS IN THE APP", pad, spec.heightPx * 0.55f, maxW, Paint(paintLabel).apply { color = onColor })
            return
        }

        val startY = pad + (32f * spec.density)
        val availableH = spec.heightPx - startY - pad
        val lineSpacing = (availableH / tasks.size).coerceIn(if (isLarge) 24f else 20f, if (isLarge) 46f else 32f)
        val todoTextSize = (if (isLarge) 15f else 11f) * spec.density
        val todoTextCap = if (isLarge) 22f else 14f

        val paintDone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = todoTextSize.coerceAtMost(todoTextCap)
            isFakeBoldText = true
        }
        val paintPending = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = todoTextSize.coerceAtMost(todoTextCap)
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
        val steps = StepRepository.todaySteps(context)
            ?: return RenderKit.message(spec, canvas, "DAILY STEPS", "ALLOW ACTIVITY ACCESS IN THE APP")
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
        val stepStr = steps.toString()
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

    fun renderFocusTimer(context: Context, spec: WidgetSpec, canvas: Canvas) =
        renderTimer(spec, canvas, WidgetStateManager.getFocusTimerState(context), "POMODORO", "FOCUSING")

    /** Countdown Pill: same layout with its own timer and length. */
    fun renderCountdownTimer(context: Context, spec: WidgetSpec, canvas: Canvas) =
        renderTimer(spec, canvas, WidgetStateManager.getCountdownState(context), "COUNTDOWN", "RUNNING")

    private fun renderTimer(
        spec: WidgetSpec,
        canvas: Canvas,
        state: WidgetStateManager.FocusTimerState,
        title: String,
        runningLabel: String
    ) {
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

        val statusStr = when {
            state.isRunning -> runningLabel
            state.remainingSeconds == state.totalSeconds -> "TAP TO START"
            else -> "PAUSED"
        }
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (state.isRunning) accentColor else 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "$title • $statusStr", pad, pad + 12f * spec.density, maxW, paintLabel)

        val mins = state.remainingSeconds / 60
        val secs = state.remainingSeconds % 60
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

        val maxTextW = maxW * 0.85f
        val maxTextH = spec.heightPx * 0.40f
        val dotGap = DotMatrixRenderer.fitDotGap(timeStr.length, maxTextW, maxTextH, 10f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textW = DotMatrixRenderer.measureTextWidth(timeStr.length, dotGap)

        val startX = (spec.widthPx - textW) / 2f
        val startY = spec.heightPx * 0.35f

        // While running, a live countdown Chronometer drawn by the launcher shows the time.
        if (!state.isRunning || !spec.liveOverlays) {
            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = timeStr,
                startX = startX,
                startY = startY,
                dotRadius = dotRadius,
                dotGap = dotGap,
                onColor = onColor,
                offColor = offColor,
                accentIndices = setOf(2),
                accentColor = accentColor
            )
        }

        // Progress bar across bottom
        val totalDots = 20
        val elapsedFraction = 1f - (state.remainingSeconds.toFloat() / state.totalSeconds.coerceAtLeast(1))
        val filledDots = (elapsedFraction * totalDots).toInt().coerceIn(0, totalDots)
        val segGap = maxW / (totalDots - 1)
        val segY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = filledDots,
            max = totalDots,
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

}
