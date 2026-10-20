package com.example.data.weather

import android.content.Context
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class AirQuality(
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val place: String,
    val fetchedAt: Long
) {
    /** US EPA AQI category. */
    val category: String
        get() = when {
            usAqi <= 50 -> "GOOD"
            usAqi <= 100 -> "MODERATE"
            usAqi <= 150 -> "UNHEALTHY FOR SOME"
            usAqi <= 200 -> "UNHEALTHY"
            usAqi <= 300 -> "VERY UNHEALTHY"
            else -> "HAZARDOUS"
        }
}

/** Air quality from the free Open-Meteo air-quality API, for the user's weather place; refreshed every 30 min. */
object AirQualityRepository {

    @Volatile private var cached: AirQuality? = null
    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Never blocks; starts a background refresh when missing, stale or for a different place. */
    fun current(context: Context): AirQuality? {
        val place = WeatherRepository.currentPlace(context)
        val c = cached
        val stale = c == null || c.place != place.name || System.currentTimeMillis() - c.fetchedAt > 30 * 60_000L
        if (stale && inFlight.compareAndSet(false, true)) {
            val app = context.applicationContext
            scope.launch {
                try {
                    fetch(place)?.let {
                        cached = it
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(app)
                    }
                } finally {
                    inFlight.set(false)
                }
            }
        }
        return c?.takeIf { it.place == place.name }
    }

    private fun fetch(place: WeatherPlace): AirQuality? = try {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${place.lat}&longitude=${place.lon}&current=us_aqi,pm2_5,pm10"
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 5000; readTimeout = 5000 }
        try {
            if (c.responseCode != 200) null
            else {
                val cur = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
                AirQuality(cur.optInt("us_aqi", 0), cur.optDouble("pm2_5", 0.0), cur.optDouble("pm10", 0.0), place.name, System.currentTimeMillis())
            }
        } finally {
            c.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}
