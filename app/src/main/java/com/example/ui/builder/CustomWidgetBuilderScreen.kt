package com.example.ui.builder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.builder.BuilderAlignment
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.BuilderTextColor
import com.example.data.builder.CustomWidget
import com.example.data.builder.CustomWidgetRepository
import com.example.data.photos.BackgroundImageStore
import com.example.ui.components.DotBadge
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotCardStyle
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.components.dotFieldColors
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetSpec
import com.example.widgets.builder.BuilderRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomWidgetBuilderScreen(
    existing: CustomWidget? = null,
    onDismiss: () -> Unit,
    onSaved: (CustomWidget) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "My widget") }
    var blocks by remember(existing?.id) {
        mutableStateOf(existing?.let { BuilderLayout.parse(it.layoutJson)?.blocks }.orEmpty())
    }
    var supportedSizes by remember(existing?.id) { mutableStateOf(existing?.supportedSizes ?: SizeBucket.entries.toSet()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf(emptyList<List<BuilderBlock>>()) }
    var redo by remember { mutableStateOf(emptyList<List<BuilderBlock>>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var sliderHistorySaved by remember { mutableStateOf(false) }
    var showMoreBlockTypes by remember(existing?.id) { mutableStateOf(false) }
    var showAdvancedSettings by remember(selectedId) { mutableStateOf(false) }
    var showTemplates by remember(existing?.id) { mutableStateOf(false) }
    val selected = blocks.firstOrNull { it.id == selectedId }

    // The preview redraws from a debounced copy so dragging a block doesn't re-render every frame.
    var previewBlocks by remember { mutableStateOf(blocks) }
    LaunchedEffect(blocks) {
        delay(120)
        previewBlocks = blocks
    }

    fun change(next: List<BuilderBlock>, rememberForUndo: Boolean = true) {
        sliderHistorySaved = false
        if (rememberForUndo) undo = (undo + listOf(blocks)).takeLast(40)
        redo = emptyList()
        blocks = next
    }

    fun changeFromSlider(next: List<BuilderBlock>) {
        if (!sliderHistorySaved) {
            undo = (undo + listOf(blocks)).takeLast(40)
            redo = emptyList()
            sliderHistorySaved = true
        }
        blocks = next
    }

    fun save() {
        saving = true
        error = null
        scope.launch {
            try {
                val widget = CustomWidget(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    name = name.trim(),
                    layoutJson = BuilderLayout(blocks).toJson(),
                    sizesCsv = SizeBucket.entries.filter { it in supportedSizes }.joinToString(",") { it.name },
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                CustomWidgetRepository.save(context, widget)
                onSaved(widget)
            } catch (e: IllegalArgumentException) {
                error = e.message ?: "Please check your design."
            } catch (e: Exception) {
                error = "Couldn't save the widget. Please try again."
                android.util.Log.e("CustomWidgetBuilder", "Unable to save custom widget", e)
            } finally {
                saving = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val id = withContext(Dispatchers.IO) { BackgroundImageStore.save(context, uri) }
                if (id == null) {
                    error = "Couldn't open that image."
                } else {
                    val target = selected
                    if (target?.type == BuilderBlockType.IMAGE) {
                        change(blocks.map { if (it.id == target.id) it.copy(imageId = id) else it })
                    } else {
                        val block = BuilderBlock(type = BuilderBlockType.IMAGE, imageId = id, x = 4, y = 4)
                        change(blocks + block)
                        selectedId = block.id
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("builder_screen")
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (existing == null) "Build a widget" else "Edit widget",
                        style = DotType.Section,
                        color = DotTheme.colors.textPrimary
                    )
                    Text(
                        if (existing == null) "Start with a template, then make it yours."
                        else "Tap a block to edit it, or drag it to move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close builder", tint = DotTheme.colors.textSecondary)
                }
            }

            BuilderPreview(
                blocks = previewBlocks,
                sizes = supportedSizes,
                name = name,
                existingId = existing?.id,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .testTag("builder_preview")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Widget name") },
                    singleLine = true,
                    colors = dotFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("builder_name")
                )

                if (existing == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Start from a template", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                                Text(
                                    "Optional. Pick a ready-made layout, then tweak it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DotTheme.colors.textSecondary
                                )
                            }
                            DotButton(
                                text = if (showTemplates) "Hide" else "Browse",
                                onClick = { showTemplates = !showTemplates },
                                style = DotButtonStyle.Ghost,
                                modifier = Modifier.testTag("builder_templates_toggle")
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = showTemplates) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                            ) {
                                BuilderTemplates.all.forEachIndexed { index, (title, layout) ->
                                    DotCard(
                                        modifier = Modifier.fillMaxWidth(0.48f).testTag("builder_template_$index"),
                                        style = DotCardStyle.Interactive,
                                        padding = DotSpacing.md,
                                        onClick = {
                                            change(layout.blocks)
                                            selectedId = null
                                            showTemplates = false
                                        }
                                    ) {
                                        Text(title, style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                                        Text(
                                            layout.blocks.take(3).joinToString(" + ") { it.type.displayLabel() },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DotTheme.colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Arrange blocks",
                            style = DotType.CardTitle,
                            color = DotTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        DotLabel("${blocks.size} / ${BuilderLayout.MAX_BLOCKS} blocks")
                    }
                    Text(
                        "Tap a block to select it. Drag it to move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotTheme.colors.textSecondary
                    )
                    BuilderCanvas(
                        blocks = blocks,
                        selectedId = selectedId,
                        onSelect = { selectedId = it },
                        onDragStart = {
                            selectedId = it
                            undo = (undo + listOf(blocks)).takeLast(40)
                            redo = emptyList()
                            sliderHistorySaved = false
                        },
                        onMove = { id, x, y ->
                            blocks = blocks.map { if (it.id == id) it.copy(x = x, y = y) else it }
                        },
                        modifier = Modifier.testTag("builder_canvas")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                        DotButton(
                            text = "Undo",
                            onClick = {
                                if (undo.isNotEmpty()) {
                                    redo = (redo + listOf(blocks)).takeLast(40)
                                    blocks = undo.last()
                                    undo = undo.dropLast(1)
                                }
                            },
                            style = DotButtonStyle.Ghost,
                            icon = Icons.AutoMirrored.Filled.Undo,
                            enabled = undo.isNotEmpty()
                        )
                        DotButton(
                            text = "Redo",
                            onClick = {
                                if (redo.isNotEmpty()) {
                                    undo = (undo + listOf(blocks)).takeLast(40)
                                    blocks = redo.last()
                                    redo = redo.dropLast(1)
                                }
                            },
                            style = DotButtonStyle.Ghost,
                            icon = Icons.AutoMirrored.Filled.Redo,
                            enabled = redo.isNotEmpty()
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                    Text("Widget sizes", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                    ) {
                        SizeBucket.entries.forEach { size ->
                            DotChip(
                                label = size.label,
                                selected = size in supportedSizes,
                                onClick = {
                                    supportedSizes =
                                        if (size in supportedSizes) supportedSizes - size else supportedSizes + size
                                },
                                modifier = Modifier.testTag("builder_size_${size.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                    Text("Add content", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
                    Text(
                        "Choose what you want to show on your widget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotTheme.colors.textSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                    ) {
                        val commonTypes = listOf(
                            BuilderBlockType.TIME,
                            BuilderBlockType.DATE,
                            BuilderBlockType.WEATHER,
                            BuilderBlockType.BATTERY,
                            BuilderBlockType.TEXT
                        )
                        val types = if (showMoreBlockTypes) BuilderBlockType.entries else commonTypes
                        types.forEach { type ->
                            DotChip(
                                label = type.displayLabel(),
                                selected = false,
                                onClick = {
                                    if (blocks.size < BuilderLayout.MAX_BLOCKS) {
                                        if (type == BuilderBlockType.IMAGE) {
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        } else {
                                            val block = BuilderBlock(
                                                type = type,
                                                x = 1,
                                                y = (blocks.size * 3).coerceAtMost(14),
                                                width = 14,
                                                height = 3,
                                                text = if (type == BuilderBlockType.TEXT) "Your text" else ""
                                            )
                                            change(blocks + block)
                                            selectedId = block.id
                                        }
                                    }
                                },
                                icon = type.icon(),
                                modifier = Modifier.testTag("builder_add_${type.name.lowercase()}")
                            )
                        }
                        DotButton(
                            text = if (showMoreBlockTypes) "Fewer options" else "More options",
                            onClick = { showMoreBlockTypes = !showMoreBlockTypes },
                            style = DotButtonStyle.Ghost,
                            modifier = Modifier.testTag("builder_more_block_types")
                        )
                    }
                }

                selected?.let { block ->
                    DotCard(padding = DotSpacing.md) {
                        Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                            Text(
                                "Edit ${block.type.displayLabel()}",
                                style = DotType.CardTitle,
                                color = DotTheme.colors.textPrimary
                            )
                            if (block.type == BuilderBlockType.TEXT || block.type == BuilderBlockType.DATE || block.type == BuilderBlockType.PROGRESS) {
                                OutlinedTextField(
                                    value = block.text,
                                    onValueChange = { value ->
                                        change(blocks.map { if (it.id == block.id) it.copy(text = value.take(120)) else it })
                                    },
                                    label = {
                                        Text(
                                            when (block.type) {
                                                BuilderBlockType.DATE -> "Date format (e.g. EEE, d MMM)"
                                                BuilderBlockType.PROGRESS -> "Progress 0-100 (blank = battery)"
                                                else -> "Text"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    colors = dotFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (block.type == BuilderBlockType.IMAGE) {
                                DotButton(
                                    text = "Choose image",
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    style = DotButtonStyle.Secondary
                                )
                            }
                            DotButton(
                                text = if (showAdvancedSettings) "Hide fine-tuning" else "Size, position & style",
                                onClick = { showAdvancedSettings = !showAdvancedSettings },
                                style = DotButtonStyle.Ghost,
                                modifier = Modifier.testTag("builder_advanced_layout")
                            )
                            if (showAdvancedSettings) {
                                SliderRow(label = "Position X", value = block.x, range = 0f..15f, steps = 14) { value ->
                                    changeFromSlider(blocks.map { if (it.id == block.id) it.copy(x = value.roundToInt()) else it })
                                }
                                SliderRow(label = "Position Y", value = block.y, range = 0f..15f, steps = 14) { value ->
                                    changeFromSlider(blocks.map { if (it.id == block.id) it.copy(y = value.roundToInt()) else it })
                                }
                                SliderRow(label = "Width", value = block.width, range = 1f..16f, steps = 14) { value ->
                                    changeFromSlider(blocks.map { if (it.id == block.id) it.copy(width = value.roundToInt()) else it })
                                }
                                SliderRow(label = "Height", value = block.height, range = 1f..16f, steps = 14) { value ->
                                    changeFromSlider(blocks.map { if (it.id == block.id) it.copy(height = value.roundToInt()) else it })
                                }
                                SliderRow(label = "Text size", value = block.scale, range = 0.5f..2f, display = { "%.1fx".format(it) }) { value ->
                                    changeFromSlider(blocks.map { if (it.id == block.id) it.copy(scale = value) else it })
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                                ) {
                                    BuilderTextColor.entries.forEach { color ->
                                        DotChip(
                                            label = color.displayLabel(),
                                            selected = color == block.color,
                                            onClick = {
                                                change(blocks.map { if (it.id == block.id) it.copy(color = color) else it })
                                            }
                                        )
                                    }
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                                ) {
                                    BuilderAlignment.entries.forEach { alignment ->
                                        DotChip(
                                            label = alignment.displayLabel(),
                                            selected = alignment == block.alignment,
                                            onClick = {
                                                change(blocks.map { if (it.id == block.id) it.copy(alignment = alignment) else it })
                                            }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                                DotButton(
                                    text = "Duplicate",
                                    onClick = {
                                        if (blocks.size < BuilderLayout.MAX_BLOCKS) {
                                            val copy = block.copy(
                                                id = java.util.UUID.randomUUID().toString(),
                                                x = (block.x + 1).coerceAtMost(15),
                                                y = (block.y + 1).coerceAtMost(15)
                                            )
                                            change(blocks + copy)
                                            selectedId = copy.id
                                        }
                                    },
                                    style = DotButtonStyle.Secondary,
                                    enabled = blocks.size < BuilderLayout.MAX_BLOCKS
                                )
                                DotButton(
                                    text = "Delete",
                                    onClick = {
                                        change(blocks.filterNot { it.id == block.id })
                                        selectedId = null
                                    },
                                    style = DotButtonStyle.Destructive
                                )
                            }
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(DotTheme.colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                error?.let {
                    Text(it, color = DotTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(DotSpacing.sm))
                }
                DotButton(
                    text = if (existing == null) "Build widget" else "Save changes",
                    onClick = { save() },
                    loading = saving,
                    enabled = name.isNotBlank() && blocks.isNotEmpty() && supportedSizes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("builder_save")
                )
            }
        }
}

/** The live widget render, refreshed from a debounced copy of the layout. */
@Composable
private fun BuilderPreview(
    blocks: List<BuilderBlock>,
    sizes: Set<SizeBucket>,
    name: String,
    existingId: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val size = SizeBucket.entries.firstOrNull { it in sizes } ?: SizeBucket.M
    val definition = remember(blocks, size, name, existingId, sizes) {
        val layoutJson = runCatching { BuilderLayout(blocks).toJson() }.getOrNull()
        if (layoutJson == null) {
            null
        } else {
            BuilderRenderer.definition(
                CustomWidget(
                    id = existingId ?: "builder-preview",
                    name = name.trim().ifBlank { "My widget" },
                    layoutJson = layoutJson,
                    sizesCsv = SizeBucket.entries.filter { it in sizes }.joinToString(",") { it.name }
                )
            )
        }
    }
    val dimensions = remember(size) { SizeResolver.resolveDimensions(context, size) }
    val spec = remember(dimensions, size) {
        WidgetSpec(
            widthPx = dimensions.widthPx,
            heightPx = dimensions.heightPx,
            density = dimensions.density,
            sizeBucket = size,
            accent = AccentOption.RED,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = false,
            tapAction = TapAction.OPEN_MAIN
        )
    }

    DotCard(style = DotCardStyle.Information, padding = DotSpacing.md, modifier = modifier) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            if (definition != null) {
                WidgetBitmapPreview(
                    definition = definition,
                    spec = spec,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            } else {
                Text(
                    "Your preview appears here",
                    style = MaterialTheme.typography.bodySmall,
                    color = DotTheme.colors.textMuted
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = DotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotLabel("Preview")
            Spacer(Modifier.weight(1f))
            if (definition != null) {
                DotBadge("${size.label} · ${blocks.size} blocks")
            }
        }
    }
}

@Composable
private fun BuilderCanvas(
    blocks: List<BuilderBlock>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDragStart: (String) -> Unit,
    onMove: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DotTheme.colors.surface)
    ) {
        val cellW = maxWidth / 16
        val cellH = maxHeight / 16
        val gridColor = DotTheme.colors.border.copy(alpha = 0.45f)
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..16) {
                drawLine(gridColor, Offset(size.width * i / 16f, 0f), Offset(size.width * i / 16f, size.height), 1f)
                drawLine(gridColor, Offset(0f, size.height * i / 16f), Offset(size.width, size.height * i / 16f), 1f)
            }
        }
        blocks.forEach { block ->
            val active = block.id == selectedId
            Box(
                Modifier
                    .offset(cellW * block.x, cellH * block.y)
                    .size(
                        cellW * block.width.coerceAtMost(16 - block.x),
                        cellH * block.height.coerceAtMost(16 - block.y)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                        else DotTheme.colors.surfaceElevated.copy(alpha = 0.9f)
                    )
                    .clickable { onSelect(block.id) }
                    .pointerInput(block.id, cellW, cellH) {
                        var originX = 0
                        var originY = 0
                        var movedX = 0f
                        var movedY = 0f
                        detectDragGestures(
                            onDragStart = {
                                originX = block.x
                                originY = block.y
                                movedX = 0f
                                movedY = 0f
                                onDragStart(block.id)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                movedX += amount.x
                                movedY += amount.y
                                val dx = (movedX / cellW.toPx()).roundToInt()
                                val dy = (movedY / cellH.toPx()).roundToInt()
                                onMove(
                                    block.id,
                                    (originX + dx).coerceIn(0, 15),
                                    (originY + dy).coerceIn(0, 15)
                                )
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    blockLabel(block),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    color = if (active) MaterialTheme.colorScheme.primary else DotTheme.colors.textPrimary
                )
            }
        }
        if (blocks.isEmpty()) {
            Text(
                "Add a block below",
                Modifier.align(Alignment.Center),
                color = DotTheme.colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValue: (Float) -> Unit
) {
    SliderRow(label = label, value = value.toFloat(), range = range, steps = steps, display = { it.roundToInt().toString() }, onValue = onValue)
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    display: (Float) -> String = { "%.1f".format(it) },
    onValue: (Float) -> Unit
) {
    Column {
        Text(
            "$label · ${display(value)}",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Slider(value = value, onValueChange = onValue, valueRange = range, steps = steps)
    }
}

private fun BuilderBlockType.displayLabel(): String =
    name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

private fun BuilderTextColor.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

private fun BuilderAlignment.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

private fun BuilderBlockType.icon(): ImageVector = when (this) {
    BuilderBlockType.TIME -> Icons.Default.AccessTime
    BuilderBlockType.DATE -> Icons.Default.CalendarToday
    BuilderBlockType.BATTERY -> Icons.Default.BatteryFull
    BuilderBlockType.WEATHER -> Icons.Default.WbSunny
    BuilderBlockType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    BuilderBlockType.NEXT_EVENT -> Icons.AutoMirrored.Filled.EventNote
    BuilderBlockType.TEXT -> Icons.Default.TextFields
    BuilderBlockType.PROGRESS -> Icons.Default.DonutLarge
    BuilderBlockType.IMAGE -> Icons.Default.Image
    BuilderBlockType.DIVIDER -> Icons.Default.HorizontalRule
    BuilderBlockType.SPACER -> Icons.Default.SpaceBar
}

private fun blockLabel(block: BuilderBlock): String = when (block.type) {
    BuilderBlockType.TEXT -> block.text.ifBlank { "Text" }
    BuilderBlockType.IMAGE -> "Image"
    BuilderBlockType.PROGRESS -> "Progress ring"
    else -> block.type.name.replace('_', ' ')
}

private object BuilderTemplates {
    private fun block(type: BuilderBlockType, x: Int, y: Int, width: Int, height: Int, text: String = "") =
        BuilderBlock(type = type, x = x, y = y, width = width, height = height, text = text)

    val all = listOf(
        "Clock + battery" to BuilderLayout(listOf(block(BuilderBlockType.TIME, 1, 2, 14, 6), block(BuilderBlockType.DATE, 1, 9, 10, 3), block(BuilderBlockType.BATTERY, 1, 13, 7, 2))),
        "Daily dashboard" to BuilderLayout(listOf(block(BuilderBlockType.DATE, 1, 1, 14, 3), block(BuilderBlockType.TIME, 1, 4, 14, 5), block(BuilderBlockType.WEATHER, 1, 10, 14, 3), block(BuilderBlockType.BATTERY, 1, 13, 8, 2))),
        "Focus progress" to BuilderLayout(listOf(block(BuilderBlockType.TEXT, 1, 1, 14, 3, "FOCUS"), block(BuilderBlockType.PROGRESS, 3, 4, 10, 10), block(BuilderBlockType.TEXT, 1, 14, 14, 2, "TODAY"))),
        "Steps tracker" to BuilderLayout(listOf(block(BuilderBlockType.TEXT, 1, 1, 14, 3, "DAILY STEPS"), block(BuilderBlockType.STEPS, 1, 5, 14, 5), block(BuilderBlockType.PROGRESS, 3, 10, 10, 5))),
        "Agenda glance" to BuilderLayout(listOf(block(BuilderBlockType.DATE, 1, 1, 14, 3), block(BuilderBlockType.DIVIDER, 1, 5, 14, 1), block(BuilderBlockType.NEXT_EVENT, 1, 7, 14, 5))),
        "Weather clock" to BuilderLayout(listOf(block(BuilderBlockType.TIME, 1, 2, 14, 5), block(BuilderBlockType.WEATHER, 1, 9, 14, 4), block(BuilderBlockType.BATTERY, 1, 14, 8, 2)))
    )
}
