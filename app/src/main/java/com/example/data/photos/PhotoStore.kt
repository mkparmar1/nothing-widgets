package com.example.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File

/** Photos chosen for the photo widgets, stored as small JPEGs in app storage. */
object PhotoStore {

    /** How many photos each photo widget uses. */
    val WIDGETS = mapOf(
        "photo.single.frame" to 1,
        "photo.duo.collage" to 2,
        "photo.monochrome.art" to 1,
        "photo.clock.overlay" to 1
    )

    private const val MAX_SIDE = 720
    private val memory = HashMap<String, Pair<Long, Bitmap>>()

    private fun file(context: Context, widgetId: String, slot: Int) =
        File(File(context.filesDir, "photos").apply { mkdirs() }, "${widgetId}_$slot.jpg")

    fun has(context: Context, widgetId: String, slot: Int) = file(context, widgetId, slot).exists()

    /** Decodes, rotates and downscales the picked image, then saves it for the widget. */
    fun save(context: Context, widgetId: String, slot: Int, uri: Uri): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("decode failed")

        val rotation = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        val finalBmp = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        file(context, widgetId, slot).outputStream().use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        synchronized(memory) { memory.remove("$widgetId/$slot") }
        true
    } catch (_: Exception) {
        false
    }

    fun remove(context: Context, widgetId: String, slot: Int) {
        file(context, widgetId, slot).delete()
        synchronized(memory) { memory.remove("$widgetId/$slot") }
    }

    /** The saved photo, or null if none was picked. Decoded bitmaps are kept in memory until the file changes. */
    fun load(context: Context, widgetId: String, slot: Int): Bitmap? {
        val f = file(context, widgetId, slot)
        if (!f.exists()) return null
        val key = "$widgetId/$slot"
        synchronized(memory) {
            memory[key]?.let { (modified, bmp) -> if (modified == f.lastModified()) return bmp }
        }
        val bmp = BitmapFactory.decodeFile(f.path) ?: return null
        synchronized(memory) { memory[key] = f.lastModified() to bmp }
        return bmp
    }
}
