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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import org.json.JSONObject

data class WidgetSetupState(
    val size: SizeBucket,
    val accent: AccentOption,
    val background: BackgroundMode,
    val theme: ThemeMode,
    val cornerRadius: Int,
    val showLabel: Boolean,
    val tapAction: TapAction
) {
    fun toConfigJson(widgetTypeId: String, existingConfigJson: String? = null): String =
        (runCatching { JSONObject(existingConfigJson ?: "{}") }.getOrElse { JSONObject() }).apply {
            put("accent", accent.name)
            put("background", background.name)
            put("theme_mode", theme.name)
            put("corner_radius", cornerRadius)
            put("show_label", showLabel)
            put("tap_action", tapAction.name)
            put("widget_type_id", widgetTypeId)
            put("size", size.name)
            put("configured", true)
        }.toString()

    companion object {
        fun defaults(
            definition: WidgetDefinition,
            preferences: PreferencesManager,
            lockedSize: SizeBucket? = null
        ) = WidgetSetupState(
            size = lockedSize ?: definition.sizes.first(),
            accent = preferences.getSavedAccent(),
            background = preferences.getSavedBgMode(),
            theme = preferences.getSavedThemeMode(),
            cornerRadius = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )

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
                theme = ThemeMode.fromName(json.optString("theme_mode", preferences.getSavedThemeMode().name)),
                cornerRadius = json.optInt("corner_radius", 20),
                showLabel = json.optBoolean("show_label", true),
                tapAction = runCatching {
                    TapAction.valueOf(json.optString("tap_action", definition.defaultTap.name))
                }.getOrDefault(definition.defaultTap)
            )
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
    val spec = WidgetSpec(
        widthPx = dimensions.widthPx,
        heightPx = dimensions.heightPx,
        density = dimensions.density,
        sizeBucket = state.size,
        accent = state.accent,
        bgMode = state.background,
        themeMode = state.theme,
        isDark = isDark,
        cornerRadiusDp = state.cornerRadius,
        showLabel = state.showLabel,
        tapAction = state.tapAction
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
            PermissionBanner(permission = permission, accent = state.accent)
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

        if (lockedSize == null && definition.sizes.size > 1) {
            SetupSectionLabel("SIZE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                definition.sizes.forEach { size ->
                    SetupChip(size == state.size, "${size.label} (${size.cells})", state.accent) {
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
                SetupChip(mode == state.background, mode.displayName, state.accent) {
                    update(state.copy(background = mode))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        SetupSectionLabel("THEME OVERRIDE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                SetupChip(mode == state.theme, mode.displayName, state.accent) {
                    update(state.copy(theme = mode))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        SetupSectionLabel("CORNER RADIUS")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 12, 20, 28).forEach { radius ->
                SetupChip(radius == state.cornerRadius, "${radius}dp", state.accent) {
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
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = state.accent.color)
            )
        }
        Spacer(Modifier.height(20.dp))

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
                        .border(1.dp, if (selected) state.accent.color else Color(0x338A8A8A), RoundedCornerShape(8.dp))
                        .background(if (selected) state.accent.color.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { update(state.copy(tapAction = action)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(action.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = state.accent.color)
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
            colors = ButtonDefaults.buttonColors(containerColor = state.accent.color, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(saveLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionBanner(permission: AppPermission, accent: AccentOption) {
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
            .border(1.dp, accent.color, RoundedCornerShape(14.dp))
            .background(accent.color.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(permission.icon, contentDescription = null, tint = accent.color, modifier = Modifier.width(22.dp))
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
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("ALLOW", fontWeight = FontWeight.Bold)
        }
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
    accent: AccentOption,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.color,
            selectedLabelColor = Color.White
        )
    )
}
