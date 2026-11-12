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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.bitmap.SizeBucket
import com.example.data.builder.BuilderAlignment
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.BuilderTextColor
import com.example.data.builder.CustomWidget
import com.example.data.builder.CustomWidgetRepository
import com.example.data.photos.BackgroundImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val selected = blocks.firstOrNull { it.id == selectedId }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (existing == null) "Build a widget" else "Edit widget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Drag blocks on the 16 × 16 snap grid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Widget name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Available sizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeBucket.entries.forEach { size ->
                    FilterChip(
                        selected = size in supportedSizes,
                        onClick = {
                            supportedSizes = if (size in supportedSizes) supportedSizes - size else supportedSizes + size
                        },
                        label = { Text(size.label) }
                    )
                }
            }

            if (existing == null && blocks.isEmpty()) {
                Text("Start with a template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BuilderTemplates.all.forEach { (title, layout) ->
                        OutlinedButton(onClick = { change(layout.blocks) }) { Text(title) }
                    }
                }
            }

            Text("Canvas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(224.dp).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                val cellW = maxWidth / 16
                val cellH = maxHeight / 16
                Canvas(Modifier.fillMaxSize()) {
                    val gridColor = Color.Gray.copy(alpha = 0.18f)
                    for (i in 0..16) {
                        drawLine(gridColor, Offset(size.width * i / 16f, 0f), Offset(size.width * i / 16f, size.height), 1f)
                        drawLine(gridColor, Offset(0f, size.height * i / 16f), Offset(size.width, size.height * i / 16f), 1f)
                    }
                }
                blocks.forEach { block ->
                    val active = block.id == selectedId
                    Box(
                        Modifier.offset(cellW * block.x, cellH * block.y)
                            .size(cellW * block.width.coerceAtMost(16 - block.x), cellH * block.height.coerceAtMost(16 - block.y))
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                            .clickable { selectedId = block.id }
                            .pointerInput(block.id, cellW, cellH) {
                                var originX = 0
                                var originY = 0
                                var movedX = 0f
                                var movedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        selectedId = block.id
                                        originX = block.x
                                        originY = block.y
                                        movedX = 0f
                                        movedY = 0f
                                        undo = (undo + listOf(blocks)).takeLast(40)
                                        redo = emptyList()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        movedX += amount.x
                                        movedY += amount.y
                                        val dx = (movedX / cellW.toPx()).roundToInt()
                                        val dy = (movedY / cellH.toPx()).roundToInt()
                                        blocks = blocks.map { item ->
                                            if (item.id == block.id) item.copy(
                                                x = (originX + dx).coerceIn(0, 15),
                                                y = (originY + dy).coerceIn(0, 15)
                                            ) else item
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(blockLabel(block), style = MaterialTheme.typography.labelSmall, maxLines = 2, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (blocks.isEmpty()) {
                    Text("Add a block below", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (undo.isNotEmpty()) {
                        redo = (redo + listOf(blocks)).takeLast(40)
                        blocks = undo.last()
                        undo = undo.dropLast(1)
                    }
                }, enabled = undo.isNotEmpty()) { Text("Undo") }
                OutlinedButton(onClick = {
                    if (redo.isNotEmpty()) {
                        undo = (undo + listOf(blocks)).takeLast(40)
                        blocks = redo.last()
                        redo = redo.dropLast(1)
                    }
                }, enabled = redo.isNotEmpty()) { Text("Redo") }
            }

            Text("Add block", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BuilderBlockType.entries.forEach { type ->
                    FilterChip(
                        selected = false,
                        onClick = {
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
                        },
                        label = { Text(type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            selected?.let { block ->
                Text("Block settings · ${block.type.name.replace('_', ' ')}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (block.type == BuilderBlockType.TEXT || block.type == BuilderBlockType.DATE || block.type == BuilderBlockType.PROGRESS) {
                    OutlinedTextField(
                        value = block.text,
                        onValueChange = { value -> change(blocks.map { if (it.id == block.id) it.copy(text = value.take(120)) else it }) },
                        label = { Text(when (block.type) {
                            BuilderBlockType.DATE -> "Date format (e.g. EEE, d MMM)"
                            BuilderBlockType.PROGRESS -> "Progress 0–100 (blank = battery)"
                            else -> "Text"
                        }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (block.type == BuilderBlockType.IMAGE) {
                    OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose image") }
                }
                Text("Position X · ${block.x}")
                Slider(value = block.x.toFloat(), onValueChange = { x -> changeFromSlider(blocks.map { if (it.id == block.id) it.copy(x = x.roundToInt()) else it }) }, onValueChangeFinished = { sliderHistorySaved = false }, valueRange = 0f..15f, steps = 14)
                Text("Position Y · ${block.y}")
                Slider(value = block.y.toFloat(), onValueChange = { y -> changeFromSlider(blocks.map { if (it.id == block.id) it.copy(y = y.roundToInt()) else it }) }, onValueChangeFinished = { sliderHistorySaved = false }, valueRange = 0f..15f, steps = 14)
                Text("Width · ${block.width}")
                Slider(value = block.width.toFloat(), onValueChange = { w -> changeFromSlider(blocks.map { if (it.id == block.id) it.copy(width = w.roundToInt()) else it }) }, onValueChangeFinished = { sliderHistorySaved = false }, valueRange = 1f..16f, steps = 14)
                Text("Height · ${block.height}")
                Slider(value = block.height.toFloat(), onValueChange = { h -> changeFromSlider(blocks.map { if (it.id == block.id) it.copy(height = h.roundToInt()) else it }) }, onValueChangeFinished = { sliderHistorySaved = false }, valueRange = 1f..16f, steps = 14)
                Text("Text size · ${"%.1f".format(block.scale)}×")
                Slider(value = block.scale, onValueChange = { scale -> changeFromSlider(blocks.map { if (it.id == block.id) it.copy(scale = scale) else it }) }, onValueChangeFinished = { sliderHistorySaved = false }, valueRange = 0.5f..2f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuilderTextColor.entries.forEach { color ->
                        FilterChip(
                            selected = color == block.color,
                            onClick = { change(blocks.map { if (it.id == block.id) it.copy(color = color) else it }) },
                            label = { Text(color.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuilderAlignment.entries.forEach { alignment ->
                        FilterChip(
                            selected = alignment == block.alignment,
                            onClick = { change(blocks.map { if (it.id == block.id) it.copy(alignment = alignment) else it }) },
                            label = { Text(alignment.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val copy = block.copy(id = java.util.UUID.randomUUID().toString(), x = (block.x + 1).coerceAtMost(15), y = (block.y + 1).coerceAtMost(15))
                        change(blocks + copy)
                        selectedId = copy.id
                    }) { Text("Duplicate") }
                    TextButton(onClick = {
                        change(blocks.filterNot { it.id == block.id })
                        selectedId = null
                    }) { Text("Delete block") }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
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
                },
                enabled = !saving && name.isNotBlank() && blocks.isNotEmpty() && supportedSizes.isNotEmpty()
            ) { Text(if (saving) "Saving…" else if (existing == null) "Save widget" else "Save changes") }
        }
    }
}

@Composable
fun CustomWidgetManagerDialog(
    widgets: List<CustomWidget>,
    onCreate: () -> Unit,
    onEdit: (CustomWidget) -> Unit,
    onDelete: (CustomWidget) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<CustomWidget?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My widgets") },
        text = {
            Column(
                Modifier.height(320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (widgets.isEmpty()) Text("Your saved designs will appear here.")
                widgets.forEach { widget ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(widget.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${BuilderLayout.parse(widget.layoutJson)?.blocks?.size ?: 0} blocks · " +
                                    widget.supportedSizes.joinToString("/") { it.label.first().toString() },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { onEdit(widget) }) { Text("Edit") }
                        TextButton(onClick = { pendingDelete = widget }) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onCreate) { Text("Create widget") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
    pendingDelete?.let { widget ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${widget.name}?") },
            text = { Text("Widgets already on your home screen will fall back to the default clock.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(widget)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
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
