package com.example.data.market

import android.content.Context
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

data class CryptoQuote(
    val symbol: String,
    val currency: String,
    val price: Double,
    val change24hPct: Double,
    /** Hourly prices over the last 24 h, oldest first (may be empty). */
    val history: List<Double>,
    val fetchedAt: Long
)

/** Bitcoin price from CoinGecko's free API, refreshed at most every 10 minutes. */
object CryptoRepository {

    @Volatile
    private var cached: CryptoQuote? = null
    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    /** INR for phones set to Indian time, USD everywhere else. */
    private fun currency() =
        if (TimeZone.getDefault().id in setOf("Asia/Kolkata", "Asia/Calcutta")) "inr" else "usd"

    /** Never blocks; starts a background refresh when the quote is missing or older than 10 minutes. */
    fun quote(context: Context): CryptoQuote? {
        val current = cached
        val stale = current == null || current.currency != currency() ||
            System.currentTimeMillis() - current.fetchedAt > 10 * 60_000L
        if (stale && inFlight.compareAndSet(false, true)) {
            val appContext = context.applicationContext
            scope.launch {
                try {
                    fetch()?.let {
                        cached = it
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(appContext)
                    }
                } finally {
                    inFlight.set(false)
                }
            }
        }
        return current
    }

    private fun fetch(): CryptoQuote? = try {
        val cur = currency()
        val simple = JSONObject(httpGet("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=$cur&include_24hr_change=true") ?: return null)
            .getJSONObject("bitcoin")
        val history = runCatching {
            val prices = JSONObject(httpGet("https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=$cur&days=1") ?: "{}")
                .getJSONArray("prices")
            // ~288 five-minute points: keep one per hour
            (0 until prices.length() step 12).map { prices.getJSONArray(it).getDouble(1) }
        }.getOrDefault(emptyList())
        CryptoQuote("BTC", cur.uppercase(), simple.getDouble(cur), simple.optDouble("${cur}_24h_change", 0.0), history, System.currentTimeMillis())
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
