package com.example.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.EmptyStateView
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
            placeholder = { Text("Search widgets & sensors...", color = Color(0xFF8A8A8A)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8A8A8A)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF8A8A8A))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = globalAccent.color,
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
                        selectedContainerColor = globalAccent.color,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
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
                                Icon(Icons.Default.Close, contentDescription = "Dismiss tip", tint = Color(0xFF8A8A8A))
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
                                color = globalAccent.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Designs no other widget app has",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A8A8A),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                color = Color(0xFF8A8A8A),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                "✦ NOTHING GLYPH" to "glyph",
                                "✦ DUAL WORLD TIME" to "world",
                                "✦ CONCENTRIC RINGS" to "ring",
                                "✦ PRODUCTIVITY" to "checklist"
                            ).forEach { (label, query) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1A8A8A8A))
                                        .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(8.dp))
                                        .clickable { searchQuery = query }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = globalAccent.color,
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
                        color = Color(0xFF8A8A8A)
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

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color(0x228A8A8A), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        // Live Bitmap Preview
        WidgetBitmapPreview(
            definition = definition,
            spec = spec,
            modifier = Modifier.fillMaxWidth()
        )

        if (exclusive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "★ EXCLUSIVE",
                style = MaterialTheme.typography.labelSmall,
                color = accent.color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info Row - Sleek & Minimal without extra buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = definition.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    SizeBadge(size = previewSize)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = definition.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A),
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Configure",
                tint = accent.color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
