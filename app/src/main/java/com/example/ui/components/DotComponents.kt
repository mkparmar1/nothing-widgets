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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.ui.theme.DotIconSize
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.ui.theme.contentColorOn
import com.example.ui.theme.readableAgainst
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec

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
    val base = if (onClick != null) modifier.pressScale(source, 0.98f) else modifier
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
fun DotChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
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
            .clip(DotShape.pill)
            .background(fill)
            .selectable(selected = selected, interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Tab, onClick = onClick)
            .padding(horizontal = DotSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = text, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = text, maxLines = 1)
        }
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

// ---------------------------------------------------------------------------------------------
// Redesign v2 components
// ---------------------------------------------------------------------------------------------

/**
 * The home-screen banner: title, supporting line, one call to action, and a real rendered widget
 * on the right over a soft accent glow. [preview] receives the box so it can size itself.
 */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    ctaLabel: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    preview: (@Composable BoxScope.() -> Unit)? = null
) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DotShape.hero)
            .background(colors.surface)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
            .heightIn(min = 196.dp)
    ) {
        preview?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = DotSpacing.lg),
                content = it
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.64f)
                .padding(DotSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.sm)
        ) {
            DotLabel("Dot Widgets")
            Text(
                text = title,
                style = DotType.Hero,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .clip(DotShape.pill)
                    .clickable(role = Role.Button, onClick = onCta)
                    .padding(vertical = DotSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ctaLabel, style = MaterialTheme.typography.labelLarge, color = accent)
                Spacer(Modifier.width(DotSpacing.xs))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(DotIconSize.small))
            }
        }
    }
}

/** 4-up square shortcut on Home. Selected tiles tint with the accent. */
@Composable
fun QuickAccessTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(source, 0.96f)
            .clip(DotShape.card)
            .background(if (selected) accent.copy(alpha = 0.14f) else colors.surface)
            .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else colors.border, DotShape.card)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(DotSpacing.md),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(DotShape.button)
                .background(if (selected) accent else colors.surfaceSecondary)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) contentColorOn(accent) else colors.textPrimary,
                modifier = Modifier.size(DotIconSize.medium)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The v2 widget tile: rendered preview, name, one-line description and its badges. Used by Explore
 * and any "Featured widgets" row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetCard(
    definition: WidgetDefinition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: AccentOption = AccentOption.RED,
    exclusive: Boolean = false,
    badges: List<String> = emptyList()
) {
    val context = LocalContext.current
    val colors = DotTheme.colors
    val previewSize = definition.sizes.first()
    val dims = remember(previewSize) { SizeResolver.resolveDimensions(context, previewSize) }
    val spec = remember(definition.id, previewSize, accent) {
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = previewSize,
            accent = accent,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.AUTO,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )
    }
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(source, 0.98f)
            .clip(DotShape.card)
            .background(colors.surface)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(DotSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DotShape.button)
                .background(colors.surfaceSecondary)
                .padding(DotSpacing.sm)
        ) {
            WidgetBitmapPreview(definition = definition, spec = spec, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(DotSpacing.md))
        Text(
            text = definition.title,
            style = DotType.CardTitle,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(DotSpacing.xs))
        Text(
            text = definition.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(DotSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
        ) {
            SizeBadge(size = previewSize)
            if (exclusive) DotBadge("EXCLUSIVE", color = colors.textPrimary)
            badges.forEach { DotBadge(it) }
        }
    }
}

/**
 * A setup with a strip of mini widget previews and one apply action. [previews] is a slot so the
 * component stays free of data-layer types; pass small [WidgetBitmapPreview]s or thumbnails.
 */
@Composable
fun DotSetupCard(
    title: String,
    caption: String,
    accent: Color,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    applyLabel: String = "Apply Setup",
    previews: @Composable RowScope.() -> Unit = {}
) {
    val colors = DotTheme.colors
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(DotShape.card)
            .background(colors.surface)
            .then(
                if (onOpen != null) Modifier
                    .pressScale(source, 0.98f)
                    .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onOpen)
                else Modifier
            )
            .padding(DotSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(DotSpacing.sm))
            Text(title, style = DotType.CardTitle, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(DotSpacing.xs))
        Text(caption, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(DotSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = previews
        )
        Spacer(Modifier.height(DotSpacing.md))
        DotButton(
            text = applyLabel,
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            style = DotButtonStyle.Secondary,
            icon = Icons.Default.Check
        )
    }
}

/** Full-bleed wallpaper tile with an optional applied check mark. [image] draws the wallpaper. */
@Composable
fun WallpaperCard(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable BoxScope.() -> Unit
) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .pressScale(source, 0.98f)
            .clip(DotShape.card)
            .background(colors.surfaceSecondary)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.matchParentSize(), content = image)
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(DotSpacing.sm)
        )
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DotSpacing.sm)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accent)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Applied", tint = contentColorOn(accent), modifier = Modifier.size(DotIconSize.small))
            }
        }
    }
}

/** Menu row: round icon tile, title, optional caption, chevron or custom trailing content. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = DotTheme.colors
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source, 0.99f)
            .clip(DotShape.button)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(vertical = DotSpacing.md, horizontal = DotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(DotShape.button)
                .background(colors.surfaceSecondary)
        ) {
            Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(DotIconSize.medium))
        }
        Spacer(Modifier.width(DotSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(DotIconSize.medium))
        }
    }
}

/** Pill segmented control: used for Skin/Shape/Accent/Size and Dark/Light/Auto. */
@Composable
fun SegmentTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** When set, each tab gets the tag `"${tagPrefix}_$index"` for UI tests. */
    tagPrefix: String? = null
) {
    val colors = DotTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DotShape.pill)
            .background(colors.surfaceSecondary)
            .padding(DotSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(DotShape.pill)
                    .background(if (selected) colors.surface else Color.Transparent)
                    .then(if (tagPrefix != null) Modifier.testTag("${tagPrefix}_$index") else Modifier)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = DotSpacing.xs)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Accent swatch row with an optional trailing "+" for a custom colour. */
@Composable
fun ColorDots(
    colors: List<Color>,
    selected: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null
) {
    val tokens = DotTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DotSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            val isSelected = selected == color
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) tokens.textPrimary else tokens.border, CircleShape)
                    .clickable(role = Role.RadioButton, onClick = { onSelect(color) })
            ) {
                if (isSelected) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(contentColorOn(color)))
                }
            }
        }
        if (onAdd != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, tokens.border, CircleShape)
                    .clickable(role = Role.Button, onClick = onAdd)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add colour", tint = tokens.textSecondary, modifier = Modifier.size(DotIconSize.small))
            }
        }
    }
}

data class DotNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * Floating bottom bar with a raised centre "+". [items] is the four non-create destinations; the
 * create action sits between the second and third.
 */
@Composable
fun BottomNav(
    items: List<DotNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    val left = items.take(2)
    val right = items.drop(2)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.sm)
            .shadow(16.dp, DotShape.pill)
            .clip(DotShape.pill)
            .background(colors.surface)
            .border(1.dp, colors.border, DotShape.pill)
            .padding(horizontal = DotSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        left.forEachIndexed { index, item ->
            BottomNavItem(item, selected = selectedIndex == index, onClick = { onSelect(index) }, modifier = Modifier.weight(1f))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = DotSpacing.xs)
                .size(52.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(accent)
                .clickable(role = Role.Button, onClick = onPlus)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create widget", tint = contentColorOn(accent), modifier = Modifier.size(DotIconSize.large))
        }
        right.forEachIndexed { index, item ->
            val itemIndex = index + 2
            BottomNavItem(item, selected = selectedIndex == itemIndex, onClick = { onSelect(itemIndex) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomNavItem(item: DotNavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DotTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(DotShape.pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = DotSpacing.sm)
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = null,
            tint = if (selected) accent else colors.textMuted,
            modifier = Modifier.size(DotIconSize.medium)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.textPrimary else colors.textMuted,
            maxLines = 1
        )
    }
}

/** Big centred state icon for Widget Status: green check or amber warning. */
@Composable
fun StatusHero(
    icon: ImageVector,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    tint: Color = DotTheme.colors.success,
    action: (@Composable () -> Unit)? = null
) {
    val colors = DotTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DotSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DotSpacing.md)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.40f), CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(44.dp))
        }
        Text(title, style = DotType.Section, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null) {
            action()
        }
    }
}
