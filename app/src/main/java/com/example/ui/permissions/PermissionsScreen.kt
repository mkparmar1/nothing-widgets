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
 * clock) are off. Everything else can be granted later from Settings.
 */
fun shouldShowPermissions(context: Context): Boolean {
    if (allPermissionsGranted(context)) return false
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

/** Shown on launch while permissions are missing; every row can also be granted later in Settings. */
@Composable
fun PermissionsScreen(
    accent: AccentOption,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("permissions_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BEFORE YOU START",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Each permission powers specific widgets with your real data. Allow the ones you want — you can change them any time in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A8A8A)
            )
            PermissionList(accent = accent, showGranted = true, onAllGranted = onDone)
            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_skip")
            ) {
                Text("CONTINUE", color = Color(0xFF8A8A8A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Rows for every permission. Runtime ones show Android's dialog; settings ones open the system
 * page and are re-checked when the user comes back.
 */
@Composable
fun PermissionList(accent: AccentOption, showGranted: Boolean, onAllGranted: () -> Unit = {}) {
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

    val states = remember(version) { AppPermission.ALL.associateWith { it.isGranted(context) } }

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
        AppPermission.ALL.filter { showGranted || states[it] != true }.forEach { permission ->
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
            .border(1.dp, if (granted) Color(0x338A8A8A) else accent.color, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("permission_${permission.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFB8E000), modifier = Modifier.size(18.dp))
                Text("ALLOWED", style = MaterialTheme.typography.labelLarge, color = Color(0xFFB8E000), fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAllow,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("ALLOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}
