package com.example.ui.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.render.WidgetShapes
import com.example.ui.theme.DotTheme
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.WidgetShape
import kotlin.math.hypot

@Composable
internal fun CustomShapeEditor(
    points: List<ShapePoint>,
    accent: Color,
    modifier: Modifier = Modifier,
    onPointsChange: (List<ShapePoint>) -> Unit
) {
    val context = LocalContext.current
    val aspect = remember {
        val dims = SizeResolver.resolveDimensions(context, SizeBucket.M)
        dims.widthPx.toFloat() / dims.heightPx.coerceAtLeast(1)
    }
    val latestPoints by rememberUpdatedState(points)
    val latestChange by rememberUpdatedState(onPointsChange)
    var dragging by remember { mutableIntStateOf(-1) }
    val ink = MaterialTheme.colorScheme.onSurface

    Column(modifier.fillMaxWidth()) {
        BoxWithAspect(aspect) { canvasModifier ->
            Box(canvasModifier) {
                Canvas(Modifier.fillMaxSize()) {
                    val step = size.width / 8f
                    for (i in 1 until 8) {
                        val x = step * i
                        drawLine(ink.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1f)
                    }
                    val rowStep = size.height / 4f
                    for (i in 1 until 4) {
                        val y = rowStep * i
                        drawLine(ink.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
                    }
                    val path = WidgetShapes.path(
                        shape = WidgetShape.CUSTOM,
                        width = size.width,
                        height = size.height,
                        cornerRadius = 0f,
                        customPoints = points
                    ).asComposePath()
                    drawPath(path, color = accent.copy(alpha = 0.22f))
                    drawPath(path, color = accent, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    points.forEach {
                        drawCircle(
                            color = accent,
                            radius = 6.dp.toPx(),
                            center = Offset(it.x * size.width, it.y * size.height)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(it.x * size.width, it.y * size.height)
                        )
                    }
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            val area = size.toSize()
                            detectTapGestures(
                                onTap = { offset ->
                                    if (nearest(offset, area, latestPoints) == null) {
                                        latestChange(
                                            latestPoints + ShapePoint(
                                                (offset.x / area.width).coerceIn(0f, 1f),
                                                (offset.y / area.height).coerceIn(0f, 1f)
                                            )
                                        )
                                    }
                                },
                                onLongPress = { offset ->
                                    val index = nearest(offset, area, latestPoints)
                                    if (index != null && latestPoints.size > 3) {
                                        latestChange(latestPoints.filterIndexed { i, _ -> i != index })
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            val area = size.toSize()
                            detectDragGestures(
                                onDragStart = { offset -> dragging = nearest(offset, area, latestPoints) ?: -1 },
                                onDrag = { change, _ ->
                                    val index = dragging
                                    if (index in latestPoints.indices) {
                                        change.consume()
                                        val moved = latestPoints.toMutableList()
                                        moved[index] = ShapePoint(
                                            (change.position.x / area.width).coerceIn(0f, 1f),
                                            (change.position.y / area.height).coerceIn(0f, 1f)
                                        )
                                        latestChange(moved)
                                    }
                                },
                                onDragEnd = { dragging = -1 },
                                onDragCancel = { dragging = -1 }
                            )
                        }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${points.size} points · tap to add, drag to move, long press to remove",
                style = MaterialTheme.typography.labelSmall,
                color = DotTheme.colors.textMuted,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { latestChange(WidgetShape.defaultCustomPoints()) },
                modifier = Modifier.padding(start = 4.dp)
            ) { Text("Reset") }
            TextButton(onClick = { latestChange(emptyList()) }) { Text("Clear") }
        }
    }
}

@Composable
private fun BoxWithAspect(
    aspect: Float,
    content: @Composable (Modifier) -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DotTheme.colors.surfaceSecondary.copy(alpha = 0.35f))
            .padding(8.dp)
    ) {
        val width = maxWidth
        val height = (width / aspect).coerceAtMost(maxHeight)
        Box(Modifier.size(width, height).clip(RoundedCornerShape(8.dp)).background(Color.Transparent)) {
            content(Modifier.fillMaxSize())
        }
    }
}

private fun nearest(offset: Offset, size: androidx.compose.ui.geometry.Size, points: List<ShapePoint>): Int? {
    var best: Int? = null
    var bestDistance = 48f
    points.forEachIndexed { index, point ->
        val distance = hypot(
            offset.x - point.x * size.width,
            offset.y - point.y * size.height
        )
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return best
}
