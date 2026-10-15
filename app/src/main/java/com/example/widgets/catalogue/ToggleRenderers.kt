package com.example.widgets.catalogue

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.widgets.base.WidgetSpec

object ToggleRenderers {

    fun renderSingleToggle(spec: WidgetSpec, canvas: Canvas, name: String, isActive: Boolean, iconSymbol: String) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

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
        val cy = spec.heightPx / 2f - 6f * spec.density
        val radius = minOf(spec.widthPx, spec.heightPx) * 0.28f

        // Draw active or inactive outer dot ring
        val ringColor = if (isActive) accentColor else onColor
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            totalDots = 16,
            filledDots = if (isActive) 16 else 4,
            dotRadius = radius * 0.12f,
            onColor = ringColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Center status dot
        val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isActive) accentColor else offColor
        }
        canvas.drawCircle(cx, cy, radius * 0.36f, paintCenter)

        // Toggle label
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isActive) onColor else 0xFF8A8A8A.toInt()
            textSize = 11f * spec.density
            isFakeBoldText = true
        }
        val lw = paintLabel.measureText(name)
        canvas.drawText(name, cx - (lw / 2f), cy + radius + 18f * spec.density, paintLabel)
    }

    fun render2x2Grid(spec: WidgetSpec, canvas: Canvas, wifiOn: Boolean = true, btOn: Boolean = true, torchOn: Boolean = false, dndOn: Boolean = false) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val halfW = spec.widthPx / 2f
        val halfH = spec.heightPx / 2f

        // Quadrant divider hairlines
        val paintDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
            strokeWidth = 1.5f
        }
        canvas.drawLine(halfW, 16f * spec.density, halfW, spec.heightPx - 16f * spec.density, paintDivider)
        canvas.drawLine(16f * spec.density, halfH, spec.widthPx - 16f * spec.density, halfH, paintDivider)

        val items = listOf(
            QuadToggle("WIFI", wifiOn, halfW / 2f, halfH / 2f),
            QuadToggle("BT", btOn, halfW + (halfW / 2f), halfH / 2f),
            QuadToggle("TORCH", torchOn, halfW / 2f, halfH + (halfH / 2f)),
            QuadToggle("DND", dndOn, halfW + (halfW / 2f), halfH + (halfH / 2f))
        )

        items.forEach { item ->
            // Active dot
            val dotColor = if (item.active) accentColor else offColor
            val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor }
            canvas.drawCircle(item.cx, item.cy - 8f * spec.density, 8f * spec.density, paintDot)

            // Label
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (item.active) onColor else 0xFF8A8A8A.toInt()
                textSize = 10f * spec.density
                isFakeBoldText = true
            }
            val lw = paintLabel.measureText(item.name)
            canvas.drawText(item.name, item.cx - (lw / 2f), item.cy + 16f * spec.density, paintLabel)
        }
    }

    private data class QuadToggle(val name: String, val active: Boolean, val cx: Float, val cy: Float)
}
