package com.example.ui.config

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.config.WidgetOption
import com.example.widgets.base.WidgetDefinition
import kotlin.math.roundToInt

/**
 * The collapsible ADVANCED section of the setup screen. Renders a control for every
 * [WidgetOption] the widget declares and writes changes back into [WidgetSetupState.optionValues].
 */
@Composable
fun AdvancedOptionsSection(
    definition: WidgetDefinition,
    state: WidgetSetupState,
    accent: Color,
    update: (WidgetSetupState) -> Unit
) {
    var expanded by remember(definition.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ADVANCED OPTIONS (${definition.options.size})",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A8A8A)
        )
        Text(
            text = if (expanded) "HIDE ▲" else "SHOW ▼",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.testTag("advanced_toggle")
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("advanced_section"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            definition.options.forEach { option ->
                val value = state.optionValues[option.key]
                when (option) {
                    is WidgetOption.Toggle -> OptionToggleRow(
                        option = option,
                        checked = value as? Boolean ?: option.default,
                        accent = accent
                    ) { update(optionValue(state, option.key, it)) }

                    is WidgetOption.ColorRule -> OptionToggleRow(
                        option = option,
                        checked = value as? Boolean ?: option.default,
                        accent = accent,
                        subtitle = "Warn below ${(option.warnRatio * 100).roundToInt()}% of goal"
                    ) { update(optionValue(state, option.key, it)) }

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
                        value = value as? String ?: option.default,
                        accent = accent
                    ) { update(optionValue(state, option.key, it)) }
                }
            }
        }
    }
}

private fun optionValue(state: WidgetSetupState, key: String, value: Any): WidgetSetupState =
    state.copy(optionValues = state.optionValues + (key to value))

@Composable
private fun OptionToggleRow(
    option: WidgetOption,
    checked: Boolean,
    accent: Color,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A8A8A)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.testTag("opt_${option.key}"),
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
            )
        }
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
        Text(
            text = option.label.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            option.options.forEach { (value, label) ->
                val active = value == selected
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color.White else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accent else Color(0x1A8A8A8A))
                        .border(
                            1.dp,
                            if (active) accent else Color(0x338A8A8A),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onChange(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("opt_${option.key}_$value")
                )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option.label.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$value${if (option.unit.isNotEmpty()) " ${option.unit}" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = if (option.step <= 1) {
                    raw.roundToInt()
                } else {
                    ((raw / option.step).roundToInt() * option.step).coerceIn(option.min, option.max)
                }
                onChange(snapped)
            },
            valueRange = option.min.toFloat()..option.max.toFloat(),
            steps = if (option.step <= 1) 0 else ((option.max - option.min) / option.step - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier.testTag("opt_${option.key}")
        )
    }
}

@Composable
private fun OptionTextRow(
    option: WidgetOption.Text,
    value: String,
    accent: Color,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = option.label.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            placeholder = option.hint.ifEmpty { null }?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth().testTag("opt_${option.key}")
        )
    }
}
