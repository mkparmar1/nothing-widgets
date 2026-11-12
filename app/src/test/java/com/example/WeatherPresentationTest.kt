package com.example

import com.example.core.bitmap.SizeBucket
import com.example.core.config.WidgetOption
import com.example.data.weather.HourlyForecast
import com.example.data.weather.WeatherData
import com.example.data.weather.WeatherPresentation
import com.example.widgets.base.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherPresentationTest {

    @Test
    fun `offline label shows coarse cached-data age`() {
        val now = 1_800_000_000_000L
        val data = WeatherData(
            cityName = "Test",
            temperatureC = 20,
            weatherCode = 0,
            description = "Clear",
            fetchedAt = now - 3 * 60 * 60_000L,
            isOffline = true
        )

        assertEquals("OFFLINE • UPDATED 3H AGO", WeatherPresentation.offlineStatus(data, now))
        assertEquals("OFFLINE • NO DATA", WeatherPresentation.offlineStatus(0L, now))
        assertNull(WeatherPresentation.offlineStatus(data.copy(isOffline = false), now))
        assertNull(WeatherPresentation.offlineStatus(now - 60 * 60_000L, false, now))
        assertEquals(
            "OFFLINE • UPDATED 1H AGO",
            WeatherPresentation.offlineStatus(now - 91 * 60_000L, false, now)
        )
    }

    @Test
    fun `forecast alert selects the earliest severe event within the window`() {
        val now = utc("2026-03-10T12:00:00")
        val hourly = listOf(
            HourlyForecast("2026-03-10T11:00", 18, 95, 10),
            HourlyForecast("2026-03-10T14:00", 19, 61, 20),
            HourlyForecast("2026-03-10T16:00", 20, 65, 75),
            HourlyForecast("2026-03-10T19:00", 21, 95, 90)
        )

        val alert = WeatherPresentation.nextSevereForecast(hourly, "UTC", now)

        assertEquals("HEAVY RAIN", alert?.label)
        assertEquals("2026-03-10T16:00", alert?.startsAt)
        assertNull(WeatherPresentation.nextSevereForecast(hourly, "UTC", now, windowHours = 2))
    }

    @Test
    fun `hourly graph and precipitation use upcoming forecast fields`() {
        val now = utc("2026-03-10T12:25:00")
        val hourly = listOf(
            HourlyForecast("2026-03-10T11:00", 35, 95, 99),
            HourlyForecast("2026-03-10T12:00", 23, 0, 0),
            HourlyForecast("2026-03-10T13:00", 18, 61, 45),
            HourlyForecast("2026-03-10T14:00", 26, 0, 110)
        )

        val upcoming = WeatherPresentation.upcomingHours(hourly, "UTC", now, count = 2)

        assertEquals(listOf(23, 18), upcoming.map { it.temperatureC })
        assertEquals(listOf(0, 45), WeatherPresentation.hourlyPrecipitationPercentages(hourly, "UTC", now, count = 2))
        assertTrue(WeatherPresentation.upcomingHours(emptyList(), "UTC", now, count = 2).isEmpty())
        assertTrue(
            WeatherPresentation.hourlyPrecipitationPercentages(emptyList(), "UTC", now, count = 2).isEmpty()
        )
    }

    @Test
    fun `temperature graph normalization handles ranges and flat forecasts`() {
        assertEquals(listOf(0f, 0.5f, 1f), WeatherPresentation.temperaturePositions(listOf(10, 15, 20)))
        assertEquals(listOf(0.5f, 0.5f), WeatherPresentation.temperaturePositions(listOf(7, 7)))
        assertTrue(WeatherPresentation.temperaturePositions(emptyList()).isEmpty())
    }

    @Test
    fun `hourly graph is registered with opt-in severe forecast indicator`() {
        val definition = WidgetRegistry.getById("weather.hourly.graph")

        assertNotNull(definition)
        assertEquals(setOf(SizeBucket.M, SizeBucket.L), definition?.sizes)
        assertNotNull(forecastAlertOption(definition?.options.orEmpty()))
        assertNotNull(WidgetRegistry.getById("weather.hourly.strip"))
        listOf("weather.current.temp", "weather.5day.strip", "weather.hourly.strip", "weather.air.quality")
            .forEach { id ->
                val options = WidgetRegistry.getById(id)?.options.orEmpty()
                assertNotNull("$id should expose the opt-in forecast indicator", forecastAlertOption(options))
                assertFalse(forecastAlertOption(options)?.default ?: true)
            }
    }

    private fun forecastAlertOption(options: List<WidgetOption>) =
        options.filterIsInstance<WidgetOption.Toggle>().firstOrNull { it.key == "show_weather_alert" }

    private fun utc(value: String): Long = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(value)!!.time
}
