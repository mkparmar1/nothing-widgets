package com.example.widgets.catalogue

import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.os.StatFs
import com.example.core.astro.Astronomy
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.GlyphTable
import com.example.core.render.RenderKit
import com.example.data.activity.StepRepository
import com.example.data.sensor.CompassRepository
import com.example.data.battery.BatteryHelper
import com.example.widgets.base.WidgetSpec
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object UniqueSensorRenderers {

    /** Standard or summer-time abbreviation for the calendar's zone at its current instant. */
    private fun zoneAbbrev(cal: Calendar, standard: String, daylight: String): String =
        if (cal.timeZone.inDaylightTime(cal.time)) daylight else standard

    /**
     * 4×4 treatment shared by the percentage stats: hero number, centred readout, progress run.
     * The compact layouts keep the number top-left; this one fills the taller canvas.
     */
    private fun renderLargeStat(
        spec: WidgetSpec,
        canvas: Canvas,
        header: String,
        value: String,
        detail: String,
        footnote: String,
        barCurrent: Int,
        barMax: Int,
        barDots: Int,
        accentIndices: Set<Int>
    ) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header(header)
        kit.dotTextCentered(
            text = value,
            centerY = kit.height * 0.38f,
            maxHeight = kit.height * 0.30f,
            preferredGap = kit.height * 0.050f,
            accentIndices = accentIndices
        )
        kit.labelCentered(detail, kit.height * 0.58f, kit.textPaint(kit.onColor, 8f))
        kit.labelCentered(footnote, kit.height * 0.68f, kit.textPaint(kit.greyColor, 5.5f))
        DotMatrixRenderer.drawDotRun(
            canvas = kit.canvas,
            current = barCurrent,
            max = barMax,
            totalDots = barDots,
            startX = kit.pad,
            startY = kit.height * 0.82f,
            dotRadius = kit.height * 0.013f,
            dotGap = kit.maxW / (barDots - 1),
            onColor = kit.onColor,
            offColor = kit.offColor,
            accentColor = kit.accentColor,
            useAccentOnActive = true
        )
    }

    /**
     * 2. Memory & RAM Usage Matrix
     */
    fun renderRamMeter(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (spec.sizeBucket == SizeBucket.L) {
            var totalGb = 8.0f
            var usedGb = 5.4f
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager?.getMemoryInfo(memInfo)
                totalGb = (memInfo.totalMem / (1024f * 1024f * 1024f))
                usedGb = ((memInfo.totalMem - memInfo.availMem) / (1024f * 1024f * 1024f))
            } catch (_: Exception) {
            }
            val pct = ((usedGb / totalGb) * 100).toInt().coerceIn(10, 95)
            val pctStr = "$pct%"
            renderLargeStat(
                spec, canvas,
                header = "SYSTEM RAM VITALS",
                value = pctStr,
                detail = String.format(Locale.US, "%.1f / %.1f GB", usedGb, totalGb),
                footnote = "MEMORY IN USE",
                barCurrent = pct,
                barMax = 100,
                barDots = 24,
                accentIndices = setOf(pctStr.length - 1)
            )
            return
        }
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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
     * 4. Dual Capsule World Time (NYC & LON)
     */
    fun renderDualCapsule(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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
        DotMatrixRenderer.drawFittedLabel(canvas, "LON (${zoneAbbrev(calLon, "GMT", "BST")})", pad, startY - (10f * spec.density), colWidth, paintCity)

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
        DotMatrixRenderer.drawFittedLabel(canvas, "NYC (${zoneAbbrev(calNyc, "EST", "EDT")})", startX2, startY - (10f * spec.density), colWidth, paintCity2)

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

        if (spec.sizeBucket == SizeBucket.L) {
            val pctStr = "$yearPct%"
            renderLargeStat(
                spec, canvas,
                header = "YEAR $year PROGRESS",
                value = pctStr,
                detail = "DAY $dayOfYear / $totalDays",
                footnote = "$year IN MOTION",
                barCurrent = dayOfYear,
                barMax = totalDays,
                barDots = 25,
                accentIndices = setOf(pctStr.length - 1)
            )
            return
        }

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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
        if (spec.sizeBucket == SizeBucket.L) {
            var totalGb = 0
            var freeGb = 0
            try {
                val path = Environment.getDataDirectory()
                val stat = StatFs(path.path)
                val blockSize = stat.blockSizeLong
                totalGb = ((stat.blockCountLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
                freeGb = ((stat.availableBlocksLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
            } catch (_: Exception) {
            }
            val usedGb = (totalGb - freeGb).coerceAtLeast(0)
            val pct = if (totalGb > 0) ((usedGb.toFloat() / totalGb) * 100).toInt().coerceIn(0, 100) else 0
            val pctStr = "$pct%"
            renderLargeStat(
                spec, canvas,
                header = "STORAGE BREAKDOWN",
                value = pctStr,
                detail = "$usedGb / $totalGb GB USED",
                footnote = "$freeGb GB FREE",
                barCurrent = usedGb,
                barMax = totalGb.coerceAtLeast(1),
                barDots = 24,
                accentIndices = setOf(pctStr.length - 1)
            )
            return
        }
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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

        var totalGb = 0
        var freeGb = 0
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            totalGb = ((stat.blockCountLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
            freeGb = ((stat.availableBlocksLong * blockSize) / (1024L * 1024L * 1024L)).toInt()
        } catch (_: Exception) {
        }
        val usedGb = (totalGb - freeGb).coerceAtLeast(0)
        val pct = if (totalGb > 0) ((usedGb.toFloat() / totalGb) * 100).toInt().coerceIn(0, 100) else 0

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
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("LUNAR MOON MATRIX")

        val phase = Astronomy.moonPhase()

        // Dot-grid moon disc on the left; each dot is lit if it's on the sunlit side of the terminator.
        val cx = kit.width * 0.28f
        val cy = kit.height * 0.54f
        val moonRadius = minOf(kit.width * 0.20f, kit.height * 0.28f)
        val gridSize = 11
        val gap = (moonRadius * 2) / (gridSize - 1)
        val k = cos(2 * Math.PI * phase.fraction)
        val paintLit = kit.fill(kit.onColor)
        val paintDark = kit.fill(kit.offColor)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val nx = (col - (gridSize - 1) / 2.0) / ((gridSize - 1) / 2.0)
                val ny = (row - (gridSize - 1) / 2.0) / ((gridSize - 1) / 2.0)
                if (nx * nx + ny * ny > 1.0) continue
                val edge = sqrt(1 - ny * ny)
                val lit = if (phase.isWaxing) nx > edge * k else nx < -edge * k
                canvas.drawCircle(
                    cx + (nx * moonRadius).toFloat(),
                    cy + (ny * moonRadius).toFloat(),
                    if (lit) gap * 0.36f else gap * 0.28f,
                    if (lit) paintLit else paintDark
                )
            }
        }

        // Readout on right
        val rightX = kit.width * 0.54f
        val rightW = kit.width - kit.pad - rightX
        val illumPct = (phase.illumination * 100).roundToInt()
        val next = if (phase.isWaxing) {
            if (phase.daysToFull == 0) "FULL MOON TODAY" else "FULL MOON IN ${phase.daysToFull}D"
        } else {
            if (phase.daysToNew == 0) "NEW MOON TODAY" else "NEW MOON IN ${phase.daysToNew}D"
        }

        DotMatrixRenderer.drawFittedLabel(canvas, phase.name, rightX, cy - (14f * d), rightW, kit.textPaint(kit.onColor, 12f, 16f))
        DotMatrixRenderer.drawFittedLabel(canvas, "$illumPct% ILLUMINATED", rightX, cy + (6f * d), rightW, kit.textPaint(kit.accentColor, 11f, 14f))
        DotMatrixRenderer.drawFittedLabel(canvas, next, rightX, cy + (24f * d), rightW, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    /**
     * 8. Concentric Fitness Activity Rings
     */
    fun renderFitnessActivityRings(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val steps = StepRepository.todaySteps(context)
            ?: return RenderKit.message(spec, canvas, "ACTIVITY", "ALLOW ACTIVITY ACCESS IN THE APP")
        val stepGoal = 10000
        val calGoal = 600
        val calBurned = (steps * 0.045f).toInt()

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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
     * 10. Compass
     *
     * A dot ring with the cardinal marks and a needle that points to the sampled heading. Because
     * widgets redraw roughly once a minute, the needle shows the last reading rather than tracking
     * the phone live; a short hint line makes that clear.
     */
    fun renderCompass(context: Context, spec: WidgetSpec, canvas: Canvas) {
        if (!CompassRepository.hasSensor(context)) {
            RenderKit.message(spec, canvas, "COMPASS", "NO MAGNETIC SENSOR ON THIS DEVICE")
            return
        }
        val heading = CompassRepository.headingDegrees(context)
        if (heading == null) {
            RenderKit.message(spec, canvas, "COMPASS", "HOLD FLAT TO READ THE COMPASS")
            return
        }

        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val cardinal = CompassRepository.cardinal(heading)
        kit.header("COMPASS • ${cardinal}", kit.accentColor, 0.9f)

        val cx = kit.width * 0.5f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width * 0.34f, kit.height * 0.40f)

        // Fixed dial: 24-dot ring plus the cardinal marks at N/E/S/W.
        DotMatrixRenderer.drawDotRing(
            canvas = canvas,
            centerX = cx,
            centerY = cy,
            radius = radius,
            totalDots = 24,
            filledDots = 24,
            dotRadius = radius * 0.045f,
            onColor = kit.offColor,
            offColor = kit.offColor,
            accentColor = kit.accentColor
        )
        val marks = listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
        marks.forEach { (deg, label) ->
            val rad = Math.toRadians((deg - 90).toDouble())
            val mx = cx + (radius * 1.22f * cos(rad)).toFloat()
            val my = cy + (radius * 1.22f * sin(rad)).toFloat()
            val paint = kit.textPaint(if (label == "N") kit.accentColor else kit.greyColor, 9f, 13f)
            canvas.drawText(label, mx - paint.measureText(label) / 2f, my + paint.textSize * 0.35f, paint)
        }

        // Needle: a bold run of dots from the centre toward the current heading, red-tipped at the
        // north end so the direction reads even at a glance.
        val headingRad = Math.toRadians((heading.toFloat() - 90f).toDouble())
        val nx = cx + (radius * 0.72f * cos(headingRad)).toFloat()
        val ny = cy + (radius * 0.72f * sin(headingRad)).toFloat()
        val needlePaint = kit.fill(kit.onColor)
        val steps = 7
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            canvas.drawCircle(cx + (nx - cx) * t, cy + (ny - cy) * t, radius * 0.055f, needlePaint)
        }
        canvas.drawCircle(nx, ny, radius * 0.085f, kit.fill(kit.accentColor))
        canvas.drawCircle(cx, cy, radius * 0.06f, kit.fill(kit.onColor))

        // Heading readout under the dial.
        val headingStr = "${heading}° $cardinal"
        kit.labelCentered(headingStr, cy + radius * 1.55f, kit.textPaint(kit.onColor, 11f, 15f))
        if (spec.sizeBucket != SizeBucket.S) {
            kit.labelCentered("SNAPSHOT AT LAST REFRESH", cy + radius * 1.55f + 16f * d, kit.textPaint(kit.greyColor, 8f, 11f))
        }
    }

    /**
     * 9. Minimalist Dot Chronometer / Stopwatch
     */
    fun renderChronographStopwatch(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val swState = com.example.widgets.base.WidgetStateManager.getStopwatchState(context)
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accentColor

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

        val statusStr = if (swState.isRunning) "RUNNING" else "PAUSED"
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (swState.isRunning) accentColor else 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "CHRONO • $statusStr", pad, pad + 12f * spec.density, maxW, paintLabel)

        // Elapsed time display
        val totalSec = swState.elapsedMs / 1000
        val mins = (totalSec / 60) % 60
        val secs = totalSec % 60
        val tenths = (swState.elapsedMs % 1000) / 100
        val chronoStr = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)

        val maxTextW = maxW * 0.90f
        val maxTextH = spec.heightPx * 0.40f
        val gap = DotMatrixRenderer.fitDotGap(chronoStr.length, maxTextW, maxTextH, 11f * spec.density)
        val textW = DotMatrixRenderer.measureTextWidth(chronoStr.length, gap)
        val startX = (spec.widthPx - textW) / 2f
        val startY = spec.heightPx * 0.34f

        // While running, a live Chronometer drawn by the launcher shows the time (see WidgetRenderEngine).
        if (!swState.isRunning || !spec.liveOverlays) {
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
        }

        // The left half starts/pauses, the right half resets.
        val paintAction = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = (11f * spec.density).coerceAtMost(15f)
            isFakeBoldText = true
        }
        val paintReset = Paint(paintAction).apply { color = if (swState.elapsedMs > 0) onColor else offColor }
        val actionY = spec.heightPx - pad - (6f * spec.density)
        val left = if (swState.isRunning) "PAUSE" else if (swState.elapsedMs > 0) "RESUME" else "START"
        canvas.drawText(left, spec.widthPx * 0.25f - paintAction.measureText(left) / 2f, actionY, paintAction)
        canvas.drawText("RESET", spec.widthPx * 0.75f - paintReset.measureText("RESET") / 2f, actionY, paintReset)
    }

}
