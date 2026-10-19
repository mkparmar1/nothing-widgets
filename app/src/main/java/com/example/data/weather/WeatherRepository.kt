package com.example.data.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.resume

data class WeatherData(
    val cityName: String,
    val temperatureC: Int,
    val weatherCode: Int,
    val description: String,
    val humidityPct: Int = 65,
    val windKmh: Int = 14,
    val highC: Int = 22,
    val lowC: Int = 14,
    val feelsLikeC: Int = temperatureC,
    val uvIndex: Double = 0.0,
    val daily: List<DailyForecast> = emptyList(),
    val hourly: List<HourlyForecast> = emptyList(),
    val isStale: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)

data class DailyForecast(
    /** yyyy-MM-dd in the city's local time. */
    val date: String,
    val weatherCode: Int,
    val highC: Int,
    val lowC: Int,
    val precipitationPct: Int
)

data class HourlyForecast(
    /** yyyy-MM-ddTHH:mm in the city's local time. */
    val time: String,
    val temperatureC: Int,
    val weatherCode: Int,
    val precipitationPct: Int,
    /** True if Open-Meteo marks this hour as daytime. */
    val isDay: Boolean = true
)

/** A named location weather can be fetched for. */
data class WeatherPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
    val timezone: String,
    /** Extra context shown in search results, e.g. "Gujarat, India". */
    val region: String = ""
)

object WeatherRepository {

    /** Fixed cities used by the London / New York / Tokyo widgets and the time-zone fallback. */
    val PRESET_CITIES = listOf(
        WeatherPlace("London", 51.5074, -0.1278, "Europe/London"),
        WeatherPlace("New York", 40.7128, -74.0060, "America/New_York"),
        WeatherPlace("Tokyo", 35.6762, 139.6503, "Asia/Tokyo"),
        WeatherPlace("Berlin", 52.5200, 13.4050, "Europe/Berlin"),
        WeatherPlace("Paris", 48.8566, 2.3522, "Europe/Paris"),
        WeatherPlace("San Francisco", 37.7749, -122.4194, "America/Los_Angeles"),
        WeatherPlace("Sydney", -33.8688, 151.2093, "Australia/Sydney"),
        WeatherPlace("Mumbai", 19.0760, 72.8777, "Asia/Kolkata")
    )

    private val TIMEZONE_ALIASES = mapOf("Asia/Calcutta" to "Asia/Kolkata")
    private const val PREFS = "dot_widgets_weather"

    private val cache = ConcurrentHashMap<String, WeatherData>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationExecutor = Executors.newSingleThreadExecutor()

    // ---------- Location ----------

    /** The user's chosen place, or the preset city matching the phone's time zone. */
    fun currentPlace(context: Context): WeatherPlace = savedPlace(context) ?: timezonePlace()

    /** True when the user has picked a city or shared their location. */
    fun hasChosenPlace(context: Context): Boolean = savedPlace(context) != null

    fun setPlace(context: Context, place: WeatherPlace?) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (place == null) {
            editor.clear()
        } else {
            editor.putString("name", place.name)
                .putString("lat", place.lat.toString())
                .putString("lon", place.lon.toString())
                .putString("tz", place.timezone)
                .putString("region", place.region)
        }
        editor.apply()
        BitmapCache.clear()
        val appContext = context.applicationContext
        scope.launch {
            fetchFreshWeather(currentPlace(appContext))
            WidgetRenderEngine.updateAllPlacedWidgets(appContext)
        }
    }

    private fun savedPlace(context: Context): WeatherPlace? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString("name", null) ?: return null
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lon = p.getString("lon", null)?.toDoubleOrNull() ?: return null
        return WeatherPlace(name, lat, lon, p.getString("tz", null) ?: TimeZone.getDefault().id, p.getString("region", "") ?: "")
    }

    private fun timezonePlace(): WeatherPlace {
        val tz = TimeZone.getDefault().id.let { TIMEZONE_ALIASES[it] ?: it }
        return PRESET_CITIES.find { it.timezone == tz } ?: PRESET_CITIES.first()
    }

    fun preset(name: String): WeatherPlace =
        PRESET_CITIES.find { it.name.equals(name, ignoreCase = true) } ?: PRESET_CITIES.first()

    /** City search via the Open-Meteo geocoding API. */
    suspend fun searchPlaces(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val json = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=en&format=json")
                ?: return@withContext emptyList()
            val results = JSONObject(json).optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).map { i ->
                val o = results.getJSONObject(i)
                WeatherPlace(
                    name = o.getString("name"),
                    lat = o.getDouble("latitude"),
                    lon = o.getDouble("longitude"),
                    timezone = o.optString("timezone", TimeZone.getDefault().id),
                    region = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", ")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Coarse device location turned into a named place. Caller must hold ACCESS_COARSE_LOCATION. */
    @SuppressLint("MissingPermission")
    suspend fun placeFromDevice(context: Context): WeatherPlace? = withContext(Dispatchers.IO) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val location = try {
            currentLocation(lm) ?: lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        } ?: return@withContext null

        val name = try {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (_: Exception) {
            null
        } ?: "My Location"
        WeatherPlace(name, location.latitude, location.longitude, TimeZone.getDefault().id)
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(lm: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                lm.getCurrentLocation(provider, signal, locationExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
            }
        }
    }

    // ---------- Weather ----------

    /**
     * Synchronous read for Canvas rendering; never blocks. Starts a background refresh when the
     * data is older than 15 minutes and redraws widgets once it arrives. [cityName] selects a
     * preset city; null means the user's current place.
     */
    fun getWeatherInstant(context: Context, cityName: String? = null): WeatherData {
        val place = if (cityName == null) currentPlace(context) else preset(cityName)
        val key = keyFor(place)
        val cached = cache[key] ?: placeholder(place)

        val now = System.currentTimeMillis()
        if (now - cached.fetchedAt > 15 * 60 * 1000L && inFlight.add(key)) {
            val appContext = context.applicationContext
            scope.launch {
                try {
                    val fresh = fetchFreshWeather(place)
                    if (!fresh.isStale && fresh.fetchedAt > cached.fetchedAt) {
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(appContext)
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return cached
    }

    /** Fresh fetch for the in-app weather sheet. */
    suspend fun getWeather(context: Context, cityName: String? = null): WeatherData =
        fetchFreshWeather(if (cityName == null) currentPlace(context) else preset(cityName))

    private fun keyFor(place: WeatherPlace) = "${place.name.lowercase()}@${"%.2f".format(Locale.US, place.lat)},${"%.2f".format(Locale.US, place.lon)}"

    /** Shown until the first fetch succeeds; stale so widgets display "--" rather than made-up numbers. */
    private fun placeholder(place: WeatherPlace) = WeatherData(
        cityName = place.name,
        temperatureC = 0,
        weatherCode = 1,
        description = "Loading",
        isStale = true,
        fetchedAt = 0L
    )

    private suspend fun fetchFreshWeather(place: WeatherPlace): WeatherData = withContext(Dispatchers.IO) {
        val key = keyFor(place)
        val cached = cache[key]
        val now = System.currentTimeMillis()

        try {
            val response = httpGet(
                "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                    "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,uv_index" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
                    "&forecast_days=5&timezone=auto"
            )
            if (response != null) {
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")

                val days = mutableListOf<DailyForecast>()
                json.optJSONObject("daily")?.let { daily ->
                    val dates = daily.optJSONArray("time")
                    val codes = daily.optJSONArray("weather_code")
                    val highs = daily.optJSONArray("temperature_2m_max")
                    val lows = daily.optJSONArray("temperature_2m_min")
                    val rain = daily.optJSONArray("precipitation_probability_max")
                    for (i in 0 until (dates?.length() ?: 0)) {
                        days += DailyForecast(
                            date = dates!!.optString(i),
                            weatherCode = codes?.optInt(i, code) ?: code,
                            highC = highs?.optDouble(i, temp.toDouble())?.toInt() ?: temp,
                            lowC = lows?.optDouble(i, temp.toDouble())?.toInt() ?: temp,
                            precipitationPct = rain?.optInt(i, 0) ?: 0
                        )
                    }
                }

                val hours = mutableListOf<HourlyForecast>()
                json.optJSONObject("hourly")?.let { hourly ->
                    val times = hourly.optJSONArray("time")
                    val temps = hourly.optJSONArray("temperature_2m")
                    val hourCodes = hourly.optJSONArray("weather_code")
                    val hourRain = hourly.optJSONArray("precipitation_probability")
                    val isDay = hourly.optJSONArray("is_day")
                    for (i in 0 until (times?.length() ?: 0)) {
                        hours += HourlyForecast(
                            time = times!!.optString(i),
                            temperatureC = temps?.optDouble(i, temp.toDouble())?.toInt() ?: temp,
                            weatherCode = hourCodes?.optInt(i, code) ?: code,
                            precipitationPct = hourRain?.optInt(i, 0) ?: 0,
                            isDay = (isDay?.optInt(i, 1) ?: 1) == 1
                        )
                    }
                }

                val data = WeatherData(
                    cityName = place.name,
                    temperatureC = temp,
                    weatherCode = code,
                    description = weatherCodeToDescription(code),
                    humidityPct = current.optInt("relative_humidity_2m", 0),
                    windKmh = current.optDouble("wind_speed_10m", 0.0).toInt(),
                    highC = days.firstOrNull()?.highC ?: temp,
                    lowC = days.firstOrNull()?.lowC ?: temp,
                    feelsLikeC = current.optDouble("apparent_temperature", temp.toDouble()).toInt(),
                    uvIndex = current.optDouble("uv_index", 0.0),
                    daily = days,
                    hourly = hours,
                    isStale = false,
                    fetchedAt = now
                )
                cache[key] = data
                return@withContext data
            }
        } catch (_: Exception) {
        }

        // Offline: keep the last good data, marking it stale after 90 minutes.
        val fallback = cached?.copy(isStale = now - cached.fetchedAt > 90 * 60 * 1000L) ?: placeholder(place)
        cache[key] = fallback
        fallback
    }

    private fun httpGet(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode == 200) connection.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            connection.disconnect()
        }
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
