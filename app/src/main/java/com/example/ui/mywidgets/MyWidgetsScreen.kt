package com.example.ui.mywidgets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.builder.CustomWidget
import com.example.data.builder.CustomWidgetRepository
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.DotBadge
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotCardStyle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SegmentTabs
import com.example.ui.components.SizeBadge
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.config.WidgetSetupState
import com.example.ui.config.pinWidgetToHomeScreen
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import java.util.UUID
import kotlinx.coroutines.launch

private data class MyWidgetEntry(
    val id: String,
    val title: String,
    val caption: String,
    val definition: WidgetDefinition?,
    val custom: CustomWidget?,
    /** Set for entries placed on the home screen. */
    val instance: WidgetInstance? = null
)

@Composable
fun MyWidgetsScreen(
    onCreate: () -> Unit,
    onEditCustom: (CustomWidget) -> Unit,
    onEditInstance: (WidgetInstance, WidgetDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val accent by prefs.accent.collectAsState()

    val customWidgets by remember(context) { CustomWidgetRepository.observe(context) }.collectAsState(initial = emptyList())
    val placed by remember(context) { AppDatabase.getInstance(context).widgetInstanceDao().observeAll() }.collectAsState(initial = emptyList())

    // Custom definitions only exist in the registry after this runs, so entries are recomputed once
    // it has (registryVersion), otherwise a fresh launch shows cards without previews or actions.
    var registryVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(customWidgets) {
        WidgetRegistry.replaceCustom(customWidgets)
        registryVersion++
    }

    var filter by remember { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<CustomWidget?>(null) }

    val placedIds = remember(placed) { placed.map { it.widgetTypeId }.toSet() }

    val active = remember(placed, customWidgets, accent, registryVersion) {
        placed.mapNotNull { instance -> instance.toEntry(customWidgets, accent) }
    }
    val inactive = remember(placedIds, customWidgets, accent, registryVersion) {
        customWidgets
            .filter { it.widgetTypeId !in placedIds }
            .map { widget -> widget.toEntry(accent) }
    }

    val duplicate: (CustomWidget) -> Unit = { widget ->
        scope.launch {
            val copy = widget.copy(
                id = UUID.randomUUID().toString(),
                name = copyName(widget.name, customWidgets),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            CustomWidgetRepository.save(context, copy)
        }
    }

    val addToHome: (MyWidgetEntry) -> Unit = { entry ->
        val definition = entry.definition
        if (entry.instance == null && definition != null) {
            scope.launch {
                pinWidgetToHomeScreen(
                    context,
                    definition,
                    WidgetSetupState.defaults(definition, prefs)
                )
            }
        }
    }
    val entries = when (filter) {
        1 -> active
        2 -> inactive
        else -> active + inactive
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DotSpacing.lg, vertical = DotSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("My Widgets", style = DotType.Section, color = DotTheme.colors.textPrimary)
                Text(
                    text = "${active.size} active · ${inactive.size} saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = DotTheme.colors.textSecondary
                )
            }
            DotButton(
                text = "Create",
                onClick = onCreate,
                style = DotButtonStyle.Secondary,
                icon = Icons.Default.Add,
                modifier = Modifier.testTag("my_widgets_create")
            )
        }

        SegmentTabs(
            items = listOf("All", "Active", "Inactive"),
            selectedIndex = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(horizontal = DotSpacing.lg).testTag("my_widgets_tabs")
        )
        Spacer(Modifier.height(DotSpacing.md))

        if (entries.isEmpty()) {
            EmptyStateView(
                title = if (filter == 1) "No active widgets" else "No saved widgets",
                message = if (filter == 1) {
                    "Widgets placed on your home screen will appear here."
                } else {
                    "Build a widget and it will be saved here, ready to place."
                },
                actionLabel = "Create widget",
                onAction = onCreate,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = DotSpacing.lg, vertical = DotSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.md)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val editAction: (() -> Unit)? = when {
                        entry.custom != null -> ({ onEditCustom(entry.custom) })
                        entry.instance != null && entry.definition != null -> ({
                            onEditInstance(entry.instance, entry.definition)
                        })
                        else -> null
                    }
                    MyWidgetRow(
                        entry = entry,
                        accent = accent,
                        onEdit = editAction,
                        onAddToHome = if (entry.instance == null && entry.definition != null) {
                            { addToHome(entry) }
                        } else {
                            null
                        },
                        onDuplicate = entry.custom?.let { custom -> { duplicate(custom) } },
                        onDelete = entry.custom?.let { custom -> { deleteTarget = custom } }
                    )
                }
            }
        }
    }

    deleteTarget?.let { widget ->
        val isPlaced = widget.widgetTypeId in placedIds
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(if (isPlaced) "Delete placed widget?" else "Delete widget?") },
            text = {
                Text(
                    if (isPlaced) {
                        "'${widget.name}' is on your home screen. Remove it from the home screen first, then delete it here."
                    } else {
                        "'${widget.name}' will be deleted. This cannot be undone."
                    }
                )
            },
            confirmButton = {
                if (isPlaced) {
                    TextButton(onClick = { deleteTarget = null }) { Text("OK") }
                } else {
                    TextButton(onClick = {
                        scope.launch {
                            CustomWidgetRepository.delete(context, widget)
                            WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
                            deleteTarget = null
                            Toast.makeText(context, "Widget deleted", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Delete") }
                }
            },
            dismissButton = if (isPlaced) null else {
                { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyWidgetRow(
    entry: MyWidgetEntry,
    accent: AccentOption,
    onEdit: (() -> Unit)?,
    onAddToHome: (() -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    DotCard(
        style = DotCardStyle.Standard,
        padding = DotSpacing.md,
        modifier = Modifier.fillMaxWidth().testTag("my_widget_${entry.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .clip(DotShape.button)
                    .background(DotTheme.colors.surfaceSecondary)
                    .padding(DotSpacing.xs)
            ) {
                val definition = entry.definition
                if (definition != null) {
                    WidgetBitmapPreview(definition = definition, spec = entrySpec(definition, accent))
                }
            }
            Spacer(Modifier.width(DotSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = DotType.CardTitle,
                    color = DotTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = DotTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(DotSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs)) {
                    entry.definition?.sizes?.firstOrNull()?.let { SizeBadge(it) }
                    entry.definition?.let { DotBadge(it.category.title) }
                }
            }
        }
        if (onEdit != null || onAddToHome != null || onDuplicate != null || onDelete != null) {
            Spacer(Modifier.height(DotSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                if (onEdit != null) {
                    TextButton(onClick = onEdit, modifier = Modifier.testTag("my_widgets_edit")) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(DotSpacing.xs))
                        Text("Edit")
                    }
                }
                if (onAddToHome != null) {
                    TextButton(onClick = onAddToHome, modifier = Modifier.testTag("my_widgets_add_home")) {
                        Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(DotSpacing.xs))
                        Text("Add to home")
                    }
                }
                if (onDuplicate != null) {
                    TextButton(onClick = onDuplicate, modifier = Modifier.testTag("my_widgets_duplicate")) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(DotSpacing.xs))
                        Text("Duplicate")
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.testTag("my_widgets_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = DotTheme.colors.danger)
                        Spacer(Modifier.width(DotSpacing.xs))
                        Text("Delete", color = DotTheme.colors.danger)
                    }
                }
            }
        }
    }
}

private fun WidgetInstance.toEntry(customWidgets: List<CustomWidget>, accent: AccentOption): MyWidgetEntry {
    val definition = WidgetRegistry.getById(widgetTypeId)
    val custom = customWidgets.firstOrNull { it.widgetTypeId == widgetTypeId }
    return MyWidgetEntry(
        id = "placed_$appWidgetId",
        title = definition?.title ?: custom?.name ?: widgetTypeId,
        caption = "On your home screen",
        definition = definition,
        custom = custom,
        instance = this
    )
}

/** First free name of the form "Name copy", "Name copy 2"... capped at the 40-char limit. */
private fun copyName(name: String, existing: List<CustomWidget>): String {
    val base = name.trim().ifEmpty { "Widget" }
    val taken = existing.map { it.name }.toSet()
    var index = 1
    while (true) {
        val suffix = if (index == 1) " copy" else " copy $index"
        val candidate = (base + suffix).take(40)
        if (candidate !in taken) return candidate
        index++
    }
}

private fun CustomWidget.toEntry(accent: AccentOption): MyWidgetEntry {
    val definition = WidgetRegistry.getById(widgetTypeId)
    return MyWidgetEntry(
        id = "saved_$id",
        title = name,
        caption = "Saved · not placed",
        definition = definition,
        custom = this
    )
}

@Composable
private fun entrySpec(definition: WidgetDefinition, accent: AccentOption): WidgetSpec {
    val context = LocalContext.current
    val size = definition.sizes.first()
    val dims = remember(size) { SizeResolver.resolveDimensions(context, size) }
    return remember(definition.id, size, accent) {
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = accent,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.AUTO,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )
    }
}
