package com.example.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
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
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
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
                    temperatureC = tempRaw / 10f
                )
            } else {
                BatteryStatus(level = 82, isCharging = false)
            }
        } catch (_: Exception) {
            BatteryStatus(level = 82, isCharging = false)
        }
    }
}
