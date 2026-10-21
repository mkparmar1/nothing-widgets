package com.example.ui.config

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.WidgetOption
import com.example.widgets.base.WidgetDefinition
import kotlin.math.roundToInt

/**
 * The collapsible ADVANCED section of the setup screen. Renders a control for every
 * [WidgetOption] the widget declares and writes changes back into [WidgetSetupState.optionValues].
 * On/off options are grouped together; choices, sliders and text follow.
 */
@Composable
fun AdvancedOptionsSection(
    definition: WidgetDefinition,
    state: WidgetSetupState,
    accent: Color,
    update: (WidgetSetupState) -> Unit
) {
    var expanded by remember(definition.id) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .heightIn(min = 48.dp)
            .clickable { expanded = !expanded }
            .padding(horizontal = PagePadding)
            .testTag("advanced_toggle"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ADVANCED OPTIONS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = mutedText(),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${definition.options.size}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Icon(Icons.Default.ExpandMore, contentDescription = if (expanded) "Hide" else "Show", tint = mutedText(), modifier = Modifier.rotate(chevron))
    }

    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("advanced_section"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val switches = definition.options.filter { it is WidgetOption.Toggle || it is WidgetOption.ColorRule }
            if (switches.isNotEmpty()) {
                SettingsGroup {
                    switches.forEachIndexed { i, option ->
                        val checked = state.optionValues[option.key] as? Boolean ?: when (option) {
                            is WidgetOption.Toggle -> option.default
                            is WidgetOption.ColorRule -> option.default
                            else -> false
                        }
                        ToggleRow(
                            title = option.label,
                            subtitle = (option as? WidgetOption.ColorRule)?.let { "Warn below ${(it.warnRatio * 100).roundToInt()}% of goal" },
                            checked = checked,
                            accent = accent,
                            modifier = Modifier.testTag("opt_${option.key}")
                        ) { update(optionValue(state, option.key, it)) }
                        if (i < switches.lastIndex) GroupDivider()
                    }
                }
            }
            definition.options.forEach { option ->
                val value = state.optionValues[option.key]
                when (option) {
                    is WidgetOption.Choice -> OptionChoiceRow(
                        option = option,
                        selected = value as? String ?: option.default,
                        accent = accent
                    ) { update(optionValue(state, option.key, it)) }

                    is WidgetOption.Slider -> OptionSliderRow(
                        option = option,
                        value = (value as? Number)?.toInt() ?: option.default,
                        accent = accent
                    ) { update(optionValue(state, option.key, it)) }

                    is WidgetOption.Text -> OptionTextRow(
                        option = option,
                        value = value as? String ?: option.default
                    ) { update(optionValue(state, option.key, it)) }

                    is WidgetOption.Toggle, is WidgetOption.ColorRule -> Unit
                }
            }
        }
    }
}

private fun optionValue(state: WidgetSetupState, key: String, value: Any): WidgetSetupState =
    state.copy(optionValues = state.optionValues + (key to value))

@Composable
private fun OptionLabel(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun OptionChoiceRow(
    option: WidgetOption.Choice,
    selected: String,
    accent: Color,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OptionLabel(option.label)
        Spacer(Modifier.height(8.dp))
        CarouselRow(spacing = 8.dp) {
            option.options.forEach { (value, label) ->
                SelectChip(
                    label = label,
                    selected = value == selected,
                    accent = accent,
                    modifier = Modifier.testTag("opt_${option.key}_$value")
                ) { onChange(value) }
            }
        }
    }
}

@Composable
private fun OptionSliderRow(
    option: WidgetOption.Slider,
    value: Int,
    accent: Color,
    onChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OptionLabel(option.label) {
            Text(
                text = "$value${if (option.unit.isNotEmpty()) " ${option.unit}" else ""}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        SlimSlider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = if (option.step <= 1) {
                    raw.roundToInt()
                } else {
                    ((raw / option.step).roundToInt() * option.step).coerceIn(option.min, option.max)
                }
                onChange(snapped)
            },
            accent = accent,
            valueRange = option.min.toFloat()..option.max.toFloat(),
            steps = if (option.step <= 1) 0 else ((option.max - option.min) / option.step - 1).coerceAtLeast(0),
            modifier = Modifier.padding(horizontal = PagePadding).testTag("opt_${option.key}")
        )
    }
}

@Composable
private fun OptionTextRow(
    option: WidgetOption.Text,
    value: String,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OptionLabel(option.label)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = option.hint.ifEmpty { null }?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding).testTag("opt_${option.key}")
        )
    }
}
