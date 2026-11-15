package com.example.ui.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.builder.CustomWidget
import com.example.data.builder.CustomWidgetRepository
import com.example.data.prefs.PreferencesManager
import com.example.ui.builder.CustomWidgetBuilderScreen
import com.example.ui.builder.CustomWidgetManagerDialog
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.WidgetCard
import com.example.ui.components.dotFieldColors
import com.example.ui.config.WidgetConfigSheet
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.builder.BuilderRenderer
import kotlinx.coroutines.launch

private val RECOMMENDED = CatalogueHighlights.RECOMMENDED

private val FEATURED_EXCLUSIVE = CatalogueHighlights.EXCLUSIVE

@Composable
fun BrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()
    val customWidgets by remember(context) { CustomWidgetRepository.observe(context) }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ALL) }
    var detailsFor by remember { mutableStateOf<WidgetDefinition?>(null) }
    var activeConfigWidget by remember { mutableStateOf<WidgetDefinition?>(null) }
    var showCustomManager by remember { mutableStateOf(false) }
    var showBuilder by remember { mutableStateOf(false) }
    var editingCustom by remember { mutableStateOf<CustomWidget?>(null) }

    LaunchedEffect(customWidgets) {
        WidgetRegistry.replaceCustom(customWidgets)
    }

    val allWidgetDefinitions = remember(customWidgets) {
        WidgetRegistry.getAll().filter { it.category != Category.CUSTOM } +
            customWidgets.mapNotNull(BuilderRenderer::definition)
    }

    val filteredWidgets = remember(searchQuery, selectedCategory, allWidgetDefinitions) {
        if (searchQuery.isNotBlank()) {
            allWidgetDefinitions.filter {
                it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.description.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.category.title.contains(searchQuery.trim(), ignoreCase = true)
            }.let { list ->
                if (selectedCategory != Category.ALL) list.filter { it.category == selectedCategory } else list
            }
        } else {
            if (selectedCategory == Category.ALL) allWidgetDefinitions
            else allWidgetDefinitions.filter { it.category == selectedCategory }
        }
    }

    val currentDetails = detailsFor
    if (currentDetails != null) {
        WidgetDetailsScreen(
            definition = currentDetails,
            onBack = { detailsFor = null },
            onCustomize = { activeConfigWidget = it }
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Explore", style = DotType.Section, color = DotTheme.colors.textPrimary)
                    Text(
                        text = "${allWidgetDefinitions.size} widgets for your home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotTheme.colors.textSecondary
                    )
                }
            }

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
                shape = DotShape.pill,
                colors = dotFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.sm)
                    .testTag("search_widgets_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
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

            if (selectedCategory == Category.CUSTOM && searchQuery.isBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DotSpacing.lg, vertical = DotSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
                ) {
                    DotButton(
                        text = "Create widget",
                        onClick = {
                            editingCustom = null
                            showBuilder = true
                        },
                        style = DotButtonStyle.Secondary,
                        icon = Icons.Default.Add
                    )
                    DotButton(
                        text = "Manage",
                        onClick = { showCustomManager = true },
                        style = DotButtonStyle.Ghost
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
                val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(
                        start = DotSpacing.lg,
                        end = DotSpacing.lg,
                        top = DotSpacing.sm,
                        bottom = DotSpacing.xl
                    ),
                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(DotSpacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Everything below the pinned search + chips scrolls with the grid, so the
                    // widget list gets the whole screen once the user starts scrolling.
                    val showExtras = searchQuery.isBlank() && selectedCategory == Category.ALL
                    val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

                    if (showExtras) {
                        item(key = "exclusive", span = fullWidth) {
                            Column {
                                SectionHeader(
                                    title = "Only in Dot Widgets",
                                    subtitle = "Designs you won't find elsewhere"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.md),
                                    contentPadding = PaddingValues(top = DotSpacing.md)
                                ) {
                                    items(FEATURED_EXCLUSIVE.mapNotNull { WidgetRegistry.getById(it) }, key = { "ex_${it.id}" }) { def ->
                                        WidgetCard(
                                            definition = def,
                                            onClick = { detailsFor = def },
                                            modifier = Modifier
                                                .width(196.dp)
                                                .testTag("exclusive_${def.id}"),
                                            accent = globalAccent,
                                            exclusive = true
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "recommended", span = fullWidth) {
                            Column {
                                SectionHeader(
                                    title = "Recommended",
                                    subtitle = "Staff picks for a quick start"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.md),
                                    contentPadding = PaddingValues(top = DotSpacing.md)
                                ) {
                                    items(RECOMMENDED.mapNotNull { WidgetRegistry.getById(it) }, key = { it.id }) { def ->
                                        WidgetCard(
                                            definition = def,
                                            onClick = { detailsFor = def },
                                            modifier = Modifier
                                                .width(196.dp)
                                                .testTag("recommended_${def.id}"),
                                            accent = globalAccent
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "ideas", span = fullWidth) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
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
                        SectionHeader(
                            title = if (showExtras) "All widgets" else "Results",
                            action = { DotLabel("${filteredWidgets.size}") }
                        )
                    }

                    gridItems(filteredWidgets, key = { it.id }) { widget ->
                        WidgetCard(
                            definition = widget,
                            onClick = { detailsFor = widget },
                            accent = globalAccent,
                            exclusive = widget.id in FEATURED_EXCLUSIVE,
                            modifier = Modifier.testTag("widget_card_${widget.id}")
                        )
                    }
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

    if (showCustomManager) {
        CustomWidgetManagerDialog(
            widgets = customWidgets,
            onCreate = {
                showCustomManager = false
                editingCustom = null
                showBuilder = true
            },
            onEdit = { widget ->
                showCustomManager = false
                editingCustom = widget
                showBuilder = true
            },
            onDelete = { widget ->
                scope.launch {
                    CustomWidgetRepository.delete(context, widget)
                    WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
                }
            },
            onDismiss = { showCustomManager = false }
        )
    }

    if (showBuilder) {
        CustomWidgetBuilderScreen(
            existing = editingCustom,
            onDismiss = {
                showBuilder = false
                editingCustom = null
            },
            onSaved = {
                showBuilder = false
                editingCustom = null
                showCustomManager = true
                scope.launch {
                    WidgetRenderEngine.updateAllPlacedWidgets(context, force = true)
                }
            }
        )
    }
}
