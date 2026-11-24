package com.example.widgets.styles

import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.os.Environment
import android.os.StatFs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import com.example.data.sensor.CompassRepository
import com.example.data.system.DeviceStatsRepository
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/** Extra designs for system / sensor widgets (Batch 2, items 1–14). */
object SystemStyles {

    private val ramBar = WidgetStyle("soft_bar", "Soft Bar", StyleFamily.SOFT) { c, s, cv -> renderRamBar(c, s, cv, StyleFamily.SOFT) }
    private val ramBold = WidgetStyle("bold_bar", "Bold Bar", StyleFamily.BOLD) { c, s, cv -> renderRamBar(c, s, cv, StyleFamily.BOLD) }
    private val storageBar = WidgetStyle("soft_bar", "Soft Bar", StyleFamily.SOFT) { c, s, cv -> renderStorageBar(c, s, cv, StyleFamily.SOFT) }
    private val storageRing = WidgetStyle("soft_ring", "Soft Ring", StyleFamily.SOFT, render = ::renderStorageRing)

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "sensor.ram.meter" to listOf(ramBar, ramBold, WidgetStyle("ram_rows", "RAM Rows", StyleFamily.SOFT, render = ::renderRamRows)),
        "sensor.ram.spark" to listOf(ramBar, ramBold),
        "sensor.storage.bar" to listOf(storageBar, storageRing),
        "sensor.storage.free" to listOf(storageRing, storageBar),
        "sensor.cpu.load" to listOf(
            WidgetStyle("cpu_bars", "CPU Bars", StyleFamily.SOFT) { c, s, cv -> renderCpu(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_cpu", "Bold CPU", StyleFamily.BOLD) { c, s, cv -> renderCpu(c, s, cv, StyleFamily.BOLD) }
        ),
        "sys.network.speed" to listOf(
            WidgetStyle("speed_card", "Speed Card", StyleFamily.SOFT) { c, s, cv -> renderSpeed(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_speed", "Bold Speed", StyleFamily.BOLD) { c, s, cv -> renderSpeed(c, s, cv, StyleFamily.BOLD) }
        ),
        "sensor.uptime" to listOf(
            WidgetStyle("soft_uptime", "Soft Uptime", StyleFamily.SOFT) { c, s, cv -> renderUptime(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_uptime", "Bold Uptime", StyleFamily.BOLD) { c, s, cv -> renderUptime(c, s, cv, StyleFamily.BOLD) }
        ),
        "sensor.compass.heading" to listOf(
            WidgetStyle("soft_compass", "Soft Compass", StyleFamily.SOFT) { c, s, cv -> renderCompass(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_compass", "Bold Compass", StyleFamily.BOLD) { c, s, cv -> renderCompass(c, s, cv, StyleFamily.BOLD) }
        ),
        "sys.mobile.data" to listOf(
            WidgetStyle("data_cap", "Data Usage", StyleFamily.SOFT, render = ::renderDataUsage)
        ),
        "sys.thermal.uptime" to listOf(
            WidgetStyle("bold_temp", "Bold Temp", StyleFamily.BOLD) { c, s, cv -> renderThermal(c, s, cv, StyleFamily.BOLD) },
            WidgetStyle("soft_temp", "Soft Temp", StyleFamily.SOFT) { c, s, cv -> renderThermal(c, s, cv, StyleFamily.SOFT) }
        )
    )

    // ------------------------------------------------------------- readings

    private data class Mem(val usedGb: Float, val totalGb: Float) {
        val freeGb: Float get() = (totalGb - usedGb).coerceAtLeast(0f)
        val pct: Int get() = if (totalGb > 0) ((usedGb / totalGb) * 100).roundToInt().coerceIn(0, 100) else 0
    }

    /** Null when the system gives no reading; cards then show "--", never invented numbers. */
    private fun ram(context: Context): Mem? = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gb = 1024f * 1024f * 1024f
        Mem((info.totalMem - info.availMem) / gb, info.totalMem / gb).takeIf { it.totalGb > 0f } 
    } catch (_: Exception) {
        null
    }

    private fun storage(): Mem? = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val gb = 1024f * 1024f * 1024f
        val total = stat.blockCountLong * stat.blockSizeLong / gb
        val free = stat.availableBlocksLong * stat.blockSizeLong / gb
        Mem(total - free, total).takeIf { it.totalGb > 0f } 
    } catch (_: Exception) {
        null
    }

    private fun pct(mem: Mem?): String = mem?.let { "${it.pct}%" } ?: "--"

    private fun gb(value: Float): String = if (value >= 100f) "${value.roundToInt()} GB" else String.format(Locale.US, "%.1f GB", value)

    /** Per-core current/max frequency ratios when the kernel exposes them, else null. */
    private fun coreLoads(): List<Float>? = try {
        val cores = Runtime.getRuntime().availableProcessors().coerceIn(1, 12)
        val loads = (0 until cores).map { i ->
            val base = "/sys/devices/system/cpu/cpu$i/cpufreq"
            val cur = File("$base/scaling_cur_freq").readText().trim().toFloat()
            val max = File("$base/cpuinfo_max_freq").readText().trim().toFloat()
            (cur / max).coerceIn(0.05f, 1f)
        }
        loads
    } catch (_: Exception) {
        null
    }

    private fun speedLabel(bytesPerSec: Long): String = when {
        bytesPerSec >= 1_000_000 -> String.format(Locale.US, "%.1f", bytesPerSec * 8 / 1_000_000f)
        else -> String.format(Locale.US, "%.0f", bytesPerSec * 8 / 1_000f)
    }

    private fun speedUnit(bytesPerSec: Long): String = if (bytesPerSec >= 1_000_000) "Mbps" else "kbps"

    // ------------------------------------------------------------- renderers

    /** "RAM 72%" with a pill bar (mockup). */
    fun renderRamBar(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        drawBarCard(k, Icons.Filled.Memory, "RAM", ram(context))
    }

    fun renderStorageBar(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        drawBarCard(k, Icons.Filled.Storage, "Storage", storage())
    }

    private fun drawBarCard(k: StyleKit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, mem: Mem?) {
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.42f else 18f * k.u
        k.icon(icon, k.left + iconSize / 2f, if (k.isWide) k.top + iconSize / 2f + 4f * k.u else k.top + iconSize / 2f, iconSize, k.ink)
        val tx = k.left + iconSize + 5f * k.u
        if (k.isWide) {
            k.text(label, tx, k.top + 12f * k.u, 13f, k.ink, Weight.MEDIUM, maxWidth = k.right - tx - k.cornerReserve)
            k.text(pct(mem), tx, k.top + 34f * k.u, 22f, k.ink, Weight.SEMIBOLD, maxWidth = (k.right - tx) * 0.5f)
            if (mem != null && k.spec.options.boolean("show_detail", true)) k.text("${gb(mem.usedGb)} of ${gb(mem.totalGb)}", k.right, k.top + 34f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.RIGHT, (k.right - tx) * 0.5f)
        } else {
            k.text(pct(mem), k.left, k.cy + 14f * k.u, if (k.isLarge) 52f else 26f, k.ink, Weight.SEMIBOLD)
            k.text(label, tx, k.top + iconSize * 0.7f, 11f, k.ink, Weight.MEDIUM, maxWidth = k.right - tx - k.cornerReserve)
        }
        val used = mem?.pct ?: 0
        k.pill(k.left, k.bottom - 5f * k.u, k.right, k.bottom, used / 100f, if (used >= 90) k.negative else k.ink)
    }

    /** RAM / Used / Total rows (mockup "RAM 72% · 3.20% · 4 GB"). */
    fun renderRamRows(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val mem = ram(context)
        k.title("RAM")
        val rows = listOf("Used" to pct(mem), "In use" to (mem?.let { gb(it.usedGb) } ?: "--"), "Total" to (mem?.let { gb(it.totalGb) } ?: "--"))
        val top = k.contentTop
        val rowH = (k.bottom - top) / rows.size
        rows.forEachIndexed { i, (label, value) ->
            val y = top + rowH * i + rowH * 0.68f
            k.text(label, k.left, y, 10.5f, k.sub, Weight.MEDIUM, maxWidth = k.contentW * 0.5f)
            k.text(value, k.right, y, 11.5f, k.ink, Weight.SEMIBOLD, Align.RIGHT, k.contentW * 0.5f)
            if (i < rows.lastIndex) {
                val ly = top + rowH * (i + 1)
                k.roundRect(k.left, ly - 0.3f * k.u, k.right, ly + 0.3f * k.u, 0.3f * k.u, k.track)
            }
        }
    }

    /** "64% Storage" ring (mockup). */
    fun renderStorageRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val mem = storage()
        val used = mem?.pct ?: 0
        val color = if (used >= 90) k.negative else k.positive
        if (k.isWide) {
            val r = (k.bottom - k.top) * 0.45f
            val x = k.left + r
            k.ring(x, k.cy, r, 4f * k.u, used / 100f, color)
            k.text(pct(mem), x, k.cy + 5f * k.u, 16f, k.ink, Weight.SEMIBOLD, Align.CENTER, r * 1.5f)
            val tx = x + r + 8f * k.u
            k.text("Storage", tx, k.cy - 3f * k.u, 14f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(mem?.let { "${gb(it.freeGb)} free" } ?: "No reading", tx, k.cy + 12f * k.u, 10.5f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
            return
        }
        val r = minOf(k.contentW, k.bottom - k.top) * 0.44f
        k.ring(k.cx, k.cy, r, 4f * k.u, used / 100f, color)
        k.text(pct(mem), k.cx, k.cy + 3f * k.u, r / k.u * 0.42f, k.ink, Weight.SEMIBOLD, Align.CENTER, r * 1.5f)
        k.text("Storage", k.cx, k.cy + r * 0.45f, 9f, k.sub, Weight.MEDIUM, Align.CENTER, r * 1.4f)
    }

    /** Per-core bars (live frequency when available, otherwise the core count). */
    fun renderCpu(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val cores = Runtime.getRuntime().availableProcessors()
        val loads = coreLoads()
        k.title(if (loads != null) "CPU Frequency" else "CPU")
        val values = loads ?: List(cores.coerceIn(1, 12)) { 1f }
        val avg = if (loads != null) "${(loads.average() * 100).roundToInt()}%" else "$cores cores"
        val top = k.contentTop + 2f * k.u
        if (k.isWide) {
            k.text(avg, k.left, k.bottom - 1f * k.u, 22f, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW * 0.34f)
            k.bars(values, k.left + k.contentW * 0.4f, top, k.right, k.bottom, k.ink, k.track, 0.4f)
        } else {
            k.text(avg, k.left, top + 14f * k.u, 16f, k.ink, Weight.SEMIBOLD)
            k.bars(values, k.left, top + 20f * k.u, k.right, k.bottom, k.ink, k.track, 0.4f)
        }
    }

    /** Download / upload speed card (mockup "Network Speed Test"). */
    fun renderSpeed(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val speed = DeviceStatsRepository.speed(context)
        k.title("Network Speed")
        val top = k.contentTop
        val rows = listOf(
            Triple(Icons.Filled.ArrowDownward, speed?.downBytesPerSec, "Down"),
            Triple(Icons.Filled.ArrowUpward, speed?.upBytesPerSec, "Up")
        )
        val colW = k.contentW / 2f
        rows.forEachIndexed { i, (icon, bytes, label) ->
            val x = k.left + colW * i
            val iconSize = 10f * k.u
            k.icon(icon, x + iconSize / 2f, top + 6f * k.u, iconSize, if (i == 0) k.positive else k.sub)
            k.text(label, x + iconSize + 2f * k.u, top + 9f * k.u, 9.5f, k.sub, Weight.MEDIUM, maxWidth = colW - iconSize - 4f * k.u)
            val value = bytes?.let { speedLabel(it) } ?: "--"
            val vw = k.text(value, x, k.bottom - 6f * k.u, if (k.isWide) 26f else 16f, k.ink, Weight.SEMIBOLD, maxWidth = colW * 0.7f)
            k.text(bytes?.let { speedUnit(it) } ?: "", x + vw + 1.5f * k.u, k.bottom - 6f * k.u, 9f, k.sub, Weight.MEDIUM, maxWidth = colW - vw - 3f * k.u)
        }
    }

    /** "00:15:30" style uptime (mockup "System Uptime"). */
    fun renderUptime(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val totalMin = DeviceStatsRepository.uptimeMs() / 60_000L
        val days = totalMin / (60 * 24)
        val main = String.format(Locale.US, "%02d:%02d", (totalMin / 60) % 24, totalMin % 60)
        k.title("System Uptime")
        val iconSize = if (k.isWide) (k.bottom - k.contentTop) * 0.6f else 0f
        if (k.isWide) k.icon(Icons.Filled.Timer, k.left + iconSize / 2f, k.cy + 6f * k.u, iconSize, k.ink)
        val x = if (k.isWide) k.left + iconSize + 6f * k.u else k.left
        k.text(main, x, k.cy + 12f * k.u, if (k.isWide) 30f else 24f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - x)
        k.text(if (days > 0) "$days ${if (days == 1L) "day" else "days"} · hh:mm" else "hours : minutes", x, k.bottom - 1f * k.u, 9f, k.sub, Weight.MEDIUM, maxWidth = k.right - x)
    }

    /** Compass rose with a needle; red north tip on the bold version (mockup compass tiles). */
    fun renderCompass(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        if (!CompassRepository.hasSensor(context)) {
            k.title("Compass")
            k.text("No compass sensor", k.cx, k.cy + 4f * k.u, 10.5f, k.sub, Weight.MEDIUM, Align.CENTER)
            return
        }
        val heading = CompassRepository.headingDegrees(context)
        val r = if (k.isWide) (k.bottom - k.top) * 0.5f else minOf(k.contentW, k.bottom - k.top) * 0.48f
        val x = if (k.isWide) k.left + r else k.cx
        val y = k.cy
        k.ticks(x, y, r, 36, r * 0.06f, r * 0.14f, 9, k.sub, maxOf(1f, r * 0.025f))
        val letter = 7.5f * (r / (40f * k.u)).coerceIn(0.8f, 1.6f)
        listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (l, deg) ->
            val a = Math.toRadians(deg - 90.0)
            val lr = r * 0.66f
            k.text(l, x + lr * Math.cos(a).toFloat(), y + lr * Math.sin(a).toFloat() + letter * k.u * 0.36f, letter, if (l == "N") k.accent else k.ink, Weight.SEMIBOLD, Align.CENTER)
        }
        k.canvas.save()
        k.canvas.rotate((heading ?: 0).toFloat(), x, y)
        k.icon(Icons.Filled.Navigation, x, y, r * 0.62f, if (k.bold) k.accent else k.ink)
        k.canvas.restore()
        if (k.isWide) {
            val tx = x + r + 8f * k.u
            k.text(heading?.let { "$it°" } ?: "--°", tx, k.cy + 4f * k.u, 28f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text(heading?.let { CompassRepository.cardinal(it) } ?: "Calibrating", tx, k.cy + 17f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
        }
    }

    /** Today's mobile data. No bar: the app doesn't know the user's plan, so there is no honest limit. */
    fun renderDataUsage(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val k = StyleKit(context, spec, canvas, StyleFamily.SOFT)
        k.card()
        val bytes = DeviceStatsRepository.mobileDataToday(context)
        k.title("Mobile Data Today")
        val gbUsed = (bytes ?: 0L) / (1024f * 1024f * 1024f)
        val value = when {
            bytes == null -> "--"
            gbUsed >= 1f -> String.format(Locale.US, "%.1f GB", gbUsed)
            else -> "${(bytes / (1024 * 1024)).coerceAtLeast(0)} MB"
        }
        val iconSize = 14f * k.u
        k.icon(Icons.Filled.DataUsage, k.right - iconSize / 2f, k.cy - 2f * k.u, iconSize, k.faint)
        k.text(value, k.left, k.cy + 8f * k.u, if (k.isWide) 26f else 20f, k.ink, Weight.SEMIBOLD, maxWidth = k.contentW - iconSize - 4f * k.u)
        k.text(if (bytes == null) "Allow usage access" else "Today", k.left, k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM)
    }

    /** Thermal status tile ("35°C"-style with the device's thermal state). */
    fun renderThermal(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val status = DeviceStatsRepository.thermalStatus(context)
        val label = when (status) {
            null -> "Unknown"
            0 -> "Cool"
            1 -> "Light"
            2 -> "Moderate"
            3 -> "Severe"
            4 -> "Critical"
            else -> "Shutdown"
        }
        val temp = com.example.data.battery.BatteryHelper.getBatteryStatus(context).temperatureC.roundToInt()
        val hot = (status ?: 0) >= 2
        val iconSize = if (k.isWide) (k.bottom - k.top) * 0.62f else 22f * k.u
        k.icon(Icons.Filled.Thermostat, k.left + iconSize * 0.4f, if (k.isWide) k.cy else k.top + iconSize / 2f, iconSize, if (hot) k.negative else k.ink)
        val tx = if (k.isWide) k.left + iconSize * 0.8f + 5f * k.u else k.left
        k.text("$temp°C", tx, if (k.isWide) k.cy + 0f * k.u else k.cy + 14f * k.u, if (k.isWide) 30f else 24f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
        val upMin = DeviceStatsRepository.uptimeMs() / 60_000L
        k.text("$label · up ${StyleData.duration(upMin.toInt())}", tx, if (k.isWide) k.cy + 20f * k.u else k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
    }
}
