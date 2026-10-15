package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "widget_instances")
data class WidgetInstance(
    @PrimaryKey val appWidgetId: Int,
    val widgetTypeId: String,     // Stable string ID (e.g. "clock.dotmatrix.v1")
    val schemaVersion: Int = 1,   // Forward migration version
    val configJson: String = "{}" // Dynamic JSON options
) {
    fun getAccent(defaultVal: String = "RED"): String {
        return try {
            JSONObject(configJson).optString("accent", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getBackground(defaultVal: String = "SOLID"): String {
        return try {
            JSONObject(configJson).optString("background", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getThemeMode(defaultVal: String = "AUTO"): String {
        return try {
            JSONObject(configJson).optString("theme_mode", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getCornerRadius(defaultVal: Int = 20): Int {
        return try {
            JSONObject(configJson).optInt("corner_radius", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getShowLabel(defaultVal: Boolean = true): Boolean {
        return try {
            JSONObject(configJson).optBoolean("show_label", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getTapAction(defaultVal: String = "DEFAULT"): String {
        return try {
            JSONObject(configJson).optString("tap_action", defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getString(key: String, defaultVal: String = ""): String {
        return try {
            JSONObject(configJson).optString(key, defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getBoolean(key: String, defaultVal: Boolean = false): Boolean {
        return try {
            JSONObject(configJson).optBoolean(key, defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }

    fun getInt(key: String, defaultVal: Int = 0): Int {
        return try {
            JSONObject(configJson).optInt(key, defaultVal)
        } catch (_: Exception) {
            defaultVal
        }
    }
}
