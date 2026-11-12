package com.example.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import com.example.core.render.PhotoFraming
import java.io.File
import java.util.UUID
import org.json.JSONObject

/**
 * Optional custom background photo for any widget. The picked image is saved once as a JPEG in
 * app storage under a random id; the widget's config stores that id plus the user's placement
 * (zoom, focus point) and darkening, so the photo never leaves the phone and the placement can be
 * adjusted again later.
 */
object BackgroundImageStore {

    const val KEY_IMAGE = "bg_image"
    const val KEY_DIM = "bg_image_dim"
    const val KEY_ZOOM = "bg_image_zoom"
    const val KEY_CX = "bg_image_cx"
    const val KEY_CY = "bg_image_cy"
    const val DEFAULT_DIM = 0.45f

    /** Long side kept; room to zoom in on a large widget and stay sharp. */
    private const val MAX_SIDE = 1600
    private val ID = Regex("[A-Za-z0-9-]{8,64}")
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** A widget's photo and how it is placed. */
    data class Choice(
        val id: String,
        val dim: Float = DEFAULT_DIM,
        val zoom: Float = 1f,
        val cx: Float = 0.5f,
        val cy: Float = 0.5f
    ) {
        fun writeTo(json: JSONObject) {
            json.put(KEY_IMAGE, id)
            json.put(KEY_DIM, Math.round(dim * 100))
            json.put(KEY_ZOOM, Math.round(zoom * 1000) / 1000.0)
            json.put(KEY_CX, Math.round(cx * 10000) / 10000.0)
            json.put(KEY_CY, Math.round(cy * 10000) / 10000.0)
        }
    }

    fun clearFrom(json: JSONObject) {
        listOf(KEY_IMAGE, KEY_DIM, KEY_ZOOM, KEY_CX, KEY_CY).forEach { json.remove(it) }
    }

    private fun dir(context: Context) = File(context.filesDir, "backgrounds").apply { mkdirs() }

    private fun file(context: Context, id: String) = File(dir(context), "$id.jpg")

    /** The photo and placement a widget's config asks for, or null when it has none. */
    fun fromConfig(configJson: String?): Choice? {
        val json = runCatching { JSONObject(configJson ?: "{}") }.getOrNull() ?: return null
        val id = json.optString(KEY_IMAGE).takeIf { it.matches(ID) } ?: return null
        return Choice(
            id = id,
            dim = (json.optInt(KEY_DIM, (DEFAULT_DIM * 100).toInt()) / 100f).coerceIn(0f, 0.9f),
            zoom = json.optDouble(KEY_ZOOM, 1.0).toFloat().takeIf { !it.isNaN() }?.coerceIn(1f, PhotoFraming.MAX_ZOOM) ?: 1f,
            cx = json.optDouble(KEY_CX, 0.5).toFloat().takeIf { !it.isNaN() }?.coerceIn(0f, 1f) ?: 0.5f,
            cy = json.optDouble(KEY_CY, 0.5).toFloat().takeIf { !it.isNaN() }?.coerceIn(0f, 1f) ?: 0.5f
        )
    }

    /** Decodes, rotates and downscales the picked photo; returns its new id, or null if it can't be read. */
    fun save(context: Context, uri: Uri): String? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("decode failed")
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        save(context, Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true))
    } catch (_: Exception) {
        null
    }

    /** Saves an already-decoded bitmap; returns its id. */
    fun save(context: Context, bitmap: Bitmap): String {
        val id = UUID.randomUUID().toString()
        file(context, id).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return id
    }

    fun load(context: Context, id: String): Bitmap? {
        if (!id.matches(ID)) return null
        cache.get(id)?.let { return it }
        val f = file(context, id)
        if (!f.exists()) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull() ?: return null
        cache.put(id, bitmap)
        return bitmap
    }

    fun delete(context: Context, id: String) {
        if (!id.matches(ID)) return
        cache.remove(id)
        file(context, id).delete()
    }
}
