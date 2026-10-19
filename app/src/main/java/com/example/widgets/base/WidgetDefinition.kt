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

    /**
     * True when this widget is safe and useful on the Android 14+ lock screen / Always-On Display
     * (glanceable, no live network or sensor dependency). Only lock-safe widgets are offered by the
     * keyguard providers; the browser marks them "LOCK SCREEN READY".
     */
    val lockSafe: Boolean
        get() = false

    fun render(context: Context, spec: WidgetSpec, canvas: Canvas)

    /** TalkBack description for the placed widget. Overridden by the main families. */
    fun describe(context: Context): String = title
}
