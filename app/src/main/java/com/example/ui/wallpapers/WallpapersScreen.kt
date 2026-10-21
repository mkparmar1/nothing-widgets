package com.example.ui.wallpapers

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.prefs.PreferencesManager
import com.example.data.wallpapers.WallpaperItem
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.launch

@Composable
fun WallpapersScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val globalAccent by prefs.accent.collectAsState()

    val wallpapers = remember { WallpaperManagerHelper.WALLPAPERS }
    var selectedTag by remember { mutableStateOf("ALL") }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val tags = remember(wallpapers) {
        listOf("ALL") + wallpapers.map { it.tag }.distinct()
    }

    val filteredWallpapers = remember(selectedTag, wallpapers) {
        if (selectedTag == "ALL") wallpapers else wallpapers.filter { it.tag == selectedTag }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "WALLPAPERS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${wallpapers.size} dot-matrix designs, palette-matched to your widgets.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A8A8A)
            )
        }

        // Tag Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSelected = tag == selectedTag
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTag = tag },
                    label = { Text(tag) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = globalAccent.color,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("wallpaper_tag_$tag")
                )
            }
        }

        if (filteredWallpapers.isEmpty()) {
            EmptyStateView(
                title = "No Wallpapers Found",
                message = "No wallpapers found for tag '$selectedTag'.",
                actionLabel = "Show All",
                onAction = { selectedTag = "ALL" },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredWallpapers, key = { it.id }) { item ->
                    WallpaperThumb(
                        item = item,
                        onClick = { selectedWallpaper = item },
                        modifier = Modifier.testTag("wallpaper_item_${item.id}")
                    )
                }
            }
        }
    }

    selectedWallpaper?.let { item ->
        Dialog(onDismissRequest = { selectedWallpaper = null }) {
            val previewBitmap = remember(item.id) {
                WallpaperManagerHelper.renderWallpaperBitmap(item, 720, 1440)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.name.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "TAG: ${item.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A8A8A)
                            )
                        }
                        IconButton(onClick = { selectedWallpaper = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isApplying = true
                            scope.launch {
                                val success = WallpaperManagerHelper.applyWallpaper(context, item)
                                isApplying = false
                                selectedWallpaper = null
                                Toast.makeText(
                                    context,
                                    if (success) "Wallpaper applied successfully!" else "Could not set wallpaper",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isApplying,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = globalAccent.color,
                            contentColor = com.example.ui.theme.contentColorOn(globalAccent.color)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_wallpaper_button")
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(color = com.example.ui.theme.contentColorOn(globalAccent.color), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SET AS WALLPAPER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperThumb(
    item: WallpaperItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumb = remember(item.id) {
        WallpaperManagerHelper.renderWallpaperBitmap(item, 240, 480)
    }

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(item.bgHex))
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Image(
            bitmap = thumb.asImageBitmap(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize()
        )

        // Tag badge bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.tag,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
