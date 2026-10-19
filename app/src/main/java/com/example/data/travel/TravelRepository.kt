package com.example.data.travel

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local store for the Phase 7 Travel family: flight/trip countdowns and a packing checklist.
 * Time-zone and currency helpers live in the clock and [com.example.data.finance.FxRepository]
 * respectively, so nothing here needs the network.
 */
class TravelRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dot_widgets_travel", Context.MODE_PRIVATE)

    // --- Flight ---
    fun flightCode(): String = prefs.getString("flight_code", "FLIGHT") ?: "FLIGHT"
    fun setFlightCode(code: String) = prefs.edit().putString("flight_code", code).apply()

    /** Flight departure epoch millis; 0 when unset. */
    fun flightTimeMs(): Long = prefs.getLong("flight_time", 0L)
    fun setFlightTimeMs(ms: Long) = prefs.edit().putLong("flight_time", ms).apply()

    // --- Trip ---
    fun tripDestination(): String = prefs.getString("trip_dest", "TRIP") ?: "TRIP"
    fun setTripDestination(dest: String) = prefs.edit().putString("trip_dest", dest).apply()

    /** Trip start date yyyy-MM-dd; defaults 14 days out. */
    fun tripDate(): String = prefs.getString("trip_date", null) ?: run {
        val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.DAY_OF_MONTH, 14)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    fun setTripDate(date: String) = prefs.edit().putString("trip_date", date).apply()

    fun daysUntilTrip(): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.parse(fmt.format(Date()))!!
        val trip = runCatching { fmt.parse(tripDate()) }.getOrNull() ?: return 0
        return ((trip.time - today.time) / 86_400_000L).toInt().coerceAtLeast(0)
    }

    // --- Packing checklist ---
    data class PackItem(val label: String, val done: Boolean)

    fun packingItems(): List<PackItem> = try {
        val arr = JSONArray(prefs.getString("packing", null) ?: DEFAULT_PACKING)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            PackItem(o.optString("label"), o.optBoolean("done"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun setPackingItems(items: List<PackItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(org.json.JSONObject().put("label", it.label).put("done", it.done)) }
        prefs.edit().putString("packing", arr.toString()).apply()
    }

    companion object {
        private const val DEFAULT_PACKING =
            """[{"label":"PASSPORT","done":false},{"label":"CHARGER","done":false},{"label":"TOOTHBRUSH","done":false},{"label":"HEADPHONES","done":false},{"label":"ADAPTER","done":false},{"label":"MEDS","done":false}]"""

        @Volatile
        private var INSTANCE: TravelRepository? = null

        fun getInstance(context: Context): TravelRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TravelRepository(context).also { INSTANCE = it }
            }
    }
}
