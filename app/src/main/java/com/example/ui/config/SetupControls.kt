package com.example.ui.config

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Building blocks for the widget customization screen. Colours come from the app theme
 * (MaterialTheme), so the screen follows light/dark mode; the selection colour is the app accent.
 */

/** Horizontal page padding used by every section. */
internal val PagePadding = 16.dp

private val ChipShape = RoundedCornerShape(12.dp)
private const val ANIM_MS = 180

@Composable
internal fun mutedText(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

/** Faint fill for unselected controls; reads on both the light and dark theme. */
@Composable
internal fun controlFill(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

@Composable
private fun selectionColors(selected: Boolean, accent: Color): Pair<Color, Color> {
    val fill by animateColorAsState(
        if (selected) accent.copy(alpha = 0.14f) else controlFill(), tween(ANIM_MS), label = "fill"
    )
    val border by animateColorAsState(
        if (selected) accent else Color.Transparent, tween(ANIM_MS), label = "border"
    )
    return fill to border
}

/** Section title with an optional value or hint on the right. */
@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = mutedText(),
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = mutedText())
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** A titled block of controls; sections are separated by space, not borders. */
@Composable
internal fun SetupSection(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        SectionHeader(title, trailing = trailing)
        content()
    }
}

/** A row that scrolls sideways edge to edge, with the first item lined up with the page padding. */
@Composable
internal fun CarouselRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PagePadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Compact selectable pill. */
@Composable
internal fun SelectChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val (fill, border) = selectionColors(selected, accent)
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(ChipShape)
            .background(fill)
            .border(1.dp, border, ChipShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leading?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Card with a visual on top and a caption, for skins and sizes. */
@Composable
internal fun SelectCard(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: () -> Unit,
    visual: @Composable () -> Unit
) {
    val (fill, border) = selectionColors(selected, accent)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        visual()
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = mutedText(), maxLines = 1)
        }
    }
}

data class SegmentOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Equal-width segmented control with a sliding selection thumb. */
@Composable
internal fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    accent: Color,
    modifier: Modifier = Modifier,
    tagPrefix: String = "segment",
    onSelect: (T) -> Unit
) {
    val index = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(controlFill())
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * index, tween(220), label = "thumb")
        Surface(
            modifier = Modifier.offset(x = offset).width(segment).fillMaxHeight(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
            shadowElevation = 1.dp
        ) {}
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, option ->
                val active = i == index
                val tint by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onSurface else mutedText(), tween(ANIM_MS), label = "tint"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .selectable(selected = active, role = Role.Tab) { onSelect(option.value) }
                        .testTag("${tagPrefix}_${option.label.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    option.icon?.let {
                        Icon(it, contentDescription = null, tint = if (active) accent else tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Slim slider: a 4dp track and a small thumb. [trackBrush] paints the whole track (e.g. a hue
 * rainbow) instead of the accent fill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackBrush: Brush? = null
) {
    val inactive = controlFill().copy(alpha = 0.16f)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier.fillMaxWidth().height(32.dp),
        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        thumb = {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(5.dp, accent, CircleShape)
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange
            val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val r = CornerRadius(size.height / 2, size.height / 2)
                if (trackBrush != null) {
                    drawRoundRect(brush = trackBrush, cornerRadius = r)
                } else {
                    drawRoundRect(color = inactive, cornerRadius = r)
                    drawRoundRect(
                        color = accent,
                        size = size.copy(width = size.width * fraction),
                        cornerRadius = r,
                        topLeft = Offset.Zero
                    )
                }
            }
        }
    )
}

/** Grouped surface for list-style rows (toggles, tap actions). */
@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .clip(RoundedCornerShape(16.dp))
            .background(controlFill())
    ) { content() }
}

@Composable
internal fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
internal fun ToggleRow(
    title: String,
    checked: Boolean,
    accent: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = mutedText())
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
        )
    }
}

@Composable
internal fun RadioRow(title: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) accent else mutedText(), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
        )
    }
}

/** Rounded surface around the live preview. */
@Composable
internal fun PreviewStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .clip(RoundedCornerShape(24.dp))
            .background(controlFill())
            .padding(vertical = 16.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Clickable modifier without the platform ripple bounds issue on custom shapes. */
internal fun Modifier.roundClickable(onClick: () -> Unit): Modifier = clip(CircleShape).clickable(onClick = onClick)
