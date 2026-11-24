package com.example.widgets.styles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.time.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Small shared formatting/data helpers for the style renderers (no state, no I/O). */
object StyleData {

    const val SUN_COLOR = 0xFFFFB020.toInt()

    /** Material glyph for a WMO weather code (Open-Meteo). */
    fun weatherIcon(code: Int, isDay: Boolean = true): ImageVector = when (code) {
        0 -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
        1, 2 -> if (isDay) Icons.Filled.WbCloudy else Icons.Filled.NightsStay
        3 -> Icons.Filled.Cloud
        45, 48 -> Icons.Filled.Dehaze
        51, 53, 55, 56, 57 -> Icons.Filled.Grain
        61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Filled.Umbrella
        71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit
        95, 96, 99 -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.Cloud
    }

    /** Sunny codes get the warm sun colour; everything else uses the card's ink. */
    fun weatherIconColor(code: Int, isDay: Boolean, ink: Int): Int =
        if (isDay && code in 0..2) SUN_COLOR else ink

    /** "Partly Cloudy" from the repository's upper-case description. */
    fun titleCase(text: String): String = text.lowercase(Locale.US).split(' ')
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }

    /** "New York" from "America/New_York". */
    fun cityFromZone(zoneId: String?): String {
        if (zoneId.isNullOrBlank()) return "Local"
        return zoneId.substringAfterLast('/').replace('_', ' ')
    }

    /** "GMT +5:30" / "GMT -5" for [zoneId] right now. */
    fun gmtLabel(zoneId: String?, nowMs: Long = System.currentTimeMillis()): String {
        val tz = if (zoneId.isNullOrBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(zoneId)
        val offsetMin = tz.getOffset(nowMs) / 60_000
        val sign = if (offsetMin < 0) "-" else "+"
        val h = abs(offsetMin) / 60
        val m = abs(offsetMin) % 60
        return if (m == 0) "GMT $sign$h" else String.format(Locale.US, "GMT %s%d:%02d", sign, h, m)
    }

    /** "Today" / "Tomorrow" / "Yesterday" for a zone relative to the phone's own day. */
    fun dayRelative(zoneId: String?, nowMs: Long = System.currentTimeMillis()): String {
        if (zoneId.isNullOrBlank()) return "Today"
        val local = Calendar.getInstance().apply { timeInMillis = nowMs }
        val there = Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply { timeInMillis = nowMs }
        val diff = (there.get(Calendar.YEAR) * 400 + there.get(Calendar.DAY_OF_YEAR)) -
            (local.get(Calendar.YEAR) * 400 + local.get(Calendar.DAY_OF_YEAR))
        return when {
            diff > 0 -> "Tomorrow"
            diff < 0 -> "Yesterday"
            else -> "Today"
        }
    }

    /** Time text in [format] plus a separate AM/PM (null for 24-hour). */
    fun time(cal: Calendar, format: TimeFormat): Pair<String, String?> = format.format(cal) to format.amPm(cal)

    /** hh:mm string for minutes after midnight. */
    fun minutesLabel(minutes: Int?, h24: Boolean = true): String {
        if (minutes == null) return "--:--"
        val m = ((minutes % 1440) + 1440) % 1440
        if (h24) return String.format(Locale.US, "%02d:%02d", m / 60, m % 60)
        val h = (m / 60) % 12
        return String.format(Locale.US, "%d:%02d %s", if (h == 0) 12 else h, m % 60, if (m < 720) "AM" else "PM")
    }

    /** "2h 45m" from minutes. */
    fun duration(minutes: Int): String = when {
        minutes <= 0 -> "0m"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    fun format(pattern: String, cal: Calendar): String =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = cal.timeZone }.format(cal.time)

    /** Hours of the angle of a clock hand: (hour, minute) → degrees for hour and minute hands. */
    fun handAngles(cal: Calendar): Pair<Float, Float> {
        val h = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
        val m = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        return h * 30f to m * 6f
    }
}
