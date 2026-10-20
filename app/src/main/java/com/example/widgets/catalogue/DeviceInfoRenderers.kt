package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.battery.BatteryHelper
import com.example.widgets.base.WidgetSpec
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/** Widgets backed by live device readings: battery health, charging, volume, plus the ambient pattern. */
object DeviceInfoRenderers {

    /** Battery health, temperature, voltage and charge cycles. */
    fun renderBatteryHealth(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val status = BatteryHelper.getBatteryStatus(context)

        val healthy = status.health == "GOOD"
        kit.header("BATTERY HEALTH", if (healthy) kit.greyColor else kit.accentColor)

        val temp = "${status.temperatureC.toInt()}C"
        val gap = DotMatrixRenderer.fitDotGap(temp.length, kit.maxW * 0.45f, kit.height * 0.36f, 10f * d)
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = temp,
            startX = kit.pad,
            startY = kit.height * 0.32f,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = kit.onColor,
            offColor = kit.offColor,
            accentIndices = setOf(temp.length - 1),
            accentColor = kit.accentColor
        )

        val rightX = kit.width * 0.55f
        val rightW = kit.width - kit.pad - rightX
        val rows = listOf(
            "HEALTH" to status.health,
            "VOLTAGE" to if (status.voltageMv > 0) String.format(java.util.Locale.US, "%.2fV", status.voltageMv / 1000f) else "--",
            "CYCLES" to if (status.cycleCount >= 0) status.cycleCount.toString() else "--",
            "LEVEL" to "${status.level}%"
        )
        val rowH = (kit.height * 0.58f) / rows.size
        rows.forEachIndexed { i, (label, value) ->
            val y = kit.height * 0.32f + i * rowH
            DotMatrixRenderer.drawFittedLabel(canvas, label, rightX, y, rightW * 0.5f, kit.textPaint(kit.greyColor, 9.5f, 12f))
            val paintValue = kit.textPaint(if (label == "HEALTH" && !healthy) kit.accentColor else kit.onColor, 10.5f, 13f)
            canvas.drawText(value, kit.width - kit.pad - paintValue.measureText(value), y, paintValue)
        }

        val tempState = when {
            status.temperatureC >= 45 -> "HOT"
            status.temperatureC >= 38 -> "WARM"
            status.temperatureC < 5 -> "COLD"
            else -> "NORMAL TEMP"
        }
        DotMatrixRenderer.drawFittedLabel(canvas, tempState, kit.pad, kit.height - kit.pad, kit.maxW * 0.45f, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    /** Charging state, charger type and time until full. */
    fun renderChargingIndicator(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val status = BatteryHelper.getBatteryStatus(context)

        kit.header(if (status.isCharging) "CHARGING • ${status.plugType}" else "NOT CHARGING", if (status.isCharging) kit.accentColor else kit.greyColor)

        val level = "${status.level}%"
        kit.dotTextCentered(level, kit.height * 0.45f, kit.height * 0.32f, 10f * d, kit.maxW * 0.7f, setOf(level.length - 1))

        // 10-dot charge bar; the next dot to fill pulses in the accent colour while charging
        val dots = 10
        val filled = (status.level + 5) / 10
        val gap = kit.maxW / (dots - 1)
        val y = kit.height * 0.72f
        for (i in 0 until dots) {
            val paint = when {
                i < filled -> kit.fill(kit.onColor)
                i == filled && status.isCharging -> kit.fill(kit.accentColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(kit.pad + i * gap, y, if (i < filled) 3.2f * d else 2.4f * d, paint)
        }

        val caption = when {
            !status.isCharging -> "PLUG IN TO CHARGE"
            status.level >= 100 -> "FULLY CHARGED"
            status.chargeTimeRemainingMs > 0 -> {
                val min = (status.chargeTimeRemainingMs / 60_000).toInt()
                if (min >= 60) "FULL IN ${min / 60}H ${min % 60}M" else "FULL IN ${min}M"
            }
            else -> "CHARGING"
        }
        kit.labelCentered(caption, kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    /** Media, ring and alarm volume as dot rows. */
    fun renderVolumeLevels(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("VOLUME")

        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val streams = listOf(
            "MEDIA" to AudioManager.STREAM_MUSIC,
            "RING" to AudioManager.STREAM_RING,
            "ALARM" to AudioManager.STREAM_ALARM
        )
        val labelW = 52f * d
        val dots = 15
        val gap = (kit.maxW - labelW - 34f * d) / (dots - 1)
        val rowH = (kit.height - kit.pad * 2 - 24f * d) / streams.size
        streams.forEachIndexed { i, (label, stream) ->
            val cur = am?.getStreamVolume(stream) ?: 0
            val max = (am?.getStreamMaxVolume(stream) ?: 1).coerceAtLeast(1)
            val y = kit.pad + 30f * d + rowH * (i + 0.5f)
            DotMatrixRenderer.drawFittedLabel(canvas, label, kit.pad, y + 4f * d, labelW, kit.textPaint(kit.greyColor, 10f, 13f))
            DotMatrixRenderer.drawDotRun(
                canvas = canvas,
                current = cur,
                max = max,
                totalDots = dots,
                startX = kit.pad + labelW,
                startY = y,
                dotRadius = 2.8f * d,
                dotGap = gap,
                onColor = kit.onColor,
                offColor = kit.offColor,
                accentColor = kit.accentColor,
                useAccentOnActive = true
            )
            val pct = "${cur * 100 / max}%"
            val paintPct = kit.textPaint(kit.onColor, 10f, 13f)
            canvas.drawText(pct, kit.width - kit.pad - paintPct.measureText(pct), y + 4f * d, paintPct)
        }
    }

    /** Nothing-style segmented arc ring showing the battery level; accent while charging. */
    fun renderGlyphBattery(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val status = BatteryHelper.getBatteryStatus(context)
        kit.header(if (status.isCharging) "GLYPH • CHARGING" else "GLYPH BATTERY", if (status.isCharging) kit.accentColor else kit.greyColor)

        val cx = kit.width / 2f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width, kit.height) * 0.32f
        val stroke = radius * 0.16f
        val segments = 3
        val gapDeg = 14f
        val segSweep = (360f - segments * gapDeg) / segments
        val litSweep = 360f * status.level / 100f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until segments) {
            val start = -90f + gapDeg / 2 + i * (segSweep + gapDeg)
            val segStartOnRing = i * (segSweep + gapDeg)
            paint.color = kit.offColor
            canvas.drawArc(rect, start, segSweep, false, paint)
            val lit = (litSweep - segStartOnRing).coerceIn(0f, segSweep)
            if (lit > 0f) {
                paint.color = if (status.isCharging) kit.accentColor else kit.onColor
                canvas.drawArc(rect, start, lit, false, paint)
            }
        }
        val pct = "${status.level}"
        kit.dotTextCentered(pct, cy, radius * 0.7f, 6f * d, radius * 1.2f)
    }

    /** Generative ripple pattern that drifts every minute; purely decorative. */
    fun renderAmbientPattern(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header("AMBIENT")
        // On the home screen the moving ripple is drawn by the flipbook overlay (AnimatedOverlays).
        if (spec.liveOverlays) return

        val cal = Calendar.getInstance()
        val phase = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) / 60.0 * 2 * PI
        val cols = 18
        val gap = kit.maxW / (cols - 1)
        val top = kit.pad + 26f * kit.d
        val rows = ((kit.height - top - kit.pad) / gap).toInt() + 1
        val cx = kit.width * (0.5f + 0.25f * sin(phase).toFloat())
        val cy = top + (rows - 1) * gap * 0.5f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = kit.pad + c * gap
                val y = top + r * gap
                val dist = hypot((x - cx).toDouble(), (y - cy).toDouble()) / gap
                val wave = (sin(dist * 0.9 - phase * 4) + 1) / 2 // 0..1
                val radius = gap * (0.10f + 0.28f * wave.toFloat())
                val paint = if (wave > 0.93) kit.fill(kit.accentColor) else kit.fill(if (wave > 0.5) kit.onColor else kit.offColor)
                canvas.drawCircle(x, y, radius, paint)
            }
        }
    }
}
