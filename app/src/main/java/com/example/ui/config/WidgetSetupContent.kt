package com.example.ui.config

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.config.WidgetOption
import com.example.core.config.WidgetOptions
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.MaterialYouPalette
import com.example.core.theme.ThemeMode
import com.example.core.time.ClockOptions
import com.example.core.time.ClockSupport
import com.example.core.time.DateFormat
import com.example.core.time.DotStyle
import com.example.core.time.TimeFormat
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import org.json.JSONObject
import kotlin.math.roundToInt

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
    /** Null = the widget's default format (see [ClockSupport]). */
    val timeFormat: TimeFormat? = null,
    val dateFormat: DateFormat? = null,
    val dotStyle: DotStyle = DotStyle.ROUND,
    /** IANA zone for world-clock widgets, null = phone's zone. */
    val timeZoneId: String? = null,
    /** Advanced-option values (Phase 8), stored under the `"opt"` config object. */
    val optionValues: Map<String, Any> = emptyMap()
) {
    fun toConfigJson(widgetTypeId: String, existingConfigJson: String? = null): String {
        val base = (runCatching { JSONObject(existingConfigJson ?: "{}") }.getOrElse { JSONObject() }).apply {
            put("accent", accent.name)
            put("background", background.name)
            put("bg_alpha", (bgAlpha * 100).roundToInt())
            put("theme_mode", theme.name)
            put("corner_radius", cornerRadius)
            put("show_label", showLabel)
            put("tap_action", tapAction.name)
            put("widget_type_id", widgetTypeId)
            put("size", size.name)
            put("configured", true)
            timeFormat?.let { put(ClockOptions.KEY_TIME, it.name) }
            dateFormat?.let { put(ClockOptions.KEY_DATE, it.name) }
            timeZoneId?.let { put(ClockOptions.KEY_TZ, it) }
            put(ClockOptions.KEY_DOT_STYLE, dotStyle.name)
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
                timeFormat = json.optString(ClockOptions.KEY_TIME).takeIf { it.isNotEmpty() }?.let { TimeFormat.fromName(it) },
                dateFormat = json.optString(ClockOptions.KEY_DATE).takeIf { it.isNotEmpty() }?.let { DateFormat.fromName(it) },
                dotStyle = DotStyle.fromName(json.optString(ClockOptions.KEY_DOT_STYLE)),
                timeZoneId = ClockOptions.timeZone(importedConfigJson ?: "{}"),
                optionValues = resolveOptions(definition, importedConfigJson)
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
                timeFormat = json.optString(ClockOptions.KEY_TIME).takeIf { it.isNotEmpty() }?.let { TimeFormat.fromName(it) },
                dateFormat = json.optString(ClockOptions.KEY_DATE).takeIf { it.isNotEmpty() }?.let { DateFormat.fromName(it) },
                dotStyle = DotStyle.fromName(json.optString(ClockOptions.KEY_DOT_STYLE)),
                timeZoneId = ClockOptions.timeZone(instance.configJson),
                optionValues = resolveOptions(definition, instance.configJson)
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

@Composable
fun WidgetSetupContent(
    definition: WidgetDefinition,
    initial: WidgetSetupState,
    lockedSize: SizeBucket?,
    saveLabel: String,
    saveError: String? = null,
    onStateChange: (WidgetSetupState) -> Unit = {},
    onSave: (WidgetSetupState) -> Unit
) {
    val context = LocalContext.current
    var state by remember(definition.id, initial, lockedSize) {
        mutableStateOf(initial.copy(size = lockedSize ?: initial.size))
    }
    var contentVersion by remember { mutableIntStateOf(0) }
    val isDark = when (state.theme) {
        ThemeMode.FORCE_DARK -> true
        ThemeMode.FORCE_LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val dimensions = remember(state.size) { SizeResolver.resolveDimensions(context, state.size) }
    val resolvedAccent = if (state.accent == AccentOption.DYNAMIC) {
        MaterialYouPalette.resolve(context)
    } else {
        state.accent.hex
    }
    val uiAccent = Color(resolvedAccent)
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
        isDark = isDark,
        cornerRadiusDp = state.cornerRadius,
        showLabel = state.showLabel,
        tapAction = state.tapAction,
        // So the preview reflects format and dot-style choices.
        configJson = state.toConfigJson(definition.id)
    )
    val update: (WidgetSetupState) -> Unit = { next ->
        state = next
        onStateChange(next)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(definition.title.uppercase(), style = MaterialTheme.typography.titleLarge)
        Text(definition.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8A8A8A))
        definition.requiredPermission?.let { permission ->
            Spacer(Modifier.height(16.dp))
            PermissionBanner(permission = permission, accentColor = uiAccent)
        }
        Spacer(Modifier.height(16.dp))
        WidgetBitmapPreview(
            definition = definition,
            spec = spec,
            modifier = Modifier.fillMaxWidth().testTag("config_live_preview"),
            refreshKey = contentVersion
        )
        Spacer(Modifier.height(24.dp))
        WidgetContentEditor(definition.id, state.accent) { contentVersion++ }

        ClockFormatSection(definition.id, state, update)

        SetupSectionLabel("DOT STYLE")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DotStyle.entries.forEach { style ->
                SetupChip(style == state.dotStyle, style.label, uiAccent) {
                    update(state.copy(dotStyle = style))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (lockedSize == null && definition.sizes.size > 1) {
            SetupSectionLabel("SIZE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                definition.sizes.forEach { size ->
                    SetupChip(size == state.size, "${size.label} (${size.cells})", uiAccent) {
                        update(state.copy(size = size))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SetupSectionLabel("ACCENT COLOR")
        AccentSelectorRow(selected = state.accent, onSelect = { update(state.copy(accent = it)) })
        Spacer(Modifier.height(20.dp))

        SetupSectionLabel("BACKGROUND MODE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundMode.entries.forEach { mode ->
                SetupChip(mode == state.background, mode.displayName, uiAccent) {
                    update(state.copy(background = mode))
                }
            }
        }
        if (state.background != BackgroundMode.TRANSPARENT) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OPACITY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8A8A))
                Text("${(state.bgAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8A8A))
            }
            Slider(
                value = state.bgAlpha,
                onValueChange = { update(state.copy(bgAlpha = it)) },
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = uiAccent,
                    activeTrackColor = uiAccent
                )
            )
        }
        Spacer(Modifier.height(20.dp))

        SetupSectionLabel("THEME OVERRIDE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                SetupChip(mode == state.theme, mode.displayName, uiAccent) {
                    update(state.copy(theme = mode))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        SetupSectionLabel("CORNER RADIUS")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 12, 20, 28).forEach { radius ->
                SetupChip(radius == state.cornerRadius, "${radius}dp", uiAccent) {
                    update(state.copy(cornerRadius = radius))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SHOW UPPERCASE LABEL", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.showLabel,
                onCheckedChange = { update(state.copy(showLabel = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = uiAccent)
            )
        }
        Spacer(Modifier.height(20.dp))

        if (definition.options.isNotEmpty()) {
            AdvancedOptionsSection(definition = definition, state = state, accent = uiAccent, update = update)
            Spacer(Modifier.height(20.dp))
        }

        SetupSectionLabel("TAP ACTION")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                TapAction.DEFAULT, TapAction.OPEN_CLOCK, TapAction.OPEN_CALENDAR,
                TapAction.OPEN_BATTERY, TapAction.OPEN_WEATHER, TapAction.NONE
            ).forEach { action ->
                val selected = action == state.tapAction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (selected) uiAccent else Color(0x338A8A8A), RoundedCornerShape(8.dp))
                        .background(if (selected) uiAccent.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { update(state.copy(tapAction = action)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(action.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = uiAccent)
                }
            }
        }
        saveError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (com.example.data.billing.Entitlements.hasFullAccess(context)) onSave(state)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pin_widget_button"),
            colors = ButtonDefaults.buttonColors(containerColor = uiAccent, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(saveLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
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
            .border(1.dp, accentColor, RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(permission.icon, contentDescription = null, tint = accentColor, modifier = Modifier.width(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${permission.title.uppercase()} NEEDED",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A8A8A)
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
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ALLOW", fontWeight = FontWeight.Bold)
        }
    }
}

/** Time and date format choices for clock widgets; shows nothing for other widgets. */
@Composable
private fun ClockFormatSection(widgetId: String, state: WidgetSetupState, update: (WidgetSetupState) -> Unit) {
    val context = LocalContext.current
    val accentColor = Color(
        if (state.accent == AccentOption.DYNAMIC) MaterialYouPalette.resolve(context) else state.accent.hex
    )
    val timeFormats = ClockSupport.timeFormats(widgetId)
    if (timeFormats.isNotEmpty()) {
        val selected = state.timeFormat?.takeIf { it in timeFormats } ?: ClockSupport.defaultTime(widgetId)
        SetupSectionLabel("TIME FORMAT")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            timeFormats.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { format ->
                        SetupChip(format == selected, format.label, accentColor) {
                            update(state.copy(timeFormat = format))
                        }
                    }
                }
            }
        }
        if (selected.hasSeconds) {
            Text(
                "Seconds tick live on the home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A8A8A),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (ClockSupport.showsDate(widgetId)) {
        val selected = state.dateFormat ?: ClockSupport.defaultDate(widgetId, state.size == SizeBucket.S)
        SetupSectionLabel("DATE FORMAT")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFormat.entries.forEach { format ->
                SetupChip(format == selected, format.label, accentColor) {
                    update(state.copy(dateFormat = format))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (widgetId in ClockSupport.TIME_ZONE_PICKERS) {
        TimeZonePicker(
            selectedZone = state.timeZoneId,
            accentColor = accentColor,
            onPick = { update(state.copy(timeZoneId = it)) }
        )
        Spacer(Modifier.height(20.dp))
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
    SetupSectionLabel("TIME ZONE")
    val label = selectedZone?.substringAfterLast("/")?.replace("_", " ") ?: "Phone's time zone"
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SetupChip(selectedZone == null, "PHONE", accentColor) { onPick(null) }
        SetupChip(selectedZone != null, label.uppercase(), accentColor) { showDialog = true }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("CANCEL") } },
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
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                zone.substringAfterLast("/").replace("_", " "),
                                modifier = Modifier.weight(1f)
                            )
                            Text(zone, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8A8A))
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SetupSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A8A8A))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SetupChip(
    selected: Boolean,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            selectedLabelColor = Color.White
        )
    )
}
