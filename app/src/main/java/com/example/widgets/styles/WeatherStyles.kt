package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbTwilight
import com.example.core.astro.Astronomy
import com.example.data.weather.AirQualityRepository
import com.example.data.weather.WeatherData
import com.example.data.weather.WeatherPresentation
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Extra designs for the weather family (Batch 1, items 22–38). */
object WeatherStyles {

    private fun sunTimes(id: String) = listOf(
        WidgetStyle("soft_sun_times", "Sun Times Card", StyleFamily.SOFT) { c, s, cv -> renderSunTimes(c, s, cv, StyleFamily.SOFT, id) },
        WidgetStyle("bold_sun_times", "Bold Sun Times", StyleFamily.BOLD) { c, s, cv -> renderSunTimes(c, s, cv, StyleFamily.BOLD, id) }
    )

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "weather.current.temp" to listOf(
            WidgetStyle("bold_cloud", "Bold Weather", StyleFamily.BOLD) { c, s, cv -> renderNow(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_sun", "Feels Like Card", StyleFamily.SOFT, render = ::renderFeelsCard),
            WidgetStyle("soft_city", "City Card", StyleFamily.SOFT) { c, s, cv -> renderNow(c, s, cv, StyleFamily.SOFT) }
        ),
        "weather.5day.strip" to listOf(
            WidgetStyle("soft_days", "Soft Days", StyleFamily.SOFT) { c, s, cv -> renderDays(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_days", "Bold Days", StyleFamily.BOLD) { c, s, cv -> renderDays(c, s, cv, StyleFamily.BOLD) }
        ),
        "weather.hourly.strip" to listOf(
            WidgetStyle("soft_hourly", "Soft Hourly", StyleFamily.SOFT) { c, s, cv -> renderHourly(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_hourly", "Bold Hourly", StyleFamily.BOLD) { c, s, cv -> renderHourly(c, s, cv, StyleFamily.BOLD) }
        ),
        "weather.uv.index" to listOf(
            WidgetStyle("soft_gauge", "Soft Gauge", StyleFamily.SOFT, render = ::renderUv)
        ),
        "weather.aqi.dial" to listOf(
            WidgetStyle("soft_arc", "Soft Arc", StyleFamily.SOFT) { c, s, cv -> renderAqi(c, s, cv, estimated = true) }
        ),
        "weather.air.quality" to listOf(
            WidgetStyle("soft_arc", "Soft Arc", StyleFamily.SOFT) { c, s, cv -> renderAqi(c, s, cv, estimated = false) }
        ),
        "weather.pressure.gauge" to listOf(
            WidgetStyle("soft_gauge", "Soft Gauge", StyleFamily.SOFT, render = ::renderPressure)
        ),
        "weather.wind.compass" to listOf(
            WidgetStyle("soft_dial", "Wind Dial", StyleFamily.SOFT, render = ::renderWind)
        ),
        "weather.sunrise.arc" to sunTimes("weather.sunrise.arc"),
        "travel.sun.times" to sunTimes("travel.sun.times"),
        "weather.moon.phase" to listOf(
            WidgetStyle("bold_moon", "Bold Moon", StyleFamily.BOLD) { c, s, cv -> renderMoon(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_moon", "Soft Moon", StyleFamily.SOFT) { c, s, cv -> renderMoon(c, s, cv, StyleFamily.SOFT) }
        ),
        "weather.feels.humidity" to listOf(
            WidgetStyle("thermometer", "Thermometer", StyleFamily.SOFT, render = ::renderThermometer)
        ),
        "weather.golden.hour" to listOf(
            WidgetStyle("soft_golden", "Soft Card", StyleFamily.SOFT, render = ::renderGolden)
        )
    )

    // ------------------------------------------------------------------ helpers

    private fun weather(context: Context): WeatherData = WeatherRepository.getWeatherInstant(context)

    /** City line, or the offline status when the data is stale (same rule as the classic widgets). */
    private fun placeLine(data: WeatherData): String =
        WeatherPresentation.offlineStatus(data)?.let { StyleData.titleCase(it) } ?: StyleData.titleCase(data.cityName)

    /** "27°", or "--°" while there is no fresh data (the same stale guard as the classic widgets). */
    private fun temp(data: WeatherData, unit: String = "°"): String = if (data.isStale) "--$unit" else "${data.temperatureC}$unit"

    /** The severe-weather badge when the user turned that option on and one is forecast. */
    private fun alert(context: Context, spec: WidgetSpec, data: WeatherData): String? {
        if (data.isStale || !spec.options.boolean("show_weather_alert", false)) return null
        val tz = WeatherRepository.currentPlace(context).timezone
        return WeatherPresentation.nextSevereForecast(data.hourly, tz)?.let { StyleData.titleCase(WeatherPresentation.alertBadge(it, tz)) }
    }

    /** Card title, or the alert in red when there is one. */
    private fun titleOrAlert(k: StyleKit, context: Context, title: String, data: WeatherData) {
        val alert = alert(context, k.spec, data)
        k.title(alert ?: title, if (alert != null) k.negative else k.ink)
    }

    private fun nowIcon(k: StyleKit, data: WeatherData, x: Float, y: Float, size: Float) =
        if (data.isStale) k.icon(Icons.Filled.CloudOff, x, y, size, k.faint) else weatherIcon(k, data.weatherCode, x, y, size)

    private fun weatherIcon(k: StyleKit, code: Int, x: Float, y: Float, size: Float, isDay: Boolean = true) =
        k.icon(StyleData.weatherIcon(code, isDay), x, y, size, StyleData.weatherIconColor(code, isDay, k.ink))

    private fun day(dateIso: String): String = runCatching {
        SimpleDateFormat("EEE", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso)!!)
    }.getOrDefault("")

    private fun hourLabel(timeIso: String): String = runCatching {
        val t = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(timeIso)!!
        SimpleDateFormat("ha", Locale.US).format(t)
    }.getOrDefault("")

    // ------------------------------------------------------------- renderers

    /** "☁ 24° / Partly Cloudy / 📍 Ahmedabad" (mockup weather tiles). */
    fun renderNow(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val data = weather(context)
        val details = if (!data.isStale && spec.options.boolean("show_details", true)) " · ${data.highC}°/${data.lowC}°" else ""
        val desc = (if (data.isStale) "No data yet" else StyleData.titleCase(data.description)) + details
        val warning = alert(context, spec, data)
        if (k.isWide) {
            val iconSize = (k.bottom - k.top) * 0.62f
            nowIcon(k, data, k.left + iconSize / 2f, k.cy - 8f * k.u, iconSize)
            val x = k.left + iconSize + 8f * k.u
            val w = k.right - x
            k.text(temp(data), x, k.cy - 3f * k.u, 34f, k.ink, Weight.SEMIBOLD, maxWidth = w)
            k.text(desc, x, k.cy + 16f * k.u, 11f, k.ink, Weight.MEDIUM, maxWidth = w)
            drawPlace(k, data, k.left, k.bottom - 1f * k.u, k.contentW, warning)
            return
        }
        val iconSize = 22f * k.u
        nowIcon(k, data, k.left + iconSize / 2f, k.top + 4f * k.u + iconSize / 2f, iconSize)
        k.text(temp(data), k.left + iconSize + 4f * k.u, k.top + 4f * k.u + iconSize * 0.88f, 24f, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW - iconSize - 4f * k.u - k.cornerReserve)
        k.text(desc, k.left, k.cy + 14f * k.u, 11.5f, k.ink, Weight.MEDIUM)
        drawPlace(k, data, k.left, k.bottom - 1f * k.u, k.contentW, warning)
    }

    private fun drawPlace(k: StyleKit, data: WeatherData, x: Float, baseline: Float, maxW: Float, alert: String? = null) {
        if (alert != null) {
            k.text(alert, x, baseline, 9.5f, k.negative, Weight.MEDIUM, maxWidth = maxW)
            return
        }
        val pin = 9f * k.u
        k.icon(Icons.Filled.LocationOn, x + pin / 2f, baseline - pin * 0.38f, pin, k.sub)
        k.text(placeLine(data), x + pin + 2f * k.u, baseline, 9.5f, k.sub, Weight.MEDIUM, maxWidth = maxW - pin - 2f * k.u)
    }

    private fun feels(data: WeatherData) = if (data.isStale) "Feels like --°" else "Feels like ${data.feelsLikeC}°"

    /** "☀ 24°C / Feels like 26°" soft card. */
    fun renderFeelsCard(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.55f else 24f * k.u
        val iconX = k.left + iconSize / 2f
        val iconY = if (k.isWide) k.cy else k.top + iconSize / 2f + 2f * k.u
        nowIcon(k, data, iconX, iconY, iconSize)
        val x = if (k.isWide) k.left + iconSize + 8f * k.u else k.left
        val w = k.right - x
        val tempY = if (k.isWide) k.cy + 0f * k.u else k.cy + 16f * k.u
        k.text(temp(data, "°C"), x, tempY, if (k.isWide) 30f else 24f, k.ink, Weight.SEMIBOLD, maxWidth = w)
        val warning = alert(context, spec, data)
        k.text(warning ?: feels(data), x, if (k.isWide) k.cy + 20f * k.u else k.bottom - 1f * k.u, 10f, if (warning != null) k.negative else k.sub, Weight.MEDIUM, maxWidth = w)
    }

    /** "Wed Thu Fri Sat" forecast columns (mockup forecast strip). */
    fun renderDays(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val data = weather(context)
        val days = data.daily.drop(1).take(if (k.isLarge) 5 else 4).ifEmpty { data.daily.take(4) }
        if (days.isEmpty()) {
            k.text("Forecast loading…", k.cx, k.cy, 11f, k.sub, Weight.MEDIUM, Align.CENTER)
            return
        }
        val warning = alert(context, spec, data)
        if (k.isLarge) {
            // Square tile: one row per day with its low–high range.
            val top = if (warning != null) k.top + 12f * k.u else k.top
            if (warning != null) k.text(warning, k.left, k.top + 8f * k.u, 10f, k.negative, Weight.MEDIUM, maxWidth = k.contentW - k.cornerReserve)
            val rowH = (k.bottom - top) / days.size
            val min = days.minOf { it.lowC }
            val range = (days.maxOf { it.highC } - min).coerceAtLeast(1)
            days.forEachIndexed { i, f ->
                val y = top + rowH * i + rowH / 2f
                val base = y + 4.5f * k.u
                k.text(day(f.date), k.left, base, 12.5f, k.ink, Weight.MEDIUM, maxWidth = k.contentW * 0.2f)
                weatherIcon(k, f.weatherCode, k.left + k.contentW * 0.3f, y, minOf(rowH * 0.62f, 18f * k.u))
                k.text("${f.lowC}°", k.left + k.contentW * 0.52f, base, 12f, k.sub, Weight.MEDIUM, Align.RIGHT, k.contentW * 0.13f)
                val bl = k.left + k.contentW * 0.56f
                val bw = k.contentW * 0.26f
                k.roundRect(bl, y - 2f * k.u, bl + bw, y + 2f * k.u, 2f * k.u, k.track)
                val fl = bl + bw * (f.lowC - min) / range
                val fr = maxOf(bl + bw * (f.highC - min) / range, fl + 4f * k.u)
                k.roundRect(fl, y - 2f * k.u, fr, y + 2f * k.u, 2f * k.u, StyleData.SUN_COLOR)
                k.text("${f.highC}°", k.right - k.cornerReserve * 0.4f, base, 12f, k.ink, Weight.SEMIBOLD, Align.RIGHT, k.contentW * 0.13f)
            }
            return
        }
        if (warning != null) k.text(warning, k.left, k.top + 5f * k.u, 8.5f, k.negative, Weight.MEDIUM, maxWidth = k.contentW - k.cornerReserve)
        val slot = (k.contentW - k.cornerReserve) / days.size
        val headY = k.top + (if (warning != null) 17f else 10f) * k.u
        val iconSize = minOf(slot * 0.5f, (k.bottom - k.top) * 0.3f)
        days.forEachIndexed { i, f ->
            val x = k.left + slot * i + slot / 2f
            k.text(day(f.date), x, headY, 10.5f, k.ink, Weight.MEDIUM, Align.CENTER, slot * 0.94f)
            weatherIcon(k, f.weatherCode, x, k.cy + 1f * k.u, iconSize)
            k.text("${f.highC}°", x, k.bottom - 1f * k.u, 12f, k.ink, Weight.SEMIBOLD, Align.CENTER, slot * 0.94f)
        }
    }

    /** Current block + "12PM 3PM 6PM 9PM" columns (mockup hourly card). */
    fun renderHourly(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val data = weather(context)
        val place = WeatherRepository.currentPlace(context)
        val hours = WeatherPresentation.upcomingHours(data.hourly, place.timezone, count = 12)
            .filterIndexed { i, _ -> i % 3 == 0 }.take(4)
        val warning = alert(context, spec, data)
        if (k.isLarge) {
            renderHourlyLarge(k, data, WeatherPresentation.upcomingHours(data.hourly, place.timezone, count = 12), hours, warning)
            return
        }
        val nowW = k.contentW * 0.36f
        val iconSize = 15f * k.u
        nowIcon(k, data, k.left + iconSize / 2f, k.top + iconSize / 2f + 2f * k.u, iconSize)
        k.text(temp(data), k.left + iconSize + 3f * k.u, k.top + iconSize * 0.85f + 2f * k.u, 18f, k.ink, Weight.SEMIBOLD, maxWidth = nowW - iconSize - 3f * k.u)
        k.text(warning ?: placeLine(data), k.left, k.bottom - 1f * k.u, 9.5f, if (warning != null) k.negative else k.sub, Weight.MEDIUM, maxWidth = nowW - 2f * k.u)
        val divX = k.left + nowW
        k.roundRect(divX - 0.4f * k.u, k.top + 4f * k.u, divX + 0.4f * k.u, k.bottom - 4f * k.u, 0.4f * k.u, k.track)
        if (hours.isEmpty()) {
            k.text("Hourly loading…", (divX + k.right) / 2f, k.cy, 10f, k.sub, Weight.MEDIUM, Align.CENTER, k.right - divX)
            return
        }
        val slot = (k.right - k.cornerReserve - divX - 3f * k.u) / hours.size
        hours.forEachIndexed { i, h ->
            val x = divX + 3f * k.u + slot * i + slot / 2f
            k.text(hourLabel(h.time), x, k.top + 8f * k.u, 8.5f, k.sub, Weight.MEDIUM, Align.CENTER, slot * 0.95f)
            weatherIcon(k, h.weatherCode, x, k.cy, minOf(slot * 0.5f, 13f * k.u), h.isDay)
            k.text("${h.temperatureC}°", x, k.bottom - 1f * k.u, 11f, k.ink, Weight.SEMIBOLD, Align.CENTER, slot * 0.95f)
        }
    }

    /** Square tile: now at the top, the 12-hour temperature line, then four hour columns. */
    private fun renderHourlyLarge(
        k: StyleKit, data: WeatherData, all: List<com.example.data.weather.HourlyForecast>,
        hours: List<com.example.data.weather.HourlyForecast>, warning: String?
    ) {
        val iconSize = 30f * k.u
        nowIcon(k, data, k.left + iconSize / 2f, k.top + iconSize / 2f, iconSize)
        val tx = k.left + iconSize + 5f * k.u
        k.text(temp(data), tx, k.top + 17f * k.u, 24f, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW * 0.5f)
        k.text(warning ?: placeLine(data), tx, k.top + 30f * k.u, 10.5f, if (warning != null) k.negative else k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
        if (hours.isEmpty()) {
            k.text("Hourly loading…", k.cx, k.cy + 12f * k.u, 11f, k.sub, Weight.MEDIUM, Align.CENTER)
            return
        }
        k.spark(all.map { it.temperatureC.toFloat() }, k.left, k.top + 44f * k.u, k.right, k.bottom - 58f * k.u, StyleData.SUN_COLOR, fillUnder = true)
        val slot = k.contentW / hours.size
        hours.forEachIndexed { i, h ->
            val x = k.left + slot * i + slot / 2f
            k.text(hourLabel(h.time), x, k.bottom - 38f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.CENTER, slot * 0.95f)
            weatherIcon(k, h.weatherCode, x, k.bottom - 23f * k.u, 18f * k.u, h.isDay)
            k.text("${h.temperatureC}°", x, k.bottom - 1f * k.u, 13f, k.ink, Weight.SEMIBOLD, Align.CENTER, slot * 0.95f)
        }
    }

    /** Title + open arc gauge + big value + band (UV / AQI / pressure cards). */
    private fun gauge(k: StyleKit, context: Context, data: WeatherData, title: String, value: String, band: String, fraction: Float, color: Int = k.ink) {
        titleOrAlert(k, context, title, data)
        val top = k.contentTop
        if (k.isWide) {
            val r = (k.bottom - top) * 0.5f
            val x = k.left + r + 2f * k.u
            val y = top + r + 2f * k.u
            k.arcGauge(x, y, r * 0.9f, 4.2f * k.u, fraction, color)
            k.text(value, x, y + 6f * k.u, 18f, k.ink, Weight.SEMIBOLD, Align.CENTER, r * 1.4f)
            val tx = x + r + 8f * k.u
            k.text(band, tx, k.cy + 8f * k.u, 13f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            return
        }
        val r = minOf(k.contentW, k.bottom - top) * 0.42f
        val y = top + r + 2f * k.u
        k.arcGauge(k.cx, y, r, 4.2f * k.u, fraction, color)
        k.text(value, k.cx, y + 6f * k.u, 20f, k.ink, Weight.SEMIBOLD, Align.CENTER, r * 1.4f)
        k.text(band, k.cx, k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.CENTER)
    }

    fun renderUv(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val uv = data.uvIndex
        val band = when {
            data.isStale -> "No data"
            uv < 3 -> "Low"
            uv < 6 -> "Moderate"
            uv < 8 -> "High"
            uv < 11 -> "Very high"
            else -> "Extreme"
        }
        val value = if (data.isStale) "--" else String.format(Locale.US, "%.0f", uv)
        gauge(k, context, data, "UV Index", value, band, if (data.isStale) 0f else (uv / 11.0).toFloat(), if (!data.isStale && uv >= 6) k.accent else k.ink)
    }

    fun renderAqi(context: Context, spec: WidgetSpec, canvas: Canvas, estimated: Boolean) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val aqi: Int?
        if (estimated) {
            // Same rough proxy as the classic Air Quality dial; the title says it is an estimate.
            aqi = if (data.isStale) null else (90 - data.windKmh * 2 + (data.humidityPct - 50)).coerceIn(5, 200)
        } else {
            aqi = AirQualityRepository.current(context)?.usAqi
        }
        val band = when {
            aqi == null -> if (estimated) "No data" else "Updating…"
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for some"
            aqi <= 200 -> "Unhealthy"
            else -> "Very unhealthy"
        }
        gauge(k, context, data, if (estimated) "AQI · estimate" else "AQI", aqi?.toString() ?: "--", band, (aqi ?: 0) / 300f, if ((aqi ?: 0) > 100) k.accent else k.ink)
    }

    fun renderPressure(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val hpa = data.pressureHpa?.takeIf { !data.isStale }
        val band = when {
            hpa == null -> "No data"
            hpa >= 1022 -> "High"
            hpa <= 1003 -> "Low"
            else -> "Normal"
        }
        gauge(k, context, data, "Pressure · hPa", hpa?.toString() ?: "--", band, hpa?.let { (it - 980) / 60f } ?: 0f)
    }

    /** Wind dial with a needle and speed (mockup "Wind speed"). */
    fun renderWind(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val from = data.windDirectionDeg?.takeIf { !data.isStale }
        titleOrAlert(k, context, "Wind", data)
        val top = k.contentTop
        val r = (k.bottom - top) * 0.48f
        val x = k.left + r + 2f * k.u
        val y = top + r
        k.ticks(x, y, r, 36, r * 0.07f, r * 0.16f, 9, k.sub, maxOf(1f, r * 0.03f))
        if (from != null) {
            // The arrow flies with the wind: it points to where the wind is going.
            k.canvas.save()
            k.canvas.rotate(from + 180f, x, y)
            k.icon(Icons.Filled.Navigation, x, y, r * 1.0f, k.ink)
            k.canvas.restore()
        }
        val tx = x + r + 8f * k.u
        val unit = if (spec.options.boolean("show_unit", true)) " km/h" else ""
        k.text((if (data.isStale) "--" else "${data.windKmh}") + unit, tx, k.cy + 4f * k.u, 22f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
        k.icon(Icons.Filled.Air, tx + 4f * k.u, k.cy + 15f * k.u, 8f * k.u, k.sub)
        k.text(if (from != null) "From ${data.windFrom}" else "Direction --", tx + 10f * k.u, k.cy + 18f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx - 10f * k.u)
    }

    /** "🌅 07:00 · 18:14 🌇" sunrise/sunset card (mockup). */
    fun renderSunTimes(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily, widgetId: String) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val place = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(place.timezone)
        val sun = Astronomy.sunTimes(place.lat, place.lon, tz, Calendar.getInstance(tz))
        val h24 = if (widgetId == "travel.sun.times") spec.options.boolean("h24", true) else true
        val colW = k.contentW / 2f
        val iconSize = minOf(colW * 0.4f, (k.bottom - k.top) * 0.34f)
        listOf("Sunrise" to sun.sunriseMin, "Sunset" to sun.sunsetMin).forEachIndexed { i, (label, minutes) ->
            val x = k.left + colW * i + colW / 2f
            k.icon(Icons.Filled.WbTwilight, x, k.top + iconSize / 2f, iconSize, if (i == 0) StyleData.SUN_COLOR else k.ink)
            k.text(StyleData.minutesLabel(minutes, h24), x, k.cy + 10f * k.u, if (k.isWide) 20f else 15f, k.ink, Weight.SEMIBOLD, Align.CENTER, colW * 0.94f)
            k.text(label, x, k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, Align.CENTER, colW * 0.94f)
        }
    }

    /** Lit moon disc for an illumination 0..1 (waxing lights the right side). */
    fun drawMoon(k: StyleKit, x: Float, y: Float, r: Float, illumination: Double, waxing: Boolean, lit: Int, dark: Int) {
        k.circle(x, y, r, dark)
        val rect = RectF(x - r, y - r, x + r, y + r)
        val half = Path().apply { arcTo(rect, if (waxing) -90f else 90f, 180f); close() }
        k.canvas.drawPath(half, k.fill(lit))
        val ew = (r * kotlin.math.abs(1 - 2 * illumination)).toFloat()
        val ellipse = RectF(x - ew, y - r, x + ew, y + r)
        k.canvas.drawOval(ellipse, k.fill(if (illumination > 0.5) lit else dark))
        if (!k.dark) k.canvas.drawCircle(x, y, r, k.stroke(dark, maxOf(1f, r * 0.03f)))
    }

    fun renderMoon(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val moon = Astronomy.moonPhase()
        val name = StyleData.titleCase(moon.name)
        val pct = "${(moon.illumination * 100).toInt()}%"
        // The lit part is always the bright one, in both themes.
        val lit = if (k.dark) 0xFFE8E8EA.toInt() else 0xFFFFFFFF.toInt()
        val shade = if (k.dark) 0xFF3A3A3C.toInt() else 0xFF8E8E93.toInt()
        if (k.isWide) {
            val r = (k.bottom - k.top) * 0.45f
            drawMoon(k, k.left + r, k.cy, r, moon.illumination, moon.isWaxing, lit, shade)
            val tx = k.left + 2 * r + 8f * k.u
            k.text(name, tx, k.cy - 2f * k.u, 14f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(pct, tx, k.cy + 14f * k.u, 12f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
            return
        }
        val r = (k.bottom - k.top) * 0.28f
        drawMoon(k, k.cx, k.top + r + 2f * k.u, r, moon.illumination, moon.isWaxing, lit, shade)
        k.text(name, k.cx, k.bottom - 11f * k.u, 11f, k.ink, Weight.SEMIBOLD, Align.CENTER)
        k.text(pct, k.cx, k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, Align.CENTER)
    }

    /** Thermometer "26°C / Feels like 28°" with a humidity bar (mockup). */
    fun renderThermometer(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val data = weather(context)
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.6f else 30f * k.u
        k.icon(Icons.Filled.Thermostat, k.left + iconSize * 0.4f, if (k.isWide) k.top + iconSize / 2f else k.top + iconSize / 2f, iconSize, k.ink)
        val x = k.left + iconSize * 0.8f + 4f * k.u
        val w = k.right - x
        k.text(temp(data, "°C"), x, if (k.isWide) k.cy - 11f * k.u else k.top + iconSize * 0.55f, if (k.isWide) 28f else 20f, k.ink, Weight.SEMIBOLD, maxWidth = w)
        k.text(feels(data), x, if (k.isWide) k.cy + 8f * k.u else k.top + iconSize * 0.55f + 11f * k.u, 9.5f, k.sub, Weight.MEDIUM, maxWidth = w)
        val barTop = k.bottom - 4f * k.u
        val warning = alert(context, spec, data)
        val humidity = if (data.isStale) "Humidity --%" else "Humidity ${data.humidityPct}%"
        k.text(warning ?: humidity, k.left, barTop - 4f * k.u, 9f, if (warning != null) k.negative else k.sub, Weight.MEDIUM)
        k.pill(k.left, barTop, k.right, k.bottom, if (data.isStale) 0f else data.humidityPct / 100f)
    }

    /** Golden / blue hour times on a soft card (same maths as the classic Golden Hour). */
    fun renderGolden(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val place = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(place.timezone)
        val sun = Astronomy.sunTimes(place.lat, place.lon, tz, Calendar.getInstance(tz))
        k.title("Golden Hour")
        val rise = sun.sunriseMin
        val set = sun.sunsetMin
        if (rise == null || set == null) {
            k.text("No sunrise or sunset today", k.left, k.cy + 4f * k.u, 11f, k.sub, Weight.MEDIUM)
            return
        }
        val rows = if (k.isLarge) listOf(
            "Blue hour" to "${StyleData.minutesLabel(rise - 25)} – ${StyleData.minutesLabel(rise)}",
            "Morning" to "${StyleData.minutesLabel(rise)} – ${StyleData.minutesLabel(rise + 60)}",
            "Evening" to "${StyleData.minutesLabel(set - 60)} – ${StyleData.minutesLabel(set)}",
            "Blue hour" to "${StyleData.minutesLabel(set)} – ${StyleData.minutesLabel(set + 25)}"
        ) else listOf(
            "Morning" to "${StyleData.minutesLabel(rise)} – ${StyleData.minutesLabel(rise + 60)}",
            "Evening" to "${StyleData.minutesLabel(set - 60)} – ${StyleData.minutesLabel(set)}"
        )
        val top = k.contentTop + 4f * k.u
        val rowH = (k.bottom - top) / rows.size
        rows.forEachIndexed { i, (label, range) ->
            val y = top + rowH * i + rowH * 0.62f
            k.circle(k.left + 2f * k.u, y - 3f * k.u, 2f * k.u, if (label == "Blue hour") 0xFF4DA3FF.toInt() else StyleData.SUN_COLOR)
            k.text(label, k.left + 7f * k.u, y, 11f, k.sub, Weight.MEDIUM, maxWidth = k.contentW * 0.35f)
            k.text(range, k.right, y, 13f, k.ink, Weight.SEMIBOLD, Align.RIGHT, k.contentW * 0.6f)
        }
    }
}
