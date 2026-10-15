package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ClockRenderers {

    fun renderDigitalClock(spec: WidgetSpec, canvas: Canvas, showSeconds: Boolean = false, timeZoneId: String? = null) {
        val cal = Calendar.getInstance()
        if (!timeZoneId.isNullOrBlank()) {
            cal.timeZone = TimeZone.getTimeZone(timeZoneId)
        }
        val is24h = true
        val hours = if (is24h) cal.get(Calendar.HOUR_OF_DAY) else {
            val h = cal.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val timeStr = if (showSeconds) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", hours, minutes)
        }

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val padding = spec.widthPx * 0.08f
        val maxAllowedW = spec.widthPx - (2 * padding)

        // Draw background
        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        // Draw top label if enabled
        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            val zoneLabel = if (!timeZoneId.isNullOrBlank()) {
                timeZoneId.substringAfterLast("/").replace("_", " ").uppercase()
            } else "TIME"
            DotMatrixRenderer.drawFittedLabel(canvas, zoneLabel, padding, padding + 12f * spec.density, maxAllowedW * 0.75f, paintLabel)
        }

        // Calculate dot dimensions dynamically so text never clips or overflows
        val maxAllowedH = spec.heightPx * (if (spec.sizeBucket == SizeBucket.S) 0.52f else 0.44f)
        val preferredGap = when (spec.sizeBucket) {
            SizeBucket.S -> 8.5f * spec.density
            SizeBucket.M -> 12.0f * spec.density
            SizeBucket.L -> 15.0f * spec.density
        }
        val dotGap = DotMatrixRenderer.fitDotGap(timeStr.length, maxAllowedW, maxAllowedH, preferredGap)
        val dotRadius = dotGap * 0.38f

        val textWidth = DotMatrixRenderer.measureTextWidth(timeStr.length, dotGap)
        val textHeight = 7 * dotGap

        val startX = (spec.widthPx - textWidth) / 2f
        val centerY = if (spec.sizeBucket != SizeBucket.S) spec.heightPx * 0.42f else spec.heightPx * 0.52f
        val startY = centerY - (textHeight / 2f) + (if (spec.showLabel) 6f * spec.density else 0f)

        // Accent colon ':' indices
        val colonIndices = mutableSetOf<Int>()
        timeStr.forEachIndexed { i, c -> if (c == ':') colonIndices.add(i) }

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = timeStr,
            startX = startX,
            startY = startY,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = colonIndices,
            accentColor = accentColor
        )

        // On M and L sizes, also display date below
        if (spec.sizeBucket != SizeBucket.S) {
            val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
                if (!timeZoneId.isNullOrBlank()) timeZone = TimeZone.getTimeZone(timeZoneId)
            }
            val dateStr = dateFormat.format(cal.time).uppercase()
            val paintDate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (12f * spec.density).coerceAtMost(16f)
                isFakeBoldText = true
            }
            val dateWidth = paintDate.measureText(dateStr)
            val dateX = (spec.widthPx - dateWidth) / 2f
            DotMatrixRenderer.drawFittedLabel(canvas, dateStr, dateX.coerceAtLeast(padding), startY + textHeight + (18f * spec.density), maxAllowedW, paintDate)
        }
    }

    fun renderAnalogRing(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val padding = spec.widthPx * 0.08f

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val cx = spec.widthPx / 2f
        val cy = spec.heightPx / 2f + (if (spec.showLabel) 6f * spec.density else 0f)
        val maxAvailableD = minOf(spec.widthPx - (2 * padding), spec.heightPx - (2 * padding) - 12f * spec.density)
        val radius = (maxAvailableD / 2f) * 0.86f

        DotMatrixRenderer.drawAnalogClock(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            accentColor = accentColor,
            onColor = onColor,
            offColor = offColor
        )

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            DotMatrixRenderer.drawFittedLabel(canvas, "ANALOG", padding, padding + 12f * spec.density, spec.widthPx * 0.6f, paintLabel)
        }
    }

    fun renderStackedTime(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val hStr = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val mStr = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val padding = spec.widthPx * 0.08f
        val maxAllowedW = spec.widthPx - (2 * padding)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val maxAllowedH = (spec.heightPx - (2 * padding) - 20f * spec.density) / 2f
        val dotGap = DotMatrixRenderer.fitDotGap(2, maxAllowedW * 0.65f, maxAllowedH, 9.5f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textWidth = DotMatrixRenderer.measureTextWidth(2, dotGap)

        val startX = (spec.widthPx - textWidth) / 2f
        val spacing = 6f * spec.density
        val blockH = 7 * dotGap
        val totalH = (2 * blockH) + spacing

        val startY1 = ((spec.heightPx - totalH) / 2f) + (if (spec.showLabel) 8f * spec.density else 0f)
        val startY2 = startY1 + blockH + spacing

        // Hours in primary onColor
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = hStr,
            startX = startX,
            startY = startY1,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor
        )

        // Minutes with accent highlight
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = mStr,
            startX = startX,
            startY = startY2,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(0, 1),
            accentColor = accentColor
        )

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            DotMatrixRenderer.drawFittedLabel(canvas, "STACKED", padding, padding + 12f * spec.density, maxAllowedW * 0.6f, paintLabel)
        }
    }

    fun renderBinaryClock(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)

        val digits = intArrayOf(h / 10, h % 10, m / 10, m % 10, s / 10, s % 10)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val padding = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * padding)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        val cols = 6
        val colGap = maxW / (cols - 1)
        val rowGap = ((spec.heightPx - (2 * padding) - 24f * spec.density) / 4f).coerceIn(12f, 22f)
        val startY = spec.heightPx / 2f - (1.5f * rowGap) + (if (spec.showLabel) 6f * spec.density else 0f)

        for (c in 0 until cols) {
            val cx = padding + (c * colGap)
            val value = digits[c]

            for (r in 0 until 4) {
                val bitWeight = 1 shl (3 - r)
                val isBitOn = (value and bitWeight) != 0
                val cy = startY + (r * rowGap)

                val paint = if (isBitOn) {
                    if (c >= 4) paintAccent else paintOn
                } else paintOff

                canvas.drawCircle(cx, cy, 5.0f * spec.density, paint)
            }
        }

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            DotMatrixRenderer.drawFittedLabel(canvas, "BINARY BCD", padding, padding + 12f * spec.density, maxW * 0.6f, paintLabel)
        }
    }

    fun renderSecondsBarClock(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)

        val timeStr = String.format(Locale.US, "%02d:%02d", hours, minutes)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val padding = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * padding)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val maxH = spec.heightPx * 0.38f
        val dotGap = DotMatrixRenderer.fitDotGap(timeStr.length, maxW, maxH, 10f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textWidth = DotMatrixRenderer.measureTextWidth(timeStr.length, dotGap)

        val startX = (spec.widthPx - textWidth) / 2f
        val startY = spec.heightPx * 0.30f

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

        // 30 dots representing 60 seconds (2 sec per dot)
        val totalSecDots = 30
        val secDotGap = maxW / (totalSecDots - 1)
        val secStartY = spec.heightPx - padding - (8f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = seconds,
            max = 60,
            totalDots = totalSecDots,
            startX = padding,
            startY = secStartY,
            dotRadius = 2.5f * spec.density,
            dotGap = secDotGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            DotMatrixRenderer.drawFittedLabel(canvas, "PRECISION SECONDS", padding, padding + 12f * spec.density, maxW * 0.65f, paintLabel)
        }
    }
}
