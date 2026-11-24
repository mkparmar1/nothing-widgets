package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Watch
import com.example.data.battery.BatteryHelper
import com.example.data.battery.BatteryStatus
import com.example.data.battery.BluetoothBatteryRepository
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight
import java.util.Locale
import kotlin.math.roundToInt

/** Extra designs for the battery family (Batch 1, items 39–50). */
object BatteryStyles {

    private val boldRing = WidgetStyle("bold_ring", "Bold Ring", StyleFamily.BOLD) { c, s, cv -> renderRing(c, s, cv, StyleFamily.BOLD) }
    private val softRing = WidgetStyle("soft_ring", "Soft Ring", StyleFamily.SOFT) { c, s, cv -> renderRing(c, s, cv, StyleFamily.SOFT) }
    private val cell = WidgetStyle("cell", "Battery Cell", StyleFamily.SPECIAL, render = ::renderCell)
    /** [estimatesDrain]: the Fuel Gauge shows time left on battery; Charge ETA only a time to full. */
    private fun timeLeft(estimatesDrain: Boolean) = listOf(
        WidgetStyle("time_left", "Time Left", StyleFamily.BOLD) { c, s, cv -> renderTimeLeft(c, s, cv, StyleFamily.BOLD, estimatesDrain) },
        WidgetStyle("soft_time_left", "Soft Time Left", StyleFamily.SOFT) { c, s, cv -> renderTimeLeft(c, s, cv, StyleFamily.SOFT, estimatesDrain) }
    )
    private val softPill = WidgetStyle("soft_pill", "Soft Pill", StyleFamily.SOFT, render = ::renderPill)

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "battery.ring" to listOf(boldRing, softRing, cell),
        "battery.percentage" to listOf(cell, boldRing),
        "battery.minimal.pill" to listOf(softPill, boldRing),
        "battery.fuel.gauge" to timeLeft(estimatesDrain = true),
        "battery.charge.eta" to timeLeft(estimatesDrain = false),
        "battery.charging.active" to listOf(
            WidgetStyle("bolt_circle", "Bolt Circle", StyleFamily.BOLD) { c, s, cv -> renderBoltCircle(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_bolt", "Soft Bolt", StyleFamily.SOFT) { c, s, cv -> renderBoltCircle(c, s, cv, StyleFamily.SOFT) }
        ),
        "battery.segmented" to listOf(cell, softPill),
        "battery.multi.devices" to listOf(softPill),
        "battery.combo.devices" to listOf(
            WidgetStyle("soft_rows", "Device Rows", StyleFamily.SOFT, render = ::renderDeviceRows)
        ),
        "battery.temp.gauge" to listOf(
            WidgetStyle("bold_temp", "Bold Temp", StyleFamily.BOLD) { c, s, cv -> renderTemp(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_temp", "Soft Temp", StyleFamily.SOFT) { c, s, cv -> renderTemp(c, s, cv, StyleFamily.SOFT) }
        ),
        "battery.health.percent" to listOf(
            WidgetStyle("soft_health", "Soft Card", StyleFamily.SOFT, render = ::renderHealth)
        )
    )

    private fun status(context: Context): BatteryStatus = BatteryHelper.getBatteryStatus(context)

    /** Green while healthy/charging, the alert red when low (mockup's green ring). */
    private fun levelColor(k: StyleKit, b: BatteryStatus, neutral: Int): Int = when {
        b.level <= 20 && !b.isCharging && k.spec.options.boolean("warn_low", true) -> k.negative
        b.isCharging || k.bold -> k.positive
        else -> neutral
    }

    private fun stateLine(b: BatteryStatus): String = when {
        b.level >= 100 && b.isPlugged -> "Fully charged"
        b.isCharging -> "Charging"
        else -> "Battery"
    }

    /** Ring with ⚡ and "78%", "Battery" underneath (mockup battery tile). */
    fun renderRing(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val b = status(context)
        val color = levelColor(k, b, k.ink)
        if (k.isWide) {
            val r = (k.bottom - k.top) * 0.44f
            val x = k.left + r + 2f * k.u
            k.ring(x, k.cy, r, 4.5f * k.u, b.level / 100f, color)
            if (b.isCharging) k.icon(Icons.Filled.Bolt, x, k.cy, r * 0.8f, color)
            val tx = x + r + 9f * k.u
            k.text("${b.level}%", tx, k.cy + 2f * k.u, 32f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(stateLine(b), tx, k.cy + 21f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
            return
        }
        val labelBand = if (spec.showLabel) 14f * k.u else 0f
        val r = minOf(k.contentW, k.bottom - k.top - labelBand) * 0.46f
        val y = k.top + r + 1f * k.u
        k.ring(k.cx, y, r, 4.5f * k.u, b.level / 100f, color)
        if (b.isCharging) k.icon(Icons.Filled.Bolt, k.cx, y - r * 0.42f, r * 0.36f, color)
        k.text("${b.level}%", k.cx, y + r * 0.2f, r / k.u * 0.5f, k.ink, Weight.SEMIBOLD, Align.CENTER, r * 1.5f)
        if (spec.showLabel) k.text(stateLine(b), k.cx, k.bottom - 1f * k.u, 10.5f, k.ink, Weight.MEDIUM, Align.CENTER)
    }

    /** A drawn battery cell, vertical on square tiles and horizontal on wide ones. */
    fun renderCell(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.BOLD)
        k.card()
        val b = status(context)
        val color = levelColor(k, b, k.positive)
        val f = b.level / 100f
        if (k.isWide) {
            val h = (k.bottom - k.top) * 0.5f
            val l = k.left
            val r = k.left + k.contentW * 0.55f
            val t = k.cy - h / 2f
            k.roundRect(l, t, r, t + h, h * 0.22f, k.track)
            k.roundRect(r + 1.2f * k.u, k.cy - h * 0.2f, r + 3.4f * k.u, k.cy + h * 0.2f, 1f * k.u, k.track)
            val inset = 1.6f * k.u
            k.roundRect(l + inset, t + inset, l + inset + (r - l - 2 * inset) * f, t + h - inset, h * 0.16f, color)
            if (b.isCharging) k.icon(Icons.Filled.Bolt, (l + r) / 2f, k.cy, h * 0.7f, k.ink)
            val tx = r + 8f * k.u
            k.text("${b.level}%", tx, k.cy + 2f * k.u, 26f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(stateLine(b), tx, k.cy + 21f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
            return
        }
        val w = k.contentW * 0.36f
        val t = k.top + 5f * k.u
        val bt = k.bottom - (if (spec.showLabel) 14f * k.u else 2f * k.u)
        val l = k.cx - w / 2f
        val r = k.cx + w / 2f
        k.roundRect(k.cx - w * 0.2f, t - 3f * k.u, k.cx + w * 0.2f, t + 1f * k.u, 1f * k.u, k.track)
        k.roundRect(l, t, r, bt, w * 0.2f, k.track)
        val inset = 1.8f * k.u
        val fillTop = bt - inset - (bt - t - 2 * inset) * f
        k.roundRect(l + inset, fillTop, r - inset, bt - inset, w * 0.14f, color)
        if (b.isCharging) k.icon(Icons.Filled.Bolt, k.cx, (t + bt) / 2f, w * 0.7f, k.ink)
        if (spec.showLabel) k.text("${b.level}%", k.cx, k.bottom - 1f * k.u, 12f, k.ink, Weight.SEMIBOLD, Align.CENTER)
    }

    /** Capsule battery glyph + "78%" + plug type on a soft card. */
    fun renderPill(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val b = status(context)
        k.title("Battery")
        val color = levelColor(k, b, k.ink)
        val barTop = k.bottom - 7f * k.u
        k.pill(k.left, barTop, k.right, k.bottom, b.level / 100f, color)
        k.text("${b.level}%", k.left, barTop - 5f * k.u, if (k.isWide) 30f else 26f, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW * 0.6f)
        val plug = when {
            b.isCharging && b.plugType != "NONE" -> StyleData.titleCase(b.plugType)
            b.isCharging -> "Charging"
            else -> "On battery"
        }
        if (spec.options.boolean("show_plug", true)) {
            k.text(plug, k.right, barTop - 5f * k.u, 10f, k.sub, Weight.MEDIUM, Align.RIGHT, k.contentW * 0.4f)
        }
    }

    /** "⚡ 78% · 2h 45m left" (mockup battery card). Same estimates as the classic widgets. */
    fun renderTimeLeft(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily, estimatesDrain: Boolean) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val b = status(context)
        val line = when {
            b.level >= 100 && b.isPlugged -> "Fully charged"
            b.isCharging && b.chargeTimeRemainingMs > 0 -> "Full in ${StyleData.duration((b.chargeTimeRemainingMs / 60_000L).toInt())}"
            b.isCharging -> "Charging"
            !estimatesDrain -> "Not charging"
            // The classic Fuel Gauge's own estimate (level / 1.4 hours).
            else -> "About ${(b.level / 1.4f).toInt()}h left"
        }
        val color = levelColor(k, b, k.ink)
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.4f else if (k.isLarge) 36f * k.u else 18f * k.u
        k.icon(Icons.Filled.Bolt, if (k.isWide) k.left + iconSize / 2f else k.cx, if (k.isWide) k.cy else k.top + iconSize / 2f, iconSize, color)
        if (k.isWide) {
            val tx = k.left + iconSize + 6f * k.u
            k.text("${b.level}%", tx, k.cy + 0f * k.u, 34f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(line, tx, k.cy + 20f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
        } else {
            k.text("${b.level}%", k.cx, k.cy + 12f * k.u, if (k.isLarge) 48f else 28f, k.ink, Weight.SEMIBOLD, Align.CENTER)
            k.text(line, k.cx, k.bottom - 1f * k.u, if (k.isLarge) 13f else 10f, k.sub, Weight.MEDIUM, Align.CENTER)
        }
        if (k.isLarge && !k.isWide) k.pill(k.left, k.bottom - 20f * k.u, k.right, k.bottom - 15f * k.u, b.level / 100f, color)
    }

    /** Bolt inside a ring (mockup charging circle). */
    fun renderBoltCircle(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val b = status(context)
        val color = if (b.isCharging) k.positive else k.sub
        val r = if (k.isWide) (k.bottom - k.top) * 0.44f else minOf(k.contentW, k.bottom - k.top) * 0.42f
        val x = if (k.isWide) k.left + r + 2f * k.u else k.cx
        val y = if (k.isWide) k.cy else k.cy - 4f * k.u
        k.circle(x, y, r, k.surface)
        k.ring(x, y, r, 2.6f * k.u, b.level / 100f, color, k.track)
        k.icon(Icons.Filled.Bolt, x, y, r * 1.0f, if (b.isCharging) k.ink else k.sub)
        val label = if (b.isCharging) "Charging · ${b.level}%" else "Not charging · ${b.level}%"
        if (k.isWide) {
            val tx = x + r + 8f * k.u
            k.text(if (b.isCharging) "Charging" else "Not charging", tx, k.cy, 15f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            val volts = if (b.voltageMv > 0) String.format(Locale.US, " · %.2f V", b.voltageMv / 1000f) else ""
            k.text("${b.level}%$volts", tx, k.cy + 14f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
        } else {
            k.text(label, k.cx, k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.CENTER)
        }
    }

    private fun deviceIcon(name: String) = when {
        name.contains("bud", true) || name.contains("pod", true) || name.contains("ear", true) || name.contains("head", true) -> Icons.Filled.Headphones
        name.contains("watch", true) -> Icons.Filled.Watch
        else -> Icons.Filled.Bluetooth
    }

    /** One row per device: icon, name, pill, % (mockup "Bluetooth Device Battery Bar"). */
    fun renderDeviceRows(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val phone = status(context)
        val others = BluetoothBatteryRepository.connectedDevices(context).orEmpty()
        val rows = buildList {
            add(Triple(Icons.Filled.Smartphone, "Phone", phone.level as Int?))
            others.take(if (k.isLarge) 4 else 2).forEach { add(Triple(deviceIcon(it.name), it.name, it.level)) }
        }
        k.title("Devices")
        val top = k.contentTop + 2f * k.u
        val rowH = (k.bottom - top) / maxOf(rows.size, if (k.isLarge) 4 else 3)
        rows.forEachIndexed { i, (icon, name, level) ->
            val y = top + rowH * i + rowH / 2f
            val iconSize = minOf(rowH * 0.62f, 12f * k.u)
            k.icon(icon, k.left + iconSize / 2f, y, iconSize, k.ink)
            val nameX = k.left + iconSize + 3f * k.u
            val nameW = k.contentW * 0.32f
            k.text(name, nameX, y + 3f * k.u, 9.5f, k.ink, Weight.MEDIUM, maxWidth = nameW)
            val barL = nameX + nameW + 2f * k.u
            val barR = k.right - 13f * k.u
            val bh = minOf(rowH * 0.3f, 4f * k.u)
            k.pill(barL, y - bh / 2f, barR, y + bh / 2f, (level ?: 0) / 100f, if ((level ?: 100) <= 15) k.negative else k.ink)
            k.text(level?.let { "$it%" } ?: "--", k.right, y + 3f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.RIGHT, 12f * k.u)
        }
    }

    /** "🌡 35°C · Normal" (mockup temperature tile). */
    fun renderTemp(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val b = status(context)
        val temp = b.temperatureC.roundToInt()
        val state = when {
            temp >= 45 -> "Hot"
            temp >= 40 -> "Warm"
            temp <= 0 -> "Cold"
            else -> "Normal"
        }
        val color = if (temp >= 40) k.negative else k.ink
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.62f else 24f * k.u
        k.icon(Icons.Filled.Thermostat, k.left + iconSize * 0.4f, if (k.isWide) k.cy else k.top + iconSize / 2f, iconSize, color)
        val tx = if (k.isWide) k.left + iconSize * 0.8f + 5f * k.u else k.left
        val w = k.right - tx
        k.text("$temp°C", tx, if (k.isWide) k.cy + 0f * k.u else k.cy + 14f * k.u, if (k.isWide) 32f else 26f, k.ink, Weight.SEMIBOLD, maxWidth = w)
        if (spec.options.boolean("show_status", true)) {
            k.text("Battery · $state", tx, if (k.isWide) k.cy + 20f * k.u else k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = w)
        }
    }

    /** Battery health + cycles on a soft card. */
    fun renderHealth(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val b = status(context)
        k.title("Battery Health")
        val health = if (b.health == "UNKNOWN") "Unknown" else StyleData.titleCase(b.health)
        val good = b.health == "GOOD"
        val iconSize = if (k.isWide) (k.bottom - k.contentTop) * 0.7f else 18f * k.u
        k.icon(Icons.Filled.BatteryFull, k.right - iconSize / 2f, k.cy + 4f * k.u, iconSize, if (good) k.positive else k.sub)
        val w = k.contentW - iconSize - 4f * k.u
        k.text(health, k.left, k.cy + 8f * k.u, if (k.isWide) 26f else 20f, k.ink, Weight.SEMIBOLD, maxWidth = w)
        val detail = if (b.cycleCount >= 0) "${b.cycleCount} cycles" else if (good) "Optimal" else "Check battery"
        if (spec.options.boolean("show_state", true)) k.text(detail, k.left, k.bottom - 1f * k.u, 10f, k.sub, Weight.MEDIUM, maxWidth = w)
    }
}
