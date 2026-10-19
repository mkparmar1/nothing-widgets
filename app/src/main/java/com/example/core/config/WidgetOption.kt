package com.example.core.config

import org.json.JSONObject

/**
 * A single advanced option a widget can offer in the setup screen. Widgets declare these via
 * [com.example.widgets.base.WidgetDefinition.options]; the setup UI renders the matching control
 * generically, and the value round-trips through the widget's `configJson` under the `"opt"` object.
 *
 * Keeping options typed (instead of raw numbers/strings) means the renderer never parses JSON ad-hoc
 * and export/import (Phase 4.2) gets option round-tripping for free.
 */
sealed interface WidgetOption {
    /** Stable key inside the `"opt"` config object. */
    val key: String
    /** Human label shown in the ADVANCED section. */
    val label: String

    /** On/off switch. */
    data class Toggle(
        override val key: String,
        override val label: String,
        val default: Boolean = false
    ) : WidgetOption

    /** One-of-N choice, rendered as chips. [options] are (value, label) pairs. */
    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        val default: String
    ) : WidgetOption {
        companion object {
            /** Convenience when the value equals the displayed label. */
            fun of(key: String, label: String, values: List<String>, default: String): Choice =
                Choice(key, label, values.map { it to it }, default)
        }
    }

    /** Integer slider. */
    data class Slider(
        override val key: String,
        override val label: String,
        val min: Int,
        val max: Int,
        val step: Int = 1,
        val default: Int,
        /** Optional short suffix shown next to the value (e.g. "steps", "%"). */
        val unit: String = ""
    ) : WidgetOption

    /** Free-text value (e.g. a city name or entity id). */
    data class Text(
        override val key: String,
        override val label: String,
        val default: String = "",
        val hint: String = ""
    ) : WidgetOption

    /**
     * A conditional colour rule: when the value crosses [warnRatio] of its goal, the widget paints
     * with the warning treatment. Renderers apply it via [com.example.core.render.ColorRule].
     */
    data class ColorRule(
        override val key: String,
        override val label: String,
        /** Fraction 0..1 of the goal at which the warning kicks in. */
        val warnRatio: Float,
        val default: Boolean = true
    ) : WidgetOption
}

/** The set of option values currently set for one widget, resolved from `configJson`. */
class WidgetOptions(private val values: Map<String, Any>) {

    fun boolean(key: String, default: Boolean = false): Boolean = (values[key] as? Boolean) ?: default

    fun int(key: String, default: Int = 0): Int = when (val v = values[key]) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }

    fun string(key: String, default: String = ""): String = (values[key] as? String) ?: default

    companion object {
        /** Config key holding the nested option object. */
        const val KEY = "opt"

        val EMPTY = WidgetOptions(emptyMap())

        /** Reads the `"opt"` object out of a widget's configJson. */
        fun parse(configJson: String): WidgetOptions {
            val root = runCatching { JSONObject(configJson) }.getOrNull() ?: return EMPTY
            val obj = root.optJSONObject(KEY) ?: return EMPTY
            val map = HashMap<String, Any>(obj.length())
            for (key in obj.keys()) {
                map[key] = obj.get(key)
            }
            return WidgetOptions(map)
        }

        /**
         * Merges [values] into the existing `"opt"` object of [existingConfigJson], preserving any
         * unrelated keys (forward compatibility with future options).
         */
        fun write(existingConfigJson: String?, values: Map<String, Any?>): String {
            val root = runCatching { JSONObject(existingConfigJson ?: "{}") }.getOrElse { JSONObject() }
            val opt = root.optJSONObject(KEY) ?: JSONObject()
            for ((key, value) in values) {
                if (value == null) opt.remove(key) else opt.put(key, value)
            }
            root.put(KEY, opt)
            return root.toString()
        }
    }
}
