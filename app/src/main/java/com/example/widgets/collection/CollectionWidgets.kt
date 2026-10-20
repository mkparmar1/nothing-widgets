package com.example.widgets.collection

import android.content.Context
import android.graphics.Canvas
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.calendar.CalendarRepository
import com.example.data.contacts.ContactRepository
import com.example.data.lists.ListStore
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One row of a scrollable list widget.
 * @param checked null when the row has no check mark (agenda, contacts)
 * @param action what tapping the row does, sent to the receiver with [itemId]
 */
data class ListRow(
    val itemId: String,
    val title: String,
    val subtitle: String? = null,
    val checked: Boolean? = null,
    val highlight: Boolean = false,
    val action: RowAction = RowAction.NONE
)

enum class RowAction { TOGGLE_TODO, TOGGLE_SHOPPING, TOGGLE_HABIT, CALL, OPEN_CALENDAR, NONE }

/**
 * Scrollable list widgets and the Smart Stack. On the home screen the bitmap draws the background
 * and header and a native ListView/StackView (fed by [CollectionWidgetService]) shows the rows;
 * in-app previews can't scroll, so they draw the first rows into the bitmap instead.
 */
object CollectionWidgets {

    const val TODO = "prod.todo.scroll"
    const val SHOPPING = "prod.shopping.list"
    const val HABITS = "prod.habit.list"
    const val WEEK_AGENDA = "calendar.week.scroll"
    const val FAVORITES = "contact.favorites.scroll"
    const val SMART_STACK = "prod.smart.stack"

    private val LISTS = setOf(TODO, SHOPPING, HABITS, WEEK_AGENDA, FAVORITES)

    fun isList(widgetId: String) = widgetId in LISTS
    fun isStack(widgetId: String) = widgetId == SMART_STACK
    fun isCollection(widgetId: String) = isList(widgetId) || isStack(widgetId)

    /** Header height reserved above the list, in dp (matches widget_list_layout). */
    const val HEADER_DP = 46f

    fun rows(context: Context, widgetId: String): List<ListRow> = when (widgetId) {
        TODO -> PreferencesManager.getInstance(context).getTodos().mapIndexed { i, t ->
            ListRow("$i", t.text, checked = t.done, action = RowAction.TOGGLE_TODO)
        }.sortedBy { it.checked == true }

        SHOPPING -> ListStore.shopping(context).map {
            ListRow(it.id, it.text, checked = it.checked, action = RowAction.TOGGLE_SHOPPING)
        }.sortedBy { it.checked == true }

        HABITS -> ListStore.habits(context).map { h ->
            val streak = h.streak()
            ListRow(h.id, h.name, if (streak > 0) "$streak DAY STREAK" else "START TODAY", checked = h.doneToday(), action = RowAction.TOGGLE_HABIT)
        }

        WEEK_AGENDA -> {
            val dayFmt = SimpleDateFormat("EEE dd", Locale.US)
            val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            CalendarRepository.upcoming(context, 7).orEmpty().map { e ->
                val isToday = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(e.beginMs)) == today
                val day = if (isToday) "TODAY" else dayFmt.format(Date(e.beginMs)).uppercase()
                val time = if (e.allDay) "ALL DAY" else timeFmt.format(Date(e.beginMs))
                ListRow("${e.beginMs}", e.title, "$day • $time", highlight = isToday, action = RowAction.OPEN_CALENDAR)
            }
        }

        FAVORITES -> ContactRepository.starred(context).map {
            ListRow(it.number, it.name, it.number, action = RowAction.CALL)
        }

        else -> emptyList()
    }

    /** Message shown when a list has no rows (or can't be read). */
    fun emptyMessage(context: Context, widgetId: String): String = when (widgetId) {
        TODO -> "ADD TASKS IN THE APP"
        SHOPPING -> "ADD ITEMS IN THE APP"
        HABITS -> "ADD HABITS IN THE APP"
        WEEK_AGENDA -> if (CalendarRepository.hasPermission(context)) "NOTHING IN THE NEXT 7 DAYS" else "ALLOW CALENDAR ACCESS IN THE APP"
        FAVORITES -> if (ContactRepository.hasPermission(context)) "STAR CONTACTS IN YOUR CONTACTS APP" else "ALLOW CONTACTS ACCESS IN THE APP"
        SMART_STACK -> "PICK WIDGETS FOR THIS STACK IN THE APP"
        else -> ""
    }

    private fun title(widgetId: String) = when (widgetId) {
        TODO -> "TO-DO"
        SHOPPING -> "SHOPPING"
        HABITS -> "HABITS"
        WEEK_AGENDA -> "NEXT 7 DAYS"
        FAVORITES -> "FAVOURITES"
        else -> ""
    }

    /**
     * Bitmap for a list widget: background, header with a count, and — in previews only — the
     * first rows (on the home screen the ListView overlay draws them).
     */
    fun renderList(context: Context, spec: WidgetSpec, canvas: Canvas, widgetId: String) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val rows = rows(context, widgetId)
        val summary = when (widgetId) {
            TODO, SHOPPING -> rows.takeIf { it.isNotEmpty() }?.let { r -> "${r.count { it.checked == true }}/${r.size} DONE" }
            HABITS -> rows.takeIf { it.isNotEmpty() }?.let { r -> "${r.count { it.checked == true }}/${r.size} TODAY" }
            else -> rows.takeIf { it.isNotEmpty() }?.let { "${it.size}" }
        }
        kit.header(listOfNotNull(title(widgetId), summary).joinToString(" • "), widthFraction = 0.95f)

        if (spec.liveOverlays) return
        // Preview: draw the first rows statically
        val top = HEADER_DP * d
        val rowH = 34f * d
        if (rows.isEmpty()) {
            kit.labelCentered(emptyMessage(context, widgetId), kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        rows.take(((kit.height - top - kit.pad) / rowH).toInt().coerceAtLeast(1)).forEachIndexed { i, row ->
            val y = top + i * rowH + rowH * 0.6f
            row.checked?.let { done ->
                canvas.drawCircle(kit.pad + 6f * d, y - 4f * d, 5f * d, kit.fill(if (done) kit.accentColor else kit.offColor))
            }
            val textX = kit.pad + if (row.checked != null) 20f * d else 0f
            val color = if (row.checked == true) kit.greyColor else if (row.highlight) kit.accentColor else kit.onColor
            DotMatrixRenderer.drawFittedLabel(canvas, row.title.uppercase(), textX, y, kit.width - kit.pad - textX, kit.textPaint(color, 12f, 18f))
            row.subtitle?.let {
                DotMatrixRenderer.drawFittedLabel(canvas, it, textX, y + 13f * d, kit.width - kit.pad - textX, kit.textPaint(kit.greyColor, 9f, 13f))
            }
        }
    }

    /** Stack widget bitmap: just the background (cards are drawn by the StackView); previews show the first card's name. */
    fun renderStackBackground(context: Context, spec: WidgetSpec, canvas: Canvas, items: List<String>) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (spec.liveOverlays) return
        kit.header("SMART STACK • ${items.size} WIDGETS")
        val message = if (items.isEmpty()) emptyMessage(context, SMART_STACK) else "SWIPE TO FLIP THROUGH ${items.size} WIDGETS"
        kit.labelCentered(message, kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
    }

    /** Widget ids chosen for a Smart Stack, from its config. */
    fun stackItems(configJson: String): List<String> = runCatching {
        val arr = org.json.JSONObject(configJson).optJSONArray(STACK_KEY) ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    const val STACK_KEY = "stack_items"
    const val MAX_STACK = 5
}
