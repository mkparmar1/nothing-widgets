package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.astro.Astronomy
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.finance.FxRepository
import com.example.data.travel.TravelRepository
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Phase 7 Travel & World family. Clocks use [TimeZone]; sun times are computed offline via
 * [Astronomy]; currency uses [FxRepository]'s bundled fallback table; flight/trip/packing come from
 * [TravelRepository]. Nothing here blocks on the network.
 */
object TravelRenderers {

    private fun zoneCity(zoneId: String): String =
        zoneId.substringAfterLast('/').replace('_', ' ').uppercase()

    // ---------------------------------------------------------------- multi-city clock

    fun renderMultiCityClock(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val zones = spec.options.string("zones", "Europe/London,Asia/Tokyo,America/New_York").split(",")
            .map { it.trim() }.filter { it.isNotBlank() }.take(4)
        kit.header("WORLD CLOCK")
        if (zones.isEmpty()) {
            kit.labelCentered("SET CITIES IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val top = kit.pad + 24f * kit.d
        val rowH = (kit.height - top - kit.pad) / zones.size
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        zones.forEachIndexed { i, zone ->
            val cal = Calendar.getInstance().apply { timeZone = TimeZone.getTimeZone(zone) }
            val y = top + i * rowH + rowH * 0.65f
            DotMatrixRenderer.drawFittedLabel(canvas, zoneCity(zone), kit.pad, y, kit.maxW * 0.5f, kit.textPaint(kit.greyColor, 10f, 13f))
            val paint = kit.textPaint(kit.onColor, 12f, 16f)
            val time = fmt.format(cal.time)
            canvas.drawText(time, kit.width - kit.pad - paint.measureText(time), y, paint)
        }
    }

    fun renderSingleWorldCity(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val zone = spec.options.string("city", "Europe/London")
        val cal = Calendar.getInstance().apply { timeZone = TimeZone.getTimeZone(zone) }
        kit.header(zoneCity(zone))
        val fmt = SimpleDateFormat(if (spec.options.boolean("h24", true)) "HH:mm" else "hh:mm", Locale.US)
        val time = fmt.format(cal.time)
        kit.dotTextCentered(time, kit.height * 0.50f, kit.height * 0.36f, 11f * kit.d, accentIndices = RenderKit.colonIndices(time))
        val amPm = if (!spec.options.boolean("h24", true)) SimpleDateFormat("a", Locale.US).format(cal.time).uppercase() else ""
        val day = SimpleDateFormat("EEE d MMM", Locale.US).format(cal.time).uppercase()
        kit.labelCentered(listOf(day, amPm).filter { it.isNotBlank() }.joinToString("  "), kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderWeatherCity(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val city = spec.options.string("city", "London")
        kit.header("WEATHER • ${city.uppercase()}")
        WeatherRenderers.renderCurrentWeather(context, spec, canvas, cityNameOverride = city)
    }

    fun renderSunTimes(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val preset = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(preset.timezone)
        val now = Calendar.getInstance(tz)
        val sun = Astronomy.sunTimes(preset.lat, preset.lon, tz, now)
        kit.header("SUNRISE & SUNSET • ${preset.name.uppercase()}", widthFraction = 0.95f)
        val colW = kit.maxW / 2f
        val labels = listOf("SUNRISE" to sun.sunriseMin, "SUNSET" to sun.sunsetMin)
        labels.forEachIndexed { i, (label, min) ->
            val left = kit.pad + i * colW
            val str = min?.let { String.format(Locale.US, "%02d:%02d", it / 60, it % 60) } ?: "--:--"
            val gap = DotMatrixRenderer.fitDotGap(5, colW * 0.82f, kit.height * 0.28f, 9f * kit.d)
            DotMatrixRenderer.drawText(
                canvas = canvas, text = str, startX = left, startY = kit.height * 0.42f,
                dotRadius = gap * 0.38f, dotGap = gap, onColor = kit.onColor, offColor = kit.offColor,
                accentIndices = setOf(2), accentColor = kit.accentColor
            )
            DotMatrixRenderer.drawFittedLabel(canvas, label, left, kit.height * 0.72f, colW * 0.9f, kit.textPaint(kit.greyColor, 10f, 13f))
        }
    }

    fun renderFlightCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = TravelRepository.getInstance(context)
        val code = spec.options.string("flight", repo.flightCode()).uppercase()
        val ms = repo.flightTimeMs()
        kit.header("FLIGHT $code")
        if (ms <= 0L) {
            kit.labelCentered("SET FLIGHT TIME IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        val diff = ms - System.currentTimeMillis()
        val days = (diff / 86_400_000L).toInt()
        val hours = ((diff % 86_400_000L) / 3_600_000L).toInt()
        val value = if (days > 0) "${days}D ${hours}H" else "${(diff / 3_600_000L).coerceAtLeast(0)}H"
        kit.dotTextCentered(value, kit.height * 0.50f, kit.height * 0.30f, 9f * kit.d)
        val whenStr = SimpleDateFormat("EEE d MMM • HH:mm", Locale.US).format(Date(ms)).uppercase()
        kit.labelCentered(if (diff <= 0) "DEPARTED" else whenStr, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderPackingCheck(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val items = TravelRepository.getInstance(context).packingItems()
        kit.header("PACKING LIST")
        if (items.isEmpty()) {
            kit.labelCentered("NO ITEMS", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val limit = spec.options.int("max_items", 6).coerceIn(3, 10)
        val shown = items.take(limit)
        val top = kit.pad + 24f * kit.d
        val rowH = (kit.height - top - kit.pad) / shown.size.coerceAtLeast(1)
        shown.forEachIndexed { i, item ->
            val y = top + i * rowH + rowH * 0.72f
            val box = 4f * kit.d
            canvas.drawCircle(kit.pad + box, y - box, box, kit.fill(if (item.done) kit.accentColor else kit.offColor))
            val paint = kit.textPaint(if (item.done) kit.greyColor else kit.onColor, 10f, 14f)
            DotMatrixRenderer.drawFittedLabel(canvas, item.label.uppercase(), kit.pad + box * 3f, y, kit.maxW - box * 3f, paint)
        }
    }

    fun renderTripCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = TravelRepository.getInstance(context)
        val dest = spec.options.string("destination", repo.tripDestination()).uppercase()
        val days = repo.daysUntilTrip()
        kit.header(dest)
        kit.dotTextCentered("$days", kit.height * 0.48f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (days <= 1) setOf(0) else emptySet())
        kit.labelCentered(if (days == 0) "TRAVEL TODAY" else "DAY${if (days == 1) "" else "S"} TO GO", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderCurrencyTip(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        FxRepository.refresh(context)
        val home = spec.options.string("home", "USD")
        val away = spec.options.string("away", "EUR")
        kit.header("$home → $away")
        val rate = FxRepository.convert(1.0, home, away)
        kit.dotTextCentered(String.format(Locale.US, "%.3f", rate), kit.height * 0.46f, kit.height * 0.32f, 10f * kit.d)
        val sample = FxRepository.convert(100.0, home, away)
        kit.labelCentered("100 $home ≈ ${String.format(Locale.US, "%.0f", sample)} $away", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderTzDiff(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val home = spec.options.string("home", TimeZone.getDefault().id)
        val away = spec.options.string("away", "Europe/London")
        val homeOffset = TimeZone.getTimeZone(home).getOffset(System.currentTimeMillis())
        val awayOffset = TimeZone.getTimeZone(away).getOffset(System.currentTimeMillis())
        val diffHours = (awayOffset - homeOffset) / 3_600_000.0
        kit.header("TIME DIFFERENCE")
        val sign = if (diffHours >= 0) "+" else "-"
        kit.dotTextCentered("$sign${String.format(Locale.US, "%.1f", kotlin.math.abs(diffHours))}H", kit.height * 0.50f, kit.height * 0.40f, 11f * kit.d,
            accentIndices = setOf(0))
        kit.labelCentered("${zoneCity(home)} → ${zoneCity(away)}", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderDayLength(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val preset = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(preset.timezone)
        val now = Calendar.getInstance(tz)
        val sun = Astronomy.sunTimes(preset.lat, preset.lon, tz, now)
        kit.header("DAYLIGHT • ${preset.name.uppercase()}", widthFraction = 0.95f)
        val rise = sun.sunriseMin
        val set = sun.sunsetMin
        if (rise == null || set == null) {
            kit.labelCentered("NO SUNRISE TODAY", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val minutes = set - rise
        val h = minutes / 60
        val m = minutes % 60
        kit.dotTextCentered(String.format(Locale.US, "%dh%02d", h, m), kit.height * 0.50f, kit.height * 0.34f, 10f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered("OF DAYLIGHT", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }
}
