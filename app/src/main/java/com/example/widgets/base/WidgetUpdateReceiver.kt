package com.example.widgets.base

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.core.bitmap.BitmapCache
import com.example.data.media.MediaRepository
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
                    // Only broadcasts still delivered to manifest receivers on Android 8+.
                    // Battery/charging changes are picked up by the minute tick.
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
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

                    WidgetTapTargets.ACTION_STOPWATCH_RESET -> {
                        vibrateClick(context)
                        WidgetStateManager.resetStopwatch(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    WidgetAlarmScheduler.ACTION_TIMER_DONE -> {
                        // Timer finished: buzz three times; drawing resets it to its full length.
                        vibratePattern(context, longArrayOf(0, 300, 150, 300, 150, 500))
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    WidgetTapTargets.ACTION_MEDIA_COMMAND -> {
                        vibrateClick(context)
                        intent.getStringExtra(WidgetTapTargets.EXTRA_MEDIA_COMMAND)
                            ?.let { runCatching { MediaRepository.Command.valueOf(it) }.getOrNull() }
                            ?.let { MediaRepository.send(context, it) }
                        // The media session callback redraws the widgets once the player reacts.
                    }

                    "com.aistudio.dotwidgets.ACTION_COUNTDOWN_TOGGLE" -> {
                        vibrateClick(context)
                        WidgetStateManager.toggleCountdown(context)
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }

                    "com.aistudio.dotwidgets.ACTION_HABIT_TOGGLE" -> {
                        vibrateClick(context)
                        PreferencesManager.getInstance(context).toggleHabitToday()
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
                        when (toggleName) {
                            "TORCH" -> DeviceToggles.toggleTorch(context)
                            "DND" -> DeviceToggles.toggleDnd(context)
                            else -> WidgetStateManager.toggleState(context, toggleName)
                        }
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
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
