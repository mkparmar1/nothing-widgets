package com.example.data.finance

import android.content.Context
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreign-exchange rates for the Finance/Travel families.
 *
 * Offline-honest by design: it ships a bundled fallback table so a rate is always available, then
 * opportunistically refreshes from a free API in the background and swaps in live rates when they
 * arrive. The render path never blocks — it always reads the current cached value.
 */
object FxRepository {

    /** Base currency is always USD; values are "1 USD = N <currency>". */
    private val FALLBACK = mapOf(
        "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 156.0, "INR" to 83.4,
        "AUD" to 1.52, "CAD" to 1.37, "CHF" to 0.89, "CNY" to 7.25, "AED" to 3.67,
        "SGD" to 1.35, "HKD" to 7.81, "KRW" to 1370.0, "BRL" to 5.43, "ZAR" to 18.2
    )

    @Volatile
    private var rates: Map<String, Double> = FALLBACK
    @Volatile
    private var fetchedAt: Long = 0L
    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    val currencies: List<String> get() = FALLBACK.keys.sorted()

    fun rate(currency: String): Double = rates[currency.uppercase()] ?: 0.0

    /** Convert [amount] from [from] to [to] using the current rate table. */
    fun convert(amount: Double, from: String, to: String): Double {
        val f = rate(from)
        val t = rate(to)
        if (f <= 0.0 || t <= 0.0) return amount
        return amount / f * t
    }

    fun fetchedAt(): Long = fetchedAt

    /** Never blocks; kicks a background refresh at most every hour. */
    fun refresh(context: Context) {
        val stale = System.currentTimeMillis() - fetchedAt > 60 * 60_000L
        if (!stale || !inFlight.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                fetch()?.let {
                    rates = it
                    fetchedAt = System.currentTimeMillis()
                    BitmapCache.clear()
                    WidgetRenderEngine.updateAllPlacedWidgets(appContext)
                }
            } finally {
                inFlight.set(false)
            }
        }
    }

    private fun fetch(): Map<String, Double>? = try {
        val json = JSONObject(
            httpGet("https://open.er-api.com/v6/latest/USD") ?: return null
        )
        val obj = json.optJSONObject("rates") ?: return null
        val out = HashMap<String, Double>(FALLBACK)
        for (cur in FALLBACK.keys) {
            val v = obj.optDouble(cur, -1.0)
            if (v > 0.0) out[cur] = v
        }
        out
    } catch (_: Exception) {
        null
    }

    private fun httpGet(url: String): String? {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }
}
