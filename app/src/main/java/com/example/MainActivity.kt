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

@Composable
fun DotWidgetsApp(
    onOpenWeather: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val currentAccent by prefs.accent.collectAsState()
    val accentColor = remember(currentAccent) {
        if (currentAccent == AccentOption.DYNAMIC) Color(MaterialYouPalette.resolve(context)) else currentAccent.color
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val battery = remember { BatteryHelper.getBatteryStatus(context) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal Brand title with accent dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DOT WIDGETS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }

                    // Live Battery Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (battery.isCharging) accentColor else Color(0xFFB8E000))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${battery.level}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .border(width = 0.5.dp, color = Color(0x228A8A8A))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Widgets") },
                    label = { Text("WIDGETS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_widgets")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Setups") },
                    label = { Text("SETUPS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_setups")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Wallpaper, contentDescription = "Wallpapers") },
                    label = { Text("WALLPAPERS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_wallpapers")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("SETTINGS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> BrowserScreen(onSelectWidget = {})
                1 -> SetupsScreen()
                2 -> WallpapersScreen()
                3 -> SettingsScreen()
            }
        }
    }
}
