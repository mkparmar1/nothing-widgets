package com.example.widgets.base

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.battery.BatteryHelper
import com.example.data.weather.WeatherRepository
import com.example.widgets.catalogue.MediaRenderers
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Flipbook animations for home-screen widgets: a few alpha-only frames the launcher cycles in a
 * ViewFlipper (see widget_canvas_layout). Frames are half resolution and tinted by the ImageView,
 * so they fit in the same update as a slightly smaller main bitmap.
 */
object AnimatedOverlays {

    data class Animation(val frames: List<Bitmap>, val tint: Int)

    const val SOUNDWAVE = "sensor.visualizer.eq"
    const val GLYPH_BATTERY = "sensor.glyph.lights"
    const val WEATHER_NOW = "weather.current.temp"
    const val AMBIENT = "fun.pattern.generator"
    const val FRAMES = 6

    private val ANIMATED = setOf(SOUNDWAVE, GLYPH_BATTERY, WEATHER_NOW, AMBIENT)

    /** Main-bitmap budget, leaving room for animation frames in the same update. */
    fun mainBudget(widgetId: String): Long = if (widgetId in ANIMATED) 650_000L else 1_000_000L

    /** Frames for the widget right now, or null when it shouldn't animate (paused music, not charging, no rain). */
    fun frames(context: Context, widgetId: String, spec: WidgetSpec): Animation? {
        val onColor = if (spec.isDark) Color.WHITE else 0xFF0A0A0A.toInt()
        return when (widgetId) {
            SOUNDWAVE -> MediaRenderers.soundwaveFrames(context, spec)?.let { Animation(it, onColor) }
            GLYPH_BATTERY -> if (BatteryHelper.getBatteryStatus(context).isCharging) Animation(pulseFrames(spec), spec.accentColor) else null
            WEATHER_NOW -> {
                val code = WeatherRepository.getWeatherInstant(context).takeIf { !it.isStale }?.weatherCode
                if (code != null && code in RAIN_CODES) Animation(rainFrames(spec), onColor) else null
            }
            AMBIENT -> Animation(ambientFrames(spec), onColor)
            else -> null
        }
    }

    private val RAIN_CODES = setOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)

    private fun frame(spec: WidgetSpec, draw: (Canvas, Float, Float) -> Unit): Bitmap {
        val w = (spec.widthPx / 2).coerceAtLeast(1)
        val h = (spec.heightPx / 2).coerceAtLeast(1)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8).also { draw(Canvas(it), w.toFloat(), h.toFloat()) }
    }

    /** Charging: a soft ring expanding out from the glyph arc and fading. */
    private fun pulseFrames(spec: WidgetSpec): List<Bitmap> = List(FRAMES) { f ->
        frame(spec) { c, w, h ->
            val t = f / FRAMES.toFloat()
            val radius = minOf(w, h) * 0.32f * (1f + 0.22f * t)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = minOf(w, h) * 0.035f
                alpha = (200 * (1f - t)).toInt()
            }
            // Matches the arc centre used by DeviceInfoRenderers.renderGlyphBattery (56% down).
            c.drawCircle(w / 2f, h * 0.56f, radius, p)
        }
    }

    /** Rain: short dot streaks falling diagonally, one step per frame. */
    private fun rainFrames(spec: WidgetSpec): List<Bitmap> = List(FRAMES) { f ->
        frame(spec) { c, w, h ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 120 }
            val rnd = java.util.Random(42)
            val step = h / FRAMES
            repeat(26) {
                val x = rnd.nextFloat() * w
                val y0 = rnd.nextFloat() * h
                val y = (y0 + f * step) % h
                val r = 1.2f + rnd.nextFloat() * 1.2f
                c.drawCircle(x - (y - y0) * 0.15f, y, r, p)
                c.drawCircle(x - (y - y0) * 0.15f + r, y - r * 3, r * 0.7f, p)
            }
        }
    }

    /** Ambient: the dot ripple from renderAmbientPattern, with the phase advancing each frame. */
    private fun ambientFrames(spec: WidgetSpec): List<Bitmap> = List(FRAMES) { f ->
        frame(spec) { c, w, h ->
            val pad = w * 0.08f
            val cols = 18
            val gap = (w - 2 * pad) / (cols - 1)
            val top = pad + 13f * spec.density
            val rows = ((h - top - pad) / gap).toInt() + 1
            val phase = 2 * PI * f / FRAMES
            val cx = w * 0.5f
            val cy = top + (rows - 1) * gap * 0.5f
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            for (r in 0 until rows) {
                for (col in 0 until cols) {
                    val x = pad + col * gap
                    val y = top + r * gap
                    val dist = hypot((x - cx).toDouble(), (y - cy).toDouble()) / gap
                    val wave = (sin(dist * 0.9 - phase) + 1) / 2
                    p.alpha = (60 + 195 * wave).toInt()
                    c.drawCircle(x, y, gap * (0.10f + 0.28f * wave.toFloat()), p)
                }
            }
        }
    }
}
