package com.example.widgets.catalogue

import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.render.DotMatrixRenderer
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarRenderers {

    fun renderDayDateCard(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val dayNumber = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(cal.time).uppercase()
        val monthYear = SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time).uppercase()

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

        // Weekday label top left
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, dayOfWeek, pad, pad + 12f * spec.density, maxW * 0.65f, paintSub)

        // Large dot matrix day number
        val maxAllowedH = spec.heightPx * 0.44f
        val dotGap = DotMatrixRenderer.fitDotGap(dayNumber.length, maxW * 0.65f, maxAllowedH, 11f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textWidth = DotMatrixRenderer.measureTextWidth(dayNumber.length, dotGap)

        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = dayNumber,
            startX = startX,
            startY = startY,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor
        )

        // Month and Year line bottom
        val paintMonth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (12f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val monthY = spec.heightPx - pad - (8f * spec.density)
        DotMatrixRenderer.drawFittedLabel(canvas, monthYear, pad, monthY, maxW, paintMonth)
    }

    fun renderMonthGrid(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // First day of week
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0..6

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time).uppercase()

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

        // Header label
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, monthName, pad, pad + 12f * spec.density, maxW * 0.70f, paintHeader)

        val cols = 7
        val colGap = maxW / (cols - 1)
        val gridStartY = pad + (32f * spec.density)
        val maxGridH = spec.heightPx - gridStartY - pad - (6f * spec.density)
        val rowGap = (maxGridH / 5.5f).coerceIn(12f, 22f)

        // Weekday headers S M T W T F S
        val headers = listOf("S", "M", "T", "W", "T", "F", "S")
        val paintCol = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (9f * spec.density).coerceAtMost(11f)
            isFakeBoldText = true
        }
        for (c in 0 until cols) {
            val hx = pad + (c * colGap)
            val hw = paintCol.measureText(headers[c])
            canvas.drawText(headers[c], hx - (hw / 2f), gridStartY, paintCol)
        }

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        // Draw day dots
        for (day in 1..35) {
            val cellIndex = day - 1
            val dayNumber = cellIndex - firstDayOfWeek + 1
            val col = cellIndex % 7
            val row = cellIndex / 7

            val cx = pad + (col * colGap)
            val cy = gridStartY + (14f * spec.density) + (row * rowGap)

            if (dayNumber in 1..maxDays) {
                if (dayNumber == currentDay) {
                    canvas.drawCircle(cx, cy, 5.0f * spec.density, paintAccent)
                } else if (dayNumber < currentDay) {
                    canvas.drawCircle(cx, cy, 3.2f * spec.density, paintOn)
                } else {
                    canvas.drawCircle(cx, cy, 2.2f * spec.density, paintOff)
                }
            } else {
                canvas.drawCircle(cx, cy, 1.4f * spec.density, paintOff)
            }
        }
    }

    fun renderWeekProgress(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val dayOfWeekIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0, Sunday = 6

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
        DotMatrixRenderer.drawFittedLabel(canvas, "WEEK PROGRESS", pad, pad + 12f * spec.density, maxW, paintLabel)

        val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
        val stepX = maxW / 6f
        val cy = spec.heightPx * 0.52f

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        for (i in 0..6) {
            val cx = pad + (i * stepX)
            val isToday = i == dayOfWeekIndex
            val isPast = i < dayOfWeekIndex

            val paint = when {
                isToday -> paintAccent
                isPast -> paintOn
                else -> paintOff
            }

            canvas.drawCircle(cx, cy, if (isToday) 8.5f * spec.density else 5.5f * spec.density, paint)

            val paintDay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isToday) accentColor else 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(13f)
                isFakeBoldText = isToday
            }
            val dw = paintDay.measureText(dayNames[i])
            canvas.drawText(dayNames[i], cx - (dw / 2f), cy + (20f * spec.density), paintDay)
        }
    }

    fun renderAgendaEvents(spec: WidgetSpec, canvas: Canvas, maxEvents: Int = 3) {
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
        DotMatrixRenderer.drawFittedLabel(canvas, "TODAY AGENDA", pad, pad + 12f * spec.density, maxW, paintLabel)

        val events = listOf(
            "10:00" to "DESIGN REVIEW",
            "13:30" to "PROJECT SYNC",
            "16:00" to "HARDWARE LAB",
            "18:30" to "GYM WORKOUT",
            "20:00" to "DINNER WITH EMMA"
        ).take(maxEvents)

        val startY = pad + (32f * spec.density)
        val availableH = spec.heightPx - startY - pad
        val lineSpacing = (availableH / events.size).coerceIn(20f, 32f)

        val paintTime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }

        events.forEachIndexed { i, (time, title) ->
            val y = startY + (i * lineSpacing) + (14f * spec.density)
            // Dot marker
            canvas.drawCircle(pad + 4f * spec.density, y - 4f * spec.density, 3.0f * spec.density, if (i == 0) paintTime else paintTitle)
            // Time
            canvas.drawText(time, pad + 14f * spec.density, y, paintTime)
            // Title fitted with remaining width
            val titleStartX = pad + (58f * spec.density)
            val titleMaxW = spec.widthPx - pad - titleStartX
            DotMatrixRenderer.drawFittedLabel(canvas, title, titleStartX, y, titleMaxW, paintTitle)
        }
    }
}
