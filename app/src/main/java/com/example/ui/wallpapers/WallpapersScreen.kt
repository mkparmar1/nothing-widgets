package com.example.ui.wallpapers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.wallpapers.WallpaperItem
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.components.DotMessage
import com.example.ui.components.WallpaperCard
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.ui.theme.contentColorOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpapersScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wallpapers = remember { WallpaperManagerHelper.WALLPAPERS }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedTag by remember { mutableStateOf("ALL") }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var appliedId by remember { mutableStateOf<Int?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val categories = remember {
        listOf("ALL", "GLYPH", "ATMOSPHERE", "CIRCUITS", "CLAY ART", "DOT MATRIX", "PALETTES")
    }

    val tags = remember(wallpapers) {
        listOf("ALL") + wallpapers.map { it.tag }.distinct()
    }

    val filtered = remember(selectedCategory, selectedTag, wallpapers) {
        wallpapers.filter { item ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "PALETTES" -> true
                else -> item.category == selectedCategory
            }
            val matchesTag = if (selectedTag == "ALL") true else item.tag == selectedTag
            matchesCategory && matchesTag
        }
    }

    val featured = filtered.firstOrNull()
    val rest = filtered.drop(1)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(DotSpacing.sm),
        modifier = modifier.fillMaxSize().testTag("wallpapers_screen")
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(bottom = DotSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                DotLabel("Appearance")
                Text("Wallpapers", style = DotType.Hero, color = DotTheme.colors.textPrimary)
                Text(
                    "${wallpapers.size} Nothing-style Glyph, Atmosphere, Circuit, Clay & Dot Matrix designs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DotTheme.colors.textSecondary
                )
            }
        }

        // Custom Wallpaper Studio Banner Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            DotCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallpaper_studio_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DotSpacing.md)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        DotLabel("Wallpaper Studio")
                        Text(
                            "Create Custom Wallpaper",
                            style = DotType.CardTitle,
                            color = DotTheme.colors.textPrimary
                        )
                        Text(
                            "Design your own with Nothing Glyphs, Atmosphere blur, colors & your photos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DotTheme.colors.textSecondary
                        )
                    }
                }
                Spacer(Modifier.height(DotSpacing.xs))
                DotButton(
                    text = "Open Wallpaper Studio",
                    icon = Icons.Default.Palette,
                    onClick = { showStudioDialog = true },
                    style = DotButtonStyle.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_wallpaper_studio")
                )
            }
        }

        // Category filter chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                categories.forEach { cat ->
                    DotChip(
                        label = catLabel(cat),
                        selected = cat == selectedCategory,
                        onClick = {
                            selectedCategory = cat
                            if (cat != "PALETTES" && cat != "ALL") {
                                selectedTag = "ALL"
                            }
                        },
                        modifier = Modifier.testTag("wallpaper_cat_$cat")
                    )
                }
            }
        }

        // Palette tags filter (always accessible, and test-supported via wallpaper_tag_MONO etc.)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                tags.forEach { tag ->
                    DotChip(
                        label = tagLabel(tag),
                        selected = tag == selectedTag,
                        onClick = { selectedTag = tag },
                        modifier = Modifier.testTag("wallpaper_tag_$tag")
                    )
                }
            }
        }

        if (featured == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DotMessage(
                    icon = Icons.Default.Wallpaper,
                    title = "No wallpapers found",
                    message = "Nothing matches the ${catLabel(selectedCategory)} filter.",
                    action = "Show all",
                    onAction = {
                        selectedCategory = "ALL"
                        selectedTag = "ALL"
                    }
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedWallpaperCard(
                    item = featured,
                    applied = appliedId == featured.id,
                    onOpen = { selectedWallpaper = featured },
                    onApply = { selectedWallpaper = featured }
                )
            }

            if (rest.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.padding(top = DotSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All wallpapers",
                            style = DotType.Section,
                            color = DotTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        DotLabel("${filtered.size}")
                    }
                }
                items(rest, key = { it.id }) { item ->
                    val thumb = remember(item.id) {
                        WallpaperManagerHelper.renderWallpaperBitmap(item, 360, 500)
                    }
                    WallpaperCard(
                        name = item.name,
                        selected = appliedId == item.id,
                        onClick = { selectedWallpaper = item },
                        modifier = Modifier.testTag("wallpaper_item_${item.id}")
                    ) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Confirmation / Full Preview Dialog
    selectedWallpaper?.let { item ->
        WallpaperConfirmDialog(
            item = item,
            applied = appliedId == item.id,
            applying = isApplying,
            onDismiss = { if (!isApplying) selectedWallpaper = null },
            onConfirm = {
                isApplying = true
                scope.launch {
                    val success = WallpaperManagerHelper.applyWallpaper(context, item)
                    isApplying = false
                    if (success) appliedId = item.id
                    selectedWallpaper = null
                    Toast.makeText(
                        context,
                        if (success) "Wallpaper applied successfully!" else "Could not set wallpaper",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Custom Wallpaper Studio Dialog
    if (showStudioDialog) {
        WallpaperStudioDialog(
            onDismiss = { showStudioDialog = false },
            onApply = { customItem ->
                scope.launch {
                    val success = WallpaperManagerHelper.applyWallpaper(context, customItem)
                    showStudioDialog = false
                    Toast.makeText(
                        context,
                        if (success) "Custom wallpaper applied!" else "Could not set wallpaper",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

/** The filtered palette's headline tile: large preview, palette tag, and one apply action. */
@Composable
private fun FeaturedWallpaperCard(
    item: WallpaperItem,
    applied: Boolean,
    onOpen: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DotTheme.colors
    val thumb = remember(item.id) {
        WallpaperManagerHelper.renderWallpaperBitmap(item, 300, 500)
    }
    DotCard(modifier = modifier.fillMaxWidth().testTag("wallpaper_featured")) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DotSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(152.dp)
                    .clip(DotShape.small)
                    .testTag("wallpaper_featured_thumb")
                    .clickable(onClick = onOpen)
            ) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (applied) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DotSpacing.xs)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Applied",
                            tint = contentColorOn(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                DotLabel(item.category)
                Text(item.name, style = DotType.CardTitle, color = colors.textPrimary)
                Text(
                    "Palette ${tagLabel(item.tag)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(DotSpacing.xs))
                DotButton(
                    text = if (applied) "Applied" else "Apply Wallpaper",
                    onClick = onApply,
                    icon = Icons.Default.Wallpaper,
                    style = DotButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth().testTag("apply_wallpaper_featured")
                )
            }
        }
    }
}

/** Interactive Wallpaper Studio: pick patterns, background shades, accent colors, or upload a photo! */
@Composable
private fun WallpaperStudioDialog(
    onDismiss: () -> Unit,
    onApply: (WallpaperItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = DotTheme.colors

    var patternType by remember { mutableIntStateOf(6) } // Default: Glyph Interface
    var bgHex by remember { mutableIntStateOf(0xFF000000.toInt()) }
    var accentHex by remember { mutableIntStateOf(0xFFE02B2B.toInt()) }
    var customTagline by remember { mutableStateOf("NOTHING // GLYPH_INTERFACE") }
    var customImage by remember { mutableStateOf<Bitmap?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeStream(stream, null, opts)
                        }
                    }.getOrNull()
                }
                customImage = bmp
            }
        }
    }

    val bgChoices = listOf(
        "OLED Black" to 0xFF000000.toInt(),
        "Dark Slate" to 0xFF0B0D10.toInt(),
        "Industrial" to 0xFF181A1D.toInt(),
        "Paper Light" to 0xFFF2F2F2.toInt(),
        "Midnight Red" to 0xFF1A0709.toInt(),
        "Deep Cyan" to 0xFF05141A.toInt()
    )

    val currentItem = remember(patternType, bgHex, accentHex, customTagline, customImage) {
        WallpaperItem(
            id = 9999,
            name = "Custom Wallpaper",
            tag = "CUSTOM",
            bgHex = bgHex,
            accentHex = accentHex,
            patternType = patternType,
            category = "CUSTOM",
            customImage = customImage,
            customTagline = customTagline.ifBlank { null }
        )
    }

    val previewBitmap = remember(currentItem) {
        WallpaperManagerHelper.renderWallpaperBitmap(currentItem, 540, 960)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(DotShape.hero)
                .background(colors.surface)
                .padding(DotSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DotLabel("Studio")
                    Text("Wallpaper Customizer", style = DotType.CardTitle, color = colors.textPrimary)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_studio")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            // Live Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 13f)
                    .clip(DotShape.card)
                    .border(1.dp, colors.surfaceSecondary, DotShape.card)
            ) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Live preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Pattern archetype selector
            DotLabel("Pattern / Style")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                WallpaperManagerHelper.PATTERN_NAMES.forEachIndexed { index, name ->
                    DotChip(
                        label = name,
                        selected = patternType == index,
                        onClick = {
                            patternType = index
                            customTagline = when (index) {
                                6 -> "NOTHING // GLYPH_INTERFACE"
                                7 -> "ATMOSPHERE // OS 3.0"
                                8 -> "SPEC // PCB_TEARDOWN"
                                9 -> "CLAY // MINIMAL_ART"
                                10 -> "AUDIO // EQUALIZER"
                                11 -> "NOTHING (R) // LAB_2400"
                                else -> "NOTHING // DOT_MATRIX"
                            }
                        }
                    )
                }
            }

            // Accent color swatches
            DotLabel("Accent Color")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                WallpaperManagerHelper.PALETTE.forEach { (name, hex) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (accentHex == hex) 3.dp else 1.dp,
                                color = if (accentHex == hex) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { accentHex = hex }
                    ) {
                        if (accentHex == hex) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = name,
                                tint = if (hex == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Background shade
            DotLabel("Background Shade")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.xs)
            ) {
                bgChoices.forEach { (label, hex) ->
                    DotChip(
                        label = label,
                        selected = bgHex == hex,
                        onClick = { bgHex = hex }
                    )
                }
            }

            // Custom photo background
            DotLabel("Custom Photo Overlay")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotButton(
                    text = if (customImage == null) "Pick Photo" else "Change Photo",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    style = DotButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                )
                if (customImage != null) {
                    DotButton(
                        text = "Remove",
                        icon = Icons.Default.Delete,
                        onClick = { customImage = null },
                        style = DotButtonStyle.Ghost,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }

            // Custom tech tagline
            DotLabel("Technical Label")
            OutlinedTextField(
                value = customTagline,
                onValueChange = { customTagline = it },
                maxLines = 1,
                placeholder = { Text("e.g. NOTHING // SPEC_01") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = colors.surfaceSecondary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = DotShape.button,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(DotSpacing.xs))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                DotButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    style = DotButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                )
                DotButton(
                    text = "Set Wallpaper",
                    icon = Icons.Default.Wallpaper,
                    loading = isApplying,
                    onClick = {
                        isApplying = true
                        onApply(currentItem)
                    },
                    style = DotButtonStyle.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_custom_wallpaper")
                )
            }
        }
    }
}

/** Confirmation sheet: nothing is applied until the person taps "Set wallpaper". */
@Composable
private fun WallpaperConfirmDialog(
    item: WallpaperItem,
    applied: Boolean,
    applying: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val colors = DotTheme.colors
        val preview = remember(item.id) {
            WallpaperManagerHelper.renderWallpaperBitmap(item, 720, 1280)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(DotShape.hero)
                .background(colors.surface)
                .padding(DotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DotSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    DotLabel("Apply wallpaper")
                    Text(item.name, style = DotType.CardTitle, color = colors.textPrimary)
                    Text(
                        "${item.category} • Palette ${tagLabel(item.tag)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !applying,
                    modifier = Modifier.testTag("close_wallpaper_preview")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(DotShape.medium)
            ) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                DotButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !applying,
                    style = DotButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                )
                DotButton(
                    text = if (applied) "Set again" else "Set wallpaper",
                    onClick = onConfirm,
                    icon = Icons.Default.Wallpaper,
                    loading = applying,
                    style = DotButtonStyle.Primary,
                    modifier = Modifier.weight(1f).testTag("apply_wallpaper_button")
                )
            }
        }
    }
}

private fun tagLabel(tag: String): String = when (tag) {
    "ALL" -> "All"
    "MONO" -> "Mono"
    else -> tag.lowercase().replaceFirstChar { it.uppercase() }
}

private fun catLabel(cat: String): String = when (cat) {
    "ALL" -> "All"
    "GLYPH" -> "Glyph"
    "ATMOSPHERE" -> "Atmosphere"
    "CIRCUITS" -> "Circuits"
    "CLAY ART" -> "Clay Art"
    "DOT MATRIX" -> "Dot Matrix"
    "PALETTES" -> "Palettes"
    else -> cat
}
