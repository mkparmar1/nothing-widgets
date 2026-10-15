package com.example.widgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.bitmap.BitmapCache
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetRenderEngine {

    suspend fun renderBitmap(
        context: Context,
        definition: WidgetDefinition,
        spec: WidgetSpec,
        instanceId: Int = 0
    ): Bitmap = withContext(Dispatchers.Default) {
        val cacheKey = BitmapCache.makeKey(
            widgetTypeId = definition.id,
            instanceId = instanceId,
            valueHash = spec.configJson.hashCode() + (System.currentTimeMillis() / 60000).toInt(),
            size = spec.sizeBucket,
            isDark = spec.isDark,
            accent = spec.accent.name
        )

        val cached = BitmapCache.get(cacheKey)
        if (cached != null) return@withContext cached

        val bitmap = Bitmap.createBitmap(
            spec.widthPx.coerceAtLeast(100),
            spec.heightPx.coerceAtLeast(100),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        try {
            definition.render(context, spec, canvas)
        } catch (e: Exception) {
            android.util.Log.e("WidgetRenderEngine", "Render failed for ${definition.id}", e)
            com.example.widgets.catalogue.ClockRenderers.renderDigitalClock(spec, canvas)
        }
        BitmapCache.put(cacheKey, bitmap)
        bitmap
    }

    suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        instance: WidgetInstance
    ): RemoteViews = withContext(Dispatchers.Default) {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(appWidgetId) ?: Bundle()

        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val sizeBucket = SizeBucket.resolve(minW, minH)

        val dims = SizeResolver.resolveDimensions(context, sizeBucket, minW, minH)

        // Read global defaults or instance override
        val prefs = PreferencesManager.getInstance(context)
        val accentStr = instance.getAccent(prefs.getSavedAccent().name)
        val accent = AccentOption.fromName(accentStr)

        val bgModeStr = instance.getBackground(prefs.getSavedBgMode().name)
        val bgMode = BackgroundMode.fromName(bgModeStr)

        val themeModeStr = instance.getThemeMode(prefs.getSavedThemeMode().name)
        val themeMode = ThemeMode.fromName(themeModeStr)

        val isDark = when (themeMode) {
            ThemeMode.FORCE_DARK -> true
            ThemeMode.FORCE_LIGHT -> false
            ThemeMode.AUTO -> {
                val nightFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val cornerRadius = instance.getCornerRadius(20)
        val showLabel = instance.getShowLabel(true)
        val tapActionName = instance.getTapAction("DEFAULT")
        val tapAction = if (tapActionName == "DEFAULT") {
            WidgetRegistry.getById(instance.widgetTypeId)?.defaultTap ?: TapAction.DEFAULT
        } else {
            try { TapAction.valueOf(tapActionName) } catch (_: Exception) { TapAction.DEFAULT }
        }

        val spec = WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = sizeBucket,
            accent = accent,
            bgMode = bgMode,
            themeMode = themeMode,
            isDark = isDark,
            cornerRadiusDp = cornerRadius,
            showLabel = showLabel,
            tapAction = tapAction,
            configJson = instance.configJson
        )

        val definition = WidgetRegistry.getById(instance.widgetTypeId)
            ?: WidgetRegistry.getAll().first()

        val bitmap = renderBitmap(context, definition, spec, appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_canvas_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        // Multi-region check for 2x2 toggles
        if (definition.id == "toggle.grid.2x2") {
            views.setViewVisibility(R.id.multi_region_container, View.VISIBLE)

            views.setOnClickPendingIntent(R.id.click_region_tl, makeTogglePendingIntent(context, appWidgetId, "WIFI", 1))
            views.setOnClickPendingIntent(R.id.click_region_tr, makeTogglePendingIntent(context, appWidgetId, "BT", 2))
            views.setOnClickPendingIntent(R.id.click_region_bl, makeTogglePendingIntent(context, appWidgetId, "TORCH", 3))
            views.setOnClickPendingIntent(R.id.click_region_br, makeTogglePendingIntent(context, appWidgetId, "DND", 4))
        } else {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)

            val pendingIntent = createTapPendingIntent(context, appWidgetId, tapAction)
            views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }

        views
    }

    private fun createTapPendingIntent(context: Context, appWidgetId: Int, tapAction: TapAction): PendingIntent {
        val intent = when (tapAction) {
            TapAction.OPEN_CLOCK -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            TapAction.OPEN_CALENDAR -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            TapAction.OPEN_BATTERY -> {
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            TapAction.OPEN_WEATHER, TapAction.OPEN_MAIN, TapAction.DEFAULT -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("launch_widget_id", appWidgetId)
                    putExtra("open_weather_detail", tapAction == TapAction.OPEN_WEATHER)
                }
            }
            TapAction.NONE -> {
                Intent()
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, appWidgetId * 10, intent, flags)
    }

    private fun makeTogglePendingIntent(context: Context, appWidgetId: Int, toggleName: String, requestCodeOffset: Int): PendingIntent {
        val intent = Intent("com.aistudio.dotwidgets.ACTION_TOGGLE_CLICK").apply {
            setPackage(context.packageName)
            putExtra("app_widget_id", appWidgetId)
            putExtra("toggle_name", toggleName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, appWidgetId * 100 + requestCodeOffset, intent, flags)
    }

    suspend fun updateAllPlacedWidgets(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val manager = AppWidgetManager.getInstance(context)

        val providers = listOf(
            android.content.ComponentName(context, DotWidgetProviderS::class.java) to "clock.dotdigital.s",
            android.content.ComponentName(context, DotWidgetProviderM::class.java) to "clock.dotdigital.m",
            android.content.ComponentName(context, DotWidgetProviderL::class.java) to "clock.dotdigital.l"
        )

        for ((provider, defaultType) in providers) {
            val ids = try { manager.getAppWidgetIds(provider) } catch (_: Exception) { IntArray(0) }
            for (id in ids) {
                var instance = db.widgetInstanceDao().getById(id)
                if (instance == null) {
                    instance = WidgetInstance(
                        appWidgetId = id,
                        widgetTypeId = defaultType,
                        schemaVersion = 1,
                        configJson = "{}"
                    )
                    db.widgetInstanceDao().insertOrUpdate(instance)
                }
                try {
                    val views = buildRemoteViews(context, id, instance)
                    manager.updateAppWidget(id, views)
                } catch (e: Exception) {
                    android.util.Log.e("WidgetRenderEngine", "Failed to update widget $id", e)
                }
            }
        }
    }
}
