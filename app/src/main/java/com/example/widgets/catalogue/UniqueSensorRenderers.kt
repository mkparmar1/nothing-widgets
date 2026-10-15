package com.example.widgets.catalogue

import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.os.StatFs
import com.example.core.render.DotMatrixRenderer
import com.example.data.battery.BatteryHelper
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

object UniqueSensorRenderers {

    /**
     * 1. Compass & Digital Heading Matrix
     */
    fun renderCompassHeading(spec: WidgetSpec, canvas: Canvas, headingDeg: Int = 42) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "DIGITAL COMPASS", pad, pad + 12f * spec.density, maxW, paintLabel)

        val cx = spec.widthPx * 0.32f
        val cy = spec.heightPx * 0.54f
        val radius = minOf(spec.widthPx * 0.24f, spec.heightPx * 0.30f)

        // 16-dot compass dial
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            totalDots = 16,
            filledDots = 16,
            dotRadius = radius * 0.10f,
            onColor = offColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Cardinal North point in accent
        val northAngle = Math.toRadians((headingDeg - 90.0))
        val nx = cx + (radius * cos(northAngle)).toFloat()
        val ny = cy + (radius * sin(northAngle)).toFloat()
        val paintNorth = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawCircle(nx, ny, radius * 0.15f, paintNorth)

        // Center pivot
        canvas.drawCircle(cx, cy, radius * 0.12f, paintNorth)

        // Heading Text on right side
        val headingStr = String.format(Locale.US, "%03d°", headingDeg)
        val textStartX = spec.widthPx * 0.58f
        val maxTextW = spec.widthPx - pad - textStartX
        val textGap = DotMatrixRenderer.fitDotGap(headingStr.length, maxTextW, spec.heightPx * 0.38f, 7.5f * spec.density)
        val textStartY = cy - (3.5f * textGap)

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = headingStr,
            startX = textStartX,
            startY = textStartY,
            dotRadius = textGap * 0.38f,
            dotGap = textGap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(headingStr.length - 1),
            accentColor = accentColor
        )

        val paintDir = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val dirY = (textStartY + (7 * textGap) + (16f * spec.density)).coerceAtMost(spec.heightPx - pad - 6f)
        DotMatrixRenderer.drawFittedLabel(canvas, "NE • NORTH EAST", textStartX, dirY, maxTextW, paintDir)
    }

    /**
     * 2. Memory & RAM Usage Matrix
     */
    fun renderRamMeter(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        var totalRamGb = 8.0f
        var usedRamGb = 5.4f
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            totalRamGb = (memInfo.totalMem / (1024f * 1024f * 1024f))
            usedRamGb = ((memInfo.totalMem - memInfo.availMem) / (1024f * 1024f * 1024f))
        } catch (_: Exception) {
        }

        val pct = ((usedRamGb / totalRamGb) * 100).toInt().coerceIn(10, 95)

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "SYSTEM RAM VITALS", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Percentage in dot matrix
        val pctStr = "$pct%"
        val maxPctW = maxW * 0.45f
        val maxPctH = spec.heightPx * 0.40f
        val gap = DotMatrixRenderer.fitDotGap(pctStr.length, maxPctW, maxPctH, 9.5f * spec.density)
        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = pctStr,
            startX = startX,
            startY = startY,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(pctStr.length - 1),
            accentColor = accentColor
        )

        // Detail readout on the right
        val paintMem = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val detailStr = String.format(Locale.US, "%.1f / %.1f GB", usedRamGb, totalRamGb)
        val rightStartX = (startX + DotMatrixRenderer.measureTextWidth(pctStr.length, gap) + 16f * spec.density).coerceAtLeast(spec.widthPx * 0.52f)
        DotMatrixRenderer.drawFittedLabel(canvas, detailStr, rightStartX, startY + (3.5f * gap), spec.widthPx - pad - rightStartX, paintMem)

        // 20-dot progress run across bottom
        val totalDots = 20
        val segGap = maxW / (totalDots - 1)
        val barY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = pct,
            max = 100,
            totalDots = totalDots,
            startX = pad,
            startY = barY,
            dotRadius = 2.8f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

    /**
     * 3. Audio Visualizer Frequency Bars
     */
    fun renderVisualizer(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "SPECTRUM EQUALIZER", pad, pad + 12f * spec.density, maxW, paintLabel)

        // 8 frequency columns of 7 dots each
        val cols = 8
        val rows = 7
        val levels = intArrayOf(3, 5, 7, 6, 4, 7, 5, 2)

        val colGap = maxW / (cols - 1)
        val maxH = spec.heightPx - pad - (50f * spec.density)
        val rowGap = (maxH / rows).coerceIn(10f, 20f)
        val startY = spec.heightPx - pad - (12f * spec.density)

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintPeak = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        for (c in 0 until cols) {
            val cx = pad + (c * colGap)
            val currentLevel = levels[c]

            for (r in 0 until rows) {
                val cy = startY - (r * rowGap)
                val isFilled = r < currentLevel
                val isPeak = (r == currentLevel - 1)

                val paint = when {
                    isPeak -> paintPeak
                    isFilled -> paintOn
                    else -> paintOff
                }
                canvas.drawCircle(cx, cy, 3.2f * spec.density, paint)
            }
        }
    }

    /**
     * 4. Dual Capsule World Time (NYC & LON)
     */
    fun renderDualCapsule(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val calLon = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"))
        val lonTime = String.format(Locale.US, "%02d:%02d", calLon.get(Calendar.HOUR_OF_DAY), calLon.get(Calendar.MINUTE))

        val calNyc = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
        val nycTime = String.format(Locale.US, "%02d:%02d", calNyc.get(Calendar.HOUR_OF_DAY), calNyc.get(Calendar.MINUTE))

        val colWidth = (maxW - (16f * spec.density)) / 2f
        val startY = spec.heightPx * 0.38f

        // Column 1: London
        val dotGap1 = DotMatrixRenderer.fitDotGap(lonTime.length, colWidth * 0.90f, spec.heightPx * 0.40f, 8.5f * spec.density)
        val paintCity = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "LON (GMT)", pad, startY - (10f * spec.density), colWidth, paintCity)

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = lonTime,
            startX = pad,
            startY = startY,
            dotRadius = dotGap1 * 0.38f,
            dotGap = dotGap1,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(2),
            accentColor = accentColor
        )

        // Column 2: New York
        val startX2 = pad + colWidth + (16f * spec.density)
        val dotGap2 = DotMatrixRenderer.fitDotGap(nycTime.length, colWidth * 0.90f, spec.heightPx * 0.40f, 8.5f * spec.density)
        val paintCity2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "NYC (EST)", startX2, startY - (10f * spec.density), colWidth, paintCity2)

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = nycTime,
            startX = startX2,
            startY = startY,
            dotRadius = dotGap2 * 0.38f,
            dotGap = dotGap2,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(2),
            accentColor = accentColor
        )
    }

    /**
     * 5. Year Progress Percentage
     */
    fun renderYearProgress(spec: WidgetSpec, canvas: Canvas) {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val totalDays = if (cal.getActualMaximum(Calendar.DAY_OF_YEAR) == 366) 366 else 365
        val yearPct = ((dayOfYear.toFloat() / totalDays) * 100).toInt()
        val year = cal.get(Calendar.YEAR)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "YEAR $year PROGRESS", pad, pad + 12f * spec.density, maxW, paintLabel)

        val pctStr = "$yearPct%"
        val maxPctW = maxW * 0.45f
        val gap = DotMatrixRenderer.fitDotGap(pctStr.length, maxPctW, spec.heightPx * 0.40f, 9.5f * spec.density)
        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = pctStr,
            startX = startX,
            startY = startY,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(pctStr.length - 1),
            accentColor = accentColor
        )

        val paintDay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val dayInfo = "DAY $dayOfYear / $totalDays"
        val rightX = (startX + DotMatrixRenderer.measureTextWidth(pctStr.length, gap) + 16f * spec.density).coerceAtLeast(spec.widthPx * 0.52f)
        DotMatrixRenderer.drawFittedLabel(canvas, dayInfo, rightX, startY + (3.5f * gap), spec.widthPx - pad - rightX, paintDay)

        // 25-dot annual progress run
        val totalDots = 25
        val segGap = maxW / (totalDots - 1)
        val barY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = dayOfYear,
            max = totalDays,
            totalDots = totalDots,
            startX = pad,
            startY = barY,
            dotRadius = 2.6f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

    /**
     * 6. Storage Capacity Breakdown
     */
    fun renderStorageBar(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        var totalGb = 256
        var freeGb = 118
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            totalGb = ((stat.blockCountLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
            freeGb = ((stat.availableBlocksLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
        } catch (_: Exception) {
        }
        val usedGb = (totalGb - freeGb).coerceAtLeast(1)
        val pct = ((usedGb.toFloat() / totalGb) * 100).toInt()

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "STORAGE BREAKDOWN", pad, pad + 12f * spec.density, maxW, paintLabel)

        val pctStr = "$pct%"
        val maxPctW = maxW * 0.45f
        val gap = DotMatrixRenderer.fitDotGap(pctStr.length, maxPctW, spec.heightPx * 0.40f, 9.5f * spec.density)
        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = pctStr,
            startX = startX,
            startY = startY,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(pctStr.length - 1),
            accentColor = accentColor
        )

        val paintFree = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val freeStr = "${freeGb} GB FREE"
        val rightX = (startX + DotMatrixRenderer.measureTextWidth(pctStr.length, gap) + 16f * spec.density).coerceAtLeast(spec.widthPx * 0.52f)
        DotMatrixRenderer.drawFittedLabel(canvas, freeStr, rightX, startY + (3.5f * gap), spec.widthPx - pad - rightX, paintFree)

        val totalDots = 20
        val segGap = maxW / (totalDots - 1)
        val barY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = usedGb,
            max = totalGb,
            totalDots = totalDots,
            startX = pad,
            startY = barY,
            dotRadius = 2.8f * spec.density,
            dotGap = segGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

    /**
     * 7. Lunar Moon Phase Matrix
     */
    fun renderLunarMoonPhase(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "LUNAR MOON MATRIX", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Draw 9x9 dot matrix moon on left
        val cx = spec.widthPx * 0.28f
        val cy = spec.heightPx * 0.54f
        val moonRadius = minOf(spec.widthPx * 0.20f, spec.heightPx * 0.28f)

        // Moon disc
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = moonRadius,
            totalDots = 20,
            filledDots = 17, // Waxing Gibbous
            dotRadius = moonRadius * 0.12f,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor
        )
        // Center moon body
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = moonRadius * 0.55f,
            totalDots = 10,
            filledDots = 8,
            dotRadius = moonRadius * 0.14f,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Readout on right
        val rightX = spec.widthPx * 0.54f
        val rightW = spec.widthPx - pad - rightX

        val paintPhase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (12f * spec.density).coerceAtMost(16f)
            isFakeBoldText = true
        }
        val paintIllum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        val paintNext = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(13f)
            isFakeBoldText = true
        }

        DotMatrixRenderer.drawFittedLabel(canvas, "WAXING GIBBOUS", rightX, cy - (14f * spec.density), rightW, paintPhase)
        DotMatrixRenderer.drawFittedLabel(canvas, "88% ILLUMINATED", rightX, cy + (6f * spec.density), rightW, paintIllum)
        DotMatrixRenderer.drawFittedLabel(canvas, "FULL MOON IN 3D", rightX, cy + (24f * spec.density), rightW, paintNext)
    }

    /**
     * 8. Concentric Fitness Activity Rings
     */
    fun renderFitnessActivityRings(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val steps = PreferencesManager.getInstance(context).getStepCount()
        val stepGoal = 10000
        val calGoal = 600
        val calBurned = (steps * 0.045f).toInt()

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "CONCENTRIC ACTIVITY", pad, pad + 12f * spec.density, maxW, paintLabel)

        val cx = spec.widthPx * 0.30f
        val cy = spec.heightPx * 0.54f
        val outerRadius = minOf(spec.widthPx * 0.22f, spec.heightPx * 0.30f)
        val innerRadius = outerRadius * 0.62f

        // Outer ring: Steps
        val totalOuter = 24
        val filledOuter = ((steps.toFloat() / stepGoal) * totalOuter).toInt().coerceIn(0, totalOuter)
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = outerRadius,
            totalDots = totalOuter,
            filledDots = filledOuter,
            dotRadius = outerRadius * 0.08f,
            onColor = accentColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Inner ring: Calories
        val totalInner = 16
        val filledInner = ((calBurned.toFloat() / calGoal) * totalInner).toInt().coerceIn(0, totalInner)
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = innerRadius,
            totalDots = totalInner,
            filledDots = filledInner,
            dotRadius = innerRadius * 0.12f,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Stats on the right
        val rightX = spec.widthPx * 0.56f
        val rightW = spec.widthPx - pad - rightX

        val paintSteps = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (12f * spec.density).coerceAtMost(16f)
            isFakeBoldText = true
        }
        val paintCals = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(13f)
            isFakeBoldText = true
        }

        DotMatrixRenderer.drawFittedLabel(canvas, "$steps STEPS", rightX, cy - (16f * spec.density), rightW, paintSteps)
        DotMatrixRenderer.drawFittedLabel(canvas, "$calBurned KCAL", rightX, cy + (4f * spec.density), rightW, paintCals)
        DotMatrixRenderer.drawFittedLabel(canvas, "RINGS: ${((filledOuter + filledInner) / 40f * 100).toInt()}%", rightX, cy + (22f * spec.density), rightW, paintSub)
    }

    /**
     * 9. Minimalist Dot Chronometer / Stopwatch
     */
    fun renderChronographStopwatch(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "DOT CHRONOGRAPH", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Elapsed time display
        val chronoStr = "04:28.6"
        val maxTextW = maxW * 0.90f
        val maxTextH = spec.heightPx * 0.40f
        val gap = DotMatrixRenderer.fitDotGap(chronoStr.length, maxTextW, maxTextH, 11f * spec.density)
        val textW = DotMatrixRenderer.measureTextWidth(chronoStr.length, gap)
        val startX = (spec.widthPx - textW) / 2f
        val startY = spec.heightPx * 0.34f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = chronoStr,
            startX = startX,
            startY = startY,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = onColor,
            offColor = offColor,
            accentIndices = setOf(2, 5),
            accentColor = accentColor
        )

        // Lap readout below
        val paintLap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val lapStr = "LAP 03 • SPLIT +0.4s • RUNNING"
        val lapY = spec.heightPx - pad - (10f * spec.density)
        DotMatrixRenderer.drawFittedLabel(canvas, lapStr, pad, lapY, maxW, paintLap)
    }

    /**
     * 10. Nothing Glyph Light Interface
     */
    fun renderGlyphMatrix(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "GLYPH LIGHT MATRIX", pad, pad + 12f * spec.density, maxW, paintLabel)

        val cx = spec.widthPx / 2f
        val cy = spec.heightPx * 0.54f
        val radius = minOf(spec.widthPx * 0.28f, spec.heightPx * 0.32f)

        // Center Ring glyph
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius * 0.75f,
            totalDots = 20,
            filledDots = 20,
            dotRadius = radius * 0.08f,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor
        )

        // Diagonal top-right strip
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawCircle(cx + radius * 0.95f, cy - radius * 0.70f, 4.5f * spec.density, paintAccent)
        canvas.drawCircle(cx + radius * 1.15f, cy - radius * 0.85f, 4.5f * spec.density, paintAccent)

        // Bottom exclamation line
        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        canvas.drawLine(cx, cy + radius * 0.90f, cx, cy + radius * 1.25f, paintOn.apply { strokeWidth = 3f * spec.density })
        canvas.drawCircle(cx, cy + radius * 1.40f, 3.5f * spec.density, paintAccent)

        // Active state badge
        val paintState = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (10f * spec.density).coerceAtMost(13f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "GLYPH ACTIVE • 100%", pad, spec.heightPx - pad - (6f * spec.density), maxW, paintState)
    }

    /**
     * 11. Crypto / Stock Ticker Matrix
     */
    fun renderCryptoTicker(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "BTC / USD • 24H TICKER", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Price in dot matrix
        val priceStr = "68,420"
        val maxPriceW = maxW * 0.60f
        val gap = DotMatrixRenderer.fitDotGap(priceStr.length, maxPriceW, spec.heightPx * 0.38f, 10f * spec.density)
        val startX = pad
        val startY = spec.heightPx * 0.32f

        DotMatrixRenderer.drawText(
            canvas = canvas,
            text = priceStr,
            startX = startX,
            startY = startY,
            dotRadius = gap * 0.38f,
            dotGap = gap,
            onColor = onColor,
            offColor = offColor
        )

        // Change badge on the right
        val paintChange = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (12f * spec.density).coerceAtMost(16f)
            isFakeBoldText = true
        }
        val changeStartX = (startX + DotMatrixRenderer.measureTextWidth(priceStr.length, gap) + 14f * spec.density).coerceAtLeast(spec.widthPx * 0.58f)
        DotMatrixRenderer.drawFittedLabel(canvas, "+3.8% ▲", changeStartX, startY + (3.5f * gap), spec.widthPx - pad - changeStartX, paintChange)

        // 12-dot trend sparkline run across bottom
        val sparkDots = 16
        val sparkGap = maxW / (sparkDots - 1)
        val sparkY = spec.heightPx - pad - (10f * spec.density)

        DotMatrixRenderer.drawDotRun(
            canvas = canvas,
            current = 13,
            max = 16,
            totalDots = sparkDots,
            startX = pad,
            startY = sparkY,
            dotRadius = 2.6f * spec.density,
            dotGap = sparkGap,
            onColor = onColor,
            offColor = offColor,
            accentColor = accentColor,
            useAccentOnActive = true
        )
    }

    /**
     * 12. 4-up Nothing Ecosystem Battery Dock
     */
    fun renderDeviceEcosystemQuad(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val status = BatteryHelper.getBatteryStatus(context)
        val phoneLevel = status.level

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.06f
        val usableW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val devices = listOf(
            Triple("PHONE", phoneLevel, status.isCharging),
            Triple("EAR(2)", 95, false),
            Triple("WATCH", 74, false),
            Triple("CMF", 60, false)
        )

        val slotWidth = usableW / 4f
        val cy = spec.heightPx * 0.50f
        val radius = minOf(slotWidth * 0.36f, spec.heightPx * 0.22f)

        devices.forEachIndexed { i, (name, pct, charging) ->
            val cx = pad + (i * slotWidth) + (slotWidth / 2f)

            // Mini ring
            val filled = ((pct / 100f) * 16).toInt()
            DotMatrixRenderer.drawDotRing(
                canvas = canvas,
                centerX = cx,
                centerY = cy - 3f * spec.density,
                radius = radius,
                totalDots = 16,
                filledDots = filled,
                dotRadius = radius * 0.12f,
                onColor = onColor,
                offColor = offColor,
                accentColor = if (charging) accentColor else onColor
            )

            // Percentage in center
            val paintPct = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (charging) accentColor else onColor
                textSize = (radius * 0.44f).coerceIn(8f, 12f)
                isFakeBoldText = true
            }
            val pctLabel = "$pct%"
            val pw = paintPct.measureText(pctLabel)
            canvas.drawText(pctLabel, cx - (pw / 2f), cy, paintPct)

            // Device Name
            val paintName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (8.5f * spec.density).coerceAtMost(11f)
                isFakeBoldText = true
            }
            val nw = paintName.measureText(name)
            canvas.drawText(name, cx - (nw / 2f), cy + radius + 12f * spec.density, paintName)
        }

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "ECOSYSTEM DOCK (4-UP)", pad, pad + 12f * spec.density, usableW * 0.70f, paintLabel)
    }
}
