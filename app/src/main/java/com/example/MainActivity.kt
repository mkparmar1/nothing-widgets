package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.lifecycleScope
import com.example.core.theme.AccentOption
import com.example.core.theme.MaterialYouPalette
import com.example.data.battery.BatteryHelper
import com.example.data.prefs.PreferencesManager
import com.example.ui.browse.BrowserScreen
import com.example.ui.permissions.AppPermission
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.permissions.markPermissionsSeen
import com.example.ui.permissions.shouldShowPermissions
import com.example.ui.settings.SettingsScreen
import com.example.ui.setups.SetupsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.ui.wallpapers.WallpapersScreen
import com.example.ui.weather.WeatherDetailSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openWeatherDetail = intent.getBooleanExtra("open_weather_detail", false)
        intent.getStringExtra("open_permission")?.let(::openPermission)

        com.example.widgets.base.WidgetAlarmScheduler.scheduleNextMinuteTick(this)
        lifecycleScope.launch(Dispatchers.IO) {
            com.example.widgets.base.WidgetRenderEngine.updateAllPlacedWidgets(this@MainActivity)
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager.getInstance(context) }
            val currentAccent by prefs.accent.collectAsState()

            var showWeatherSheet by remember { mutableStateOf(openWeatherDetail) }
            // First launch, or while exact alarms are off: ask for permissions before showing the app.
            var showPermissions by remember { mutableStateOf(shouldShowPermissions(context)) }

            DotWidgetsTheme(accentOption = currentAccent) {
                if (showPermissions) {
                    PermissionsScreen(
                        accent = currentAccent,
                        onDone = {
                            markPermissionsSeen(context)
                            showPermissions = false
                        }
                    )
                } else {
                    DotWidgetsApp(
                        onOpenWeather = { showWeatherSheet = true }
                    )
                }

                if (!showPermissions && showWeatherSheet) {
                    WeatherDetailSheet(
                        accent = currentAccent,
                        onDismiss = { showWeatherSheet = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("open_permission")?.let(::openPermission)
    }

    /** Redraw placed widgets promptly when the wallpaper (and thus the Material You accent) changes. */
    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lifecycleScope.launch(Dispatchers.IO) {
                com.example.widgets.base.WidgetRenderEngine.updateAllPlacedWidgets(this@MainActivity)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            wallpaperReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(wallpaperReceiver) }
    }

    /** A tapped "needs access" widget lands directly on the permission that widget is missing. */
    private fun openPermission(id: String) {
        val permission = AppPermission.ALL.firstOrNull { it.id == id } ?: return
        when (permission) {
            is AppPermission.Runtime -> ActivityCompat.requestPermissions(this, permission.permissions, 1001)
            is AppPermission.SettingsPage -> permission.intent(this)?.let {
                runCatching { startActivity(it) }
            }
        }
    }
}

/** Outlined when idle and filled when selected, so state doesn't depend on colour alone. */
private enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    WIDGETS("Widgets", Icons.Outlined.Widgets, Icons.Filled.Widgets),
    SETUPS("Setups", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    WALLPAPERS("Wallpapers", Icons.Outlined.Wallpaper, Icons.Filled.Wallpaper),
    SETTINGS("Settings", Icons.Outlined.Tune, Icons.Filled.Tune)
}

@Composable
fun DotWidgetsApp(
    onOpenWeather: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val currentAccent by prefs.accent.collectAsState()
    // The theme primary is the app accent, made readable on light backgrounds.
    val accentColor = MaterialTheme.colorScheme.primary

    var selectedTab by remember { mutableIntStateOf(0) }
    val battery = remember { BatteryHelper.getBatteryStatus(context) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Dot Widgets",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(modifier = Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(accentColor))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(com.example.ui.theme.DotShape.small)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (battery.isCharging) accentColor else com.example.ui.theme.DotTheme.colors.success)
                    )
                    Text(
                        text = "${battery.level}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = com.example.ui.theme.DotTheme.colors.divider)
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    MainTab.entries.forEachIndexed { i, tab ->
                        val selected = selectedTab == i
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = i },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentColor,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                unselectedIconColor = com.example.ui.theme.DotTheme.colors.textMuted,
                                unselectedTextColor = com.example.ui.theme.DotTheme.colors.textMuted,
                                indicatorColor = accentColor.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            androidx.compose.animation.Crossfade(selectedTab, label = "tabs") { tab ->
                when (tab) {
                    0 -> BrowserScreen(onSelectWidget = {})
                    1 -> SetupsScreen()
                    2 -> WallpapersScreen()
                    else -> SettingsScreen()
                }
            }
        }
    }
}
