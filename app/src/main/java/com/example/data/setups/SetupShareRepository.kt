package com.example.data.setups

import android.content.Context
import android.util.Base64
import com.example.core.theme.AccentOption
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Shares a home-screen setup (the placed widgets + their configs + the global accent) as a short,
 * copy-pasteable code. Android does not let an app place widgets on the launcher for the user, so
 * "import" stores each widget's config so the config screen pre-fills it the next time the user adds
 * that widget type, then applies the shared accent. That keeps the round-trip honest and useful.
 */
object SetupShareRepository {

    /** Prefix + version so a future format change can be detected. */
    private const val PREFIX = "DWSET1:"

    /** Prefs key holding imported per-widget-type configs as a JSON object. */
    private const val KEY_IMPORTED = "imported_widget_configs"

    data class ExportResult(val code: String, val widgetCount: Int, val accent: AccentOption)
    data class ImportResult(val widgetCount: Int, val accent: AccentOption, val knownIds: Int)

    /** Serializes the currently placed widgets to a compact code, or null when nothing is placed. */
    suspend fun export(context: Context): ExportResult? {
        val instances = AppDatabase.getInstance(context).widgetInstanceDao().getAll()
        if (instances.isEmpty()) return null

        val arr = JSONArray()
        for (inst in instances) {
            if (WidgetRegistry.getById(inst.widgetTypeId) == null) continue
            arr.put(
                JSONObject().apply {
                    put("t", inst.widgetTypeId)
                    put("c", JSONObject(inst.configJson))
                }
            )
        }
        if (arr.length() == 0) return null

        val accent = PreferencesManager.getInstance(context).getSavedAccent()
        val root = JSONObject().apply {
            put("v", 1)
            put("accent", accent.name)
            put("widgets", arr)
        }

        val raw = root.toString().toByteArray(Charsets.UTF_8)
        val code = PREFIX + Base64.encodeToString(deflate(raw), Base64.NO_WRAP or Base64.URL_SAFE)
        return ExportResult(code, arr.length(), accent)
    }

    /** Parses a shared code, saves its configs for re-add, and applies the shared accent. */
    suspend fun import(context: Context, code: String): ImportResult {
        val trimmed = code.trim()
        require(trimmed.startsWith(PREFIX)) { "Not a Dot Widgets setup code" }
        val payload = trimmed.removePrefix(PREFIX)
        val json = try {
            String(inflate(Base64.decode(payload, Base64.NO_WRAP or Base64.URL_SAFE)), Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Setup code is corrupted", e)
        }

        val root = JSONObject(json)
        val accent = AccentOption.fromName(root.optString("accent", AccentOption.RED.name))
        val widgets = root.optJSONArray("widgets") ?: JSONArray()

        val existing = loadImported(context)
        var known = 0
        for (i in 0 until widgets.length()) {
            val obj = widgets.optJSONObject(i) ?: continue
            val typeId = obj.optString("t")
            if (typeId.isBlank() || WidgetRegistry.getById(typeId) == null) continue
            known++
            existing[typeId] = obj.optJSONObject("c") ?: JSONObject()
        }
        saveImported(context, existing)
        PreferencesManager.getInstance(context).setGlobalAccent(accent)

        return ImportResult(widgets.length(), accent, known)
    }

    /** The imported config for a widget type, if the user recently imported a matching setup. */
    fun importedConfig(context: Context, widgetTypeId: String): JSONObject? {
        val all = loadImported(context)
        return all[widgetTypeId]
    }

    fun hasImported(context: Context): Boolean = loadImported(context).isNotEmpty()

    private fun loadImported(context: Context): MutableMap<String, JSONObject> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_IMPORTED, null)
        val map = linkedMapOf<String, JSONObject>()
        if (raw.isNullOrBlank()) return map
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return map
        for (key in obj.keys()) {
            obj.optJSONObject(key)?.let { map[key] = it }
        }
        return map
    }

    private fun saveImported(context: Context, map: Map<String, JSONObject>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IMPORTED, obj.toString())
            .apply()
    }

    /** The Room DB name and prefs file are shared with the rest of the app. */
    private const val PREFS = "dot_widgets_prefs"

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(bytes)
        deflater.finish()
        val out = java.io.ByteArrayOutputStream(bytes.size)
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            out.write(buf, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val out = java.io.ByteArrayOutputStream(bytes.size * 4)
        val buf = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && inflater.needsInput()) break
            out.write(buf, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }

    /** Convenience for tests / callers that only have an [WidgetInstance] list. */
    fun encode(instances: List<WidgetInstance>, accent: AccentOption): String {
        val arr = JSONArray()
        for (inst in instances) {
            arr.put(JSONObject().apply { put("t", inst.widgetTypeId); put("c", JSONObject(inst.configJson)) })
        }
        val root = JSONObject().apply { put("v", 1); put("accent", accent.name); put("widgets", arr) }
        return PREFIX + Base64.encodeToString(deflate(root.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
