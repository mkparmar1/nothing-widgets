package com.example.widgets.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.core.bitmap.BitmapCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "com.aistudio.dotwidgets.ACTION_REFRESH_WIDGETS" -> {
                BitmapCache.clear()
                scope.launch {
                    WidgetRenderEngine.updateAllPlacedWidgets(context)
                }
            }

            "com.aistudio.dotwidgets.ACTION_TOGGLE_CLICK" -> {
                val toggleName = intent.getStringExtra("toggle_name") ?: "TOGGLE"
                vibrateClick(context)
                BitmapCache.clear()
                scope.launch {
                    WidgetRenderEngine.updateAllPlacedWidgets(context)
                }
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
