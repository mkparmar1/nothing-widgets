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
        // Minimal Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search widgets & sensors...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0x338A8A8A),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_widgets_input")
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category.title.uppercase()) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = com.example.ui.theme.contentColorOn(MaterialTheme.colorScheme.primary),
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add a widget: long-press your home screen → Widgets → Dot Widgets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                showTip = false
                                prefs.setBrowseTipDismissed(true)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss tip", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                if (showExtras) {
                    item(key = "exclusive", span = fullWidth) {
                        Column {
                            Text(
                                text = "ONLY IN DOT WIDGETS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Designs no other widget app has",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
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
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
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
                                "✦ HARDWARE VITALS" to "sensor",
                                "✦ LUNAR & SUN" to "moon",
                                "✦ LIGHT STRIP" to "glyph",
                                "✦ DUAL WORLD TIME" to "world",
                                "✦ CONCENTRIC RINGS" to "ring",
                                "✦ PRODUCTIVITY" to "checklist"
                            ).forEach { (label, query) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { searchQuery = query }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "count", span = fullWidth) {
                    Text(
                        text = if (showExtras) "ALL ${filteredWidgets.size} WIDGETS" else "${filteredWidgets.size} WIDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // The whole card is the tap target; no border, the surface tone separates it from the page.
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    ) {
        WidgetBitmapPreview(
            definition = definition,
            spec = spec,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            // Title case: uppercase words are too wide for the narrow 3-up cards and break mid-word.
            text = definition.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = definition.description,
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SizeBadge(size = previewSize)
            if (exclusive) CardTag("EXCLUSIVE", MaterialTheme.colorScheme.onSurface)
            if (definition.lockSafe) {
                CardTag("LOCK SCREEN", muted, Modifier.testTag("lock_badge_${definition.id}"))
            }
            // Widgets with their own advanced options (beyond the universal text size / corner dot).
            if (definition.options.count { it.key !in UniversalOptions.KEYS } >= 2) {
                CardTag("OPTIONS", muted, Modifier.testTag("advanced_badge_${definition.id}"))
            }
        }
    }
}

/** Small neutral tag on a widget card; "exclusive" is brighter than the rest. */
@Composable
private fun CardTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
