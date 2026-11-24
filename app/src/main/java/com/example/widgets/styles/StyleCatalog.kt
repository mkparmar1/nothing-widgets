package com.example.widgets.styles

import com.example.core.config.WidgetOptions
import com.example.widgets.base.WidgetDefinition

/**
 * Which existing widgets offer extra designs, and what they are. Widgets not listed here are left
 * exactly as they were. Each category keeps its styles in its own file; this only merges them.
 */
object StyleCatalog {

    /** Option key holding the chosen style id. */
    const val KEY = "design"

    /** The implicit original design of every widget. */
    const val CLASSIC = "classic"

    private val byWidget: Map<String, List<WidgetStyle>> by lazy {
        val merged = LinkedHashMap<String, MutableList<WidgetStyle>>()
        listOf(
            ClockStyles.ALL,
            CalendarStyles.ALL,
            WeatherStyles.ALL,
            BatteryStyles.ALL,
            SystemStyles.ALL
        ).forEach { group ->
            group.forEach { (widgetId, styles) -> merged.getOrPut(widgetId) { mutableListOf() } += styles }
        }
        merged
    }

    /** Extra styles of [widgetId] (Classic excluded); empty when the widget has none. */
    fun stylesFor(widgetId: String): List<WidgetStyle> = byWidget[widgetId].orEmpty()

    /** Every widget id that has extra styles. */
    fun styledWidgetIds(): Set<String> = byWidget.keys

    /** Number of designs including Classic, for the browse badge. */
    fun designCount(widgetId: String): Int = stylesFor(widgetId).size + 1

    /** Adds the Style option to [definition] when it has extra styles; returns it unchanged otherwise. */
    fun decorate(definition: WidgetDefinition): WidgetDefinition {
        val styles = stylesFor(definition.id)
        return if (styles.isEmpty()) definition else StyledDefinition(definition, styles)
    }

    /** The style for [styleId], or null for Classic / an unknown id (both render the original design). */
    fun selectedStyle(styleId: String, styles: List<WidgetStyle>): WidgetStyle? =
        if (styleId == CLASSIC) null else styles.firstOrNull { it.id == styleId }

    /** The style a placed widget uses, read from its configJson; null = Classic. */
    fun selectedStyle(widgetId: String, configJson: String): WidgetStyle? {
        val styles = stylesFor(widgetId)
        if (styles.isEmpty()) return null
        return selectedStyle(WidgetOptions.parse(configJson).string(KEY, CLASSIC), styles)
    }

    /**
     * The id the render engine should use for id-specific live overlays and tap regions. A non-classic
     * style that draws its own layout gets an empty id so classic overlays never draw over it.
     */
    fun overlayId(widgetId: String, configJson: String): String {
        val style = selectedStyle(widgetId, configJson) ?: return widgetId
        return if (style.keepsClassicLayout) widgetId else ""
    }
}
