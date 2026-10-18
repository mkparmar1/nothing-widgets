package com.example.data.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Today's step count from the hardware step counter. The sensor reports steps since boot, so we
 * keep a per-day baseline (and carry over across reboots) in preferences.
 */
object StepRepository {

    private const val PREFS = "dot_widgets_steps"

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun hasSensor(context: Context) =
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    /** Last computed steps for today, or null if we can't count steps (no permission or sensor). */
    fun todaySteps(context: Context): Int? {
        if (!hasPermission(context) || !hasSensor(context)) return null
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getString("day", null) != today()) return 0
        return p.getInt("today", 0)
    }

    /** Reads the sensor (at most ~2 s) and updates today's count. Call before rendering step widgets. */
    suspend fun refresh(context: Context) {
        if (!hasPermission(context)) return
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val counter = withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine<Long> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        } ?: return
        record(context, counter)
    }

    @Synchronized
    private fun record(context: Context, counter: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = today()
        val last = p.getLong("last", -1)
        var base = p.getLong("base", counter)

        if (p.getString("day", null) != today) {
            // New day: steps since the last reading belong to today only if they happened after
            // midnight, which we can't know; starting from the last reading is the closest guess.
            base = if (last in 0..counter) last else counter
        } else if (last >= 0 && counter < last) {
            // Phone rebooted, counter restarted from zero: keep today's steps so far.
            base -= last
        }

        p.edit()
            .putString("day", today)
            .putLong("base", base)
            .putLong("last", counter)
            .putInt("today", (counter - base).coerceAtLeast(0).toInt())
            .apply()
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
