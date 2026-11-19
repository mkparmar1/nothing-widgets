package com.example.widgets.base

import com.example.core.bitmap.SizeBucket
import com.example.core.config.WidgetOptions
import com.example.core.render.Frame
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
    val liveOverlays: Boolean = false,
    /** Resolved advanced-option values (from `configJson`'s `"opt"` object). */
    val options: WidgetOptions = WidgetOptions.parse(configJson),
    /** Outline of the widget. [WidgetShape.ROUNDED] keeps the legacy corner-radius look. */
    val shape: WidgetShape = WidgetShape.ROUNDED,
    /** Normalized outline points used when [shape] is [WidgetShape.CUSTOM]. */
    val customPoints: List<ShapePoint> = emptyList(),
    /** Overrides the background fill colour for the shape; null keeps the theme background. */
    val shapeFillColor: Int? = null,
    /** Overrides the border colour; null keeps the theme edge colour. */
    val shapeBorderColor: Int? = null,
    /** Overrides the frame style; null keeps the widget's Look frame. */
    val shapeBorderStyle: Frame? = null
)

enum class TapAction(val displayName: String) {
    DEFAULT("Default (Smart Action)"),
    OPEN_CLOCK("Clock & Alarms"),
    OPEN_CALENDAR("Calendar App"),
    OPEN_BATTERY("Battery Settings"),
    OPEN_WEATHER("In-App Weather Forecast"),
    OPEN_MAIN("Open Dot Widgets App"),
    OPEN_SEARCH("Web Search"),
    OPEN_APP("Open an app"),
    NONE("Do Nothing")
}

enum class Category(val title: String, val iconName: String) {
    ALL("All", "widgets"),
    THEMES("Themes", "palette"),
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
    FUN("Fun & Lab", "casino"),
    FINANCE("Finance", "payments"),
    HEALTH("Health", "favorite"),
    SMART_HOME("Smart Home", "home"),
    SPORTS("Sports", "sports_soccer"),
    LEARNING("Learning", "school"),
    TRAVEL("Travel", "flight"),
    CUSTOM("My widgets", "widgets")
}
