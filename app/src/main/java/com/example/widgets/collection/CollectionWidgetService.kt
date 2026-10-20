package com.example.widgets.collection

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.time.ClockOptions
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.runBlocking

/**
 * Feeds rows to the scrollable list widgets and cards to the Smart Stack. The launcher binds one
 * factory per placed widget (the intent's data URI includes the widget id).
 */
class CollectionWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return Factory(applicationContext, appWidgetId)
    }

    private class Factory(private val context: Context, private val appWidgetId: Int) : RemoteViewsFactory {

        private var instance: WidgetInstance? = null
        private var spec: WidgetSpec? = null
        private var rows: List<ListRow> = emptyList()
        private var stackItems: List<String> = emptyList()

        override fun onCreate() {}

        /** Called by the launcher after notifyAppWidgetViewDataChanged; runs off the main thread. */
        override fun onDataSetChanged() {
            val inst = runBlocking { AppDatabase.getInstance(context).widgetInstanceDao().getById(appWidgetId) } ?: return
            instance = inst
            spec = WidgetRenderEngine.specFor(context, appWidgetId, inst, STACK_CARD_BUDGET)
            if (CollectionWidgets.isStack(inst.widgetTypeId)) {
                stackItems = CollectionWidgets.stackItems(inst.configJson)
                rows = emptyList()
            } else {
                rows = CollectionWidgets.rows(context, inst.widgetTypeId)
                stackItems = emptyList()
            }
        }

        override fun onDestroy() {}

        override fun getCount() = if (stackItems.isNotEmpty()) stackItems.size else rows.size

        override fun getViewAt(position: Int): RemoteViews =
            if (stackItems.isNotEmpty()) stackCard(position) else listRow(position)

        private fun listRow(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_list_row)
            val row = rows.getOrNull(position) ?: return views
            val s = spec
            val dark = s?.isDark ?: true
            val onColor = if (dark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
            val grey = if (dark) 0xFF8A8A8A.toInt() else 0xFF6A6A6A.toInt()
            val accent = s?.accentColor ?: 0xFFE02B2B.toInt()

            if (row.checked != null) {
                views.setTextViewText(R.id.row_check, if (row.checked) "●" else "○")
                views.setTextColor(R.id.row_check, if (row.checked) accent else grey)
                views.setViewVisibility(R.id.row_check, android.view.View.VISIBLE)
            } else {
                views.setTextViewText(R.id.row_check, if (row.highlight) "●" else "•")
                views.setTextColor(R.id.row_check, if (row.highlight) accent else grey)
            }
            views.setTextViewText(R.id.row_title, row.title)
            views.setTextColor(R.id.row_title, if (row.checked == true) grey else onColor)
            if (row.subtitle != null) {
                views.setTextViewText(R.id.row_subtitle, row.subtitle)
                views.setTextColor(R.id.row_subtitle, if (row.highlight) accent else grey)
                views.setViewVisibility(R.id.row_subtitle, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.row_subtitle, android.view.View.GONE)
            }

            // Fill-in for the list's PendingIntent template (see WidgetRenderEngine)
            val fillIn = Intent()
                .putExtra(EXTRA_ROW_ACTION, row.action.name)
                .putExtra(EXTRA_ITEM_ID, row.itemId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Contacts and agenda lists use an activity template (dialer / calendar); the row supplies the target.
            when (row.action) {
                RowAction.CALL -> fillIn.data = Uri.parse("tel:" + Uri.encode(row.itemId))
                RowAction.OPEN_CALENDAR -> fillIn.data = Uri.parse("content://com.android.calendar/time/${row.itemId}")
                else -> Unit
            }
            views.setOnClickFillInIntent(R.id.list_row, fillIn)
            return views
        }

        /** One stack card: the chosen widget design drawn with the stack's own size and style. */
        private fun stackCard(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_stack_item)
            val base = spec ?: return views
            val definition = WidgetRegistry.getById(stackItems.getOrNull(position) ?: return views) ?: return views
            val cardSpec = base.copy(liveOverlays = false)
            val bitmap = Bitmap.createBitmap(cardSpec.widthPx, cardSpec.heightPx, Bitmap.Config.ARGB_8888)
            runCatching {
                DotMatrixRenderer.withStyle(
                    ClockOptions.dotStyle(cardSpec.configJson), Skin.from(cardSpec.configJson), Frame.from(cardSpec.configJson)
                ) { definition.render(context, cardSpec, Canvas(bitmap)) }
            }
            views.setImageViewBitmap(R.id.stack_card, bitmap)
            views.setContentDescription(R.id.stack_card, definition.describe(context))
            views.setOnClickFillInIntent(
                R.id.stack_card,
                Intent().putExtra(EXTRA_ROW_ACTION, RowAction.NONE.name).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 2
        override fun getItemId(position: Int) =
            (if (stackItems.isNotEmpty()) stackItems.getOrNull(position) else rows.getOrNull(position)?.itemId).hashCode().toLong()
        override fun hasStableIds() = true
    }

    companion object {
        const val EXTRA_ROW_ACTION = "row_action"
        const val EXTRA_ITEM_ID = "item_id"

        /** Each stack card is sent on its own, but several are kept by the launcher; keep them modest. */
        private const val STACK_CARD_BUDGET = 700_000L
    }
}
