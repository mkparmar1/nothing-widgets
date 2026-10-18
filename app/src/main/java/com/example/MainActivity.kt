package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import com.example.core.theme.AccentOption
import com.example.data.battery.BatteryHelper
import com.example.data.prefs.PreferencesManager
import com.example.ui.browse.BrowserScreen
import com.example.ui.components.AccentSelectorRow
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.permissions.markPermissionsSeen
import com.example.ui.permissions.shouldShowPermissions
import com.example.ui.setups.SetupsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.ui.wallpapers.WallpapersScreen
import com.example.ui.weather.WeatherDetailSheet
import com.example.widgets.base.WidgetDefinition

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openWeatherDetail = intent.getBooleanExtra("open_weather_detail", false)

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
}

@Composable
fun DotWidgetsApp(
    onOpenWeather: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val currentAccent by prefs.accent.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAccentPopup by remember { mutableStateOf(false) }

    val battery = remember { BatteryHelper.getBatteryStatus(context) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand title with accent dot
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DOT WIDGETS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(currentAccent.color)
                        )
                    }

                    // Status and Accent controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    .background(if (battery.isCharging) currentAccent.color else Color(0xFFB8E000))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${battery.level}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Accent Quick Trigger
                        IconButton(
                            onClick = { showAccentPopup = !showAccentPopup },
                            modifier = Modifier.testTag("quick_accent_button")
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Accent",
                                tint = currentAccent.color
                            )
                        }
                    }
                }

                if (showAccentPopup) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        AccentSelectorRow(
                            selected = currentAccent,
                            onSelect = {
                                prefs.setGlobalAccent(it)
                                showAccentPopup = false
                            }
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
                        selectedIconColor = currentAccent.color,
                        selectedTextColor = currentAccent.color,
                        indicatorColor = Color(0x1AE02B2B)
                    ),
                    modifier = Modifier.testTag("nav_widgets")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Setups") },
                    label = { Text("SETUPS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentAccent.color,
                        selectedTextColor = currentAccent.color,
                        indicatorColor = Color(0x1AE02B2B)
                    ),
                    modifier = Modifier.testTag("nav_setups")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Wallpaper, contentDescription = "Wallpapers") },
                    label = { Text("WALLPAPERS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentAccent.color,
                        selectedTextColor = currentAccent.color,
                        indicatorColor = Color(0x1AE02B2B)
                    ),
                    modifier = Modifier.testTag("nav_wallpapers")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("SETTINGS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentAccent.color,
                        selectedTextColor = currentAccent.color,
                        indicatorColor = Color(0x1AE02B2B)
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
