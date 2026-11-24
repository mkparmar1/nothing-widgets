package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import com.example.widgets.base.WidgetSpec

/** The design family a style belongs to; shown as a small tag in the style picker. */
enum class StyleFamily(val label: String) {
    /** Rounded light/dark card, title-case label, red status dot, glyph icons, clean numerals. */
    SOFT("Soft"),
    /** High-contrast black tile with white numerals and accent highlights. */
    BOLD("Bold"),
    /** A one-off layout from the mockups (vinyl, hourglass, analog trio...). */
    SPECIAL("Special")
}

/**
 * One extra design of an existing widget. The widget's original renderer is always the implicit
 * "Classic" style; these are the alternatives a user can switch to in the style picker.
 */
data class WidgetStyle(
    /** Stable id stored in the widget's options under [StyleCatalog.KEY]. Never rename once shipped. */
    val id: String,
    val label: String,
    val family: StyleFamily,
    /**
     * True when this style keeps the classic layout's live overlays and tap regions (e.g. a 2×2 grid
     * that stays a 2×2 grid). False hides them so nothing classic draws over the new design.
     */
    val keepsClassicLayout: Boolean = false,
    val render: (Context, WidgetSpec, Canvas) -> Unit
)
