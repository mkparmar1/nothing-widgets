package com.example.ui.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.PreferencesManager
import com.example.data.sensor.CompassRepository
import com.example.widgets.base.WidgetRenderEngine
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Live compass opened from the compass widget. Android delivers no compass events to background apps,
 * so this is the only place the needle can move in real time; on leaving, the last heading is saved
 * and the placed widget redrawn.
 */
class CompassActivity : ComponentActivity(), SensorEventListener {

    private var heading by mutableStateOf<Float?>(null)
    private val matrix = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    private val sensors by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val rotation: Sensor? by lazy { sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val accent = PreferencesManager.getInstance(this).getSavedAccent().color
        setContent {
            val h = heading
            Column(
                Modifier.fillMaxSize().background(Color.Black).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val r = size.minDimension / 2f
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = r * 0.16f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    // The dial turns so its N always points at magnetic north.
                    rotate(-(h ?: 0f)) {
                        for (i in 0 until 72) rotate(i * 5f) {
                            val major = i % 18 == 0
                            drawLine(
                                if (major) Color.White else Color.Gray,
                                Offset(center.x, center.y - r * 0.96f),
                                Offset(center.x, center.y - r * if (major) 0.84f else 0.9f),
                                strokeWidth = r * 0.014f
                            )
                        }
                        listOf("N", "E", "S", "W").forEachIndexed { i, letter ->
                            rotate(i * 90f) {
                                paint.color = if (i == 0) accent.toArgb() else android.graphics.Color.WHITE
                                drawContext.canvas.nativeCanvas.drawText(letter, center.x, center.y - r * 0.64f, paint)
                            }
                        }
                    }
                    val pointer = Path().apply {
                        moveTo(center.x, center.y - r * 0.5f)
                        lineTo(center.x - r * 0.09f, center.y)
                        lineTo(center.x + r * 0.09f, center.y)
                        close()
                    }
                    drawPath(pointer, accent)
                    drawCircle(Color.White, r * 0.05f, center)
                }
                Spacer(Modifier.height(32.dp))
                val degrees = h?.roundToInt()?.rem(360)
                Text(degrees?.let { "$it° ${CompassRepository.cardinal(it)}" } ?: "--°", color = Color.White, fontSize = 44.sp)
                Text(
                    when {
                        rotation == null -> "This phone has no compass sensor"
                        h == null -> "Reading the sensor…"
                        else -> "Hold the phone flat, away from metal"
                    },
                    color = Color.Gray, fontSize = 14.sp
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotation?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        sensors.unregisterListener(this)
        heading?.let { CompassRepository.save(this, it.roundToInt()) }
        val app = applicationContext
        CoroutineScope(Dispatchers.Default).launch { WidgetRenderEngine.updateAllPlacedWidgets(app) }
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        @Suppress("DEPRECATION")
        val screenRotation = windowManager.defaultDisplay.rotation
        val (ax, ay) = when (screenRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, ax, ay, remapped)
        SensorManager.getOrientation(remapped, orientation)
        val now = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
        // Light smoothing along the shortest way round, so the dial doesn't jitter or spin at north.
        val old = heading
        heading = if (old == null) now else {
            val delta = ((now - old + 540f) % 360f) - 180f
            (old + delta * 0.25f + 360f) % 360f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
