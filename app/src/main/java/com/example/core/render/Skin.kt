package com.example.core.render

import org.json.JSONObject

/**
 * Visual language a widget is drawn in. Applied centrally by [DotMatrixRenderer] (digits, labels,
 * dot runs/rings, background), so every widget can be restyled without its own code changing.
 */
enum class Skin(val label: String) {
    /** Classic 5×7 dot matrix. */
    DOT("Dot"),
    /** Nothing-style thin glowing light strips. */
    GLYPH("Glyph"),
    /** Retro 7-segment LCD. */
    SEGMENT("Segment"),
    /** Chunky square pixels, no gaps. */
    PIXEL("Pixel"),
    /** Monospace text on a dark scan-lined screen. */
    TERMINAL("Terminal"),
    /** Warm off-white paper with ink, always light. */
    PAPER("Paper"),
    /** Glowing dots on black, always dark. */
    NEON("Neon");

    /** Skins with a fixed light/dark look override the theme. */
    fun resolveDark(isDark: Boolean): Boolean = when (this) {
        PAPER -> false
        NEON, TERMINAL -> true
        else -> isDark
    }

    companion object {
        const val KEY = "skin"

        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: DOT

        fun from(configJson: String): Skin = fromName(runCatching { JSONObject(configJson).optString(KEY) }.getOrNull())
    }
}

/** Edge treatment drawn around the widget background. */
enum class Frame(val label: String) {
    HAIRLINE("Hairline"),
    NONE("None"),
    DOTTED("Dotted"),
    GLOW("Glow");

    companion object {
        const val KEY = "frame"

        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: HAIRLINE

        fun from(configJson: String): Frame = fromName(runCatching { JSONObject(configJson).optString(KEY) }.getOrNull())
    }
}
