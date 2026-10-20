package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.astro.Astronomy
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.india.IndiaCalendar
import com.example.data.system.DeviceStatsRepository
import com.example.data.weather.AirQualityRepository
import com.example.data.weather.WeatherRepository
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Phase 11 data widgets: alarm, network, thermal, battery history, air quality, golden hour, usage, India calendar. */
object InsightRenderers {

    fun renderNextAlarm(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("NEXT ALARM")
        val next = DeviceStatsRepository.nextAlarmMs(context)
        if (next == null) {
            kit.labelCentered("NO ALARM SET", kit.height * 0.55f, kit.textPaint(kit.onColor, 12f, 16f))
            return
        }
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(next))
        kit.dotTextCentered(time, kit.height * 0.47f, kit.height * 0.34f, 10f * d, accentIndices = RenderKit.colonIndices(time))
        val mins = ((next - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
        val inText = when {
            mins < 60 -> "IN ${mins}M"
            mins < 24 * 60 -> "IN ${mins / 60}H ${mins % 60}M"
            else -> SimpleDateFormat("EEE dd MMM", Locale.US).format(Date(next)).uppercase()
        }
        kit.labelCentered(inText, kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 14f))
    }

    fun renderNetworkSpeed(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("NETWORK • LAST MINUTE AVG")
        val speed = DeviceStatsRepository.speed(context)
        val colW = kit.maxW / 2
        listOf("DOWN" to speed?.downBytesPerSec, "UP" to speed?.upBytesPerSec).forEachIndexed { i, (label, bps) ->
            val left = kit.pad + i * colW
            val (value, unit) = formatRate(bps)
            val gap = DotMatrixRenderer.fitDotGap(value.length, colW * 0.85f, kit.height * 0.30f, 8f * d)
            DotMatrixRenderer.drawText(canvas, value, left, kit.height * 0.36f, gap * 0.38f, gap, kit.onColor, kit.offColor)
            DotMatrixRenderer.drawFittedLabel(canvas, "$label • $unit", left, kit.height * 0.36f + 7 * gap + 12f * d, colW * 0.9f, kit.textPaint(if (i == 0) kit.accentColor else kit.greyColor, 10f, 13f))
        }
    }

    private fun formatRate(bps: Long?): Pair<String, String> = when {
        bps == null -> "--" to "WAITING"
        bps >= 1_000_000 -> String.format(Locale.US, "%.1f", bps / 1_000_000.0) to "MB/S"
        bps >= 1_000 -> (bps / 1_000).toString() to "KB/S"
        else -> bps.toString() to "B/S"
    }

    fun renderThermalUptime(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val status = DeviceStatsRepository.thermalStatus(context)
        val (word, hot) = when (status) {
            null -> "UNKNOWN" to false
            0 -> "COOL" to false
            1 -> "LIGHT LOAD" to false
            2 -> "WARM" to true
            3 -> "HOT" to true
            else -> "THROTTLING" to true
        }
        kit.header("THERMAL • $word", if (hot) kit.accentColor else kit.greyColor)
        val upMin = DeviceStatsRepository.uptimeMs() / 60_000L
        val up = if (upMin >= 24 * 60) "${upMin / (24 * 60)}D${(upMin / 60) % 24}H" else "${upMin / 60}H${String.format(Locale.US, "%02d", upMin % 60)}"
        kit.dotTextCentered(up, kit.height * 0.48f, kit.height * 0.32f, 9f * d)
        kit.labelCentered("UPTIME SINCE LAST RESTART", kit.height - kit.pad, kit.textPaint(kit.greyColor, 9.5f, 13f))
    }

    fun renderBatteryHistory(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val history = DeviceStatsRepository.batteryHistory(context)
        val now = history.lastOrNull()
        kit.header(if (now == null) "BATTERY • 24H" else "BATTERY • 24H • NOW ${now.level}%")
        if (history.size < 2) {
            kit.labelCentered("COLLECTING… CHECK BACK SOON", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        val top = kit.pad + 26f * d
        val bottom = kit.height - kit.pad - 14f * d
        val start = System.currentTimeMillis() - 24 * 3_600_000L
        history.forEach { s ->
            val x = kit.pad + kit.maxW * ((s.timeMs - start).toFloat() / (24 * 3_600_000f)).coerceIn(0f, 1f)
            val y = bottom - (bottom - top) * s.level / 100f
            canvas.drawCircle(x, y, 2.6f * d, kit.fill(if (s.charging) kit.accentColor else kit.onColor))
        }
        val paint = kit.textPaint(kit.greyColor, 9f, 12f)
        canvas.drawText("-24H", kit.pad, kit.height - kit.pad, paint)
        canvas.drawText("NOW", kit.width - kit.pad - paint.measureText("NOW"), kit.height - kit.pad, paint)
    }

    fun renderAirQuality(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val aq = AirQualityRepository.current(context)
        val place = WeatherRepository.currentPlace(context).name.uppercase()
        if (aq == null) {
            kit.header("AIR QUALITY • $place")
            kit.labelCentered("LOADING…", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        val bad = aq.usAqi > 100
        kit.header("AQI • $place", if (bad) kit.accentColor else kit.greyColor)
        kit.dotTextCentered(aq.usAqi.toString(), kit.height * 0.43f, kit.height * 0.32f, 10f * d, kit.maxW * 0.6f)
        kit.labelCentered(aq.category, kit.height * 0.72f, kit.textPaint(if (bad) kit.accentColor else kit.onColor, 11f, 15f))
        if (spec.sizeBucket != SizeBucket.S) {
            kit.labelCentered(String.format(Locale.US, "PM2.5 %.0f • PM10 %.0f µg/m³", aq.pm25, aq.pm10), kit.height - kit.pad, kit.textPaint(kit.greyColor, 9.5f, 13f))
        }
    }

    fun renderGoldenHour(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val place = WeatherRepository.currentPlace(context)
        val tz = TimeZone.getTimeZone(place.timezone)
        val sun = Astronomy.sunTimes(place.lat, place.lon, tz, Calendar.getInstance(tz))
        kit.header("GOLDEN HOUR • ${place.name.uppercase()}")
        val rise = sun.sunriseMin
        val set = sun.sunsetMin
        if (rise == null || set == null) {
            kit.labelCentered("NO SUNRISE / SUNSET TODAY", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        // Golden hour ≈ the hour after sunrise and before sunset; blue hour ≈ 25 min either side.
        val rows = listOf(
            Triple("MORNING BLUE", rise - 25, rise),
            Triple("MORNING GOLD", rise, rise + 60),
            Triple("EVENING GOLD", set - 60, set),
            Triple("EVENING BLUE", set, set + 25)
        )
        val nowMin = Calendar.getInstance(tz).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val rowH = (kit.height - kit.pad * 2 - 24f * d) / rows.size
        rows.forEachIndexed { i, (label, from, to) ->
            val y = kit.pad + 32f * d + i * rowH
            val active = nowMin in from until to
            DotMatrixRenderer.drawFittedLabel(canvas, label, kit.pad, y, kit.maxW * 0.5f, kit.textPaint(if (active) kit.accentColor else kit.greyColor, 10f, 14f))
            val time = "${hm(from)}–${hm(to)}"
            val p = kit.textPaint(if (active) kit.accentColor else kit.onColor, 11f, 15f)
            canvas.drawText(time, kit.width - kit.pad - p.measureText(time), y, p)
        }
    }

    private fun hm(min: Int): String { val m = ((min % 1440) + 1440) % 1440; return String.format(Locale.US, "%02d:%02d", m / 60, m % 60) }

    fun renderTopApps(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (!AppPermission.UsageAccess.isGranted(context)) return RenderKit.message(spec, canvas, "TOP APPS TODAY", "ALLOW USAGE ACCESS IN THE APP")
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("TOP APPS TODAY")
        val apps = DeviceStatsRepository.topAppsToday(context).orEmpty()
        if (apps.isEmpty()) {
            kit.labelCentered("NO USAGE YET TODAY", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        val shown = apps.take(if (spec.sizeBucket == SizeBucket.L) 5 else 3)
        val maxMs = shown.first().foregroundMs.coerceAtLeast(1)
        val rowH = (kit.height - kit.pad * 2 - 26f * d) / shown.size
        shown.forEachIndexed { i, app ->
            val y = kit.pad + 34f * d + i * rowH
            val mins = (app.foregroundMs / 60_000L).toInt()
            val t = if (mins >= 60) "${mins / 60}H ${mins % 60}M" else "${mins}M"
            val pt = kit.textPaint(kit.onColor, 10.5f, 14f)
            canvas.drawText(t, kit.width - kit.pad - pt.measureText(t), y, pt)
            DotMatrixRenderer.drawFittedLabel(canvas, app.label.uppercase(), kit.pad, y, kit.maxW * 0.55f, kit.textPaint(if (i == 0) kit.accentColor else kit.onColor, 10.5f, 14f))
            DotMatrixRenderer.drawDotRun(canvas, (app.foregroundMs * 100 / maxMs).toInt(), 100, 16, kit.pad, y + 8f * d, 1.8f * d, (kit.maxW * 0.55f) / 15f, kit.onColor, kit.offColor)
        }
    }

    fun renderMobileData(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (!AppPermission.UsageAccess.isGranted(context)) return RenderKit.message(spec, canvas, "MOBILE DATA", "ALLOW USAGE ACCESS IN THE APP")
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("MOBILE DATA TODAY")
        val bytes = DeviceStatsRepository.mobileDataToday(context)
        val (value, unit) = when {
            bytes == null -> "--" to ""
            bytes >= 1_000_000_000 -> String.format(Locale.US, "%.2f", bytes / 1e9) to "GB"
            else -> (bytes / 1_000_000).toString() to "MB"
        }
        kit.dotTextCentered(value, kit.height * 0.47f, kit.height * 0.32f, 10f * d, kit.maxW * 0.7f)
        kit.labelCentered(if (unit.isEmpty()) "WAITING FOR DATA" else unit, kit.height - kit.pad, kit.textPaint(kit.accentColor, 11f, 15f))
    }

    fun renderFestivals(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("FESTIVALS & HOLIDAYS")
        val count = if (spec.sizeBucket == SizeBucket.L) 6 else 3
        val list = IndiaCalendar.upcoming(count)
        val rowH = (kit.height - kit.pad * 2 - 24f * d) / count
        list.forEachIndexed { i, f ->
            val y = kit.pad + 34f * d + i * rowH
            val days = f.daysAway()
            val whenText = when (days) { 0 -> "TODAY"; 1 -> "TOMORROW"; else -> "IN ${days}D" }
            val color = if (days == 0) kit.accentColor else kit.onColor
            DotMatrixRenderer.drawFittedLabel(canvas, f.name.uppercase(), kit.pad, y, kit.maxW * 0.6f, kit.textPaint(color, 11f, 15f))
            val pw = kit.textPaint(color, 10.5f, 14f)
            canvas.drawText(whenText, kit.width - kit.pad - pw.measureText(whenText), y, pw)
            DotMatrixRenderer.drawFittedLabel(canvas, (if (f.approximate) "≈ " else "") + IndiaCalendar.formatDate(f.date), kit.pad, y + 13f * d, kit.maxW * 0.6f, kit.textPaint(kit.greyColor, 9f, 12f))
        }
    }

    fun renderTithi(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val t = IndiaCalendar.tithi()
        kit.header("TITHI • ${t.paksha} PAKSHA")
        val day = if (t.number > 15) t.number - 15 else t.number
        kit.dotTextCentered(day.toString(), kit.height * 0.42f, kit.height * 0.30f, 10f * d, kit.maxW * 0.5f)
        kit.labelCentered(t.name, kit.height * 0.72f, kit.textPaint(kit.accentColor, 12f, 17f))
        kit.labelCentered("≈ FROM MOON PHASE", kit.height - kit.pad, kit.textPaint(kit.greyColor, 9f, 12f))
    }
}
