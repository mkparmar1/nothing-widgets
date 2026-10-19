package com.example.ui.config

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.core.bitmap.SizeBucket
import com.example.data.prefs.PreferencesManager
import com.example.widgets.base.DotWidgetProviderL
import com.example.widgets.base.DotWidgetProviderM
import com.example.widgets.base.DotWidgetProviderS
import com.example.widgets.base.WidgetDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigSheet(
    definition: WidgetDefinition,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val initial = remember(definition.id) { WidgetSetupState.defaults(definition, prefs) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        WidgetSetupContent(
            definition = definition,
            initial = initial,
            lockedSize = null,
            saveLabel = "ADD TO HOME SCREEN",
            onSave = { state ->
                pinWidgetToHomeScreen(context, definition, state)
                onDismiss()
            }
        )
    }
}

fun pinWidgetToHomeScreen(
    context: Context,
    definition: WidgetDefinition,
    state: WidgetSetupState
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val providerClass = when (state.size) {
        SizeBucket.S -> DotWidgetProviderS::class.java
        SizeBucket.M -> DotWidgetProviderM::class.java
        SizeBucket.L -> DotWidgetProviderL::class.java
    }
    val provider = ComponentName(context, providerClass)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val successIntent = Intent(context, WidgetConfigReceiver::class.java).apply {
                putExtra("widget_type_id", definition.id)
                putExtra("config_json", state.toConfigJson(definition.id))
            }
            val successPendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                successIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            appWidgetManager.requestPinAppWidget(provider, null, successPendingIntent)
            Toast.makeText(context, "Placing ${definition.title}...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Launcher does not support direct widget pinning. Add from launcher widget drawer.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Add widget from your home screen widget menu.", Toast.LENGTH_LONG).show()
    }
}
