package com.example.ui.setups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.prefs.PreferencesManager
import com.example.data.setups.CommunityRepository
import com.example.data.setups.SetupBundle
import com.example.data.setups.SetupRepository
import com.example.data.setups.SetupShareRepository
import com.example.data.setups.SetupStyle
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotChip
import com.example.ui.components.DotLabel
import com.example.ui.components.DotMessage
import com.example.ui.components.DotSetupCard
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.components.dotFieldColors
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
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

    // null = All, so the chips read like a segmented filter row.
    var filter by remember { mutableStateOf<SetupStyle?>(null) }
    val curated = remember(filter, setups) { setups.filter { filter == null || it.style == filter } }
    val looks = remember(filter, community) { community.filter { filter == null || it.style == filter } }

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
        modifier = modifier.fillMaxSize().testTag("setups_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(DotSpacing.lg)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)) {
                DotLabel("Library")
                Text(
                    "Setups",
                    style = DotType.Hero,
                    color = DotTheme.colors.textPrimary
                )
                Text(
                    "Wallpaper, accent and widgets, matched for you in one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DotTheme.colors.textSecondary
                )
            }
        }

        item { SetupShareCard() }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)
            ) {
                DotChip(
                    label = "All",
                    selected = filter == null,
                    onClick = { filter = null },
                    modifier = Modifier.testTag("setup_filter_all")
                )
                SetupStyle.entries.forEach { style ->
                    DotChip(
                        label = style.label,
                        selected = filter == style,
                        onClick = { filter = if (filter == style) null else style },
                        modifier = Modifier.testTag("setup_filter_${style.name.lowercase()}")
                    )
                }
            }
        }

        if (curated.isEmpty() && looks.isEmpty()) {
            item {
                DotMessage(
                    icon = Icons.Default.AutoAwesome,
                    title = "No setups here",
                    message = "Nothing matches the ${filter?.label?.lowercase()} filter yet.",
                    action = "Show all",
                    onAction = { filter = null }
                )
            }
        }

        if (curated.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Curated setups",
                        style = DotType.Section,
                        color = DotTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DotLabel("${curated.size}")
                }
            }
            items(curated, key = { it.id }) { setup ->
                SetupBundleCard(
                    setup = setup,
                    onApply = { applyBundle(setup) },
                    modifier = Modifier.testTag("setup_card_${setup.id}")
                )
            }
        }

        if (looks.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Curated looks",
                        style = DotType.Section,
                        color = DotTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DotLabel("${looks.size}")
                }
            }
            items(looks, key = { it.id }) { pick ->
                val bundle = remember(pick.id) {
                    SetupBundle(
                        id = pick.id,
                        title = pick.title,
                        tagline = pick.tagline,
                        accent = pick.accent,
                        wallpaperIndex = pick.wallpaperIndex,
                        widgetIds = pick.widgetIds,
                        description = pick.tagline,
                        style = pick.style
                    )
                }
                SetupBundleCard(
                    setup = bundle,
                    onApply = { applyBundle(bundle) },
                    modifier = Modifier.testTag("community_card_${pick.id}")
                )
            }
        }
    }
}

/**
 * A setup card with a wallpaper strip and mini widget previews in the setup's accent.
 * [modifier] carries the card's identity tag; the apply button gets its own tag.
 */
@Composable
private fun SetupBundleCard(
    setup: SetupBundle,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallpaper = remember(setup.id) {
        WallpaperManagerHelper.renderWallpaperBitmap(
            WallpaperManagerHelper.forAccent(setup.accent, setup.wallpaperIndex),
            200,
            360
        )
    }
    DotSetupCard(
        title = setup.title,
        caption = setup.tagline,
        accent = setup.accent.color,
        onApply = onApply,
        modifier = modifier,
        applyModifier = Modifier.testTag("apply_setup_${setup.id}"),
        previews = {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(72.dp)
                    .clip(DotShape.small)
                    .testTag("setup_preview_${setup.id}")
            ) {
                Image(
                    bitmap = wallpaper.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            setup.widgetIds.take(2).forEach { widgetId ->
                WidgetRegistry.getById(widgetId)?.let { definition ->
                    Box(Modifier.width(68.dp)) {
                        WidgetBitmapPreview(
                            definition = definition,
                            spec = miniSpec(definition, setup.accent)
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            DotLabel("${setup.widgetIds.size} widgets")
        }
    )
}

/** Export / import the user's own home-screen setup as a short shareable code. */
@Composable
fun SetupShareCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }

    DotCard(modifier = modifier.fillMaxWidth()) {
        Text(
            "Share your setup",
            style = DotType.CardTitle,
            color = DotTheme.colors.textPrimary
        )
        Spacer(Modifier.height(DotSpacing.xs))
        Text(
            "Export your placed widgets as a code, or import a friend's.",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
            DotButton(
                text = "Export",
                icon = Icons.Default.Upload,
                style = DotButtonStyle.Secondary,
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
                modifier = Modifier.weight(1f).testTag("export_setup")
            )
            DotButton(
                text = "Import",
                icon = Icons.Default.Download,
                style = DotButtonStyle.Secondary,
                onClick = { importCode = ""; showImport = true },
                modifier = Modifier.weight(1f).testTag("import_setup")
            )
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import setup code") },
            text = {
                OutlinedTextField(
                    colors = dotFieldColors(),
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
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun miniSpec(definition: WidgetDefinition, accent: AccentOption): WidgetSpec {
    val context = LocalContext.current
    val size = definition.sizes.first()
    val dims = remember(size) { SizeResolver.resolveDimensions(context, size) }
    return remember(definition.id, size, accent) {
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = accent,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.AUTO,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )
    }
}
