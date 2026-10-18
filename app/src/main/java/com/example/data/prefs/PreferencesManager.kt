package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Step counter simulation / reading
    fun getStepCount(): Int = prefs.getInt("step_count", 7420)
    fun setStepCount(steps: Int) = prefs.edit().putInt("step_count", steps).apply()

    // Water intake (glasses 0..8)
    fun getWaterCount(): Int = prefs.getInt("water_count", 5)
    fun setWaterCount(count: Int) = prefs.edit().putInt("water_count", count).apply()

    // Notes widget memo
    fun getNoteText(): String = prefs.getString("quick_note", "MEETING AT 14:30\nREVIEW DESIGN SYSTEM\nCALL ALEX") ?: ""
    fun setNoteText(text: String) = prefs.edit().putString("quick_note", text).apply()

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
