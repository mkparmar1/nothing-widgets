package com.example.widgets.base

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.core.bitmap.BitmapCache
import com.example.data.lists.ListStore
import com.example.data.lists.TapStore
import com.example.data.media.MediaRepository
import com.example.widgets.collection.CollectionViews
import com.example.widgets.collection.CollectionWidgetService
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.collection.RowAction
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
                    // After boot, an app update or a clock change the launcher may need every
                    // widget again, so send all of them.
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
                    }

                    // Minute tick: only widgets whose picture changed are sent.
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

                    CollectionViews.ACTION_LIST_ITEM -> {
                        val item = intent.getStringExtra(CollectionWidgetService.EXTRA_ITEM_ID)
                        val rowAction = intent.getStringExtra(CollectionWidgetService.EXTRA_ROW_ACTION)
                            ?.let { runCatching { RowAction.valueOf(it) }.getOrNull() }
                        if (item != null && rowAction != null) {
                            vibrateClick(context)
                            val changedType = when (rowAction) {
                                RowAction.TOGGLE_TODO -> {
                                    val prefs = PreferencesManager.getInstance(context)
                                    val todos = prefs.getTodos().toMutableList()
                                    item.toIntOrNull()?.takeIf { it in todos.indices }?.let { todos[it] = todos[it].copy(done = !todos[it].done) }
                                    prefs.setTodos(todos)
                                    CollectionWidgets.TODO
                                }
                                RowAction.TOGGLE_SHOPPING -> { ListStore.toggleShopping(context, item); CollectionWidgets.SHOPPING }
                                RowAction.TOGGLE_HABIT -> { ListStore.toggleHabitToday(context, item); CollectionWidgets.HABITS }
                                else -> null
                            }
                            if (changedType != null) {
                                // Header counts live in the bitmap; rows come from the service.
                                BitmapCache.clear()
                                WidgetRenderEngine.updateAllPlacedWidgets(context)
                                CollectionViews.notifyChanged(context, CollectionViews.placedTypes(context), setOf(changedType))
                            }
                        }
                    }

                    WidgetTapTargets.ACTION_TALLY, WidgetTapTargets.ACTION_MOOD,
                    WidgetTapTargets.ACTION_TIMER_PRESET, WidgetTapTargets.ACTION_VOLUME_STEP -> {
                        vibrateClick(context)
                        val value = intent.getIntExtra(WidgetTapTargets.EXTRA_VALUE, 0)
                        when (action) {
                            WidgetTapTargets.ACTION_TALLY -> TapStore.addTally(context, value)
                            WidgetTapTargets.ACTION_MOOD -> TapStore.setMoodToday(context, value)
                            WidgetTapTargets.ACTION_TIMER_PRESET -> WidgetStateManager.restartCountdown(context, value)
                            WidgetTapTargets.ACTION_VOLUME_STEP -> {
                                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                am?.let { it.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) * value / 100, 0) }
                            }
                        }
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
