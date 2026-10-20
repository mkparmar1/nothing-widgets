package com.example.data.system

import android.app.AlarmManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.example.data.battery.BatteryHelper
import com.example.ui.permissions.AppPermission
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Device readings for the Phase 11 widgets. None need a new permission; data use and top apps use Usage access. */
object DeviceStatsRepository {

    private const val PREFS = "dot_widgets_device_stats"

    // ---------- Next alarm ----------

    /** Trigger time of the next alarm set in any clock app, or null. */
    fun nextAlarmMs(context: Context): Long? =
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock?.triggerTime

    // ---------- Thermal / uptime ----------

    /** 0 = none … 6 = shutdown (PowerManager.THERMAL_STATUS_*), or null before Android 10. */
    fun thermalStatus(context: Context): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus else null

    fun uptimeMs(): Long = SystemClock.elapsedRealtime()

    // ---------- Network speed (average since the previous refresh) ----------

    data class Speed(val downBytesPerSec: Long, val upBytesPerSec: Long)

    /** Samples total traffic counters; call once per refresh. */
    fun sampleNetwork(context: Context) {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx < 0 || tx < 0) return
        val p = prefs(context)
        val now = SystemClock.elapsedRealtime()
        val lastT = p.getLong("net_t", 0L)
        val dt = (now - lastT) / 1000.0
        val editor = p.edit().putLong("net_t", now).putLong("net_rx", rx).putLong("net_tx", tx)
        if (lastT in 1 until now && dt >= 5) {
            val down = ((rx - p.getLong("net_rx", rx)) / dt).toLong().coerceAtLeast(0)
            val up = ((tx - p.getLong("net_tx", tx)) / dt).toLong().coerceAtLeast(0)
            editor.putLong("net_down", down).putLong("net_up", up)
        }
        editor.apply()
    }

    fun speed(context: Context): Speed? {
        val p = prefs(context)
        if (!p.contains("net_down")) return null
        return Speed(p.getLong("net_down", 0), p.getLong("net_up", 0))
    }

    // ---------- Battery history (one sample per 15 minutes, last 24 h) ----------

    data class BatterySample(val timeMs: Long, val level: Int, val charging: Boolean)

    fun sampleBattery(context: Context) {
        val now = System.currentTimeMillis()
        val history = batteryHistory(context).toMutableList()
        if (history.isNotEmpty() && now - history.last().timeMs < 15 * 60_000L) return
        val status = BatteryHelper.getBatteryStatus(context)
        history += BatterySample(now, status.level, status.isCharging)
        val kept = history.filter { now - it.timeMs <= 24 * 3_600_000L }
        val arr = JSONArray()
        kept.forEach { arr.put(JSONObject().put("t", it.timeMs).put("l", it.level).put("c", it.charging)) }
        prefs(context).edit().putString("battery_history", arr.toString()).apply()
    }

    fun batteryHistory(context: Context): List<BatterySample> = runCatching {
        val arr = JSONArray(prefs(context).getString("battery_history", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it) }.map { BatterySample(it.getLong("t"), it.getInt("l"), it.optBoolean("c")) }
    }.getOrDefault(emptyList())

    // ---------- Mobile data today (Usage access) ----------

    @Volatile private var mobileBytesToday: Long? = null

    fun mobileDataToday(context: Context): Long? = if (AppPermission.UsageAccess.isGranted(context)) mobileBytesToday else null

    fun refreshMobileData(context: Context) {
        if (!AppPermission.UsageAccess.isGranted(context)) return
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return
        mobileBytesToday = runCatching {
            @Suppress("DEPRECATION")
            val bucket: NetworkStats.Bucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startOfDay(), System.currentTimeMillis())
            bucket.rxBytes + bucket.txBytes
        }.getOrNull()
    }

    // ---------- Top apps today (Usage access) ----------

    data class AppUsage(val packageName: String, val label: String, val foregroundMs: Long)

    @Volatile private var topApps: List<AppUsage>? = null

    fun topAppsToday(context: Context): List<AppUsage>? = if (AppPermission.UsageAccess.isGranted(context)) topApps else null

    fun refreshTopApps(context: Context) {
        if (!AppPermission.UsageAccess.isGranted(context)) return
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val pm = context.packageManager
        topApps = runCatching {
            usm.queryAndAggregateUsageStats(startOfDay(), System.currentTimeMillis()).values
                .filter { it.totalTimeInForeground > 60_000L && it.packageName != context.packageName }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // skip system services
                .sortedByDescending { it.totalTimeInForeground }
                .take(5)
                .map { s ->
                    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(s.packageName, 0)).toString() }.getOrDefault(s.packageName)
                    AppUsage(s.packageName, label, s.totalTimeInForeground)
                }
        }.getOrNull()
    }

    private fun startOfDay() = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
