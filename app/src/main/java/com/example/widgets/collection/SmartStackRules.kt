package com.example.widgets.collection

import com.example.widgets.base.Category
import java.util.Calendar

enum class StackMode {
    MANUAL,
    TIME_OF_DAY,
    SMART;

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: MANUAL
    }
}

enum class StackTimeSlot(val label: String) {
    MORNING("Morning"),
    DAY("Day"),
    EVENING("Evening"),
    NIGHT("Night");

    companion object {
        fun forHour(hour: Int): StackTimeSlot = when (hour.coerceIn(0, 23)) {
            in 5..10 -> MORNING
            in 11..16 -> DAY
            in 17..21 -> EVENING
            else -> NIGHT
        }

        fun nextBoundaryMillis(nowMillis: Long): Long {
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val candidates = (0..1).flatMap { dayOffset ->
                listOf(5, 11, 17, 22).map { hour ->
                    Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                    }.timeInMillis
                }
            }
            return candidates.filter { it > now.timeInMillis }.minOrNull()
                ?: error("No future Smart Stack slot boundary")
        }
    }
}

data class StackSmartSignals(
    val minutesUntilNextEvent: Int? = null,
    val batteryLevelPercent: Int? = null,
    val isCharging: Boolean = false
)

/** Pure selection logic; runtime scheduling and automatic adapter updates are intentionally separate. */
object SmartStackRules {

    fun selectCard(
        items: List<String>,
        mode: StackMode,
        hour: Int,
        timeSlots: Map<StackTimeSlot, String>,
        categories: Map<String, Category>,
        signals: StackSmartSignals = StackSmartSignals()
    ): String? {
        if (items.isEmpty()) return null
        val fallback = timeSlots[StackTimeSlot.forHour(hour)]?.takeIf(items::contains) ?: items.first()

        return when (mode) {
            StackMode.MANUAL -> items.first()
            StackMode.TIME_OF_DAY -> fallback
            StackMode.SMART -> {
                val calendarCard = items.firstOrNull { categories[it] == Category.CALENDAR }
                val batteryCard = items.firstOrNull { categories[it] == Category.BATTERY }
                when {
                    signals.minutesUntilNextEvent?.let { it in 0..30 } == true && calendarCard != null -> calendarCard
                    (signals.batteryLevelPercent?.let { it < 20 } == true || signals.isCharging) && batteryCard != null -> batteryCard
                    else -> fallback
                }
            }
        }
    }
}
