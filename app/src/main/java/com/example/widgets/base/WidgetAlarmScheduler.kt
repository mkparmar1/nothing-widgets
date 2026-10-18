package com.example.widgets.base

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object WidgetAlarmScheduler {

    const val ACTION_REFRESH_WIDGETS = "com.aistudio.dotwidgets.ACTION_REFRESH_WIDGETS"
    private const val REQUEST_CODE = 9918

    /** True when at least one Dot widget is on the home screen. */
    fun hasPlacedWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return listOf(DotWidgetProviderS::class.java, DotWidgetProviderM::class.java, DotWidgetProviderL::class.java)
            .any { cls ->
                try {
                    manager.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty()
                } catch (_: Exception) {
                    false
                }
            }
    }

    /**
     * Android 12+ requires the user to allow "Alarms & reminders" for exact alarms, and on
     * Android 14+ it is off by default. Without it, minute ticks are batched and clocks lag.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /** Opens the system screen where the user can allow exact alarms for this app. */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    }

    fun scheduleNextMinuteTick(context: Context) {
        // Don't keep waking up every minute once the user has removed all widgets.
        if (!hasPlacedWidgets(context)) {
            cancelMinuteTick(context)
            return
        }
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = tickPendingIntent(context)

            // Trigger right at the next minute boundary (:00 seconds)
            val now = System.currentTimeMillis()
            val nextMinute = ((now / 60000) + 1) * 60000

            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("WidgetAlarmScheduler", "Failed to schedule next minute tick", e)
        }
    }

    fun cancelMinuteTick(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            alarmManager.cancel(tickPendingIntent(context))
        } catch (_: Exception) {
        }
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = ACTION_REFRESH_WIDGETS
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
