package com.example.data.wallpapers

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class WallpaperItem(
    val id: Int,
    val name: String,
    val tag: String,
    val bgHex: Int,
    val accentHex: Int,
    val patternType: Int,
    val category: String = "DOT MATRIX",
    val customImage: Bitmap? = null,
    val customTagline: String? = null
)

object WallpaperManagerHelper {

    /** Accent palette wallpapers are generated from; matches the widget [com.example.core.theme.AccentOption] set. */
    val PALETTE = listOf(
        "MONO" to 0xFFFFFFFF.toInt(),
        "RED" to 0xFFE02B2B.toInt(),
        "CYAN" to 0xFF00C2D1.toInt(),
        "AMBER" to 0xFFF2A516.toInt(),
        "LIME" to 0xFFB8E000.toInt(),
        "VIOLET" to 0xFF8A5CF6.toInt(),
        "ROSE" to 0xFFF43F5E.toInt()
    )

    /** Supported Nothing OS pattern archetypes. */
    val PATTERN_NAMES = listOf(
        "Dot Matrix",
        "Technical Grid",
        "Radar Halo",
        "Diagonal Waves",
        "Industrial Frame",
        "Honeycomb",
        "Glyph Interface",
        "Atmosphere & Glass",
        "Circuits & PCB",
        "Clay Geometry",
        "Audio Spectrum",
        "Technical HUD"
    )

    /** Base 112 dot-matrix wallpapers (indexes 0..111). */
    private val BASE_WALLPAPERS = (0 until 112).map { index ->
        val (tag, accent) = PALETTE[index % PALETTE.size]
        val isPaper = index % 8 == 3
        val isOled = index % 5 == 4
        val bg = when {
            isPaper -> 0xFFF2F2F2.toInt()
            isOled -> 0xFF000000.toInt()
            else -> 0xFF0A0A0A.toInt()
        }
        val variant = index / PALETTE.size
        val name = if (isPaper) "PAPER $tag #${String.format(Locale.US, "%02d", variant + 1)}"
        else "DOT $tag #${String.format(Locale.US, "%02d", variant + 1)}"

        WallpaperItem(
            id = index,
            name = name,
            tag = tag,
            bgHex = bg,
            accentHex = accent,
            patternType = index % 6,
            category = "DOT MATRIX"
        )
    }

    /** Signature Nothing Phone series (indexes 112..159). */
    private val SIGNATURE_WALLPAPERS = buildList {
        var nextId = 112

        // 1. Glyph Interface Collection (Pattern 6)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "GLYPH $tag",
                    tag = tag,
                    bgHex = 0xFF000000.toInt(),
                    accentHex = accent,
                    patternType = 6,
                    category = "GLYPH"
                )
            )
        }

        // 2. Atmosphere & Glass Collection (Pattern 7)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "ATMOSPHERE $tag",
                    tag = tag,
                    bgHex = 0xFF08080A.toInt(),
                    accentHex = accent,
                    patternType = 7,
                    category = "ATMOSPHERE"
                )
            )
        }

        // 3. Circuits & PCB Teardown (Pattern 8)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "CIRCUITS $tag",
                    tag = tag,
                    bgHex = 0xFF0B0D10.toInt(),
                    accentHex = accent,
                    patternType = 8,
                    category = "CIRCUITS"
                )
            )
        }

        // 4. Clay & Bauhaus Sculptural (Pattern 9)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "CLAY $tag",
                    tag = tag,
                    bgHex = 0xFF121214.toInt(),
                    accentHex = accent,
                    patternType = 9,
                    category = "CLAY ART"
                )
            )
        }

        // 5. Audio Spectrum Waveforms (Pattern 10)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "WAVES $tag",
                    tag = tag,
                    bgHex = 0xFF000000.toInt(),
                    accentHex = accent,
                    patternType = 10,
                    category = "AUDIO"
                )
            )
        }

        // 6. Technical HUD & Specifications (Pattern 11)
        PALETTE.forEach { (tag, accent) ->
            add(
                WallpaperItem(
                    id = nextId++,
                    name = "TECH HUD $tag",
                    tag = tag,
                    bgHex = 0xFF0A0A0A.toInt(),
                    accentHex = accent,
                    patternType = 11,
                    category = "MINIMAL"
                )
            )
        }
    }

    /** Full catalogue spanning Classic Dot Matrix + Signature Nothing Phone collections. */
    val WALLPAPERS: List<WallpaperItem> = BASE_WALLPAPERS + SIGNATURE_WALLPAPERS

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
     * miniature of the applied wallpaper.
     */
    fun renderWallpaperBitmap(item: WallpaperItem, pxWidth: Int = 1080, pxHeight: Int = 2400): Bitmap {
        val bitmap = Bitmap.createBitmap(pxWidth, pxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = pxWidth / 1080f
        canvas.scale(scale, scale)
        val width = 1080
        val height = (pxHeight / scale).toInt()

        // Background
        val paintBg = Paint().apply { color = item.bgHex }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        // If a custom image was provided, draw it centered with aspect fill
        if (item.customImage != null) {
            val img = item.customImage
            val imgAspect = img.width.toFloat() / img.height.toFloat()
            val canvasAspect = width.toFloat() / height.toFloat()
            val (srcW, srcH) = if (imgAspect > canvasAspect) {
                (img.height * canvasAspect).toInt() to img.height
            } else {
                img.width to (img.width / canvasAspect).toInt()
            }
            val srcX = (img.width - srcW) / 2
            val srcY = (img.height - srcH) / 2
            val srcRect = Rect(srcX, srcY, srcX + srcW, srcY + srcH)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(img, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

            // Dark vignette overlay so widgets and text remain completely legible
            val vignette = RadialGradient(
                width / 2f, height / 2f, width * 0.95f,
                intArrayOf(Color.argb(40, 0, 0, 0), Color.argb(180, 0, 0, 0)),
                floatArrayOf(0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = vignette })
        }

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
            5 -> {
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
            6 -> {
                // Glyph Interface: authentic Nothing Phone light strips
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.accentHex
                    style = Paint.Style.STROKE
                    strokeWidth = 16f
                    strokeCap = Paint.Cap.ROUND
                    alpha = 80
                }
                val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLight) Color.DKGRAY else Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    strokeCap = Paint.Cap.ROUND
                }

                // Camera surround at top-left
                val camRect = RectF(140f, 180f, 460f, 500f)
                canvas.drawRoundRect(camRect, 140f, 140f, glowPaint)
                canvas.drawRoundRect(camRect, 140f, 140f, corePaint)

                // Top right diagonal slash
                canvas.drawLine(700f, 220f, 880f, 400f, glowPaint)
                canvas.drawLine(700f, 220f, 880f, 400f, corePaint)

                // Center wireless charging loop
                val cx = width / 2f
                val cy = height * 0.48f
                val radius = 280f
                canvas.drawCircle(cx, cy, radius, glowPaint)
                canvas.drawCircle(cx, cy, radius, corePaint)

                // Accent arc highlight on the charging loop
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.accentHex
                    style = Paint.Style.STROKE
                    strokeWidth = 9f
                    strokeCap = Paint.Cap.ROUND
                }
                val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                canvas.drawArc(arcRect, 35f, 60f, false, arcPaint)

                // Bottom exclamation mark
                val botY = height * 0.74f
                canvas.drawLine(cx, botY, cx, botY + 160f, glowPaint)
                canvas.drawLine(cx, botY, cx, botY + 160f, corePaint)
                canvas.drawCircle(cx, botY + 220f, 10f, paintAccent)
            }
            7 -> {
                // Atmosphere & Frosted Glass: ambient chromatic glow + frosted glass pill
                val glowRadius = width * 0.85f
                val glowX = width * 0.72f
                val glowY = height * 0.36f
                val r = Color.red(item.accentHex)
                val g = Color.green(item.accentHex)
                val b = Color.blue(item.accentHex)
                val radGradient = RadialGradient(
                    glowX, glowY, glowRadius,
                    intArrayOf(Color.argb(130, r, g, b), Color.argb(40, r, g, b), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(glowX, glowY, glowRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = radGradient })

                // Secondary soft ambient white haze
                val rad2 = RadialGradient(
                    width * 0.25f, height * 0.70f, width * 0.75f,
                    intArrayOf(Color.argb(45, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(width * 0.25f, height * 0.70f, width * 0.75f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = rad2 })

                // Frosted glass card in center
                val glassFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(25, 255, 255, 255) }
                val glassStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(70, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val glassCard = RectF(width * 0.16f, height * 0.38f, width * 0.84f, height * 0.54f)
                canvas.drawRoundRect(glassCard, 56f, 56f, glassFill)
                canvas.drawRoundRect(glassCard, 56f, 56f, glassStroke)

                // Tactile dot grid inside the card
                val dotCard = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
                for (dx in 0..10) {
                    for (dy in 0..4) {
                        canvas.drawCircle(glassCard.left + 50f + dx * 36f, glassCard.top + 60f + dy * 36f, 2f, dotCard)
                    }
                }
                canvas.drawCircle(glassCard.right - 44f, glassCard.top + 44f, 6f, paintAccent)
            }
            8 -> {
                // Circuits & Hardware: technical PCB blueprint
                val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLight) Color.argb(80, 0, 0, 0) else Color.argb(85, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                val padPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.accentHex
                    style = Paint.Style.FILL
                }

                // Central IC Chip
                val chip = RectF(width * 0.26f, height * 0.35f, width * 0.74f, height * 0.49f)
                canvas.drawRoundRect(chip, 20f, 20f, tracePaint)

                // Chip pin tracks & solder pads
                val pinStep = chip.width() / 10f
                for (i in 0..10) {
                    val px = chip.left + i * pinStep
                    // Top pins
                    canvas.drawLine(px, chip.top, px, chip.top - 40f, tracePaint)
                    canvas.drawCircle(px, chip.top - 46f, 4f, padPaint)
                    // Bottom pins
                    canvas.drawLine(px, chip.bottom, px, chip.bottom + 40f, tracePaint)
                    canvas.drawCircle(px, chip.bottom + 46f, 4f, padPaint)
                }

                // Main circuit bus line with 45-degree doglegs
                val busPath = Path().apply {
                    moveTo(width * 0.5f, chip.bottom + 46f)
                    lineTo(width * 0.5f, height * 0.68f)
                    lineTo(width * 0.78f, height * 0.76f)
                    lineTo(width * 0.78f, height * 0.88f)
                }
                canvas.drawPath(busPath, tracePaint)
                canvas.drawCircle(width * 0.78f, height * 0.88f, 9f, padPaint)
            }
            9 -> {
                // Clay Geometry: Nothing OS 2.5/3.0 Bauhaus sculptural forms
                val clayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLight) Color.argb(45, 0, 0, 0) else Color.argb(60, 255, 255, 255)
                    style = Paint.Style.FILL
                }
                // Rounded tall clay pillar
                val pill = RectF(width * 0.18f, height * 0.25f, width * 0.52f, height * 0.62f)
                canvas.drawRoundRect(pill, 120f, 120f, clayPaint)

                // Overlapping sculptural clay circle
                val discX = width * 0.64f
                val discY = height * 0.46f
                canvas.drawCircle(discX, discY, 170f, clayPaint)
                canvas.drawCircle(discX, discY, 60f, paintAccent)
            }
            10 -> {
                // Audio Spectrum: dot matrix acoustic waveforms
                val barCount = 17
                val barWidth = width / (barCount + 2f)
                val centerY = height * 0.44f
                for (i in 0 until barCount) {
                    val bx = (i + 1.5f) * barWidth
                    val norm = Math.sin(i.toDouble() / barCount * Math.PI).toFloat()
                    val dots = (norm * 14).toInt().coerceAtLeast(1)
                    for (d in -dots..dots) {
                        val by = centerY + d * 24f
                        val isCenterPeak = Math.abs(d) <= 1 && (i == barCount / 2 || i == barCount / 2 - 1)
                        canvas.drawCircle(bx, by, if (isCenterPeak) 6f else 3f, if (isCenterPeak) paintAccent else paintDotOff)
                    }
                }
            }
            11 -> {
                // Technical HUD & Specifications
                val margin = 72f
                val bracketLen = 64f
                val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(70, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                // Four corner precision brackets
                canvas.drawLine(margin, margin, margin + bracketLen, margin, hudPaint)
                canvas.drawLine(margin, margin, margin, margin + bracketLen, hudPaint)
                canvas.drawLine(width - margin, margin, width - margin - bracketLen, margin, hudPaint)
                canvas.drawLine(width - margin, margin, width - margin, margin + bracketLen, hudPaint)
                canvas.drawLine(margin, height - margin, margin + bracketLen, height - margin, hudPaint)
                canvas.drawLine(margin, height - margin, margin, height - margin - bracketLen, hudPaint)
                canvas.drawLine(width - margin, height - margin, width - margin - bracketLen, height - margin, hudPaint)
                canvas.drawLine(width - margin, height - margin, width - margin, height - margin - bracketLen, hudPaint)

                // Center crosshair
                val midX = width / 2f
                val midY = height / 2f
                canvas.drawLine(midX - 40f, midY, midX + 40f, midY, hudPaint)
                canvas.drawLine(midX, midY - 40f, midX, midY + 40f, hudPaint)
                canvas.drawCircle(midX, midY, 6f, paintAccent)
            }
        }

        // Custom or technical branding stamp if requested
        val tagline = item.customTagline ?: when (item.patternType) {
            6 -> "NOTHING // GLYPH_INTERFACE"
            7 -> "ATMOSPHERE // OS 3.0"
            8 -> "SPEC // PCB_TEARDOWN"
            9 -> "CLAY // MINIMAL_ART"
            10 -> "AUDIO // EQUALIZER"
            11 -> "NOTHING (R) // LAB_2400"
            else -> null
        }
        if (tagline != null && item.patternType >= 6) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color.argb(160, 20, 20, 20) else Color.argb(160, 240, 240, 240)
                textSize = 24f
                letterSpacing = 0.25f
            }
            canvas.drawText(tagline, 80f, height - 90f, textPaint)
            canvas.drawCircle(width - 90f, height - 98f, 5f, paintAccent)
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
