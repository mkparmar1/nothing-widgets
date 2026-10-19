package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dot_widgets_prefs", Context.MODE_PRIVATE)

    private val _accent = MutableStateFlow(getSavedAccent())
    val accent: StateFlow<AccentOption> = _accent.asStateFlow()

    private val _bgMode = MutableStateFlow(getSavedBgMode())
    val bgMode: StateFlow<BackgroundMode> = _bgMode.asStateFlow()

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun getSavedAccent(): AccentOption {
        val name = prefs.getString("global_accent", AccentOption.RED.name)
        return AccentOption.fromName(name)
    }

    fun setGlobalAccent(option: AccentOption) {
        prefs.edit().putString("global_accent", option.name).apply()
        _accent.value = option
    }

    fun getSavedBgMode(): BackgroundMode {
        val name = prefs.getString("global_bg_mode", BackgroundMode.SOLID.name)
        return BackgroundMode.fromName(name)
    }

    fun setGlobalBgMode(mode: BackgroundMode) {
        prefs.edit().putString("global_bg_mode", mode.name).apply()
        _bgMode.value = mode
    }

    fun getSavedThemeMode(): ThemeMode {
        val name = prefs.getString("global_theme_mode", ThemeMode.AUTO.name)
        return ThemeMode.fromName(name)
    }

    fun setGlobalThemeMode(mode: ThemeMode) {
        prefs.edit().putString("global_theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    // Water intake (glasses 0..8), reset every day
    fun getWaterCount(): Int =
        if (prefs.getString("water_date", null) == today()) prefs.getInt("water_count", 0) else 0
    fun setWaterCount(count: Int) =
        prefs.edit().putInt("water_count", count).putString("water_date", today()).apply()

    // Notes widget memo
    fun getNoteText(): String = prefs.getString("quick_note", "") ?: ""
    fun setNoteText(text: String) = prefs.edit().putString("quick_note", text).apply()

    // To-do checklist widget
    data class TodoItem(val text: String, val done: Boolean)

    fun getTodos(): List<TodoItem> = try {
        val arr = JSONArray(prefs.getString("todos", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TodoItem(o.optString("text"), o.optBoolean("done"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun setTodos(items: List<TodoItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("text", it.text).put("done", it.done)) }
        prefs.edit().putString("todos", arr.toString()).apply()
    }

    // Habit streak widget: name plus the set of days (yyyy-MM-dd) it was done
    fun getHabitName(): String = prefs.getString("habit_name", "DAILY HABIT") ?: "DAILY HABIT"
    fun setHabitName(name: String) = prefs.edit().putString("habit_name", name).apply()

    fun getHabitDays(): Set<String> = prefs.getStringSet("habit_days", emptySet()) ?: emptySet()

    /** Marks or unmarks today; returns whether today is now done. */
    fun toggleHabitToday(): Boolean {
        val days = getHabitDays().toMutableSet()
        val today = today()
        val done = if (days.remove(today)) false else days.add(today)
        prefs.edit().putStringSet("habit_days", days).apply()
        return done
    }

    // Event countdown widget
    fun getEventName(): String = prefs.getString("event_name", "NEW YEAR") ?: "NEW YEAR"
    fun setEventName(name: String) = prefs.edit().putString("event_name", name).apply()

    /** Event date as yyyy-MM-dd; defaults to the next 1 January. */
    fun getEventDate(): String = prefs.getString("event_date", null)
        ?: String.format(Locale.US, "%04d-01-01", Calendar.getInstance().get(Calendar.YEAR) + 1)
    fun setEventDate(date: String) = prefs.edit().putString("event_date", date).apply()

    // Countdown pill timer length
    fun getCountdownMinutes(): Int = prefs.getInt("countdown_minutes", 10)
    fun setCountdownMinutes(minutes: Int) = prefs.edit().putInt("countdown_minutes", minutes).apply()

    // First-run browse tip (Phase D)
    fun getBrowseTipDismissed(): Boolean = prefs.getBoolean("browse_tip_dismissed", false)
    fun setBrowseTipDismissed(dismissed: Boolean) =
        prefs.edit().putBoolean("browse_tip_dismissed", dismissed).apply()

    // Widget status card (Phase D): last time every placed widget was refreshed
    fun getLastWidgetRefresh(): Long = prefs.getLong("last_widget_refresh_ms", 0L)
    fun setLastWidgetRefresh(ms: Long) = prefs.edit().putLong("last_widget_refresh_ms", ms).apply()

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
