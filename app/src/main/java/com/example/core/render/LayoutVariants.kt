package com.example.core.render

/**
 * Small helpers for the "layout variant" advanced option, so renderers that offer e.g. a ring or a
 * bar chart don't each re-implement the branch. Keep renderers thin: pick a branch here, draw there.
 */
object LayoutVariants {

    /** Chooses between a ring and a linear bar presentation (default "ring"). */
    inline fun <T> ringOrBar(variant: String, ring: () -> T, bar: () -> T): T =
        if (variant.equals("bar", ignoreCase = true)) bar() else ring()

    /** Chooses between a rows and columns layout (default "rows"). */
    inline fun <T> rowsOrColumns(variant: String, rows: () -> T, columns: () -> T): T =
        if (variant.equals("columns", ignoreCase = true)) columns() else rows()

    /**
     * Maps a user-facing variant name to how many items to show in a labelled list. Clamps to
     * [min]..[max] so a bad config can never overflow the widget.
     */
    fun itemCount(variant: String, min: Int, max: Int, fallback: Int): Int {
        val parsed = variant.toIntOrNull() ?: return fallback
        return parsed.coerceIn(min, max)
    }
}
