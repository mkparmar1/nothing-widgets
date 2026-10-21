package com.example.ui.permissions

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.core.bitmap.BitmapCache
import com.example.core.theme.AccentOption
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** True when every permission the widgets can use has been granted. */
fun allPermissionsGranted(context: Context): Boolean = AppPermission.missing(context).isEmpty()

private const val ONBOARDING_PREFS = "dot_widgets_onboarding"

/**
 * The permissions screen shows on first launch, and again while exact alarms (needed by every
 * clock) are off. Everything else can be granted later from Settings or in a widget's setup.
 */
fun shouldShowPermissions(context: Context): Boolean {
    val seen = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE).getBoolean("seen", false)
    return !seen || !AppPermission.ExactAlarms.isGranted(context)
}

fun markPermissionsSeen(context: Context) {
    context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE).edit().putBoolean("seen", true).apply()
}

/** Called after any permission changes: reschedules the minute tick and redraws every widget. */
fun onPermissionsChanged(context: Context, scope: CoroutineScope) {
    WidgetAlarmScheduler.scheduleNextMinuteTick(context)
    BitmapCache.clear()
    scope.launch { WidgetRenderEngine.updateAllPlacedWidgets(context.applicationContext) }
}

/**
 * First launch (and while exact alarms are off): a welcome with live widgets, then the one
 * permission every clock needs, with the action in the bottom third of the screen. Everything else
 * is asked for later, only when a widget needs it.
 */
@Composable
fun PermissionsScreen(
    accent: AccentOption,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose { }
    }
    val granted = remember(version) { AppPermission.ExactAlarms.isGranted(context) }
    var wasGranted by remember { mutableStateOf<Boolean?>(null) }
    // Coming back from the system page with access granted: reschedule and move on.
    LaunchedEffect(granted) {
        val before = wasGranted
        wasGranted = granted
        if (before == false && granted) {
            onPermissionsChanged(context, scope)
            onDone()
        }
    }
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("permissions_screen")
    ) {
        // ---- Welcome: what the app makes, shown live
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DOT WIDGETS",
                style = MaterialTheme.typography.labelSmall,
                color = accent.color
            )
            Text(
                text = "Dot-matrix widgets for your home screen",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Clocks, weather, battery, calendar and 200 more. All free, and most work offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
            WelcomePreview(accent)
        }

        // ---- Thumb zone: the one permission and the main action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("permission_${AppPermission.ExactAlarms.id}")
            ) {
                Icon(
                    if (granted) Icons.Default.CheckCircle else AppPermission.ExactAlarms.icon,
                    contentDescription = null,
                    tint = if (granted) Color(0xFFB8E000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (granted) "Clocks stay on the minute" else "Keep clocks on the minute",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (granted) "Alarms & reminders access is on."
                        else "Needs Alarms & reminders access. Weather, calendar and other data are asked for only when a widget uses them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Button(
                onClick = {
                    if (granted) {
                        onDone()
                    } else {
                        val intent = AppPermission.ExactAlarms.intent(context)
                        if (intent == null) onDone()
                        else try { context.startActivity(intent) } catch (_: Exception) { onDone() }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.color,
                    contentColor = com.example.ui.theme.contentColorOn(accent.color)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("permissions_primary")
            ) {
                Text(if (granted) "GET STARTED" else "ALLOW AND CONTINUE", style = MaterialTheme.typography.titleMedium)
            }
            if (!granted) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("permissions_skip")
                ) {
                    Text("Not now", color = muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Two live widgets in the app accent, so the first screen shows what the app makes. */
@Composable
private fun WelcomePreview(accent: AccentOption) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf("clock.dotdigital.m", "battery.ring").forEach { id ->
            val def = remember(id) { com.example.widgets.base.WidgetRegistry.getById(id) } ?: return@forEach
            val size = def.sizes.first()
            val dims = remember(size) { com.example.core.bitmap.SizeResolver.resolveDimensions(context, size) }
            val spec = remember(id, accent) {
                com.example.widgets.base.WidgetSpec(
                    widthPx = dims.widthPx,
                    heightPx = dims.heightPx,
                    density = dims.density,
                    sizeBucket = size,
                    accent = accent,
                    accentColor = com.example.core.theme.WidgetAccent.resolve(context, accent, null),
                    bgMode = com.example.core.theme.BackgroundMode.SOLID,
                    themeMode = com.example.core.theme.ThemeMode.FORCE_DARK,
                    isDark = true,
                    cornerRadiusDp = 24,
                    showLabel = true,
                    tapAction = def.defaultTap
                )
            }
            val aspect = dims.widthPx.toFloat() / dims.heightPx.coerceAtLeast(1)
            com.example.ui.components.WidgetBitmapPreview(
                definition = def,
                spec = spec,
                modifier = Modifier
                    .heightIn(max = 160.dp)
                    .widthIn(max = 160.dp * aspect)
                    .testTag("welcome_preview_$id")
            )
        }
    }
}

/**
 * Rows for every permission. Runtime ones show Android's dialog; settings ones open the system
 * page and are re-checked when the user comes back.
 */
@Composable
fun PermissionList(
    accent: AccentOption,
    showGranted: Boolean,
    permissions: List<AppPermission> = AppPermission.ALL,
    onAllGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Bumped whenever something may have changed, to re-read every isGranted().
    var version by remember { mutableIntStateOf(0) }
    val previouslyGranted = remember { mutableMapOf<String, Boolean>() }

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        version++
    }

    LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose { }
    }

    val states = remember(version, permissions) { permissions.associateWith { it.isGranted(context) } }

    // React to permissions that changed since the last check.
    LaunchedEffect(states) {
        var changed = false
        states.forEach { (permission, granted) ->
            val before = previouslyGranted.put(permission.id, granted)
            if (before != null && before != granted) {
                changed = true
                if (permission == AppPermission.Location && granted && !WeatherRepository.hasChosenPlace(context)) {
                    WeatherRepository.placeFromDevice(context)?.let { WeatherRepository.setPlace(context, it) }
                }
            }
        }
        if (changed) onPermissionsChanged(context, scope)
        if (states.values.all { it }) onAllGranted()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        permissions.filter { showGranted || states[it] != true }.forEach { permission ->
            PermissionRow(
                permission = permission,
                granted = states[permission] == true,
                accent = accent,
                onAllow = {
                    when (permission) {
                        is AppPermission.Runtime -> runtimeLauncher.launch(permission.permissions)
                        is AppPermission.SettingsPage -> permission.intent(context)?.let {
                            try { context.startActivity(it) } catch (_: Exception) { }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionRow(
    permission: AppPermission,
    granted: Boolean,
    accent: AccentOption,
    onAllow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("permission_${permission.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(permission.icon, contentDescription = null, tint = accent.color, modifier = Modifier.size(22.dp))
            Text(
                text = permission.title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = permission.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8A8A8A)
        )
        if (granted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFB8E000), modifier = Modifier.size(18.dp))
                Text("ALLOWED", style = MaterialTheme.typography.labelLarge, color = Color(0xFFB8E000), fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAllow,
                shape = RoundedCornerShape(10.dp),
                colors = com.example.ui.components.tonalButtonColors(accent.color),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("ALLOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}
