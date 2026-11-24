package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeBucket
import com.example.core.config.WidgetOptions
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.StyleCatalog

/**
 * A live thumbnail per design of [definition] (Classic first), shared by the details, setup and edit
 * screens. [spec] is the preview being shown; each thumbnail is that same spec in another style.
 */
@Composable
fun StylePickerRow(
    definition: WidgetDefinition,
    spec: WidgetSpec,
    selected: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    /** Show each design in the theme it was made for (details); false keeps the preview's theme (setup). */
    ownColours: Boolean = false,
    onSelect: (String) -> Unit
) {
    val entries = remember(definition.id) {
        listOf(StyleCatalog.CLASSIC to "Classic") + StyleCatalog.stylesFor(definition.id).map { it.id to it.label }
    }
    val thumbWidth = if (spec.sizeBucket == SizeBucket.M) 132.dp else 84.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
    ) {
        entries.forEach { (id, label) ->
            val isSelected = id == selected
            val thumbSpec = remember(spec, id, ownColours) {
                val json = WidgetOptions.write(spec.configJson, mapOf(StyleCatalog.KEY to id))
                spec.copy(
                    configJson = json,
                    options = WidgetOptions.parse(json),
                    isDark = if (ownColours) !StyleCatalog.prefersLight(definition.id, id) else spec.isDark
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else DotTheme.colors.border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(role = Role.RadioButton) { onSelect(id) }
                    .semantics { this.selected = isSelected }
                    .padding(DotSpacing.sm)
                    .testTag("style_$id")
            ) {
                WidgetBitmapPreview(definition = definition, spec = thumbSpec, modifier = Modifier.width(thumbWidth))
                Spacer(Modifier.height(DotSpacing.xs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) DotTheme.colors.textPrimary else DotTheme.colors.textSecondary
                )
            }
        }
    }
}
