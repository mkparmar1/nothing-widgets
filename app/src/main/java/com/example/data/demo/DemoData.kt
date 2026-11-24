package com.example.data.demo

import android.graphics.Color
import com.example.data.battery.BatteryStatus
import com.example.data.calendar.CalendarEvent
import com.example.data.media.NowPlaying
import com.example.data.system.DeviceStatsRepository
import com.example.data.usage.ScreenTime
import com.example.data.weather.DailyForecast
import com.example.data.weather.HourlyForecast
import com.example.data.weather.WeatherData
import com.example.data.weather.WeatherPlace
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Opt-in realistic render fixtures for snapshot tests and preview tooling. */
object DemoData {

    data class Snapshot(
        val battery: BatteryStatus,
        val weatherPlace: WeatherPlace,
        val weather: WeatherData,
        val calendarEvents: List<CalendarEvent>,
        val nowPlaying: NowPlaying,
        val steps: Int,
        val screenTime: ScreenTime,
        val batteryHistory: List<DeviceStatsRepository.BatterySample>
    ) {
        fun weatherFor(cityName: String?): WeatherData {
            val name = cityName?.takeIf { it.isNotBlank() } ?: weatherPlace.name
            return when (name.lowercase(Locale.US)) {
                "london" -> weather.copy(cityName = name, temperatureC = 12, weatherCode = 3, description = "Overcast")
                "tokyo" -> weather.copy(cityName = name, temperatureC = 19, weatherCode = 2, description = "Partly cloudy")
                "new york" -> weather.copy(cityName = name, temperatureC = 16, weatherCode = 61, description = "Light rain")
                else -> weather.copy(cityName = name)
            }
        }
    }

    private val activeSnapshot = ThreadLocal<Snapshot?>()

    fun current(): Snapshot? = activeSnapshot.get()

    fun <T> withSnapshot(block: () -> T): T {
        val previous = activeSnapshot.get()
        activeSnapshot.set(createSnapshot())
        return try {
            block()
        } finally {
            if (previous == null) activeSnapshot.remove() else activeSnapshot.set(previous)
        }
    }

    private fun createSnapshot(): Snapshot {
        val place = WeatherPlace("Mumbai", 19.0760, 72.8777, "Asia/Kolkata", "Maharashtra, India")
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val hour = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date(now))
            .substringAfter('T').toInt()
        val weather = WeatherData(
            cityName = place.name,
            temperatureC = 27,
            weatherCode = 0,
            description = "Clear",
            humidityPct = 72,
            windKmh = 11,
            windDirectionDeg = 270,
            pressureHpa = 1012,
            highC = 30,
            lowC = 24,
            feelsLikeC = 29,
            uvIndex = 5.2,
            daily = (0..4).map { day ->
                val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day) }
                DailyForecast(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.time),
                    weatherCode = if (day == 1) 2 else 0,
                    highC = 29 + day % 2,
                    lowC = 23 + day % 2,
                    precipitationPct = if (day == 1) 20 else 5
                )
            },
            hourly = (0..11).map { offset ->
                val date = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, offset) }
                HourlyForecast(
                    time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(date.time),
                    temperatureC = 26 + ((hour + offset) % 5),
                    weatherCode = 0,
                    precipitationPct = 0
                )
            },
            fetchedAt = now
        )
        val events = listOf(
            CalendarEvent("Morning stand-up", now + 30 * 60_000L, now + 60 * 60_000L, false, Color.RED),
            CalendarEvent("Design review", now + 2 * 60 * 60_000L, now + 3 * 60 * 60_000L, false, Color.BLUE),
            CalendarEvent("Send project update", now + 5 * 60 * 60_000L, now + 5 * 60 * 60_000L + 30 * 60_000L, false, Color.GREEN)
        )
        return Snapshot(
            battery = BatteryStatus(
                level = 76,
                isCharging = true,
                isPlugged = true,
                temperatureC = 28.1f,
                health = "GOOD",
                plugType = "USB",
                voltageMv = 4200,
                chargeTimeRemainingMs = 45 * 60_000L
            ),
            weatherPlace = place,
            weather = weather,
            calendarEvents = events,
            nowPlaying = NowPlaying(
                title = "Midnight City",
                artist = "M83",
                appPackage = "com.example.demo",
                isPlaying = true,
                positionMs = 92_000,
                durationMs = 243_000,
                art = null
            ),
            steps = 6_240,
            screenTime = ScreenTime(
                screenOnMs = 2 * 60 * 60_000L + 18 * 60_000L,
                unlocks = 42,
                lastSevenDaysMs = listOf(90, 130, 110, 165, 145, 190, 138).map { it * 60_000L }
            ),
            batteryHistory = listOf(38, 42, 40, 51, 47, 62, 70, 76).mapIndexed { index, level ->
                DeviceStatsRepository.BatterySample(
                    timeMs = now - (7 - index) * 3 * 60 * 60_000L,
                    level = level,
                    charging = index >= 5
                )
            }
        )
    }
}
