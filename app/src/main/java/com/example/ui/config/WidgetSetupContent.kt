package com.example.ui.config

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.graphics.drawable.toBitmap
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.config.WidgetOption
import com.example.core.config.WidgetOptions
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.MaterialYouPalette
import com.example.core.theme.ThemeMode
import com.example.core.theme.WidgetAccent
import com.example.core.time.ClockOptions
import com.example.core.time.ClockSupport
import com.example.core.time.DateFormat
import com.example.core.time.DotStyle
import com.example.core.time.TimeFormat
import com.example.data.db.WidgetInstance
import com.example.data.photos.BackgroundImageStore
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.SegmentTabs
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.permissions.AppPermission
import com.example.ui.theme.DotTheme
import com.example.widgets.base.ShapeGroup
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetShape
import com.example.widgets.base.WidgetSpec
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.collection.StackMode
import com.example.widgets.collection.StackTimeSlot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class WidgetSetupState(
    val size: SizeBucket,
    val accent: AccentOption,
    val background: BackgroundMode,
    /** Background fill opacity, 0 = see-through, 1 = fully opaque. */
    val bgAlpha: Float = 1f,
    val theme: ThemeMode,
    val cornerRadius: Int,
    val showLabel: Boolean,
    val tapAction: TapAction,
    /** Package name launched when [tapAction] is [TapAction.OPEN_APP]; null = not chosen yet. */
    val tapAppPackage: String? = null,
    /** Null = the widget's default format (see [ClockSupport]). */
    val timeFormat: TimeFormat? = null,
    val dateFormat: DateFormat? = null,
    val dotStyle: DotStyle = DotStyle.ROUND,
    val skin: Skin = Skin.DOT,
    val frame: Frame = Frame.HAIRLINE,
    /** Smart Stack only: widget ids shown as swipeable cards, in order. */
    val stackItems: List<String> = emptyList(),
    val stackMode: StackMode = StackMode.MANUAL,
    val stackTimeSlots: Map<StackTimeSlot, String> = emptyMap(),
    /** IANA zone for world-clock widgets, null = phone's zone. */
    val timeZoneId: String? = null,
    /** Advanced-option values (Phase 8), stored under the `"opt"` config object. */
    val optionValues: Map<String, Any> = emptyMap(),
    /** A custom accent colour (ARGB); overrides [accent] when set. */
    val customAccent: Int? = null,
    /** Optional custom background photo with its placement and darkening; null = plain background. */
    val backgroundPhoto: BackgroundImageStore.Choice? = null,
    /** Outline of the widget; [WidgetShape.ROUNDED] keeps the original corner-radius look. */
    val shape: WidgetShape = WidgetShape.ROUNDED,
    /** Normalized outline points used when [shape] is [WidgetShape.CUSTOM]. */
    val customPoints: List<ShapePoint> = emptyList(),
    /** Shape fill colour override; null keeps the theme background. */
    val shapeFillColor: Int? = null,
    /** Shape border colour override; null keeps the theme edge colour. */
    val shapeBorderColor: Int? = null,
    /** Shape border style override; null keeps the Look frame. */
    val shapeBorderStyle: Frame? = null
) {
    fun toConfigJson(widgetTypeId: String, existingConfigJson: String? = null): String {
        val base = (runCatching { JSONObject(existingConfigJson ?: "{}") }.getOrElse { JSONObject() }).apply {
            put("accent", accent.name)
            if (customAccent != null) put(WidgetAccent.CUSTOM_KEY, WidgetAccent.toHex(customAccent)) else remove(WidgetAccent.CUSTOM_KEY)
            put("background", background.name)
            put("bg_alpha", (bgAlpha * 100).roundToInt())
            BackgroundImageStore.clearFrom(this)
            backgroundPhoto?.writeTo(this)
            put("theme_mode", theme.name)
            put("corner_radius", cornerRadius)
            put("shape", shape.name)
            if (shape == WidgetShape.CUSTOM) put("shape_points", WidgetShape.encodePoints(customPoints)) else remove("shape_points")
            if (shapeFillColor != null) put("shape_fill", WidgetAccent.toHex(shapeFillColor)) else remove("shape_fill")
            if (shapeBorderColor != null) put("shape_border", WidgetAccent.toHex(shapeBorderColor)) else remove("shape_border")
            if (shapeBorderStyle != null) put("shape_border_style", shapeBorderStyle.name) else remove("shape_border_style")
            put("show_label", showLabel)
            put("tap_action", tapAction.name)
            if (tapAppPackage != null) put("tap_app_package", tapAppPackage) else remove("tap_app_package")
            put("widget_type_id", widgetTypeId)
            put("size", size.name)
            put("configured", true)
            timeFormat?.let { put(ClockOptions.KEY_TIME, it.name) }
            dateFormat?.let { put(ClockOptions.KEY_DATE, it.name) }
            timeZoneId?.let { put(ClockOptions.KEY_TZ, it) }
            put(ClockOptions.KEY_DOT_STYLE, dotStyle.name)
            put(Skin.KEY, skin.name)
            put(Frame.KEY, frame.name)
            put(CollectionWidgets.STACK_KEY, JSONArray(stackItems))
            if (CollectionWidgets.isStack(widgetTypeId)) {
                put(CollectionWidgets.STACK_MODE_KEY, stackMode.name)
                put(
                    CollectionWidgets.STACK_TIME_SLOTS_KEY,
                    JSONObject().apply { stackTimeSlots.forEach { (slot, id) -> put(slot.name, id) } }
                )
            }
        }.toString()
        // Option values live in a nested object so they never collide with core config keys.
        return com.example.core.config.WidgetOptions.write(base, optionValues)
    }

    companion object {
        fun defaults(
            definition: WidgetDefinition,
            preferences: PreferencesManager,
            lockedSize: SizeBucket? = null,
            importedConfigJson: String? = null
        ): WidgetSetupState {
            val json = runCatching { JSONObject(importedConfigJson ?: "{}") }.getOrElse { JSONObject() }
            return WidgetSetupState(
                size = lockedSize ?: definition.sizes.first(),
                accent = AccentOption.fromName(json.optString("accent", preferences.getSavedAccent().name)),
                background = BackgroundMode.fromName(json.optString("background", preferences.getSavedBgMode().name)),
                bgAlpha = (json.optInt("bg_alpha", 100) / 100f).coerceIn(0f, 1f),
                theme = ThemeMode.fromName(json.optString("theme_mode", preferences.getSavedThemeMode().name)),
                cornerRadius = json.optInt("corner_radius", 20),
                showLabel = json.optBoolean("show_label", true),
                tapAction = runCatching {
                    TapAction.valueOf(json.optString("tap_action", definition.defaultTap.name))
                }.getOrDefault(definition.defaultTap),
                tapAppPackage = json.optString("tap_app_package").takeIf { it.isNotEmpty() },
                timeFormat = json.optString(ClockOptions.KEY_TIME).takeIf { it.isNotEmpty() }?.let { TimeFormat.fromName(it) },
                dateFormat = json.optString(ClockOptions.KEY_DATE).takeIf { it.isNotEmpty() }?.let { DateFormat.fromName(it) },
                dotStyle = DotStyle.fromName(json.optString(ClockOptions.KEY_DOT_STYLE)),
                skin = Skin.fromName(json.optString(Skin.KEY)),
                frame = Frame.fromName(json.optString(Frame.KEY)),
                stackItems = CollectionWidgets.stackItems(importedConfigJson ?: "{}"),
                stackMode = CollectionWidgets.stackMode(importedConfigJson ?: "{}"),
                stackTimeSlots = CollectionWidgets.stackTimeSlots(importedConfigJson ?: "{}"),
                timeZoneId = ClockOptions.timeZone(importedConfigJson ?: "{}"),
                optionValues = resolveOptions(definition, importedConfigJson),
                customAccent = WidgetAccent.custom(importedConfigJson),
                backgroundPhoto = BackgroundImageStore.fromConfig(importedConfigJson),
                shape = WidgetShape.fromName(json.optString("shape")),
                customPoints = WidgetShape.decodePoints(json.optString("shape_points")),
                shapeFillColor = WidgetAccent.parseHex(json.optString("shape_fill")),
                shapeBorderColor = WidgetAccent.parseHex(json.optString("shape_border")),
                shapeBorderStyle = json.optString("shape_border_style").takeIf { it.isNotEmpty() }?.let { Frame.fromName(it) }
            )
        }

        fun fromInstance(
            instance: WidgetInstance,
            definition: WidgetDefinition,
            preferences: PreferencesManager,
            lockedSize: SizeBucket? = null
        ): WidgetSetupState {
            val json = runCatching { JSONObject(instance.configJson) }.getOrElse { JSONObject() }
            return WidgetSetupState(
                size = lockedSize
                    ?: runCatching { SizeBucket.valueOf(json.optString("size")) }.getOrNull()
                        ?.takeIf { it in definition.sizes }
                    ?: definition.sizes.first(),
                accent = AccentOption.fromName(json.optString("accent", preferences.getSavedAccent().name)),
                background = BackgroundMode.fromName(json.optString("background", preferences.getSavedBgMode().name)),
                bgAlpha = (json.optInt("bg_alpha", 100) / 100f).coerceIn(0f, 1f),
                theme = ThemeMode.fromName(json.optString("theme_mode", preferences.getSavedThemeMode().name)),
                cornerRadius = json.optInt("corner_radius", 20),
                showLabel = json.optBoolean("show_label", true),
                tapAction = runCatching {
                    TapAction.valueOf(json.optString("tap_action", definition.defaultTap.name))
                }.getOrDefault(definition.defaultTap),
                tapAppPackage = json.optString("tap_app_package").takeIf { it.isNotEmpty() },
                timeFormat = json.optString(ClockOptions.KEY_TIME).takeIf { it.isNotEmpty() }?.let { TimeFormat.fromName(it) },
                dateFormat = json.optString(ClockOptions.KEY_DATE).takeIf { it.isNotEmpty() }?.let { DateFormat.fromName(it) },
                dotStyle = DotStyle.fromName(json.optString(ClockOptions.KEY_DOT_STYLE)),
                skin = Skin.fromName(json.optString(Skin.KEY)),
                frame = Frame.fromName(json.optString(Frame.KEY)),
                stackItems = CollectionWidgets.stackItems(instance.configJson),
                stackMode = CollectionWidgets.stackMode(instance.configJson),
                stackTimeSlots = CollectionWidgets.stackTimeSlots(instance.configJson),
                timeZoneId = ClockOptions.timeZone(instance.configJson),
                optionValues = resolveOptions(definition, instance.configJson),
                customAccent = WidgetAccent.custom(instance.configJson),
                backgroundPhoto = BackgroundImageStore.fromConfig(instance.configJson),
                shape = WidgetShape.fromName(json.optString("shape")),
                customPoints = WidgetShape.decodePoints(json.optString("shape_points")),
                shapeFillColor = WidgetAccent.parseHex(json.optString("shape_fill")),
                shapeBorderColor = WidgetAccent.parseHex(json.optString("shape_border")),
                shapeBorderStyle = json.optString("shape_border_style").takeIf { it.isNotEmpty() }?.let { Frame.fromName(it) }
            )
        }

        /**
         * Reads each declared option's current value from [configJson] (falling back to its default),
         * so the ADVANCED controls always show a value and the renderer always gets one.
         */
        fun resolveOptions(
            definition: WidgetDefinition,
            configJson: String?
        ): Map<String, Any> {
            if (definition.options.isEmpty()) return emptyMap()
            val parsed = WidgetOptions.parse(configJson ?: "{}")
            val out = LinkedHashMap<String, Any>()
            for (option in definition.options) {
                out[option.key] = when (option) {
                    is WidgetOption.Toggle -> parsed.boolean(option.key, option.default)
                    is WidgetOption.Choice -> parsed.string(option.key, option.default)
                        .takeIf { v -> option.options.any { it.first == v } } ?: option.default
                    is WidgetOption.Slider -> parsed.int(option.key, option.default)
                        .coerceIn(option.min, option.max)
                    is WidgetOption.Text -> parsed.string(option.key, option.default)
                    is WidgetOption.ColorRule -> parsed.boolean(option.key, option.default)
                }
            }
            return out
        }

        fun isConfigured(instance: WidgetInstance): Boolean {
            val json = runCatching { JSONObject(instance.configJson) }.getOrNull() ?: return false
            return json.optBoolean("configured", false) || json.has("widget_type_id")
        }
    }
}


/**
 * The widget customization screen: a pinned live preview, the options in scrolling sections
 * (content, format, style, size, colour, shape, advanced, tap) and a pinned save bar.
 * Used by the in-app sheet and by the launcher's configure activity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetSetupContent(
    definition: WidgetDefinition,
    initial: WidgetSetupState,
    lockedSize: SizeBucket?,
    saveLabel: String,
    saveError: String? = null,
    onStateChange: (WidgetSetupState) -> Unit = {},
    /** Shown as a second action for lock-safe widgets when set (the in-app sheet). */
    onAddToLockScreen: ((WidgetSetupState) -> Unit)? = null,
    onSave: (WidgetSetupState) -> Unit
) {
    val context = LocalContext.current
    var state by remember(definition.id, initial, lockedSize) {
        mutableStateOf(initial.copy(size = lockedSize ?: initial.size))
    }
    var contentVersion by remember { mutableIntStateOf(0) }
    var showAppPicker by remember(definition.id) { mutableStateOf(false) }
    val isDark = when (state.theme) {
        ThemeMode.FORCE_DARK -> true
        ThemeMode.FORCE_LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val dimensions = remember(state.size) { SizeResolver.resolveDimensions(context, state.size) }
    val resolvedAccent = state.customAccent ?: if (state.accent == AccentOption.DYNAMIC) {
        MaterialYouPalette.resolve(context)
    } else {
        state.accent.hex
    }
    val spec = WidgetSpec(
        widthPx = dimensions.widthPx,
        heightPx = dimensions.heightPx,
        density = dimensions.density,
        sizeBucket = state.size,
        accent = state.accent,
        accentColor = resolvedAccent,
        bgMode = state.background,
        bgAlpha = state.bgAlpha,
        themeMode = state.theme,
        // Paper is always light; Neon and Terminal always dark (matches the home screen).
        isDark = state.skin.resolveDark(isDark),
        cornerRadiusDp = state.cornerRadius,
        showLabel = state.showLabel,
        tapAction = state.tapAction,
        // So the preview reflects format and dot-style choices.
        configJson = state.toConfigJson(definition.id),
        shape = state.shape,
        customPoints = state.customPoints,
        shapeFillColor = state.shapeFillColor,
        shapeBorderColor = state.shapeBorderColor,
        shapeBorderStyle = state.shapeBorderStyle
    )
    // Controls use the app accent so the screen stays consistent whatever colour the widget gets.
    val ui = MaterialTheme.colorScheme.primary
    val update: (WidgetSetupState) -> Unit = { next ->
        state = next
        onStateChange(next)
    }

    val sizes = remember(definition.id) { definition.sizes.toList() }
    val canPageSizes = lockedSize == null && sizes.size > 1
    val scope = rememberCoroutineScope()
    var tab by remember(definition.id) { mutableStateOf(SetupTab.SKIN) }
    val startPage = sizes.indexOf(state.size).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { if (canPageSizes) sizes.size else 1 })
    // Swiping the preview is the same choice as tapping a size card.
    LaunchedEffect(pagerState.currentPage, canPageSizes) {
        if (canPageSizes) {
            val target = sizes.getOrNull(pagerState.currentPage)
            if (target != null && target != state.size) update(state.copy(size = target))
        }
    }
    val scrollState = rememberScrollState()
    // Each tab starts at the top, like a fresh page.
    LaunchedEffect(tab) { scrollState.scrollTo(0) }

    // The preview and tabs stay pinned; only the selected tab's options scroll. Its own surface, so
    // it reads correctly whatever window or sheet hosts it.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        // On short windows (landscape, large font) keep a smaller preview and drop the caption so
        // the tab content still gets room.
        val compact = constraints.hasBoundedHeight && maxHeight < 520.dp
        val previewHeight = when {
            !constraints.hasBoundedHeight -> 160.dp
            compact -> (maxHeight * 0.24f).coerceIn(80.dp, 120.dp)
            else -> (maxHeight * 0.30f).coerceIn(96.dp, 176.dp)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 4.dp, bottom = if (compact) 4.dp else 8.dp)) {
                Text(
                    definition.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!compact) {
                    Text(
                        definition.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            PreviewStage {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight)
                        .testTag("config_live_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    if (canPageSizes) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            SetupPreview(
                                definition = definition,
                                state = state,
                                size = sizes[page],
                                accentColor = resolvedAccent,
                                isDark = isDark,
                                refreshKey = contentVersion,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        SetupPreview(
                            definition = definition,
                            state = state,
                            size = state.size,
                            accentColor = resolvedAccent,
                            isDark = isDark,
                            refreshKey = contentVersion,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            if (canPageSizes) PageDots(count = sizes.size, selected = pagerState.currentPage)
            Spacer(Modifier.height(12.dp))
            SegmentTabs(
                items = SetupTab.entries.map { it.label },
                selectedIndex = tab.ordinal,
                onSelect = { tab = SetupTab.entries[it] },
                modifier = Modifier.padding(horizontal = PagePadding),
                tagPrefix = "setup_tab"
            )

            // ---- Scrolling options for the selected tab
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(top = 4.dp, bottom = 16.dp)
                    .testTag("setup_scroll")
            ) {
                definition.requiredPermissions.forEach { permission ->
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.padding(horizontal = PagePadding)) {
                        PermissionBanner(permission = permission, accentColor = ui)
                    }
                }

                if (tab == SetupTab.SIZE) {
                    // Notes, to-dos, shortcuts, photos... (only for widgets with their own content)
                    Box(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 16.dp)) {
                        WidgetContentEditor(definition.id, state.accent) { contentVersion++ }
                    }

                    if (CollectionWidgets.isStack(definition.id)) {
                        Box(Modifier.padding(horizontal = PagePadding)) { StackItemsSection(state, ui, update) }
                    }

                    ClockFormatSection(definition.id, state, ui, update)
                }

                if (tab == SetupTab.SKIN) {
                    SetupSection("Skin", trailing = state.skin.label) {
                        SkinCarousel(definition, spec, state.skin, ui) { update(state.copy(skin = it)) }
                    }

                    SetupSection("Frame") {
                        CarouselRow(spacing = 8.dp) {
                            Frame.entries.forEach { f ->
                                SelectChip(
                                    label = f.label,
                                    selected = f == state.frame,
                                    accent = ui,
                                    modifier = Modifier.testTag("frame_${f.name.lowercase()}"),
                                    leading = { FrameGlyph(f, Color(resolvedAccent)) }
                                ) { update(state.copy(frame = f)) }
                            }
                        }
                    }

                    // Dot shapes only apply to the dot-based skins.
                    AnimatedVisibility(visible = state.skin == Skin.DOT || state.skin == Skin.PAPER || state.skin == Skin.NEON) {
                        SetupSection("Dot style") {
                            CarouselRow(spacing = 8.dp) {
                                DotStyle.entries.forEach { style ->
                                    SelectChip(style.label, style == state.dotStyle, ui) { update(state.copy(dotStyle = style)) }
                                }
                            }
                        }
                    }
                }

                if (tab == SetupTab.SIZE && lockedSize == null && definition.sizes.size > 1) {
                    SetupSection("Size") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            definition.sizes.forEach { size ->
                                SelectCard(
                                    label = size.label,
                                    caption = size.cells,
                                    selected = size == state.size,
                                    accent = ui,
                                    modifier = Modifier.weight(1f).testTag("size_${size.name.lowercase()}"),
                                    onClick = {
                                        update(state.copy(size = size))
                                        scope.launch { pagerState.animateScrollToPage(sizes.indexOf(size)) }
                                    }
                                ) { SizeGlyph(size, size == state.size, ui) }
                            }
                        }
                    }
                }

                if (tab == SetupTab.ACCENT) {
                    SetupSection(
                        "Accent colour",
                        trailing = if (state.customAccent != null) WidgetAccent.toHex(state.customAccent!!) else state.accent.displayName
                    ) {
                        AccentSwatches(
                            selected = state.accent,
                            customAccent = state.customAccent,
                            onSelect = { update(state.copy(accent = it, customAccent = null)) },
                            onCustom = { update(state.copy(customAccent = it)) }
                        )
                    }
                }

                if (tab == SetupTab.SHAPE) {
                SetupSection("Background") {
                    SegmentedControl(
                        options = BackgroundMode.entries.map { SegmentOption(it, it.displayName) },
                        selected = state.background,
                        accent = ui,
                        tagPrefix = "background"
                    ) { update(state.copy(background = it)) }
                    AnimatedVisibility(visible = state.background != BackgroundMode.TRANSPARENT) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Opacity", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Text(
                                    "${(state.bgAlpha * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ui,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ui.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .testTag("opacity_value")
                                )
                            }
                            SlimSlider(
                                value = state.bgAlpha,
                                onValueChange = { update(state.copy(bgAlpha = it)) },
                                accent = ui,
                                valueRange = 0.1f..1f,
                                modifier = Modifier.testTag("opacity_slider")
                            )
                        }
                    }
                }

                    // Theme widgets paint their own full artwork, so they don't take a photo.
                    if (definition.category != com.example.widgets.base.Category.THEMES) {
                        BackgroundImageSection(state, spec, ui, update)
                    }

                    val shapeGroups = ShapeGroup.entries
                    val groupLabels = listOf("Basic", "Shapes", "Fun", "Special", "Custom")
                    var shapeGroupIndex by remember(definition.id) {
                        mutableIntStateOf(shapeGroups.indexOf(state.shape.group).coerceAtLeast(0))
                    }
                    SetupSection("Shape", trailing = state.shape.label) {
                        LaunchedEffect(state.shape) {
                            shapeGroupIndex = shapeGroups.indexOf(state.shape.group).coerceAtLeast(0)
                        }
                        SegmentTabs(
                            items = groupLabels,
                            selectedIndex = shapeGroupIndex,
                            onSelect = { shapeGroupIndex = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                            tagPrefix = "shape_group"
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                            maxItemsInEachRow = 4,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WidgetShape.entries
                                .filter { it.group == shapeGroups[shapeGroupIndex] }
                                .forEach { option ->
                                    val selected = option == state.shape
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) Color(resolvedAccent).copy(alpha = 0.14f)
                                                else DotTheme.colors.surfaceSecondary.copy(alpha = 0.45f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) Color(resolvedAccent) else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                update(
                                                    state.copy(
                                                        shape = option,
                                                        customPoints = if (option == WidgetShape.CUSTOM && state.customPoints.isEmpty()) {
                                                            WidgetShape.defaultCustomPoints()
                                                        } else state.customPoints
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 4.dp, vertical = 10.dp)
                                            .testTag("shape_${option.name.lowercase()}")
                                    ) {
                                        WidgetShapeGlyph(
                                            shape = option,
                                            accent = Color(resolvedAccent),
                                            customPoints = state.customPoints.ifEmpty { WidgetShape.defaultCustomPoints() },
                                            modifier = Modifier.size(width = 44.dp, height = 30.dp)
                                        )
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) Color(resolvedAccent) else DotTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                        }
                        AnimatedVisibility(visible = state.shape == WidgetShape.CUSTOM) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 16.dp)) {
                                CustomShapeEditor(
                                    points = state.customPoints.ifEmpty { WidgetShape.defaultCustomPoints() },
                                    accent = Color(resolvedAccent),
                                    onPointsChange = { update(state.copy(customPoints = it)) },
                                    modifier = Modifier.testTag("custom_shape_editor")
                                )
                            }
                        }
                    }

                    SetupSection("Shape style") {
                        ShapeColorRow(
                            label = "Fill colour",
                            color = state.shapeFillColor,
                            tag = "shape_fill_color",
                            onPick = { update(state.copy(shapeFillColor = it)) },
                            onClear = { update(state.copy(shapeFillColor = null)) }
                        )
                        ShapeColorRow(
                            label = "Border colour",
                            color = state.shapeBorderColor,
                            tag = "shape_border_color",
                            onPick = { update(state.copy(shapeBorderColor = it)) },
                            onClear = { update(state.copy(shapeBorderColor = null)) }
                        )
                        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 8.dp)) {
                            Text(
                                "Border style",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            val borderOptions = listOf<Frame?>(null) + Frame.entries
                            SegmentTabs(
                                items = listOf("Look") + Frame.entries.map { it.label },
                                selectedIndex = borderOptions.indexOf(state.shapeBorderStyle),
                                onSelect = { update(state.copy(shapeBorderStyle = borderOptions[it])) },
                                modifier = Modifier.fillMaxWidth(),
                                tagPrefix = "shape_border_style"
                            )
                        }
                        AnimatedVisibility(
                            visible = state.shape in setOf(
                                WidgetShape.ROUNDED, WidgetShape.CUT, WidgetShape.BEVEL, WidgetShape.CUSTOM
                            )
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 16.dp)) {
                                Text(
                                    "Corner radius",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                SegmentedControl(
                                    // FULL_ROUND makes a pill (or a circle on square widgets).
                                    options = listOf(0, 12, 20, 28, FULL_ROUND).map { SegmentOption(it, if (it == FULL_ROUND) "Full" else "$it") },
                                    selected = state.cornerRadius,
                                    accent = ui,
                                    tagPrefix = "corner"
                                ) { update(state.copy(cornerRadius = it)) }
                            }
                        }
                    }

                    SetupSection("Details") {
                        SettingsGroup {
                            ToggleRow(
                                title = "Show label",
                                subtitle = "The small uppercase title on the widget",
                                checked = state.showLabel,
                                accent = ui
                            ) { update(state.copy(showLabel = it)) }
                        }
                    }
                }

                if (tab == SetupTab.ACCENT) {
                    SetupSection("Theme") {
                        SegmentedControl(
                            options = listOf(
                                SegmentOption(ThemeMode.AUTO, "System", Icons.Default.BrightnessAuto),
                                SegmentOption(ThemeMode.FORCE_DARK, "Dark", Icons.Default.DarkMode),
                                SegmentOption(ThemeMode.FORCE_LIGHT, "Light", Icons.Default.LightMode)
                            ),
                            selected = state.theme,
                            accent = ui,
                            tagPrefix = "theme"
                        ) { update(state.copy(theme = it)) }
                    }
                }

                if (tab == SetupTab.SIZE) {
                    if (definition.options.isNotEmpty()) {
                        AdvancedOptionsSection(definition = definition, state = state, accent = ui, update = update)
                    }

                    SetupSection("Tap action") {
                        SettingsGroup {
                            val actions = listOf(
                                TapAction.DEFAULT, TapAction.OPEN_CLOCK, TapAction.OPEN_CALENDAR,
                                TapAction.OPEN_BATTERY, TapAction.OPEN_WEATHER, TapAction.OPEN_APP,
                                TapAction.NONE
                            )
                            actions.forEachIndexed { i, action ->
                                RadioRow(action.displayName, action == state.tapAction, ui) {
                                    if (action == TapAction.OPEN_APP) showAppPicker = true
                                    update(state.copy(tapAction = action))
                                }
                                if (i < actions.lastIndex) GroupDivider()
                            }
                            if (state.tapAction == TapAction.OPEN_APP) {
                                GroupDivider()
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { showAppPicker = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "App",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            state.tapAppPackage?.let { appLabel(context, it) ?: it } ?: "Choose an app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = mutedText()
                                        )
                                    }
                                    Text("Change", style = MaterialTheme.typography.labelLarge, color = ui)
                                }
                            }
                        }
                    }
                    if (showAppPicker) {
                        AppPickerDialog(
                            onDismiss = { showAppPicker = false },
                            onPick = { pkg ->
                                update(state.copy(tapAction = TapAction.OPEN_APP, tapAppPackage = pkg))
                                showAppPicker = false
                            }
                        )
                    }
                }
            }

            // ---- Pinned save bar
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = PagePadding, vertical = 12.dp)
            ) {
                saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        if (com.example.data.billing.Entitlements.hasFullAccess(context)) onSave(state)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pin_widget_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ui, contentColor = com.example.ui.theme.contentColorOn(ui)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(saveLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (onAddToLockScreen != null && definition.lockSafe) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAddToLockScreen(state) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("lock_screen_button"),
                        colors = com.example.ui.components.tonalButtonColors(ui),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Add to lock screen", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** The four customization pages, in the order the mockup shows them. */
private enum class SetupTab(val label: String) {
    SKIN("Skin"),
    SHAPE("Shape"),
    ACCENT("Accent"),
    SIZE("Size")
}

/** One page of the pinned preview pager, rendered at that size's real proportions. */
@Composable
private fun SetupPreview(
    definition: WidgetDefinition,
    state: WidgetSetupState,
    size: SizeBucket,
    accentColor: Int,
    isDark: Boolean,
    refreshKey: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dims = remember(size) { SizeResolver.resolveDimensions(context, size) }
    val pageSpec = remember(size, state, accentColor, isDark) {
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = state.accent,
            accentColor = accentColor,
            bgMode = state.background,
            bgAlpha = state.bgAlpha,
            themeMode = state.theme,
            isDark = state.skin.resolveDark(isDark),
            cornerRadiusDp = state.cornerRadius,
            showLabel = state.showLabel,
            tapAction = state.tapAction,
            configJson = state.copy(size = size).toConfigJson(definition.id),
            shape = state.shape,
            customPoints = state.customPoints,
            shapeFillColor = state.shapeFillColor,
            shapeBorderColor = state.shapeBorderColor,
            shapeBorderStyle = state.shapeBorderStyle
        )
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val aspect = pageSpec.widthPx.toFloat() / pageSpec.heightPx.coerceAtLeast(1)
        WidgetBitmapPreview(
            definition = definition,
            spec = pageSpec,
            modifier = Modifier.width(minOf(maxWidth, maxHeight * aspect)),
            refreshKey = refreshKey
        )
    }
}

/** Page indicator under the size pager. */
@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(if (index == selected) 18.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) MaterialTheme.colorScheme.primary else mutedText().copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun PermissionBanner(permission: AppPermission, accentColor: Color) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        version++
    }
    LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose { }
    }
    val granted = remember(version) { permission.isGranted(context) }
    if (granted || !permission.isApplicable()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(permission.icon, contentDescription = null, tint = accentColor, modifier = Modifier.width(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${permission.title} needed",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText()
            )
        }
        Button(
            onClick = {
                when (permission) {
                    is AppPermission.Runtime -> launcher.launch(permission.permissions)
                    is AppPermission.SettingsPage -> permission.intent(context)?.let {
                        runCatching { context.startActivity(it) }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = com.example.ui.theme.contentColorOn(accentColor)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Allow", fontWeight = FontWeight.Bold)
        }
    }
}

/** Time and date format choices for clock widgets; shows nothing for other widgets. */
@Composable
private fun ClockFormatSection(widgetId: String, state: WidgetSetupState, accent: Color, update: (WidgetSetupState) -> Unit) {
    val timeFormats = ClockSupport.timeFormats(widgetId)
    if (timeFormats.isNotEmpty()) {
        val selected = state.timeFormat?.takeIf { it in timeFormats } ?: ClockSupport.defaultTime(widgetId)
        SetupSection("Time format", trailing = if (selected.hasSeconds) "Seconds tick live" else null) {
            CarouselRow(spacing = 8.dp) {
                timeFormats.forEach { format ->
                    SelectChip(format.label, format == selected, accent) { update(state.copy(timeFormat = format)) }
                }
            }
        }
    }

    if (ClockSupport.showsDate(widgetId)) {
        val selected = state.dateFormat ?: ClockSupport.defaultDate(widgetId, state.size == SizeBucket.S)
        SetupSection("Date format") {
            CarouselRow(spacing = 8.dp) {
                DateFormat.entries.forEach { format ->
                    SelectChip(format.label, format == selected, accent) { update(state.copy(dateFormat = format)) }
                }
            }
        }
    }

    if (widgetId in ClockSupport.TIME_ZONE_PICKERS) {
        TimeZonePicker(
            selectedZone = state.timeZoneId,
            accentColor = accent,
            onPick = { update(state.copy(timeZoneId = it)) }
        )
    }
}

/** Curated list of common IANA zones; enough for a second-city clock without a giant list. */
private val COMMON_TIME_ZONES = listOf(
    "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
    "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
    "America/Sao_Paulo", "Africa/Cairo", "Africa/Lagos", "Asia/Dubai",
    "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka", "Asia/Bangkok",
    "Asia/Shanghai", "Asia/Singapore", "Asia/Tokyo", "Asia/Seoul",
    "Australia/Sydney", "Pacific/Auckland"
)

@Composable
private fun TimeZonePicker(selectedZone: String?, accentColor: Color, onPick: (String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val label = selectedZone?.substringAfterLast("/")?.replace("_", " ") ?: "Pick a city"
    SetupSection("Time zone") {
        CarouselRow(spacing = 8.dp) {
            SelectChip("Phone", selectedZone == null, accentColor) { onPick(null) }
            SelectChip(label, selectedZone != null, accentColor) { showDialog = true }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text("Pick a time zone") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(COMMON_TIME_ZONES) { zone ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(zone)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                zone.substringAfterLast("/").replace("_", " "),
                                modifier = Modifier.weight(1f)
                            )
                            Text(zone, style = MaterialTheme.typography.bodySmall, color = mutedText())
                        }
                    }
                }
            }
        )
    }
}

/** Choose up to [CollectionWidgets.MAX_STACK] designs (that fit the stack's size) as Smart Stack cards. */
@Composable
private fun StackItemsSection(state: WidgetSetupState, accent: Color, update: (WidgetSetupState) -> Unit) {
    val candidates = remember(state.size) {
        WidgetRegistry.getAll().filter { state.size in it.sizes && !CollectionWidgets.isCollection(it.id) }
    }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            "STACK CARDS • ${state.stackItems.size}/${CollectionWidgets.MAX_STACK}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = mutedText()
        )
        Spacer(Modifier.height(8.dp))
        Text("ROTATION MODE", style = MaterialTheme.typography.labelMedium, color = mutedText())
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                StackMode.MANUAL to "Manual",
                StackMode.TIME_OF_DAY to "Time of day",
                StackMode.SMART to "Smart"
            ).forEach { (mode, label) ->
                SelectChip(label, state.stackMode == mode, accent) {
                    update(state.copy(stackMode = mode))
                }
            }
        }
        if (state.stackMode != StackMode.MANUAL) {
            Spacer(Modifier.height(8.dp))
            Text(
                "The stack auto-selects this slot at its boundary. Swipes remain manual until the next boundary.",
                style = MaterialTheme.typography.bodySmall,
                color = mutedText()
            )
            StackTimeSlot.entries.forEach { slot ->
                val hours = when (slot) {
                    StackTimeSlot.MORNING -> "05:00–10:59"
                    StackTimeSlot.DAY -> "11:00–16:59"
                    StackTimeSlot.EVENING -> "17:00–21:59"
                    StackTimeSlot.NIGHT -> "22:00–04:59"
                }
                Text(
                    "${slot.label.uppercase()} • $hours",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = mutedText()
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.stackItems.forEach { id ->
                        val title = WidgetRegistry.getById(id)?.title ?: id
                        SelectChip(
                            title,
                            state.stackTimeSlots[slot] == id ||
                                (state.stackTimeSlots[slot] == null && state.stackItems.firstOrNull() == id),
                            accent
                        ) {
                            update(state.copy(stackTimeSlots = state.stackTimeSlots + (slot to id)))
                        }
                    }
                }
            }
            if (state.stackMode == StackMode.SMART) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "At each slot boundary, Smart prefers a calendar card for an event within 30 minutes, then a battery card below 20% or while charging; otherwise it uses the time-slot choice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedText()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        state.stackItems.forEachIndexed { i, id ->
            val def = WidgetRegistry.getById(id) ?: return@forEachIndexed
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${i + 1}. ${def.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    update(
                        state.copy(
                            stackItems = state.stackItems - id,
                            stackTimeSlots = state.stackTimeSlots.filterValues { it != id }
                        )
                    )
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove card", tint = mutedText())
                }
            }
        }
        if (state.stackItems.size < CollectionWidgets.MAX_STACK) {
            OutlinedTextField(
                colors = com.example.ui.components.dotFieldColors(),
                value = query,
                onValueChange = { query = it },
                label = { Text("Search a widget to add") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val q = query.trim().lowercase()
            candidates.filter { it.id !in state.stackItems && (q.isEmpty() || it.title.lowercase().contains(q)) }.take(6).forEach { def ->
                Text(
                    text = "+ ${def.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { update(state.copy(stackItems = state.stackItems + def.id)) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

/** Corner radius large enough to fully round any widget edge. */
private const val FULL_ROUND = 999

private data class InstalledApp(val label: String, val packageName: String)

private fun appLabel(context: Context, packageName: String): String? = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrNull()

/** Lets the user pick any installed, launchable app to open when the widget is tapped. */
@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val apps = remember {
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val infos = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            infos.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                InstalledApp(info.loadLabel(pm).toString(), pkg)
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        }.getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open an app on tap") },
        text = {
            if (apps.isEmpty()) {
                Text("No launchable apps found.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.packageName) }
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = remember(app.packageName) {
                                runCatching {
                                    pm.getApplicationIcon(app.packageName).toBitmap(64, 64).asImageBitmap()
                                }.getOrNull()
                            }
                            if (icon != null) {
                                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
