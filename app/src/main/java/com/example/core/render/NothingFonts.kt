package com.example.core.render

import android.graphics.Typeface

/**
 * Nothing OS registers its dot-matrix and mono fonts as system font families ("NDot57", "Lettera").
 * Apps can use them by name on Nothing phones; elsewhere the names fall back to the default font,
 * which we detect so the Nothing skin can fall back to the Dot look instead.
 */
object NothingFonts {

    private val fallback: Typeface by lazy { Typeface.create("dot-widgets-missing-family", Typeface.NORMAL) }

    /** NDot57 digits/caps, or null when this phone doesn't have Nothing's fonts. */
    val ndot: Typeface? by lazy { Typeface.create("NDot57", Typeface.NORMAL).takeIf { it != fallback } }

    /** Lettera Mono for labels, or null when unavailable. */
    val lettera: Typeface? by lazy { Typeface.create("Lettera", Typeface.NORMAL).takeIf { it != fallback } }

    val available: Boolean get() = ndot != null
}
