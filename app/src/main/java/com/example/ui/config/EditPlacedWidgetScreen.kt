package com.example.ui.config

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Edits a widget that is already on the home screen: same [WidgetSetupContent] as everywhere else,
 * with the current instance as its starting state, then redraws the placed widget in place.
 */
@Composable
fun EditPlacedWidgetScreen(
    instance: WidgetInstance,
    definition: WidgetDefinition,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val initial = remember(instance.appWidgetId, instance.configJson) {
        WidgetSetupState.fromInstance(instance, definition, prefs)
    }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().testTag("edit_placed_screen")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("edit_placed_back")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to My Widgets",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "Edit placed widget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(Modifier.weight(1f)) {
            WidgetSetupContent(
                definition = definition,
                initial = initial,
                lockedSize = initial.size,
                saveLabel = "Save changes",
                saveError = error,
                onSave = { state ->
                    if (!saving) {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                val updated = instance.copy(
                                    configJson = state.toConfigJson(definition.id, instance.configJson)
                                )
                                withContext(Dispatchers.IO) {
                                    AppDatabase.getInstance(context).widgetInstanceDao().insertOrUpdate(updated)
                                }
                                val views = WidgetRenderEngine.buildRemoteViews(context, instance.appWidgetId, updated)
                                AppWidgetManager.getInstance(context)
                                    .updateAppWidget(instance.appWidgetId, views)
                                WidgetAlarmScheduler.scheduleNextSmartStackSlot(context)
                                onDismiss()
                            } catch (e: Exception) {
                                error = "Couldn't save this widget. ${e.localizedMessage ?: "Please try again."}"
                                saving = false
                            }
                        }
                    }
                }
            )
        }
    }
}
