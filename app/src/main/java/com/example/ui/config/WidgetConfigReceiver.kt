package com.example.ui.config

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val widgetTypeId = intent.getStringExtra("widget_type_id") ?: return
        val configJson = intent.getStringExtra("config_json") ?: "{}"

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val instance = WidgetInstance(
                        appWidgetId = appWidgetId,
                        widgetTypeId = widgetTypeId,
                        schemaVersion = 1,
                        configJson = configJson
                    )
                    db.widgetInstanceDao().insertOrUpdate(instance)

                    // Immediate render to home screen
                    val views = WidgetRenderEngine.buildRemoteViews(context, appWidgetId, instance)
                    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e("WidgetConfigReceiver", "Failed to configure pinned widget $appWidgetId", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
