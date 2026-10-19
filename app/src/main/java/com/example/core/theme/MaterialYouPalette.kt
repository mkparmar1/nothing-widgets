package com.example.core.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build

/**
 * Resolves the wallpaper-derived accent used by [AccentOption.DYNAMIC] ("Wallpaper" / Material You).
 *
 * Reads the system wallpaper's primary colour via [WallpaperManager.getWallpaperColors] and nudges
 * it into a legible range so it stays visible as a dot-matrix accent on black as well as paper
 * backgrounds. Falls back to red when the platform can't provide a colour.
 */
object MaterialYouPalette {

    fun resolve(context: Context, fallback: Int = AccentOption.RED.hex): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return fallback
        val source = try {
            WallpaperManager.getInstance(context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?.primaryColor
                ?.toArgb()
        } catch (_: Exception) {
            null
        } ?: return fallback
        return harmonize(source)
    }

    /** Keeps the wallpaper colour saturated and mid-bright so it reads on both themes. */
    internal fun harmonize(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceIn(0.55f, 1f)
        hsv[2] = hsv[2].coerceIn(0.62f, 0.92f)
        return Color.HSVToColor(hsv)
    }
}
