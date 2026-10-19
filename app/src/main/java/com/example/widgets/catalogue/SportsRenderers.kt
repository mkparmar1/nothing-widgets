package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.render.ColorRule
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.sports.SportsRepository
import com.example.data.sports.TeamSnapshot
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 7 Sports & Live family. Renders the last-known followed-team snapshot when the user has set
 * one up, a bundled offline F1 calendar for the race widget, and a clean empty state otherwise.
 */
object SportsRenderers {

    private fun empty(kit: RenderKit, title: String, message: String = "SET UP A TEAM IN THE APP") {
        kit.header(title)
        kit.labelCentered(message, kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    // ---------------------------------------------------------------- live score

    fun renderLiveScore(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null) {
            empty(kit, spec.options.string("league", "LIVE SCORE")); return
        }
        kit.header("${snap.league.uppercase()} • ${if (snap.live) "LIVE" else "FT"}")
        val cy = kit.height * 0.52f
        val paintTeam = kit.textPaint(kit.greyColor, 10f, 13f)
        kit.canvas.drawText(snap.team.uppercase(), kit.pad, cy - 6f * kit.d, paintTeam)
        kit.canvas.drawText(snap.opponent.uppercase(), kit.pad, cy + 18f * kit.d, paintTeam)
        val scoreColor = if (snap.live) kit.accentColor else kit.onColor
        val paintScore = kit.textPaint(scoreColor, 16f, 24f)
        val scoreStr = "${snap.homeScore} - ${snap.awayScore}"
        kit.canvas.drawText(scoreStr, kit.width - kit.pad - paintScore.measureText(scoreStr), cy + 8f * kit.d, paintScore)
    }

    fun renderNextFixture(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null) {
            empty(kit, "NEXT FIXTURE"); return
        }
        kit.header("NEXT FIXTURE")
        val days = daysUntil(snap.kickoffMs)
        val fixture = "${snap.team.uppercase()} V ${snap.opponent.uppercase()}"
        DotMatrixRenderer.drawFittedLabel(kit.canvas, fixture, kit.pad, kit.height * 0.45f, kit.maxW, kit.textPaint(kit.onColor, 12f, 16f))
        val whenStr = if (snap.kickoffMs > 0) SimpleDateFormat("EEE d MMM • HH:mm", Locale.US).format(Date(snap.kickoffMs)).uppercase() else "TBD"
        kit.labelCentered(whenStr, kit.height * 0.62f, kit.textPaint(kit.greyColor, 10f, 13f))
        kit.labelCentered(if (days >= 0) "IN $days DAYS" else "PLAYED", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderMiniTable(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val rows = SportsRepository.table(context)
        val league = spec.options.string("league", "LEAGUE").uppercase()
        kit.header("$league TABLE")
        if (rows.isEmpty()) {
            empty(kit, "$league TABLE", "SET UP A LEAGUE IN THE APP"); return
        }
        val limit = spec.options.int("rows", if (spec.sizeBucket.name == "L") 8 else 5).coerceIn(3, 12)
        val top = kit.pad + 24f * kit.d
        val rowH = (kit.height - top - kit.pad) / rows.take(limit).size.coerceAtLeast(1)
        rows.take(limit).forEachIndexed { i, r ->
            val y = top + i * rowH + rowH * 0.72f
            val paintPos = kit.textPaint(kit.greyColor, 10f, 13f)
            kit.canvas.drawText("${i + 1}", kit.pad, y, paintPos)
            DotMatrixRenderer.drawFittedLabel(kit.canvas, r.team.uppercase(), kit.pad + 20f * kit.d, y, kit.maxW - 60f * kit.d, kit.textPaint(kit.onColor, 10f, 14f))
            val paintPts = kit.textPaint(if (i == 0) kit.accentColor else kit.onColor, 11f, 15f)
            val pts = "${r.points}"
            kit.canvas.drawText(pts, kit.width - kit.pad - paintPts.measureText(pts), y, paintPts)
        }
    }

    fun renderFormGuide(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null || snap.form.isEmpty()) {
            empty(kit, "FORM GUIDE"); return
        }
        kit.header("FORM • ${snap.team.uppercase()}")
        val form = snap.form.take(6)
        val cellW = kit.maxW / form.size.coerceAtLeast(1)
        val cy = kit.height * 0.55f
        val r = minOf(cellW * 0.32f, kit.height * 0.16f)
        form.forEachIndexed { i, result ->
            val cx = kit.pad + i * cellW + cellW / 2f
            val color = when (result) { 'W' -> kit.accentColor; 'L' -> ColorRule.WARN; else -> kit.onColor }
            // W/D/L as filled / hollow / half dots so colour isn't the only signal.
            kit.canvas.drawCircle(cx, cy, r, kit.fill(color))
            if (result == 'D') kit.canvas.drawCircle(cx, cy, r * 0.45f, kit.fill(if (kit.spec.isDark) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt()))
            val paint = kit.textPaint(kit.onColor, 9f, 12f)
            kit.canvas.drawText(result.toString(), cx - paint.measureText(result.toString()) / 2f, cy + r + 14f * kit.d, paint)
        }
    }

    fun renderMatchCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null) {
            empty(kit, "KICK-OFF"); return
        }
        kit.header("${snap.team.uppercase()} KICK-OFF")
        val days = daysUntil(snap.kickoffMs)
        kit.dotTextCentered("$days", kit.height * 0.50f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (days <= 1) setOf(0) else emptySet())
        kit.labelCentered(if (days == 0) "TODAY" else "DAY${if (days == 1) "" else "S"} TO GO", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderF1NextRace(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val series = spec.options.string("series", "F1")
        val race = SportsRepository.nextRace(context, series)
        kit.header("$series • NEXT RACE")
        DotMatrixRenderer.drawFittedLabel(kit.canvas, race.name.uppercase(), kit.pad, kit.height * 0.42f, kit.maxW, kit.textPaint(kit.onColor, 12f, 16f))
        kit.labelCentered(race.circuit.uppercase(), kit.height * 0.60f, kit.textPaint(kit.greyColor, 10f, 13f))
        kit.labelCentered("${race.date} • IN ${race.daysAway}D", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderCricketOvers(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null) {
            empty(kit, "CRICKET"); return
        }
        // Cricket reads as runs/wickets + overs; reuse the snapshot fields.
        kit.header("${snap.league.uppercase()} • ${if (snap.live) "LIVE" else "FT"}")
        kit.dotTextCentered("${snap.homeScore}/${snap.awayScore}", kit.height * 0.50f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = if (snap.live) setOf(0) else emptySet())
        val overs = spec.options.int("overs", 20)
        kit.labelCentered("${snap.team.uppercase()} V ${snap.opponent.uppercase()} • $overs OV", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderTennisSets(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val snap = SportsRepository.snapshot(context)
        if (snap == null) {
            empty(kit, "TENNIS"); return
        }
        kit.header(spec.options.string("tour", "TENNIS").uppercase())
        val setCount = spec.options.int("best_of", 3).coerceIn(3, 5)
        val cy = kit.height * 0.55f
        val cellW = kit.maxW / setCount
        for (i in 0 until setCount) {
            val cx = kit.pad + i * cellW + cellW / 2f
            val homeWon = i < snap.homeScore
            val awayWon = i < snap.awayScore
            kit.canvas.drawCircle(cx, cy - 8f * kit.d, 3.4f * kit.d, kit.fill(if (homeWon) kit.accentColor else kit.offColor))
            kit.canvas.drawCircle(cx, cy + 8f * kit.d, 3.4f * kit.d, kit.fill(if (awayWon) kit.onColor else kit.offColor))
        }
        kit.labelCentered("${snap.homeScore} - ${snap.awayScore}", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.onColor, 10f, 14f))
    }

    fun renderStandingsDots(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val rows = SportsRepository.table(context)
        val league = spec.options.string("league", "LEAGUE").uppercase()
        if (rows.isEmpty()) {
            empty(kit, "$league STANDINGS"); return
        }
        kit.header("$league STANDINGS")
        val limit = spec.options.int("rows", 6).coerceIn(3, 10)
        val maxPts = rows.maxOf { it.points }.coerceAtLeast(1)
        val top = kit.pad + 24f * kit.d
        val rowH = (kit.height - top - kit.pad) / rows.take(limit).size.coerceAtLeast(1)
        rows.take(limit).forEachIndexed { i, r ->
            val y = top + i * rowH + rowH * 0.55f
            DotMatrixRenderer.drawFittedLabel(kit.canvas, r.team.uppercase(), kit.pad, y, kit.maxW * 0.45f, kit.textPaint(kit.onColor, 9f, 12f))
            val frac = r.points.toFloat() / maxPts
            DotMatrixRenderer.drawDotRun(
                canvas = canvas, current = (frac * 10).toInt(), max = 10, totalDots = 10,
                startX = kit.pad + kit.maxW * 0.48f, startY = y - 3f * kit.d,
                dotRadius = 2.0f * kit.d, dotGap = kit.maxW * 0.05f,
                onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor, useAccentOnActive = true
            )
        }
    }

    fun renderHighlightsLink(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        kit.header("HIGHLIGHTS")
        val label = spec.options.string("label", "WATCH HIGHLIGHTS").uppercase()
        val cx = kit.width / 2f
        val cy = kit.height * 0.54f
        // A play triangle in dots.
        val r = minOf(kit.width, kit.height) * 0.18f
        val paint = kit.fill(kit.accentColor)
        for (i in 0 until 6) {
            val x = cx - r * 0.6f + i * (r * 0.24f)
            val half = r * (0.6f - i * 0.09f)
            for (j in -2..2) {
                kit.canvas.drawCircle(x, cy + j * (half / 2.5f), 2.2f * kit.d, paint)
            }
        }
        kit.labelCentered(label, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    private fun daysUntil(epochMs: Long): Int {
        if (epochMs <= 0L) return 0
        val diff = epochMs - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / 86_400_000L).toInt()
    }
}
