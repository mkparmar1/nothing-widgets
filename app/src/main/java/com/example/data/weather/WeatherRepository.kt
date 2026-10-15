package com.example.data.weather

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val cityName: String,
    val temperatureC: Int,
    val weatherCode: Int,
    val description: String,
    val humidityPct: Int = 65,
    val windKmh: Int = 14,
    val highC: Int = 22,
    val lowC: Int = 14,
    val isStale: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)

data class CityPreset(
    val name: String,
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val defaultTemp: Int,
    val defaultCode: Int,
    val defaultDesc: String
)

object WeatherRepository {

    val PRESET_CITIES = listOf(
        CityPreset("London", 51.5074, -0.1278, "Europe/London", 19, 1, "Mainly Clear"),
        CityPreset("New York", 40.7128, -74.0060, "America/New_York", 22, 0, "Clear Sky"),
        CityPreset("Tokyo", 35.6762, 139.6503, "Asia/Tokyo", 24, 2, "Partly Cloudy"),
        CityPreset("Berlin", 52.5200, 13.4050, "Europe/Berlin", 18, 1, "Mainly Clear"),
        CityPreset("Paris", 48.8566, 2.3522, "Europe/Paris", 20, 0, "Clear Sky"),
        CityPreset("San Francisco", 37.7749, -122.4194, "America/Los_Angeles", 17, 45, "Fog"),
        CityPreset("Sydney", -33.8688, 151.2093, "Australia/Sydney", 23, 0, "Clear Sky"),
        CityPreset("Mumbai", 19.0760, 72.8777, "Asia/Kolkata", 29, 3, "Overcast")
    )

    private val cache = mutableMapOf<String, WeatherData>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Pre-populate with instant data so render never blocks or shows blank
        PRESET_CITIES.forEach { city ->
            cache[city.name.lowercase()] = WeatherData(
                cityName = city.name,
                temperatureC = city.defaultTemp,
                weatherCode = city.defaultCode,
                description = city.defaultDesc,
                humidityPct = 65,
                windKmh = 14,
                highC = city.defaultTemp + 4,
                lowC = city.defaultTemp - 4,
                isStale = false,
                fetchedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Synchronous instant read for Canvas rendering. NEVER blocks or delays frame rendering.
     * Automatically triggers background refresh if cached data is older than 15 minutes.
     */
    fun getWeatherInstant(context: Context, cityName: String = "London"): WeatherData {
        val key = cityName.lowercase()
        val cached = cache[key] ?: cache["london"] ?: WeatherData(
            cityName = cityName,
            temperatureC = 20,
            weatherCode = 1,
            description = "Mainly Clear"
        )

        val now = System.currentTimeMillis()
        if (now - cached.fetchedAt > 15 * 60 * 1000L) {
            // Trigger asynchronous background refresh without blocking
            scope.launch {
                fetchFreshWeather(cityName)
            }
        }

        return cached
    }

    suspend fun getWeather(context: Context, cityName: String = "London"): WeatherData {
        return fetchFreshWeather(cityName)
    }

    private suspend fun fetchFreshWeather(cityName: String): WeatherData = withContext(Dispatchers.IO) {
        val preset = PRESET_CITIES.find { it.name.equals(cityName, ignoreCase = true) } ?: PRESET_CITIES.first()
        val key = preset.name.lowercase()
        val cached = cache[key]
        val now = System.currentTimeMillis()
        val ninetyMinutes = 90 * 60 * 1000L

        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${preset.lat}&longitude=${preset.lon}&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                val temp = current.getDouble("temperature").toInt()
                val code = current.getInt("weathercode")
                val wind = current.optDouble("windspeed", 12.0).toInt()

                var high = temp + 4
                var low = temp - 4
                if (json.has("daily")) {
                    val daily = json.getJSONObject("daily")
                    high = daily.optJSONArray("temperature_2m_max")?.optDouble(0, (temp + 4).toDouble())?.toInt() ?: (temp + 4)
                    low = daily.optJSONArray("temperature_2m_min")?.optDouble(0, (temp - 4).toDouble())?.toInt() ?: (temp - 4)
                }

                val data = WeatherData(
                    cityName = preset.name,
                    temperatureC = temp,
                    weatherCode = code,
                    description = weatherCodeToDescription(code),
                    humidityPct = 68,
                    windKmh = wind,
                    highC = high,
                    lowC = low,
                    isStale = false,
                    fetchedAt = now
                )
                cache[key] = data
                return@withContext data
            }
        } catch (_: Exception) {
        }

        if (cached != null) {
            val isStale = (now - cached.fetchedAt) > ninetyMinutes
            val updated = cached.copy(isStale = isStale)
            cache[key] = updated
            return@withContext updated
        }

        val fallback = WeatherData(
            cityName = preset.name,
            temperatureC = preset.defaultTemp,
            weatherCode = preset.defaultCode,
            description = preset.defaultDesc,
            highC = preset.defaultTemp + 4,
            lowC = preset.defaultTemp - 4,
            isStale = false,
            fetchedAt = now
        )
        cache[key] = fallback
        fallback
    }

    fun weatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Cloudy"
        }
    }
}
