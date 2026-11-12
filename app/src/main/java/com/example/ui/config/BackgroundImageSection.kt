package com.example.ui.config

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.photos.BackgroundImageStore
import com.example.widgets.base.WidgetSpec
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Optional: put one of your own photos behind the widget. After picking, a placement editor opens
 * (pinch to zoom, drag to move) in the widget's exact shape. The photo is copied into app storage
 * (it never leaves the phone) and darkened by the chosen amount so the widget stays readable.
 */
@Composable
internal fun BackgroundImageSection(
    state: WidgetSetupState,
    spec: WidgetSpec,
    ui: Color,
    update: (WidgetSetupState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    /** The photo being placed in the editor, and whether it was just picked (vs. adjusting). */
    var editing by remember { mutableStateOf<Triple<Bitmap, BackgroundImageStore.Choice, Boolean>?>(null) }
    val current = state.backgroundPhoto

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                BackgroundImageStore.save(context, uri)?.let { id -> BackgroundImageStore.load(context, id)?.let { id to it } }
            }
            busy = false
            if (picked != null) {
                val (id, bitmap) = picked
                editing = Triple(bitmap, BackgroundImageStore.Choice(id, dim = current?.dim ?: BackgroundImageStore.DEFAULT_DIM), true)
            } else {
                Toast.makeText(context, "Couldn't open that photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val thumb by produceState<Bitmap?>(null, current?.id) {
        value = current?.id?.let { id -> withContext(Dispatchers.IO) { BackgroundImageStore.load(context, id) } }
    }
    val adjust = {
        val bitmap = thumb
        if (current != null && bitmap != null) editing = Triple(bitmap, current, false)
    }

    SetupSection("Background image", trailing = "Optional") {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ui.copy(alpha = 0.10f))
                        .clickable(enabled = thumb != null, onClick = adjust),
                    contentAlignment = Alignment.Center
                ) {
                    thumb?.let {
                        Image(it.asImageBitmap(), contentDescription = "Background photo", contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (current != null) "Your photo" else "None",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (busy) "Preparing photo…" else "Stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText()
                    )
                }
                OutlinedButton(
                    onClick = pick,
                    enabled = !busy,
                    modifier = Modifier.testTag("bg_image_pick")
                ) { Text(if (current != null) "Change" else "Choose") }
            }
            AnimatedVisibility(visible = current != null) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = adjust,
                            enabled = thumb != null,
                            modifier = Modifier.weight(1f).testTag("bg_image_adjust")
                        ) { Text("Adjust") }
                        TextButton(
                            onClick = { update(state.copy(backgroundPhoto = null)) },
                            modifier = Modifier.testTag("bg_image_remove")
                        ) { Text("Remove") }
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Darken", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(
                            "${((current?.dim ?: 0f) * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ui,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ui.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    SlimSlider(
                        value = current?.dim ?: BackgroundImageStore.DEFAULT_DIM,
                        onValueChange = { d -> current?.let { update(state.copy(backgroundPhoto = it.copy(dim = d))) } },
                        accent = ui,
                        valueRange = 0f..0.9f,
                        modifier = Modifier.testTag("bg_image_dim")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "Off" to com.example.data.photos.PhotoFilter.OFF,
                            "Mono" to com.example.data.photos.PhotoFilter.MONO,
                            "Accent" to com.example.data.photos.PhotoFilter.ACCENT,
                            "Colour" to com.example.data.photos.PhotoFilter.COLOUR
                        ).forEach { (label, filter) ->
                            OutlinedButton(
                                onClick = { current?.let { update(state.copy(backgroundPhoto = it.copy(filter = filter))) } },
                                modifier = Modifier.weight(1f),
                                border = if (current?.filter == filter) null else BorderStroke(1.dp, ui.copy(alpha = 0.45f)),
                                colors = if (current?.filter == filter) ButtonDefaults.buttonColors(containerColor = ui.copy(alpha = 0.12f))
                                else ButtonDefaults.outlinedButtonColors(contentColor = ui)
                            ) { Text(label) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dot size", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(
                            "${((current?.dotSize ?: BackgroundImageStore.DEFAULT_DOT_SIZE) * 10).roundToInt() / 10f}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ui,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ui.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    SlimSlider(
                        value = current?.dotSize ?: BackgroundImageStore.DEFAULT_DOT_SIZE,
                        onValueChange = { d -> current?.let { update(state.copy(backgroundPhoto = it.copy(dotSize = d))) } },
                        accent = ui,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.testTag("bg_image_dot_size")
                    )
                }
            }
        }
    }

    editing?.let { (bitmap, choice, isNew) ->
        val width = spec.widthPx.coerceAtLeast(1).toFloat()
        PhotoPlacementEditor(
            image = bitmap,
            initial = choice,
            widgetAspect = width / spec.heightPx.coerceAtLeast(1),
            // Same rounding as the widget: the corner setting in dp, at most a full pill.
            cornerFraction = (spec.cornerRadiusDp * spec.density) / width,
            accent = ui,
            onCancel = {
                // A just-picked photo that was never used is thrown away.
                if (isNew) scope.launch(Dispatchers.IO) { BackgroundImageStore.delete(context, choice.id) }
                editing = null
            },
            onDone = { placed ->
                update(state.copy(backgroundPhoto = placed))
                editing = null
            }
        )
    }
}
