package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.render.ColorRule
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.activity.StepRepository
import com.example.data.health.HealthRepository
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import java.util.Locale

/**
 * Phase 7 Health & Fitness family. Steps come from the real hardware counter; the rest are
 * user-logged values in [HealthRepository]. All renderers degrade to a clean prompt when a metric
 * has not been logged yet rather than inventing data.
 */
object HealthRenderers {

    private fun requireSteps(context: Context, spec: WidgetSpec, canvas: Canvas, title: String): Int? {
        val steps = StepRepository.todaySteps(context)
        if (steps == null) {
            RenderKit.message(spec, canvas, title, "ALLOW ACTIVITY ACCESS IN THE APP")
        }
        return steps
    }

    // ---------------------------------------------------------------- steps ring

    fun renderStepsRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val steps = requireSteps(context, spec, canvas, "STEPS") ?: return
        val kit = RenderKit(spec, canvas)
        kit.background()
        val goal = spec.options.int("goal", 10000).coerceIn(1000, 50000)
        val ring = spec.options.string("style", "ring") == "ring"
        kit.header("DAILY STEPS")

        val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
        val ringColor = ColorRule.paint(progress, 1f, 0.99f, kit.accentColor, belowGoal = false)
        if (ring) {
            val cx = kit.width / 2f
            val cy = kit.height * 0.58f
            val radius = minOf(kit.width, kit.height) * 0.24f
            DotMatrixRenderer.drawDotRing(
                canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 24,
                filledDots = (progress * 24).toInt(), dotRadius = radius * 0.14f,
                onColor = kit.onColor, offColor = kit.offColor, accentColor = ringColor
            )
        }
        val pct = (progress * 100).toInt()
        kit.dotTextCentered("$pct%", kit.height * 0.56f, kit.height * 0.30f, 10f * kit.d)
        kit.labelCentered("$steps / $goal", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderStepsWeek(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val steps = requireSteps(context, spec, canvas, "STEPS THIS WEEK") ?: return
        val kit = RenderKit(spec, canvas)
        kit.background()
        val goal = spec.options.int("goal", 10000).coerceIn(1000, 50000)
        kit.header("7-DAY STEPS")
        // We only have today's real count; show it as today's column and leave the rest as goal markers.
        val cols = 7
        val colW = kit.maxW / cols
        val top = kit.pad + 24f * kit.d
        val h = kit.height - top - kit.pad
        for (i in 0 until cols) {
            val value = if (i == cols - 1) steps else 0
            val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
            val cx = kit.pad + i * colW + colW / 2f
            val filled = (frac * 10).toInt()
            for (dot in 0 until 10) {
                val on = dot < filled
                val y = top + h - dot * (h / 10f)
                val color = when {
                    !on -> kit.offColor
                    i == cols - 1 -> ColorRule.paint(frac, 1f, 0.99f, kit.accentColor, belowGoal = false)
                    else -> kit.onColor
                }
                canvas.drawCircle(cx, y, 2.4f * kit.d, kit.fill(color))
            }
        }
        if (spec.options.boolean("show_goal", true)) {
            val goalY = top
            val paint = kit.fill(kit.greyColor)
            canvas.drawRect(kit.pad, goalY - 0.6f * kit.d, kit.width - kit.pad, goalY + 0.6f * kit.d, paint)
        }
    }

    // ---------------------------------------------------------------- water dots

    fun renderWaterDots(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val glasses = PreferencesManager.getInstance(context).getWaterCount()
        val goal = spec.options.int("glasses_goal", 8).coerceIn(4, 16)
        kit.header("HYDRATION")
        val cols = if (goal > 8) 8 else goal
        val dotR = 3.0f * kit.d
        val cellW = kit.maxW / cols
        val top = kit.pad + 30f * kit.d
        for (i in 0 until goal) {
            val cx = kit.pad + (i % cols) * cellW + cellW / 2f
            val cy = top + (i / cols) * (dotR * 4f)
            val on = i < glasses
            canvas.drawCircle(cx, cy, dotR, kit.fill(if (on) kit.accentColor else kit.offColor))
        }
        kit.labelCentered("$glasses / $goal GLASSES", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    // ---------------------------------------------------------------- sleep

    fun renderSleepLast(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val minutes = repo.lastSleepMinutes()
        val goal = spec.options.int("goal_hours", 8) * 60
        kit.header("LAST NIGHT")
        if (minutes <= 0) {
            kit.labelCentered("LOG SLEEP IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val h = minutes / 60
        val m = minutes % 60
        kit.dotTextCentered(String.format(Locale.US, "%d:%02d", h, m), kit.height * 0.52f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(1))
        val sleepColor = ColorRule.paint(minutes, goal, 0.75f, kit.accentColor)
        if (spec.options.boolean("show_goal", true)) {
            kit.labelCentered("GOAL ${goal / 60}H", kit.height - kit.pad - 2f * kit.d, kit.textPaint(if (minutes < goal * 0.75f) sleepColor else kit.greyColor, 10f, 13f))
        }
    }

    fun renderSleepWeek(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val week = repo.sleepWeek()
        val goal = spec.options.int("goal_hours", 8) * 60
        kit.header("SLEEP • 7 NIGHTS")
        if (week.all { it <= 0 }) {
            kit.labelCentered("LOG SLEEP IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val maxMin = maxOf(goal, week.max())
        val colW = kit.maxW / 7
        val top = kit.pad + 24f * kit.d
        val h = kit.height - top - kit.pad - 12f * kit.d
        week.forEachIndexed { i, min ->
            val frac = (min.toFloat() / maxMin).coerceIn(0f, 1f)
            val cx = kit.pad + i * colW + colW / 2f
            val barH = frac * h
            val color = if (min < goal * 0.75f) ColorRule.WARN else kit.accentColor
            canvas.drawRoundRect(cx - colW * 0.22f, top + h - barH, cx + colW * 0.22f, top + h, 2f * kit.d, 2f * kit.d, kit.fill(color))
        }
        val avg = week.average().toInt()
        kit.labelCentered("AVG ${avg / 60}H ${avg % 60}M", kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderHeartRate(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val bpm = repo.restingHeartRate()
        kit.header(if (spec.options.boolean("resting", true)) "RESTING HR" else "HEART RATE")
        if (bpm <= 0) {
            kit.labelCentered("LOG HEART RATE IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        kit.dotTextCentered("$bpm", kit.height * 0.50f, kit.height * 0.40f, 12f * kit.d)
        // A short ECG-like dot trace to read as a pulse.
        val cy = kit.height * 0.72f
        val paint = kit.fill(kit.accentColor)
        val baseY = cy
        for (i in 0 until 24) {
            val x = kit.pad + i * (kit.maxW / 23f)
            val spike = when (i % 8) { 3 -> -6f; 4 -> 8f; else -> 0f }
            canvas.drawCircle(x, baseY + spike * kit.d, 1.8f * kit.d, paint)
        }
        kit.labelCentered("BPM", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    // ---------------------------------------------------------------- workout

    fun renderWorkoutStreak(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val streak = repo.workoutStreak()
        kit.header("WORKOUT STREAK")
        kit.dotTextCentered("$streak", kit.height * 0.48f, kit.height * 0.38f, 12f * kit.d,
            accentIndices = if (streak > 0) setOf(0) else emptySet())
        kit.labelCentered("DAY STREAK", kit.height * 0.78f, kit.textPaint(kit.greyColor, 9f, 12f))
        if (spec.options.boolean("show_week", true)) {
            val week = repo.workoutWeek()
            val g = kit.maxW / 7
            week.forEachIndexed { i, done ->
                val cx = kit.pad + i * g + g / 2f
                canvas.drawCircle(cx, kit.height - kit.pad - 4f * kit.d, 2.6f * kit.d, kit.fill(if (done) kit.accentColor else kit.offColor))
            }
        }
    }

    fun renderCaloriesRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val kcal = repo.caloriesToday()
        val goal = spec.options.int("goal", 600).coerceIn(100, 3000)
        kit.header("ACTIVE CALORIES")
        if (kcal <= 0) {
            kit.labelCentered("LOG CALORIES IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        val frac = (kcal.toFloat() / goal).coerceIn(0f, 1f)
        val cx = kit.width / 2f
        val cy = kit.height * 0.58f
        val radius = minOf(kit.width, kit.height) * 0.23f
        val color = ColorRule.paint(frac, 1f, 0.99f, kit.accentColor, belowGoal = false)
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 24,
            filledDots = (frac * 24).toInt(), dotRadius = radius * 0.14f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = color
        )
        kit.dotTextCentered("$kcal", kit.height * 0.56f, kit.height * 0.26f, 9f * kit.d)
        kit.labelCentered("OF $goal KCAL", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderWeightTrend(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val series = repo.weightSeries()
        val unit = spec.options.string("unit", repo.weightUnit()).uppercase()
        val goal = spec.options.int("goal_kg", repo.goalWeightKg())
        kit.header("WEIGHT TREND")
        if (series.size < 2) {
            kit.labelCentered("LOG WEIGHT IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val latest = series.last()
        kit.dotTextCentered(String.format(Locale.US, "%.1f", latest), kit.height * 0.34f, kit.height * 0.26f, 10f * kit.d,
            accentIndices = setOf(series.last().toString().length.coerceAtLeast(1)))
        // Sparkline of recent entries.
        val min = series.min(); val max = series.max()
        val range = (max - min).coerceAtLeast(0.1)
        val top = kit.height * 0.58f
        val h = kit.height - kit.pad - top
        val step = kit.maxW / (series.size - 1)
        series.forEachIndexed { i, v ->
            val y = top + h - ((v - min) / range * h).toFloat()
            val last = i == series.lastIndex
            canvas.drawCircle(kit.pad + i * step, y, if (last) 3.2f * kit.d else 2.0f * kit.d, kit.fill(if (last) kit.accentColor else kit.onColor))
        }
        kit.labelCentered("$unit · GOAL $goal", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderMeditation(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val min = repo.meditationMinutes()
        val goal = spec.options.int("goal", 10).coerceIn(1, 120)
        kit.header("MINDFULNESS")
        if (min <= 0 && !spec.options.boolean("show_zero", false)) {
            kit.labelCentered("LOG MINDFUL MINUTES IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        kit.dotTextCentered("$min", kit.height * 0.50f, kit.height * 0.36f, 11f * kit.d)
        kit.labelCentered("$min OF $goal MIN", kit.height * 0.78f, kit.textPaint(kit.greyColor, 9f, 12f))
        val frac = (min.toFloat() / goal).coerceIn(0f, 1f)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = min, max = goal, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 4f * kit.d,
            dotRadius = 2.4f * kit.d, dotGap = kit.maxW / 19f,
            onColor = kit.onColor, offColor = kit.offColor,
            accentColor = ColorRule.paint(frac, 1f, 0.99f, kit.accentColor, belowGoal = false), useAccentOnActive = true
        )
    }

    fun renderStandHours(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val hours = repo.standHours()
        val goal = spec.options.int("goal", 12).coerceIn(1, 24)
        kit.header("STAND HOURS")
        if (hours <= 0) {
            kit.labelCentered("LOG STAND HOURS IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        // A 24-segment radial of the day; filled up to the logged hours.
        val cx = kit.width / 2f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width, kit.height) * 0.24f
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = goal.coerceAtMost(24),
            filledDots = hours.coerceAtMost(goal), dotRadius = radius * 0.14f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor
        )
        kit.dotTextCentered("$hours", kit.height * 0.54f, kit.height * 0.24f, 9f * kit.d)
        kit.labelCentered("OF $goal HOURS", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderBodyBattery(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = HealthRepository.getInstance(context)
        val level = repo.bodyBattery()
        val preset = spec.options.string("formula", "BALANCED")
        kit.header("BODY BATTERY · $preset")
        // Vertical fuel-gauge of 10 segments.
        val segs = 10
        val barH = (kit.height - kit.pad * 2 - 24f * kit.d) / segs
        val filled = (level * segs / 100)
        val color = ColorRule.paint(level, 100, 0.2f, kit.accentColor)
        for (i in 0 until segs) {
            val on = i < filled
            val top = kit.height - kit.pad - (i + 1) * barH + 1.5f * kit.d
            canvas.drawRect(kit.pad, top, kit.pad + kit.maxW * 0.28f, top + barH - 3f * kit.d, kit.fill(if (on) color else kit.offColor))
        }
        val paint = kit.textPaint(color, 14f, 20f)
        val str = "$level"
        canvas.drawText(str, kit.pad + kit.maxW * 0.36f, kit.height * 0.58f, paint)
        kit.labelCentered("CHARGE", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }
}
