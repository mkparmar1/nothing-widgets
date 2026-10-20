package com.example.data.lists

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/** A checkable list item (shopping list). */
data class CheckItem(val id: String, val text: String, val checked: Boolean)

/** A habit with the days (yyyy-MM-dd) it was done. */
data class Habit(val id: String, val name: String, val doneDays: Set<String>) {
    fun doneToday(): Boolean = ListStore.today() in doneDays

    /** Consecutive days done, counting back from today (or yesterday if today isn't done yet). */
    fun streak(): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        if (fmt.format(cal.time) !in doneDays) cal.add(Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (fmt.format(cal.time) in doneDays) {
            n++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return n
    }
}

/** Storage for the scrollable list widgets' own data (shopping list, multi-habit list). */
object ListStore {

    private const val PREFS = "dot_widgets_lists"
    private const val KEY_SHOPPING = "shopping"
    private const val KEY_HABITS = "habits"

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun newId() = UUID.randomUUID().toString().take(8)

    // ---------- Shopping list ----------

    fun shopping(context: Context): List<CheckItem> = readArray(context, KEY_SHOPPING).map {
        CheckItem(it.optString("id"), it.optString("text"), it.optBoolean("checked"))
    }

    fun setShopping(context: Context, items: List<CheckItem>) = writeArray(context, KEY_SHOPPING, items.map {
        JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked)
    })

    fun addShopping(context: Context, text: String) =
        setShopping(context, shopping(context) + CheckItem(newId(), text.trim(), false))

    fun toggleShopping(context: Context, id: String) =
        setShopping(context, shopping(context).map { if (it.id == id) it.copy(checked = !it.checked) else it })

    // ---------- Habits ----------

    fun habits(context: Context): List<Habit> = readArray(context, KEY_HABITS).map { o ->
        val days = o.optJSONArray("days")
        Habit(o.optString("id"), o.optString("name"), (0 until (days?.length() ?: 0)).map { days!!.getString(it) }.toSet())
    }

    fun setHabits(context: Context, habits: List<Habit>) = writeArray(context, KEY_HABITS, habits.map { h ->
        JSONObject().put("id", h.id).put("name", h.name).put("days", JSONArray(h.doneDays.sorted()))
    })

    fun addHabit(context: Context, name: String) =
        setHabits(context, habits(context) + Habit(newId(), name.trim(), emptySet()))

    /** Marks or unmarks today for one habit. */
    fun toggleHabitToday(context: Context, id: String) {
        val today = today()
        setHabits(context, habits(context).map { h ->
            if (h.id != id) h else h.copy(doneDays = if (today in h.doneDays) h.doneDays - today else h.doneDays + today)
        })
    }

    // ---------- JSON helpers ----------

    private fun readArray(context: Context, key: String): List<JSONObject> = try {
        val arr = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, "[]"))
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeArray(context: Context, key: String, items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, arr.toString()).apply()
    }
}
