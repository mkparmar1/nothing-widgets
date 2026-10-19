package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.coroutines.resume

/**
 * Compass heading from the rotation-vector sensor (accelerometer + magnetometer fused).
 *
 * Widgets redraw about once a minute, so we can't show a live rotating needle the way an in-app
 * compass would. Instead we sample the heading once per refresh (at most ~1.5 s) and store it;
 * [headingDegrees] is the synchronous read the renderer uses.
 */
object CompassRepository {

    private const val PREFS = "dot_widgets_compass"

    fun hasSensor(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }

    /** Last sampled heading in degrees (0 = North, clockwise), or null if we've never sampled one. */
    fun headingDegrees(context: Context): Int? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("heading")) return null
        return p.getInt("heading", 0)
    }

    /** Samples the sensor (at most ~1.5 s) and stores the heading. Call before rendering compass widgets. */
    suspend fun refresh(context: Context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val heading = if (rotation != null) {
            sampleRotationVector(sm, rotation)
        } else {
            sampleAccelMag(sm)
        } ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("heading", heading).apply()
    }

    private suspend fun sampleRotationVector(sm: SensorManager, sensor: Sensor): Int? {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        return withTimeoutOrNull(1_500) {
            suspendCancellableCoroutine<Int> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        if (cont.isActive) cont.resume(toDegrees(orientation[0]))
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
    }

    private suspend fun sampleAccelMag(sm: SensorManager): Int? {
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return null
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        return withTimeoutOrNull(1_500) {
            suspendCancellableCoroutine<Int> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, 3)
                            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        }
                        if (gravity.any { it != 0f } && geomagnetic.any { it != 0f }) {
                            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                sm.unregisterListener(this)
                                SensorManager.getOrientation(rotationMatrix, orientation)
                                if (cont.isActive) cont.resume(toDegrees(orientation[0]))
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
                sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
    }

    /** Radians (North = 0, counter-clockwise positive) to a 0..359 clockwise heading. */
    private fun toDegrees(radians: Float): Int = ((Math.toDegrees(radians.toDouble()) + 360.0) % 360.0).roundToInt() % 360

    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    /** 16-point compass label for a heading, e.g. "NNE". */
    fun cardinal(heading: Int): String {
        val points = arrayOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
        )
        val index = ((heading + 11.25f) / 22.5f).toInt().coerceIn(0, 15)
        return points[index]
    }

    /** 8-point label for the tight space on small widgets. */
    fun cardinal8(heading: Int): String = CARDINALS[((heading + 22.5f) / 45f).toInt().coerceIn(0, 7)]
}
