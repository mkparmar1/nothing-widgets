package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.widgets.base.WidgetSpec

/** The search bar widget that fills the "missing launcher utility" gap. */
object UtilityRenderers {

    /**
     * A pill-shaped search bar. Tapping it opens the system web-search / assistant, so the widget
     * behaves like the search field of a launcher without needing any special permission.
     */
    fun renderSearchBar(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        if (spec.showLabel) kit.header("SEARCH")

        val barHeight = kit.height * 0.52f
        val barTop = if (spec.showLabel) kit.pad + 16f * d else (kit.height - barHeight) / 2f
        val barRect = RectF(kit.pad, barTop, kit.width - kit.pad, barTop + barHeight)
        val radius = barHeight / 2f

        canvas.drawRoundRect(barRect, radius, radius, kit.fill(kit.offColor))

        val iconRadius = barHeight * 0.16f
        val iconCx = barRect.left + barHeight * 0.5f
        val iconCy = barRect.centerY()
        canvas.drawCircle(iconCx, iconCy - iconRadius * 0.3f, iconRadius, kit.fill(kit.accentColor))
        canvas.drawCircle(iconCx, iconCy - iconRadius * 0.3f, iconRadius * 0.6f, kit.fill(if (spec.isDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()))
        canvas.drawLine(
            iconCx + iconRadius * 0.7f, iconCy + iconRadius * 0.4f,
            iconCx + iconRadius * 1.3f, iconCy + iconRadius * 1.0f,
            kit.fill(kit.accentColor).apply { strokeWidth = iconRadius * 0.5f; strokeCap = android.graphics.Paint.Cap.ROUND }
        )

        val hint = "SEARCH"
        val paint = kit.textPaint(kit.greyColor, 12f, 16f)
        val textX = iconCx + iconRadius * 2.0f
        DotMatrixRenderer.drawFittedLabel(canvas, hint, textX, iconCy + paint.textSize * 0.35f, (barRect.right - textX) * 0.6f, paint)

        if (spec.sizeBucket == SizeBucket.L) {
            kit.labelCentered("TAP TO SEARCH THE WEB", kit.height - kit.pad - 2f * d, kit.textPaint(kit.greyColor, 8f, 11f))
        }
    }
}
