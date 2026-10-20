package com.example.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar

data class CalendarEvent(
    val title: String,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    /** ARGB colour of the event's calendar. */
    val color: Int
)

/** Reads the user's events from the system calendar provider (needs READ_CALENDAR). */
object CalendarRepository {

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Today's events (all-day ones first), or null when calendar access isn't granted. */
    fun today(context: Context): List<CalendarEvent>? {
        if (!hasPermission(context)) return null
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return query(context, start.timeInMillis, end.timeInMillis)
            .sortedWith(compareBy<CalendarEvent>({ !it.allDay }, { it.beginMs }))
    }

    /** Events from now through the next [days] days that haven't finished, or null without access. */
    fun upcoming(context: Context, days: Int): List<CalendarEvent>? {
        if (!hasPermission(context)) return null
        val now = System.currentTimeMillis()
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
        return query(context, now - 86_400_000L, end).filter { it.endMs > now }.sortedBy { it.beginMs }
    }

    /** Events from now until the end of today that haven't finished yet. */
    fun upcomingToday(context: Context): List<CalendarEvent>? {
        val now = System.currentTimeMillis()
        return today(context)?.filter { it.allDay || it.endMs > now }
    }

    private fun query(context: Context, fromMs: Long, toMs: Long): List<CalendarEvent> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, fromMs)
            ContentUris.appendId(it, toMs)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR
        )
        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(
                uri, projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null, "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    events += CalendarEvent(
                        title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(No title)",
                        beginMs = c.getLong(1),
                        endMs = c.getLong(2),
                        allDay = c.getInt(3) == 1,
                        color = c.getInt(4)
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        return events
    }
}
