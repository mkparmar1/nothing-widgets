package com.example.core.bitmap

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class SizeBucket(val label: String, val cells: String) {
    S("Small", "2×2"),
    M("Medium", "4×2"),
    L("Large", "4×4");

    companion object {
        fun resolve(widthDp: Int, heightDp: Int): SizeBucket {
            return when {
                widthDp >= 200 && heightDp >= 200 -> L
                widthDp >= 200 -> M
                else -> S
            }
        }
    }
}

data class RenderDimensions(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val sizeBucket: SizeBucket
)

object SizeResolver {

    /**
     * RemoteViews bitmaps travel over Binder, which fails around 1 MB. Every widget is drawn
     * at its real on-screen pixel size, then automatically scaled down if it would exceed this.
     */
    private const val BINDER_BUDGET_BYTES = 1_000_000L

    /** Fallback dp sizes when the launcher reports nothing usable. */
    private fun defaultDp(sizeBucket: SizeBucket): Pair<Int, Int> = when (sizeBucket) {
        SizeBucket.S -> 150 to 150
        SizeBucket.M -> 300 to 150
        SizeBucket.L -> 260 to 260
    }

    /**
     * Renders at the widget's real pixel size and aspect ratio (density-corrected), clamped so the
     * ARGB_8888 bitmap stays under the Binder budget. When [customWidthDp]/[customHeightDp] are 0,
     * sensible per-bucket defaults are used.
     */
    fun resolveDimensions(
        context: Context,
        sizeBucket: SizeBucket,
        customWidthDp: Int = 0,
        customHeightDp: Int = 0,
        budgetBytes: Long = BINDER_BUDGET_BYTES
    ): RenderDimensions {
        val metrics = context.resources.displayMetrics
        val baseDensity = metrics.density.takeIf { it > 0f } ?: 2.5f

        val (dpW, dpH) = if (customWidthDp > 0 && customHeightDp > 0) {
            customWidthDp to customHeightDp
        } else {
            defaultDp(sizeBucket)
        }

        var pxW = (dpW * baseDensity).roundToInt().coerceAtLeast(100)
        var pxH = (dpH * baseDensity).roundToInt().coerceAtLeast(100)

        val bytes = pxW.toLong() * pxH.toLong() * 4L
        var scale = 1f
        if (bytes > budgetBytes) {
            scale = sqrt(budgetBytes.toDouble() / bytes.toDouble()).toFloat()
            // Round down: rounding up could land a few bytes over the budget.
            pxW = (pxW * scale).toInt().coerceAtLeast(100)
            pxH = (pxH * scale).toInt().coerceAtLeast(100)
        }

        return RenderDimensions(
            widthPx = pxW,
            heightPx = pxH,
            // Keep dp-based renderer sizing proportional to the pixels actually drawn.
            density = baseDensity * scale,
            sizeBucket = sizeBucket
        )
    }

    /**
     * Reads the real size from launcher options. Android 12+ exposes the exact list of sizes the
     * widget can take; older versions provide min/max width/height, where portrait is
     * MIN_WIDTH × MAX_HEIGHT.
     */
    fun resolveFromOptions(
        context: Context,
        options: Bundle?,
        sizeBucket: SizeBucket,
        budgetBytes: Long = BINDER_BUDGET_BYTES
    ): RenderDimensions {
        if (options != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                val largest = sizes?.maxByOrNull { it.width * it.height }
                if (largest != null && largest.width > 0f && largest.height > 0f) {
                    return resolveDimensions(context, sizeBucket, largest.width.roundToInt(), largest.height.roundToInt(), budgetBytes)
                }
            }
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            // Portrait (taller than wide) is the natural home-screen orientation.
            val widthDp = if (minW > 0) minW else maxW
            val heightDp = if (maxH > 0) maxH else minH
            if (widthDp > 0 && heightDp > 0) {
                return resolveDimensions(context, sizeBucket, widthDp, heightDp, budgetBytes)
            }
        }
        return resolveDimensions(context, sizeBucket, budgetBytes = budgetBytes)
    }
}
