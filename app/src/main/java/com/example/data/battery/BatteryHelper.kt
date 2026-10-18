package com.example.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val isPlugged: Boolean = false,
    val temperatureC: Float = 28f
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

                BatteryStatus(
                    level = pct.coerceIn(0, 100),
                    isCharging = isCharging,
                    isPlugged = isPlugged,
                    temperatureC = tempRaw / 10f
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
