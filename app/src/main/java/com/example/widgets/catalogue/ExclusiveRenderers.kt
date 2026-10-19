package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.core.time.ClockOptions
import com.example.data.battery.BatteryHelper
import com.example.widgets.base.WidgetSpec
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The "only in Dot Widgets" set: designs no rival ships. Each reuses [RenderKit] / [DotMatrixRenderer]
 * so it inherits the theme, accent and transparency handling for free.
 */
object ExclusiveRenderers {

    /**
     * Glyph-reactive clock. A dot-matrix time with a segmented accent ring that pulses a little
     * brighter while charging, echoing Nothing's glyph lights. The ring segmests are static between
     * redraws (widgets update about once a minute); the pulse is strongest on the currently-lit
     * segment.
     */
    fun renderGlyphClock(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val status = BatteryHelper.getBatteryStatus(context)
        val charging = status.isCharging
        kit.header(if (charging) "GLYPH • CHARGING" else "GLYPH CLOCK", if (charging) kit.accentColor else kit.greyColor)

        val cal = Calendar.getInstance()
        val opts = ClockOptions.from(spec.configJson)
        val timeStr = opts.time.format(cal)

        val cx = kit.width / 2f
        val cy = kit.height * 0.52f
        val radius = min(kit.width, kit.height) * 0.36f

        val segments = 12
        val gapDeg = 10f
        val segSweep = (360f - segments * gapDeg) / segments
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = radius * 0.11f
            strokeCap = Paint.Cap.ROUND
        }
        val minute = cal.get(Calendar.MINUTE)
        val litCount = (minute * segments / 60f).toInt()
        for (i in 0 until segments) {
            val start = -90f + gapDeg / 2 + i * (segSweep + gapDeg)
            val lit = i <= litCount
            paint.color = when {
                !lit -> kit.offColor
                charging && i == litCount -> kit.accentColor
                lit -> kit.onColor
                else -> kit.offColor
            }
            canvas.drawArc(rect, start, segSweep, false, paint)
        }

        kit.dotTextCentered(timeStr, cy, radius * 0.72f, 6f * d, radius * 1.5f, setOf(timeStr.length - 1))
    }

    /**
     * "Life in dots": one dot per 10 minutes of the day (144 dots), filled as the day passes. A
     * whole day's passing reads at a glance.
     */
    fun renderLifeInDots(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val cal = Calendar.getInstance()
        val minutesToday = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val totalDots = 144
        val filled = (minutesToday * totalDots / 1440)

        kit.header("LIFE IN DOTS • ${filled * 10} MIN", kit.greyColor, 0.95f)

        val cols = 24
        val rows = 6
        val top = kit.pad + 26f * d
        val cellW = kit.maxW / cols
        val cellH = (kit.height - top - kit.pad) / rows
        val r = min(cellW, cellH) * 0.34f
        for (i in 0 until totalDots) {
            val col = i % cols
            val row = i / cols
            val x = kit.pad + col * cellW + cellW / 2f
            val y = top + row * cellH + cellH / 2f
            val paint = when {
                i < filled -> kit.fill(if (i == filled - 1) kit.accentColor else kit.onColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(x, y, if (i < filled) r else r * 0.75f, paint)
        }

        val pct = minutesToday * 100 / 1440
        kit.labelCentered("$pct% OF THE DAY", kit.height - kit.pad, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    /** Battery as a horizontal dot fuel gauge with time-to-full / time-to-empty from BatteryManager. */
    fun renderBatteryFuelGauge(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val status = BatteryHelper.getBatteryStatus(context)
        kit.header(if (status.isCharging) "FUEL • CHARGING" else "FUEL GAUGE", if (status.isCharging) kit.accentColor else kit.greyColor)

        val level = "${status.level}%"
        kit.dotTextCentered(level, kit.height * 0.42f, kit.height * 0.34f, 10f * d, kit.maxW * 0.7f, setOf(level.length - 1))

        val dots = 20
        val filled = (status.level * dots + 50) / 100
        val gap = kit.maxW / (dots - 1)
        val y = kit.height * 0.68f
        for (i in 0 until dots) {
            val paint = when {
                i < filled -> kit.fill(kit.onColor)
                i == filled && status.isCharging -> kit.fill(kit.accentColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(kit.pad + i * gap, y, if (i < filled) 3.4f * d else 2.6f * d, paint)
        }

        val eta = when {
            status.level >= 100 -> "FULL"
            status.isCharging && status.chargeTimeRemainingMs > 0 -> {
                val min = (status.chargeTimeRemainingMs / 60_000).toInt()
                if (min >= 60) "${min / 60}H ${min % 60}M TO FULL" else "${min}M TO FULL"
            }
            status.isCharging -> "CHARGING"
            else -> "~${(status.level / 1.4f).toInt()}H LEFT"
        }
        kit.labelCentered(eta, kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    /**
     * Week-in-dots: a 7-column heatmap, one column per weekday and one row per 3-hour block, filled
     * by how much of the current week has elapsed. Doubles as a habit/screen-time matrix.
     */
    fun renderWeekInDots(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val mondayIndex = (dayOfWeek + 5) % 7
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        kit.header("WEEK IN DOTS", kit.greyColor)

        val cols = 7
        val rows = 8
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        val top = kit.pad + 30f * d
        val cellW = kit.maxW / cols
        val cellH = (kit.height - top - kit.pad) / rows
        val r = min(cellW, cellH) * 0.32f
        val paintLabel = kit.greyColor
        for (c in 0 until cols) {
            val x = kit.pad + c * cellW + cellW / 2f
            val txt = labels[c]
            val p = kit.textPaint(if (c == mondayIndex) kit.accentColor else paintLabel, 8f, 11f)
            canvas.drawText(txt, x - p.measureText(txt) / 2f, top - 6f * d, p)
        }
        val elapsedBlocks = mondayIndex * rows + hourOfDay / 3
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Dot index runs top-to-bottom within each weekday column, so the fill advances a
                // full column a day.
                val here = col * rows + row
                val x = kit.pad + col * cellW + cellW / 2f
                val y = top + row * cellH + cellH / 2f
                val paint = when {
                    here < elapsedBlocks -> kit.fill(kit.onColor)
                    here == elapsedBlocks -> kit.fill(kit.accentColor)
                    else -> kit.fill(kit.offColor)
                }
                canvas.drawCircle(x, y, r, paint)
            }
        }
    }

    /**
     * Focus companion: a dot ring that fills with the focus-timer progress plus a tiny silent-streak
     * row. Reuses the same persisted focus-timer state as the standard focus widget.
     */
    fun renderFocusCompanion(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val state = com.example.widgets.base.WidgetStateManager.getFocusTimerState(context)
        val running = state.isRunning
        kit.header(if (running) "FOCUS • RUNNING" else "FOCUS COMPANION", if (running) kit.accentColor else kit.greyColor)

        val total = if (state.totalSeconds > 0) state.totalSeconds else 25 * 60
        val remaining = state.remainingSeconds.coerceIn(0, total)
        val progress = if (total > 0) (total - remaining).toFloat() / total else 0f

        val cx = kit.width / 2f
        val cy = kit.height * 0.5f
        val radius = min(kit.width, kit.height) * 0.3f
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            totalDots = 40,
            filledDots = (progress * 40).toInt(),
            dotRadius = radius * 0.09f,
            onColor = kit.onColor,
            offColor = kit.offColor,
            accentColor = kit.accentColor
        )

        val min = remaining / 60
        val timeStr = if (min >= 10) "$min" else "0$min"
        kit.dotTextCentered(timeStr, cy, radius * 0.66f, 6f * d, radius * 1.2f)
        kit.labelCentered(if (running) "MIN LEFT" else "TAP TO START", cy + radius * 1.35f, kit.textPaint(kit.greyColor, 9f, 12f))

        val streak = com.example.widgets.base.WidgetStateManager.getFocusStreak(context)
        val streakY = kit.height - kit.pad
        val dotsShown = 7
        val sGap = (kit.maxW * 0.5f) / (dotsShown - 1)
        val startX = kit.width / 2f - ((dotsShown - 1) * sGap) / 2f
        for (i in 0 until dotsShown) {
            val on = i < streak.coerceIn(0, dotsShown)
            canvas.drawCircle(startX + i * sGap, streakY - 4f * d, if (on) 2.6f * d else 2.0f * d, kit.fill(if (on) kit.accentColor else kit.offColor))
        }
    }

    /** Rotating compass-free "orbit" ambient: concentric rings of dots drifting slowly. Purely decorative. */
    fun renderOrbitAmbient(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header("ORBIT")

        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.MINUTE) + cal.get(Calendar.HOUR_OF_DAY) * 60
        val cxx = kit.width / 2f
        val cyy = kit.height * 0.56f
        val maxR = min(kit.width, kit.height) * 0.42f

        for (ring in 1..4) {
            val radius = maxR * ring / 4f
            val dots = 8 + ring * 6
            val offset = Math.toRadians((minute * (ring + 1) * 3.0) % 360)
            for (i in 0 until dots) {
                val ang = offset + i * 2 * Math.PI / dots
                val x = cxx + (radius * cos(ang)).toFloat()
                val y = cyy + (radius * sin(ang)).toFloat()
                val lead = i == 0
                canvas.drawCircle(x, y, if (lead) 3.0f * kit.d else 2.0f * kit.d, kit.fill(if (lead) kit.accentColor else kit.onColor))
            }
        }
    }
}
