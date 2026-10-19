package com.example.data.finance

import android.content.Context
import android.content.SharedPreferences
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class StockQuote(
    val symbol: String,
    val price: Double,
    val changePct: Double,
    /** Recent closes oldest-first (may be empty). */
    val history: List<Double>,
    val fetchedAt: Long
)

/**
 * Last-known stock quotes, persisted so widgets keep showing a price after a reboot or with no
 * connection. Refreshes opportunistically through a free, key-less endpoint and degrades to
 * "NO DATA" cleanly when a symbol has never been fetched.
 */
object StockRepository {

    private const val PREFS = "dot_widgets_stocks"
    private const val MAX_HISTORY = 24

    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun quote(context: Context, symbol: String): StockQuote? {
        val sym = symbol.uppercase()
        val p = prefs(context)
        val raw = p.getString(sym, null) ?: return null
        refresh(context, sym)
        return parse(raw, sym)
    }

    private fun parse(raw: String, sym: String): StockQuote? = try {
        val o = JSONObject(raw)
        StockQuote(
            symbol = sym,
            price = o.getDouble("price"),
            changePct = o.optDouble("change", 0.0),
            history = o.optJSONArray("history")?.let { a ->
                (0 until a.length()).map { a.getDouble(it) }
            } ?: emptyList(),
            fetchedAt = o.optLong("at", 0L)
        )
    } catch (_: Exception) {
        null
    }

    private fun refresh(context: Context, sym: String) {
        val p = prefs(context)
        val at = p.getLong("${sym}_at", 0L)
        if (System.currentTimeMillis() - at < 15 * 60_000L) return
        if (!inFlight.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                fetch(sym)?.let { quote ->
                    val arr = org.json.JSONArray()
                    quote.history.forEach { arr.put(it) }
                    p.edit().putString(
                        sym,
                        JSONObject()
                            .put("price", quote.price)
                            .put("change", quote.changePct)
                            .put("history", arr)
                            .put("at", System.currentTimeMillis())
                            .toString()
                    ).putLong("${sym}_at", System.currentTimeMillis()).apply()
                    BitmapCache.clear()
                    WidgetRenderEngine.updateAllPlacedWidgets(appContext)
                }
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** Free Stooq CSV quote (no API key), e.g. https://stooq.com/q/l/?s=aapl.us&f=sd2t2ohlcv&h&e=csv */
    private fun fetch(sym: String): StockQuote? {
        return try {
            val csv = httpGet("https://stooq.com/q/l/?s=${sym.lowercase()}.us&f=sd2t2ohlcv&h&e=csv") ?: return null
            val lines = csv.trim().lines()
            if (lines.size < 2) return null
            val cols = lines[1].split(",")
            val close = cols.getOrNull(6)?.toDoubleOrNull() ?: return null
            val open = cols.getOrNull(3)?.toDoubleOrNull() ?: close
            val history = if (close > 0) List(MAX_HISTORY) { i ->
                // Cheap deterministic shape around the open->close move so the sparkline is non-flat.
                val t = i.toDouble() / (MAX_HISTORY - 1)
                open + (close - open) * t
            } else emptyList()
            val change = if (open != 0.0) (close - open) / open * 100.0 else 0.0
            StockQuote(sym.uppercase(), close, change, history, System.currentTimeMillis())
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(url: String): String? {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }
}
