package com.example.ui.wallpapers

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

@Composable
fun WallpapersScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wallpapers = remember { WallpaperManagerHelper.WALLPAPERS }
    var selectedTag by remember { mutableStateOf("ALL") }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var appliedId by remember { mutableStateOf<Int?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val tags = remember(wallpapers) {
        listOf("ALL") + wallpapers.map { it.tag }.distinct()
    }
    val filtered = remember(selectedTag, wallpapers) {
        if (selectedTag == "ALL") wallpapers else wallpapers.filter { it.tag == selectedTag }
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
                    "${wallpapers.size} dot-matrix designs, palette-matched to your widgets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DotTheme.colors.textSecondary
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    title = "No wallpapers here",
                    message = "Nothing matches the ${tagLabel(selectedTag).lowercase()} palette.",
                    action = "Show all",
                    onAction = { selectedTag = "ALL" }
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
                    .then(
                        Modifier.clickable(onClick = onOpen)
                    )
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
                DotLabel("Featured")
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
                        "Palette ${tagLabel(item.tag)}",
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
