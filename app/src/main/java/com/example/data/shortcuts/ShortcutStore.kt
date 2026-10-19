package com.example.data.shortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** One tappable target on a shortcut widget. */
data class Shortcut(
    val kind: Kind,
    /** What the widget shows, e.g. a contact's name or an app's label. */
    val label: String,
    /** Phone number, package name or URL depending on [kind]. */
    val value: String
) {
    enum class Kind { CALL, MESSAGE, APP, WEB }

    /** The intent the widget launches when this slot is tapped. */
    fun intent(context: Context): Intent? = when (kind) {
        Kind.CALL -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(value)}"))
        Kind.MESSAGE -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(value)}"))
        Kind.APP -> context.packageManager.getLaunchIntentForPackage(value)
        Kind.WEB -> Intent(Intent.ACTION_VIEW, Uri.parse(if (value.contains("://")) value else "https://$value"))
    }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** What each shortcut widget holds: how many slots and which kind of target. */
object ShortcutStore {

    data class Slots(val count: Int, val kind: Shortcut.Kind, val defaults: List<Shortcut> = emptyList())

    val WIDGETS = mapOf(
        "contact.favorite.single" to Slots(1, Shortcut.Kind.CALL),
        "contact.duo" to Slots(2, Shortcut.Kind.CALL),
        "contact.quad" to Slots(4, Shortcut.Kind.CALL),
        // 112 is the emergency number across India and the EU; it can be changed in the app.
        "shortcut.direct.dial" to Slots(1, Shortcut.Kind.CALL, listOf(Shortcut(Shortcut.Kind.CALL, "EMERGENCY", "112"))),
        "shortcut.direct.sms" to Slots(1, Shortcut.Kind.MESSAGE),
        "shortcut.apps.4" to Slots(4, Shortcut.Kind.APP),
        "shortcut.apps.6" to Slots(6, Shortcut.Kind.APP),
        "utility.folder.grid" to Slots(8, Shortcut.Kind.APP),
        "shortcut.web.favorite" to Slots(1, Shortcut.Kind.WEB)
    )

    private const val PREFS = "dot_widgets_shortcuts"

    /** Saved slots for a widget, padded with nulls (or defaults) up to its slot count. */
    fun get(context: Context, widgetId: String): List<Shortcut?> {
        val slots = WIDGETS[widgetId] ?: return emptyList()
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(widgetId, null)
            ?: return List(slots.count) { slots.defaults.getOrNull(it) }
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return List(slots.count) { i ->
            arr.optJSONObject(i)?.let {
                Shortcut(Shortcut.Kind.valueOf(it.optString("kind", slots.kind.name)), it.optString("label"), it.optString("value"))
            }
        }
    }

    fun set(context: Context, widgetId: String, index: Int, shortcut: Shortcut?) {
        val current = get(context, widgetId).toMutableList()
        if (index !in current.indices) return
        current[index] = shortcut
        val arr = JSONArray()
        current.forEach { s ->
            arr.put(s?.let { JSONObject().put("kind", it.kind.name).put("label", it.label).put("value", it.value) } ?: JSONObject.NULL)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(widgetId, arr.toString()).apply()
    }
}
