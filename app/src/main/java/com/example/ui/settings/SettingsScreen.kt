package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.example.ui.permissions.AppPermission
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.bitmap.BitmapCache
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.ui.permissions.PermissionList
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.launch

/** Public copy of docs/PRIVACY_POLICY.md; also the URL to enter in Play Console. */
const val PRIVACY_POLICY_URL = "https://github.com/mkparmar1/nothing-widgets/blob/main/docs/PRIVACY_POLICY.md"

/** The Settings tab: a short menu, with each group of settings on its own page. */
private enum class SettingsPage(val title: String, val subtitle: String, val icon: ImageVector) {
    APPEARANCE("Appearance", "Themes, Looks and accent colour", Icons.Default.Palette),
    DATA("Weather & connections", "Weather location, Home Assistant, sports feed", Icons.Default.Link),
    PERMISSIONS("Permissions", "What widgets are allowed to read", Icons.Default.Security),
    STATUS("Widget status", "Placed widgets and last refresh", Icons.Default.BatteryChargingFull),
    ABOUT("About", "Version, data sources and privacy policy", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var openPage by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler(enabled = openPage != null) { openPage = null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val page = openPage) {
            null -> SettingsMenu(onOpen = { openPage = it })
            else -> {
                SettingsPageHeader(page.title, onBack = { openPage = null })
                when (page) {
                    SettingsPage.APPEARANCE -> AppearancePage()
                    SettingsPage.DATA -> DataPage()
                    SettingsPage.PERMISSIONS -> PermissionsPage()
                    SettingsPage.STATUS -> WidgetStatusCard()
                    SettingsPage.ABOUT -> AboutCard()
                }
            }
        }
    }
}

@Composable
private fun SettingsMenu(onOpen: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    Text(
        text = "SETTINGS",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SettingsPage.entries.forEachIndexed { i, page ->
            val missing = if (page == SettingsPage.PERMISSIONS) AppPermission.missing(context).size else 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onOpen(page) }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("settings_row_${page.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(page.icon, contentDescription = null, tint = com.example.ui.components.headerIconTint())
                Column(Modifier.weight(1f)) {
                    Text(page.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (missing > 0) "$missing not allowed yet" else page.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (i < SettingsPage.entries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@Composable
private fun SettingsPageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun AppearancePage() {
    val prefs = PreferencesManager.getInstance(LocalContext.current)
    val globalAccent by prefs.accent.collectAsState()
    com.example.themes.ui.ThemeStudioCard()
    LooksCard(accent = globalAccent)
    AccentCard()
}

@Composable
private fun DataPage() {
    val prefs = PreferencesManager.getInstance(LocalContext.current)
    val globalAccent by prefs.accent.collectAsState()
    WeatherLocationCard(accent = globalAccent)
    ConnectionsCard(accent = globalAccent)
}

@Composable
private fun PermissionsPage() {
    val prefs = PreferencesManager.getInstance(LocalContext.current)
    val globalAccent by prefs.accent.collectAsState()
    Text(
        text = "Every permission is optional. Allow only what your widgets need.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    PermissionList(accent = globalAccent, showGranted = true)
}

@Composable
private fun AccentCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = com.example.ui.components.headerIconTint())
                Text(
                    text = "GLOBAL ACCENT REPAINT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Instantly re-colors all active widgets placed on your home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            AccentSelectorRow(
                selected = globalAccent,
                onSelect = { option ->
                    prefs.setGlobalAccent(option)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        BitmapCache.clear()
                        WidgetRenderEngine.updateAllPlacedWidgets(context)
                        Toast.makeText(context, "Repainted all placed widgets with ${globalAccent.displayName}!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = com.example.ui.components.tonalButtonColors(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("repaint_all_widgets_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REPAINT ALL PLACED WIDGETS", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun WidgetStatusCard() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    val widgetCount = remember { WidgetRenderEngine.placedWidgetCount(context) }
    val exactAlarms = remember { WidgetAlarmScheduler.canScheduleExactAlarms(context) }
    val lastRefresh = prefs.getLastWidgetRefresh()
    val nextRefresh = if (widgetCount == 0) "Paused — no widgets" else "In 1 min"
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = com.example.ui.components.headerIconTint())
                Text(
                    text = "WIDGET STATUS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            AuditRow("Widgets placed", "$widgetCount")
            AuditRow("Last refresh", formatRelative(lastRefresh))
            AuditRow("Next refresh", nextRefresh)
            AuditRow("Exact alarms", if (exactAlarms) "On" else "Off")
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = com.example.ui.components.headerIconTint())
                Text(
                    text = "DOT WIDGETS v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "An independent dot-matrix widget pack. No ads, no accounts, no analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Dot Widgets is an independent app. It is not affiliated with, endorsed by, or sponsored by Nothing Technology Limited. Nothing, Nothing OS and Glyph are trademarks of their owner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.testTag("non_affiliation")
            )
            Text(
                text = "Data sources: weather, air quality and geocoding by Open-Meteo.com (CC BY 4.0); crypto prices by CoinGecko; exchange rates by ExchangeRate-API (open.er-api.com); stock quotes by Stooq.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Typography: Space Grotesk (OFL) & JetBrains Mono (OFL).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    }.onFailure { Toast.makeText(context, PRIVACY_POLICY_URL, Toast.LENGTH_LONG).show() }
                },
                modifier = Modifier.testTag("privacy_policy")
            ) { Text("PRIVACY POLICY", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun formatRelative(ms: Long): String {
    if (ms <= 0L) return "Never"
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} h ago"
        else -> "${diff / 86_400_000} d ago"
    }
}

@Composable
fun AuditRow(title: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB8E000))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
