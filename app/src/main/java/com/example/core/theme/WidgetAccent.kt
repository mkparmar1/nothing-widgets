package com.example.core.theme

import android.content.Context
import org.json.JSONObject

/**
 * A widget's resolved accent colour. A custom colour (optional `accent_custom` key, "#RRGGBB")
 * wins over the named [AccentOption]; configs without the key behave exactly as before.
 */
object WidgetAccent {

    const val CUSTOM_KEY = "accent_custom"

    fun custom(configJson: String?): Int? {
        val raw = runCatching { JSONObject(configJson ?: "{}").optString(CUSTOM_KEY) }.getOrNull()
        return parseHex(raw)
    }

    fun resolve(context: Context, accent: AccentOption, configJson: String?): Int =
        custom(configJson) ?: if (accent == AccentOption.DYNAMIC) MaterialYouPalette.resolve(context) else accent.hex

    fun toHex(color: Int): String = String.format("#%06X", color and 0xFFFFFF)

    fun parseHex(raw: String?): Int? {
        val hex = raw?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
        return hex.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
    }
}
