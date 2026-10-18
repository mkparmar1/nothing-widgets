package com.example.widgets.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.core.bitmap.BitmapCache
import com.example.data.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        // Always ensure the minute-alarm tick is scheduled
        WidgetAlarmScheduler.scheduleNextMinuteTick(context)

        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_USER_PRESENT,
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED,
                    Intent.ACTION_BATTERY_LOW,
                    Intent.ACTION_BATTERY_OKAY,
                    WidgetAlarmScheduler.ACTION_REFRESH_WIDGETS -> {
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_DICE_ROLL" -> {
                        vibrateClick(context)
                        WidgetStateManager.rollDice(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_SNAKE_STEP" -> {
                        vibrateClick(context)
                        WidgetStateManager.stepSnake(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_STOPWATCH_TOGGLE" -> {
                        vibrateClick(context)
                        WidgetStateManager.toggleStopwatch(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_TIMER_TOGGLE" -> {
                        vibrateClick(context)
                        WidgetStateManager.toggleFocusTimer(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_WATER_ADD" -> {
                        vibrateClick(context)
                        val prefs = PreferencesManager.getInstance(context)
                        val cur = prefs.getWaterCount()
                        prefs.setWaterCount((cur + 1) % 9)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_TOGGLE_CLICK" -> {
                        val toggleName = intent.getStringExtra("toggle_name") ?: "TOGGLE"
                        vibrateClick(context)
                        WidgetStateManager.toggleState(context, toggleName)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun vibrateClick(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (_: Exception) {
        }
    }
}
