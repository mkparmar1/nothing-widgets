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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.example.R
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.MaterialYouPalette
import com.example.core.theme.ThemeMode
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var lockGuideFor by remember { mutableStateOf<WidgetSetupState?>(null) }

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
            onAddToLockScreen = { state ->
                if (LockScreenWidgets.isSupported(context)) {
                    LockScreenWidgets.savePending(context, definition.id, state.toConfigJson(definition.id))
                }
                lockGuideFor = state
            },
            onSave = { state ->
                scope.launch {
                    pinWidgetToHomeScreen(context, definition, state)
                    onDismiss()
                }
            }
        )
    }

    lockGuideFor?.let { state ->
        LockScreenGuideDialog(
            definition = definition,
            supported = remember { LockScreenWidgets.isSupported(context) },
            onAddToHomeScreen = {
                lockGuideFor = null
                scope.launch {
                    pinWidgetToHomeScreen(context, definition, state)
                    onDismiss()
                }
            },
            onDismiss = { lockGuideFor = null }
        )
    }
}

suspend fun pinWidgetToHomeScreen(
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

            // Without a preview the launcher's "add widget" dialog shows the provider's generic
            // placeholder image, not the design being placed.
            val extras = runCatching { pinPreview(context, definition, state) }.getOrNull()?.let { preview ->
                Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
            }
            appWidgetManager.requestPinAppWidget(provider, extras, successPendingIntent)
            Toast.makeText(context, "Placing ${definition.title}...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Launcher does not support direct widget pinning. Add from launcher widget drawer.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Add widget from your home screen widget menu.", Toast.LENGTH_LONG).show()
    }
}

/** The chosen design drawn as the launcher's pin-dialog preview, small enough to cross Binder. */
private suspend fun pinPreview(
    context: Context,
    definition: WidgetDefinition,
    state: WidgetSetupState
): RemoteViews {
    val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val isDark = when (state.theme) {
        ThemeMode.FORCE_DARK -> true
        ThemeMode.FORCE_LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }
    val dimensions = SizeResolver.resolveDimensions(context, state.size)
    val accentColor = state.customAccent ?: if (state.accent == AccentOption.DYNAMIC) MaterialYouPalette.resolve(context) else state.accent.hex
    val spec = WidgetSpec(
        widthPx = dimensions.widthPx,
        heightPx = dimensions.heightPx,
        density = dimensions.density,
        sizeBucket = state.size,
        accent = state.accent,
        accentColor = accentColor,
        bgMode = state.background,
        bgAlpha = state.bgAlpha,
        themeMode = state.theme,
        isDark = state.skin.resolveDark(isDark),
        cornerRadiusDp = state.cornerRadius,
        showLabel = state.showLabel,
        tapAction = state.tapAction,
        configJson = state.toConfigJson(definition.id)
    )
    var bitmap = WidgetRenderEngine.renderBitmap(context, definition, spec, 0)
    val maxSide = 480
    if (bitmap.width > maxSide || bitmap.height > maxSide) {
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }
    return RemoteViews(context.packageName, R.layout.widget_canvas_layout).apply {
        setImageViewBitmap(R.id.widget_image_view, bitmap)
    }
}
