package com.example.widgets.base

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object WidgetAlarmScheduler {

    const val ACTION_REFRESH_WIDGETS = "com.aistudio.dotwidgets.ACTION_REFRESH_WIDGETS"
    private const val REQUEST_CODE = 9918

    fun scheduleNextMinuteTick(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGETS
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

            // Trigger right at the next minute boundary (:00 seconds)
            val now = System.currentTimeMillis()
            val nextMinute = ((now / 60000) + 1) * 60000

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                } catch (se: SecurityException) {
                    // In case exact alarm permission is restricted on Android 12+
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC, nextMinute, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("WidgetAlarmScheduler", "Failed to schedule next minute tick", e)
        }
    }

    fun cancelMinuteTick(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGETS
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }
}
