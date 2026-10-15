package com.example.core.bitmap

import android.graphics.Bitmap
import android.util.LruCache

/**
 * In-memory LRU cache to skip redundant canvas redrawing and avoid launcher binder thrash.
 */
object BitmapCache {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory

    private val lru = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun makeKey(widgetTypeId: String, instanceId: Int, valueHash: Int, size: SizeBucket, isDark: Boolean, accent: String): String {
        return "$widgetTypeId-$instanceId-$valueHash-${size.name}-$isDark-$accent"
    }

    fun get(key: String): Bitmap? {
        val bmp = lru.get(key)
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(key: String, bitmap: Bitmap) {
        lru.put(key, bitmap)
    }

    fun clear() {
        lru.evictAll()
    }
}
