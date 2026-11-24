package com.example.widgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import com.example.core.theme.WidgetAccent
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.core.config.UniversalOptions
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.render.WidgetShapes
import com.example.core.time.ClockOptions
import com.example.core.time.ClockSupport
import com.example.core.time.TimeFormat
import com.example.data.prefs.PreferencesManager
import com.example.widgets.catalogue.MediaRenderers
import com.example.widgets.collection.CollectionViews
import com.example.widgets.collection.CollectionWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

object WidgetRenderEngine {

    suspend fun renderBitmap(
        context: Context,
        definition: WidgetDefinition,
        spec: WidgetSpec,
        instanceId: Int = 0
    ): Bitmap = withContext(Dispatchers.Default) {
        // Optional custom photo background: content is drawn light-on-dark over the darkened photo.
        // Theme widgets paint their own full artwork, so they don't take a photo.
        val photoChoice = if (definition.category == Category.THEMES) null
            else com.example.data.photos.BackgroundImageStore.fromConfig(spec.configJson)
        val photo = photoChoice?.let { c ->
            com.example.data.photos.BackgroundImageStore.load(context, c.id)
                ?.let { DotMatrixRenderer.PhotoBackground(it, c.dim, c.zoom, c.cx, c.cy, c.filter, c.dotSize, c.id) }
        }
        @Suppress("NAME_SHADOWING")
        val spec = if (photo != null) spec.copy(isDark = true) else spec
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
                valueHash = spec.configJson.hashCode() + spec.shape.name.hashCode() + spec.customPoints.hashCode() +
                    spec.shapeFillColor.hashCode() + spec.shapeBorderColor.hashCode() + spec.shapeBorderStyle.hashCode() +
                    definition.renderVersion + (System.currentTimeMillis() / 60000).toInt(),
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
        if (spec.shape != WidgetShape.ROUNDED) {
            canvas.clipPath(
                WidgetShapes.path(
                    shape = spec.shape,
                    width = spec.widthPx.toFloat(),
                    height = spec.heightPx.toFloat(),
                    cornerRadius = spec.cornerRadiusDp * spec.density,
                    customPoints = spec.customPoints
                )
            )
        }

        try {
            DotMatrixRenderer.withStyle(
                style = ClockOptions.dotStyle(spec.configJson),
                widgetSkin = Skin.from(spec.configJson),
                widgetFrame = Frame.from(spec.configJson),
                fillOverride = spec.shapeFillColor,
                borderOverride = spec.shapeBorderColor,
                borderOverrideStyle = spec.shapeBorderStyle
            ) {
                DotMatrixRenderer.withUniversal(UniversalOptions.textScale(spec.options), UniversalOptions.cornerDot(spec.options)) {
                    DotMatrixRenderer.withBackgroundImage(photo) {
                        definition.render(context, spec, canvas)
                        drawClockFormatCaption(canvas, spec, definition.id, photo != null)
                    }
                }
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

    /**
     * Small caption for clocks whose artwork cannot show a time/date format by itself. Drawn only when
     * the user explicitly chose that format, and never over a restyled widget, so a default clock
     * looks exactly as designed.
     */
    private fun drawClockFormatCaption(
        canvas: Canvas,
        spec: WidgetSpec,
        widgetTypeId: String,
        hasPhoto: Boolean
    ) {
        if (widgetTypeId !in ClockSupport.CAPTION_WIDGETS) return
        if (widgetTypeId == "photo.clock.overlay" && !hasPhoto) return
        if (com.example.widgets.styles.StyleCatalog.selectedStyle(widgetTypeId, spec.configJson) != null) return

        val config = runCatching { org.json.JSONObject(spec.configJson) }.getOrNull() ?: return
        val choseTime = config.optString(ClockOptions.KEY_TIME).isNotBlank()
        val choseDate = config.optString(ClockOptions.KEY_DATE).isNotBlank()
        if (!choseTime && !choseDate) return

        val options = ClockOptions.from(spec.configJson)
        val zoneId = ClockOptions.timeZone(spec.configJson)
            ?: spec.options.string(if (widgetTypeId == "travel.clock.city") "city" else "zone", "").takeIf { it.isNotBlank() }
            ?: "Europe/London".takeIf { widgetTypeId == "travel.clock.city" }
        val calendar = Calendar.getInstance(
            if (zoneId.isNullOrBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(zoneId)
        )
        // Several cities have no single time, and the bitmap redraws once a minute (no seconds).
        val showTime = choseTime && widgetTypeId !in ClockSupport.DRAWS_TIME && widgetTypeId != "travel.clock.multi"
        val timeFormat = if (options.time.is24h) TimeFormat.H24 else TimeFormat.H12
        val time = if (showTime) {
            timeFormat.format(calendar) + (timeFormat.amPm(calendar)?.let { " $it" } ?: "")
        } else null
        val date = if (choseDate && widgetTypeId !in ClockSupport.DRAWS_DATE) options.date.format(calendar) else null
        val caption = listOfNotNull(time, date).joinToString("  ·  ")
        if (caption.isBlank()) return

        val density = spec.density
        val padding = 10f * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (spec.isDark) 0xFFBFC3C7.toInt() else 0xFF3A3E42.toInt()
            textSize = (9.5f * density).coerceAtMost(13f)
            isFakeBoldText = true
        }
        // Right-aligned, clear of the corner dot.
        val right = spec.widthPx - padding - if (DotMatrixRenderer.hasCornerDot) maxOf(18f * density, 24f) else 0f
        val maxWidth = spec.widthPx * 0.62f
        val scaled = paint.measureText(caption) * DotMatrixRenderer.currentTextScale
        DotMatrixRenderer.drawFittedLabel(
            canvas, caption, right - scaled.coerceAtMost(maxWidth), padding + 11f * density, maxWidth, paint
        )
    }

    suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        instance: WidgetInstance
    ): RemoteViews {
        // Callers send these straight to the launcher, so remember what it now shows.
        val (views, signature) = buildWidget(
            context, appWidgetId, instance, refreshSmartStackSelection = true
        )
        lastSignatures[appWidgetId] = signature
        return views
    }

    /**
     * The spec a placed widget renders with: real size, accent (incl. Material You), background,
     * theme and skin-adjusted dark mode. Shared by normal widgets and collection rows/stack cards.
     */
    fun specFor(
        context: Context,
        appWidgetId: Int,
        instance: WidgetInstance,
        budgetBytes: Long? = null
    ): WidgetSpec {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(appWidgetId) ?: Bundle()

        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val sizeBucket = SizeBucket.resolve(minW, minH)

        // The animated Soundwave sends extra frames in the same update, so its main image is smaller.
        val budget = budgetBytes ?: AnimatedOverlays.mainBudget(instance.widgetTypeId)
        val dims = SizeResolver.resolveFromOptions(context, options, sizeBucket, budget)

        // Read global defaults or instance override
        val prefs = PreferencesManager.getInstance(context)
        val accentStr = instance.getAccent(prefs.getSavedAccent().name)
        val accent = AccentOption.fromName(accentStr)
        val accentColor = com.example.core.theme.WidgetAccent.resolve(context, accent, instance.configJson)

        val onLockScreen = WidgetProviders.isLockScreenWidget(context, appWidgetId)
        val bgModeStr = instance.getBackground(prefs.getSavedBgMode().name)
        val bgMode = LockScreenSafety.effectiveBackground(
            BackgroundMode.fromName(bgModeStr),
            onLockScreen
        )
        val bgAlpha = instance.getBgAlphaPercent(100) / 100f
        val configJson = if (onLockScreen) LockScreenSafety.sanitizedConfig(instance.configJson)
        else instance.configJson

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
            isDark = Skin.from(configJson).resolveDark(isDark),
            cornerRadiusDp = cornerRadius,
            showLabel = showLabel,
            tapAction = tapAction,
            configJson = configJson,
            liveOverlays = true,
            shape = WidgetShape.fromName(instance.getShape()),
            customPoints = WidgetShape.decodePoints(instance.getString("shape_points")),
            shapeFillColor = WidgetAccent.parseHex(instance.getString("shape_fill")),
            shapeBorderColor = WidgetAccent.parseHex(instance.getString("shape_border")),
            shapeBorderStyle = instance.getString("shape_border_style")
                .takeIf { it.isNotEmpty() }?.let { Frame.fromName(it) }
        )

        return spec
    }

    /** Builds the widget's views plus a signature of everything visible, used to skip no-op updates. */
    private suspend fun buildWidget(
        context: Context,
        appWidgetId: Int,
        instance: WidgetInstance,
        refreshSmartStackSelection: Boolean = false
    ): Pair<RemoteViews, Long> = withContext(Dispatchers.Default) {
        com.example.data.builder.CustomWidgetRepository.loadIntoRegistry(context)
        val requested = WidgetRegistry.getById(instance.widgetTypeId)
        val onLockScreen = WidgetProviders.isLockScreenWidget(context, appWidgetId)
        val useFallback = onLockScreen && !LockScreenSafety.safeForLockScreen(requested)
        val definition = if (useFallback) {
            // Never leak personal data if a lock widget's stored design is stale or tampered with.
            WidgetRegistry.getById(WidgetProviders.defaultWidgetTypeIdFor(context, appWidgetId))
                ?: WidgetRegistry.getAll().first()
        } else {
            requested ?: WidgetRegistry.getAll().first()
        }
        val baseSpec = specFor(context, appWidgetId, instance)
        val spec = if (useFallback) baseSpec.copy(tapAction = definition.defaultTap) else baseSpec
        val sizeBucket = spec.sizeBucket
        val tapAction = spec.tapAction
        val tapAppPackage = instance.getString("tap_app_package").takeIf { it.isNotBlank() }

        val missingPermissions = definition.requiredPermissions.filterNot { it.isGranted(context) }
        val bitmap = if (missingPermissions.isNotEmpty()) {
            renderPermissionMessage(definition, spec, missingPermissions.first())
        } else {
            renderBitmap(context, definition, spec, appWidgetId)
        }

        // Scrollable lists and the Smart Stack use their own layouts and a RemoteViewsService.
        if (missingPermissions.isEmpty() && CollectionWidgets.isCollection(definition.id)) {
            val selectedStackCard = if (CollectionWidgets.isStack(definition.id)) {
                CollectionWidgets.selectedAutomaticStackCard(
                    context, appWidgetId, instance.configJson, refreshSmartStackSelection
                )
            } else null
            val stackItems = CollectionWidgets.stackItems(instance.configJson)
            val selectedStackChild = selectedStackCard?.let(stackItems::indexOf)?.takeIf { it >= 0 }
            val views = CollectionViews.build(
                context, appWidgetId, definition.id, spec, bitmap,
                selectedStackChild.takeIf { refreshSmartStackSelection }
            )
            views.setContentDescription(R.id.widget_image_view, definition.describe(context))
            return@withContext views to (
                bitmapSignature(bitmap) * 31 +
                    listOf(definition.id, definition.title, definition.renderVersion, instance.configJson, selectedStackChild).hashCode()
                )
        }

        val views = RemoteViews(context.packageName, R.layout.widget_canvas_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)
        views.setContentDescription(R.id.widget_image_view, definition.describe(context))
        // A non-classic style draws its own layout, so classic id-specific overlays and tap regions
        // (live clock digits, timers, animations, multi-zone taps) are switched off for it.
        val overlayId = com.example.widgets.styles.StyleCatalog.overlayId(definition.id, spec.configJson)
        applyLiveClock(views, overlayId, spec)
        applyLiveTimer(context, views, overlayId, spec)
        applyAnimation(context, views, overlayId, spec)

        val regions = WidgetTapTargets.regions(context, appWidgetId, overlayId, sizeBucket)
        if (regions == null) views.setViewVisibility(R.id.dynamic_regions, View.GONE)

        // Multi-region check for 2x2 toggles
        if (regions != null) {
            views.setViewVisibility(R.id.multi_region_container, View.GONE)
            WidgetTapTargets.applyRegions(context, views, regions)
        } else if (overlayId == "toggle.grid.2x2") {
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
                else -> createTapPendingIntent(context, appWidgetId, tapAction, tapAppPackage)
            }

            views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }

        // A missing permission replaces the design with a tappable prompt.
        val missingPermission = missingPermissions.firstOrNull()
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
            listOf(definition.id, definition.title, definition.renderVersion, instance.configJson, missingPermissions.map { it.id }, DeviceToggles.hasDndAccess(context)).hashCode()
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

    private val WAVE_FRAME_IDS = intArrayOf(
        R.id.wave_frame_0, R.id.wave_frame_1, R.id.wave_frame_2,
        R.id.wave_frame_3, R.id.wave_frame_4, R.id.wave_frame_5
    )

    /** Animated bars for a playing Soundwave; hidden otherwise (the bitmap shows a static wave). */
    /** Flipbook animation (soundwave, charging pulse, rain, ambient) when the widget has one right now. */
    private fun applyAnimation(context: Context, views: RemoteViews, widgetTypeId: String, spec: WidgetSpec) {
        val animation = AnimatedOverlays.frames(context, widgetTypeId, spec)
        if (animation == null) {
            views.setViewVisibility(R.id.soundwave_flipper, View.GONE)
            return
        }
        animation.frames.take(WAVE_FRAME_IDS.size).forEachIndexed { i, frame ->
            views.setImageViewBitmap(WAVE_FRAME_IDS[i], frame)
            views.setInt(WAVE_FRAME_IDS[i], "setColorFilter", animation.tint)
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
            "prod.countdown.timer", "prod.timer.presets" -> WidgetStateManager.getCountdownState(context)
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

    private fun createTapPendingIntent(
        context: Context,
        appWidgetId: Int,
        tapAction: TapAction,
        tapAppPackage: String? = null
    ): PendingIntent {
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
            TapAction.OPEN_APP -> {
                val launch = tapAppPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
                launch?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK } ?: Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("launch_widget_id", appWidgetId)
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
    suspend fun updateAllPlacedWidgets(
        context: Context,
        force: Boolean = false,
        refreshSmartStackSelection: Boolean = false
    ) = withContext(Dispatchers.IO) {
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
                val (views, signature) = buildWidget(
                    context, instance.appWidgetId, instance,
                    refreshSmartStackSelection = refreshSmartStackSelection
                )
                if (lastSignatures[instance.appWidgetId] == signature) continue
                manager.updateAppWidget(instance.appWidgetId, views)
                lastSignatures[instance.appWidgetId] = signature
                sent++
            } catch (e: Exception) {
                android.util.Log.e("WidgetRenderEngine", "Failed to update widget ${instance.appWidgetId}", e)
            }
        }

        // Lists/stacks change with time too (event ended, new day, stack clocks), so let the launcher re-read them.
        CollectionViews.notifyChanged(context, placed.associate { it.appWidgetId to it.widgetTypeId })
        android.util.Log.d("WidgetRenderEngine", "Refresh: sent $sent of ${placed.size} widgets (${placed.size - sent} unchanged)")
        PreferencesManager.getInstance(context).setLastWidgetRefresh(System.currentTimeMillis())
    }

    /** Re-selects automatic Smart Stack cards at a slot boundary without touching other widgets. */
    suspend fun updateAutomaticSmartStacks(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val manager = AppWidgetManager.getInstance(context)
        val updated = linkedMapOf<Int, String>()

        for ((_, ids) in WidgetProviders.placedIds(context)) {
            for (id in ids) {
                if (WidgetProviders.isLockScreenWidget(context, id)) continue
                val instance = db.widgetInstanceDao().getById(id) ?: continue
                if (!CollectionWidgets.isStack(instance.widgetTypeId) ||
                    CollectionWidgets.stackMode(instance.configJson) == com.example.widgets.collection.StackMode.MANUAL ||
                    CollectionWidgets.stackItems(instance.configJson).isEmpty()
                ) continue

                try {
                    val (views, signature) = buildWidget(
                        context, id, instance, refreshSmartStackSelection = true
                    )
                    manager.updateAppWidget(id, views)
                    lastSignatures[id] = signature
                    updated[id] = instance.widgetTypeId
                } catch (e: Exception) {
                    android.util.Log.e("WidgetRenderEngine", "Failed to rotate Smart Stack $id", e)
                }
            }
        }
        CollectionViews.notifyChanged(context, updated)
    }

    /** Number of Dot Widgets currently placed on the home screen. */
    fun placedWidgetCount(context: Context): Int =
        WidgetProviders.placedIds(context).sumOf { (_, ids) -> ids.size }
}
