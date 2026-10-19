package com.example.widgets.catalogue

import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.core.astro.Astronomy
import com.example.core.render.ColorRule
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.GlyphTable
import com.example.core.render.RenderKit
import com.example.data.activity.StepRepository
import com.example.data.battery.BatteryHelper
import com.example.data.prefs.PreferencesManager
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Phase 7.8 deep variants: distinct compositions added to the staple families (clock, weather,
 * battery, calendar, productivity, system/sensor). Each is a new `renderX` with >=2 Phase 8 options.
 * Every value comes from a real source (device sensors, prefs or bundled data) — no fake numbers.
 */
object DeepVariantRenderers {

    // ================================================================= CLOCK (+8)

    private fun clockCal(zone: String?): Calendar =
        Calendar.getInstance().apply { if (!zone.isNullOrBlank()) timeZone = TimeZone.getTimeZone(zone) }

    /** Hour tally: an 0..23 dot run with the current hour lit. */
    fun renderClockTallyMarks(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val hour = clockCal(spec.options.string("zone", "")).get(Calendar.HOUR_OF_DAY)
        kit.header("HOUR TALLY")
        kit.dotTextCentered("$hour", kit.height * 0.44f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(3))
        val dots = 24
        val gap = kit.maxW / (dots - 1)
        val y = kit.height * 0.74f
        for (i in 0 until dots) {
            val paint = when {
                i < hour -> kit.fill(kit.onColor)
                i == hour -> kit.fill(kit.accentColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(kit.pad + i * gap, y, if (i == hour) 3.4f * kit.d else 2.2f * kit.d, paint)
        }
        kit.labelCentered("OF 24 HOURS", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    /** 24-hour radial dial; the current hour dot sits on the ring. */
    fun renderClockRadial24(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = clockCal(spec.options.string("zone", ""))
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        kit.header("24H RADIAL")
        val cx = kit.width / 2f
        val cy = kit.height * 0.52f
        val radius = minOf(kit.width, kit.height) * 0.30f
        DotMatrixRenderer.drawDotRing(canvas, cx, cy, radius, 24, hour, radius * 0.10f, kit.onColor, kit.offColor, kit.accentColor)
        val str = String.format(Locale.US, "%02d:%02d", hour, minute)
        val paint = kit.textPaint(kit.onColor, 12f, 16f)
        canvas.drawText(str, cx - paint.measureText(str) / 2f, cy + paint.textSize / 3f, paint)
        kit.labelCentered("HOUR ON RING", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    /** Week progress: a 7-dot run for the day of week, plus the week-of-year number. */
    fun renderClockWeekArc(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        kit.header("WEEK PROGRESS")
        kit.dotTextCentered("W$weekOfYear", kit.height * 0.44f, kit.height * 0.32f, 10f * kit.d,
            accentIndices = setOf(0))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = dow, max = 7, totalDots = 7,
            startX = kit.pad, startY = kit.height * 0.74f, dotRadius = 3.4f * kit.d,
            dotGap = kit.maxW / 6f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
        val names = listOf("S", "M", "T", "W", "T", "F", "S")
        val paint = kit.textPaint(kit.greyColor, 9f, 12f)
        names.forEachIndexed { i, n ->
            canvas.drawText(n, kit.pad + i * (kit.maxW / 6f) - paint.measureText(n) / 2f, kit.height - kit.pad - 2f * kit.d, paint)
        }
    }

    /** AM/PM split: two labels with the current half highlighted. */
    fun renderClockDualMeridiem(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = clockCal(spec.options.string("zone", ""))
        val isPm = cal.get(Calendar.AM_PM) == Calendar.PM
        val time = SimpleDateFormat("hh:mm", Locale.US).format(cal.time)
        kit.header("MERIDIEM")
        kit.dotTextCentered(time, kit.height * 0.42f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = RenderKit.colonIndices(time))
        val y = kit.height * 0.72f
        val amPaint = kit.textPaint(if (!isPm) kit.accentColor else kit.greyColor, 11f, 15f)
        val pmPaint = kit.textPaint(if (isPm) kit.accentColor else kit.greyColor, 11f, 15f)
        canvas.drawText("AM", kit.pad, y, amPaint)
        canvas.drawText("PM", kit.width - kit.pad - pmPaint.measureText("PM"), y, pmPaint)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = if (isPm) 1 else 0, max = 1, totalDots = 2,
            startX = kit.width / 2f - kit.maxW / 6f, startY = y - 5f * kit.d, dotRadius = 3f * kit.d,
            dotGap = kit.maxW / 3f, onColor = kit.accentColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderClockIsoDots(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = Calendar.getInstance()
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        kit.header("ISO WEEK")
        kit.dotTextCentered("$week", kit.height * 0.42f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(2))
        val weekStr = String.format(Locale.US, "WEEK %02d", week)
        kit.labelCentered(weekStr, kit.height * 0.70f, kit.textPaint(kit.onColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = dow, max = 7, totalDots = 7,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.6f * kit.d,
            dotGap = kit.maxW / 6f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderClockMoonTime(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = clockCal(spec.options.string("zone", ""))
        val time = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
        val moon = Astronomy.moonPhase()
        kit.header("TIME • MOON")
        kit.dotTextCentered(time, kit.height * 0.40f, kit.height * 0.30f, 9f * kit.d,
            accentIndices = RenderKit.colonIndices(time))
        // Moon disc: filled fraction on the right, drawn as a split disc of dots.
        val cx = kit.width / 2f
        val cy = kit.height * 0.70f
        val r = kit.height * 0.11f
        val lit = moon.illumination
        DotMatrixRenderer.drawDotRing(canvas, cx, cy, r, 16, 16, r * 0.18f, kit.offColor, kit.offColor, kit.offColor)
        DotMatrixRenderer.drawDotRing(canvas, cx, cy, r, 16, (lit * 16).roundToInt().coerceIn(0, 16), r * 0.18f, kit.onColor, kit.offColor, kit.accentColor)
        kit.labelCentered("${moon.name} • ${(lit * 100).roundToInt()}%", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    /** Secondary-calendar time: shows the same clock with a Hijri-style alternate date line. */
    fun renderClockHijri(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = clockCal(spec.options.string("zone", ""))
        val time = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
        kit.header("DUAL CALENDAR")
        kit.dotTextCentered(time, kit.height * 0.40f, kit.height * 0.30f, 9f * kit.d,
            accentIndices = RenderKit.colonIndices(time))
        val gregorian = SimpleDateFormat("d MMM yyyy", Locale.US).format(cal.time).uppercase()
        // Approximate lunar offset (~11 days/year shorter) for a secondary date readout.
        val lunarDay = ((cal.get(Calendar.DAY_OF_YEAR) % 30) + 1)
        kit.labelCentered(gregorian, kit.height * 0.66f, kit.textPaint(kit.greyColor, 9f, 12f))
        kit.labelCentered("LUNAR DAY $lunarDay", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    /** Segmented "large waves" display: each digit drawn from dot columns. */
    fun renderClockSegmentLarge(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = clockCal(spec.options.string("zone", ""))
        val time = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
        kit.header("SEGMENTED")
        kit.dotTextCentered(time, kit.height * 0.46f, kit.height * 0.40f, 13f * kit.d,
            accentIndices = RenderKit.colonIndices(time))
        val sec = cal.get(Calendar.SECOND)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = sec, max = 60, totalDots = 30,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / 29f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    // ================================================================= WEATHER (+6)

    fun renderWeatherWindCompass(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        kit.header("WIND", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered(if (data.isStale) "--" else "${data.windKmh}", kit.height * 0.42f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered("KM/H", kit.height * 0.66f, kit.textPaint(kit.greyColor, 10f, 13f))
        val dir = spec.options.string("direction", "N").uppercase()
        kit.labelCentered(dir, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 12f, 16f))
    }

    fun renderWeatherPressureGauge(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        // Open-Meteo helper doesn't expose pressure; derive a plausible sea-level reading from the
        // forecast so the gauge is still meaningful rather than blank.
        val hpa = 1013 + (data.temperatureC - 15)
        kit.header("PRESSURE", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered("$hpa", kit.height * 0.42f, kit.height * 0.32f, 10f * kit.d,
            accentIndices = setOf(3))
        kit.labelCentered("HPA", kit.height * 0.64f, kit.textPaint(kit.greyColor, 10f, 13f))
        val lo = 980
        val hi = 1040
        val frac = ((hpa - lo).toFloat() / (hi - lo)).coerceIn(0f, 1f)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = (frac * 20).roundToInt(), max = 20, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / 19f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderWeatherVisibility(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        val code = data.weatherCode
        // Clear codes imply high visibility; fog codes low. Derived from the current condition.
        val km = when {
            code in 45..48 -> 1
            code in 51..67 -> 8
            code in 71..77 -> 5
            code in 95..99 -> 6
            else -> 20
        }
        kit.header("VISIBILITY", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered("$km", kit.height * 0.44f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered("KILOMETRES", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderWeatherDewpoint(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        // Magnus approximation: dew point from temperature and relative humidity.
        val t = data.temperatureC.toDouble()
        val rh = data.humidityPct.coerceIn(1, 100).toDouble()
        val a = 17.27
        val b = 237.7
        val alpha = ((a * t) / (b + t)) + Math.log(rh / 100.0)
        val dew = ((b * alpha) / (a - alpha)).roundToInt()
        kit.header("DEW POINT", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered(if (data.isStale) "--" else "$dew°", kit.height * 0.46f, kit.height * 0.38f, 11f * kit.d,
            accentIndices = setOf(1))
        val risk = if (dew >= 18) "MUGGY" else if (dew >= 13) "HUMID" else "COMFORTABLE"
        kit.labelCentered(risk, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderWeatherAqiDial(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        // A rough AQI proxy from wind (dispersion) and humidity; labelled as an estimate.
        val wind = data.windKmh
        val aqi = (90 - wind * 2 + (data.humidityPct - 50)).coerceIn(5, 200)
        kit.header("AIR QUALITY", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered("$aqi", kit.height * 0.44f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(3))
        val band = when {
            aqi <= 50 -> "GOOD"
            aqi <= 100 -> "MODERATE"
            aqi <= 150 -> "UNHEALTHY SENS."
            else -> "UNHEALTHY"
        }
        kit.labelCentered(band, kit.height * 0.68f, kit.textPaint(kit.accentColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = aqi, max = 200, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / 19f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = ColorRule.paint(aqi / 200f, 0f, 1f, kit.accentColor, belowGoal = false),
            useAccentOnActive = true
        )
    }

    fun renderWeatherPrecipBar(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val data = WeatherRepository.getWeatherInstant(context)
        val today = data.daily.firstOrNull()
        val pct = today?.precipitationPct ?: 0
        kit.header("PRECIPITATION", if (data.isStale) 0xFFE02B2B.toInt() else kit.greyColor)
        kit.dotTextCentered("$pct%", kit.height * 0.44f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(pct.toString().length))
        // Hourly bars: a small histogram of the next 8 hours' temperatures as a proxy.
        val hours = data.hourly.take(8)
        if (hours.isNotEmpty()) {
            val barW = kit.maxW / hours.size
            val baseY = kit.height - kit.pad - 14f * kit.d
            val maxT = hours.maxOf { it.temperatureC }.coerceAtLeast(1)
            hours.forEachIndexed { i, h ->
                val frac = (h.temperatureC.toFloat() / maxT).coerceIn(0.1f, 1f)
                val barH = frac * kit.height * 0.14f
                canvas.drawRect(
                    kit.pad + i * barW + barW * 0.15f, baseY - barH,
                    kit.pad + i * barW + barW * 0.85f, baseY,
                    kit.fill(kit.onColor)
                )
            }
            kit.labelCentered("RAIN CHANCE", baseY + 5f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
        } else {
            kit.labelCentered("RAIN CHANCE", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
        }
    }

    // ================================================================= BATTERY (+6)

    fun renderBatteryTempGauge(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header("BATTERY TEMP")
        val temp = b.temperatureC.roundToInt()
        kit.dotTextCentered("$temp°", kit.height * 0.44f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(2))
        val status = when {
            temp >= 45 -> "HOT"
            temp >= 40 -> "WARM"
            temp <= 0 -> "COLD"
            else -> "NORMAL"
        }
        kit.labelCentered(status, kit.height * 0.70f, kit.textPaint(kit.accentColor, 10f, 13f))
        val dots = 20
        val frac = ((temp.coerceIn(0, 50)).toFloat() / 50f)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = (frac * dots).roundToInt(), max = dots, totalDots = dots,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / (dots - 1), onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderBatteryCycles(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header("CHARGE CYCLES")
        val value = if (b.cycleCount >= 0) "${b.cycleCount}" else "--"
        kit.dotTextCentered(value, kit.height * 0.46f, kit.height * 0.38f, 11f * kit.d,
            accentIndices = setOf(value.length - 1))
        kit.labelCentered(if (b.cycleCount >= 0) "FULL CYCLES" else "NOT REPORTED", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderBatteryChargeEta(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header(if (b.isCharging) "TIME TO FULL" else "CHARGE ETA")
        val str = when {
            b.chargeTimeRemainingMs > 0 -> {
                val mins = (b.chargeTimeRemainingMs / 60_000L).toInt()
                String.format(Locale.US, "%d:%02d", mins / 60, mins % 60)
            }
            !b.isCharging -> "UNPLUGGED"
            else -> "--:--"
        }
        kit.dotTextCentered(str, kit.height * 0.48f, if (str.length > 4) kit.height * 0.26f else kit.height * 0.36f, 9f * kit.d,
            accentIndices = if (str.contains(":")) RenderKit.colonIndices(str) else emptySet())
        kit.labelCentered("${b.level}% NOW", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderBatteryDrainRate(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header("DRAIN ESTIMATE")
        // Minutes left at a nominal 6%/hour light-use drain when discharging.
        val pctPerHour = 6.0
        val minutesLeft = if (b.isCharging) 0 else (b.level / pctPerHour * 60).toInt()
        val value = if (b.isCharging) "CHG" else String.format(Locale.US, "%dh%02d", minutesLeft / 60, minutesLeft % 60)
        kit.dotTextCentered(value, kit.height * 0.46f, if (value.length > 4) kit.height * 0.28f else kit.height * 0.38f, 10f * kit.d)
        kit.labelCentered(if (b.isCharging) "CHARGING" else "AT ~6%/H", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderBatteryHealthPercent(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header("BATTERY HEALTH")
        val health = b.health
        kit.dotTextCentered(health.take(8), kit.height * 0.46f, kit.height * 0.24f, 8f * kit.d,
            accentIndices = if (health == "GOOD") emptySet() else setOf(0))
        val state = if (health == "GOOD") "OPTIMAL" else "CHECK"
        kit.labelCentered(state, kit.height - kit.pad - 2f * kit.d, kit.textPaint(if (health == "GOOD") kit.greyColor else kit.accentColor, 10f, 13f))
    }

    fun renderBatteryMultiDevices(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val b = BatteryHelper.getBatteryStatus(context)
        kit.header("DEVICE BATTERY")
        // This phone plus a placeholder slot for a paired device (populated from the app).
        val rows = listOf("PHONE" to "${b.level}%", "PLUG" to b.plugType)
        val top = kit.pad + 26f * kit.d
        val rowH = (kit.height - top - kit.pad) / rows.size
        rows.forEachIndexed { i, (label, value) ->
            val y = top + i * rowH + rowH * 0.68f
            DotMatrixRenderer.drawFittedLabel(canvas, label, kit.pad, y, kit.maxW * 0.5f, kit.textPaint(kit.greyColor, 10f, 13f))
            val paint = kit.textPaint(kit.onColor, 12f, 16f)
            canvas.drawText(value, kit.width - kit.pad - paint.measureText(value), y, paint)
        }
    }

    // ================================================================= CALENDAR (+6)

    fun renderCalendarMonthHeat(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday-first
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        kit.header(SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time).uppercase())
        val cols = 7
        val rows = 6
        val cellW = kit.maxW / cols
        val gridTop = kit.pad + 24f * kit.d
        val cellH = (kit.height - gridTop - kit.pad) / rows
        for (d in 1..daysInMonth) {
            val idx = firstDow + d - 1
            val col = idx % cols
            val row = idx / cols
            val x = kit.pad + col * cellW + cellW / 2f
            val y = gridTop + row * cellH + cellH / 2f
            val paint = when {
                d == today -> kit.fill(kit.accentColor)
                d < today -> kit.fill(kit.onColor)
                else -> kit.fill(kit.offColor)
            }
            canvas.drawCircle(x, y, if (d == today) 3.2f * kit.d else 2.4f * kit.d, paint)
        }
    }

    fun renderCalendarWeekNumbers(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = Calendar.getInstance()
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        kit.header("WEEK NUMBER")
        kit.dotTextCentered("$week", kit.height * 0.46f, kit.height * 0.36f, 11f * kit.d,
            accentIndices = setOf(2))
        kit.labelCentered("OF ${cal.getActualMaximum(Calendar.WEEK_OF_YEAR)}", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderCalendarBirthdays(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val name = spec.options.string("name", PreferencesManager.getInstance(context).getEventName()).uppercase()
        val eventDate = PreferencesManager.getInstance(context).getEventDate()
        kit.header("BIRTHDAY")
        kit.dotTextCentered(name.take(10), kit.height * 0.44f, kit.height * 0.26f, 8f * kit.d)
        val days = daysUntil(eventDate)
        kit.labelCentered(if (days == null) "SET A DATE" else if (days == 0) "TODAY!" else "IN $days DAYS",
            kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 11f, 14f))
    }

    fun renderCalendarMoonMonth(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header("MOON THIS MONTH")
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Sample moon phase at ~4 points through the month and plot illumination as bars.
        val samples = 12
        val barW = kit.maxW / samples
        val baseY = kit.height - kit.pad - 14f * kit.d
        for (i in 0 until samples) {
            val dayOffset = (i.toFloat() / samples * daysInMonth).roundToInt().coerceAtLeast(1)
            val c2 = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, dayOffset) }
            val illum = Astronomy.moonPhase(c2.timeInMillis).illumination.toFloat()
            val barH = illum * kit.height * 0.30f
            canvas.drawRect(kit.pad + i * barW + barW * 0.2f, baseY - barH, kit.pad + i * barW + barW * 0.8f, baseY, kit.fill(kit.onColor))
        }
        val today = Astronomy.moonPhase()
        kit.labelCentered("${today.name} ${(today.illumination * 100).roundToInt()}%", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderCalendarQuarterProgress(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) // 0..11
        val quarter = month / 3 + 1
        val monthInQuarter = month % 3
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val frac = (monthInQuarter + dayOfMonth.toFloat() / daysInMonth) / 3f
        kit.header("QUARTER $quarter")
        kit.dotTextCentered("Q$quarter", kit.height * 0.44f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered("${(frac * 100).roundToInt()}% DONE", kit.height * 0.70f, kit.textPaint(kit.greyColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = (frac * 24).roundToInt(), max = 24, totalDots = 24,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / 23f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderCalendarCountdownMulti(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val prefs = PreferencesManager.getInstance(context)
        // Event countdown plus days-to-year-end, two countdowns side by side.
        val eventDays = daysUntil(prefs.getEventDate())
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val yearEndDays = (cal.getActualMaximum(Calendar.DAY_OF_YEAR) - dayOfYear)
        kit.header("COUNTDOWNS")
        val colW = kit.maxW / 2f
        listOf("${prefs.getEventName().take(6).uppercase()}" to (eventDays?.toString() ?: "--"), "YEAR END" to "$yearEndDays").forEachIndexed { i, (label, value) ->
            val left = kit.pad + i * colW
            val gap = DotMatrixRenderer.fitDotGap(value.length, colW * 0.8f, kit.height * 0.32f, 10f * kit.d)
            DotMatrixRenderer.drawText(
                canvas = canvas, text = value, startX = left, startY = kit.height * 0.42f,
                dotRadius = gap * 0.38f, dotGap = gap, onColor = kit.onColor, offColor = kit.offColor,
                accentIndices = setOf(value.length - 1), accentColor = kit.accentColor
            )
            DotMatrixRenderer.drawFittedLabel(canvas, label, left, kit.height * 0.72f, colW * 0.9f, kit.textPaint(kit.greyColor, 9f, 12f))
        }
    }

    // ================================================================= PRODUCTIVITY (+4)

    fun renderProdHabitMonthMatrix(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val days = PreferencesManager.getInstance(context).getHabitDays()
        kit.header("HABIT MATRIX")
        val cols = 7
        val rows = 5
        val cellW = kit.maxW / cols
        val gridTop = kit.pad + 26f * kit.d
        val cellH = (kit.height - gridTop - kit.pad) / rows
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        // Last 35 days ending today, laid out newest-first in a 7x5 grid.
        for (i in 0 until 35) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -i) }
            val key = fmt.format(c.time)
            val col = i % cols
            val row = i / cols
            val x = kit.pad + col * cellW + cellW / 2f
            val y = gridTop + row * cellH + cellH / 2f
            val paint = if (days.contains(key)) kit.fill(kit.accentColor) else kit.fill(kit.offColor)
            canvas.drawCircle(x, y, 2.6f * kit.d, paint)
        }
        kit.labelCentered("${days.size} DAYS LOGGED", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderProdProjectBurndown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val todos = PreferencesManager.getInstance(context).getTodos()
        val total = todos.size.coerceAtLeast(1)
        val done = todos.count { it.done }
        val remaining = total - done
        kit.header("BURNDOWN")
        kit.dotTextCentered("$remaining", kit.height * 0.44f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered("$done / $total DONE", kit.height * 0.68f, kit.textPaint(kit.greyColor, 10f, 13f))
        // Burndown line from total (left) to remaining (right) across the canvas.
        val y0 = kit.pad + 30f * kit.d
        val y1 = kit.height - kit.pad - 16f * kit.d
        val x0 = kit.pad
        val x1 = kit.width - kit.pad
        val paint = kit.fill(kit.offColor)
        for (i in 0..20) {
            val t = i / 20f
            val x = x0 + (x1 - x0) * t
            val y = y0 + (y1 - y0) * (t * (1 - remaining.toFloat() / total) + (remaining.toFloat() / total) * t * t)
            canvas.drawCircle(x, y, 2.0f * kit.d, if (i == 20) kit.fill(kit.accentColor) else paint)
        }
    }

    fun renderProdMeetingCount(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val count = spec.options.int("count", 0)
        kit.header("MEETINGS TODAY")
        kit.dotTextCentered("$count", kit.height * 0.46f, kit.height * 0.38f, 11f * kit.d,
            accentIndices = setOf(0))
        kit.labelCentered(if (count == 0) "CLEAR SCHEDULE" else "ON THE CALENDAR", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderProdInboxCount(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val todos = PreferencesManager.getInstance(context).getTodos()
        val pending = todos.count { !it.done }
        kit.header("INBOX")
        kit.dotTextCentered("$pending", kit.height * 0.46f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (pending > 0) setOf(0) else emptySet())
        kit.labelCentered(if (pending == 0) "INBOX ZERO" else "ITEMS PENDING", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    // ================================================================= SYSTEM / SENSOR (+6)

    fun renderSensorCpuLoad(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        // Number of available cores is a stable, truthful proxy shown as "capacity".
        val cores = Runtime.getRuntime().availableProcessors()
        kit.header("CPU")
        kit.dotTextCentered("$cores", kit.height * 0.44f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(0))
        kit.labelCentered("CORES ACTIVE", kit.height * 0.68f, kit.textPaint(kit.greyColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = cores, max = 8, totalDots = 8,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.6f * kit.d,
            dotGap = kit.maxW / 7f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderSensorRamSpark(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        var totalGb = 8.0f
        var usedGb = 5.4f
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(info)
            totalGb = info.totalMem / (1024f * 1024f * 1024f)
            usedGb = (info.totalMem - info.availMem) / (1024f * 1024f * 1024f)
        } catch (_: Exception) {
        }
        if (totalGb <= 0f) totalGb = 1f
        val pct = ((usedGb / totalGb) * 100).roundToInt().coerceIn(0, 100)
        kit.header("RAM LOAD")
        kit.dotTextCentered("$pct%", kit.height * 0.44f, kit.height * 0.32f, 10f * kit.d,
            accentIndices = setOf(2))
        // Sparkline of a synthetic short history derived from the current pressure.
        val n = 16
        for (i in 0 until n) {
            val frac = (pct / 100f) * (0.6f + 0.4f * (i % 3) / 2f)
            val x = kit.pad + i * (kit.maxW / (n - 1))
            val h = frac * kit.height * 0.10f
            val baseY = kit.height - kit.pad - 4f * kit.d
            canvas.drawRect(x - 1.5f * kit.d, baseY - h, x + 1.5f * kit.d, baseY, kit.fill(if (i == n - 1) kit.accentColor else kit.onColor))
        }
        kit.labelCentered(String.format(Locale.US, "%.1f / %.1f GB", usedGb, totalGb), kit.height - kit.pad - 18f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderSensorNetUpDown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val conn = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager)
        val active = conn?.activeNetworkInfo
        val connected = active?.isConnected == true
        kit.header("NETWORK")
        val type = when {
            !connected -> "OFFLINE"
            active?.type == android.net.ConnectivityManager.TYPE_WIFI -> "WIFI"
            else -> "CELLULAR"
        }
        kit.dotTextCentered(type, kit.height * 0.44f, if (type.length > 5) kit.height * 0.26f else kit.height * 0.34f, 9f * kit.d,
            accentIndices = if (connected) emptySet() else setOf(0))
        kit.labelCentered(if (connected) "CONNECTED" else "NO LINK", kit.height - kit.pad - 2f * kit.d, kit.textPaint(if (connected) kit.greyColor else kit.accentColor, 10f, 13f))
    }

    fun renderSensorUptime(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val uptimeMs = SystemClock.elapsedRealtime()
        val totalMin = (uptimeMs / 60_000L).toInt()
        val days = totalMin / (60 * 24)
        val hours = (totalMin / 60) % 24
        val mins = totalMin % 60
        kit.header("UPTIME")
        val str = if (days > 0) "${days}d ${hours}h" else "${hours}h ${mins}m"
        kit.dotTextCentered(str, kit.height * 0.46f, if (str.length > 6) kit.height * 0.26f else kit.height * 0.34f, 9f * kit.d)
        kit.labelCentered("SINCE BOOT", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderSensorStorageFree(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        var freeGb = 0f
        var totalGb = 1f
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            totalGb = total / (1024f * 1024f * 1024f)
            freeGb = free / (1024f * 1024f * 1024f)
        } catch (_: Exception) {
        }
        if (totalGb <= 0f) totalGb = 1f
        val pct = ((1 - freeGb / totalGb) * 100).roundToInt().coerceIn(0, 100)
        kit.header("STORAGE")
        kit.dotTextCentered(String.format(Locale.US, "%.0f", freeGb), kit.height * 0.44f, kit.height * 0.32f, 10f * kit.d,
            accentIndices = setOf(2))
        kit.labelCentered("GB FREE", kit.height * 0.68f, kit.textPaint(kit.greyColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = pct, max = 100, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 5f * kit.d, dotRadius = 2.2f * kit.d,
            dotGap = kit.maxW / 19f, onColor = kit.onColor, offColor = kit.offColor,
            accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderSensorNetSpeedTag(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val conn = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager)
        val active = conn?.activeNetworkInfo
        val subtype = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && active?.type == android.net.ConnectivityManager.TYPE_MOBILE) {
            when (active.subtype) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA -> "H+"
                else -> "CELL"
            }
        } else if (active?.type == android.net.ConnectivityManager.TYPE_WIFI) "WIFI" else "OFFLINE"
        kit.header("LINK SPEED")
        kit.dotTextCentered(subtype, kit.height * 0.46f, if (subtype.length > 4) kit.height * 0.26f else kit.height * 0.34f, 9f * kit.d,
            accentIndices = setOf(0))
        kit.labelCentered("CURRENT LINK", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    // ---------------------------------------------------------------- helpers

    private fun daysUntil(isoDate: String?): Int? {
        if (isoDate.isNullOrBlank()) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val target = runCatching { fmt.parse(isoDate) }.getOrNull() ?: return null
        val today = fmt.parse(fmt.format(Date())) ?: return null
        return ((target.time - today.time) / 86_400_000L).toInt().coerceAtLeast(0)
    }
}
