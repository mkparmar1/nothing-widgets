package com.example.ui.config

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.render.PhotoDotFilter
import com.example.core.render.PhotoFraming
import com.example.data.photos.BackgroundImageStore
import com.example.data.photos.PhotoFilter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen editor for placing a background photo, like setting a profile picture: a window the
 * exact shape of the widget, pinch to zoom, drag to move, double-tap to reset. The photo always
 * covers the window, and outside it is dimmed so you see exactly what the widget will show.
 */
@Composable
internal fun PhotoPlacementEditor(
    image: Bitmap,
    initial: BackgroundImageStore.Choice,
    /** The widget's shape: width / height, and corner radius as a fraction of its width. */
    widgetAspect: Float,
    cornerFraction: Float,
    accent: Color,
    onCancel: () -> Unit,
    onDone: (BackgroundImageStore.Choice) -> Unit
) {
    val imageBitmap = remember(image) { image.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(initial.zoom) }
    var cx by remember { mutableFloatStateOf(initial.cx) }
    var cy by remember { mutableFloatStateOf(initial.cy) }
    var dim by remember { mutableFloatStateOf(initial.dim) }
    var filter by remember { mutableStateOf(initial.filter) }
    var dotSize by remember { mutableFloatStateOf(initial.dotSize) }
    var touching by remember { mutableStateOf(false) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val margin = with(LocalDensity.current) { 24.dp.toPx() }

    // Live dot preview: rebuilt off the main thread when the look changes, then drawn in place.
    val filtered by produceState<Bitmap?>(null, image, filter, dotSize, accent) {
        value = if (filter == PhotoFilter.OFF) null else withContext(Dispatchers.Default) {
            PhotoDotFilter.filteredWhole(image, filter, dotSize, accent.toArgb())
        }
    }
    val filteredBitmap = remember(filtered) { filtered?.asImageBitmap() }

    // The widget-shaped window, as large as fits in the editing area.
    fun frame(): Rect {
        val availW = (area.width - margin * 2).coerceAtLeast(1f)
        val availH = (area.height - margin * 2).coerceAtLeast(1f)
        val w = minOf(availW, availH * widgetAspect)
        val h = w / widgetAspect
        val left = (area.width - w) / 2f
        val top = (area.height - h) / 2f
        return Rect(left, top, left + w, top + h)
    }

    fun clampNow() {
        val f = frame()
        val (x, y) = PhotoFraming.clampFocus(image.width, image.height, f.width, f.height, zoom, cx, cy)
        cx = x
        cy = y
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.testTag("photo_edit_cancel")) {
                    Text("Cancel", color = Color.White)
                }
                Text(
                    "Adjust photo",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Button(
                    onClick = {
                        clampNow()
                        onDone(initial.copy(zoom = zoom, cx = cx, cy = cy, dim = dim, filter = filter, dotSize = dotSize))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.testTag("photo_edit_done")
                ) { Text("Done") }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { area = it; clampNow() }
                    .pointerInput(image) {
                        detectTapGestures(onDoubleTap = { zoom = 1f; cx = 0.5f; cy = 0.5f; clampNow() })
                    }
                    .pointerInput(image) {
                        detectTransformGestures { centroid, pan, zoomChange, _ ->
                            touching = true
                            val f = frame()
                            val before = PhotoFraming.scale(image.width, image.height, f.width, f.height, zoom)
                            val newZoom = (zoom * zoomChange).coerceIn(1f, PhotoFraming.MAX_ZOOM)
                            val after = PhotoFraming.scale(image.width, image.height, f.width, f.height, newZoom)
                            // Keep the photo point under the fingers in place while zooming...
                            val dx = centroid.x - f.center.x
                            val dy = centroid.y - f.center.y
                            val u = cx + dx / (image.width * before)
                            val v = cy + dy / (image.height * before)
                            // ...then move with the drag.
                            zoom = newZoom
                            cx = u - (dx + pan.x) / (image.width * after)
                            cy = v - (dy + pan.y) / (image.height * after)
                            clampNow()
                        }
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent()
                                if (e.changes.none { it.pressed }) touching = false
                            }
                        }
                    }
                    .testTag("photo_edit_area")
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    if (area == IntSize.Zero) return@Canvas
                    val f = frame()
                    val b = PhotoFraming.bounds(image.width, image.height, f.left, f.top, f.width, f.height, zoom, cx, cy)
                    drawImage(
                        imageBitmap,
                        dstOffset = IntOffset(b[0].roundToInt(), b[1].roundToInt()),
                        dstSize = IntSize((b[2] - b[0]).roundToInt(), (b[3] - b[1]).roundToInt()),
                        filterQuality = FilterQuality.High
                    )
                    val r = (cornerFraction * f.width).coerceAtMost(minOf(f.width, f.height) / 2f)
                    val window = Path().apply { addRoundRect(RoundRect(f, CornerRadius(r, r))) }
                    // Inside: the darkening the widget will use, then the dot filter preview.
                    clipPath(window) {
                        drawRect(Color.Black.copy(alpha = dim))
                        filteredBitmap?.let {
                            drawImage(
                                it,
                                dstOffset = IntOffset(b[0].roundToInt(), b[1].roundToInt()),
                                dstSize = IntSize((b[2] - b[0]).roundToInt(), (b[3] - b[1]).roundToInt()),
                                filterQuality = FilterQuality.High
                            )
                        }
                    }
                    // Outside: dimmed, so only the widget's part stands out.
                    val outside = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(RoundRect(f, CornerRadius(r, r)))
                    }
                    drawPath(outside, Color.Black.copy(alpha = 0.7f))
                    // Thirds grid while moving, to help line things up.
                    if (touching) {
                        clipPath(window) {
                            for (i in 1..2) {
                                val x = f.left + f.width * i / 3f
                                val y = f.top + f.height * i / 3f
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(x, f.top), Offset(x, f.bottom), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(f.left, y), Offset(f.right, y), 1.dp.toPx())
                            }
                        }
                    }
                    drawPath(window, Color.White.copy(alpha = 0.9f), style = Stroke(1.5.dp.toPx()))
                }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pinch to zoom · drag to move · double-tap to reset",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Darken", color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${(dim * 100).roundToInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                SlimSlider(
                    value = dim,
                    onValueChange = { dim = it },
                    accent = accent,
                    valueRange = 0f..0.9f,
                    modifier = Modifier.testTag("photo_edit_dim")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Off" to PhotoFilter.OFF,
                        "Mono" to PhotoFilter.MONO,
                        "Accent" to PhotoFilter.ACCENT,
                        "Colour" to PhotoFilter.COLOUR
                    ).forEach { (label, mode) ->
                        FilterChip(
                            selected = filter == mode,
                            onClick = { filter = mode },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White.copy(alpha = 0.75f),
                                selectedContainerColor = accent.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                if (filter != PhotoFilter.OFF) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dot size", color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            "${(dotSize * 10).roundToInt() / 10f}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    SlimSlider(
                        value = dotSize,
                        onValueChange = { dotSize = it },
                        accent = accent,
                        valueRange = 0.5f..2f,
                        modifier = Modifier.testTag("photo_edit_dot_size")
                    )
                }
            }
        }
    }
}
