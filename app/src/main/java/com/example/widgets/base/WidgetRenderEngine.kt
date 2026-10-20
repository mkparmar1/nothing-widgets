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
import android.app.SearchManager
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
import com.example.core.theme.MaterialYouPalette
import com.example.core.theme.ThemeMode
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.time.ClockOptions
import com.example.core.time.ClockSupport
import com.example.core.time.TimeFormat
import com.example.data.prefs.PreferencesManager
import com.example.widgets.catalogue.MediaRenderers
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
                accent = spec.accentColor.toString()
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
            DotMatrixRenderer.withStyle(ClockOptions.dotStyle(spec.configJson), Skin.from(spec.configJson), Frame.from(spec.configJson)) {
                definition.render(context, spec, canvas)
            }
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
    ): RemoteViews {
        // Callers send these straight to the launcher, so remember what it now shows.
        val (views, signature) = buildWidget(context, appWidgetId, instance)
        lastSignatures[appWidgetId] = signature
        return views
    }

    /** Builds the widget's views plus a signature of everything visible, used to skip no-op updates. */
    private suspend fun buildWidget(
        context: Context,
        appWidgetId: Int,
        instance: WidgetInstance
    ): Pair<RemoteViews, Long> = withContext(Dispatchers.Default) {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(appWidgetId) ?: Bundle()

        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val sizeBucket = SizeBucket.resolve(minW, minH)

        // The animated Soundwave sends extra frames in the same update, so its main image is smaller.
        val budget = if (instance.widgetTypeId == SOUNDWAVE_WIDGET_ID) MediaRenderers.SOUNDWAVE_MAIN_BUDGET else 1_000_000L
        val dims = SizeResolver.resolveFromOptions(context, options, sizeBucket, budget)

        // Read global defaults or instance override
        val prefs = PreferencesManager.getInstance(context)
        val accentStr = instance.getAccent(prefs.getSavedAccent().name)
        val accent = AccentOption.fromName(accentStr)
        val accentColor = if (accent == AccentOption.DYNAMIC) {
            MaterialYouPalette.resolve(context)
        } else {
            accent.hex
        }

        val bgModeStr = instance.getBackground(prefs.getSavedBgMode().name)
        val bgMode = BackgroundMode.fromName(bgModeStr)
        val bgAlpha = instance.getBgAlphaPercent(100) / 100f

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
            accentColor = accentColor,
            bgMode = bgMode,
            bgAlpha = bgAlpha,
            themeMode = themeMode,
            // Paper is always light; Neon and Terminal always dark.
            isDark = Skin.from(instance.configJson).resolveDark(isDark),
            cornerRadiusDp = cornerRadius,
            showLabel = showLabel,
            tapAction = tapAction,
            configJson = instance.configJson,
            liveOverlays = true
        )

        val definition = WidgetRegistry.getById(instance.widgetTypeId)
            ?: WidgetRegistry.getAll().first()

        val missingPermission = definition.requiredPermission?.takeIf { !it.isGranted(context) }
        val bitmap = if (missingPermission != null) {
            renderPermissionMessage(definition, spec, missingPermission)
        } else {
            renderBitmap(context, definition, spec, appWidgetId)
        }

        val views = RemoteViews(context.packageName, R.layout.widget_canvas_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)
        views.setContentDescription(R.id.widget_image_view, definition.describe(context))
        applyLiveClock(views, definition.id, spec)
        applyLiveTimer(context, views, definition.id, spec)
        applySoundwave(context, views, definition.id, spec)

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

        // A missing permission replaces the design with a tappable prompt.
        if (missingPermission != null) {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)
            val permissionIntent = createPermissionPendingIntent(context, appWidgetId, missingPermission.id)
            views.setOnClickPendingIntent(R.id.widget_image_view, permissionIntent)
            views.setOnClickPendingIntent(R.id.widget_root, permissionIntent)
        }

        // Live overlays (TextClock, Chronometer, Soundwave frames) tick on their own; anything they
        // change visibly (running/paused, title, date) is in the bitmap, so the bitmap plus these
        // inputs identify what the launcher would show.
        val signature = bitmapSignature(bitmap) * 31 +
            listOf(definition.id, instance.configJson, missingPermission?.id, DeviceToggles.hasDndAccess(context)).hashCode()
        views to signature
    }

    /** Last signature sent per widget id; empty after a process restart, so the first refresh resends everything. */
    private val lastSignatures = java.util.concurrent.ConcurrentHashMap<Int, Long>()

    /** Cheap rolling hash of the pixels (row by row, no full-size copy). */
    private fun bitmapSignature(bitmap: android.graphics.Bitmap): Long {
        val row = IntArray(bitmap.width)
        var h = 1125899906842597L
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (px in row) h = 31 * h + px
        }
        return h * 31 + bitmap.width * 7919 + bitmap.height
    }

    private fun renderPermissionMessage(
        definition: WidgetDefinition,
        spec: WidgetSpec,
        permission: com.example.ui.permissions.AppPermission
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            spec.widthPx.coerceAtLeast(100),
            spec.heightPx.coerceAtLeast(100),
            Bitmap.Config.ARGB_8888
        )
        com.example.core.render.RenderKit.message(
            spec,
            Canvas(bitmap),
            definition.title.uppercase(),
            "${permission.title.uppercase()} ACCESS NEEDED"
        )
        return bitmap
    }

    private fun createPermissionPendingIntent(
        context: Context,
        appWidgetId: Int,
        permissionId: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_permission", permissionId)
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId * 37 + 7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val SOUNDWAVE_WIDGET_ID = "sensor.visualizer.eq"
    private val WAVE_FRAME_IDS = intArrayOf(
        R.id.wave_frame_0, R.id.wave_frame_1, R.id.wave_frame_2,
        R.id.wave_frame_3, R.id.wave_frame_4, R.id.wave_frame_5
    )

    /** Animated bars for a playing Soundwave; hidden otherwise (the bitmap shows a static wave). */
    private fun applySoundwave(context: Context, views: RemoteViews, widgetTypeId: String, spec: WidgetSpec) {
        val frames = if (widgetTypeId == SOUNDWAVE_WIDGET_ID) MediaRenderers.soundwaveFrames(context, spec) else null
        if (frames == null) {
            views.setViewVisibility(R.id.soundwave_flipper, View.GONE)
            return
        }
        val tint = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        frames.forEachIndexed { i, frame ->
            views.setImageViewBitmap(WAVE_FRAME_IDS[i], frame)
            views.setInt(WAVE_FRAME_IDS[i], "setColorFilter", tint)
        }
        views.setViewVisibility(R.id.soundwave_flipper, View.VISIBLE)
    }

    /** Widgets whose time is shown by the live TextClock overlay instead of the bitmap. */
    /** Digital clocks that can show seconds, with their time zone (null = phone's) and default format. */
    private val LIVE_CLOCK_WIDGETS = mapOf(
        "clock.seconds.live" to (null to TimeFormat.H24_SECONDS),
        "clock.dotdigital.s" to (null to TimeFormat.H24),
        "clock.dotdigital.m" to (null to TimeFormat.H24),
        "clock.dotdigital.l" to (null to TimeFormat.H24),
        "clock.world.london" to ("Europe/London" to TimeFormat.H24),
        "clock.world.tokyo" to ("Asia/Tokyo" to TimeFormat.H24),
        "clock.world.nyc" to ("America/New_York" to TimeFormat.H24),
        "clock.world.custom" to (null to TimeFormat.H24)
    )

    /**
     * Clocks set to show seconds: a TextClock ticked by the launcher draws the time in the chosen
     * 12/24-hour format and time zone (the bitmap leaves the digits out).
     */
    private fun applyLiveClock(views: RemoteViews, widgetTypeId: String, spec: WidgetSpec) {
        val (zone, defaultTime) = LIVE_CLOCK_WIDGETS[widgetTypeId] ?: (null to TimeFormat.H24)
        // A custom world clock keeps its zone in configJson rather than a fixed entry.
        val resolvedZone = if (widgetTypeId in ClockSupport.LIVE_ZONE_FROM_CONFIG) {
            ClockOptions.timeZone(spec.configJson) ?: zone
        } else zone
        val time = ClockOptions.from(spec.configJson, defaultTime).time
        if (widgetTypeId !in LIVE_CLOCK_WIDGETS || !time.hasSeconds) {
            views.setViewVisibility(R.id.live_clock_container, View.GONE)
            return
        }
        views.setViewVisibility(R.id.live_clock_container, View.VISIBLE)
        views.setTextColor(R.id.live_clock, if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt())
        views.setString(R.id.live_clock, "setTimeZone", resolvedZone)

        // Colour the colons with the accent, matching the dot-matrix clocks. TextClock keeps
        // spans on literal characters of its format.
        val format = SpannableString(time.pattern)
        format.forEachIndexed { i, c ->
            if (c == ':') {
                format.setSpan(ForegroundColorSpan(spec.accentColor), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
            TapAction.OPEN_SEARCH -> {
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, "")
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

    /**
     * Redraws every placed widget (home and lock screen). Widgets whose picture and settings are
     * unchanged since the last update are skipped, so the launcher isn't sent ~1 MB per widget every
     * minute for nothing. Pass [force] after boot, app updates or time changes, when the launcher
     * may need everything again.
     */
    suspend fun updateAllPlacedWidgets(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val manager = AppWidgetManager.getInstance(context)

        val placed = mutableListOf<WidgetInstance>()
        for ((entry, ids) in WidgetProviders.placedIds(context)) {
            val defaultType = entry.defaultWidgetTypeId
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

        if (force) lastSignatures.clear()
        val placedIds = placed.map { it.appWidgetId }.toSet()
        lastSignatures.keys.retainAll(placedIds)

        var sent = 0
        for (instance in placed) {
            try {
                val (views, signature) = buildWidget(context, instance.appWidgetId, instance)
                if (lastSignatures[instance.appWidgetId] == signature) continue
                manager.updateAppWidget(instance.appWidgetId, views)
                lastSignatures[instance.appWidgetId] = signature
                sent++
            } catch (e: Exception) {
                android.util.Log.e("WidgetRenderEngine", "Failed to update widget ${instance.appWidgetId}", e)
            }
        }

        android.util.Log.d("WidgetRenderEngine", "Refresh: sent $sent of ${placed.size} widgets (${placed.size - sent} unchanged)")
        PreferencesManager.getInstance(context).setLastWidgetRefresh(System.currentTimeMillis())
    }

    /** Number of Dot Widgets currently placed on the home screen. */
    fun placedWidgetCount(context: Context): Int =
        WidgetProviders.placedIds(context).sumOf { (_, ids) -> ids.size }
}
