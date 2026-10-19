package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.astro.Astronomy
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object WeatherRenderers {

    fun renderCurrentWeather(context: Context, spec: WidgetSpec, canvas: Canvas, cityNameOverride: String? = null) {
        val data = WeatherRepository.getWeatherInstant(context, cityNameOverride)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        // Stale guard: if data is stale >90 min, show dash "--" indicator
        val tempStr = if (data.isStale) "--°" else "${data.temperatureC}°"

        // Draw City name top left
        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (data.isStale) 0xFFE02B2B.toInt() else 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            val label = when {
                data.fetchedAt == 0L -> data.cityName.uppercase()
                data.isStale -> "${data.cityName.uppercase()} (OFFLINE)"
                else -> data.cityName.uppercase()
            }
            DotMatrixRenderer.drawFittedLabel(canvas, label, pad, pad + 12f * spec.density, maxW * 0.70f, paintLabel)
        }

        if (spec.sizeBucket == SizeBucket.S) {
            // S Size: Temperature left-aligned, Weather icon right-aligned (guaranteed non-overlapping)
            val iconRadius = 18f * spec.density
            val iconCx = spec.widthPx - pad - iconRadius
            val iconCy = spec.heightPx * 0.44f

            // Safe temperature width strictly bounded away from the icon
            val maxTempW = iconCx - iconRadius - pad - (10f * spec.density)
            val maxTempH = spec.heightPx * 0.36f
            val dotGap = DotMatrixRenderer.fitDotGap(tempStr.length, maxTempW, maxTempH, 8.5f * spec.density)
            val dotRadius = dotGap * 0.38f

            val startX = pad
            val startY = spec.heightPx * 0.35f

            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = tempStr,
                startX = startX,
                startY = startY,
                dotRadius = dotRadius,
                dotGap = dotGap,
                onColor = onColor,
                offColor = offColor,
                accentIndices = setOf(tempStr.length - 1),
                accentColor = accentColor
            )

            // Weather icon
            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = data.weatherCode,
                centerX = iconCx,
                centerY = iconCy,
                dotGap = 4.5f * spec.density,
                dotRadius = 1.8f * spec.density,
                onColor = onColor,
                offColor = offColor,
                accentColor = accentColor
            )

            // High / Low and Condition below with auto-fit and ellipsize
            val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(13f)
                isFakeBoldText = true
            }
            val subText = if (data.fetchedAt == 0L) "WAITING FOR DATA" else "${data.description.uppercase()} • H:${data.highC}° L:${data.lowC}°"
            val subY = spec.heightPx - pad - (8f * spec.density)
            DotMatrixRenderer.drawFittedLabel(canvas, subText, pad, subY, maxW, paintSub)

        } else {
            // M and L Sizes: Wide layout with temperature on left and hero icon on right
            val iconCx = spec.widthPx - pad - (60f * spec.density)
            val iconCy = spec.heightPx * 0.46f

            val maxTempW = spec.widthPx * 0.42f
            val maxTempH = spec.heightPx * 0.45f
            val dotGap = DotMatrixRenderer.fitDotGap(tempStr.length, maxTempW, maxTempH, 12.0f * spec.density)
            val dotRadius = dotGap * 0.38f

            val startX = pad
            val startY = spec.heightPx * 0.33f

            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = tempStr,
                startX = startX,
                startY = startY,
                dotRadius = dotRadius,
                dotGap = dotGap,
                onColor = onColor,
                offColor = offColor,
                accentIndices = setOf(tempStr.length - 1),
                accentColor = accentColor
            )

            // Hero Weather icon
            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = data.weatherCode,
                centerX = iconCx,
                centerY = iconCy,
                dotGap = 6.2f * spec.density,
                dotRadius = 2.4f * spec.density,
                onColor = onColor,
                offColor = offColor,
                accentColor = accentColor
            )

            // High / Low and Condition below
            val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(15f)
                isFakeBoldText = true
            }
            val subText = if (data.fetchedAt == 0L) "WAITING FOR WEATHER DATA" else "${data.description.uppercase()} • HIGH ${data.highC}° • LOW ${data.lowC}° • WIND ${data.windKmh}KM/H"
            val subY = spec.heightPx - pad - (8f * spec.density)
            DotMatrixRenderer.drawFittedLabel(canvas, subText, pad, subY, maxW, paintSub)
        }
    }

    /**
     * Real 5-day outlook from Open-Meteo. With [showPrecipitation] the bottom row shows the
     * chance of rain as a dot column instead of the high temperature.
     */
    fun renderForecast5Day(context: Context, spec: WidgetSpec, canvas: Canvas, showPrecipitation: Boolean = false) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val data = WeatherRepository.getWeatherInstant(context)
        val title = if (showPrecipitation) "RAIN CHANCE" else "5-DAY FORECAST"
        kit.header("$title • ${data.cityName.uppercase()}", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor, 0.95f)

        val days = data.daily.take(5)
        if (days.isEmpty()) {
            kit.dotTextCentered("--", kit.height * 0.55f, kit.height * 0.30f, 10f * d)
            return
        }

        val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayName = SimpleDateFormat("EEE", Locale.US)
        val colW = kit.maxW / days.size
        val labelY = kit.pad + (32f * d)
        val paintOn = kit.fill(kit.onColor)
        val paintOff = kit.fill(kit.offColor)
        val paintAccent = kit.fill(kit.accentColor)

        days.forEachIndexed { i, day ->
            val cx = kit.pad + (i * colW) + (colW / 2f)
            val name = if (i == 0) "TODAY" else runCatching { dayName.format(parse.parse(day.date)!!).uppercase() }.getOrDefault("--")
            val paintDay = kit.textPaint(if (i == 0) kit.accentColor else kit.greyColor, 10f, 13f)
            canvas.drawText(name, cx - paintDay.measureText(name) / 2f, labelY, paintDay)

            if (showPrecipitation) {
                // 5-dot column: one dot per 20% chance of rain
                val filled = ((day.precipitationPct + 10) / 20).coerceIn(0, 5)
                val gap = 7f * d
                val baseY = labelY + 50f * d
                for (k in 0 until 5) {
                    val on = k < filled
                    canvas.drawCircle(cx, baseY - k * gap, if (on) 2.6f * d else 2.0f * d, if (on) (if (k == filled - 1) paintAccent else paintOn) else paintOff)
                }
                val pct = "${day.precipitationPct}%"
                val paintPct = kit.textPaint(kit.onColor, 11f, 14f)
                canvas.drawText(pct, cx - paintPct.measureText(pct) / 2f, labelY + 70f * d, paintPct)
            } else {
                DotMatrixRenderer.drawWeatherIcon(
                    canvas = canvas,
                    weatherCode = day.weatherCode,
                    centerX = cx,
                    centerY = labelY + 28f * d,
                    dotGap = 3.6f * d,
                    dotRadius = 1.3f * d,
                    onColor = kit.onColor,
                    offColor = kit.offColor,
                    accentColor = kit.accentColor
                )
                val paintTemp = kit.textPaint(kit.onColor, 11f, 14f)
                val high = "${day.highC}°"
                canvas.drawText(high, cx - paintTemp.measureText(high) / 2f, labelY + 58f * d, paintTemp)
                if (spec.sizeBucket == SizeBucket.L) {
                    val paintLow = kit.textPaint(kit.greyColor, 10f, 13f)
                    val low = "${day.lowC}°"
                    canvas.drawText(low, cx - paintLow.measureText(low) / 2f, labelY + 74f * d, paintLow)
                    val paintRain = kit.textPaint(kit.accentColor, 9f, 12f)
                    val rain = "${day.precipitationPct}%"
                    canvas.drawText(rain, cx - paintRain.measureText(rain) / 2f, labelY + 90f * d, paintRain)
                }
            }
        }
    }

    /** Sun position between today's sunrise and sunset, calculated offline for the default city. */
    fun renderSunriseSunsetArc(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val preset = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(preset.timezone)
        val now = Calendar.getInstance(tz)
        val sun = Astronomy.sunTimes(preset.lat, preset.lon, tz, now)
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        kit.header("SUN TRACKER • ${preset.name.uppercase()}", widthFraction = 0.95f)

        val cx = kit.width / 2f
        val cy = kit.height - kit.pad - (20f * d)
        val arcRadius = minOf(kit.width * 0.34f, kit.height * 0.45f)
        val totalArcDots = 15

        val rise = sun.sunriseMin
        val set = sun.sunsetMin
        // Progress 0..1 across the day; null while the sun is down.
        val progress = if (rise != null && set != null && nowMin in rise..set) (nowMin - rise).toFloat() / (set - rise) else null
        val sunIndex = progress?.let { (it * (totalArcDots - 1)).toInt() }

        val paintOn = kit.fill(kit.onColor)
        val paintOff = kit.fill(kit.offColor)
        val paintAccent = kit.fill(kit.accentColor)

        for (i in 0 until totalArcDots) {
            val angleRad = Math.toRadians(180.0 + i * (180.0 / (totalArcDots - 1)))
            val x = cx + (arcRadius * Math.cos(angleRad)).toFloat()
            val y = cy + (arcRadius * Math.sin(angleRad)).toFloat()
            when {
                sunIndex != null && i == sunIndex -> canvas.drawCircle(x, y, 6.0f * d, paintAccent)
                sunIndex != null && i < sunIndex -> canvas.drawCircle(x, y, 3.2f * d, paintOn)
                else -> canvas.drawCircle(x, y, 2.4f * d, paintOff)
            }
        }

        val paintTime = kit.textPaint(kit.greyColor, 10f, 13f)
        val riseStr = rise?.let { formatMinutes(it) } ?: "--:--"
        val setStr = set?.let { formatMinutes(it) } ?: "--:--"
        canvas.drawText(riseStr, kit.pad, cy + 16f * d, paintTime)
        canvas.drawText(setStr, kit.width - kit.pad - paintTime.measureText(setStr), cy + 16f * d, paintTime)

        // Centre caption: time until the next sunrise/sunset
        val caption = when {
            rise == null || set == null -> "NO SUNRISE TODAY"
            nowMin < rise -> "SUNRISE IN ${formatDuration(rise - nowMin)}"
            nowMin <= set -> "SUNSET IN ${formatDuration(set - nowMin)}"
            else -> "SUNRISE IN ${formatDuration(1440 - nowMin + rise)}"
        }
        kit.labelCentered(caption, cy + 16f * d, kit.textPaint(kit.onColor, 10f, 13f), kit.maxW * 0.5f)
    }

    /** Humidity and "feels like" temperature for the default city. */
    fun renderComfort(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val data = WeatherRepository.getWeatherInstant(context)
        kit.header("COMFORT • ${data.cityName.uppercase()}", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)

        val colW = kit.maxW / 2f
        val valueY = kit.height * 0.50f
        val feels = if (data.isStale) "--°" else "${data.feelsLikeC}°"
        val humidity = if (data.isStale) "--%" else "${data.humidityPct}%"

        listOf("FEELS LIKE" to feels, "HUMIDITY" to humidity).forEachIndexed { i, (label, value) ->
            val left = kit.pad + i * colW
            val gap = DotMatrixRenderer.fitDotGap(value.length, colW * 0.85f, kit.height * 0.30f, 9f * d)
            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = value,
                startX = left,
                startY = valueY - 3 * gap,
                dotRadius = gap * 0.38f,
                dotGap = gap,
                onColor = kit.onColor,
                offColor = kit.offColor,
                accentIndices = setOf(value.length - 1),
                accentColor = kit.accentColor
            )
            DotMatrixRenderer.drawFittedLabel(canvas, label, left, valueY + 4 * gap + 10f * d, colW * 0.9f, kit.textPaint(kit.greyColor, 10f, 13f))
        }

        if (!data.isStale) {
            val sub = "ACTUAL ${data.temperatureC}° • ${data.description.uppercase()}"
            DotMatrixRenderer.drawFittedLabel(canvas, sub, kit.pad, kit.height - kit.pad - 4f * d, kit.maxW, kit.textPaint(kit.greyColor, 10f, 13f))
        }
    }

    /** Current UV index with a 0..11+ dot scale and WHO risk level. */
    fun renderUvIndex(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val data = WeatherRepository.getWeatherInstant(context)
        kit.header("UV INDEX", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)

        val uv = data.uvIndex.roundToInt()
        val value = if (data.isStale) "--" else uv.toString()
        kit.dotTextCentered(value, kit.height * 0.44f, kit.height * 0.32f, 10f * d, kit.maxW * 0.6f)

        // 11-dot scale; risky levels (6+) drawn in the accent colour
        val dots = 11
        val gap = kit.maxW / (dots - 1)
        val y = kit.height * 0.70f
        for (i in 0 until dots) {
            val on = !data.isStale && i < uv
            val paint = when {
                !on -> kit.fill(kit.offColor)
                i >= 5 -> kit.fill(kit.accentColor)
                else -> kit.fill(kit.onColor)
            }
            canvas.drawCircle(kit.pad + i * gap, y, if (on) 3.0f * d else 2.2f * d, paint)
        }

        val risk = when {
            data.isStale -> "NO DATA"
            uv <= 2 -> "LOW"
            uv <= 5 -> "MODERATE"
            uv <= 7 -> "HIGH"
            uv <= 10 -> "VERY HIGH"
            else -> "EXTREME"
        }
        kit.labelCentered(risk, kit.height - kit.pad - 2f * d, kit.textPaint(if (uv >= 6) kit.accentColor else kit.greyColor, 10f, 13f))
    }

    /**
     * Next hours from Open-Meteo's hourly series: one column per hour with the time, a dot weather
     * icon and the temperature. Columns shown depend on the size (4 for M, 6 for L).
     */
    fun renderHourlyForecast(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val data = WeatherRepository.getWeatherInstant(context)
        val place = WeatherRepository.currentPlace(context)
        kit.header("NEXT HOURS • ${data.cityName.uppercase()}", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor, 0.95f)

        val columns = if (spec.sizeBucket == SizeBucket.L) 6 else 4
        val hours = upcomingHours(data.hourly, place.timezone, columns)
        if (hours.isEmpty()) {
            kit.dotTextCentered("--", kit.height * 0.55f, kit.height * 0.30f, 10f * d)
            return
        }

        val timeLabel = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone(place.timezone) }
        val parse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone(place.timezone) }
        val colW = kit.maxW / hours.size
        val labelY = kit.pad + (30f * d)

        hours.forEachIndexed { i, hour ->
            val cx = kit.pad + (i * colW) + (colW / 2f)
            val label = if (i == 0) "NOW" else runCatching { timeLabel.format(parse.parse(hour.time)!!) }.getOrDefault("--")
            val paintTime = kit.textPaint(if (i == 0) kit.accentColor else kit.greyColor, if (columns > 4) 9f else 10f, 13f)
            canvas.drawText(label, cx - paintTime.measureText(label) / 2f, labelY, paintTime)

            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = hour.weatherCode,
                centerX = cx,
                centerY = labelY + (26f * d),
                dotGap = 3.4f * d,
                dotRadius = 1.25f * d,
                onColor = kit.onColor,
                offColor = kit.offColor,
                accentColor = kit.accentColor
            )

            val paintTemp = kit.textPaint(kit.onColor, if (columns > 4) 11f else 12f, 15f)
            val temp = "${hour.temperatureC}°"
            canvas.drawText(temp, cx - paintTemp.measureText(temp) / 2f, labelY + (56f * d), paintTemp)

            if (spec.sizeBucket == SizeBucket.L) {
                val paintRain = kit.textPaint(kit.accentColor, 9f, 12f)
                val rain = "${hour.precipitationPct}%"
                canvas.drawText(rain, cx - paintRain.measureText(rain) / 2f, labelY + (72f * d), paintRain)
            }
        }
    }

    /** The next [count] hourly entries from now (in the city's time zone), falling back to the head. */
    private fun upcomingHours(
        all: List<com.example.data.weather.HourlyForecast>,
        timezone: String,
        count: Int
    ): List<com.example.data.weather.HourlyForecast> {
        if (all.isEmpty()) return emptyList()
        val zone = TimeZone.getTimeZone(timezone)
        val parse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = zone }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply { timeZone = zone }.format(Calendar.getInstance(zone).time)
        val fromNow = all.dropWhile { it.time < now }
        return (if (fromNow.isEmpty()) all else fromNow).take(count)
    }

    private fun formatMinutes(min: Int) = String.format(Locale.US, "%02d:%02d", min / 60, min % 60)

    private fun formatDuration(min: Int) = if (min >= 60) "${min / 60}H ${min % 60}M" else "${min}M"
}
