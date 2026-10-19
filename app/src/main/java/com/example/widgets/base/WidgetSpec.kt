package com.example.widgets.base

import com.example.core.bitmap.SizeBucket
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode

data class WidgetSpec(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val sizeBucket: SizeBucket,
    val accent: AccentOption,
    /**
     * The accent actually drawn. Equal to [AccentOption.hex] for fixed colours, or the resolved
     * wallpaper colour when [accent] is [AccentOption.DYNAMIC]. Renderers must use this, never
     * `accent.hex`, so Material You works everywhere.
     */
    val accentColor: Int = accent.hex,
    val bgMode: BackgroundMode,
    /** Background fill opacity, 0 = see-through, 1 = fully opaque. Ignored when [bgMode] is TRANSPARENT. */
    val bgAlpha: Float = 1f,
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val cornerRadiusDp: Int,
    val showLabel: Boolean,
    val tapAction: TapAction,
    val configJson: String = "{}",
    /**
     * True on the home screen, where live overlays (animated soundwave, ticking clocks/timers) are
     * drawn on top of the bitmap; false in in-app previews, which must draw everything themselves.
     */
    val liveOverlays: Boolean = false
)

enum class TapAction(val displayName: String) {
    DEFAULT("Default (Smart Action)"),
    OPEN_CLOCK("Clock & Alarms"),
    OPEN_CALENDAR("Calendar App"),
    OPEN_BATTERY("Battery Settings"),
    OPEN_WEATHER("In-App Weather Forecast"),
    OPEN_MAIN("Open Dot Widgets App"),
    OPEN_SEARCH("Web Search"),
    NONE("Do Nothing")
}

enum class Category(val title: String, val iconName: String) {
    ALL("All", "widgets"),
    CLOCK("Clock", "schedule"),
    CALENDAR("Calendar", "calendar_month"),
    BATTERY("Battery", "battery_charging_full"),
    WEATHER("Weather", "wb_sunny"),
    SYSTEM("Sensors & System", "sensors"),
    TOGGLES("Toggles", "toggle_on"),
    CONTACTS("Shortcuts", "person"),
    PRODUCTIVITY("Productivity", "checklist"),
    PHOTO("Photo", "image"),
    MEDIA("Media", "music_note"),
    FUN("Fun & Lab", "casino")
}
