package com.example.ui.components

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import com.example.core.render.WidgetShapes
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.WidgetShape

fun widgetShape(
    shape: WidgetShape,
    cornerRadiusFraction: Float,
    customPoints: List<ShapePoint> = emptyList()
): Shape = GenericShape { size, _ ->
    addPath(
        WidgetShapes.path(
            shape = shape,
            width = size.width,
            height = size.height,
            cornerRadius = size.minDimension * cornerRadiusFraction,
            customPoints = customPoints
        ).asComposePath()
    )
}
