package com.example.themes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import com.example.core.bitmap.SizeBucket
import com.example.data.battery.BatteryHelper
import com.example.data.media.MediaRepository
import com.example.widgets.base.AnimatedOverlays
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetStateManager

/**
 * Looping animations for theme widgets, played by the launcher in the widget's flipbook, so they
 * use no background work and only run while the widget is on screen.
 *
 * Widgets with something going on pulse in the accent colour (music playing, focus running,
 * battery charging). The rest get a soft light sweep across the clay.
 */
object ThemeAnimations {

    private val ids = ThemeWidgets.ALL.map { it.id }.toSet()

    /** Main-bitmap budget, leaving room for the frames in the same update. */
    fun budget(widgetId: String): Long? = if (widgetId in ids) 650_000L else null

    fun frames(context: Context, widgetId: String, spec: WidgetSpec): AnimatedOverlays.Animation? {
        if (widgetId !in ids || !ThemeRepository.animationsEnabled(context)) return null
        val pack = ThemeRepository.forConfig(context, spec.configJson)
        val w = (spec.widthPx / 2).coerceAtLeast(1)
        val h = (spec.heightPx / 2).coerceAtLeast(1)
        val centre = pulseCentre(context, widgetId, pack, spec, w.toFloat(), h.toFloat())
        return if (centre != null) {
            AnimatedOverlays.Animation(pulse(w, h, centre), pack.accent)
        } else {
            AnimatedOverlays.Animation(sweep(context, widgetId, spec, w, h), Color.WHITE)
        }
    }

    /** Where to pulse (x, y, radius) in half-size frame pixels, or null when nothing is active. */
    private fun pulseCentre(context: Context, widgetId: String, pack: ThemePack, spec: WidgetSpec, w: Float, h: Float): Triple<Float, Float, Float>? =
        when (widgetId) {
            "fun.theme.music" -> if (MediaRepository.nowPlaying(context)?.isPlaying == true) {
                val r = fitted(context, pack, if (spec.sizeBucket == SizeBucket.S) "tile_square" else "tile_wide", w, h)
                Triple(r.centerX(), r.top + r.height() * 0.70f, r.height() * 0.15f)
            } else null
            "prod.focus.timer.theme" -> if (WidgetStateManager.getFocusTimerState(context).isRunning) {
                val r = fitted(context, pack, "tile_pill", w, h)
                Triple(r.left + r.height() * 0.52f, r.centerY(), r.height() * 0.26f)
            } else null
            "battery.theme.level" -> if (BatteryHelper.getBatteryStatus(context).isCharging) {
                val r = fitted(context, pack, "tile_square", w, h)
                Triple(r.centerX(), r.centerY(), r.height() * 0.31f)
            } else null
            else -> null
        }

    private fun fitted(context: Context, pack: ThemePack, name: String, w: Float, h: Float): RectF {
        val image = ThemeRepository.image(context, pack, name) ?: return RectF(0f, 0f, w, h)
        val scale = minOf(w / image.width, h / image.height)
        val iw = image.width * scale
        val ih = image.height * scale
        return RectF((w - iw) / 2, (h - ih) / 2, (w + iw) / 2, (h + ih) / 2)
    }

    /** Two rings spreading out from a button or ring and fading, offset so the loop is seamless. */
    private fun pulse(w: Int, h: Int, centre: Triple<Float, Float, Float>): List<Bitmap> {
        val (cx, cy, radius) = centre
        return List(AnimatedOverlays.FRAMES) { f ->
            Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8).also { frame ->
                val canvas = Canvas(frame)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = radius * 0.09f
                }
                listOf(0f, 0.5f).forEach { offset ->
                    val t = (f.toFloat() / AnimatedOverlays.FRAMES + offset) % 1f
                    paint.alpha = (190 * (1f - t)).toInt()
                    canvas.drawCircle(cx, cy, radius * (1f + 0.42f * t), paint)
                }
            }
        }
    }

    /** A soft diagonal band of light crossing the widget, kept inside its shape, then a short rest. */
    private fun sweep(context: Context, widgetId: String, spec: WidgetSpec, w: Int, h: Int): List<Bitmap> {
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val definition = ThemeWidgets.ALL.first { it.id == widgetId }
        definition.render(context, spec.copy(widthPx = w, heightPx = h, density = spec.density / 2), Canvas(mask))
        val keepShape = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        val sweepFrames = AnimatedOverlays.FRAMES - 2
        return List(AnimatedOverlays.FRAMES) { f ->
            Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8).also { frame ->
                if (f < sweepFrames) {
                    val canvas = Canvas(frame)
                    val band = w * 0.5f
                    val x = -band + (w + 2 * band) * (f + 0.5f) / sweepFrames
                    val light = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            x - band, 0f, x + band, h * 0.6f,
                            intArrayOf(0, Color.argb(64, 255, 255, 255), 0), null, Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), light)
                    canvas.drawBitmap(mask, 0f, 0f, keepShape)
                }
            }
        }
    }
}
