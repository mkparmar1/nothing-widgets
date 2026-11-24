package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import com.example.core.time.ClockOptions
import com.example.core.time.DateFormat
import com.example.core.time.TimeFormat
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight
import java.util.Calendar
import java.util.TimeZone

/** Extra designs for the clock family (Batch 1, items 1–12). */
object ClockStyles {

    private val boldTime = WidgetStyle("bold_time", "Bold Time", StyleFamily.BOLD) { c, s, cv -> renderBoldTime(c, s, cv, null) }
    private val softTime = WidgetStyle("soft_time", "Soft Time", StyleFamily.SOFT) { c, s, cv -> renderSoftTime(c, s, cv, null) }

    private fun city(zone: String?) = listOf(
        WidgetStyle("soft_city", "City Card", StyleFamily.SOFT) { c, s, cv -> renderCityCard(c, s, cv, zone, StyleFamily.SOFT) },
        WidgetStyle("bold_city", "Bold City", StyleFamily.BOLD) { c, s, cv -> renderCityCard(c, s, cv, zone, StyleFamily.BOLD) }
    )

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "clock.dotdigital.m" to listOf(boldTime, softTime),
        "clock.dotdigital.s" to listOf(boldTime, softTime),
        "clock.stacked.hhmm" to listOf(
            WidgetStyle("bold_pill", "Bold Stack", StyleFamily.BOLD, render = ::renderBoldStack),
            WidgetStyle("soft_stack", "Soft Stack", StyleFamily.SOFT, render = ::renderSoftStack)
        ),
        "clock.analog.ring" to listOf(
            WidgetStyle("classic_light", "Classic Analog", StyleFamily.SOFT) { c, s, cv -> renderAnalog(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("classic_dark", "Black Analog", StyleFamily.BOLD) { c, s, cv -> renderAnalog(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("hybrid_dial", "Hybrid Dial", StyleFamily.BOLD, render = ::renderHybridDial)
        ),
        "clock.world.custom" to city(null),
        "clock.world.london" to city("Europe/London"),
        "clock.world.tokyo" to city("Asia/Tokyo"),
        "clock.world.nyc" to city("America/New_York"),
        "travel.clock.multi" to listOf(
            WidgetStyle("analog_trio", "Analog Dials", StyleFamily.SOFT, render = ::renderAnalogTrio)
        )
    )

    // ------------------------------------------------------------------ helpers

    private fun calendar(zone: String?): Calendar =
        if (zone.isNullOrBlank()) Calendar.getInstance() else Calendar.getInstance(TimeZone.getTimeZone(zone))

    /** The mockups' "Tue, 18 Nov" in title case for the default format; the user's format otherwise. */
    fun dateText(cal: Calendar, format: DateFormat): String? = when (format) {
        DateFormat.NONE -> null
        DateFormat.WEEKDAY_DAY_MONTH -> StyleData.format("EEE, d MMM", cal)
        DateFormat.FULL -> StyleData.format("EEEE, d MMMM", cal)
        else -> format.format(cal)
    }

    private fun options(spec: WidgetSpec) = ClockOptions.from(spec.configJson, TimeFormat.H24, DateFormat.WEEKDAY_DAY_MONTH)

    /** Hours without seconds — the bitmap redraws once a minute, so styles never show stale seconds. */
    private fun minuteFormat(format: TimeFormat): TimeFormat = when (format) {
        TimeFormat.H24_SECONDS -> TimeFormat.H24
        TimeFormat.H12_SECONDS -> TimeFormat.H12
        else -> format
    }

    private fun drawTimeAndDate(k: StyleKit, spec: WidgetSpec, zone: String?) {
        val opts = options(spec)
        val cal = calendar(zone ?: ClockOptions.timeZone(spec.configJson))
        val (time, amPm) = StyleData.time(cal, minuteFormat(opts.time))
        val date = dateText(cal, opts.date)
        val timeSize = when {
            k.isWide -> 46f
            k.isLarge -> 34f
            else -> 33f
        }
        val timeBaseline = if (date == null) k.cy + timeSize * k.u * 0.36f else k.cy + timeSize * k.u * 0.18f
        val ampmW = if (amPm != null) k.measure(amPm, timeSize * 0.32f) + 2f * k.u else 0f
        val timeW = k.text(time, k.cx - ampmW / 2f, timeBaseline, timeSize, k.ink, Weight.SEMIBOLD, Align.CENTER, k.contentW - ampmW)
        if (amPm != null) {
            k.text(amPm, k.cx - ampmW / 2f + timeW / 2f + 2f * k.u, timeBaseline, timeSize * 0.32f, k.sub, Weight.MEDIUM)
        }
        if (date != null) {
            k.text(date, k.cx, k.bottom - 2f * k.u, if (k.isSmall) 11f else 12f, if (k.bold) k.ink else k.sub, Weight.MEDIUM, Align.CENTER)
        }
    }

    // ------------------------------------------------------------- renderers

    fun renderBoldTime(context: Context, spec: WidgetSpec, canvas: Canvas, zone: String?) {
        val k = StyleKit(context, spec, canvas, StyleFamily.BOLD)
        k.card()
        drawTimeAndDate(k, spec, zone)
    }

    fun renderSoftTime(context: Context, spec: WidgetSpec, canvas: Canvas, zone: String?) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        drawTimeAndDate(k, spec, zone)
    }

    private fun hhmm(spec: WidgetSpec): Pair<String, String> {
        val opts = options(spec)
        val cal = Calendar.getInstance()
        val h = if (opts.time.is24h) cal.get(Calendar.HOUR_OF_DAY) else (cal.get(Calendar.HOUR).takeIf { it != 0 } ?: 12)
        return String.format(java.util.Locale.US, "%02d", h) to String.format(java.util.Locale.US, "%02d", cal.get(Calendar.MINUTE))
    }

    /** Black tile with dot-matrix hours over minutes and a red dot (mockup "10 / 45 ●"). */
    fun renderBoldStack(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.BOLD)
        k.card()
        val (h, m) = hhmm(spec)
        if (k.isWide) {
            // Wide tile: hours and minutes side by side, red dot between them.
            val half = k.contentW / 2f
            val height = (k.bottom - k.top) * 0.78f
            val gapW = 8f * k.u
            k.kit.dotTextCentered("$h $m", k.cy, height, 9f * k.u, maxWidth = k.contentW - gapW)
            k.circle(k.left + half, k.bottom - 2f * k.u, 2.6f * k.u, k.accent)
            return
        }
        val band = (k.bottom - k.top) * 0.36f
        val gap = 6f * k.u
        k.kit.dotTextCentered(h, k.top + band / 2f + 2f * k.u, band, gap, maxWidth = k.contentW * 0.8f)
        k.kit.dotTextCentered(m, k.top + band * 1.5f + 6f * k.u, band, gap, maxWidth = k.contentW * 0.8f)
        k.circle(k.cx, k.bottom - 3f * k.u, 2.6f * k.u, k.accent)
    }

    /** Soft card with big clean stacked digits and the weekday. */
    fun renderSoftStack(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val (h, m) = hhmm(spec)
        val size = if (k.isWide) 34f else 30f
        k.text(h, k.cx, k.top + size * k.u * 0.85f, size, k.ink, Weight.SEMIBOLD, Align.CENTER)
        k.text(m, k.cx, k.top + size * k.u * 1.85f, size, k.ink, Weight.SEMIBOLD, Align.CENTER)
        k.text(StyleData.format("EEE", Calendar.getInstance()), k.cx, k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, Align.CENTER)
    }

    /** A clean analog face with 60 ticks; red centre cap (mockup "Classic Analog"). */
    fun drawDial(k: StyleKit, x: Float, y: Float, r: Float, cal: Calendar, face: Int?, tickColor: Int, handColor: Int) {
        if (face != null) k.circle(x, y, r, face)
        k.ticks(x, y, r * 0.92f, 60, r * 0.04f, r * 0.12f, 5, tickColor, maxOf(1f, r * 0.025f))
        val (hourDeg, minuteDeg) = StyleData.handAngles(cal)
        k.hand(x, y, hourDeg, r * 0.48f, r * 0.075f, handColor)
        k.hand(x, y, minuteDeg, r * 0.72f, r * 0.05f, handColor)
        k.hand(x, y, minuteDeg + 180f, r * 0.16f, r * 0.03f, k.accent)
        k.circle(x, y, r * 0.06f, k.accent)
    }

    fun renderAnalog(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val cal = Calendar.getInstance()
        if (k.isWide) {
            val r = (k.bottom - k.top) / 2f
            val x = k.left + r
            drawDial(k, x, k.cy, r, cal, null, k.ink, k.ink)
            val textX = x + r + 8f * k.u
            val w = k.right - textX
            k.text(StyleData.format("EEEE", cal), textX, k.cy - 4f * k.u, 13f, k.ink, Weight.SEMIBOLD, maxWidth = w)
            k.text(StyleData.format("d MMMM", cal), textX, k.cy + 12f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = w)
        } else {
            val r = minOf(k.width, k.height) / 2f - k.pad * 0.6f
            drawDial(k, k.cx, k.cy, r, cal, null, k.ink, k.ink)
        }
    }

    /** Black tachymeter-style bezel with the digital time and date inside (mockup "Hybrid Dial"). */
    fun renderHybridDial(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.BOLD)
        k.card()
        val cal = Calendar.getInstance()
        val opts = options(spec)
        val r = minOf(k.width, k.height) / 2f - k.pad * 0.5f
        val x = if (k.isWide) k.left + r else k.cx
        k.ticks(x, k.cy, r, 60, r * 0.05f, r * 0.13f, 5, k.ink, maxOf(1f, r * 0.022f))
        // Seconds-of-minute marker: a red dot on the bezel at the current minute.
        val a = Math.toRadians(cal.get(Calendar.MINUTE) * 6.0 - 90.0)
        k.circle(x + (r * 0.74f) * Math.cos(a).toFloat(), k.cy + (r * 0.74f) * Math.sin(a).toFloat(), r * 0.05f, k.accent)
        val (time, amPm) = StyleData.time(cal, minuteFormat(opts.time))
        val inner = r * 1.25f
        if (k.isWide) {
            val textX = x + r + 7f * k.u
            k.text(time + (amPm?.let { " $it" } ?: ""), textX, k.cy + 4f * k.u, 26f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - textX)
            dateText(cal, opts.date)?.let { k.text(it, textX, k.cy + 18f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = k.right - textX) }
        } else {
            k.text(time, x, k.cy + 5f * k.u, 20f, k.ink, Weight.SEMIBOLD, Align.CENTER, inner)
            dateText(cal, opts.date)?.let { k.text(it, x, k.cy + 16f * k.u, 8f, k.sub, Weight.MEDIUM, Align.CENTER, inner) }
        }
    }

    /** "New York · 12:15 AM · GMT -5" card with a globe (mockup world-clock tile). */
    fun renderCityCard(context: Context, spec: WidgetSpec, canvas: Canvas, fixedZone: String?, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val zone = fixedZone ?: ClockOptions.timeZone(spec.configJson)
        val cal = calendar(zone)
        val opts = ClockOptions.from(spec.configJson, TimeFormat.H24, DateFormat.NONE)
        val (time, amPm) = StyleData.time(cal, minuteFormat(opts.time))
        val city = StyleData.cityFromZone(zone ?: TimeZone.getDefault().id)
        val sub = "${StyleData.gmtLabel(zone)} · ${StyleData.dayRelative(zone)}"
        var textX = k.left
        if (k.isWide) {
            val iconSize = (k.bottom - k.top) * 0.62f
            k.icon(Icons.Filled.Public, k.left + iconSize / 2f, k.cy, iconSize, k.ink)
            textX = k.left + iconSize + 7f * k.u
        }
        val w = k.right - textX
        if (spec.showLabel) k.text(city, textX, k.top + 9f * k.u, 11f, k.ink, Weight.MEDIUM, maxWidth = w - 8f * k.u)
        val timeLine = time + (amPm?.let { " $it" } ?: "")
        k.text(timeLine, textX, k.cy + 9f * k.u, if (k.isWide) 26f else 22f, k.ink, Weight.SEMIBOLD, maxWidth = w)
        k.text(sub, textX, k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM, maxWidth = w)
    }

    /** Tokyo / London / Sydney analog dials in a row (mockup "World clocks"). */
    fun renderAnalogTrio(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val zones = spec.options.string("zones", "Europe/London,Asia/Tokyo,America/New_York").split(",")
            .map { it.trim() }.filter { it.isNotBlank() }
            .take(spec.options.int("count", 3).coerceIn(2, 4))
        if (zones.isEmpty()) {
            k.text("Set cities in the app", k.cx, k.cy, 11f, k.sub, Weight.MEDIUM, Align.CENTER)
            return
        }
        if (k.isLarge) {
            // Square tile: a 2-column grid so the dials fill the space.
            val rows = (zones.size + 1) / 2
            val cellW = k.contentW / 2f
            val cellH = (k.bottom - k.top) / rows
            zones.forEachIndexed { i, zone ->
                val lastAlone = i == zones.lastIndex && zones.size % 2 == 1
                val x = if (lastAlone) k.cx else k.left + cellW * (i % 2) + cellW / 2f
                val cellTop = k.top + cellH * (i / 2)
                val r = minOf(cellW * 0.4f, (cellH - 24f * k.u) / 2f)
                drawCity(k, zone, x, cellTop + r + 1f * k.u, r, i == 0, cellW * 0.94f)
            }
            return
        }
        val slot = k.contentW / zones.size
        val labelBand = 24f * k.u
        val r = minOf(slot * 0.42f, (k.bottom - k.top - labelBand) / 2f)
        val dialY = k.top + (k.bottom - k.top - labelBand - 2f * r) / 2f + r
        zones.forEachIndexed { i, zone ->
            val x = k.left + slot * i + slot / 2f
            drawCity(k, zone, x, dialY, r, i == 0, slot * 0.94f)
        }
    }

    /** One dial with its city and time underneath; the first city is inverted like the mockup. */
    private fun drawCity(k: StyleKit, zone: String, x: Float, y: Float, r: Float, first: Boolean, labelW: Float) {
        val cal = calendar(zone)
        val inverse = if (k.dark) 0xFF0B0B0C.toInt() else 0xFFFFFFFF.toInt()
        drawDial(k, x, y, r, cal, if (first) k.ink else k.surface, if (first) k.faint else k.sub, if (first) inverse else k.ink)
        val labelY = y + r + 10f * k.u
        k.text(StyleData.cityFromZone(zone), x, labelY, 9.5f, k.ink, Weight.MEDIUM, Align.CENTER, labelW)
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val h24 = ClockOptions.from(k.spec.configJson).time.is24h
        k.text(StyleData.minutesLabel(minutes, h24).lowercase(), x, labelY + 10f * k.u, 8.5f, k.sub, Weight.REGULAR, Align.CENTER, labelW)
    }
}
