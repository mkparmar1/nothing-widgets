package com.example.core.time

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** How a clock widget shows the time. Formats with seconds are drawn by a live TextClock overlay. */
enum class TimeFormat(val label: String, val hasSeconds: Boolean, val is24h: Boolean) {
    H24("24h • 21:45", hasSeconds = false, is24h = true),
    H12("12h • 09:45 PM", hasSeconds = false, is24h = false),
    H24_SECONDS("24h + seconds • 21:45:30", hasSeconds = true, is24h = true),
    H12_SECONDS("12h + seconds • 09:45:30 PM", hasSeconds = true, is24h = false);

    /** Pattern for SimpleDateFormat / TextClock (no AM/PM; that's drawn separately). */
    val pattern: String
        get() = (if (is24h) "HH:mm" else "hh:mm") + if (hasSeconds) ":ss" else ""

    fun format(cal: Calendar): String = SimpleDateFormat(pattern, Locale.US).apply { timeZone = cal.timeZone }.format(cal.time)

    /** "AM"/"PM" for 12-hour formats, null for 24-hour. */
    fun amPm(cal: Calendar): String? = if (is24h) null else if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    companion object {
        fun fromName(name: String?, default: TimeFormat = H24) = entries.firstOrNull { it.name == name } ?: default
    }
}

/** How a clock widget shows the date. */
enum class DateFormat(val label: String, private val pattern: String?) {
    WEEKDAY_DAY_MONTH("MON, 19 OCT", "EEE, dd MMM"),
    DD_MM_YY("19-10-26", "dd-MM-yy"),
    DD_MM_YYYY("19/10/2026", "dd/MM/yyyy"),
    MM_DD_YY("10/19/26", "MM/dd/yy"),
    ISO("2026-10-19", "yyyy-MM-dd"),
    FULL("MONDAY 19 OCTOBER", "EEEE dd MMMM"),
    NONE("No date", null);

    /** Formatted date, or null when the date is hidden. */
    fun format(cal: Calendar): String? = pattern?.let {
        SimpleDateFormat(it, Locale.US).apply { timeZone = cal.timeZone }.format(cal.time).uppercase()
    }

    companion object {
        fun fromName(name: String?, default: DateFormat = WEEKDAY_DAY_MONTH) = entries.firstOrNull { it.name == name } ?: default
    }
}

/** Look of every dot drawn by the dot-matrix renderer. */
enum class DotStyle(val label: String) {
    ROUND("Round"),
    SQUARE("Square"),
    BOLD("Bold"),
    /** Only lit dots; the faint background grid is hidden. */
    MINIMAL("Minimal");

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: ROUND
    }
}

/** Per-widget clock options stored in the widget's configJson. */
data class ClockOptions(val time: TimeFormat, val date: DateFormat) {
    companion object {
        const val KEY_TIME = "time_format"
        const val KEY_DATE = "date_format"
        const val KEY_DOT_STYLE = "dot_style"
        /** Optional IANA time zone id for world-clock widgets (e.g. "Asia/Kolkata"). */
        const val KEY_TZ = "time_zone"

        fun from(
            configJson: String,
            defaultTime: TimeFormat = TimeFormat.H24,
            defaultDate: DateFormat = DateFormat.WEEKDAY_DAY_MONTH
        ): ClockOptions {
            val json = runCatching { JSONObject(configJson) }.getOrElse { JSONObject() }
            return ClockOptions(
                TimeFormat.fromName(json.optString(KEY_TIME, ""), defaultTime),
                DateFormat.fromName(json.optString(KEY_DATE, ""), defaultDate)
            )
        }

        fun dotStyle(configJson: String): DotStyle =
            DotStyle.fromName(runCatching { JSONObject(configJson).optString(KEY_DOT_STYLE) }.getOrNull())

        /** IANA time zone stored for a world-clock widget, or null when none is set. */
        fun timeZone(configJson: String): String? =
            runCatching { JSONObject(configJson).optString(KEY_TZ) }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

/** Which clock widgets offer which format options in setup, and their defaults. */
object ClockSupport {
    /** Clocks with a live seconds overlay, so they can offer formats with seconds. */
    val SECONDS_CAPABLE = setOf(
        "clock.seconds.live", "clock.dotdigital.s", "clock.dotdigital.m", "clock.dotdigital.l",
        "clock.world.london", "clock.world.tokyo", "clock.world.nyc", "clock.world.custom"
    )

    /** Clocks that offer a time-zone picker in setup. */
    val TIME_ZONE_PICKERS = setOf("clock.world.custom")

    /** Clocks whose live TextClock overlay needs a zone from configJson. */
    val LIVE_ZONE_FROM_CONFIG = setOf("clock.world.custom")

    /** Clocks that only switch between 12- and 24-hour (no live seconds overlay). */
    private val HOUR_CYCLE_ONLY = setOf("clock.minimal.pill", "clock.flip.style", "clock.stacked.hhmm")

    private val DATE_WIDGETS = SECONDS_CAPABLE + setOf("clock.minimal.pill", "clock.flip.style")

    fun timeFormats(widgetId: String): List<TimeFormat> = when (widgetId) {
        in SECONDS_CAPABLE -> TimeFormat.entries
        in HOUR_CYCLE_ONLY -> listOf(TimeFormat.H24, TimeFormat.H12)
        else -> emptyList()
    }

    fun showsDate(widgetId: String) = widgetId in DATE_WIDGETS

    fun defaultTime(widgetId: String) = if (widgetId == "clock.seconds.live") TimeFormat.H24_SECONDS else TimeFormat.H24

    /** Small digital clocks are time-only unless the user picks a date format. */
    fun defaultDate(widgetId: String, isSmall: Boolean) =
        if (isSmall && widgetId in SECONDS_CAPABLE) DateFormat.NONE else DateFormat.WEEKDAY_DAY_MONTH
}
