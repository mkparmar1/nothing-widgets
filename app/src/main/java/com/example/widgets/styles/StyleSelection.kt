package com.example.widgets.styles

import java.util.concurrent.ConcurrentHashMap

/**
 * Hands the style picked on a widget's details screen to the setup sheet opened by "Customize".
 * Set on that tap and consumed once; a placed widget stores its style in its own config.
 */
object StyleSelection {

    private val picked = ConcurrentHashMap<String, String>()

    fun set(widgetId: String, styleId: String) {
        if (styleId == StyleCatalog.CLASSIC) picked.remove(widgetId) else picked[widgetId] = styleId
    }

    /** The style handed over for [widgetId], consumed so it can't leak into later widgets. */
    fun take(widgetId: String): String = picked.remove(widgetId) ?: StyleCatalog.CLASSIC
}
