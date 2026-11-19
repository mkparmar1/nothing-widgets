package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Brand red: the stop in the dot-matrix "D." mark, shared with the launcher icon. */
object DotBrand {
    val red = Color(0xFFE02B2B)
}

/** The 5x7 dot-matrix "D" used by the launcher icon. */
private val D_GLYPH = listOf(
    "11110",
    "10001",
    "10001",
    "10001",
    "10001",
    "10001",
    "11110"
)

private const val DOT_RADIUS = 0.4f
private const val STOP_RADIUS = 0.48f
private const val STOP_GAP = 1.32f
private const val MARK_WIDTH = 4f + STOP_GAP
private const val MARK_HEIGHT = 6f

/**
 * The app mark: a dot-matrix "D" with a red stop, fitted to the given bounds.
 * Same geometry as the launcher icon, so in-app branding matches the home screen.
 */
@Composable
fun DotMark(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    accentColor: Color = DotBrand.red,
    showOffDots: Boolean = true
) {
    Canvas(modifier) {
        drawDotMark(dotColor, accentColor, showOffDots, stopPulse = 1f)
    }
}

/** The same mark with the red stop gently breathing, for the first-run welcome. */
@Composable
fun AnimatedDotMark(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    accentColor: Color = DotBrand.red,
    showOffDots: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "dotMark")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "stopPulse"
    )
    Canvas(modifier) {
        drawDotMark(dotColor, accentColor, showOffDots, stopPulse = pulse)
    }
}

private fun DrawScope.drawDotMark(
    dotColor: Color,
    accentColor: Color,
    showOffDots: Boolean,
    stopPulse: Float
) {
    val ox = -MARK_WIDTH / 2f
    val oy = -MARK_HEIGHT / 2f

    val minX = ox - DOT_RADIUS
    val maxX = ox + 4f + STOP_GAP + STOP_RADIUS
    val minY = oy - DOT_RADIUS
    val maxY = oy + 6f + STOP_RADIUS
    val scale = minOf(size.width / (maxX - minX), size.height / (maxY - minY))

    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val markCenterX = (minX + maxX) / 2f
    val markCenterY = (minY + maxY) / 2f
    val offColor = dotColor.copy(alpha = 0.09f)

    D_GLYPH.forEachIndexed { row, line ->
        line.forEachIndexed { col, cell ->
            val lit = cell == '1'
            if (!lit && !showOffDots) return@forEachIndexed
            drawCircle(
                color = if (lit) dotColor else offColor,
                radius = DOT_RADIUS * scale,
                center = Offset(
                    centerX + (ox + col - markCenterX) * scale,
                    centerY + (oy + row - markCenterY) * scale
                )
            )
        }
    }

    drawCircle(
        color = accentColor,
        radius = STOP_RADIUS * scale * stopPulse,
        center = Offset(
            centerX + (ox + 4f + STOP_GAP - markCenterX) * scale,
            centerY + (oy + 6f - markCenterY) * scale
        )
    )
}
