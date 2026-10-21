package com.example.ui.config

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeBucket
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.theme.AccentOption
import com.example.core.theme.WidgetAccent
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import org.json.JSONObject

// ------------------------------------------------------------------ skins

/**
 * One card per skin, each showing this widget actually rendered in that skin at thumbnail size.
 * Thumbnails redraw only when something that changes their look changes (not on every opacity drag).
 */
@Composable
internal fun SkinCarousel(
    definition: WidgetDefinition,
    spec: WidgetSpec,
    selected: Skin,
    accent: Color,
    onSelect: (Skin) -> Unit
) {
    val aspect = spec.widthPx.toFloat() / spec.heightPx.coerceAtLeast(1)
    val thumbHeight = 60.dp
    val thumbWidth = (thumbHeight * aspect).coerceIn(60.dp, 128.dp)
    CarouselRow {
        Skin.entries.forEach { skin ->
            SelectCard(
                label = skin.label,
                selected = skin == selected,
                accent = accent,
                modifier = Modifier.testTag("skin_${skin.name.lowercase()}"),
                onClick = { onSelect(skin) }
            ) {
                SkinThumbnail(definition, spec, skin, Modifier.width(thumbWidth).height(thumbHeight))
            }
        }
    }
}

@Composable
private fun SkinThumbnail(definition: WidgetDefinition, spec: WidgetSpec, skin: Skin, modifier: Modifier) {
    val context = LocalContext.current
    var bitmap by remember(definition.id, skin) { mutableStateOf<Bitmap?>(null) }
    val config = remember(spec.configJson, skin) {
        runCatching { JSONObject(spec.configJson).put(Skin.KEY, skin.name).toString() }.getOrDefault(spec.configJson)
    }
    LaunchedEffect(definition.id, skin, spec.sizeBucket, spec.accentColor, spec.isDark, spec.bgMode, config) {
        val scale = 0.45f
        val thumbSpec = spec.copy(
            widthPx = (spec.widthPx * scale).toInt(),
            heightPx = (spec.heightPx * scale).toInt(),
            density = spec.density * scale,
            isDark = skin.resolveDark(spec.isDark),
            configJson = config
        )
        // A distinct instance id keeps thumbnails out of the full-size preview's cache slot.
        bitmap = runCatching { WidgetRenderEngine.renderBitmap(context, definition, thumbSpec, -1000 - skin.ordinal) }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        bitmap?.takeIf { !it.isRecycled }?.let {
            Image(it.asImageBitmap(), contentDescription = "${skin.label} skin preview", modifier = Modifier.fillMaxSize())
        } ?: Box(Modifier.fillMaxSize().background(controlFill()))
    }
}

// ------------------------------------------------------------------ frames

/** Miniature of how each frame outlines a widget. */
@Composable
internal fun FrameGlyph(frame: Frame, accent: Color, modifier: Modifier = Modifier.size(width = 26.dp, height = 18.dp)) {
    val ink = MaterialTheme.colorScheme.onSurface
    Box(modifier, contentAlignment = Alignment.Center) {
        if (frame == Frame.GLOW) {
            Canvas(Modifier.fillMaxSize().blur(3.dp)) {
                drawRoundRect(accent, cornerRadius = CornerRadius(5.dp.toPx()), style = Stroke(3.dp.toPx()))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val r = CornerRadius(5.dp.toPx())
            drawRoundRect(ink.copy(alpha = 0.12f), cornerRadius = r)
            val stroke = when (frame) {
                Frame.HAIRLINE -> Stroke(1.dp.toPx())
                Frame.DOTTED -> Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 2.5.dp.toPx())))
                Frame.GLOW -> Stroke(1.dp.toPx())
                Frame.NONE -> null
            }
            if (stroke != null) drawRoundRect(if (frame == Frame.GLOW) accent else ink.copy(alpha = 0.7f), cornerRadius = r, style = stroke)
        }
    }
}

// ------------------------------------------------------------------ sizes

/** Cell-grid miniature of a widget size (2×2, 4×2, 4×4). */
@Composable
internal fun SizeGlyph(size: SizeBucket, selected: Boolean, accent: Color) {
    val (cols, rows) = when (size) {
        SizeBucket.S -> 2 to 2
        SizeBucket.M -> 4 to 2
        SizeBucket.L -> 4 to 4
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val cell = 9.dp
    Box(Modifier.size(width = cell * 4 + 6.dp, height = cell * 4 + 6.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = cell * cols + 2.dp * (cols - 1), height = cell * rows + 2.dp * (rows - 1))) {
            val c = cell.toPx()
            val gap = 2.dp.toPx()
            for (x in 0 until cols) for (y in 0 until rows) {
                drawRoundRect(
                    color = if (selected) accent else ink.copy(alpha = 0.35f),
                    topLeft = Offset(x * (c + gap), y * (c + gap)),
                    size = Size(c, c),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
            }
        }
    }
}

// ------------------------------------------------------------------ accent

private val RainbowBrush = Brush.sweepGradient(
    listOf(Color(0xFFE02B2B), Color(0xFFF2A516), Color(0xFFB8E000), Color(0xFF00C2D1), Color(0xFF7C4DFF), Color(0xFFE02B2B))
)

/**
 * Accent swatches: the named accents, Wallpaper (Material You) and a custom colour. A chosen
 * custom colour overrides the named accent; picking a named one clears it.
 */
@Composable
internal fun AccentSwatches(
    selected: AccentOption,
    customAccent: Int?,
    onSelect: (AccentOption) -> Unit,
    onCustom: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    CarouselRow(spacing = 12.dp) {
        AccentOption.entries.forEach { option ->
            Swatch(
                fill = if (option == AccentOption.DYNAMIC) null else option.color,
                brush = if (option == AccentOption.DYNAMIC) RainbowBrush else null,
                selected = customAccent == null && option == selected,
                description = option.displayName,
                tag = "accent_option_${option.name.lowercase()}"
            ) { onSelect(option) }
        }
        val custom = customAccent?.let { Color(it) }
        Swatch(
            fill = custom,
            brush = if (custom == null) RainbowBrush else null,
            selected = custom != null,
            description = "Custom colour",
            tag = "accent_option_custom",
            icon = if (custom == null) Icons.Default.Add else Icons.Default.Edit
        ) { showPicker = true }
    }
    if (showPicker) {
        CustomColorDialog(
            initial = customAccent ?: selected.hex,
            onDismiss = { showPicker = false },
            onPick = { onCustom(it); showPicker = false }
        )
    }
}

@Composable
private fun Swatch(
    fill: Color?,
    brush: Brush?,
    selected: Boolean,
    description: String,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val ring by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, label = "ring"
    )
    val markColor = if ((fill ?: Color.Gray).luminance() > 0.6f) Color.Black else Color.White
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag)
            .border(2.dp, ring, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(fill ?: Color.Gray))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            selected -> Icon(Icons.Default.Check, contentDescription = null, tint = markColor, modifier = Modifier.size(18.dp))
            icon != null -> Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Hue / saturation / brightness picker. Brightness has a floor so the accent stays visible on the
 * black widget background.
 */
@Composable
private fun CustomColorDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceIn(MIN_BRIGHTNESS, 1f)) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    var hexText by remember(color) { mutableStateOf(WidgetAccent.toHex(color.toArgb())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom colour", fontWeight = FontWeight.Bold) },
        confirmButton = {
            TextButton(onClick = { onPick(color.toArgb()) }, modifier = Modifier.testTag("custom_color_apply")) {
                Text("APPLY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = mutedText()) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { text ->
                            hexText = text
                            WidgetAccent.parseHex(text)?.let { parsed ->
                                val out = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed, out)
                                hue = out[0]; sat = out[1]; value = out[2].coerceIn(MIN_BRIGHTNESS, 1f)
                            }
                        },
                        label = { Text("Hex") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                PickerLabel("Hue")
                SlimSlider(
                    value = hue, onValueChange = { hue = it }, accent = color, valueRange = 0f..360f,
                    trackBrush = Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) })
                )
                PickerLabel("Saturation")
                SlimSlider(
                    value = sat, onValueChange = { sat = it }, accent = color,
                    trackBrush = Brush.horizontalGradient(
                        listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))
                        )
                    )
                )
                PickerLabel("Brightness")
                SlimSlider(
                    value = value, onValueChange = { value = it }, accent = color, valueRange = MIN_BRIGHTNESS..1f,
                    trackBrush = Brush.horizontalGradient(
                        listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, MIN_BRIGHTNESS))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
                        )
                    )
                )
            }
        }
    )
}

@Composable
private fun PickerLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = mutedText(), modifier = Modifier.fillMaxWidth())
}

private const val MIN_BRIGHTNESS = 0.4f
