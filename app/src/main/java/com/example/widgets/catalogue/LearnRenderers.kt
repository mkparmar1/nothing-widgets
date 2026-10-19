package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.render.ColorRule
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.learn.LearnRepository
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetStateManager
import java.util.Locale

/**
 * Phase 7 Learning / Focus family. Pomodoro reuses the shared focus timer; word/quiz come from the
 * bundled offline lists; the rest are user-logged study metrics.
 */
object LearnRenderers {

    // ---------------------------------------------------------------- pomodoro

    fun renderPomodoroRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val presetWork = spec.options.int("work_min", 25).coerceIn(1, 90)
        // The live timer length is set in the app; show its remaining time in a dot ring.
        val state = WidgetStateManager.getFocusTimerState(context)
        kit.header(if (state.isRunning) "FOCUS • RUNNING" else "POMODORO")

        val frac = if (state.totalSeconds > 0) state.remainingSeconds.toFloat() / state.totalSeconds else 0f
        val cx = kit.width / 2f
        val cy = kit.height * 0.58f
        val radius = minOf(kit.width, kit.height) * 0.24f
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 24,
            filledDots = (frac * 24).toInt().coerceIn(0, 24), dotRadius = radius * 0.14f,
            onColor = kit.onColor, offColor = kit.offColor,
            accentColor = if (state.isRunning) kit.accentColor else kit.onColor
        )
        val mm = state.remainingSeconds / 60
        val ss = state.remainingSeconds % 60
        val str = String.format(Locale.US, "%02d:%02d", mm, ss)
        val paint = kit.textPaint(kit.onColor, 12f, 16f)
        canvas.drawText(str, cx - paint.measureText(str) / 2f, cy + paint.textSize / 3f, paint)
        kit.labelCentered("$presetWork MIN PRESET", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderStudySessions(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val done = repo.studySessionsToday()
        val target = spec.options.int("target", 3).coerceIn(1, 12)
        kit.header("STUDY SESSIONS")
        kit.dotTextCentered("$done", kit.height * 0.48f, kit.height * 0.38f, 12f * kit.d)
        kit.labelCentered("$done OF $target TODAY", kit.height * 0.80f, kit.textPaint(kit.greyColor, 10f, 13f))
        val frac = (done.toFloat() / target).coerceIn(0f, 1f)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = done, max = target, totalDots = 12,
            startX = kit.pad, startY = kit.height - kit.pad - 4f * kit.d,
            dotRadius = 2.6f * kit.d, dotGap = kit.maxW / 11f,
            onColor = kit.onColor, offColor = kit.offColor,
            accentColor = ColorRule.paint(frac, 1f, 0.99f, kit.accentColor, belowGoal = false), useAccentOnActive = true
        )
    }

    fun renderWordOfDay(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val language = spec.options.string("language", "en")
        val word = repo.wordOfDay(language)
        kit.header("WORD OF THE DAY")
        if (word == null) {
            kit.labelCentered("NO WORD LIST", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val gap = DotMatrixRenderer.fitDotGap(word.word.length, kit.maxW, kit.height * 0.28f, 9f * kit.d)
        DotMatrixRenderer.drawText(
            canvas = canvas, text = word.word, startX = kit.pad, startY = kit.height * 0.30f,
            dotRadius = gap * 0.38f, dotGap = gap, onColor = kit.onColor, offColor = kit.offColor
        )
        if (spec.options.boolean("show_meaning", true)) {
            val paint = kit.textPaint(kit.greyColor, 10f, 13f)
            var y = kit.height * 0.55f
            wrapText(word.meaning.uppercase(), kit.maxW, paint).take(2).forEach { line ->
                DotMatrixRenderer.drawFittedLabel(canvas, line, kit.pad, y, kit.maxW, paint)
                y += 14f * kit.d
            }
        }
    }

    fun renderReadingGoal(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val pages = repo.readingPagesToday()
        val goal = spec.options.int("goal", 20).coerceIn(1, 200)
        kit.header(if (spec.options.string("unit", "pages") == "mins") "READING MINUTES" else "READING PAGES")
        kit.dotTextCentered("$pages", kit.height * 0.48f, kit.height * 0.36f, 11f * kit.d)
        kit.labelCentered("$pages / $goal", kit.height * 0.80f, kit.textPaint(kit.greyColor, 10f, 13f))
        val frac = (pages.toFloat() / goal).coerceIn(0f, 1f)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = pages, max = goal, totalDots = 24,
            startX = kit.pad, startY = kit.height - kit.pad - 4f * kit.d,
            dotRadius = 2.2f * kit.d, dotGap = kit.maxW / 23f,
            onColor = kit.onColor, offColor = kit.offColor,
            accentColor = ColorRule.paint(frac, 1f, 0.99f, kit.accentColor, belowGoal = false), useAccentOnActive = true
        )
    }

    fun renderLearningStreak(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val streak = repo.learningStreak()
        val minStreak = spec.options.int("min_streak", 7).coerceIn(1, 365)
        kit.header("LEARNING STREAK")
        kit.dotTextCentered("$streak", kit.height * 0.48f, kit.height * 0.38f, 12f * kit.d,
            accentIndices = if (streak >= minStreak) setOf(0) else emptySet())
        kit.labelCentered("DAY STREAK · GOAL $minStreak", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f))
    }

    fun renderDailyQuiz(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val category = spec.options.string("category", "SCIENCE")
        val q = repo.quizOfDay(category)
        kit.header("QUIZ • $category")
        if (q == null) {
            kit.labelCentered("NO QUIZ AVAILABLE", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val paint = kit.textPaint(kit.onColor, 11f, 15f)
        var y = kit.height * 0.36f
        wrapText(q.q.uppercase(), kit.maxW, paint).take(3).forEach { line ->
            DotMatrixRenderer.drawFittedLabel(canvas, line, kit.pad, y, kit.maxW, paint)
            y += 15f * kit.d
        }
        if (spec.options.boolean("show_options", true)) {
            val paintOpt = kit.textPaint(kit.greyColor, 10f, 13f)
            var oy = kit.height * 0.72f
            q.options.take(3).forEachIndexed { i, opt ->
                DotMatrixRenderer.drawFittedLabel(canvas, "${'A' + i}) ${opt.uppercase()}", kit.pad, oy, kit.maxW, paintOpt)
                oy += 13f * kit.d
            }
        }
    }

    fun renderFocusTotal(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val unit = spec.options.string("period", "today").lowercase()
        // Live timer's accumulated total from the app, shown in minutes.
        val total = repo.focusMinutesTotal()
        kit.header("FOCUS TIME")
        val h = total / 60
        val m = total % 60
        kit.dotTextCentered(String.format(Locale.US, "%dh%02d", h, m), kit.height * 0.50f, kit.height * 0.32f, 10f * kit.d)
        kit.labelCentered(unit.uppercase(), kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderCourseProgress(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val name = spec.options.string("course", repo.courseName()).uppercase()
        val total = spec.options.int("modules", repo.courseModulesTotal()).coerceAtLeast(1)
        val done = repo.courseModulesDone().coerceIn(0, total)
        kit.header(name)
        val frac = done.toFloat() / total
        kit.dotTextCentered("${(frac * 100).toInt()}%", kit.height * 0.48f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(3))
        kit.labelCentered("$done / $total MODULES", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = done, max = total, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 20f * kit.d,
            dotRadius = 2.2f * kit.d, dotGap = kit.maxW / 19f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderLastNote(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val note = PreferencesManager.getInstance(context).getNoteText()
        val showTitle = spec.options.boolean("show_title", true)
        if (showTitle) kit.header("LAST NOTE")
        if (note.isBlank()) {
            kit.labelCentered("NO NOTES YET", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val paint = kit.textPaint(kit.onColor, 11f, 15f)
        var y = kit.height * (if (showTitle) 0.36f else 0.28f)
        wrapText(note.uppercase(), kit.maxW, paint).take(4).forEach { line ->
            DotMatrixRenderer.drawFittedLabel(canvas, line, kit.pad, y, kit.maxW, paint)
            y += 15f * kit.d
        }
    }

    fun renderExamCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = LearnRepository.getInstance(context)
        val days = repo.daysUntilExam()
        val name = spec.options.string("exam", repo.examName()).uppercase()
        kit.header(name)
        kit.dotTextCentered("$days", kit.height * 0.48f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (days <= 3) setOf(0) else emptySet())
        kit.labelCentered(if (days == 0) "EXAM TODAY" else "DAY${if (days == 1) "" else "S"} LEFT", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    // ---------------------------------------------------------------- helpers

    private fun wrapText(text: String, maxWidth: Float, paint: android.graphics.Paint): List<String> {
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
