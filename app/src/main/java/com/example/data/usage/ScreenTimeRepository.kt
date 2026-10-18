package com.example.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.example.ui.permissions.AppPermission
import java.util.Calendar

data class ScreenTime(
    val screenOnMs: Long,
    val unlocks: Int,
    /** Screen-on time for each of the last 7 days, oldest first (today last). */
    val lastSevenDaysMs: List<Long>
)

/** Today's screen-on time from usage events (needs Usage access, Android 9+). */
object ScreenTimeRepository {

    @Volatile
    private var cached: ScreenTime? = null

    fun isAvailable(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AppPermission.UsageAccess.isGranted(context)

    /** Last value computed by [refresh], or null without access. */
    fun current(context: Context): ScreenTime? = if (isAvailable(context)) cached else null

    fun refresh(context: Context) {
        if (!isAvailable(context)) return
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val days = (6 downTo 0).map { offset ->
            val start = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }.timeInMillis
            val end = if (offset == 0) System.currentTimeMillis() else start + 86_400_000L
            measure(usm, start, end)
        }
        cached = ScreenTime(days.last().first, days.last().second, days.map { it.first })
    }

    /** Sums screen-interactive intervals and counts unlocks between [start] and [end]. */
    private fun measure(usm: UsageStatsManager, start: Long, end: Long): Pair<Long, Int> {
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        var onSince: Long? = null
        var total = 0L
        var unlocks = 0
        var sawAny = false
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    sawAny = true
                    if (onSince == null) onSince = event.timeStamp
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // If the day began with the screen on, count from the start of the window.
                    total += event.timeStamp - (onSince ?: if (!sawAny) start else event.timeStamp)
                    sawAny = true
                    onSince = null
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocks++
            }
        }
        onSince?.let { total += end - it }
        return total to unlocks
    }
}
