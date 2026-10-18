package com.example.widgets.base

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket

interface WidgetDefinition {
    val id: String
    val category: Category
    val title: String
    val description: String
    val sizes: Set<SizeBucket>
    val defaultTap: TapAction
    val requiresNetwork: Boolean
        get() = false

    fun render(context: Context, spec: WidgetSpec, canvas: Canvas)
}
