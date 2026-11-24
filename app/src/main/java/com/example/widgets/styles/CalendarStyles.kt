package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassBottom
import com.example.data.calendar.CalendarRepository
import com.example.data.prefs.PreferencesManager
import com.example.data.travel.TravelRepository
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Extra designs for the calendar family (Batch 1, items 13–21). */
object CalendarStyles {

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "calendar.day.date" to listOf(
            WidgetStyle("bold_date", "Bold Date", StyleFamily.BOLD) { c, s, cv -> renderDateTile(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_date", "Soft Date", StyleFamily.SOFT) { c, s, cv -> renderDateTile(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("dot_date", "Stacked Dot Date", StyleFamily.SPECIAL, render = ::renderStackedDotDate)
        ),
        "calendar.minimal.split" to listOf(
            WidgetStyle("soft_split", "Soft Split", StyleFamily.SOFT, render = ::renderSoftSplit)
        ),
        "calendar.month.grid" to listOf(
            WidgetStyle("soft_month", "Soft Month", StyleFamily.SOFT) { c, s, cv -> renderMonth(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_month", "Bold Month", StyleFamily.BOLD) { c, s, cv -> renderMonth(c, s, cv, StyleFamily.BOLD) }
        ),
        "calendar.week.progress" to listOf(
            WidgetStyle("week_pill", "Week Pill", StyleFamily.SOFT, render = ::renderWeekPill)
        ),
        "calendar.agenda.3" to listOf(
            WidgetStyle("soft_agenda", "Today Card", StyleFamily.SOFT, render = ::renderSoftAgenda),
            WidgetStyle("timeline_bar", "Timeline Bar", StyleFamily.SOFT, render = ::renderTimelineBar)
        ),
        "calendar.countdown.date" to listOf(
            WidgetStyle("hourglass", "Hourglass", StyleFamily.BOLD) { c, s, cv -> renderEventCountdown(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_countdown", "Soft Countdown", StyleFamily.SOFT) { c, s, cv -> renderEventCountdown(c, s, cv, StyleFamily.SOFT) }
        ),
        "travel.trip.countdown" to listOf(
            WidgetStyle("soft_countdown", "Soft Countdown", StyleFamily.SOFT) { c, s, cv -> renderTripCountdown(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("hourglass", "Hourglass", StyleFamily.BOLD) { c, s, cv -> renderTripCountdown(c, s, cv, StyleFamily.BOLD) }
        )
    )

    /** "TUE / 18 / Nov" tile (mockup date tiles). */
    fun renderDateTile(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val cal = Calendar.getInstance()
        val showWeekday = spec.options.boolean("show_weekday", true)
        val day = cal.get(Calendar.DAY_OF_MONTH).toString()
        val weekday = StyleData.format("EEE", cal).uppercase(Locale.US)
        val month = StyleData.format("MMM", cal)
        if (k.isWide) {
            val numSize = 48f
            val numW = k.text(day, k.left, k.cy + numSize * k.u * 0.36f, numSize, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW * 0.5f)
            val textX = k.left + numW + 8f * k.u
            if (showWeekday) k.text(StyleData.format("EEEE", cal), textX, k.cy - 2f * k.u, 13f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - textX)
            k.text(StyleData.format("MMMM yyyy", cal), textX, k.cy + 13f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - textX)
            return
        }
        if (showWeekday) k.text(weekday, k.cx, k.top + 12f * k.u, 13f, k.ink, Weight.MEDIUM, Align.CENTER)
        k.text(day, k.cx, k.cy + 15f * k.u, 44f, k.ink, Weight.SEMIBOLD, Align.CENTER)
        k.text(month, k.cx, k.bottom - 1f * k.u, 13f, k.ink, Weight.MEDIUM, Align.CENTER)
    }

    /** "TUE / 18 / NOV" stacked in dot matrix on a soft card (mockup vertical dot date). */
    fun renderStackedDotDate(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val cal = Calendar.getInstance()
        val lines = listOf(
            StyleData.format("EEE", cal).uppercase(Locale.US),
            cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'),
            StyleData.format("MMM", cal).uppercase(Locale.US)
        )
        val band = (k.bottom - k.top) / 3f
        val maxW = if (k.isWide) k.contentW * 0.4f else k.contentW * 0.78f
        lines.forEachIndexed { i, line ->
            k.kit.dotTextCentered(line, k.top + band * i + band / 2f, band * 0.78f, 5f * k.u, maxWidth = maxW,
                accentIndices = emptySet())
        }
    }

    fun renderSoftSplit(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val cal = Calendar.getInstance()
        val mid = k.left + k.contentW * 0.5f
        k.text(cal.get(Calendar.DAY_OF_MONTH).toString(), (k.left + mid) / 2f, k.cy + 14f * k.u, 40f, k.ink, Weight.SEMIBOLD, Align.CENTER, k.contentW * 0.48f)
        k.roundRect(mid - 0.6f * k.u, k.top + 8f * k.u, mid + 0.6f * k.u, k.bottom - 8f * k.u, 0.6f * k.u, k.track)
        val rx = (mid + k.right) / 2f
        k.text(StyleData.format("EEE", cal), rx, k.cy - 3f * k.u, 13f, k.ink, Weight.SEMIBOLD, Align.CENTER, k.contentW * 0.46f)
        k.text(StyleData.format("MMM", cal), rx, k.cy + 13f * k.u, 13f, k.sub, Weight.MEDIUM, Align.CENTER, k.contentW * 0.46f)
    }

    /** "November" mini month with Monday-first letters and today circled in the accent. */
    fun renderMonth(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val today = Calendar.getInstance()
        val first = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday-first offset
        val days = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = (lead + days + 6) / 7
        if (k.isWide) {
            renderWideMonth(k, today, lead, days, rows)
            return
        }
        k.title(StyleData.format("MMMM", today))
        drawGrid(k, today, lead, days, rows, k.left, k.right, k.contentTop + 2f * k.u)
    }

    /** Wide month card: month and today's number on the left, the whole month on the right. */
    private fun renderWideMonth(k: StyleKit, today: Calendar, lead: Int, days: Int, rows: Int) {
        val leftW = k.contentW * 0.27f
        if (k.spec.showLabel) k.text(StyleData.format("MMMM", today), k.left, k.top + 9f * k.u, 11f, k.ink, Weight.MEDIUM, maxWidth = leftW)
        k.text(today.get(Calendar.DAY_OF_MONTH).toString(), k.left, k.bottom - 18f * k.u, 34f, k.accent, Weight.SEMIBOLD, maxWidth = leftW)
        k.text(StyleData.format("EEEE", today), k.left, k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = leftW)
        drawGrid(k, today, lead, days, rows, k.left + leftW + 5f * k.u, k.right - k.cornerReserve, k.top - 2f * k.u)
    }

    private fun drawGrid(k: StyleKit, today: Calendar, lead: Int, days: Int, rows: Int, gl: Float, gr: Float, gridTop: Float) {
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val colW = (gr - gl) / 7f
        val rowH = (k.bottom - gridTop) / (rows + 1)
        val size = minOf(rowH / k.u * 0.72f, colW / k.u * 0.5f, 11f)
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, letter ->
            k.text(letter, gl + colW * i + colW / 2f, gridTop + rowH * 0.7f, size, k.sub, Weight.MEDIUM, Align.CENTER, colW)
        }
        for (d in 1..days) {
            val index = lead + d - 1
            val x = gl + colW * (index % 7) + colW / 2f
            val baseline = gridTop + rowH * (index / 7 + 1) + rowH * 0.7f
            if (d == todayDay) {
                k.circle(x, baseline - size * k.u * 0.35f, minOf(rowH, colW) * 0.48f, k.accent)
                k.text(d.toString(), x, baseline, size, 0xFFFFFFFF.toInt(), Weight.SEMIBOLD, Align.CENTER, colW)
            } else {
                k.text(d.toString(), x, baseline, size, if (d < todayDay) k.faint else k.ink, Weight.MEDIUM, Align.CENTER, colW)
            }
        }
    }

    /** "M T (W) T F S S" pill with today in an ink circle. */
    fun renderWeekPill(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val dow = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
        val colW = k.contentW / 7f
        val y = k.cy
        val r = minOf(colW * 0.42f, (k.bottom - k.top) * 0.3f)
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, letter ->
            val x = k.left + colW * i + colW / 2f
            val active = i == dow
            if (active) k.circle(x, y, r, k.ink)
            k.text(letter, x, y + r * 0.42f, r / k.u * 1.2f, if (active) (if (k.dark) 0xFF0B0B0C.toInt() else 0xFFFFFFFF.toInt()) else if (i < dow) k.faint else k.ink, Weight.SEMIBOLD, Align.CENTER, colW)
        }
    }

    private fun todaysEvents(context: Context) = CalendarRepository.upcomingToday(context)

    private fun timeOf(ms: Long): String = SimpleDateFormat("h:mm a", Locale.US).format(ms)

    /** "● Today · Team Meeting · 10:30 AM" (mockup agenda tile). */
    fun renderSoftAgenda(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val events = todaysEvents(context)
        val dotR = 2.6f * k.u
        k.circle(k.left + dotR, k.top + 5.5f * k.u, dotR, k.accent)
        if (spec.showLabel) k.text("Today", k.left + dotR * 2 + 3f * k.u, k.top + 9f * k.u, 10.5f, k.ink, Weight.MEDIUM)
        val iconSize = 16f * k.u
        k.icon(Icons.Filled.CalendarMonth, k.right - iconSize / 2f, k.cy, iconSize, k.faint)
        val textW = k.contentW - iconSize - 4f * k.u
        if (events.isNullOrEmpty()) {
            k.text(if (events == null) "Allow calendar access" else "No more events today", k.left, k.cy + 6f * k.u, 13f, k.ink, Weight.SEMIBOLD, maxWidth = textW)
            return
        }
        val first = events.first()
        k.text(first.title.ifBlank { "Event" }, k.left, k.cy + 3f * k.u, 14f, k.ink, Weight.SEMIBOLD, maxWidth = textW)
        k.text(if (first.allDay) "All day" else timeOf(first.beginMs), k.left, k.cy + 19f * k.u, 13f, k.ink, Weight.MEDIUM, maxWidth = textW)
        if (events.size > 1) k.text("+${events.size - 1} more", k.left, k.bottom - 1f * k.u, 9f, k.sub, Weight.MEDIUM, maxWidth = textW)
    }

    /** A day track with black event blocks and a red "now" marker (mockup "Visual Timeline"). */
    fun renderTimelineBar(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        k.title("Timeline")
        val events = todaysEvents(context)
        // A 12-hour window starting 3 hours before now, so "now" and what's next are always visible.
        val dayStart = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
        val span = TimeUnit.HOURS.toMillis(12)
        val trackTop = k.cy - 3f * k.u
        val trackBottom = k.cy + 5f * k.u
        k.roundRect(k.left, trackTop, k.right, trackBottom, (trackBottom - trackTop) / 2f, k.track)
        fun xOf(ms: Long) = k.left + k.contentW * ((ms - dayStart).toFloat() / span).coerceIn(0f, 1f)
        events.orEmpty().filter { !it.allDay }.forEach { e ->
            val l = xOf(e.beginMs)
            val r = maxOf(xOf(e.endMs), l + 4f * k.u)
            k.roundRect(l, trackTop, r, trackBottom, (trackBottom - trackTop) / 2f, k.ink)
        }
        val now = System.currentTimeMillis()
        val nx = xOf(now)
        k.canvas.drawLine(nx, trackTop - 7f * k.u, nx, trackBottom + 2f * k.u, k.stroke(k.accent, 0.9f * k.u))
        k.circle(nx, trackTop - 7f * k.u, 1.6f * k.u, k.accent)
        k.text(SimpleDateFormat("HH:mm", Locale.US).format(now), nx + 3f * k.u, trackTop - 5f * k.u, 8.5f, k.ink, Weight.MEDIUM, maxWidth = k.right - nx - 3f * k.u)
        val names = events.orEmpty().filter { it.endMs >= now }.take(2).map { it.title.ifBlank { "Event" } }.ifEmpty {
            listOf(if (events == null) "Allow calendar access" else "Free day")
        }
        names.forEachIndexed { i, name ->
            val y = trackBottom + 11f * k.u + i * 10f * k.u
            if (y > k.bottom + 2f * k.u) return@forEachIndexed
            k.circle(k.left + 1.4f * k.u, y - 2.6f * k.u, 1.4f * k.u, k.ink)
            k.text(name, k.left + 5f * k.u, y, 9f, k.ink, Weight.MEDIUM, maxWidth = k.contentW - 5f * k.u)
        }
    }

    private fun daysUntil(dateIso: String?): Int? {
        val target = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso ?: "") }.getOrNull() ?: return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(target.time - today.timeInMillis + 43_200_000L).toInt()
    }

    private fun drawCountdown(k: StyleKit, name: String, days: Int?) {
        if (k.isWide) {
            val iconSize = (k.bottom - k.top) * 0.7f
            k.icon(Icons.Filled.HourglassBottom, k.left + iconSize / 2f, k.cy, iconSize, k.ink)
            val x = k.left + iconSize + 8f * k.u
            k.text(name, x, k.cy - 9f * k.u, 12f, k.sub, Weight.MEDIUM, maxWidth = k.right - x)
            k.text(countdownLine(days), x, k.cy + 17f * k.u, 22f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - x)
            return
        }
        val iconSize = 26f * k.u
        k.icon(Icons.Filled.HourglassBottom, k.cx, k.top + iconSize / 2f + 2f * k.u, iconSize, k.ink)
        k.text(name, k.cx, k.cy + 14f * k.u, 11f, k.ink, Weight.MEDIUM, Align.CENTER)
        k.text(countdownLine(days), k.cx, k.bottom - 1f * k.u, 12f, k.ink, Weight.SEMIBOLD, Align.CENTER)
    }

    private fun countdownLine(days: Int?): String = when {
        days == null -> "Set a date"
        days > 1 -> "$days Days"
        days == 1 -> "1 Day"
        days == 0 -> "Today"
        else -> "${-days} days ago"
    }

    /** Hourglass "Vacation in / 12 Days" (mockup). Uses the same event name and date as the classic. */
    fun renderEventCountdown(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val prefs = PreferencesManager.getInstance(context)
        val days = daysUntil(prefs.getEventDate())
        val name = StyleData.titleCase(prefs.getEventName())
        drawCountdown(k, if ((days ?: 0) > 0) "$name in" else name, days)
    }

    fun renderTripCountdown(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val repo = TravelRepository.getInstance(context)
        val dest = StyleData.titleCase(spec.options.string("destination", repo.tripDestination()))
        val date = if (spec.options.boolean("show_date", false)) runCatching {
            SimpleDateFormat("d MMM", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(repo.tripDate())!!)
        }.getOrNull() else null
        drawCountdown(k, if (date != null) "$dest · $date" else "$dest in", repo.daysUntilTrip())
    }
}
