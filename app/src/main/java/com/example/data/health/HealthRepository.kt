package com.example.data.health

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local health store for the Phase 7 Health family. Values are user-logged in the app (and, where
 * available, the real step counter via [com.example.data.activity.StepRepository]); nothing here
 * fabricates sensor data. Widgets read the last-known value and show a clean empty state when a
 * metric has never been logged, so the family is fully offline-honest.
 */
class HealthRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dot_widgets_health", Context.MODE_PRIVATE)

    // --- Sleep (minutes slept, per night) ---
    /** Minutes slept last night; 0 until the user logs it. */
    fun lastSleepMinutes(): Int = prefs.getInt("sleep_last", 0)

    fun setLastSleepMinutes(min: Int) = prefs.edit().putInt("sleep_last", min.coerceAtLeast(0)).apply()

    /** Last 7 nights oldest-first; padded with the last known value so the chart is never blank. */
    fun sleepWeek(): List<Int> {
        val arr = loadIntArray("sleep_week")
        if (arr.size == 7) return arr
        val last = lastSleepMinutes().takeIf { it > 0 } ?: 0
        return List(7) { last }
    }

    fun setSleepWeek(values: List<Int>) = saveIntArray("sleep_week", values.takeLast(7))

    // --- Heart rate (bpm) ---
    fun restingHeartRate(): Int = prefs.getInt("hr_resting", 0)
    fun setRestingHeartRate(bpm: Int) = prefs.edit().putInt("hr_resting", bpm.coerceIn(0, 220)).apply()

    // --- Calories (active kcal) ---
    fun caloriesToday(): Int = prefs.getInt("cal_today", 0)
    fun setCaloriesToday(kcal: Int) = prefs.edit().putInt("cal_today", kcal.coerceAtLeast(0)).apply()

    // --- Weight (kg entries, oldest-first) ---
    fun weightSeries(): List<Double> = loadDoubleArray("weight_series")
    fun setWeightSeries(values: List<Double>) = saveDoubleArray("weight_series", values.takeLast(30))
    fun goalWeightKg(): Int = prefs.getInt("weight_goal", 70)
    fun setGoalWeightKg(kg: Int) = prefs.edit().putInt("weight_goal", kg.coerceIn(20, 300)).apply()
    fun weightUnit(): String = prefs.getString("weight_unit", "KG") ?: "KG"
    fun setWeightUnit(unit: String) = prefs.edit().putString("weight_unit", unit).apply()

    // --- Meditation (minutes today) ---
    fun meditationMinutes(): Int = prefs.getInt("meditation_today", 0)
    fun setMeditationMinutes(min: Int) = prefs.edit().putInt("meditation_today", min.coerceAtLeast(0)).apply()

    // --- Stand hours (hours with movement today) ---
    fun standHours(): Int = prefs.getInt("stand_hours", 0)
    fun setStandHours(hours: Int) = prefs.edit().putInt("stand_hours", hours.coerceIn(0, 24)).apply()

    // --- Workout log (yyyy-MM-dd set) ---
    fun workoutDays(): Set<String> = prefs.getStringSet("workout_days", emptySet()) ?: emptySet()

    fun toggleWorkoutToday(): Boolean {
        val days = workoutDays().toMutableSet()
        val today = today()
        val added = days.add(today)
        if (!added) days.remove(today)
        prefs.edit().putStringSet("workout_days", days).apply()
        return added
    }

    /** Consecutive days ending today on which a workout was logged. */
    fun workoutStreak(): Int {
        val days = workoutDays()
        val cal = Calendar.getInstance()
        var streak = 0
        while (true) {
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            if (days.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_MONTH, -1)
            } else break
        }
        return streak
    }

    /** How many of the last 7 days had a workout. */
    fun workoutWeek(): List<Boolean> {
        val days = workoutDays()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -6)
        return (0 until 7).map {
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            days.contains(key)
        }
    }

    // --- Body battery (derived 0..100) ---
    fun bodyBattery(): Int = prefs.getInt("body_battery", 0)
    fun setBodyBattery(level: Int) = prefs.edit().putInt("body_battery", level.coerceIn(0, 100)).apply()

    // --- helpers ---
    private fun loadIntArray(key: String): List<Int> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { arr.getInt(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveIntArray(key: String, values: List<Int>) {
        val arr = JSONArray(); values.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun loadDoubleArray(key: String): List<Double> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { arr.getDouble(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveDoubleArray(key: String, values: List<Double>) {
        val arr = JSONArray(); values.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        @Volatile
        private var INSTANCE: HealthRepository? = null

        fun getInstance(context: Context): HealthRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthRepository(context).also { INSTANCE = it }
            }
    }
}
