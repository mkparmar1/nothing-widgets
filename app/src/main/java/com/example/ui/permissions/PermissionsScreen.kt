package com.example.ui.permissions

import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.core.theme.AccentOption
import com.example.widgets.base.DeviceToggles
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.launch

/** True when every special permission the widgets rely on has been granted. */
fun allPermissionsGranted(context: Context): Boolean =
    WidgetAlarmScheduler.canScheduleExactAlarms(context) && DeviceToggles.hasDndAccess(context)

/**
 * Shown on launch while permissions are missing. Exact alarms and DND access have no runtime
 * dialog on Android, so each row opens the matching system settings page; state is re-checked
 * whenever the user returns to the app.
 */
@Composable
fun PermissionsScreen(
    accent: AccentOption,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exactAlarmsAllowed by remember { mutableStateOf(WidgetAlarmScheduler.canScheduleExactAlarms(context)) }
    var dndAccessGranted by remember { mutableStateOf(DeviceToggles.hasDndAccess(context)) }

    LifecycleResumeEffect(Unit) {
        val hadDndAccess = dndAccessGranted
        exactAlarmsAllowed = WidgetAlarmScheduler.canScheduleExactAlarms(context)
        dndAccessGranted = DeviceToggles.hasDndAccess(context)
        if (exactAlarmsAllowed) WidgetAlarmScheduler.scheduleNextMinuteTick(context)
        // The DND widget's tap target depends on access, so redraw when it changes.
        if (hadDndAccess != dndAccessGranted) {
            scope.launch { WidgetRenderEngine.updateAllPlacedWidgets(context) }
        }
        if (exactAlarmsAllowed && dndAccessGranted) onDone()
        onPauseOrDispose { }
    }

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
                text = "Dot Widgets needs two permissions so your widgets stay accurate. Tap each one, switch it on, then come back.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A8A8A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            PermissionRow(
                icon = Icons.Default.Alarm,
                title = "Alarms & reminders",
                description = "Updates clock widgets exactly on the minute. Without it, Android can delay them by several minutes.",
                granted = exactAlarmsAllowed,
                accent = accent,
                onAllow = {
                    WidgetAlarmScheduler.exactAlarmSettingsIntent(context)?.let {
                        try { context.startActivity(it) } catch (_: Exception) { }
                    }
                }
            )

            PermissionRow(
                icon = Icons.Default.DoNotDisturbOn,
                title = "Do Not Disturb access",
                description = "Lets the DND toggle widget switch Do Not Disturb on and off.",
                granted = dndAccessGranted,
                accent = accent,
                onAllow = {
                    try { context.startActivity(DeviceToggles.dndAccessSettingsIntent()) } catch (_: Exception) { }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_skip")
            ) {
                Text(
                    text = if (exactAlarmsAllowed || dndAccessGranted) "CONTINUE" else "SKIP FOR NOW",
                    color = Color(0xFF8A8A8A),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    accent: AccentOption,
    onAllow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (granted) Color(0x338A8A8A) else accent.color, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent.color, modifier = Modifier.size(22.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = description,
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
