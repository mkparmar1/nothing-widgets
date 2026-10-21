package com.example.data.wallpapers

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WallpaperItem(
    val id: Int,
    val name: String,
    val tag: String,
    val bgHex: Int,
    val accentHex: Int,
    val patternType: Int
)

object WallpaperManagerHelper {

    /** Accent palette wallpapers are generated from; matches the widget [com.example.core.theme.AccentOption] set. */
    private val PALETTE = listOf(
        "MONO" to 0xFFFFFFFF.toInt(),
        "RED" to 0xFFE02B2B.toInt(),
        "CYAN" to 0xFF00C2D1.toInt(),
        "AMBER" to 0xFFF2A516.toInt(),
        "LIME" to 0xFFB8E000.toInt(),
        "VIOLET" to 0xFF8A5CF6.toInt(),
        "ROSE" to 0xFFF43F5E.toInt()
    )

    /** 7 palettes × 16 pattern/darkness combinations = 112 wallpapers, each matched to a widget accent. */
    val WALLPAPERS = (0 until 112).map { index ->
        val (tag, accent) = PALETTE[index % PALETTE.size]
        // Every 8th tile is a light "paper" variant; the rest are dark/OLED.
        val isPaper = index % 8 == 3
        val isOled = index % 5 == 4
        val bg = when {
            isPaper -> 0xFFF2F2F2.toInt()
            isOled -> 0xFF000000.toInt()
            else -> 0xFF0A0A0A.toInt()
        }
        val variant = index / PALETTE.size
        val name = if (isPaper) "PAPER $tag #${String.format("%02d", variant + 1)}"
        else "DOT $tag #${String.format("%02d", variant + 1)}"

        WallpaperItem(
            id = index,
            name = name,
            tag = tag,
            bgHex = bg,
            accentHex = accent,
            patternType = index % 6
        )
    }

    /**
     * The wallpaper a setup applies: one whose accent matches the setup's (white for White and
     * Wallpaper). [variant] picks among the matching designs so setups don't all look alike.
     */
    fun forAccent(accent: com.example.core.theme.AccentOption, variant: Int): WallpaperItem {
        val hex = if (accent == com.example.core.theme.AccentOption.DYNAMIC) 0xFFFFFFFF.toInt() else accent.hex
        val matching = WALLPAPERS.filter { it.accentHex == hex && it.bgHex != 0xFFF2F2F2.toInt() }
        return if (matching.isEmpty()) WALLPAPERS.first() else matching[Math.floorMod(variant, matching.size)]
    }

    /**
     * Draws the design on a 1080px-wide canvas scaled to the output size, so a thumbnail is an exact
     * miniature of the applied wallpaper (sizes used to be fixed pixels, so small renders differed).
     */
    fun renderWallpaperBitmap(item: WallpaperItem, pxWidth: Int = 1080, pxHeight: Int = 2400): Bitmap {
        val bitmap = Bitmap.createBitmap(pxWidth, pxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = pxWidth / 1080f
        canvas.scale(scale, scale)
        val width = 1080
        val height = (pxHeight / scale).toInt()

        val paintBg = Paint().apply { color = item.bgHex }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        // Only the paper variant is light (pure-black OLED backgrounds used to get dark dots on black).
        val isLight = item.bgHex == 0xFFF2F2F2.toInt()
        val dotOffColor = if (isLight) Color.argb(32, 10, 10, 10) else Color.argb(40, 255, 255, 255)
        val paintDotOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotOffColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = item.accentHex }

        val spacing = 48f
        val cols = (width / spacing).toInt()
        val rows = (height / spacing).toInt()

        when (item.patternType) {
            0 -> {
                // Regular matrix with single glowing accent dot
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val cx = c * spacing + (spacing / 2f)
                        val cy = r * spacing + (spacing / 2f)
                        if (c == cols / 2 && r == rows / 3) {
                            canvas.drawCircle(cx, cy, 7.5f, paintAccent)
                        } else {
                            canvas.drawCircle(cx, cy, 2.2f, paintDotOff)
                        }
                    }
                }
            }
            1 -> {
                // Crosshairs and micro dots
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val cx = c * spacing + (spacing / 2f)
                        val cy = r * spacing + (spacing / 2f)
                        if ((r + c) % 4 == 0) {
                            canvas.drawLine(cx - 4f, cy, cx + 4f, cy, paintDotOff)
                            canvas.drawLine(cx, cy - 4f, cx, cy + 4f, paintDotOff)
                        } else {
                            canvas.drawCircle(cx, cy, 1.8f, paintDotOff)
                        }
                    }
                }
                // Center accent indicator
                canvas.drawCircle(width / 2f, height / 2.5f, 6f, paintAccent)
            }
            2 -> {
                // Circular concentric dot halos
                val cx = width / 2f
                val cy = height / 2.8f
                for (ring in 1..8) {
                    val radius = ring * 80f
                    val count = ring * 12
                    for (i in 0 until count) {
                        val angle = (i * (360f / count)) * (Math.PI / 180f)
                        val x = cx + (radius * Math.cos(angle)).toFloat()
                        val y = cy + (radius * Math.sin(angle)).toFloat()
                        if (ring == 4 && i == 0) {
                            canvas.drawCircle(x, y, 6.5f, paintAccent)
                        } else {
                            canvas.drawCircle(x, y, 2.0f, paintDotOff)
                        }
                    }
                }
            }
            3 -> {
                // Asymmetric diagonal dot field
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val cx = c * spacing + (spacing / 2f)
                        val cy = r * spacing + (spacing / 2f)
                        val distFromCenter = Math.abs(c - cols / 2) + Math.abs(r - rows / 2)
                        val rad = (1.5f + (distFromCenter % 3) * 0.8f)
                        canvas.drawCircle(cx, cy, rad, paintDotOff)
                    }
                }
                canvas.drawCircle(width * 0.75f, height * 0.35f, 6.5f, paintAccent)
            }
            4 -> {
                // Minimal technical border framing
                val margin = 80f
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = dotOffColor
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRoundRect(RectF(margin, margin, width - margin, height - margin), 40f, 40f, framePaint)
                canvas.drawCircle(width - margin - 20f, margin + 20f, 6f, paintAccent)
            }
            else -> {
                // Staggered honeycomb dots
                for (r in 0..rows) {
                    val offsetX = if (r % 2 == 1) spacing / 2f else 0f
                    for (c in 0..cols) {
                        val cx = c * spacing + offsetX
                        val cy = r * spacing + (spacing / 2f)
                        canvas.drawCircle(cx, cy, 2.0f, paintDotOff)
                    }
                }
                canvas.drawCircle(width / 2f, height * 0.4f, 6.5f, paintAccent)
            }
        }

        return bitmap
    }

    suspend fun applyWallpaper(context: Context, item: WallpaperItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderWallpaperBitmap(item, 1080, 2400)
            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bitmap)
            true
        } catch (_: Exception) {
            false
        }
    }
}
