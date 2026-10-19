package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.battery.BatteryHelper
import com.example.data.battery.BluetoothBatteryRepository
import com.example.data.market.CryptoRepository
import com.example.data.usage.ScreenTimeRepository
import com.example.widgets.base.WidgetSpec
import java.util.Locale
import kotlin.math.abs

/** Widgets backed by system stats or online data: screen time, crypto price, device batteries. */
object LiveDataRenderers {

    fun renderScreenTime(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (!ScreenTimeRepository.isAvailable(context)) {
            return RenderKit.message(spec, canvas, "SCREEN TIME", "ALLOW USAGE ACCESS IN THE APP")
        }
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val stats = ScreenTimeRepository.current(context)
        kit.header("SCREEN TIME TODAY")

        val minutes = ((stats?.screenOnMs ?: 0L) / 60_000L).toInt()
        val value = if (stats == null) "--" else if (minutes >= 60) "${minutes / 60}H${String.format(Locale.US, "%02d", minutes % 60)}" else "${minutes}M"
        val gap = DotMatrixRenderer.fitDotGap(value.length, kit.maxW * 0.55f, kit.height * 0.34f, 10f * d)
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = value,
            startX = kit.pad,
            startY = kit.height * 0.30f,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = kit.onColor,
            offColor = kit.offColor,
            accentIndices = value.indices.filter { value[it] == 'H' || value[it] == 'M' }.toSet(),
            accentColor = kit.accentColor
        )
        DotMatrixRenderer.drawFittedLabel(canvas, "${stats?.unlocks ?: 0} UNLOCKS", kit.pad, kit.height - kit.pad, kit.maxW * 0.5f, kit.textPaint(kit.greyColor, 10f, 13f))

        // Last 7 days as dot columns (today in the accent colour)
        val days = stats?.lastSevenDaysMs.orEmpty()
        if (days.isNotEmpty()) {
            val left = kit.width * 0.62f
            val colGap = (kit.width - kit.pad - left) / 6f
            val maxDots = 6
            val maxMs = days.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            val bottom = kit.height - kit.pad - 4f * d
            val dotGap = (kit.height * 0.55f) / maxDots
            days.forEachIndexed { i, ms ->
                val lit = ((ms.toFloat() / maxMs) * maxDots).toInt().coerceIn(if (ms > 0) 1 else 0, maxDots)
                for (k in 0 until maxDots) {
                    val on = k < lit
                    val color = if (!on) kit.offColor else if (i == days.lastIndex) kit.accentColor else kit.onColor
                    canvas.drawCircle(left + i * colGap, bottom - k * dotGap, if (on) 2.8f * d else 2.0f * d, kit.fill(color))
                }
            }
        }
    }

    /** Dedicated 7-day screen-time chart: taller bars per weekday with a total readout. */
    fun renderWeeklyScreenTime(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (!ScreenTimeRepository.isAvailable(context)) {
            return RenderKit.message(spec, canvas, "SCREEN TIME", "ALLOW USAGE ACCESS IN THE APP")
        }
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val stats = ScreenTimeRepository.current(context)
        val days = stats?.lastSevenDaysMs.orEmpty()
        kit.header("SCREEN TIME • 7 DAYS")

        if (days.isEmpty()) {
            kit.labelCentered("NO DATA YET", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 15f))
            return
        }

        val totalMs = days.sum()
        val totalMin = (totalMs / 60_000L).toInt()
        val avg = totalMin / days.size
        val summary = "AVG ${avg / 60}H${String.format(Locale.US, "%02d", avg % 60)}M"
        kit.labelCentered(summary, kit.height * 0.26f, kit.textPaint(kit.onColor, 11f, 15f))

        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        val todayDow = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        val bottom = kit.height - kit.pad - 14f * d
        val top = kit.height * 0.34f
        val maxDots = 9
        val maxMs = days.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val colW = kit.maxW / days.size
        val dotGap = (bottom - top) / maxDots
        days.forEachIndexed { i, ms ->
            val cx = kit.pad + i * colW + colW / 2f
            val lit = ((ms.toFloat() / maxMs) * maxDots).toInt().coerceIn(if (ms > 0) 1 else 0, maxDots)
            for (k in 0 until maxDots) {
                val on = k < lit
                val color = if (!on) kit.offColor else if (i == days.lastIndex) kit.accentColor else kit.onColor
                canvas.drawCircle(cx, bottom - k * dotGap, if (on) 3.0f * d else 2.0f * d, kit.fill(color))
            }
            val label = labels[i]
            val p = kit.textPaint(if (i == todayDow) kit.accentColor else kit.greyColor, 8f, 11f)
            canvas.drawText(label, cx - p.measureText(label) / 2f, bottom + 12f * d, p)
        }
    }

    fun renderCrypto(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val quote = CryptoRepository.quote(context)
        if (quote == null) {
            kit.header("BITCOIN")
            kit.labelCentered("LOADING PRICE…", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        kit.header("BTC / ${quote.currency} • 24H")

        // Indian rupee prices are shown in lakh (e.g. 59.62L) to fit the dot display.
        val value = if (quote.currency == "INR") String.format(Locale.US, "%.2fL", quote.price / 100_000) else String.format(Locale.US, "%.0f", quote.price)
        val gap = DotMatrixRenderer.fitDotGap(value.length, kit.maxW * 0.62f, kit.height * 0.30f, 9f * d)
        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = value,
            startX = kit.pad,
            startY = kit.height * 0.30f,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = kit.onColor,
            offColor = kit.offColor
        )

        val up = quote.change24hPct >= 0
        val change = String.format(Locale.US, "%s%.1f%% %s", if (up) "+" else "-", abs(quote.change24hPct), if (up) "▲" else "▼")
        val paintChange = kit.textPaint(if (up) kit.onColor else kit.accentColor, 11f, 15f)
        canvas.drawText(change, kit.width - kit.pad - paintChange.measureText(change), kit.height * 0.30f + 3 * gap, paintChange)

        // 24 h sparkline as dots
        val history = quote.history
        if (history.size >= 2) {
            val min = history.min()
            val range = (history.max() - min).coerceAtLeast(1e-9)
            val top = kit.height * 0.66f
            val h = kit.height - kit.pad - top
            val step = kit.maxW / (history.size - 1)
            history.forEachIndexed { i, p ->
                val y = top + h - ((p - min) / range * h).toFloat()
                val last = i == history.lastIndex
                canvas.drawCircle(kit.pad + i * step, y, if (last) 3.4f * d else 2.0f * d, kit.fill(if (last) kit.accentColor else kit.onColor))
            }
        }
    }

    /** Phone battery plus connected Bluetooth devices (earbuds, watch...), [slots] in a row. */
    fun renderDeviceBatteries(context: Context, spec: WidgetSpec, canvas: Canvas, slots: Int) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("DEVICE BATTERY")

        val phone = BatteryHelper.getBatteryStatus(context)
        val others = BluetoothBatteryRepository.connectedDevices(context)
        val items = buildList {
            add(Triple("PHONE", phone.level as Int?, phone.isCharging))
            others?.take(slots - 1)?.forEach { add(Triple(it.name.uppercase(), it.level, false)) }
        }

        if (spec.sizeBucket == SizeBucket.L) {
            renderDeviceBatteriesLarge(spec, canvas, kit, items, slots)
            return
        }

        val slotW = kit.maxW / slots
        val cy = kit.height * 0.52f
        val radius = minOf(slotW * 0.36f, kit.height * 0.22f)
        items.forEachIndexed { i, (name, level, charging) ->
            val cx = kit.pad + i * slotW + slotW / 2f
            DotMatrixRenderer.drawDotRing(
                canvas = canvas,
                centerX = cx,
                centerY = cy,
                radius = radius,
                totalDots = 16,
                filledDots = level?.let { it * 16 / 100 } ?: 0,
                dotRadius = radius * 0.12f,
                onColor = kit.onColor,
                offColor = kit.offColor,
                accentColor = if (charging) kit.accentColor else kit.onColor
            )
            val pct = level?.let { "$it%" } ?: "--"
            val paintPct = kit.textPaint(if (charging) kit.accentColor else kit.onColor, 11f, 14f)
            canvas.drawText(pct, cx - paintPct.measureText(pct) / 2f, cy + paintPct.textSize / 3f, paintPct)
            val paintName = kit.textPaint(kit.greyColor, 9f, 12f)
            DotMatrixRenderer.drawFittedLabel(canvas, name, cx - slotW * 0.45f, cy + radius + 16f * d, slotW * 0.9f, paintName)
        }

        // Explain empty slots instead of inventing devices.
        if (items.size == 1) {
            val hint = if (others == null) "ALLOW NEARBY DEVICES IN THE APP" else "NO BLUETOOTH DEVICES CONNECTED"
            val x = kit.pad + slotW + 8f * d
            DotMatrixRenderer.drawFittedLabel(canvas, hint, x, cy + 4f * d, kit.width - kit.pad - x, kit.textPaint(kit.greyColor, 10f, 13f))
        }
    }

    /** 4×4 device battery: rings laid out in two columns (2×2 for quad, one row for combo). */
    private fun renderDeviceBatteriesLarge(
        spec: WidgetSpec,
        canvas: Canvas,
        kit: RenderKit,
        items: List<Triple<String, Int?, Boolean>>,
        slots: Int
    ) {
        val d = kit.d
        val cols = 2
        val rows = if (slots >= 4) 2 else 1
        val cellW = kit.maxW / cols
        val areaTop = kit.pad + 26f * d
        val cellH = (kit.height - areaTop - kit.pad) / rows
        val radius = minOf(cellW * 0.30f, cellH * 0.30f)

        items.forEachIndexed { i, (name, level, charging) ->
            val col = i % cols
            val row = i / cols
            val cx = kit.pad + col * cellW + cellW / 2f
            val cy = areaTop + row * cellH + cellH * 0.40f
            DotMatrixRenderer.drawDotRing(
                canvas = canvas,
                centerX = cx,
                centerY = cy,
                radius = radius,
                totalDots = 20,
                filledDots = level?.let { it * 20 / 100 } ?: 0,
                dotRadius = radius * 0.12f,
                onColor = kit.onColor,
                offColor = kit.offColor,
                accentColor = if (charging) kit.accentColor else kit.onColor
            )
            val pct = level?.let { "$it%" } ?: "--"
            val paintPct = kit.textPaint(if (charging) kit.accentColor else kit.onColor, 12f, 18f)
            canvas.drawText(pct, cx - paintPct.measureText(pct) / 2f, cy + paintPct.textSize / 3f, paintPct)
            val paintName = kit.textPaint(kit.greyColor, 8f, 12f)
            DotMatrixRenderer.drawFittedLabel(canvas, name, cx - cellW * 0.45f, cy + radius + 16f * d, cellW * 0.9f, paintName)
        }

        if (items.size == 1) {
            val hint = "ALLOW NEARBY DEVICES IN THE APP"
            val cx = kit.pad + cellW + cellW / 2f
            DotMatrixRenderer.drawFittedLabel(canvas, hint, cx - cellW * 0.45f, areaTop + cellH * 0.40f + 6f * d, cellW * 0.9f, kit.textPaint(kit.greyColor, 9f, 13f))
        }
    }
}
