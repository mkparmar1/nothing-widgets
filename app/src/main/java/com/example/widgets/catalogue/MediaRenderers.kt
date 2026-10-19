package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.media.MediaRepository
import com.example.data.media.NowPlaying
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.WidgetSpec
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

/** Now-playing widgets driven by the active media session. */
object MediaRenderers {

    /** Title, artist, play state and progress. Tapping toggles play/pause. */
    fun renderCompact(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val np = nowPlayingOrMessage(context, spec, canvas) ?: return
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header(if (np.isPlaying) "NOW PLAYING" else "PAUSED", if (np.isPlaying) kit.accentColor else kit.greyColor)

        drawPlayPauseIcon(kit, kit.width - kit.pad - 14f * d, kit.pad + 8f * d, 9f * d, np.isPlaying)
        DotMatrixRenderer.drawFittedLabel(canvas, np.title.uppercase(), kit.pad, kit.height * 0.48f, kit.maxW, kit.textPaint(kit.onColor, 14f, 20f))
        DotMatrixRenderer.drawFittedLabel(canvas, np.artist.uppercase(), kit.pad, kit.height * 0.48f + 20f * d, kit.maxW, kit.textPaint(kit.greyColor, 11f, 15f))
        drawProgress(kit, np, kit.pad, kit.width - kit.pad, kit.height - kit.pad - 6f * d)
    }

    /** Album art as a dot-matrix halftone next to the track details. Tapping opens the player. */
    fun renderArtCard(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val np = nowPlayingOrMessage(context, spec, canvas) ?: return
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header(if (np.isPlaying) "NOW PLAYING" else "PAUSED", if (np.isPlaying) kit.accentColor else kit.greyColor)

        val square = kit.height > kit.width * 0.8f
        val artSize = if (square) kit.maxW * 0.62f else kit.height - kit.pad * 2 - 26f * d
        val artLeft = if (square) (kit.width - artSize) / 2f else kit.pad
        val artTop = kit.pad + 24f * d
        drawDotArt(kit, np.art, artLeft, artTop, artSize)

        val textLeft = if (square) kit.pad else artLeft + artSize + 14f * d
        val textW = kit.width - kit.pad - textLeft
        val titleY = if (square) artTop + artSize + 20f * d else artTop + 22f * d
        DotMatrixRenderer.drawFittedLabel(canvas, np.title.uppercase(), textLeft, titleY, textW, kit.textPaint(kit.onColor, 13f, 18f))
        DotMatrixRenderer.drawFittedLabel(canvas, np.artist.uppercase(), textLeft, titleY + 18f * d, textW, kit.textPaint(kit.greyColor, 10.5f, 14f))
        drawProgress(kit, np, textLeft, kit.width - kit.pad, kit.height - kit.pad - 6f * d)
    }

    /** Track line plus previous / play-pause / next buttons (each third of the widget is a tap target). */
    fun renderControls(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val np = nowPlayingOrMessage(context, spec, canvas) ?: return
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("MEDIA CONTROLS")
        DotMatrixRenderer.drawFittedLabel(canvas, "${np.title.uppercase()} • ${np.artist.uppercase()}", kit.pad, kit.pad + 34f * d, kit.maxW, kit.textPaint(kit.onColor, 11f, 15f))

        val cy = kit.height * 0.66f
        val r = minOf(kit.height * 0.16f, kit.width * 0.08f)
        val thirds = listOf(kit.width / 6f, kit.width / 2f, kit.width * 5f / 6f)
        canvas.drawCircle(thirds[1], cy, r * 1.35f, kit.fill(kit.accentColor))
        drawPlayPauseIcon(kit, thirds[1], cy, r * 0.7f, np.isPlaying, Color.WHITE)
        drawSkipIcon(kit, thirds[0], cy, r * 0.7f, forward = false)
        drawSkipIcon(kit, thirds[2], cy, r * 0.7f, forward = true)
    }

    /** Main-bitmap budget for the Soundwave, leaving room for its animation frames in the same update. */
    const val SOUNDWAVE_MAIN_BUDGET = 650_000L
    const val SOUNDWAVE_FRAMES = 6
    private const val WAVE_BARS = 16
    private const val WAVE_DOTS = 7

    /**
     * Soundwave for the track that's playing. Each song gets its own wave shape (seeded from the
     * title). On the home screen the bars are animated by a ViewFlipper overlay (see
     * [soundwaveFrames]); the bitmap then only carries the header and title.
     */
    fun renderSoundwave(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val np = nowPlayingOrMessage(context, spec, canvas) ?: return
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header(if (np.isPlaying) "SOUNDWAVE" else "SOUNDWAVE • PAUSED", if (np.isPlaying) kit.accentColor else kit.greyColor)

        val animated = spec.liveOverlays && np.isPlaying
        if (!animated) {
            val heights = if (np.isPlaying) waveShape(np.title) else List(WAVE_BARS) { 1 }
            drawWave(kit, canvas, heights, accentTips = np.isPlaying)
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "${np.title.uppercase()} • ${np.artist.uppercase()}", kit.pad, kit.height - kit.pad, kit.maxW, kit.textPaint(kit.onColor, 10.5f, 14f))
    }

    /**
     * Animation frames for a playing Soundwave: alpha-only bitmaps at half resolution (tinted by
     * the ImageView), small enough that all frames plus the main bitmap fit in one widget update.
     * Returns null when nothing is playing, so the static wave is shown instead.
     */
    fun soundwaveFrames(context: Context, spec: WidgetSpec): List<Bitmap>? {
        if (!AppPermission.MediaAccess.isGranted(context)) return null
        val np = MediaRepository.nowPlaying(context)?.takeIf { it.isPlaying } ?: return null
        val base = waveShape(np.title)
        val w = (spec.widthPx / 2).coerceAtLeast(1)
        val h = (spec.heightPx / 2).coerceAtLeast(1)
        return List(SOUNDWAVE_FRAMES) { f ->
            val phase = 2 * Math.PI * f / SOUNDWAVE_FRAMES
            // Each bar pulses between ~40% and 100% of its height, out of step with its neighbours.
            val heights = base.mapIndexed { b, top ->
                val pulse = 0.7 + 0.3 * sin(phase + b * 0.9)
                (1 + ((top - 1) * pulse).roundToInt()).coerceIn(1, WAVE_DOTS)
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8).also { bmp ->
                val c = Canvas(bmp)
                c.scale(0.5f, 0.5f)
                drawWave(RenderKit(spec, c), c, heights, accentTips = false, solid = true)
            }
        }
    }

    private fun waveShape(title: String): List<Int> {
        val rnd = java.util.Random(title.hashCode().toLong())
        return List(WAVE_BARS) { 2 + rnd.nextInt(WAVE_DOTS - 1) }
    }

    /** 16 dot columns; [solid] draws only lit dots (for alpha-only animation frames). */
    private fun drawWave(kit: RenderKit, canvas: Canvas, heights: List<Int>, accentTips: Boolean, solid: Boolean = false) {
        val d = kit.d
        val colGap = kit.maxW / (WAVE_BARS - 1)
        val bottom = kit.height - kit.pad - 26f * d
        val rowGap = (bottom - (kit.pad + 26f * d)) / WAVE_DOTS
        val onPaint = kit.fill(if (solid) Color.BLACK else kit.onColor)
        val offPaint = kit.fill(kit.offColor)
        val tipPaint = kit.fill(kit.accentColor)
        heights.forEachIndexed { b, h ->
            for (k in 0 until WAVE_DOTS) {
                val on = k < h
                if (!on && solid) continue
                val paint = when {
                    !on -> offPaint
                    accentTips && k == h - 1 -> tipPaint
                    else -> onPaint
                }
                canvas.drawCircle(kit.pad + b * colGap, bottom - k * rowGap, if (on) 2.8f * d else 2.0f * d, paint)
            }
        }
    }

    private fun nowPlayingOrMessage(context: Context, spec: WidgetSpec, canvas: Canvas): NowPlaying? {
        if (!AppPermission.MediaAccess.isGranted(context)) {
            RenderKit.message(spec, canvas, "NOW PLAYING", "ALLOW NOTIFICATION ACCESS IN THE APP")
            return null
        }
        val np = MediaRepository.nowPlaying(context)
        if (np == null) RenderKit.message(spec, canvas, "NOW PLAYING", "NOTHING PLAYING")
        return np
    }

    private fun drawProgress(kit: RenderKit, np: NowPlaying, left: Float, right: Float, y: Float) {
        val d = kit.d
        val paintTime = kit.textPaint(kit.greyColor, 9f, 12f)
        val elapsed = formatTime(np.positionMs)
        val total = if (np.durationMs > 0) formatTime(np.durationMs) else "--:--"
        val barLeft = left + paintTime.measureText(elapsed) + 8f * d
        val barRight = right - paintTime.measureText(total) - 8f * d
        kit.canvas.drawText(elapsed, left, y + 4f * d, paintTime)
        kit.canvas.drawText(total, right - paintTime.measureText(total), y + 4f * d, paintTime)
        val dots = 20
        DotMatrixRenderer.drawDotRun(
            canvas = kit.canvas,
            current = if (np.durationMs > 0) (np.positionMs * 1000 / np.durationMs).toInt() else 0,
            max = 1000,
            totalDots = dots,
            startX = barLeft,
            startY = y,
            dotRadius = 2.4f * d,
            dotGap = (barRight - barLeft) / (dots - 1),
            onColor = kit.onColor,
            offColor = kit.offColor,
            accentColor = kit.accentColor,
            useAccentOnActive = true
        )
    }

    /** Album art sampled into a dot grid; brighter pixels make bigger dots. Placeholder grid when absent. */
    private fun drawDotArt(kit: RenderKit, art: Bitmap?, left: Float, top: Float, size: Float) {
        val grid = 14
        val step = size / grid
        val sample = art?.let { runCatching { Bitmap.createScaledBitmap(it, grid, grid, true) }.getOrNull() }
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                val lum = sample?.getPixel(x, y)?.let { (0.299 * Color.red(it) + 0.587 * Color.green(it) + 0.114 * Color.blue(it)) / 255.0 }
                val cx = left + step * (x + 0.5f)
                val cy = top + step * (y + 0.5f)
                if (lum == null) {
                    kit.canvas.drawCircle(cx, cy, step * 0.18f, kit.fill(kit.offColor))
                } else {
                    val v = if (kit.spec.isDark) lum else 1 - lum
                    kit.canvas.drawCircle(cx, cy, step * (0.08f + 0.38f * v.toFloat()), kit.fill(if (v > 0.12) kit.onColor else kit.offColor))
                }
            }
        }
    }

    private fun drawPlayPauseIcon(kit: RenderKit, cx: Float, cy: Float, r: Float, playing: Boolean, color: Int = kit.onColor) {
        val paint = kit.fill(color)
        if (playing) {
            kit.canvas.drawRect(RectF(cx - r * 0.7f, cy - r, cx - r * 0.2f, cy + r), paint)
            kit.canvas.drawRect(RectF(cx + r * 0.2f, cy - r, cx + r * 0.7f, cy + r), paint)
        } else {
            kit.canvas.drawPath(Path().apply {
                moveTo(cx - r * 0.6f, cy - r); lineTo(cx + r * 0.9f, cy); lineTo(cx - r * 0.6f, cy + r); close()
            }, paint)
        }
    }

    private fun drawSkipIcon(kit: RenderKit, cx: Float, cy: Float, r: Float, forward: Boolean) {
        val s = if (forward) 1f else -1f
        val paint = kit.fill(kit.onColor)
        kit.canvas.drawPath(Path().apply {
            moveTo(cx - s * r, cy - r); lineTo(cx + s * r * 0.4f, cy); lineTo(cx - s * r, cy + r); close()
        }, paint)
        kit.canvas.drawRect(RectF(minOf(cx + s * r * 0.45f, cx + s * r * 0.8f), cy - r, maxOf(cx + s * r * 0.45f, cx + s * r * 0.8f), cy + r), paint)
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }
}
