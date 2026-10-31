package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DotIconSize
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.contentColorOn
import com.example.ui.theme.readableAgainst

/** Small shrink while pressed, the app's shared touch feedback. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, spring(stiffness = 900f), label = "press")
    return this.scale(scale)
}

enum class DotButtonStyle { Primary, Secondary, Ghost, Destructive }

@Composable
fun DotButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DotButtonStyle = DotButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val colors = DotTheme.colors
    val readable = readableAgainst(accent, colors.surface)
    val (fill, content) = when (style) {
        DotButtonStyle.Primary -> accent to contentColorOn(accent)
        DotButtonStyle.Secondary -> colors.surfaceSecondary to colors.textPrimary
        DotButtonStyle.Ghost -> Color.Transparent to readable
        DotButtonStyle.Destructive -> colors.danger.copy(alpha = 0.14f) to colors.danger
    }
    val source = remember { MutableInteractionSource() }
    val usable = enabled && !loading
    val animatedFill by animateColorAsState(if (usable) fill else colors.surfaceSecondary, tween(160), label = "fill")
    Row(
        modifier = modifier
            .pressScale(source)
            .heightIn(min = 48.dp)
            .clip(DotShape.medium)
            .background(animatedFill)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), enabled = usable, role = Role.Button, onClick = onClick)
            .padding(horizontal = DotSpacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (usable) content else colors.textMuted
        when {
            loading -> {
                CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(DotIconSize.small))
                Spacer(Modifier.width(DotSpacing.sm))
            }
            icon != null -> {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(DotIconSize.small))
                Spacer(Modifier.width(DotSpacing.sm))
            }
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class DotCardStyle { Standard, Elevated, Information, Interactive }

/**
 * Cards separate by surface tone, not outlines. Standard sits on the page, Elevated lifts one step,
 * Information is the quietest (notes and hints), and Interactive gets press feedback.
 */
@Composable
fun DotCard(
    modifier: Modifier = Modifier,
    style: DotCardStyle = DotCardStyle.Standard,
    shape: Shape = DotShape.large,
    onClick: (() -> Unit)? = null,
    padding: Dp = DotSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DotTheme.colors
    val fill = when (style) {
        DotCardStyle.Standard, DotCardStyle.Interactive -> colors.surface
        DotCardStyle.Elevated -> colors.surfaceElevated
        DotCardStyle.Information -> colors.surfaceSecondary
    }
    val source = remember { MutableInteractionSource() }
    val base = if (onClick != null) modifier.pressScale(source, 0.985f) else modifier
    Column(
        modifier = base
            .clip(shape)
            .background(fill)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(padding),
        content = content
    )
}

/** Small mono section label: the app's technical-label look, used sparingly. */
@Composable
fun DotLabel(text: String, modifier: Modifier = Modifier, color: Color = DotTheme.colors.textMuted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** A value with its label; the value leads, in mono. */
@Composable
fun DotStat(value: String, label: String, modifier: Modifier = Modifier, dotColor: Color? = null) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            dotColor?.let { Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(it)) }
            Text(value, style = com.example.ui.theme.DotType.NumericSmall, color = DotTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = DotTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DotBadge(text: String, modifier: Modifier = Modifier, color: Color = DotTheme.colors.textSecondary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(DotShape.small)
            .background(DotTheme.colors.surfaceSecondary)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Filter chip: a quiet pill that fills with the accent when selected. */
@Composable
fun DotChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(if (selected) accent else colors.surface, tween(160), label = "chip")
    val text by animateColorAsState(if (selected) contentColorOn(accent) else colors.textSecondary, tween(160), label = "chipText")
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pressScale(source, 0.95f)
            .heightIn(min = 40.dp)
            .clip(DotShape.medium)
            .background(fill)
            .selectable(selected = selected, interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Tab, onClick = onClick)
            .padding(horizontal = DotSpacing.lg)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1)
    }
}

/** Text fields share these colours: a filled quiet surface with the accent on focus. */
@Composable
fun dotFieldColors(): TextFieldColors {
    val c = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface,
        focusedBorderColor = accent,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = accent,
        focusedLabelColor = readableAgainst(accent, c.surface),
        unfocusedLabelColor = c.textMuted,
        focusedPlaceholderColor = c.textMuted,
        unfocusedPlaceholderColor = c.textMuted,
        focusedLeadingIconColor = c.textSecondary,
        unfocusedLeadingIconColor = c.textMuted,
        focusedTrailingIconColor = c.textSecondary,
        unfocusedTrailingIconColor = c.textMuted,
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary
    )
}

/** Placeholder block that breathes gently while content loads. */
@Composable
fun DotSkeleton(modifier: Modifier = Modifier, shape: Shape = DotShape.medium) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(modifier.alpha(pulse).clip(shape).background(DotTheme.colors.surfaceSecondary))
}

/** Very faint dot grid behind hero areas; the one decorative signature of the app. */
@Composable
fun DotMatrixBackdrop(modifier: Modifier = Modifier, pitch: Dp = 16.dp, radius: Dp = 1.dp) {
    val color = DotTheme.colors.dotPattern
    Canvas(modifier) {
        val step = pitch.toPx()
        val r = radius.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** Icon-led message for empty and error states, with an optional action. */
@Composable
fun DotMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val colors = DotTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(DotSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).clip(DotShape.large).background(colors.surfaceSecondary)
        ) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(DotIconSize.large))
        }
        Spacer(Modifier.height(DotSpacing.xs))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        if (action != null) {
            Spacer(Modifier.height(DotSpacing.sm))
            DotButton(action, onAction, style = DotButtonStyle.Secondary)
        }
    }
}

/** Thin hairline ring, kept for places where a shape must read on the same tone (e.g. previews). */
fun Modifier.dotHairline(color: Color, shape: Shape): Modifier = border(0.5.dp, color, shape)
