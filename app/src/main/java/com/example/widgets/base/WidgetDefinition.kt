package com.example.widgets.base

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.ui.permissions.AppPermission

interface WidgetDefinition {
    val id: String
    val category: Category
    val title: String
    val description: String
    val sizes: Set<SizeBucket>
    val defaultTap: TapAction
    val requiresNetwork: Boolean
        get() = false

    /**
     * The single permission this widget needs to show real data. When it isn't granted the setup
     * screen offers an inline "allow" banner and the placed widget shows a tappable prompt instead
     * of the design.
     */
    val requiredPermission: AppPermission?
        get() = null

    fun render(context: Context, spec: WidgetSpec, canvas: Canvas)

    /** TalkBack description for the placed widget. Overridden by the main families. */
    fun describe(context: Context): String = title
}
