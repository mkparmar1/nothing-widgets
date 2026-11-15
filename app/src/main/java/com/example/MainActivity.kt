package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.core.theme.ThemeMode
import com.example.data.builder.CustomWidget
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.EditPlacedWidgetScreen
import com.example.ui.browse.BrowserScreen
import com.example.ui.builder.CustomWidgetBuilderScreen
import com.example.ui.components.BottomNav
import com.example.ui.components.DotNavItem
import com.example.ui.home.HomeScreen
import com.example.ui.mywidgets.MyWidgetsScreen
import com.example.ui.permissions.AppPermission
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.permissions.markPermissionsSeen
import com.example.ui.permissions.shouldShowPermissions
import com.example.ui.settings.SettingsScreen
import com.example.ui.setups.SetupsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.ui.wallpapers.WallpapersScreen
import com.example.ui.weather.WeatherDetailSheet
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRenderEngine
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
            com.example.data.builder.CustomWidgetRepository.loadIntoRegistry(this@MainActivity)
            com.example.widgets.base.WidgetRenderEngine.updateAllPlacedWidgets(this@MainActivity)
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager.getInstance(context) }
            val currentAccent by prefs.accent.collectAsState()
            val themeMode by prefs.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.FORCE_DARK -> true
                ThemeMode.FORCE_LIGHT -> false
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            SideEffect {
                val activity = context as? ComponentActivity ?: return@SideEffect
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            var showWeatherSheet by remember { mutableStateOf(openWeatherDetail) }
            // First launch, or while exact alarms are off: ask for permissions before showing the app.
            var showPermissions by remember { mutableStateOf(shouldShowPermissions(context)) }

            DotWidgetsTheme(darkTheme = darkTheme, accentOption = currentAccent) {
                if (showPermissions) {
                    PermissionsScreen(
                        accent = currentAccent,
                        onDone = {
                            markPermissionsSeen(context)
                            showPermissions = false
                        }
                    )
                } else {
                    DotWidgetsApp()
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

/** Screens that sit above the five-destination shell and dismiss back to it. */
private sealed interface AppOverlay {
    data object Setups : AppOverlay
    data object Wallpapers : AppOverlay
    data class Builder(val existing: CustomWidget? = null) : AppOverlay
    data class EditInstance(val instance: WidgetInstance, val definition: WidgetDefinition) : AppOverlay
}

@Composable
fun DotWidgetsApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<AppOverlay?>(null) }

    val navItems = remember {
        listOf(
            DotNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
            DotNavItem("Explore", Icons.Outlined.Widgets, Icons.Filled.Widgets),
            DotNavItem("My Widgets", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
            DotNavItem("Settings", Icons.Outlined.Tune, Icons.Filled.Tune)
        )
    }

    BackHandler(enabled = overlay != null) { overlay = null }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // The menu stays on every screen, including Builder / Setups / Wallpapers, so the user
            // can move around without backing out first.
            Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                BottomNav(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onSelect = {
                        overlay = null
                        selectedTab = it
                    },
                    onPlus = { overlay = AppOverlay.Builder() }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val current = overlay) {
                AppOverlay.Setups -> SetupsScreen()
                AppOverlay.Wallpapers -> WallpapersScreen()
                is AppOverlay.Builder -> CustomWidgetBuilderScreen(
                    existing = current.existing,
                    onDismiss = { overlay = null },
                    onSaved = {
                        overlay = null
                        scope.launch { WidgetRenderEngine.updateAllPlacedWidgets(context, force = true) }
                    }
                )
                is AppOverlay.EditInstance -> EditPlacedWidgetScreen(
                    instance = current.instance,
                    definition = current.definition,
                    onDismiss = {
                        overlay = null
                        scope.launch { WidgetRenderEngine.updateAllPlacedWidgets(context, force = true) }
                    }
                )
                null -> Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(200),
                    label = "tabs"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            onExplore = { selectedTab = 1 },
                            onBuilder = { overlay = AppOverlay.Builder() },
                            onSetups = { overlay = AppOverlay.Setups },
                            onWallpapers = { overlay = AppOverlay.Wallpapers }
                        )
                        1 -> BrowserScreen()
                        2 -> MyWidgetsScreen(
                            onCreate = { overlay = AppOverlay.Builder() },
                            onEditCustom = { custom -> overlay = AppOverlay.Builder(custom) },
                            onEditInstance = { instance, definition ->
                                overlay = AppOverlay.EditInstance(instance, definition)
                            }
                        )
                        else -> SettingsScreen()
                    }
                }
            }
        }
    }
}
