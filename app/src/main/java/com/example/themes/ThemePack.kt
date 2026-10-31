package com.example.themes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import org.json.JSONObject

/** A themed set of tile images in `assets/themes/<id>/`, described by its `theme.json`. */
data class ThemePack(
    val id: String,
    val name: String,
    val description: String,
    /** Text and icons on a tile. */
    val onTile: Int,
    val onButtonOn: Int,
    val onButtonOff: Int,
    /** Secondary text on a tile. */
    val muted: Int,
    /** Rings, progress and highlights. */
    val accent: Int
)

/** Bundled packs, the one in use, and their decoded images. */
object ThemeRepository {

    /** Config key that pins a widget to a pack; used for previews of packs that aren't selected. */
    const val KEY_PACK = "theme_pack"

    private const val ROOT = "themes"
    private const val PREFS = "dot_widgets_themes"
    private const val KEY_SELECTED = "selected_pack"
    private const val KEY_ANIMATIONS = "animations"

    /** Bundled packs, in gallery order. */
    private val PACK_IDS = listOf("clay_red", "clay_blue")

    /** Used if a pack can't be read: live text still draws, without tile images. */
    private val FALLBACK = ThemePack("none", "", "", Color.WHITE, Color.WHITE, Color.WHITE, Color.LTGRAY, Color.RED)

    private var cached: List<ThemePack>? = null
    private val imageCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun packs(context: Context): List<ThemePack> = cached ?: synchronized(this) {
        val assets = context.assets
        val loaded = PACK_IDS.mapNotNull { id ->
            runCatching {
                val o = JSONObject(assets.open("$ROOT/$id/theme.json").bufferedReader().use { it.readText() })
                ThemePack(
                    id = id,
                    name = o.getString("name"),
                    description = o.getString("description"),
                    onTile = Color.parseColor(o.getString("onTile")),
                    onButtonOn = Color.parseColor(o.getString("onButtonOn")),
                    onButtonOff = Color.parseColor(o.getString("onButtonOff")),
                    muted = Color.parseColor(o.getString("muted")),
                    accent = Color.parseColor(o.getString("accent"))
                )
            }.getOrNull()
        }
        if (loaded.isNotEmpty()) cached = loaded
        loaded
    }

    fun selected(context: Context): ThemePack {
        val all = packs(context)
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SELECTED, null)
        return all.firstOrNull { it.id == id } ?: all.firstOrNull() ?: FALLBACK
    }

    fun select(context: Context, pack: ThemePack) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SELECTED, pack.id).apply()
    }

    fun animationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ANIMATIONS, true)

    fun setAnimationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ANIMATIONS, enabled).apply()
    }

    /** The pack a widget draws with: pinned in its config, otherwise the selected one. */
    fun forConfig(context: Context, configJson: String): ThemePack {
        val id = runCatching { JSONObject(configJson).optString(KEY_PACK) }.getOrDefault("")
        return packs(context).firstOrNull { it.id == id } ?: selected(context)
    }

    /** A pack image, or null if the pack doesn't have it. */
    fun image(context: Context, pack: ThemePack, name: String): Bitmap? {
        val key = "${pack.id}/$name"
        imageCache.get(key)?.let { return it }
        val bitmap = runCatching {
            context.assets.open("$ROOT/$key.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        imageCache.put(key, bitmap)
        return bitmap
    }
}
