package com.example.data.india

import com.example.core.astro.Astronomy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Indian festivals/holidays and the Hindu lunar day (tithi), computed offline. */
object IndiaCalendar {

    data class Festival(val date: String, val name: String, val approximate: Boolean) {
        fun daysAway(today: Calendar = startOfToday()): Int {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return -1
            return TimeUnit.MILLISECONDS.toDays(d.time - today.timeInMillis + 43_200_000L).toInt()
        }
    }

    /**
     * Fixed-date national holidays are exact. Lunar festivals follow the calendar and can differ by a
     * day by region, so they're marked approximate (shown with "≈").
     */
    private val FESTIVALS = listOf(
        Festival("2026-10-20", "Dussehra", true),
        Festival("2026-11-08", "Diwali", true),
        Festival("2026-11-24", "Guru Nanak Jayanti", true),
        Festival("2026-12-25", "Christmas", false),
        Festival("2027-01-01", "New Year", false),
        Festival("2027-01-14", "Makar Sankranti / Pongal", true),
        Festival("2027-01-26", "Republic Day", false),
        Festival("2027-03-10", "Eid al-Fitr", true),
        Festival("2027-03-22", "Holi", true),
        Festival("2027-03-26", "Good Friday", false),
        Festival("2027-04-14", "Ambedkar Jayanti", false),
        Festival("2027-05-17", "Eid al-Adha", true),
        Festival("2027-08-15", "Independence Day", false),
        Festival("2027-10-02", "Gandhi Jayanti", false),
        Festival("2027-10-29", "Diwali", true),
        Festival("2027-12-25", "Christmas", false)
    )

    /** Festivals from today on, nearest first. */
    fun upcoming(limit: Int): List<Festival> {
        val today = startOfToday()
        return FESTIVALS.filter { it.daysAway(today) >= 0 }.take(limit)
    }

    private val TITHI_NAMES = listOf(
        "PRATIPADA", "DWITIYA", "TRITIYA", "CHATURTHI", "PANCHAMI", "SHASHTHI", "SAPTAMI",
        "ASHTAMI", "NAVAMI", "DASHAMI", "EKADASHI", "DWADASHI", "TRAYODASHI", "CHATURDASHI"
    )

    data class Tithi(val number: Int, val name: String, val paksha: String)

    /**
     * Tithi from the moon's phase (each tithi is 12° of moon–sun separation, 1/30 of the cycle).
     * Approximate: the moon's speed varies, so near a boundary this can be a day off a printed panchang.
     */
    fun tithi(timeMs: Long = System.currentTimeMillis()): Tithi {
        val n = (Astronomy.moonPhase(timeMs).fraction * 30).toInt().coerceIn(0, 29) + 1 // 1..30
        return when {
            n == 15 -> Tithi(15, "PURNIMA", "SHUKLA")
            n == 30 -> Tithi(30, "AMAVASYA", "KRISHNA")
            n < 15 -> Tithi(n, TITHI_NAMES[n - 1], "SHUKLA")
            else -> Tithi(n, TITHI_NAMES[n - 16], "KRISHNA")
        }
    }

    private fun startOfToday(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    fun formatDate(date: String): String = runCatching {
        SimpleDateFormat("EEE dd MMM", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: Date()).uppercase()
    }.getOrDefault(date)
}
