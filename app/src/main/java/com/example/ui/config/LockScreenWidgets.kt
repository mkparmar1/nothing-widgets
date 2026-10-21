package com.example.ui.config

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.widgets.base.WidgetDefinition

/**
 * Android has no API for an app to place a widget on the lock screen (requestPinAppWidget always
 * goes to the home-screen launcher). Where the lock screen hosts standard widgets, the user adds
 * "Dot Widgets • Lock" there themselves; we remember the design chosen in the app so the lock-screen
 * setup opens with it already filled in.
 */
object LockScreenWidgets {

    private const val PREFS = "dot_widgets_lock_pending"
    private const val KEY_ID = "widget_type_id"
    private const val KEY_CONFIG = "config_json"
    private const val KEY_AT = "saved_at"
    private const val TTL_MS = 30 * 60 * 1000L

    /**
     * Best guess at whether this phone's lock screen hosts standard app widgets: Android 16's
     * lock-screen widget hub (Pixel, QPR2+) keeps its on/off switch in this secure setting. Nothing OS
     * and older Android don't have it; their lock screens only show the maker's own widgets.
     */
    fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return runCatching {
            Settings.Secure.getString(context.contentResolver, "glanceable_hub_enabled") != null
        }.getOrDefault(false)
    }

    fun savePending(context: Context, widgetTypeId: String, configJson: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ID, widgetTypeId)
            .putString(KEY_CONFIG, configJson)
            .putLong(KEY_AT, System.currentTimeMillis())
            .apply()
    }

    /** The design picked in the app for the next lock-screen widget, once, if it's recent. */
    fun takePending(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ID, null)
        val config = prefs.getString(KEY_CONFIG, null)
        val fresh = System.currentTimeMillis() - prefs.getLong(KEY_AT, 0L) < TTL_MS
        prefs.edit().clear().apply()
        return if (id != null && config != null && fresh) id to config else null
    }
}

/** What "Add to lock screen" can honestly do on this phone. */
@Composable
fun LockScreenGuideDialog(
    definition: WidgetDefinition,
    supported: Boolean,
    onAddToHomeScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("lock_screen_guide"),
        title = {
            Text(
                if (supported) "Add to your lock screen" else "Lock screen not available",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (supported) {
                    Text(
                        "Android only lets you add lock-screen widgets from the lock screen itself. " +
                            "${definition.title} and your settings are saved for it:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "1. Lock your phone and wake the screen.\n" +
                            "2. Open the lock-screen widgets and tap Edit / Add widget.\n" +
                            "3. Choose Dot Widgets • Lock (Small or Wide).\n" +
                            "4. Your design opens ready: tap Add widget.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Saved for the next 30 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        "This phone's lock screen only shows its maker's own widgets. Android doesn't " +
                            "let other apps add widgets there yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${definition.title} is lock-screen ready, so it will work there as soon as your " +
                            "phone supports lock-screen widgets. For now you can add it to your home screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (supported) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("lock_guide_ok")) {
                    Text("GOT IT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onAddToHomeScreen, modifier = Modifier.testTag("lock_guide_home")) {
                    Text("ADD TO HOME SCREEN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = if (supported) null else {
            {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    )
}
