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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
                val wpItem = WallpaperManagerHelper.forAccent(setup.accent, setup.wallpaperIndex)
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                        text = "CURATED LOOKS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ready-made home screen ideas, picked by Dot Widgets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            items(community, key = { it.id }) { pick ->
                val bundle = remember(pick.id) {
                    SetupBundle(
                        id = pick.id,
                        title = pick.title,
                        tagline = pick.tagline,
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

    val wallpaperThumbnail = remember(setup.id) {
        WallpaperManagerHelper.renderWallpaperBitmap(WallpaperManagerHelper.forAccent(setup.accent, setup.wallpaperIndex), 360, 640)
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
                        // A hairline so the White setup's dot shows on the light theme.
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = setup.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = setup.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Preview Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The setup as it will look: its wallpaper with its widgets on top, in its accent.
            SetupPhonePreview(setup = setup, wallpaper = wallpaperThumbnail)

            // Included widgets column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(184.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "INCLUDED WIDGETS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    setup.widgetIds.forEach { widgetId ->
                        val widgetDef = WidgetRegistry.getById(widgetId)
                        val title = widgetDef?.title ?: widgetId
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = com.example.ui.theme.readableAccent(setup.accent.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tonal: the setup's accent as a soft tint with accent text, so a list of setups isn't
                // a wall of full-colour buttons. Readable for every accent on the dark card.
                val onAccent = com.example.ui.theme.readableAccent(setup.accent.color)
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
                        containerColor = onAccent.copy(alpha = 0.14f),
                        contentColor = onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("apply_setup_${setup.id}")
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(color = onAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("APPLY SETUP", style = MaterialTheme.typography.labelLarge)
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
            text = "Export your placed widgets as a code, or import a friend's.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text("EXPORT", style = MaterialTheme.typography.labelLarge)
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
                Text("IMPORT", style = MaterialTheme.typography.labelLarge)
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

/** A miniature phone: the setup's wallpaper with its widgets drawn on it in the setup's accent. */
@Composable
private fun SetupPhonePreview(setup: SetupBundle, wallpaper: android.graphics.Bitmap) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(104.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(16.dp))
            .testTag("setup_preview_${setup.id}")
    ) {
        Image(
            bitmap = wallpaper.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            setup.widgetIds.mapNotNull { WidgetRegistry.getById(it) }.take(3).forEach { def ->
                val size = def.sizes.first()
                val dims = remember(size) { com.example.core.bitmap.SizeResolver.resolveDimensions(context, size) }
                val spec = remember(def.id, setup.accent) {
                    com.example.widgets.base.WidgetSpec(
                        widthPx = dims.widthPx,
                        heightPx = dims.heightPx,
                        density = dims.density,
                        sizeBucket = size,
                        accent = setup.accent,
                        bgMode = com.example.core.theme.BackgroundMode.SOLID,
                        themeMode = com.example.core.theme.ThemeMode.FORCE_DARK,
                        isDark = true,
                        cornerRadiusDp = 20,
                        showLabel = true,
                        tapAction = def.defaultTap
                    )
                }
                val aspect = dims.widthPx.toFloat() / dims.heightPx.coerceAtLeast(1)
                com.example.ui.components.WidgetBitmapPreview(
                    definition = def,
                    spec = spec,
                    modifier = Modifier.heightIn(max = 44.dp).widthIn(max = 44.dp * aspect)
                )
            }
        }
    }
}
