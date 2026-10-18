package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.core.render.RenderKit
import com.example.data.photos.PhotoStore
import com.example.widgets.base.WidgetSpec
import java.util.Calendar
import java.util.Locale

/** Widgets showing photos the user picked in the app. */
object PhotoRenderers {

    /** Photo filling the widget inside a dotted frame. */
    fun renderFrame(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val photo = PhotoStore.load(context, "photo.single.frame", 0)
            ?: return RenderKit.message(spec, canvas, "PHOTO FRAME", "PICK A PHOTO IN THE APP")
        val kit = RenderKit(spec, canvas)
        kit.background()
        val inset = kit.pad * 0.7f
        val rect = RectF(inset, inset, kit.width - inset, kit.height - inset)
        drawPhoto(canvas, photo, rect, spec.cornerRadiusDp * kit.d * 0.6f)
        drawDotBorder(kit, RectF(inset * 0.45f, inset * 0.45f, kit.width - inset * 0.45f, kit.height - inset * 0.45f))
    }

    /** Two photos side by side. */
    fun renderDuo(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val left = PhotoStore.load(context, "photo.duo.collage", 0)
        val right = PhotoStore.load(context, "photo.duo.collage", 1)
        if (left == null && right == null) return RenderKit.message(spec, canvas, "DUO PHOTOS", "PICK TWO PHOTOS IN THE APP")
        val kit = RenderKit(spec, canvas)
        kit.background()
        val inset = kit.pad * 0.7f
        val gap = 6f * kit.d
        val mid = kit.width / 2f
        val r = spec.cornerRadiusDp * kit.d * 0.6f
        listOf(left to RectF(inset, inset, mid - gap / 2, kit.height - inset), right to RectF(mid + gap / 2, inset, kit.width - inset, kit.height - inset))
            .forEach { (bmp, rect) ->
                if (bmp != null) drawPhoto(canvas, bmp, rect, r)
                else kit.labelCentered("ADD PHOTO", rect.centerY(), kit.textPaint(kit.greyColor, 10f, 13f), rect.width())
            }
    }

    /** The photo as a monochrome dot halftone: darker areas get bigger dots. */
    fun renderHalftone(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val photo = PhotoStore.load(context, "photo.monochrome.art", 0)
            ?: return RenderKit.message(spec, canvas, "HALFTONE ART", "PICK A PHOTO IN THE APP")
        val kit = RenderKit(spec, canvas)
        kit.background()
        val cols = if (kit.width > kit.height * 1.5f) 44 else 30
        val step = (kit.width - kit.pad) / cols
        val rows = ((kit.height - kit.pad) / step).toInt()
        val crop = centerCrop(photo, cols, rows)
        val sampled = Bitmap.createScaledBitmap(crop, cols, rows, true)
        val left = (kit.width - cols * step) / 2f
        val top = (kit.height - rows * step) / 2f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val px = sampled.getPixel(x, y)
                val lum = (0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)) / 255.0
                val v = if (spec.isDark) lum else 1 - lum
                if (v < 0.06) continue
                canvas.drawCircle(left + step * (x + 0.5f), top + step * (y + 0.5f), step * 0.48f * v.toFloat(), kit.fill(kit.onColor))
            }
        }
    }

    /** Photo background with a dimmed band and the current time in dots. */
    fun renderClockOverlay(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val photo = PhotoStore.load(context, "photo.clock.overlay", 0)
        if (photo == null) return ClockRenderers.renderDigitalClock(spec, canvas)
        val kit = RenderKit(spec, canvas)
        kit.background()
        val inset = 1.5f
        val rect = RectF(inset, inset, kit.width - inset, kit.height - inset)
        drawPhoto(canvas, photo, rect, spec.cornerRadiusDp * kit.d)
        // Darken behind the time so it stays readable on bright photos
        canvas.drawRect(RectF(0f, kit.height * 0.30f, kit.width, kit.height * 0.70f), kit.fill(0x88000000.toInt()))

        val cal = Calendar.getInstance()
        val time = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        kit.dotTextCentered(time, kit.height * 0.50f, kit.height * 0.26f, 10f * kit.d, accentIndices = RenderKit.colonIndices(time), color = Color.WHITE)
    }

    private fun drawPhoto(canvas: Canvas, photo: Bitmap, rect: RectF, radius: Float) {
        val crop = centerCropRect(photo.width, photo.height, rect.width(), rect.height())
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) })
        canvas.drawBitmap(photo, crop, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()
    }

    private fun drawDotBorder(kit: RenderKit, rect: RectF) {
        val step = 9f * kit.d
        val paint = kit.fill(kit.accentColor)
        var x = rect.left
        while (x <= rect.right) {
            kit.canvas.drawCircle(x, rect.top, 1.6f * kit.d, paint)
            kit.canvas.drawCircle(x, rect.bottom, 1.6f * kit.d, paint)
            x += step
        }
        var y = rect.top + step
        while (y < rect.bottom) {
            kit.canvas.drawCircle(rect.left, y, 1.6f * kit.d, paint)
            kit.canvas.drawCircle(rect.right, y, 1.6f * kit.d, paint)
            y += step
        }
    }

    /** Source rectangle that fills [dstW] x [dstH] without distortion. */
    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Float, dstH: Float): Rect {
        val srcRatio = srcW.toFloat() / srcH
        val dstRatio = dstW / dstH
        return if (srcRatio > dstRatio) {
            val w = (srcH * dstRatio).toInt()
            Rect((srcW - w) / 2, 0, (srcW + w) / 2, srcH)
        } else {
            val h = (srcW / dstRatio).toInt()
            Rect(0, (srcH - h) / 2, srcW, (srcH + h) / 2)
        }
    }

    private fun centerCrop(photo: Bitmap, w: Int, h: Int): Bitmap {
        val r = centerCropRect(photo.width, photo.height, w.toFloat(), h.toFloat())
        return Bitmap.createBitmap(photo, r.left, r.top, r.width(), r.height())
    }
}
