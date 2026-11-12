package com.example.core.render

/**
 * Where a background photo sits inside a widget, from the user's placement: [zoom] (1 = the
 * photo just covers the widget) and the focus point ([cx], [cy], 0..1 across the photo) that
 * lands in the widget's centre. The photo always covers the whole widget, so if the widget's
 * shape changes (another size, a launcher resize) the same spot stays in the middle.
 * Shared by the widget renderer and the placement editor so both show exactly the same crop.
 */
object PhotoFraming {

    const val MAX_ZOOM = 5f

    /** Photo scale (photo px → widget px) for a [frameW]×[frameH] widget. */
    fun scale(imageW: Int, imageH: Int, frameW: Float, frameH: Float, zoom: Float): Float =
        maxOf(frameW / imageW, frameH / imageH) * zoom.coerceIn(1f, MAX_ZOOM)

    /** Keeps the focus point far enough from the photo's edges that no empty space shows. */
    fun clampFocus(imageW: Int, imageH: Int, frameW: Float, frameH: Float, zoom: Float, cx: Float, cy: Float): Pair<Float, Float> {
        val s = scale(imageW, imageH, frameW, frameH, zoom)
        val halfX = (frameW / 2f) / (imageW * s)
        val halfY = (frameH / 2f) / (imageH * s)
        return cx.coerceIn(halfX, (1f - halfX).coerceAtLeast(halfX)) to cy.coerceIn(halfY, (1f - halfY).coerceAtLeast(halfY))
    }

    /** The photo's drawn bounds (left, top, right, bottom) for a frame starting at [left],[top]. */
    fun bounds(
        imageW: Int, imageH: Int,
        left: Float, top: Float, frameW: Float, frameH: Float,
        zoom: Float, cx: Float, cy: Float
    ): FloatArray {
        val s = scale(imageW, imageH, frameW, frameH, zoom)
        val (fx, fy) = clampFocus(imageW, imageH, frameW, frameH, zoom, cx, cy)
        val w = imageW * s
        val h = imageH * s
        val l = left + frameW / 2f - fx * w
        val t = top + frameH / 2f - fy * h
        return floatArrayOf(l, t, l + w, t + h)
    }
}
