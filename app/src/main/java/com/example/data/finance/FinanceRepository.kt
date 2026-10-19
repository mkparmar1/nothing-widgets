package com.example.data.finance

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local, offline-first store for the Finance widget family: the user's budget, spend, savings goal
 * and payday. Nothing here touches the network — the live crypto/FX/stock prices come from the
 * cache-backed repositories below, so the widgets stay useful even with no data connection.
 */
class FinanceRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dot_widgets_finance", Context.MODE_PRIVATE)

    // --- Daily budget / spend ---
    fun getDailyBudget(): Int = prefs.getInt("daily_budget", 2000)
    fun setDailyBudget(value: Int) = prefs.edit().putInt("daily_budget", value.coerceAtLeast(0)).apply()

    /** Spend logged for [today]; resets automatically each new day. */
    fun getSpentToday(): Int =
        if (prefs.getString("spend_date", null) == today()) prefs.getInt("spent_today", 0) else 0

    fun getBudgetCurrency(): String = prefs.getString("currency", "₹") ?: "₹"
    fun setBudgetCurrency(symbol: String) = prefs.edit().putString("currency", symbol).apply()

    // --- Savings goal ---
    fun getSavingsGoal(): Int = prefs.getInt("savings_goal", 100000)
    fun setSavingsGoal(value: Int) = prefs.edit().putInt("savings_goal", value.coerceAtLeast(1)).apply()
    fun getSavingsCurrent(): Int = prefs.getInt("savings_current", 42000)
    fun setSavingsCurrent(value: Int) = prefs.edit().putInt("savings_current", value.coerceAtLeast(0)).apply()

    // --- Payday ---
    /** Day of month (1..28) the salary lands; used for the countdown. */
    fun getPaydayDayOfMonth(): Int = prefs.getInt("payday_dom", 1)
    fun setPaydayDayOfMonth(day: Int) =
        prefs.edit().putInt("payday_dom", day.coerceIn(1, 28)).apply()

    /** Whole days until the next payday (0 when today is payday). */
    fun daysUntilPayday(): Int {
        val cal = Calendar.getInstance()
        val dom = getPaydayDayOfMonth()
        val target = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dom)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        if (!target.after(cal)) target.add(Calendar.MONTH, 1)
        val today = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val diff = target.timeInMillis - today.timeInMillis
        return (diff / 86_400_000L).toInt().coerceAtLeast(0)
    }

    // --- Portfolio holdings (symbol -> units) ---
    data class Holding(val symbol: String, val units: Double)

    fun getHoldings(): List<Holding> = try {
        val arr = JSONArray(prefs.getString("holdings", null) ?: DEFAULT_HOLDINGS)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val sym = o.optString("symbol")
            if (sym.isBlank()) null else Holding(sym.uppercase(), o.optDouble("units", 0.0))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun setHoldings(list: List<Holding>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("symbol", it.symbol).put("units", it.units)) }
        prefs.edit().putString("holdings", arr.toString()).apply()
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        /** A sensible default portfolio so the widget shows something meaningful before the user edits it. */
        private const val DEFAULT_HOLDINGS = """[{"symbol":"BTC","units":0.015},{"symbol":"ETH","units":0.4}]"""

        @Volatile
        private var INSTANCE: FinanceRepository? = null

        fun getInstance(context: Context): FinanceRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinanceRepository(context).also { INSTANCE = it }
            }
    }
}
