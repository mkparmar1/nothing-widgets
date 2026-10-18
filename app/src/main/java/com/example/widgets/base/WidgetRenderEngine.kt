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
import android.os.SystemClock
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
        val isDynamic = definition.id.startsWith("battery.") || 
                        definition.id.startsWith("fun.") || 
                        definition.id.startsWith("sensor.stopwatch") ||
                        definition.id.startsWith("prod.focus.timer") ||
                        definition.id.startsWith("prod.countdown") ||
                        definition.id.startsWith("prod.water") ||
                        definition.id.startsWith("toggle.")

        val cacheKey = if (isDynamic) {
            null
        } else {
            BitmapCache.makeKey(
                widgetTypeId = definition.id,
                instanceId = instanceId,
                valueHash = spec.configJson.hashCode() + (System.currentTimeMillis() / 60000).toInt(),
                size = spec.sizeBucket,
                isDark = spec.isDark,
                accent = spec.accent.name
            )
        }

        if (cacheKey != null) {
            val cached = BitmapCache.get(cacheKey)
            if (cached != null) return@withContext cached
        }

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
        if (cacheKey != null) {
            BitmapCache.put(cacheKey, bitmap)
        }
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
        applyLiveClock(views, definition.id, spec)
        applyLiveTimer(context, views, definition.id, spec)

        val regions = WidgetTapTargets.regions(context, appWidgetId, definition.id, sizeBucket)
        if (regions == null) views.setViewVisibility(R.id.dynamic_regions, View.GONE)

        // Multi-region check for 2x2 toggles
        if (regions != null) {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)
            WidgetTapTargets.applyRegions(context, views, regions)
        } else if (definition.id == "toggle.grid.2x2") {
            views.setViewVisibility(R.id.multi_region_container, View.VISIBLE)

            views.setOnClickPendingIntent(R.id.click_region_tl, makeTogglePendingIntent(context, appWidgetId, "WIFI", 1))
            views.setOnClickPendingIntent(R.id.click_region_tr, makeTogglePendingIntent(context, appWidgetId, "BT", 2))
            views.setOnClickPendingIntent(R.id.click_region_bl, makeTogglePendingIntent(context, appWidgetId, "TORCH", 3))
            views.setOnClickPendingIntent(R.id.click_region_br, makeTogglePendingIntent(context, appWidgetId, "DND", 4))
        } else if (definition.id !in INTERACTIVE_WIDGET_IDS && tapAction == TapAction.NONE) {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)

            val pendingIntent = WidgetTapTargets.single(context, appWidgetId, definition.id) ?: when (definition.id) {
                "fun.dice.roll" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_DICE_ROLL")
                "fun.snake.dot" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_SNAKE_STEP")
                "sensor.stopwatch.chrono" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_STOPWATCH_TOGGLE")
                "prod.focus.timer" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_TIMER_TOGGLE")
                "prod.countdown.timer" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_COUNTDOWN_TOGGLE")
                "prod.water.tracker" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_WATER_ADD")
                "toggle.wifi" -> makeTogglePendingIntent(context, appWidgetId, "WIFI", 1)
                "toggle.bluetooth" -> makeTogglePendingIntent(context, appWidgetId, "BT", 2)
                "toggle.torch" -> makeTogglePendingIntent(context, appWidgetId, "TORCH", 3)
                "toggle.dnd" -> makeTogglePendingIntent(context, appWidgetId, "DND", 4)
                "toggle.darkmode" -> makeTogglePendingIntent(context, appWidgetId, "DARK_MODE", 5)
                "toggle.rotation" -> makeTogglePendingIntent(context, appWidgetId, "ROTATION", 6)
                "toggle.datasaver" -> makeTogglePendingIntent(context, appWidgetId, "DATA_SAVER", 7)
                "toggle.hotspot" -> makeTogglePendingIntent(context, appWidgetId, "HOTSPOT", 8)
                "toggle.airplane" -> makeTogglePendingIntent(context, appWidgetId, "AIRPLANE", 9)
                "prod.habit.dots" -> makeActionPendingIntent(context, appWidgetId, "com.aistudio.dotwidgets.ACTION_HABIT_TOGGLE")
                else -> createTapPendingIntent(context, appWidgetId, tapAction)
            }

            views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }

        views
    }

    /** Widgets whose time is shown by the live TextClock overlay instead of the bitmap. */
    private val LIVE_SECONDS_WIDGET_IDS = setOf("clock.seconds.live")

    private fun applyLiveClock(views: RemoteViews, widgetTypeId: String, spec: WidgetSpec) {
        if (widgetTypeId !in LIVE_SECONDS_WIDGET_IDS) {
            views.setViewVisibility(R.id.live_clock_container, View.GONE)
            return
        }
        views.setViewVisibility(R.id.live_clock_container, View.VISIBLE)
        views.setTextColor(R.id.live_clock, if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt())

        // Colour the colons with the accent, matching the dot-matrix clocks. TextClock keeps
        // spans on literal characters of its format.
        val format = SpannableString("HH:mm:ss")
        format.forEachIndexed { i, c ->
            if (c == ':') {
                format.setSpan(ForegroundColorSpan(spec.accent.hex), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        views.setCharSequence(R.id.live_clock, "setFormat24Hour", format)
        views.setCharSequence(R.id.live_clock, "setFormat12Hour", format)
    }

    /**
     * Running stopwatch / focus timer / countdown: a Chronometer ticks every second on the launcher
     * side, so the time stays live between our once-a-minute redraws.
     */
    private fun applyLiveTimer(context: Context, views: RemoteViews, widgetTypeId: String, spec: WidgetSpec) {
        val now = SystemClock.elapsedRealtime()
        val base: Long? = when (widgetTypeId) {
            "sensor.stopwatch.chrono" -> WidgetStateManager.getStopwatchState(context)
                .takeIf { it.isRunning }?.let { now - it.elapsedMs }
            "prod.focus.timer" -> WidgetStateManager.getFocusTimerState(context)
                .takeIf { it.isRunning }?.let { now + it.remainingSeconds * 1000L }
            "prod.countdown.timer" -> WidgetStateManager.getCountdownState(context)
                .takeIf { it.isRunning }?.let { now + it.remainingSeconds * 1000L }
            else -> null
        }
        if (base == null) {
            views.setViewVisibility(R.id.live_timer_container, View.GONE)
            views.setChronometer(R.id.live_timer, now, null, false)
            return
        }
        views.setViewVisibility(R.id.live_timer_container, View.VISIBLE)
        views.setTextColor(R.id.live_timer, if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt())
        views.setChronometerCountDown(R.id.live_timer, widgetTypeId != "sensor.stopwatch.chrono")
        views.setChronometer(R.id.live_timer, base, null, true)
    }

    private fun makeActionPendingIntent(context: Context, appWidgetId: Int, actionStr: String): PendingIntent {
        val intent = Intent(actionStr).apply {
            setPackage(context.packageName)
            putExtra("app_widget_id", appWidgetId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val reqCode = appWidgetId * 1000 + (kotlin.math.abs(actionStr.hashCode()) % 900)
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
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
            // NONE is handled by not attaching a click handler at all; fall back to the app.
            TapAction.NONE -> Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, appWidgetId * 10, intent, flags)
    }

    private val INTERACTIVE_WIDGET_IDS = setOf(
        "fun.dice.roll", "fun.snake.dot", "sensor.stopwatch.chrono", "prod.focus.timer",
        "prod.countdown.timer", "prod.water.tracker", "toggle.wifi", "toggle.bluetooth",
        "toggle.torch", "toggle.dnd", "toggle.darkmode", "toggle.rotation", "toggle.datasaver",
        "toggle.hotspot", "toggle.airplane", "prod.habit.dots", "media.nowplaying.compact",
        "media.nowplaying.art", "media.volume.dots"
    )

    private fun makeTogglePendingIntent(context: Context, appWidgetId: Int, toggleName: String, requestCodeOffset: Int): PendingIntent {
        // Normal apps can't switch Wi-Fi, Bluetooth or dark mode, so open the system panel directly.
        // Using an activity PendingIntent avoids background-activity-start restrictions.
        val settingsIntent = when (toggleName) {
            "WIFI" -> DeviceToggles.wifiSettingsIntent()
            "BT" -> DeviceToggles.bluetoothSettingsIntent()
            "DARK_MODE" -> DeviceToggles.darkModeSettingsIntent()
            "ROTATION" -> DeviceToggles.autoRotateSettingsIntent()
            "DATA_SAVER" -> DeviceToggles.dataSaverSettingsIntent()
            "HOTSPOT" -> DeviceToggles.hotspotSettingsIntent()
            "AIRPLANE" -> DeviceToggles.airplaneSettingsIntent()
            "DND" -> if (DeviceToggles.hasDndAccess(context)) null else DeviceToggles.dndAccessSettingsIntent()
            else -> null
        }
        if (settingsIntent != null) {
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + requestCodeOffset,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

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

        val placed = mutableListOf<WidgetInstance>()
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
                placed += instance
            }
        }

        // Read slow data sources once, and only when a widget on screen needs them.
        WidgetDataPrefetch.prefetch(context, placed.map { it.widgetTypeId }.toSet())

        for (instance in placed) {
            try {
                val views = buildRemoteViews(context, instance.appWidgetId, instance)
                manager.updateAppWidget(instance.appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("WidgetRenderEngine", "Failed to update widget ${instance.appWidgetId}", e)
            }
        }
    }
}
