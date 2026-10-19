package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.calendar.CalendarRepository
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Calendar widgets with their own data: agenda, timeline, ISO week, event countdown, split card and habit streak. */
object CalendarExtraRenderers {

    private val timeFmt get() = SimpleDateFormat("HH:mm", Locale.US)

    /** Today's remaining events from the phone's calendar. */
    fun renderAgenda(context: Context, spec: WidgetSpec, canvas: Canvas, maxEvents: Int) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val events = CalendarRepository.upcomingToday(context)
        val title = SimpleDateFormat("EEE dd MMM", Locale.US).format(Date()).uppercase()
        kit.header(if (events.isNullOrEmpty()) "TODAY • $title" else "TODAY • $title • ${events.size} LEFT", widthFraction = 0.95f)

        val message = when {
            events == null -> "ALLOW CALENDAR ACCESS IN THE APP"
            events.isEmpty() -> "NO MORE EVENTS TODAY"
            else -> null
        }
        if (message != null) {
            kit.labelCentered(message, kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }

        val shown = events!!.take(maxEvents)
        val startY = kit.pad + 34f * d
        val lineSpacing = ((kit.height - startY - kit.pad) / maxEvents).coerceAtMost(30f * d)
        val paintTime = kit.textPaint(kit.accentColor, 11f, 15f)
        val paintTitle = kit.textPaint(kit.onColor, 11f, 15f)
        val now = System.currentTimeMillis()
        val timeW = paintTime.measureText("00:00") + 12f * d

        shown.forEachIndexed { i, event ->
            val y = startY + i * lineSpacing + 10f * d
            val happening = !event.allDay && event.beginMs <= now
            canvas.drawCircle(kit.pad + 4f * d, y - 4f * d, 3f * d, kit.fill(if (happening) kit.accentColor else (event.color or 0xFF000000.toInt())))
            val time = when {
                event.allDay -> "ALL DAY"
                happening -> "NOW"
                else -> timeFmt.format(Date(event.beginMs))
            }
            DotMatrixRenderer.drawFittedLabel(canvas, time, kit.pad + 14f * d, y, timeW, paintTime)
            val titleX = kit.pad + 14f * d + timeW
            DotMatrixRenderer.drawFittedLabel(canvas, event.title.uppercase(), titleX, y, kit.width - kit.pad - titleX, paintTitle)
        }
        val isLarge = spec.sizeBucket == SizeBucket.L
        if (events.size > shown.size) {
            val more = "+${events.size - shown.size} MORE"
            val paintMore = kit.textPaint(kit.greyColor, 9.5f, 12f)
            val moreY = if (isLarge) kit.height - kit.pad * 2.6f else kit.height - kit.pad * 0.6f
            canvas.drawText(more, kit.width - kit.pad - paintMore.measureText(more), moreY, paintMore)
        }

        // 4×4 adds a full 24-hour strip so the taller canvas still reads as "today".
        if (isLarge) {
            val timed = events.filter { !it.allDay }
            val dayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val nowForStrip = System.currentTimeMillis()
            val nowHour = ((nowForStrip - dayStart) / 3_600_000L).toInt()
            val cols = 24
            val gap = kit.maxW / (cols - 1)
            val y = kit.height - kit.pad * 0.9f
            for (h in 0 until cols) {
                val x = kit.pad + h * gap
                val hourStart = dayStart + h * 3_600_000L
                val busy = timed.any { it.beginMs < hourStart + 3_600_000L && it.endMs > hourStart }
                val paint = when {
                    h == nowHour -> kit.fill(kit.accentColor)
                    busy -> kit.fill(kit.onColor)
                    else -> kit.fill(kit.offColor)
                }
                canvas.drawCircle(x, y, if (busy || h == nowHour) gap * 0.42f else gap * 0.26f, paint)
            }
        }
    }

    /** Today as a 24-hour dot timeline with events marked, plus the next event below. */
    fun renderTimeline(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("TIMELINE")

        val events = CalendarRepository.today(context)
        if (events == null) {
            kit.labelCentered("ALLOW CALENDAR ACCESS IN THE APP", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }

        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val nowHour = ((now - dayStart) / 3_600_000L).toInt()
        val timed = events.filter { !it.allDay }

        // 24 hour columns in a 12x2 grid (AM row, PM row)
        val cols = 12
        val gap = kit.maxW / (cols - 1)
        val rowY = listOf(kit.height * 0.30f, kit.height * 0.48f)
        val paintHour = kit.textPaint(kit.greyColor, 8.5f, 11f)
        for (h in 0 until 24) {
            val x = kit.pad + (h % cols) * gap
            val y = rowY[h / cols]
            val hourStart = dayStart + h * 3_600_000L
            val busy = timed.any { it.beginMs < hourStart + 3_600_000L && it.endMs > hourStart }
            val paint = when {
                h == nowHour -> kit.fill(kit.accentColor)
                busy -> kit.fill(kit.onColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(x, y, if (busy || h == nowHour) gap * 0.26f else gap * 0.18f, paint)
            if (h % 6 == 0) {
                val label = String.format(Locale.US, "%02d", h)
                canvas.drawText(label, x - paintHour.measureText(label) / 2f, y + gap * 0.26f + 12f * d, paintHour)
            }
        }

        val next = timed.firstOrNull { it.endMs > now }
        val paintNext = kit.textPaint(kit.onColor, 11f, 15f)
        val caption = when {
            next == null && events.isEmpty() -> "NOTHING SCHEDULED TODAY"
            next == null -> "NO MORE EVENTS TODAY"
            next.beginMs <= now -> "NOW: ${next.title.uppercase()}"
            else -> "NEXT ${timeFmt.format(Date(next.beginMs))}: ${next.title.uppercase()}"
        }
        DotMatrixRenderer.drawFittedLabel(canvas, caption, kit.pad, kit.height * 0.72f, kit.maxW, paintNext)
        val allDay = events.filter { it.allDay }
        if (allDay.isNotEmpty()) {
            DotMatrixRenderer.drawFittedLabel(canvas, "ALL DAY: " + allDay.joinToString(", ") { it.title.uppercase() }, kit.pad, kit.height * 0.72f + 20f * d, kit.maxW, kit.textPaint(kit.greyColor, 10f, 13f))
        }
    }

    /** ISO-8601 week number with day-of-year. */
    fun renderIsoWeek(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("ISO WEEK", kit.accentColor)

        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        val week = String.format(Locale.US, "W%02d", cal.get(Calendar.WEEK_OF_YEAR))
        kit.dotTextCentered(week, kit.height * 0.46f, kit.height * 0.34f, 10f * d, accentIndices = setOf(0))

        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        kit.labelCentered("DAY $dayOfYear / $daysInYear", kit.height - kit.pad - 14f * d, kit.textPaint(kit.onColor, 11f, 14f))
        kit.labelCentered("${daysInYear - dayOfYear} DAYS LEFT", kit.height - kit.pad, kit.textPaint(kit.greyColor, 9.5f, 12f))
    }

    /** Days remaining until the user's event (set in the widget's CONTENT section). */
    fun renderEventCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val prefs = PreferencesManager.getInstance(context)
        val name = prefs.getEventName().uppercase()
        kit.header(name, kit.accentColor)

        val target = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(prefs.getEventDate()) }.getOrNull()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val days = target?.let { TimeUnit.MILLISECONDS.toDays(it.time - today.timeInMillis + 43_200_000L).toInt() }

        val (value, caption) = when {
            days == null -> "--" to "SET A DATE IN THE APP"
            days > 0 -> days.toString() to if (days == 1) "DAY TO GO" else "DAYS TO GO"
            days == 0 -> "0" to "IT'S TODAY"
            else -> (-days).toString() to "DAYS AGO"
        }
        kit.dotTextCentered(value, kit.height * 0.48f, kit.height * 0.36f, 11f * d)
        val dateStr = target?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(it).uppercase() } ?: ""
        kit.labelCentered(caption, kit.height - kit.pad - 14f * d, kit.textPaint(kit.onColor, 11f, 14f))
        kit.labelCentered(dateStr, kit.height - kit.pad, kit.textPaint(kit.greyColor, 9.5f, 12f))
    }

    /** Two-tone card: weekday on an accent strip, big day number below. */
    fun renderSplitCard(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val cal = Calendar.getInstance()
        val weekday = SimpleDateFormat("EEEE", Locale.US).format(cal.time).uppercase()
        val month = SimpleDateFormat("MMM", Locale.US).format(cal.time).uppercase()
        val day = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))

        // Accent top strip clipped to the widget's rounded corners
        val stripH = kit.height * 0.30f
        val r = spec.cornerRadiusDp * d
        canvas.save()
        canvas.clipRect(0f, 0f, kit.width, stripH)
        canvas.drawRoundRect(RectF(1.5f, 1.5f, kit.width - 1.5f, kit.height - 1.5f), r, r, kit.fill(kit.accentColor))
        canvas.restore()
        val paintWeekday = kit.textPaint(0xFFFFFFFF.toInt(), 12f, 16f)
        kit.labelCentered(weekday, stripH / 2f + paintWeekday.textSize / 3f, paintWeekday)

        kit.dotTextCentered(day, stripH + (kit.height - stripH) * 0.44f, (kit.height - stripH) * 0.48f, 10f * d)
        kit.labelCentered("$month ${cal.get(Calendar.YEAR)}", kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    /**
     * Habit streak: last 5 weeks as a dot grid (Mon..Sun columns), done days lit.
     * Tapping the widget marks today done/undone.
     */
    fun renderHabitMatrix(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val prefs = PreferencesManager.getInstance(context)
        val doneDays = prefs.getHabitDays()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        kit.header(prefs.getHabitName().uppercase(), widthFraction = 0.6f)

        // Current streak counting back from today (or yesterday if today isn't done yet)
        val cursor = Calendar.getInstance()
        if (fmt.format(cursor.time) !in doneDays) cursor.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (fmt.format(cursor.time) in doneDays) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        val streakStr = "$streak DAY STREAK"
        val paintStreak = kit.textPaint(kit.accentColor, 10f, 13f)
        canvas.drawText(streakStr, kit.width - kit.pad - paintStreak.measureText(streakStr), kit.pad + 12f * d, paintStreak)

        val weeks = 5
        val today = Calendar.getInstance()
        val todayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(todayIndex + 7 * (weeks - 1))) }

        val top = kit.pad + 30f * d
        val gridH = kit.height - top - kit.pad - 4f * d
        val cellX = kit.maxW / 7f
        val cellY = gridH / weeks
        val dotR = minOf(cellX, cellY) * 0.26f
        val dayCal = start.clone() as Calendar
        for (w in 0 until weeks) {
            for (dow in 0 until 7) {
                val cx = kit.pad + cellX * (dow + 0.5f)
                val cy = top + cellY * (w + 0.5f)
                val key = fmt.format(dayCal.time)
                val isToday = w == weeks - 1 && dow == todayIndex
                val isFuture = w == weeks - 1 && dow > todayIndex
                val paint = when {
                    key in doneDays -> kit.fill(if (isToday) kit.accentColor else kit.onColor)
                    isFuture -> kit.fill(kit.offColor).apply { alpha = alpha / 2 }
                    else -> kit.fill(kit.offColor)
                }
                canvas.drawCircle(cx, cy, if (key in doneDays) dotR else dotR * 0.75f, paint)
                if (isToday && key !in doneDays) {
                    canvas.drawCircle(cx, cy, dotR, kit.fill(kit.accentColor).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * d
                    })
                }
                dayCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}
