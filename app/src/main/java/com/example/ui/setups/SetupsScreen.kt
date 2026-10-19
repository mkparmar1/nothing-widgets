package com.example.ui.setups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.setups.CommunityRepository
import com.example.data.setups.SetupBundle
import com.example.data.setups.SetupRepository
import com.example.data.setups.SetupShareRepository
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
    val community = remember { CommunityRepository.picks(context) }

    val applyBundle: (SetupBundle) -> Unit = remember {
        { setup ->
            scope.launch {
                prefs.setGlobalAccent(setup.accent)
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
        }
    }

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

        item { SetupShareCard() }

        items(setups, key = { it.id }) { setup ->
            SetupCard(
                setup = setup,
                onApply = { applyBundle(setup) },
                modifier = Modifier.testTag("setup_card_${setup.id}")
            )
        }

        if (community.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = "COMMUNITY PICKS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Setups shared by the r/NothingTech and Nothing Playground community.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A8A8A)
                    )
                }
            }

            items(community, key = { it.id }) { pick ->
                val bundle = remember(pick.id) {
                    SetupBundle(
                        id = pick.id,
                        title = pick.title,
                        tagline = "${pick.tagline}  — ${pick.author}",
                        accent = pick.accent,
                        wallpaperIndex = pick.wallpaperIndex,
                        widgetIds = pick.widgetIds,
                        description = pick.tagline
                    )
                }
                SetupCard(
                    setup = bundle,
                    onApply = { applyBundle(bundle) },
                    modifier = Modifier.testTag("community_card_${pick.id}")
                )
            }
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

/**
 * Export / import the user's own home-screen setup as a short shareable code. Export copies a
 * compressed code of every placed widget's config (plus the accent) to the clipboard; import stores
 * those configs so the config screen pre-fills them on the next add, and applies the shared accent.
 */
@Composable
fun SetupShareCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "SHARE YOUR SETUP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Export your placed widgets as a code, or import a friend's. Great for r/NothingTech.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8A8A)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val result = SetupShareRepository.export(context)
                        if (result == null) {
                            Toast.makeText(context, "Add a widget first, then export.", Toast.LENGTH_SHORT).show()
                        } else {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Dot Widgets setup", result.code))
                            Toast.makeText(
                                context,
                                "Copied setup code for ${result.widgetCount} widget(s).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).height(42.dp).testTag("export_setup")
            ) {
                Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { importCode = ""; showImport = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).height(42.dp).testTag("import_setup")
            ) {
                Text("IMPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("IMPORT SETUP CODE") },
            text = {
                OutlinedTextField(
                    value = importCode,
                    onValueChange = { importCode = it },
                    singleLine = false,
                    maxLines = 4,
                    placeholder = { Text("Paste a DWSET1: code") },
                    modifier = Modifier.fillMaxWidth().testTag("import_code_field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = SetupShareRepository.import(context, importCode)
                                Toast.makeText(
                                    context,
                                    "Imported ${result.knownIds} widget config(s) and ${result.accent.displayName} accent. " +
                                        "Add those widgets to see them.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showImport = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid code: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = importCode.isNotBlank()
                ) { Text("IMPORT") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("CANCEL") }
            }
        )
    }
}
