package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.render.DotMatrixRenderer
import com.example.data.battery.BatteryHelper
import com.example.widgets.base.WidgetSpec

object BatteryRenderers {

    fun renderBatteryRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val status = BatteryHelper.getBatteryStatus(context)
        val level = status.level
        val isCharging = status.isCharging

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()

        // Advanced options: dot count and a "low battery" colour rule.
        val totalDots = spec.options.int("ring_dots", 24).coerceIn(12, 36)
        val warnEnabled = spec.options.boolean("warn_low", true)
        val accentColor = com.example.core.render.ColorRule.paint(
            value = level,
            goal = 100,
            warnRatio = 0.2f,
            accentColor = spec.accentColor,
            enabled = warnEnabled && !isCharging
        )

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val cx = spec.widthPx / 2f
        val cy = spec.heightPx / 2f + (if (spec.showLabel) 6f * spec.density else 0f)
        val maxD = minOf(spec.widthPx - (2 * pad), spec.heightPx - (2 * pad) - (12f * spec.density))
        val radius = (maxD / 2f) * 0.82f

        val filledDots = ((level / 100f) * totalDots).toInt()

        // Circular ring of dots
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            totalDots = totalDots,
            filledDots = filledDots,
            dotRadius = radius * 0.085f,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Center percentage text auto-fitted inside the inner circle
        val pctStr = "$level%"
        val maxTextW = radius * 1.35f
        val maxTextH = radius * 0.85f
        val dotGap = DotMatrixRenderer.fitDotGap(pctStr.length, maxTextW, maxTextH, 6.5f * spec.density)
        val textW = DotMatrixRenderer.measureTextWidth(pctStr.length, dotGap)
        val textH = 7 * dotGap

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = pctStr,
            startX = cx - (textW / 2f),
            startY = cy - (textH / 2f),
            dotRadius = dotGap * 0.38f,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = if (isCharging) setOf(pctStr.length - 1) else emptySet(),
            accentColor = accentColor
        )

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            val label = if (isCharging) "CHARGING AC" else "BATTERY"
            DotMatrixRenderer.drawFittedLabel(canvas, label, pad, pad + 12f * spec.density, maxW * 0.65f, paintLabel)
        }
    }

    fun renderSegmentedBar(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val status = BatteryHelper.getBatteryStatus(context)
        val level = status.level
        val isCharging = status.isCharging

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        // Percentage text in dot matrix on the left
        val pctStr = "$level%"
        val maxPctW = maxW * 0.45f
        val maxPctH = spec.heightPx * 0.40f
        val dotGap = DotMatrixRenderer.fitDotGap(pctStr.length, maxPctW, maxPctH, 10.0f * spec.density)
        val dotRadius = dotGap * 0.38f
        val textW = DotMatrixRenderer.measureTextWidth(pctStr.length, dotGap)

        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = pctStr,
            startX = startX,
            startY = startY,
            dotRadius = dotRadius,
            dotGap = dotGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = if (isCharging) setOf(pctStr.length - 1) else emptySet(),
            accentColor = accentColor
        )

        // Status description on right side without colliding
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCharging) accentColor else 0xFF8A8A8A.toInt()
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val statusText = if (isCharging) "CHARGING AC" else "${status.temperatureC}°C OPTIMAL"
        val statusStartX = (startX + textW + (16f * spec.density)).coerceAtLeast(spec.widthPx * 0.52f)
        val statusMaxW = spec.widthPx - pad - statusStartX
        DotMatrixRenderer.drawFittedLabel(canvas, statusText, statusStartX, startY + (3.5f * dotGap), statusMaxW, paintSub)

        // Segmented bar across the bottom (advanced option: segment count)
        val totalDots = spec.options.int("segments", 20).coerceIn(10, 30)
        val segGap = maxW / (totalDots - 1)
        val segY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = level,
            max = 100,
            totalDots = totalDots,
            startX = pad,
            startY = segY,
            dotRadius = 2.8f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )

        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            DotMatrixRenderer.drawFittedLabel(canvas, "BATTERY METER", pad, pad + 12f * spec.density, maxW * 0.65f, paintLabel)
        }
    }

}
