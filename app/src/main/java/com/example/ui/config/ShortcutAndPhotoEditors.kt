package com.example.ui.config

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.theme.AccentOption
import com.example.data.contacts.ContactRepository
import com.example.data.photos.PhotoStore
import com.example.data.shortcuts.Shortcut
import com.example.data.shortcuts.ShortcutStore

/** Pick contacts, apps, a link or a number for each slot of a shortcut widget. */
@Composable
fun ShortcutEditor(widgetId: String, accent: AccentOption, onChanged: () -> Unit) {
    val context = LocalContext.current
    val config = ShortcutStore.WIDGETS[widgetId] ?: return
    var slots by remember { mutableStateOf(ShortcutStore.get(context, widgetId)) }
    var pickingSlot by remember { mutableIntStateOf(-1) }
    var showAppPicker by remember { mutableStateOf(false) }

    fun save(index: Int, shortcut: Shortcut?) {
        ShortcutStore.set(context, widgetId, index, shortcut)
        slots = ShortcutStore.get(context, widgetId)
        onChanged()
    }

    var showContactPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showContactPicker = true
        else Toast.makeText(context, "Contacts access is off — using the system picker", Toast.LENGTH_SHORT).show()
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val slot = pickingSlot
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) save(slot, Shortcut(config.kind, c.getString(0) ?: "CONTACT", c.getString(1) ?: ""))
            }
        }.onFailure { Toast.makeText(context, "Couldn't read that contact", Toast.LENGTH_SHORT).show() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEachIndexed { i, slot ->
            when (config.kind) {
                Shortcut.Kind.WEB -> {
                    var url by remember { mutableStateOf(slot?.value ?: "") }
                    var label by remember { mutableStateOf(slot?.label ?: "") }
                    val update = {
                        save(i, if (url.isBlank()) null else Shortcut(Shortcut.Kind.WEB, label.ifBlank { url.removePrefix("https://").removePrefix("www.").substringBefore('/') }, url.trim()))
                    }
                    OutlinedTextField(value = url, onValueChange = { url = it; update() }, label = { Text("Link (e.g. github.com)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = label, onValueChange = { label = it.take(20); update() }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                else -> SlotRow(
                    label = slot?.let { if (it.kind == Shortcut.Kind.APP) it.label else "${it.label} • ${it.value}" } ?: "Slot ${i + 1}: empty",
                    filled = slot != null,
                    accent = accent,
                    actionText = if (config.kind == Shortcut.Kind.APP) "PICK APP" else "PICK CONTACT",
                    onPick = {
                        pickingSlot = i
                        when {
                            config.kind == Shortcut.Kind.APP -> showAppPicker = true
                            ContactRepository.hasPermission(context) -> showContactPicker = true
                            else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    onClear = { save(i, null) }
                )
            }
        }

        if (widgetId == "shortcut.direct.dial") {
            var number by remember { mutableStateOf(slots.firstOrNull()?.value ?: "") }
            OutlinedTextField(
                value = number,
                onValueChange = {
                    number = it.filter { ch -> ch.isDigit() || ch == '+' }
                    save(0, if (number.isBlank()) null else Shortcut(Shortcut.Kind.CALL, slots.firstOrNull()?.label ?: "EMERGENCY", number))
                },
                label = { Text("Or type a number (default 112)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onPick = { label, pkg ->
                save(pickingSlot, Shortcut(Shortcut.Kind.APP, label, pkg))
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            onPick = { name, number ->
                save(pickingSlot, Shortcut(config.kind, name, number))
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false },
            onSystemPicker = {
                showContactPicker = false
                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }
        )
    }
}

/** In-app contact list (nicer than the system picker); falls back to the system picker on request. */
@Composable
private fun ContactPickerDialog(
    onPick: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val contacts = remember { ContactRepository.contacts(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        title = { Text("Pick a contact") },
        text = {
            if (contacts.isEmpty()) {
                Column {
                    Text("No contacts found.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onSystemPicker) { Text("USE SYSTEM PICKER") }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(contacts, key = { "${it.name}|${it.number}" }) { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(contact.name, contact.number) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(contact.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(contact.number, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8A8A))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SlotRow(label: String, filled: Boolean, accent: AccentOption, actionText: String, onPick: () -> Unit, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (filled) accent.color else Color(0x338A8A8A), RoundedCornerShape(8.dp))
            .padding(start = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (filled) MaterialTheme.colorScheme.onSurface else Color(0xFF8A8A8A), modifier = Modifier.weight(1f))
        TextButton(onClick = onPick) { Text(actionText, color = accent.color) }
        if (filled) {
            IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8A8A8A)) }
        }
    }
}

/** List of launchable apps on the phone. */
@Composable
private fun AppPickerDialog(onPick: (String, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { Triple(it.loadLabel(pm).toString(), it.activityInfo.packageName, it) }
            .filter { it.second != context.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        title = { Text("Pick an app") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.second }) { (label, pkg, info) ->
                    val icon = remember(pkg) { runCatching { info.loadIcon(context.packageManager).toBitmap(64, 64).asImageBitmap() }.getOrNull() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(label, pkg) }
                            .padding(vertical = 8.dp)
                    ) {
                        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    )
}

/** Pick (or remove) the photos used by a photo widget. */
@Composable
fun PhotoEditor(widgetId: String, accent: AccentOption, onChanged: () -> Unit) {
    val context = LocalContext.current
    val count = PhotoStore.WIDGETS[widgetId] ?: return
    var version by remember { mutableIntStateOf(0) }
    var pickingSlot by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if (PhotoStore.save(context, widgetId, pickingSlot, uri)) {
                version++
                onChanged()
            } else {
                Toast.makeText(context, "Couldn't open that photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val has = remember(version) { PhotoStore.has(context, widgetId, i) }
            SlotRow(
                label = if (count == 1) (if (has) "Photo selected" else "No photo yet") else "Photo ${i + 1}: ${if (has) "selected" else "empty"}",
                filled = has,
                accent = accent,
                actionText = if (has) "CHANGE" else "PICK PHOTO",
                onPick = {
                    pickingSlot = i
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = {
                    PhotoStore.remove(context, widgetId, i)
                    version++
                    onChanged()
                }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
