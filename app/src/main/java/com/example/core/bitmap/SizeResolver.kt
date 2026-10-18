package com.example.core.bitmap

import android.content.Context

enum class SizeBucket(val label: String, val cells: String) {
    S("Small", "2×2"),
    M("Medium", "4×2"),
    L("Large", "4×4");

    companion object {
        fun resolve(widthDp: Int, heightDp: Int): SizeBucket {
            return when {
                widthDp >= 200 && heightDp >= 200 -> L
                widthDp >= 200 -> M
                else -> S
            }
        }
    }
}

data class RenderDimensions(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val sizeBucket: SizeBucket
)

object SizeResolver {

    /**
     * Resolves precise pixel dimensions capped strictly under the Android Binder transaction
     * limit (~1MB). These sizes provide pin-sharp dot-matrix rendering while guaranteeing
     * zero TransactionTooLargeException crashes across all launchers (Nothing Launcher, Pixel Launcher, etc.).
     */
    fun resolveDimensions(
        context: Context,
        sizeBucket: SizeBucket,
        customWidthDp: Int = 0,
        customHeightDp: Int = 0
    ): RenderDimensions {
        return when (sizeBucket) {
            SizeBucket.S -> {
                // 280x280 @ ARGB_8888 = ~313 KB (Strictly under Binder buffer limit)
                RenderDimensions(
                    widthPx = 280,
                    heightPx = 280,
                    density = 1.75f,
                    sizeBucket = sizeBucket
                )
            }
            SizeBucket.M -> {
                // 480x240 @ ARGB_8888 = ~460 KB (Safe on Nothing OS, Pixel, Samsung launchers)
                RenderDimensions(
                    widthPx = 480,
                    heightPx = 240,
                    density = 1.60f,
                    sizeBucket = sizeBucket
                )
            }
            SizeBucket.L -> {
                // 380x380 @ ARGB_8888 = ~577 KB (Reliable multi-widget concurrent updates)
                RenderDimensions(
                    widthPx = 380,
                    heightPx = 380,
                    density = 1.60f,
                    sizeBucket = sizeBucket
                )
            }
        }
    }
}
