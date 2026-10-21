package com.example.ui.config

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetProviders
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, result)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val providerInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        val size = providerInfo?.let(::sizeForProvider)
        lockScreenOnly = providerInfo?.let { WidgetProviders.isLockScreenProvider(it.provider.className) } == true
        if (providerInfo == null || size == null) {
            Log.e(TAG, "Cannot resolve provider size for app widget $appWidgetId")
            finish()
            return
        }

        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).widgetInstanceDao().getById(appWidgetId)
            }
            val editInstance = existing?.takeIf { WidgetSetupState.isConfigured(it) }
            // A new lock-screen widget: start from the design picked with "Add to lock screen".
            val pending = if (editInstance == null && lockScreenOnly) LockScreenWidgets.takePending(applicationContext) else null
            val pendingDefinition = pending?.let { WidgetRegistry.getById(it.first) }?.takeIf { size in it.sizes && it.lockSafe }
            val initialDefinition = editInstance
                ?.let { WidgetRegistry.getById(it.widgetTypeId) }
                ?.takeIf { size in it.sizes }
                ?: pendingDefinition
            showConfiguration(appWidgetId, size, editInstance, initialDefinition, pending?.second?.takeIf { pendingDefinition != null })
        }
    }

    /** True when configuring a lock-screen widget: the picker then lists only lock-safe designs. */
    private var lockScreenOnly = false

    private fun showConfiguration(
        appWidgetId: Int,
        size: SizeBucket,
        editInstance: WidgetInstance?,
        initialDefinition: WidgetDefinition?,
        prefillConfig: String? = null
    ) {
        val preferences = PreferencesManager.getInstance(this)
        val initialState = if (editInstance != null && initialDefinition != null) {
            WidgetSetupState.fromInstance(editInstance, initialDefinition, preferences, size)
        } else if (prefillConfig != null && initialDefinition != null) {
            WidgetSetupState.defaults(initialDefinition, preferences, size, prefillConfig)
        } else {
            null
        }
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        setContent {
            // The app's own accent and background, like MainActivity; the widget's chosen accent
            // only affects the preview inside the setup content.
            AppScreen(accent = preferences.getSavedAccent()) {
                var definition by remember {
                    mutableStateOf(initialDefinition)
                }
                var setupState by remember {
                    mutableStateOf(initialState)
                }
                var isSaving by remember { mutableStateOf(false) }
                var saveError by remember { mutableStateOf<String?>(null) }

                if (definition == null) {
                    WidgetDesignPicker(
                        size = size,
                        lockScreenOnly = lockScreenOnly,
                        onSelect = { selected ->
                            definition = selected
                            setupState = WidgetSetupState.defaults(
                                selected,
                                preferences,
                                size,
                                com.example.data.setups.SetupShareRepository.importedConfig(this@WidgetConfigureActivity, selected.id)?.toString()
                            )
                            saveError = null
                        }
                    )
                } else {
                    val selectedDefinition = definition!!
                    WidgetSetupContent(
                        definition = selectedDefinition,
                        initial = setupState ?: WidgetSetupState.defaults(
                            selectedDefinition,
                            preferences,
                            size,
                            com.example.data.setups.SetupShareRepository.importedConfig(this@WidgetConfigureActivity, selectedDefinition.id)?.toString()
                        ),
                        lockedSize = size,
                        saveLabel = if (editInstance == null) "ADD WIDGET" else "SAVE CHANGES",
                        saveError = saveError,
                        onStateChange = { setupState = it },
                        onSave = { state ->
                            if (!isSaving && com.example.data.billing.Entitlements.hasFullAccess(applicationContext)) {
                                isSaving = true
                                saveError = null
                                lifecycleScope.launch {
                                    try {
                                        val instance = WidgetInstance(
                                            appWidgetId = appWidgetId,
                                            widgetTypeId = selectedDefinition.id,
                                            schemaVersion = 1,
                                            configJson = state.toConfigJson(
                                                selectedDefinition.id,
                                                editInstance?.configJson.takeIf { initialDefinition != null }
                                            )
                                        )
                                        withContext(Dispatchers.IO) {
                                            AppDatabase.getInstance(applicationContext)
                                                .widgetInstanceDao()
                                                .insertOrUpdate(instance)
                                        }
                                        val views = WidgetRenderEngine.buildRemoteViews(
                                            applicationContext,
                                            appWidgetId,
                                            instance
                                        )
                                        AppWidgetManager.getInstance(this@WidgetConfigureActivity)
                                            .updateAppWidget(appWidgetId, views)
                                        setResult(RESULT_OK, result)
                                        finish()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to save configuration for widget $appWidgetId", e)
                                        saveError = "Couldn't save this widget. ${e.localizedMessage ?: "Please try again."}"
                                        isSaving = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    /** Home-screen and lock-screen providers alike (see [WidgetProviders]). */
    private fun sizeForProvider(info: AppWidgetProviderInfo): SizeBucket? =
        WidgetProviders.sizeForProviderClass(info.provider.className)

    companion object {
        private const val TAG = "WidgetConfigureActivity"
    }
}

/**
 * Same look as the main app: theme, black/white app background, readable text colours, and
 * content kept clear of the status and navigation bars.
 */
@Composable
private fun AppScreen(accent: AccentOption, content: @Composable () -> Unit) {
    DotWidgetsTheme(accentOption = accent) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun WidgetDesignPicker(
    size: SizeBucket,
    lockScreenOnly: Boolean,
    onSelect: (WidgetDefinition) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { PreferencesManager.getInstance(context) }
    val definitionsByCategory = remember(size) {
        WidgetRegistry.getAll()
            .filter { size in it.sizes && (!lockScreenOnly || it.lockSafe) }
            .groupBy { it.category }
            .toList()
    }
    val accent = preferences.getSavedAccent()
    val dark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "CHOOSE A ${size.label.uppercase()} WIDGET",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            definitionsByCategory.forEach { (category, definitions) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "category_${category.name}") {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }
                items(definitions, key = { it.id }) { definition ->
                    val dimensions = remember(size) { SizeResolver.resolveDimensions(context, size) }
                    val spec = remember(definition.id, accent, dark, dimensions) {
                        WidgetSpec(
                            widthPx = dimensions.widthPx,
                            heightPx = dimensions.heightPx,
                            density = dimensions.density,
                            sizeBucket = size,
                            accent = accent,
                            bgMode = preferences.getSavedBgMode(),
                            themeMode = ThemeMode.AUTO,
                            isDark = dark,
                            cornerRadiusDp = 16,
                            showLabel = true,
                            tapAction = definition.defaultTap
                        )
                    }
                    Card(
                        onClick = { onSelect(definition) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            WidgetBitmapPreview(
                                definition = definition,
                                spec = spec,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = definition.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = size.cells,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
