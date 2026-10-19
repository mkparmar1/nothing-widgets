package com.example.widgets.catalogue

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.widgets.base.WidgetSpec
import com.example.core.time.ClockOptions
import java.util.Calendar
import java.util.Locale

/** Alternative clock faces that each have their own look (flip cards, words, pill badge). */
object ClockStyleRenderers {

    /** Split-flap style: each digit sits on its own card with a horizontal split line. */
    fun renderFlipClock(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("FLIP CLOCK")

        val cal = Calendar.getInstance()
        if (spec.sizeBucket == SizeBucket.L) {
            renderFlipClockLarge(spec, canvas, kit, cal)
            return
        }
        val digits = String.format(Locale.US, "%02d%02d", hour(spec, cal), cal.get(Calendar.MINUTE))

        val colonW = 18f * d
        val cardGap = 6f * d
        val cardW = ((kit.maxW - colonW - cardGap * 2) / 4f)
        // Leave room for the header above and the date below.
        val headerH = kit.pad + 20f * d
        val footerH = if (spec.sizeBucket != SizeBucket.S) kit.pad + 22f * d else kit.pad
        val cardH = minOf(kit.height - headerH - footerH, cardW * 1.45f)
        val top = headerH + (kit.height - headerH - footerH - cardH) / 2f
        val radius = 8f * d
        val cardPaint = kit.fill(if (spec.isDark) 0xFF161616.toInt() else 0xFFE4E4E4.toInt())
        val splitPaint = kit.fill(if (spec.isDark) 0xFF000000.toInt() else 0xFFF2F2F2.toInt()).apply { strokeWidth = 2f * d }

        var x = kit.pad
        digits.forEachIndexed { i, ch ->
            if (i == 2) {
                // Accent colon between hours and minutes
                val cx = x + colonW / 2f - cardGap / 2f
                canvas.drawCircle(cx, top + cardH * 0.35f, 3f * d, kit.fill(kit.accentColor))
                canvas.drawCircle(cx, top + cardH * 0.65f, 3f * d, kit.fill(kit.accentColor))
                x += colonW
            }
            val rect = RectF(x, top, x + cardW, top + cardH)
            canvas.drawRoundRect(rect, radius, radius, cardPaint)

            val gap = DotMatrixRenderer.fitDotGap(1, cardW * 0.62f, cardH * 0.62f, 9f * d)
            val textW = DotMatrixRenderer.measureTextWidth(1, gap)
            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = ch.toString(),
                startX = rect.centerX() - textW / 2f,
                startY = rect.centerY() - 3 * gap,
                dotRadius = gap * 0.38f,
                dotGap = gap,
                onColor = kit.onColor,
                offColor = kit.offColor
            )
            // The "flap" split
            canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), splitPaint)
            x += cardW + if (i == 1) 0f else cardGap
        }

        if (spec.sizeBucket != SizeBucket.S) {
            dateLine(spec, cal)?.let { kit.labelCentered(it, kit.height - kit.pad, kit.textPaint(kit.greyColor, 11f, 15f)) }
        }
    }

    /** 4×4 flip clock: hours on the top pair of cards, minutes on the bottom pair. */
    private fun renderFlipClockLarge(spec: WidgetSpec, canvas: Canvas, kit: RenderKit, cal: Calendar) {
        val d = kit.d
        val rows = listOf(
            String.format(Locale.US, "%02d", hour(spec, cal)),
            String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
        )

        val top = kit.pad + 24f * d
        val bottom = kit.height - kit.pad - 20f * d
        val rowGap = 10f * d
        val cardGap = 6f * d
        val cardH = minOf((bottom - top - rowGap) / 2f, kit.maxW * 0.30f)
        val cardW = (kit.maxW - cardGap) / 2f
        val radius = 10f * d
        val cardPaint = kit.fill(if (spec.isDark) 0xFF161616.toInt() else 0xFFE4E4E4.toInt())
        val splitPaint = kit.fill(if (spec.isDark) 0xFF000000.toInt() else 0xFFF2F2F2.toInt()).apply { strokeWidth = 2f * d }
        val startTop = top + ((bottom - top) - (cardH * 2f + rowGap)) / 2f

        rows.forEachIndexed { r, pair ->
            val y = startTop + r * (cardH + rowGap)
            pair.forEachIndexed { c, ch ->
                val x = kit.pad + c * (cardW + cardGap)
                val rect = RectF(x, y, x + cardW, y + cardH)
                canvas.drawRoundRect(rect, radius, radius, cardPaint)
                val gap = DotMatrixRenderer.fitDotGap(1, cardW * 0.5f, cardH * 0.62f, 12f * d)
                val textW = DotMatrixRenderer.measureTextWidth(1, gap)
                DotMatrixRenderer.drawText(
                    canvas = canvas,
                    text = ch.toString(),
                    startX = rect.centerX() - textW / 2f,
                    startY = rect.centerY() - 3 * gap,
                    dotRadius = gap * 0.38f,
                    dotGap = gap,
                    onColor = kit.onColor,
                    offColor = kit.offColor
                )
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), splitPaint)
            }
        }

        dateLine(spec, cal)?.let { kit.labelCentered(it, kit.height - kit.pad, kit.textPaint(kit.greyColor, 7f)) }
    }

    /** Time spelled out in words, rounded to the nearest five minutes. */
    fun renderWordClock(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()

        val cal = Calendar.getInstance()
        val lines = timeInWords(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        kit.header("IT IS ${String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}")

        // All lines share one dot size so they read as a block.
        val longest = lines.maxOf { it.length }
        val bandH = kit.height * 0.70f
        val lineH = bandH / lines.size
        val gap = DotMatrixRenderer.fitDotGap(longest, kit.maxW, lineH * 0.78f, 7f * d)
        val top = kit.height * 0.24f
        lines.forEachIndexed { i, line ->
            val textW = DotMatrixRenderer.measureTextWidth(line.length, gap)
            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = line,
                startX = (kit.width - textW) / 2f,
                startY = top + i * lineH + (lineH - 6 * gap) / 2f,
                dotRadius = gap * 0.38f,
                dotGap = gap,
                onColor = if (i == lines.lastIndex) kit.accentColor else kit.onColor,
                offColor = kit.offColor
            )
        }
    }

    /** Compact pill-shaped time badge. */
    fun renderCapsulePill(spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        kit.header("TIME")

        val cal = Calendar.getInstance()
        val time = String.format(Locale.US, "%02d:%02d", hour(spec, cal), cal.get(Calendar.MINUTE))

        val pillH = kit.height * 0.34f
        val pill = RectF(kit.pad, (kit.height - pillH) / 2f, kit.width - kit.pad, (kit.height + pillH) / 2f)
        canvas.drawRoundRect(pill, pillH / 2f, pillH / 2f, kit.fill(if (spec.isDark) 0xFF161616.toInt() else 0xFFE4E4E4.toInt()))
        canvas.drawRoundRect(pill, pillH / 2f, pillH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * d
            color = kit.accentColor
        })
        kit.dotTextCentered(time, pill.centerY(), pillH * 0.55f, 6f * d, pill.width() - pillH * 0.6f, RenderKit.colonIndices(time))

        dateLine(spec, cal)?.let { kit.labelCentered(it, kit.height - kit.pad, kit.textPaint(kit.greyColor, 10f, 13f)) }
    }

    /** Hour in the widget's 12/24-hour setting. */
    private fun hour(spec: WidgetSpec, cal: Calendar): Int =
        if (ClockOptions.from(spec.configJson).time.is24h) cal.get(Calendar.HOUR_OF_DAY)
        else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }

    /** Date in the widget's chosen format, with AM/PM in front for 12-hour clocks; null when hidden. */
    private fun dateLine(spec: WidgetSpec, cal: Calendar): String? {
        val opts = ClockOptions.from(spec.configJson)
        return listOfNotNull(opts.time.amPm(cal), opts.date.format(cal)).joinToString(" • ").ifEmpty { null }
    }

    private val HOUR_WORDS = listOf("TWELVE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN")

    /** e.g. 20:06 -> ["FIVE", "PAST", "EIGHT"]; 20:58 -> ["NINE", "OCLOCK"]. */
    internal fun timeInWords(hour24: Int, minute: Int): List<String> {
        val rounded = ((minute + 2) / 5) * 5
        val baseHour = if (rounded > 30) hour24 + 1 else hour24
        val hourWord = HOUR_WORDS[baseHour % 12]
        return when (val m = rounded % 60) {
            0 -> listOf(if (rounded == 60) HOUR_WORDS[(hour24 + 1) % 12] else hourWord, "OCLOCK")
            15 -> listOf("QUARTER", "PAST", hourWord)
            30 -> listOf("HALF", "PAST", hourWord)
            45 -> listOf("QUARTER", "TO", hourWord)
            in 5..25 -> listOf(minuteWord(m), "PAST", hourWord)
            else -> listOf(minuteWord(60 - m), "TO", hourWord)
        }
    }

    private fun minuteWord(m: Int) = when (m) {
        5 -> "FIVE"
        10 -> "TEN"
        20 -> "TWENTY"
        25 -> "TWENTY FIVE"
        else -> m.toString()
    }
}
