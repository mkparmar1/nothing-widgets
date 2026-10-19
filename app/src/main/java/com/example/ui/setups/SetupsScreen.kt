package com.example.ui.setups

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.PreferencesManager
import com.example.data.setups.SetupBundle
import com.example.data.setups.SetupRepository
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.ui.components.TagPill
import com.example.widgets.base.WidgetRegistry
import kotlinx.coroutines.launch

@Composable
fun SetupsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager.getInstance(context) }
    val setups = remember { SetupRepository.SETUPS }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    text = "CURATED SETUPS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Matching wallpaper + widget combination + accent in one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A)
                )
            }
        }

        items(setups, key = { it.id }) { setup ->
            SetupCard(
                setup = setup,
                onApply = {
                    scope.launch {
                        // 1. Set global accent
                        prefs.setGlobalAccent(setup.accent)

                        // 2. Apply wallpaper
                        val wpItem = WallpaperManagerHelper.WALLPAPERS.getOrElse(setup.wallpaperIndex) {
                            WallpaperManagerHelper.WALLPAPERS.first()
                        }
                        WallpaperManagerHelper.applyWallpaper(context, wpItem)

                        Toast.makeText(
                            context,
                            "Applied '${setup.title}' wallpaper & ${setup.accent.displayName} accent!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.testTag("setup_card_${setup.id}")
            )
        }
    }
}

@Composable
fun SetupCard(
    setup: SetupBundle,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isApplying by remember { mutableStateOf(false) }

    val wallpaperThumbnail = remember(setup.wallpaperIndex) {
        val wpItem = WallpaperManagerHelper.WALLPAPERS.getOrElse(setup.wallpaperIndex) {
            WallpaperManagerHelper.WALLPAPERS.first()
        }
        WallpaperManagerHelper.renderWallpaperBitmap(wpItem, 360, 640)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(setup.accent.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = setup.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TagPill(
                text = setup.accent.displayName,
                accentColor = setup.accent.color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = setup.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8A8A8A)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Preview Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Wallpaper Thumbnail
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(12.dp))
            ) {
                Image(
                    bitmap = wallpaperThumbnail.asImageBitmap(),
                    contentDescription = setup.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Included widgets column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "INCLUDED WIDGETS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A8A8A)
                    )
                    setup.widgetIds.forEach { widgetId ->
                        val widgetDef = WidgetRegistry.getById(widgetId)
                        val title = widgetDef?.title ?: widgetId
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = setup.accent.color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!isApplying) {
                            isApplying = true
                            onApply()
                            isApplying = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = setup.accent.color,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("apply_setup_${setup.id}")
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("APPLY SETUP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
