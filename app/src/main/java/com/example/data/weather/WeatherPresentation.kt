package com.example.data.weather

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

data class WeatherAlert(val label: String, val startsAt: String)

object WeatherPresentation {

    fun upcomingHours(
        hourly: List<HourlyForecast>,
        timezone: String,
        nowMillis: Long = System.currentTimeMillis(),
        count: Int
    ): List<HourlyForecast> {
        if (hourly.isEmpty() || count <= 0) return emptyList()
        val zone = TimeZone.getTimeZone(timezone)
        val currentHour = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US)
            .apply { timeZone = zone }
            .format(Date(nowMillis))
        val fromNow = hourly.dropWhile { it.time < currentHour }
        return (if (fromNow.isEmpty()) hourly else fromNow).take(count)
    }

    fun hourlyPrecipitationPercentages(
        hourly: List<HourlyForecast>,
        timezone: String,
        nowMillis: Long = System.currentTimeMillis(),
        count: Int
    ): List<Int> = upcomingHours(hourly, timezone, nowMillis, count)
        .map { it.precipitationPct.coerceIn(0, 100) }

    fun offlineStatus(data: WeatherData, nowMillis: Long = System.currentTimeMillis()): String? {
        if (!data.isOffline && !data.isStale) return null
        return offlineStatus(data.fetchedAt, nowMillis)
    }

    fun offlineStatus(fetchedAt: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (fetchedAt <= 0L) return "OFFLINE • NO DATA"
        val ageHours = ((nowMillis - fetchedAt).coerceAtLeast(0L) / 3_600_000L).toInt()
        val age = when {
            ageHours < 1 -> "<1H AGO"
            ageHours < 24 -> "${ageHours}H AGO"
            else -> "${ageHours / 24}D AGO"
        }
        return "OFFLINE • UPDATED $age"
    }

    fun offlineStatus(
        fetchedAt: Long,
        isOffline: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        staleAfterMillis: Long = 90 * 60_000L
    ): String? {
        val age = (nowMillis - fetchedAt).coerceAtLeast(0L)
        if (!isOffline && fetchedAt > 0L && age <= staleAfterMillis) return null
        return offlineStatus(fetchedAt, nowMillis)
    }

    fun nextSevereForecast(
        hourly: List<HourlyForecast>,
        timezone: String,
        nowMillis: Long = System.currentTimeMillis(),
        windowHours: Int = 6
    ): WeatherAlert? {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone(timezone)
        }
        val cutoff = nowMillis + windowHours * 3_600_000L
        return hourly.asSequence()
            .mapNotNull { forecast ->
                val startsAt = runCatching { parser.parse(forecast.time)?.time }.getOrNull()
                    ?: return@mapNotNull null
                val label = when (forecast.weatherCode) {
                    95, 96, 99 -> "THUNDERSTORM"
                    65, 82 -> "HEAVY RAIN"
                    75, 86 -> "HEAVY SNOW"
                    67 -> "FREEZING RAIN"
                    else -> null
                } ?: return@mapNotNull null
                if (startsAt < nowMillis || startsAt > cutoff) return@mapNotNull null
                WeatherAlert(label, forecast.time)
            }
            .minByOrNull {
                runCatching { parser.parse(it.startsAt)?.time ?: Long.MAX_VALUE }.getOrDefault(Long.MAX_VALUE)
            }
    }

    fun alertBadge(alert: WeatherAlert, timezone: String, nowMillis: Long = System.currentTimeMillis()): String {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(timezone)
        }
        val startsAt = runCatching { parser.parse(alert.startsAt)?.time }.getOrNull()
            ?: return alert.label
        val hours = ceil((startsAt - nowMillis).coerceAtLeast(0L) / 3_600_000.0).toInt()
        return if (hours == 0) "${alert.label} SOON" else "${alert.label} ${hours}H"
    }

    fun temperaturePositions(values: List<Int>): List<Float> {
        if (values.isEmpty()) return emptyList()
        val low = values.minOrNull() ?: return emptyList()
        val high = values.maxOrNull() ?: return emptyList()
        if (high == low) return List(values.size) { 0.5f }
        return values.map { (it - low).toFloat() / (high - low) }
    }
}
