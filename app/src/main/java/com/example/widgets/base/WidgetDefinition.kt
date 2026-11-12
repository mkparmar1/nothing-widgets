package com.example.widgets.base

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.core.config.WidgetOption
import com.example.ui.permissions.AppPermission

interface WidgetDefinition {
    val id: String
    val category: Category
    val title: String
    val description: String
    val renderVersion: Int
        get() = 0
    val sizes: Set<SizeBucket>
    val defaultTap: TapAction
    val requiresNetwork: Boolean
        get() = false

    /**
     * Advanced options shown in the setup screen's ADVANCED section and resolved into
     * [WidgetSpec.options]. Empty means the widget has no advanced controls.
     */
    val options: List<WidgetOption>
        get() = emptyList()

    /** Legacy single-permission declaration; use [requiredPermissions] when a widget needs several. */
    val requiredPermission: AppPermission?
        get() = null

    /** Permissions needed for this widget's live content. */
    val requiredPermissions: Set<AppPermission>
        get() = requiredPermission?.let(::setOf) ?: emptySet()

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
