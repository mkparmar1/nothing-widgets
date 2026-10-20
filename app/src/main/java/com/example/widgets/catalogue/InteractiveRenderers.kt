package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.lists.TapStore
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetStateManager
import java.util.Locale

/**
 * Multi-button widgets. Each one draws its buttons in the same grid that
 * WidgetTapTargets.regions() lays out as tap targets, so taps land on what's drawn.
 */
object InteractiveRenderers {

    val TIMER_PRESETS = listOf(1, 5, 10, 25)
    val VOLUME_STEPS = listOf(0, 33, 66, 100)

    /** − | count | + : the left half subtracts, the right half adds. */
    fun renderTally(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("TALLY")
        val count = TapStore.tally(context)
        kit.dotTextCentered(count.toString(), kit.height * 0.52f, kit.height * 0.36f, 10f * d, kit.maxW * 0.5f)
        val paint = kit.textPaint(kit.accentColor, 22f, 40f)
        canvas.drawText("−", kit.pad + 6f * d, kit.height * 0.52f + paint.textSize / 3f, paint)
        canvas.drawText("+", kit.width - kit.pad - paint.measureText("+") - 6f * d, kit.height * 0.52f + paint.textSize / 3f, paint)
        kit.labelCentered("TAP − OR +", kit.height - kit.pad * 0.8f, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    /** Five mood buttons across the bottom; last 7 days as dots above. */
    fun renderMood(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val week = TapStore.lastWeekMoods(context)
        val today = week.last()
        kit.header(if (today == null) "HOW ARE YOU TODAY?" else "MOOD • ${MOOD_WORDS[today - 1]}")

        // History: 7 columns, height = mood
        val histTop = kit.pad + 24f * d
        val histBottom = kit.height * 0.52f
        val colGap = kit.maxW / 6f
        val rowGap = (histBottom - histTop) / 5f
        week.forEachIndexed { i, mood ->
            for (k in 0 until 5) {
                val on = mood != null && k < mood
                val color = if (!on) kit.offColor else if (i == week.lastIndex) kit.accentColor else kit.onColor
                canvas.drawCircle(kit.pad + i * colGap, histBottom - k * rowGap, if (on) 3f * d else 2f * d, kit.fill(color))
            }
        }

        // Buttons: 5 cells (matches the 1×5 tap grid)
        val cellW = kit.width / 5f
        val cy = kit.height * 0.78f
        for (m in 1..5) {
            val cx = cellW * (m - 0.5f)
            val r = minOf(cellW * 0.28f, kit.height * 0.12f)
            val selected = m == today
            canvas.drawCircle(cx, cy, r, kit.fill(if (selected) kit.accentColor else kit.offColor))
            // m dots inside = mood level
            for (k in 0 until m) {
                val a = Math.toRadians(-90.0 + k * 360.0 / m)
                canvas.drawCircle(cx + (r * 0.5f * kotlin.math.cos(a)).toFloat(), cy + (r * 0.5f * kotlin.math.sin(a)).toFloat(), r * 0.14f, kit.fill(if (selected) 0xFFFFFFFF.toInt() else kit.onColor))
            }
        }
    }

    private val MOOD_WORDS = listOf("ROUGH", "LOW", "OKAY", "GOOD", "GREAT")

    /** Four preset buttons; shows the countdown (live while running). */
    fun renderTimerPresets(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val state = WidgetStateManager.getCountdownState(context)
        kit.header(if (state.isRunning) "TIMER RUNNING" else "QUICK TIMER", if (state.isRunning) kit.accentColor else kit.greyColor)
        if (!state.isRunning || !spec.liveOverlays) {
            val t = String.format(Locale.US, "%02d:%02d", state.remainingSeconds / 60, state.remainingSeconds % 60)
            kit.dotTextCentered(t, kit.height * 0.40f, kit.height * 0.26f, 8f * d, kit.maxW * 0.7f, RenderKit.colonIndices(t))
        }
        drawButtonRow(kit, TIMER_PRESETS.map { "${it}M" }, selected = null)
    }

    /** Media volume set by tapping one of four steps. */
    fun renderVolumeSteps(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val max = (am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1).coerceAtLeast(1)
        val pct = (am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) * 100 / max
        kit.header("MEDIA VOLUME • $pct%")
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = pct, max = 100, totalDots = 20,
            startX = kit.pad, startY = kit.height * 0.40f, dotRadius = 3f * kit.d, dotGap = kit.maxW / 19f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor, useAccentOnActive = true
        )
        val nearest = VOLUME_STEPS.minByOrNull { kotlin.math.abs(it - pct) }
        drawButtonRow(kit, VOLUME_STEPS.map { if (it == 0) "MUTE" else "$it%" }, selected = VOLUME_STEPS.indexOf(nearest))
    }

    /** Equal-width pill buttons across the lower half (the tap grid is 1 row × labels.size). */
    private fun drawButtonRow(kit: RenderKit, labels: List<String>, selected: Int?) {
        val d = kit.d
        val cellW = kit.width / labels.size
        val top = kit.height * 0.62f
        val bottom = kit.height - kit.pad * 0.8f
        labels.forEachIndexed { i, label ->
            val rect = RectF(i * cellW + 5f * d, top, (i + 1) * cellW - 5f * d, bottom)
            val isSel = i == selected
            kit.canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, kit.fill(if (isSel) kit.accentColor else kit.offColor))
            kit.canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = if (isSel) kit.accentColor else kit.greyColor
            })
            val p = kit.textPaint(if (isSel) 0xFFFFFFFF.toInt() else kit.onColor, 11f, 16f)
            kit.canvas.drawText(label, rect.centerX() - p.measureText(label) / 2, rect.centerY() + p.textSize / 3, p)
        }
    }
}
