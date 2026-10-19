package com.example.data.learn

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local study log for the Phase 7 Learning family, plus the bundled offline word/quiz lists.
 * Nothing is fetched: word-of-the-day and the daily quiz are picked from assets so they work with
 * no connection, and study/reading/streak numbers are user-logged.
 */
class LearnRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dot_widgets_learn", Context.MODE_PRIVATE)

    // --- Study sessions today ---
    fun studySessionsToday(): Int =
        if (prefs.getString("study_date", null) == today()) prefs.getInt("study_sessions", 0) else 0

    fun logStudySession() = prefs.edit()
        .putInt("study_sessions", studySessionsToday() + 1)
        .putString("study_date", today())
        .apply()

    // --- Reading (pages today) ---
    fun readingPagesToday(): Int =
        if (prefs.getString("reading_date", null) == today()) prefs.getInt("reading_pages", 0) else 0

    fun logReadingPages(pages: Int) = prefs.edit()
        .putInt("reading_pages", (readingPagesToday() + pages).coerceAtLeast(0))
        .putString("reading_date", today())
        .apply()

    // --- Learning streak ---
    fun learningDays(): Set<String> = prefs.getStringSet("learn_days", emptySet()) ?: emptySet()

    fun markLearnedToday(): Boolean {
        val days = learningDays().toMutableSet()
        val added = days.add(today())
        prefs.edit().putStringSet("learn_days", days).apply()
        return added
    }

    fun learningStreak(): Int {
        val days = learningDays()
        val cal = Calendar.getInstance()
        var streak = 0
        while (days.contains(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    // --- Course progress ---
    fun courseName(): String = prefs.getString("course_name", "COURSE") ?: "COURSE"
    fun setCourseName(name: String) = prefs.edit().putString("course_name", name).apply()
    fun courseModulesTotal(): Int = prefs.getInt("course_total", 12)
    fun setCourseModulesTotal(n: Int) = prefs.edit().putInt("course_total", n.coerceAtLeast(1)).apply()
    fun courseModulesDone(): Int = prefs.getInt("course_done", 0)
    fun setCourseModulesDone(n: Int) = prefs.edit().putInt("course_done", n.coerceAtLeast(0)).apply()

    // --- Exam countdown ---
    fun examName(): String = prefs.getString("exam_name", "EXAM") ?: "EXAM"
    fun setExamName(name: String) = prefs.edit().putString("exam_name", name).apply()
    /** Exam date yyyy-MM-dd; defaults to 30 days out. */
    fun examDate(): String = prefs.getString("exam_date", null) ?: run {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_MONTH, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    fun setExamDate(date: String) = prefs.edit().putString("exam_date", date).apply()

    fun daysUntilExam(): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.parse(fmt.format(Date()))!!
        val exam = runCatching { fmt.parse(examDate()) }.getOrNull() ?: return 0
        return ((exam.time - today.time) / 86_400_000L).toInt().coerceAtLeast(0)
    }

    // --- Total focus time (minutes accumulated) ---
    fun focusMinutesTotal(): Int = prefs.getInt("focus_total", 0)
    fun addFocusMinutes(min: Int) = prefs.edit().putInt("focus_total", (focusMinutesTotal() + min).coerceAtLeast(0)).apply()

    // ---------------------------------------------------------------- bundled assets

    data class Word(val word: String, val meaning: String)
    data class QuizQuestion(val q: String, val options: List<String>, val answer: Int)

    /** Deterministic word of the day for the given language; same all day, changes daily. */
    fun wordOfDay(language: String = "en"): Word? {
        val list = words(language)
        if (list.isEmpty()) return null
        val idx = (dayOfYear() * 2654435761uL.toLong().toInt()) and 0x7FFFFFFF
        return list[idx % list.size]
    }

    fun words(language: String): List<Word> = try {
        val root = JSONObject(context.assets.open("words.json").bufferedReader().use { it.readText() })
        val arr = root.optJSONArray(language.lowercase()) ?: root.optJSONArray("en") ?: return emptyList()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it); Word(o.getString("word"), o.getString("meaning"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun quizCategories(): List<String> = try {
        val root = JSONObject(context.assets.open("quiz.json").bufferedReader().use { it.readText() })
        root.optJSONObject("categories")?.keys()?.asSequence()?.toList() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    /** Deterministic daily question for a category. */
    fun quizOfDay(category: String): QuizQuestion? {
        val list = quiz(category)
        if (list.isEmpty()) return null
        return list[dayOfYear() % list.size]
    }

    fun quiz(category: String): List<QuizQuestion> = try {
        val root = JSONObject(context.assets.open("quiz.json").bufferedReader().use { it.readText() })
        val arr = root.optJSONObject("categories")?.optJSONArray(category.uppercase()) ?: return emptyList()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val opts = o.getJSONArray("options")
            QuizQuestion(o.getString("q"), (0 until opts.length()).map { i -> opts.getString(i) }, o.getInt("answer"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    // ---------------------------------------------------------------- helpers

    private fun dayOfYear(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        @Volatile
        private var INSTANCE: LearnRepository? = null

        fun getInstance(context: Context): LearnRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearnRepository(context).also { INSTANCE = it }
            }
    }
}
