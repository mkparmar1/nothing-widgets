package com.example.widgets.base

import kotlin.math.roundToInt

enum class ShapeGroup(val title: String) {
    BASIC("Basic"),
    GEOMETRIC("Geometric"),
    FUN("Fun"),
    SPECIAL("Special"),
    CUSTOM("Custom")
}

data class ShapePoint(val x: Float, val y: Float)

/** The outline of a widget. [ROUNDED] keeps the original corner-radius look. */
enum class WidgetShape(val label: String, val group: ShapeGroup) {
    SQUARE("Square", ShapeGroup.BASIC),
    ROUNDED("Rounded", ShapeGroup.BASIC),
    PILL("Pill", ShapeGroup.BASIC),
    CIRCLE("Circle", ShapeGroup.BASIC),
    ELLIPSE("Ellipse", ShapeGroup.BASIC),
    SQUIRCLE("Squircle", ShapeGroup.BASIC),
    CUT("Cut", ShapeGroup.BASIC),
    BEVEL("Bevel", ShapeGroup.BASIC),

    DIAMOND("Diamond", ShapeGroup.GEOMETRIC),
    SOFT_DIAMOND("Soft diamond", ShapeGroup.GEOMETRIC),
    TRIANGLE("Triangle", ShapeGroup.GEOMETRIC),
    TRIANGLE_DOWN("Down triangle", ShapeGroup.GEOMETRIC),
    PENTAGON("Pentagon", ShapeGroup.GEOMETRIC),
    HEXAGON("Hexagon", ShapeGroup.GEOMETRIC),
    OCTAGON("Octagon", ShapeGroup.GEOMETRIC),
    DECAGON("Decagon", ShapeGroup.GEOMETRIC),
    STAR_5("Star 5", ShapeGroup.GEOMETRIC),
    STAR_6("Star 6", ShapeGroup.GEOMETRIC),
    STAR_8("Star 8", ShapeGroup.GEOMETRIC),
    BURST("Burst", ShapeGroup.GEOMETRIC),

    GEAR("Gear", ShapeGroup.FUN),
    HEART("Heart", ShapeGroup.FUN),
    SHIELD("Shield", ShapeGroup.FUN),
    ARROW("Arrow", ShapeGroup.FUN),
    CHEVRON("Chevron", ShapeGroup.FUN),
    CROSS("Cross", ShapeGroup.FUN),
    PLUS("Plus", ShapeGroup.FUN),
    FLOWER_4("Flower 4", ShapeGroup.FUN),
    FLOWER_6("Flower 6", ShapeGroup.FUN),
    FLOWER_8("Flower 8", ShapeGroup.FUN),
    BLOB("Blob", ShapeGroup.FUN),

    SPEECH("Speech", ShapeGroup.SPECIAL),
    TICKET("Ticket", ShapeGroup.SPECIAL),
    LEAF("Leaf", ShapeGroup.SPECIAL),
    DROP("Drop", ShapeGroup.SPECIAL),
    CRESCENT("Crescent", ShapeGroup.SPECIAL),
    LENS("Lens", ShapeGroup.SPECIAL),
    RIBBON("Ribbon", ShapeGroup.SPECIAL),

    CUSTOM("Custom", ShapeGroup.CUSTOM);

    companion object {
        fun fromName(name: String?): WidgetShape =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ROUNDED

        fun defaultCustomPoints(): List<ShapePoint> = listOf(
            ShapePoint(0.5f, 0.05f),
            ShapePoint(0.9f, 0.25f),
            ShapePoint(0.9f, 0.75f),
            ShapePoint(0.5f, 0.95f),
            ShapePoint(0.1f, 0.75f),
            ShapePoint(0.1f, 0.25f)
        )

        fun encodePoints(points: List<ShapePoint>): String = points.joinToString(";") {
            "${format(it.x)},${format(it.y)}"
        }

        fun decodePoints(value: String?): List<ShapePoint> {
            if (value.isNullOrBlank()) return emptyList()
            val points = value.split(';').mapNotNull { part ->
                val values = part.split(',')
                if (values.size != 2) return@mapNotNull null
                val x = values[0].toFloatOrNull() ?: return@mapNotNull null
                val y = values[1].toFloatOrNull() ?: return@mapNotNull null
                if (x in 0f..1f && y in 0f..1f) ShapePoint(x, y) else null
            }
            return points.takeIf { it.size >= 3 } ?: emptyList()
        }

        private fun format(value: Float): String =
            (value * 1000).roundToInt().toFloat().div(1000).toString()
    }
}
