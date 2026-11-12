package com.example.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import com.example.core.time.DotStyle
import com.example.data.photos.PhotoFilter
import kotlin.math.roundToInt

/**
 * Redraws a photo as Nothing-style dot-matrix art. Each cell of a grid gets one dot; for mono and
 * accent modes the dot gets bigger and more opaque the darker the photo (white/accent dots on
 * black), while colour mode keeps a fixed dot size and samples the photo's own colours.
 *
 * Filtered bitmaps are expensive to build, so they are cached by photo, placement, output size,
 * filter, dot size, accent and dot style. The dots reuse [DotMatrixRenderer]'s current dot shape,
 * so the user's Dot style (round, square, bold, minimal) and skin apply here too.
 */
object PhotoDotFilter {

    /** Dots per side are capped so a big widget stays cheap to draw. */
    private const val MAX_CELLS = 48
    private const val MIN_CELLS = 6
    private const val MAX_CACHED_BYTES = 12 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHED_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private val samplePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    /**
     * Draws [image]'s [src] region into [dst] as dots. [cacheKey] identifies the photo (e.g. its
     * stored id) so the cache never mixes two photos with the same size.
     */
    fun draw(
        canvas: Canvas,
        image: Bitmap,
        src: RectF,
        dst: RectF,
        filter: PhotoFilter,
        dotSize: Float,
        accentColor: Int,
        cacheKey: String
    ) {
        if (filter == PhotoFilter.OFF) return
        val outW = dst.width().roundToInt().coerceIn(1, 2048)
        val outH = dst.height().roundToInt().coerceIn(1, 2048)
        val style = DotMatrixRenderer.currentDotStyle
        val skin = DotMatrixRenderer.currentSkin
        val clamped = clampToImage(src, image)
        val key = listOf(
            cacheKey, image.width, image.height, clamped.left, clamped.top, clamped.right, clamped.bottom,
            outW, outH, filter.name, dotSize.coerceIn(0.5f, 2.5f), accentColor, style.name, skin.name
        ).joinToString("|")
        val filtered = cache.get(key) ?: build(
            image = image,
            src = Rect(
                clamped.left.roundToInt(), clamped.top.roundToInt(),
                clamped.right.roundToInt(), clamped.bottom.roundToInt()
            ),
            outW = outW,
            outH = outH,
            filter = filter,
            dotSize = dotSize,
            accentColor = accentColor,
            style = style,
            skin = skin
        ).also { cache.put(key, it) }
        canvas.drawBitmap(filtered, null, dst, drawPaint)
    }

    /**
     * The whole photo as dots on a transparent background, for the live editor preview. Returns
     * null when the filter is off. The preview keeps the photo's aspect ratio (dots scale with
     * the placement zoom) and is capped in size so dragging stays smooth.
     */
    fun filteredWhole(image: Bitmap, filter: PhotoFilter, dotSize: Float, accentColor: Int): Bitmap? {
        if (filter == PhotoFilter.OFF) return null
        val scale = minOf(1f, 720f / maxOf(image.width, image.height))
        val outW = (image.width * scale).roundToInt().coerceAtLeast(1)
        val outH = (image.height * scale).roundToInt().coerceAtLeast(1)
        val key = listOf(
            "whole", System.identityHashCode(image), image.width, image.height,
            outW, outH, filter.name, dotSize.coerceIn(0.5f, 2.5f), accentColor,
            DotStyle.ROUND.name, Skin.DOT.name
        ).joinToString("|")
        cache.get(key)?.let { return it }
        val built = build(
            image = image,
            src = Rect(0, 0, image.width, image.height),
            outW = outW,
            outH = outH,
            filter = filter,
            dotSize = dotSize,
            accentColor = accentColor,
            style = DotStyle.ROUND,
            skin = Skin.DOT
        )
        cache.put(key, built)
        return built
    }

    private fun build(
        image: Bitmap,
        src: Rect,
        outW: Int,
        outH: Int,
        filter: PhotoFilter,
        dotSize: Float,
        accentColor: Int,
        style: DotStyle,
        skin: Skin
    ): Bitmap {
        val size = dotSize.coerceIn(0.5f, 2.5f)
        val cols = (outW / (size * 8f)).roundToInt().coerceIn(MIN_CELLS, MAX_CELLS)
        val rows = (outH / (size * 8f)).roundToInt().coerceIn(MIN_CELLS, MAX_CELLS)

        // Sample the visible part of the photo on a small grid, then expand each sample to a dot.
        val sample = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        Canvas(sample).drawBitmap(image, src, RectF(0f, 0f, cols.toFloat(), rows.toFloat()), samplePaint)

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cellW = outW.toFloat() / cols
        val cellH = outH.toFloat() / rows
        val cell = minOf(cellW, cellH)
        val accentR = Color.red(accentColor)
        val accentG = Color.green(accentColor)
        val accentB = Color.blue(accentColor)

        DotMatrixRenderer.withStyle(style, skin) {
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val px = sample.getPixel(x, y)
                    val r = Color.red(px)
                    val g = Color.green(px)
                    val b = Color.blue(px)
                    val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    val cx = cellW * (x + 0.5f)
                    val cy = cellH * (y + 0.5f)
                    if (filter == PhotoFilter.COLOUR) {
                        // Fixed dot size, the photo's colour, so bright areas read as colour.
                        dotPaint.color = Color.argb(255, r, g, b)
                        DotMatrixRenderer.drawLitDot(canvas, cx, cy, cell * 0.45f, dotPaint)
                    } else {
                        val alpha = ((1f - lum).coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
                        if (alpha < 36) continue
                        dotPaint.color = when (filter) {
                            PhotoFilter.MONO -> Color.argb(alpha, 255, 255, 255)
                            PhotoFilter.ACCENT -> Color.argb(alpha, accentR, accentG, accentB)
                            PhotoFilter.OFF, PhotoFilter.COLOUR -> Color.TRANSPARENT
                        }
                        val radius = (cell * 0.45f * (0.35f + (1f - lum) * 0.9f)).coerceAtLeast(1f)
                        DotMatrixRenderer.drawLitDot(canvas, cx, cy, radius, dotPaint)
                    }
                }
            }
        }
        sample.recycle()
        return out
    }

    private fun clampToImage(src: RectF, image: Bitmap): RectF {
        val left = src.left.coerceIn(0f, image.width.toFloat())
        val top = src.top.coerceIn(0f, image.height.toFloat())
        val right = src.right.coerceIn(left + 1f, image.width.toFloat())
        val bottom = src.bottom.coerceIn(top + 1f, image.height.toFloat())
        return RectF(left, top, right, bottom)
    }
}
