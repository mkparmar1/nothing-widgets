package com.example.ui.config

import android.app.DatePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.BitmapCache
import com.example.core.theme.AccentOption
import com.example.data.photos.PhotoStore
import com.example.data.prefs.PreferencesManager
import com.example.data.shortcuts.ShortcutStore
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Widget ids that have editable content in the setup sheet. */
fun hasEditableContent(widgetId: String): Boolean = widgetId in setOf(
    "prod.notes.pin", "prod.todo.3", "prod.todo.5", "prod.habit.dots",
    "calendar.countdown.date", "prod.countdown.timer",
    CollectionWidgets.TODO, CollectionWidgets.SHOPPING, CollectionWidgets.HABITS
) || widgetId in ShortcutStore.WIDGETS || widgetId in PhotoStore.WIDGETS

/**
 * CONTENT section of the setup sheet: notes, to-dos, habit name, event date or timer length.
 * Values are saved immediately; [onChanged] refreshes the preview, and placed widgets are
 * redrawn when the sheet closes.
 */
@Composable
fun WidgetContentEditor(widgetId: String, accent: AccentOption, onChanged: () -> Unit) {
    if (!hasEditableContent(widgetId)) return
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    val changed = {
        BitmapCache.clear()
        onChanged()
    }
    DisposableEffect(widgetId) {
        onDispose {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { WidgetRenderEngine.updateAllPlacedWidgets(appContext) }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    SectionLabel("CONTENT")
    when (widgetId) {
        "prod.notes.pin" -> {
            var note by remember { mutableStateOf(prefs.getNoteText()) }
            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                    prefs.setNoteText(it)
                    changed()
                },
                label = { Text("Note (up to 4 lines)") },
                minLines = 3,
                maxLines = 4,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("content_note")
            )
        }

        CollectionWidgets.SHOPPING -> ShoppingEditor(accent, changed)

        CollectionWidgets.HABITS -> HabitListEditor(accent, changed)

        "prod.todo.3", "prod.todo.5", CollectionWidgets.TODO -> {
            var todos by remember { mutableStateOf(prefs.getTodos()) }
            var newTask by remember { mutableStateOf("") }
            val save = { list: List<PreferencesManager.TodoItem> ->
                todos = list
                prefs.setTodos(list)
                changed()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                todos.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { checked -> save(todos.toMutableList().also { it[i] = item.copy(done = checked) }) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { save(todos.toMutableList().also { it.removeAt(i) }) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove task", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        label = { Text("New task") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f).testTag("content_new_task")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                save(todos + PreferencesManager.TodoItem(newTask.trim(), false))
                                newTask = ""
                            }
                        },
                        modifier = Modifier.testTag("content_add_task")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add task", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        "prod.habit.dots" -> {
            var name by remember { mutableStateOf(prefs.getHabitName()) }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(24)
                    prefs.setHabitName(name.ifBlank { "DAILY HABIT" })
                    changed()
                },
                label = { Text("Habit name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Hint("Tap the widget on your home screen to mark today as done.")
        }

        "calendar.countdown.date" -> {
            var name by remember { mutableStateOf(prefs.getEventName()) }
            var date by remember { mutableStateOf(prefs.getEventDate()) }
            val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(24)
                    prefs.setEventName(name.ifBlank { "EVENT" })
                    changed()
                },
                label = { Text("Event name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            val shown = runCatching { SimpleDateFormat("dd MMM yyyy", Locale.US).format(fmt.parse(date)!!) }.getOrDefault(date)
            Text(
                text = "Date: $shown",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply { runCatching { time = fmt.parse(date)!! } }
                        DatePickerDialog(context, { _, y, m, d ->
                            date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                            prefs.setEventDate(date)
                            changed()
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    .padding(16.dp)
                    .testTag("content_event_date")
            )
        }

        in ShortcutStore.WIDGETS -> ShortcutEditor(widgetId, accent, changed)

        in PhotoStore.WIDGETS -> PhotoEditor(widgetId, accent, changed)

        "prod.countdown.timer" -> {
            var minutes by remember { mutableStateOf(prefs.getCountdownMinutes()) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30, 45, 60).forEach { m ->
                    FilterChip(
                        selected = m == minutes,
                        onClick = {
                            minutes = m
                            prefs.setCountdownMinutes(m)
                            changed()
                        },
                        label = { Text("${m}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Hint("Tap the widget to start or pause.")
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun Hint(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
}
