package com.example.data.setups

import android.content.Context
import com.example.core.theme.AccentOption
import org.json.JSONObject

/**
 * Bundled, backend-free "Featured / Community" feed. Ships as `assets/community_setups.json` so it
 * works offline and needs no server; it gives the setup-sharing feature a home and keeps users in-app.
 */
object CommunityRepository {

    data class CommunityPick(
        val id: String,
        val title: String,
        val author: String,
        val tagline: String,
        val accent: AccentOption,
        val wallpaperIndex: Int,
        val widgetIds: List<String>
    )

    @Volatile
    private var cached: List<CommunityPick>? = null

    fun picks(context: Context): List<CommunityPick> {
        cached?.let { return it }
        val parsed = runCatching {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr = root.optJSONArray("picks")
            val list = mutableListOf<CommunityPick>()
            for (i in 0 until (arr?.length() ?: 0)) {
                val o = arr!!.optJSONObject(i) ?: continue
                val ids = o.optJSONArray("widgetIds") ?: continue
                val widgetIds = (0 until ids.length()).mapNotNull { ids.optString(it).takeIf { s -> s.isNotBlank() } }
                list += CommunityPick(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    author = o.optString("author"),
                    tagline = o.optString("tagline"),
                    accent = AccentOption.fromName(o.optString("accent", AccentOption.RED.name)),
                    wallpaperIndex = o.optInt("wallpaperIndex", 0),
                    widgetIds = widgetIds
                )
            }
            list
        }.getOrElse { emptyList() }
        cached = parsed
        return parsed
    }

    private const val ASSET = "community_setups.json"
}
