package com.example.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val isPlugged: Boolean = false,
    val temperatureC: Float = 28f,
    /** GOOD, OVERHEAT, DEAD, COLD... */
    val health: String = "UNKNOWN",
    /** AC, USB, WIRELESS, DOCK or NONE */
    val plugType: String = "NONE",
    val voltageMv: Int = 0,
    /** Charge cycles (Android 14+), or -1 when unavailable. */
    val cycleCount: Int = -1,
    /** Estimated ms until full while charging (Android 9+), or -1 when unknown. */
    val chargeTimeRemainingMs: Long = -1
)

object BatteryHelper {

    fun getBatteryStatus(context: Context): BatteryStatus {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                
                val isPlugged = plugged > 0
                val isCharging = isPlugged || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)

                val pct = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100).toInt()
                } else {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 82
                }

                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER VOLTAGE"
                    BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
                    else -> "UNKNOWN"
                }
                val plugType = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                    8 /* BATTERY_PLUGGED_DOCK, API 33 */ -> "DOCK"
                    else -> "NONE"
                }
                val chargeTime = if (isCharging && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    bm?.computeChargeTimeRemaining() ?: -1L
                } else -1L

                BatteryStatus(
                    level = pct.coerceIn(0, 100),
                    isCharging = isCharging,
                    isPlugged = isPlugged,
                    temperatureC = tempRaw / 10f,
                    health = health,
                    plugType = plugType,
                    voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
                    cycleCount = intent.getIntExtra("android.os.extra.CYCLE_COUNT", -1),
                    chargeTimeRemainingMs = chargeTime
                )
            } else {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 82
                val isCharging = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    bm?.isCharging == true
                } else false
                BatteryStatus(level = capacity.coerceIn(0, 100), isCharging = isCharging, isPlugged = isCharging)
            }
        } catch (_: Exception) {
            BatteryStatus(level = 82, isCharging = false, isPlugged = false)
        }
    }
}
