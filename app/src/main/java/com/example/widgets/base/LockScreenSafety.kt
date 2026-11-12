package com.example.widgets.base

import com.example.core.theme.BackgroundMode
import com.example.data.photos.BackgroundImageStore
import org.json.JSONObject

/**
 * Lock-screen widgets sit on top of an unknown wallpaper, so they need a guaranteed backdrop.
 * The user's transparent choice is kept for the home screen but rendered as a scrim on the keyguard.
 */
object LockScreenSafety {

    /** Background actually drawn for a widget, accounting for the lock-screen readability guard. */
    fun effectiveBackground(requested: BackgroundMode, onLockScreen: Boolean): BackgroundMode =
        if (onLockScreen && requested == BackgroundMode.TRANSPARENT) BackgroundMode.SCRIM else requested

    /**
     * Families that surface personal content (people, messages, media titles, notes, to-dos, photos,
     * health, money or app usage). None of these may render on the lock screen while the phone is
     * locked. Kept as prefixes/ids so a new variant in the same family is covered automatically.
     */
    private val PRIVATE_PREFIXES = listOf(
        "contact.", "shortcut.", "media.", "photo.", "prod.notes", "prod.todo", "prod.search",
        "prod.tally", "calendar.agenda", "calendar.timeline", "health.", "finance.", "smart.",
        "sports.", "learning.", "travel.", "sensor.visualizer", "battery.screen",
        "sys.top.apps", "sys.mobile.data", "sys.network.speed"
    )

    private val PRIVATE_IDS = setOf(
        "fun.message.scramble", "fun.secret.cipher", "fun.private.notes"
    )

    fun showsPrivateData(widgetTypeId: String): Boolean =
        widgetTypeId in PRIVATE_IDS || PRIVATE_PREFIXES.any { widgetTypeId.startsWith(it) }

    /**
     * Removes private config payloads before a design is rendered on the keyguard. A lock-safe
     * clock can still have a personal photo attached from its home-screen setup; that photo must
     * never be visible while the phone is locked.
     */
    fun sanitizedConfig(configJson: String?): String {
        if (configJson.isNullOrBlank()) return "{}"
        val json = runCatching { JSONObject(configJson) }.getOrNull() ?: return "{}"
        BackgroundImageStore.clearFrom(json)
        return json.toString()
    }

    /** A design is safe for the keyguard only when it is marked lock-safe and carries no private data. */
    fun safeForLockScreen(definition: WidgetDefinition?): Boolean =
        definition != null &&
            definition.lockSafe &&
            !definition.requiresNetwork &&
            definition.requiredPermission == null &&
            !showsPrivateData(definition.id)
}
