package com.example.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.DotBadge
import com.example.ui.components.DotCard
import com.example.ui.components.DotCardStyle
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.components.dotFieldColors
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotTheme
import com.example.ui.components.EmptyStateView
import com.example.core.config.UniversalOptions
import com.example.ui.components.SizeBadge
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.config.WidgetConfigSheet
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec

private val RECOMMENDED = listOf(
    "clock.dotdigital.s",
    "sensor.glyph.lights",
    "weather.current.temp",
    "calendar.agenda.3",
    "media.nowplaying.compact",
    "clock.flip.style"
)

/**
 * The "only in Dot Widgets" set: designs no competitor ships. Surfaced as a dedicated row and
 * badged on their cards. Keep this list in sync with the exclusive widgets registered in
 * [WidgetRegistry].
 */
private val FEATURED_EXCLUSIVE = listOf(
    "clock.glyph.ring",
    "calendar.life.dots",
    "battery.fuel.gauge",
    "prod.week.dots",
    "prod.focus.companion",
    "fun.orbit.ambient",
    "clock.flip.style",
    "clock.binary",
    "clock.text.words",
    "sensor.ram.meter",
    "sensor.year.progress"
)

@Composable
fun BrowserScreen(
    onSelectWidget: (WidgetDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ALL) }
    var activeConfigWidget by remember { mutableStateOf<WidgetDefinition?>(null) }
    var showTip by remember { mutableStateOf(!prefs.getBrowseTipDismissed()) }

    val filteredWidgets = remember(searchQuery, selectedCategory) {
        if (searchQuery.isNotBlank()) {
            WidgetRegistry.search(searchQuery).let { list ->
                if (selectedCategory != Category.ALL) list.filter { it.category == selectedCategory } else list
            }
        } else {
            WidgetRegistry.getByCategory(selectedCategory)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search widgets") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = DotShape.medium,
            colors = dotFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_widgets_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { category ->
                DotChip(
                    label = category.title,
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                )
            }
        }

        if (filteredWidgets.isEmpty()) {
            EmptyStateView(
                title = "No Matching Widgets",
                message = "No widget matched '$searchQuery'. Try searching 'clock', 'weather', or 'battery'.",
                actionLabel = "Clear Search",
                onAction = {
                    searchQuery = ""
                    selectedCategory = Category.ALL
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Widgets Grid - Minimal Clean Cards
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Everything below the pinned search + chips scrolls with the grid, so the
                // widget list gets the whole screen once the user starts scrolling.
                val showExtras = searchQuery.isBlank() && selectedCategory == Category.ALL
                val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

                if (showExtras && showTip) {
                    item(key = "tip", span = fullWidth) {
                        DotCard(style = DotCardStyle.Information, padding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add a widget: long-press your home screen, then Widgets, then Dot Widgets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DotTheme.colors.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    showTip = false
                                    prefs.setBrowseTipDismissed(true)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss tip", tint = DotTheme.colors.textMuted)
                                }
                            }
                        }
                    }
                }

                if (showExtras) {
                    item(key = "exclusive", span = fullWidth) {
                        Column {
                            Text("Only in Dot Widgets", style = MaterialTheme.typography.headlineSmall, color = DotTheme.colors.textPrimary)
                            Text(
                                text = "Designs you won't find elsewhere",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DotTheme.colors.textSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(FEATURED_EXCLUSIVE.mapNotNull { WidgetRegistry.getById(it) }, key = { "ex_${it.id}" }) { def ->
                                    Box(modifier = Modifier.width(120.dp)) {
                                        MinimalWidgetCard(
                                            definition = def,
                                            accent = globalAccent,
                                            onClick = { activeConfigWidget = def },
                                            exclusive = true,
                                            modifier = Modifier.testTag("exclusive_${def.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "recommended", span = fullWidth) {
                        Column {
                            Text(
                                "Recommended",
                                style = MaterialTheme.typography.headlineSmall,
                                color = DotTheme.colors.textPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(RECOMMENDED.mapNotNull { WidgetRegistry.getById(it) }, key = { it.id }) { def ->
                                    Box(modifier = Modifier.width(120.dp)) {
                                        MinimalWidgetCard(
                                            definition = def,
                                            accent = globalAccent,
                                            onClick = { activeConfigWidget = def },
                                            modifier = Modifier.testTag("recommended_${def.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "ideas", span = fullWidth) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Hardware" to "sensor",
                                "Moon & sun" to "moon",
                                "Light strip" to "glyph",
                                "World time" to "world",
                                "Rings" to "ring",
                                "Productivity" to "checklist"
                            ).forEach { (label, query) ->
                                DotChip(label, selected = false, onClick = { searchQuery = query })
                            }
                        }
                    }
                }

                item(key = "count", span = fullWidth) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (showExtras) "All widgets" else "Results", style = MaterialTheme.typography.headlineSmall, color = DotTheme.colors.textPrimary)
                        DotLabel("${filteredWidgets.size}", modifier = Modifier.padding(bottom = 5.dp))
                    }
                }

                gridItems(filteredWidgets, key = { it.id }) { widget ->
                    MinimalWidgetCard(
                        definition = widget,
                        accent = globalAccent,
                        onClick = { activeConfigWidget = widget },
                        exclusive = widget.id in FEATURED_EXCLUSIVE,
                        modifier = Modifier.testTag("widget_card_${widget.id}")
                    )
                }
            }
        }
    }

    activeConfigWidget?.let { widget ->
        WidgetConfigSheet(
            definition = widget,
            onDismiss = { activeConfigWidget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinimalWidgetCard(
    definition: WidgetDefinition,
    accent: AccentOption,
    onClick: () -> Unit,
    exclusive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewSize = definition.sizes.first()
    val dims = remember(previewSize) {
        SizeResolver.resolveDimensions(context, previewSize)
    }

    val spec = WidgetSpec(
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

    // The whole card is the tap target; the surface tone separates it from the page.
    DotCard(
        modifier = modifier,
        style = DotCardStyle.Interactive,
        padding = 12.dp,
        onClick = onClick
    ) {
        WidgetBitmapPreview(
            definition = definition,
            spec = spec,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = definition.title,
            style = MaterialTheme.typography.titleSmall,
            color = DotTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = definition.description,
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SizeBadge(size = previewSize)
            if (exclusive) DotBadge("EXCLUSIVE", color = DotTheme.colors.textPrimary)
            if (definition.lockSafe) DotBadge("LOCK", modifier = Modifier.testTag("lock_badge_${definition.id}"))
            // Widgets with their own advanced options (beyond the universal text size / corner dot).
            if (definition.options.count { it.key !in UniversalOptions.KEYS } >= 2) {
                DotBadge("OPTIONS", modifier = Modifier.testTag("advanced_badge_${definition.id}"))
            }
        }
    }
}
