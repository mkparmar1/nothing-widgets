package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.bitmap.BitmapCache
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.core.theme.WidgetAccent
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.AccentSelectorRow
import com.example.ui.components.DotBadge
import com.example.ui.components.DotBrand
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotLabel
import com.example.ui.components.DotMark
import com.example.ui.components.SegmentTabs
import com.example.ui.components.SettingsRow
import com.example.ui.components.StatusHero
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.permissions.AppPermission
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Public copy of docs/PRIVACY_POLICY.md; also the URL to enter in Play Console. */
const val PRIVACY_POLICY_URL = "https://github.com/mkparmar1/nothing-widgets/blob/main/docs/PRIVACY_POLICY.md"

private const val SOURCE_URL = "https://github.com/mkparmar1/nothing-widgets"

/** The Settings tab: a short menu, with each group of settings on its own page. */
private enum class SettingsPage(val title: String, val subtitle: String, val icon: ImageVector) {
    HELP("Help & guide", "Replay the app tour and quick tips", Icons.AutoMirrored.Filled.HelpOutline),
    APPEARANCE("Appearance", "Themes, Looks and accent colour", Icons.Default.Palette),
    DATA("Weather & connections", "Weather location, Home Assistant, sports feed", Icons.Default.Link),
    PERMISSIONS("Permissions", "What widgets are allowed to read", Icons.Default.Security),
    STATUS("Widget status", "Placed widgets and last refresh", Icons.Default.BatteryChargingFull),
    ABOUT("About", "Version, data sources and privacy policy", Icons.Default.Info),
    PREVIEW("Theme preview", "Try a theme before applying it", Icons.Default.DarkMode);

    companion object {
        val menu: List<SettingsPage> = entries.filter { it != PREVIEW }
    }
}

@Composable
fun SettingsScreen(
    onStartTour: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var openPage by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    fun back() {
        openPage = if (openPage == SettingsPage.PREVIEW) SettingsPage.APPEARANCE else null
    }
    BackHandler(enabled = openPage != null) { back() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DotSpacing.lg)
    ) {
        val page = openPage
        if (page == null) {
            SettingsMenu(onOpen = { openPage = it })
        } else {
            SettingsPageHeader(page.title, onBack = { back() })
            when (page) {
                SettingsPage.HELP -> HelpPage(onStartTour = onStartTour)
                SettingsPage.APPEARANCE -> AppearancePage(onPreview = { openPage = SettingsPage.PREVIEW })
                SettingsPage.DATA -> DataPage()
                SettingsPage.PERMISSIONS -> PermissionsPage()
                SettingsPage.STATUS -> WidgetStatusPage()
                SettingsPage.ABOUT -> AboutPage()
                SettingsPage.PREVIEW -> ThemePreviewPage(onApplied = { openPage = SettingsPage.APPEARANCE })
            }
        }
    }
}

@Composable
private fun SettingsMenu(onOpen: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val colors = DotTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)) {
        DotLabel("Dot Widgets")
        Text("Settings", style = DotType.Hero, color = colors.textPrimary)
        Text(
            "Appearance, data sources, permissions and support.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
    }

    DotCard(padding = DotSpacing.xs) {
        SettingsPage.menu.forEachIndexed { index, page ->
            val missing = if (page == SettingsPage.PERMISSIONS) AppPermission.missing(context).count { it.isApplicable() } else 0
            SettingsRow(
                icon = page.icon,
                title = page.title,
                onClick = { onOpen(page) },
                caption = if (missing > 0) "$missing permission(s) not allowed yet" else page.subtitle,
                modifier = Modifier.testTag("settings_row_${page.name.lowercase()}"),
                trailing = if (missing > 0) {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DotBadge("$missing missing", color = colors.warning)
                            Spacer(Modifier.width(DotSpacing.xs))
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else null
            )
            if (index < SettingsPage.menu.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = DotSpacing.xxl),
                    thickness = 0.5.dp,
                    color = colors.divider
                )
            }
        }
    }
}

@Composable
private fun HelpPage(onStartTour: () -> Unit) {
    val colors = DotTheme.colors

    DotCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(DotShape.button)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(DotSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("Take the app tour", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    "A quick walkthrough of Home, Explore, Builder and every screen in between.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(Modifier.height(DotSpacing.md))
        DotButton(
            text = "Start the app tour",
            onClick = onStartTour,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("help_start_tour")
        )
    }

    DotLabel("Quick tips")
    DotCard(padding = DotSpacing.xs) {
        HelpTip(
            icon = Icons.Default.Home,
            title = "Add a widget",
            body = "Explore, open a widget, then Add to home screen, or tap + in the nav to build one."
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = DotSpacing.xxl),
            thickness = 0.5.dp,
            color = colors.divider
        )
        HelpTip(
            icon = Icons.Default.Widgets,
            title = "Build your own",
            body = "Start from a template or a blank canvas, then add time, battery, weather and text blocks."
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = DotSpacing.xxl),
            thickness = 0.5.dp,
            color = colors.divider
        )
        HelpTip(
            icon = Icons.Default.Security,
            title = "Missing data?",
            body = "Open Settings, Permissions. Weather, calendar and health are only read when a widget uses them."
        )
    }
}

@Composable
private fun HelpTip(icon: ImageVector, title: String, body: String) {
    val colors = DotTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DotSpacing.md, vertical = DotSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DotSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun SettingsPageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DotTheme.colors.textPrimary)
        }
        Column {
            DotLabel("Settings")
            Text(title, style = DotType.Section, color = DotTheme.colors.textPrimary)
        }
    }
}

@Composable
private fun AppearancePage(onPreview: () -> Unit) {
    val prefs = PreferencesManager.getInstance(LocalContext.current)
    val globalAccent by prefs.accent.collectAsState()

    ThemeCard(onPreview = onPreview)
    com.example.themes.ui.ThemeStudioCard()
    LooksCard(accent = globalAccent)
    AccentCard()
}

/** System / Light / Dark for the whole app, with a shortcut to the full theme preview. */
@Composable
private fun ThemeCard(onPreview: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesManager.getInstance(context) }
    val mode by prefs.themeMode.collectAsState()
    val options = remember { listOf(ThemeMode.AUTO, ThemeMode.FORCE_LIGHT, ThemeMode.FORCE_DARK) }
    val colors = DotTheme.colors

    DotCard(modifier = Modifier.fillMaxWidth().testTag("theme_mode_card")) {
        DotLabel("Theme")
        Text("App theme", style = DotType.CardTitle, color = colors.textPrimary)
        Text(
            "Follow the system, or keep Dot Widgets light or dark all the time.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        SegmentTabs(
            items = options.map { it.displayName },
            selectedIndex = options.indexOf(mode).coerceAtLeast(0),
            onSelect = { prefs.setGlobalThemeMode(options[it]) },
            tagPrefix = "theme_mode",
            modifier = Modifier.testTag("theme_mode_tabs")
        )
        Spacer(Modifier.height(DotSpacing.sm))
        HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
        SettingsRow(
            icon = Icons.Default.DarkMode,
            title = "Theme preview",
            onClick = onPreview,
            caption = "See it on a real widget, then apply.",
            modifier = Modifier.testTag("theme_preview_row")
        )
    }
}

@Composable
private fun AccentCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()
    val colors = DotTheme.colors

    DotCard(modifier = Modifier.fillMaxWidth()) {
        DotLabel("Accent")
        Text("Global accent", style = DotType.CardTitle, color = colors.textPrimary)
        Text(
            "Recolours every widget placed on your home screen.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.md))
        AccentSelectorRow(
            selected = globalAccent,
            onSelect = { option -> prefs.setGlobalAccent(option) }
        )
        Spacer(Modifier.height(DotSpacing.md))
        DotButton(
            text = "Repaint all placed widgets",
            onClick = {
                scope.launch {
                    BitmapCache.clear()
                    WidgetRenderEngine.updateAllPlacedWidgets(context)
                    Toast.makeText(
                        context,
                        "Repainted all placed widgets with ${globalAccent.displayName}!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            icon = Icons.Default.Refresh,
            style = DotButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("repaint_all_widgets_button")
        )
    }
}

@Composable
private fun ThemePreviewPage(onApplied: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()
    val savedMode by prefs.themeMode.collectAsState()
    var mode by remember { mutableStateOf(savedMode) }
    var dynamic by remember { mutableStateOf(prefs.isMaterialYou()) }
    val colors = DotTheme.colors
    val options = remember { listOf(ThemeMode.AUTO, ThemeMode.FORCE_LIGHT, ThemeMode.FORCE_DARK) }
    val dark = when (mode) {
        ThemeMode.FORCE_DARK -> true
        ThemeMode.FORCE_LIGHT -> false
        ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val previewAccent = if (dynamic) AccentOption.DYNAMIC else globalAccent

    ThemePreviewCard(
        dark = dark,
        accent = previewAccent,
        dynamic = dynamic,
        modifier = Modifier.testTag("theme_preview_card")
    )

    DotCard(modifier = Modifier.fillMaxWidth()) {
        Text("Theme style", style = DotType.CardTitle, color = colors.textPrimary)
        Text(
            "Pick how Dot Widgets follows your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        SegmentTabs(
            items = options.map { it.displayName },
            selectedIndex = options.indexOf(mode).coerceAtLeast(0),
            onSelect = { mode = options[it] },
            tagPrefix = "preview_mode"
        )
        Spacer(Modifier.height(DotSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dynamic colour", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    "Use Material You colours from your wallpaper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Switch(
                checked = dynamic,
                onCheckedChange = { dynamic = it },
                modifier = Modifier.testTag("dynamic_color_switch")
            )
        }
    }

    DotButton(
        text = "Apply theme",
        onClick = {
            prefs.setGlobalThemeMode(mode)
            prefs.setMaterialYou(dynamic)
            Toast.makeText(context, "Theme applied", Toast.LENGTH_SHORT).show()
            onApplied()
        },
        icon = Icons.Default.CheckCircle,
        modifier = Modifier.fillMaxWidth().testTag("apply_theme")
    )
}

@Composable
private fun ThemePreviewCard(
    dark: Boolean,
    accent: AccentOption,
    dynamic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    DotWidgetsTheme(darkTheme = dark, accentOption = if (dynamic) AccentOption.DYNAMIC else accent) {
        val colors = DotTheme.colors
        val definition = remember { WidgetRegistry.getById("clock.dotdigital.m") }
        DotCard(modifier = modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    DotLabel("Live preview")
                    Text(
                        if (dark) "Dark theme" else "Light theme",
                        style = DotType.CardTitle,
                        color = colors.textPrimary
                    )
                }
                DotBadge(if (dynamic) "Material You" else accent.displayName)
            }
            Spacer(Modifier.height(DotSpacing.md))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DotShape.large)
                    .background(colors.surfaceSecondary)
                    .padding(DotSpacing.lg)
            ) {
                if (definition != null) {
                    val dims = remember { SizeResolver.resolveDimensions(context, SizeBucket.M) }
                    val spec = remember(dark, accent, dynamic) {
                        WidgetSpec(
                            widthPx = dims.widthPx,
                            heightPx = dims.heightPx,
                            density = dims.density,
                            sizeBucket = SizeBucket.M,
                            accent = if (dynamic) AccentOption.DYNAMIC else accent,
                            accentColor = WidgetAccent.resolve(
                                context,
                                if (dynamic) AccentOption.DYNAMIC else accent,
                                null
                            ),
                            bgMode = BackgroundMode.TRANSPARENT,
                            themeMode = if (dark) ThemeMode.FORCE_DARK else ThemeMode.FORCE_LIGHT,
                            isDark = dark,
                            cornerRadiusDp = 20,
                            showLabel = false,
                            tapAction = definition.defaultTap
                        )
                    }
                    WidgetBitmapPreview(definition = definition, spec = spec, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DataPage() {
    val prefs = PreferencesManager.getInstance(LocalContext.current)
    val globalAccent by prefs.accent.collectAsState()
    WeatherSettingsCard(accent = globalAccent)
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
        color = DotTheme.colors.textSecondary
    )
    com.example.ui.permissions.PermissionList(accent = globalAccent, showGranted = true)
}

@Composable
private fun WidgetStatusPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    var version by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }

    val widgetCount = remember(version) { WidgetRenderEngine.placedWidgetCount(context) }
    val exactAlarms = remember(version) { WidgetAlarmScheduler.canScheduleExactAlarms(context) }
    val lastRefresh = remember(version) { prefs.getLastWidgetRefresh() }
    val warning = widgetCount > 0 && !exactAlarms
    val tint = when {
        widgetCount == 0 -> DotTheme.colors.textSecondary
        warning -> DotTheme.colors.warning
        else -> DotTheme.colors.success
    }
    val title = when {
        widgetCount == 0 -> "No widgets placed"
        warning -> "$widgetCount widget(s) need attention"
        else -> "Widgets placed"
    }
    val caption = when {
        widgetCount == 0 -> "Add a widget to your home screen to see it here."
        warning -> "Minute alarms are off; clock widgets may update late."
        else -> "$widgetCount widget(s) set up. Last refresh: ${formatRelative(lastRefresh)}."
    }

    StatusHero(
        icon = when {
            warning -> Icons.Default.Warning
            widgetCount == 0 -> Icons.Default.Info
            else -> Icons.Default.CheckCircle
        },
        title = title,
        caption = caption,
        tint = tint,
        modifier = Modifier.testTag("widget_status_hero")
    ) {
        DotButton(
            text = "Check again",
            onClick = {
                checking = true
                scope.launch {
                    BitmapCache.clear()
                    WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
                    checking = false
                    version++
                }
            },
            icon = Icons.Default.Refresh,
            style = DotButtonStyle.Secondary,
            loading = checking,
            modifier = Modifier.testTag("check_widgets_again")
        )
    }

    DotCard(modifier = Modifier.fillMaxWidth()) {
        DotLabel("Audit")
        AuditRow(
            "Widgets placed",
            "$widgetCount",
            if (widgetCount > 0) DotTheme.colors.success else DotTheme.colors.textMuted
        )
        AuditRow("Last refresh", formatRelative(lastRefresh))
        AuditRow(
            "Exact alarms",
            if (exactAlarms) "On" else "Off",
            if (exactAlarms) DotTheme.colors.success else DotTheme.colors.warning
        )
    }
}

@Composable
private fun AboutPage() {
    val context = LocalContext.current
    var showLicences by remember { mutableStateOf(false) }
    val colors = DotTheme.colors

    DotCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.md)) {
            AboutLogo()
            Column(Modifier.weight(1f)) {
                DotLabel("Dot Widgets")
                Text("v${BuildConfig.VERSION_NAME}", style = DotType.CardTitle, color = colors.textPrimary)
                Text("Dot-matrix widgets for your home screen.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(DotSpacing.md))
        Text(
            "An independent dot-matrix widget pack. No ads, no accounts, no analytics.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Text(
            "Dot Widgets is an independent app. It is not affiliated with, endorsed by, or sponsored by Nothing Technology Limited. Nothing, Nothing OS and Glyph are trademarks of their owner.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.testTag("non_affiliation")
        )
        Spacer(Modifier.height(DotSpacing.sm))
        HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
        SettingsRow(
            icon = Icons.Default.Info,
            title = "Privacy policy",
            onClick = { openUrl(context, PRIVACY_POLICY_URL) },
            caption = "How your data stays on your phone.",
            modifier = Modifier.testTag("privacy_policy")
        )
        SettingsRow(
            icon = Icons.Default.Info,
            title = "Open-source licences",
            onClick = { showLicences = true },
            caption = "Geist, Open-Meteo, CoinGecko and more.",
            modifier = Modifier.testTag("open_source_licences")
        )
        SettingsRow(
            icon = Icons.Default.Link,
            title = "Source code",
            onClick = { openUrl(context, SOURCE_URL) },
            caption = "github.com/mkparmar1/nothing-widgets",
            modifier = Modifier.testTag("source_code")
        )
    }

    DotCard(modifier = Modifier.fillMaxWidth()) {
        Text("Data sources", style = DotType.CardTitle, color = colors.textPrimary)
        Text(
            "Weather, air quality and geocoding by Open-Meteo.com (CC BY 4.0); crypto prices by CoinGecko; exchange rates by ExchangeRate-API (open.er-api.com); stock quotes by Stooq.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.testTag("data_sources")
        )
        Text(
            "Typography: Geist and Geist Mono (OFL).",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
    }

    if (showLicences) {
        AlertDialog(
            onDismissRequest = { showLicences = false },
            title = { Text("Open-source licences") },
            text = {
                Text(
                    "Geist and Geist Mono are used under the SIL Open Font License 1.1.\n\n" +
                        "Weather, air quality and geocoding data: Open-Meteo.com, CC BY 4.0.\n" +
                        "Crypto prices: CoinGecko.\n" +
                        "Exchange rates: ExchangeRate-API (open.er-api.com).\n" +
                        "Stock quotes: Stooq."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicences = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AboutLogo() {
    val colors = DotTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(DotShape.large)
            .background(colors.surfaceSecondary)
    ) {
        DotMark(
            modifier = Modifier.size(30.dp),
            dotColor = colors.textPrimary,
            accentColor = DotBrand.red
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, url, Toast.LENGTH_LONG).show() }
}

private fun formatRelative(ms: Long): String {
    if (ms <= 0L) return "Never"
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
    }
}

@Composable
fun AuditRow(title: String, status: String, dotColor: Color? = null) {
    val color = dotColor ?: DotTheme.colors.success
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DotSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = DotTheme.colors.textPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(DotSpacing.sm))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = DotTheme.colors.textSecondary
            )
        }
    }
}
