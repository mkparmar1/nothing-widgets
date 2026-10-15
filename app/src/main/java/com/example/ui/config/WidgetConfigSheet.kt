package com.example.ui.config

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.ui.components.WidgetBitmapPreview
import com.example.widgets.base.DotWidgetProviderL
import com.example.widgets.base.DotWidgetProviderM
import com.example.widgets.base.DotWidgetProviderS
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigSheet(
    definition: WidgetDefinition,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }

    var selectedSize by remember { mutableStateOf(definition.sizes.first()) }
    var selectedAccent by remember { mutableStateOf(prefs.getSavedAccent()) }
    var selectedBg by remember { mutableStateOf(prefs.getSavedBgMode()) }
    var selectedTheme by remember { mutableStateOf(ThemeMode.AUTO) }
    var cornerRadius by remember { mutableIntStateOf(20) }
    var showLabel by remember { mutableStateOf(true) }
    var selectedTap by remember { mutableStateOf(definition.defaultTap) }

    val isDark = when (selectedTheme) {
        ThemeMode.FORCE_DARK -> true
        ThemeMode.FORCE_LIGHT -> false
        ThemeMode.AUTO -> true
    }

    val dims = remember(selectedSize) {
        SizeResolver.resolveDimensions(context, selectedSize)
    }

    val spec = WidgetSpec(
        widthPx = dims.widthPx,
        heightPx = dims.heightPx,
        density = dims.density,
        sizeBucket = selectedSize,
        accent = selectedAccent,
        bgMode = selectedBg,
        themeMode = selectedTheme,
        isDark = isDark,
        cornerRadiusDp = cornerRadius,
        showLabel = showLabel,
        tapAction = selectedTap
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = definition.title.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = definition.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A8A8A)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("config_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Canvas Bitmap Preview
            WidgetBitmapPreview(
                definition = definition,
                spec = spec,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_live_preview")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Size Selector (if multiple sizes supported)
            if (definition.sizes.size > 1) {
                Text(
                    text = "SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A8A8A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    definition.sizes.forEach { size ->
                        FilterChip(
                            selected = size == selectedSize,
                            onClick = { selectedSize = size },
                            label = { Text("${size.label} (${size.cells})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedAccent.color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Accent Selector
            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AccentSelectorRow(
                selected = selectedAccent,
                onSelect = { selectedAccent = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Background Mode
            Text(
                text = "BACKGROUND MODE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackgroundMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selectedBg,
                        onClick = { selectedBg = mode },
                        label = { Text(mode.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedAccent.color,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme Mode
            Text(
                text = "THEME OVERRIDE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selectedTheme,
                        onClick = { selectedTheme = mode },
                        label = { Text(mode.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedAccent.color,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Corner Radius
            Text(
                text = "CORNER RADIUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 12, 20, 28).forEach { r ->
                    FilterChip(
                        selected = r == cornerRadius,
                        onClick = { cornerRadius = r },
                        label = { Text("${r}dp") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedAccent.color,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Label toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHOW UPPERCASE LABEL",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = showLabel,
                    onCheckedChange = { showLabel = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = selectedAccent.color
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Remappable Tap Action (§4.2)
            Text(
                text = "TAP ACTION (REMAPPABLE)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    TapAction.DEFAULT,
                    TapAction.OPEN_CLOCK,
                    TapAction.OPEN_CALENDAR,
                    TapAction.OPEN_BATTERY,
                    TapAction.OPEN_WEATHER,
                    TapAction.NONE
                ).forEach { action ->
                    val isSelected = action == selectedTap
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) selectedAccent.color else Color(0x338A8A8A), RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x1AE02B2B) else Color.Transparent)
                            .clickable { selectedTap = action }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = action.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) selectedAccent.color else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = selectedAccent.color)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Add to Home Screen Pin Action Button
            Button(
                onClick = {
                    pinWidgetToHomeScreen(
                        context = context,
                        definition = definition,
                        size = selectedSize,
                        accent = selectedAccent,
                        bgMode = selectedBg,
                        themeMode = selectedTheme,
                        cornerRadius = cornerRadius,
                        showLabel = showLabel,
                        tapAction = selectedTap
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pin_widget_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedAccent.color,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADD TO HOME SCREEN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun pinWidgetToHomeScreen(
    context: Context,
    definition: WidgetDefinition,
    size: SizeBucket,
    accent: AccentOption,
    bgMode: BackgroundMode,
    themeMode: ThemeMode,
    cornerRadius: Int,
    showLabel: Boolean,
    tapAction: TapAction
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val providerClass = when (size) {
        SizeBucket.S -> DotWidgetProviderS::class.java
        SizeBucket.M -> DotWidgetProviderM::class.java
        SizeBucket.L -> DotWidgetProviderL::class.java
    }
    val provider = ComponentName(context, providerClass)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val configJson = JSONObject().apply {
                put("accent", accent.name)
                put("background", bgMode.name)
                put("theme_mode", themeMode.name)
                put("corner_radius", cornerRadius)
                put("show_label", showLabel)
                put("tap_action", tapAction.name)
                put("widget_type_id", definition.id)
            }.toString()

            val successIntent = Intent(context, WidgetConfigReceiver::class.java).apply {
                putExtra("widget_type_id", definition.id)
                putExtra("config_json", configJson)
            }
            val successPendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                successIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            appWidgetManager.requestPinAppWidget(provider, null, successPendingIntent)
            Toast.makeText(context, "Placing ${definition.title}...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Launcher does not support direct widget pinning. Add from launcher widget drawer.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Add widget from your home screen widget menu.", Toast.LENGTH_LONG).show()
    }
}
