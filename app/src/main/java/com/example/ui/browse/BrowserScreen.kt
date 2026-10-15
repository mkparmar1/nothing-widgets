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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.ui.components.SizeBadge
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.config.WidgetConfigSheet
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec

@Composable
fun BrowserScreen(
    onSelectWidget: (WidgetDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()
    val isUnlocked by prefs.isUnlocked.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.ALL) }
    var activeConfigWidget by remember { mutableStateOf<WidgetDefinition?>(null) }

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
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 90+ widgets & sensors...", color = Color(0xFF8A8A8A)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8A8A8A)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF8A8A8A))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
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

        // Widget Design Ideas Strip
        if (searchQuery.isBlank() && selectedCategory == Category.ALL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

        // Subheader count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredWidgets.size} WIDGETS AVAILABLE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(globalAccent.color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "0% IDLE DRAIN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A8A8A)
                )
            }
        }

        // Widgets Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredWidgets, key = { it.id }) { widget ->
                WidgetCard(
                    definition = widget,
                    accent = globalAccent,
                    isUnlocked = isUnlocked || widget.isFree,
                    onConfigure = { activeConfigWidget = widget },
                    modifier = Modifier.testTag("widget_card_${widget.id}")
                )
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
fun WidgetCard(
    definition: WidgetDefinition,
    accent: AccentOption,
    isUnlocked: Boolean,
    onConfigure: () -> Unit,
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
            .clickable { onConfigure() }
            .padding(14.dp)
    ) {
        // Live Bitmap Preview
        WidgetBitmapPreview(
            definition = definition,
            spec = spec,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Info and Actions
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
                        color = MaterialTheme.colorScheme.onSurface
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

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConfigure,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUnlocked) accent.color else Color(0x338A8A8A),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                if (!isUnlocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Pro",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFD4D4D4)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
