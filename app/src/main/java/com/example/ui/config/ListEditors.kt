package com.example.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.lists.ListStore

/** Add / check / remove items for the Shopping List widget. */
@Composable
fun ShoppingEditor(accent: AccentOption, onChanged: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(ListStore.shopping(context)) }
    val refresh = {
        items = ListStore.shopping(context)
        onChanged()
    }
    EditableList(
        rows = items.map { Triple(it.id, it.text, it.checked) },
        placeholder = "New item",
        accent = accent,
        onToggle = { id -> ListStore.toggleShopping(context, id); refresh() },
        onRemove = { id -> ListStore.setShopping(context, items.filterNot { it.id == id }); refresh() },
        onAdd = { text -> ListStore.addShopping(context, text); refresh() }
    )
}

/** Add / remove habits for the Habit List widget; the checkbox marks today. */
@Composable
fun HabitListEditor(accent: AccentOption, onChanged: () -> Unit) {
    val context = LocalContext.current
    var habits by remember { mutableStateOf(ListStore.habits(context)) }
    val refresh = {
        habits = ListStore.habits(context)
        onChanged()
    }
    EditableList(
        rows = habits.map { Triple(it.id, it.name, it.doneToday()) },
        placeholder = "New habit",
        accent = accent,
        onToggle = { id -> ListStore.toggleHabitToday(context, id); refresh() },
        onRemove = { id -> ListStore.setHabits(context, habits.filterNot { it.id == id }); refresh() },
        onAdd = { text -> ListStore.addHabit(context, text); refresh() }
    )
}

@Composable
private fun EditableList(
    rows: List<Triple<String, String, Boolean>>,
    placeholder: String,
    accent: AccentOption,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    var newText by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { (id, text, checked) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle(id) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it.take(40) },
                label = { Text(placeholder) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (newText.isNotBlank()) {
                    onAdd(newText)
                    newText = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
