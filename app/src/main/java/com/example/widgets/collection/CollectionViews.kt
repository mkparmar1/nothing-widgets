package com.example.widgets.collection

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.widgets.base.WidgetProviders
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetUpdateReceiver

/** Builds the RemoteViews for list and stack widgets and tells the launcher when their rows change. */
object CollectionViews {

    const val ACTION_LIST_ITEM = "com.aistudio.dotwidgets.ACTION_LIST_ITEM"

    fun build(context: Context, appWidgetId: Int, widgetId: String, spec: WidgetSpec, bitmap: Bitmap): RemoteViews {
        val isStack = CollectionWidgets.isStack(widgetId)
        val views = RemoteViews(context.packageName, if (isStack) R.layout.widget_stack_layout else R.layout.widget_list_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        // One factory per widget: the data URI makes each service intent distinct.
        val service = Intent(context, CollectionWidgetService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(Uri.parse("dotwidgets://collection/$appWidgetId"))
        val listId = if (isStack) R.id.stack_view else R.id.list_view
        val emptyId = if (isStack) R.id.stack_empty else R.id.list_empty
        @Suppress("DEPRECATION")
        views.setRemoteAdapter(listId, service)
        views.setEmptyView(listId, emptyId)
        views.setTextViewText(emptyId, CollectionWidgets.emptyMessage(context, widgetId))
        views.setTextColor(emptyId, if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt())

        val openApp = PendingIntent.getActivity(
            context, appWidgetId * 100 + 61,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(emptyId, openApp)
        if (!isStack) views.setOnClickPendingIntent(R.id.list_header, openApp)

        // Row taps: rows fill in the item (and the dial/calendar target) on a mutable template.
        val mutable = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val template = when (widgetId) {
            CollectionWidgets.FAVORITES ->
                PendingIntent.getActivity(context, appWidgetId * 100 + 62, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), mutable)
            CollectionWidgets.WEEK_AGENDA ->
                PendingIntent.getActivity(context, appWidgetId * 100 + 62, Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), mutable)
            CollectionWidgets.SMART_STACK -> PendingIntent.getActivity(
                context, appWidgetId * 100 + 62,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), mutable
            )
            else -> PendingIntent.getBroadcast(
                context, appWidgetId * 100 + 62,
                Intent(context, WidgetUpdateReceiver::class.java).setAction(ACTION_LIST_ITEM), mutable
            )
        }
        views.setPendingIntentTemplate(listId, template)
        if (isStack) views.setViewVisibility(R.id.stack_empty, View.GONE)
        return views
    }

    /** Asks the launcher to re-read the rows/cards of every placed collection widget of [widgetIds] types (all when null). */
    fun notifyChanged(context: Context, placed: Map<Int, String>, widgetIds: Set<String>? = null) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        placed.forEach { (appWidgetId, typeId) ->
            if (!CollectionWidgets.isCollection(typeId)) return@forEach
            if (widgetIds != null && typeId !in widgetIds) return@forEach
            val viewId = if (CollectionWidgets.isStack(typeId)) R.id.stack_view else R.id.list_view
            @Suppress("DEPRECATION")
            manager.notifyAppWidgetViewDataChanged(appWidgetId, viewId)
        }
    }

    /** All placed widgets by id → type, read from the database. */
    suspend fun placedTypes(context: Context): Map<Int, String> {
        val dao = com.example.data.db.AppDatabase.getInstance(context).widgetInstanceDao()
        return WidgetProviders.placedIds(context).flatMap { (_, ids) -> ids.toList() }
            .mapNotNull { id -> dao.getById(id)?.let { id to it.widgetTypeId } }
            .toMap()
    }
}
