package com.example.data.home

import android.content.Context
import android.content.SharedPreferences
import com.example.core.security.SecretBox
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** A cached Home Assistant entity state (last-known, refreshed opportunistically). */
data class HomeEntity(
    val entityId: String,
    val friendlyName: String,
    val state: String,
    val unit: String
) {
    val isOn: Boolean get() = state.equals("on", true) || state == "open" || state == "home"
    val numeric: Double? get() = state.toDoubleOrNull()
}

/**
 * Optional Home Assistant binding for the Smart Home family. Plain REST over [HttpURLConnection] —
 * no SDK dependency — with every entity state persisted so widgets keep rendering the last-known
 * value offline. When no server is configured, widgets show a "connect in the app" prompt.
 */
object HomeAssistantRepository {

    private const val PREFS = "dot_widgets_home"
    private const val CACHE = "entity_cache"

    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun baseUrl(context: Context): String = prefs(context).getString("base_url", "") ?: ""
    /** Stored encrypted with a Keystore key ([SecretBox]); also excluded from backups. */
    fun token(context: Context): String = SecretBox.decrypt(prefs(context).getString("token", "") ?: "")
    fun isConfigured(context: Context): Boolean = baseUrl(context).isNotBlank() && token(context).isNotBlank()

    fun setServer(context: Context, baseUrl: String, token: String) =
        prefs(context).edit().putString("base_url", baseUrl.trim().trimEnd('/')).putString("token", SecretBox.encrypt(token.trim())).apply()

    fun clearServer(context: Context) =
        prefs(context).edit().remove("base_url").remove("token").remove(CACHE).apply()

    /** Cached entity, or null if it has never been fetched. */
    fun entity(context: Context, entityId: String): HomeEntity? = cache(context)[entityId]

    fun entities(context: Context, ids: List<String>): List<HomeEntity> =
        ids.mapNotNull { cache(context)[it] }

    fun allCached(context: Context): List<HomeEntity> = cache(context).values.toList()

    /** Never blocks; refreshes the requested entities when the cache is older than 60 s. */
    fun refresh(context: Context, entityIds: List<String>) {
        if (!isConfigured(context) || entityIds.isEmpty()) return
        val p = prefs(context)
        val at = p.getLong("last_fetch", 0L)
        if (System.currentTimeMillis() - at < 60_000L) return
        if (!inFlight.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                val fetched = fetchStates(appContext, entityIds)
                if (fetched.isNotEmpty()) {
                    val merged = cache(appContext).toMutableMap()
                    fetched.forEach { merged[it.entityId] = it }
                    saveCache(appContext, merged)
                    prefs(appContext).edit().putLong("last_fetch", System.currentTimeMillis()).apply()
                    BitmapCache.clear()
                    WidgetRenderEngine.updateAllPlacedWidgets(appContext)
                }
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** Toggles a `switch`/`light` entity; returns the new state or null on failure. */
    suspend fun toggle(context: Context, entityId: String, currentOn: Boolean): Boolean? {
        if (!isConfigured(context)) return null
        val domain = entityId.substringBefore('.', "homeassistant")
        val service = if (currentOn) "turn_off" else "turn_on"
        return post(context, "/api/services/$domain/$service", JSONObject().put("entity_id", entityId))
            ?.let { !currentOn }
    }

    private fun fetchStates(context: Context, ids: List<String>): List<HomeEntity> {
        val encoded = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        val json = get(context, "/api/states?filter=$encoded")
        if (!json.isNullOrBlank()) {
            val arr = runCatching { JSONArray(json) }.getOrNull()
            if (arr != null) {
                val wanted = ids.toSet()
                val out = ArrayList<HomeEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("entity_id")
                    if (id !in wanted) continue
                    out.add(toEntity(o))
                }
                if (out.isNotEmpty()) return out
            }
        }
        // Fall back to per-entity fetches.
        return ids.mapNotNull { id ->
            runCatching { get(context, "/api/states/$id") }.getOrNull()?.let { body ->
                runCatching { toEntity(JSONObject(body)) }.getOrNull()
            }
        }
    }

    private fun toEntity(o: JSONObject): HomeEntity {
        val attrs = o.optJSONObject("attributes") ?: JSONObject()
        return HomeEntity(
            entityId = o.optString("entity_id"),
            friendlyName = attrs.optString("friendly_name", o.optString("entity_id")),
            state = o.optString("state"),
            unit = attrs.optString("unit_of_measurement", "")
        )
    }

    private fun cache(context: Context): Map<String, HomeEntity> = try {
        val arr = JSONArray(prefs(context).getString(CACHE, "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id")
            if (id.isBlank()) null else HomeEntity(id, o.optString("name"), o.optString("state"), o.optString("unit"))
        }.associateBy { it.entityId }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun saveCache(context: Context, map: Map<String, HomeEntity>) {
        val arr = JSONArray()
        map.values.forEach { e ->
            arr.put(JSONObject().put("id", e.entityId).put("name", e.friendlyName).put("state", e.state).put("unit", e.unit))
        }
        prefs(context).edit().putString(CACHE, arr.toString()).apply()
    }

    private fun get(context: Context, path: String): String? = try {
        val url = URL(baseUrl(context) + path)
        (url.openConnection() as HttpURLConnection).run {
            connectTimeout = 5000; readTimeout = 5000
            setRequestProperty("Authorization", "Bearer ${token(context)}")
            setRequestProperty("Accept", "application/json")
            try {
                if (responseCode == 200) inputStream.bufferedReader().use { it.readText() } else null
            } finally {
                disconnect()
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun post(context: Context, path: String, body: JSONObject): String? = try {
        val url = URL(baseUrl(context) + path)
        (url.openConnection() as HttpURLConnection).run {
            connectTimeout = 5000; readTimeout = 5000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${token(context)}")
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toString().toByteArray()) }
            try {
                if (responseCode in 200..299) inputStream.bufferedReader().use { it.readText() } else null
            } finally {
                disconnect()
            }
        }
    } catch (_: Exception) {
        null
    }

}
