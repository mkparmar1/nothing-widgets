package com.example.data.lists

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Small state behind the multi-button widgets: tally count and daily moods. */
object TapStore {

    private const val PREFS = "dot_widgets_taps"

    fun tally(context: Context): Int = prefs(context).getInt("tally", 0)

    fun addTally(context: Context, delta: Int) =
        prefs(context).edit().putInt("tally", (tally(context) + delta).coerceIn(0, 9999)).apply()

    fun resetTally(context: Context) = prefs(context).edit().putInt("tally", 0).apply()

    /** Mood 1 (bad) .. 5 (great) per day, keyed yyyy-MM-dd. */
    fun moods(context: Context): Map<String, Int> = runCatching {
        val o = JSONObject(prefs(context).getString("moods", "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getInt(it) }
    }.getOrDefault(emptyMap())

    fun setMoodToday(context: Context, mood: Int) {
        val o = JSONObject(moods(context))
        o.put(ListStore.today(), mood.coerceIn(1, 5))
        // Keep two months of history
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -60) }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        o.keys().asSequence().toList().forEach { key -> if (key < fmt.format(cutoff.time)) o.remove(key) }
        prefs(context).edit().putString("moods", o.toString()).apply()
    }

    /** Moods for the last 7 days, oldest first; null for days without an entry. */
    fun lastWeekMoods(context: Context): List<Int?> {
        val all = moods(context)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return (6 downTo 0).map { offset ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            all[fmt.format(day.time)]
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
