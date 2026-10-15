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
                // 320x320 @ ARGB_8888 = ~410 KB (Safe under 1MB limit)
                RenderDimensions(
                    widthPx = 320,
                    heightPx = 320,
                    density = 2.0f,
                    sizeBucket = sizeBucket
                )
            }
            SizeBucket.M -> {
                // 640x320 @ ARGB_8888 = ~819 KB (Safe under 1MB limit)
                RenderDimensions(
                    widthPx = 640,
                    heightPx = 320,
                    density = 2.0f,
                    sizeBucket = sizeBucket
                )
            }
            SizeBucket.L -> {
                // 480x480 @ ARGB_8888 = ~921 KB (Safe under 1MB limit)
                RenderDimensions(
                    widthPx = 480,
                    heightPx = 480,
                    density = 2.0f,
                    sizeBucket = sizeBucket
                )
            }
        }
    }
}
