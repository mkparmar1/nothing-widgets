package com.example.widgets.base

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class BaseDotWidgetProvider(private val defaultWidgetTypeId: String) : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetAlarmScheduler.scheduleNextMinuteTick(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetAlarmScheduler.scheduleNextMinuteTick(context)
        val pendingResult = goAsync()
        val db = AppDatabase.getInstance(context)

        scope.launch {
            try {
                for (id in appWidgetIds) {
                    var instance = db.widgetInstanceDao().getById(id)
                    if (instance == null) {
                        instance = WidgetInstance(
                            appWidgetId = id,
                            widgetTypeId = defaultWidgetTypeId,
                            schemaVersion = 1,
                            configJson = "{}"
                        )
                        db.widgetInstanceDao().insertOrUpdate(instance)
                    }

                    try {
                        val views = WidgetRenderEngine.buildRemoteViews(context, id, instance)
                        appWidgetManager.updateAppWidget(id, views)
                    } catch (e: Exception) {
                        Log.e("DotWidgetProvider", "Failed to update widget $id", e)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        val db = AppDatabase.getInstance(context)
        scope.launch {
            try {
                var instance = db.widgetInstanceDao().getById(appWidgetId)
                if (instance == null) {
                    instance = WidgetInstance(
                        appWidgetId = appWidgetId,
                        widgetTypeId = defaultWidgetTypeId,
                        schemaVersion = 1,
                        configJson = "{}"
                    )
                    db.widgetInstanceDao().insertOrUpdate(instance)
                }
                val views = WidgetRenderEngine.buildRemoteViews(context, appWidgetId, instance)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("DotWidgetProvider", "Failed to update options for widget $appWidgetId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pendingResult = goAsync()
        val db = AppDatabase.getInstance(context)
        scope.launch {
            try {
                for (id in appWidgetIds) {
                    db.widgetInstanceDao().deleteById(id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancels the minute tick if no widgets from any provider remain.
        WidgetAlarmScheduler.scheduleNextMinuteTick(context)
    }
}

class DotWidgetProviderS : BaseDotWidgetProvider("clock.dotdigital.s")
class DotWidgetProviderM : BaseDotWidgetProvider("clock.dotdigital.m")
class DotWidgetProviderL : BaseDotWidgetProvider("clock.dotdigital.l")
