package com.example.themes.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.BitmapCache
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.themes.ThemePack
import com.example.themes.ThemeRepository
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.components.tonalButtonColors
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Settings card: choose the pack the Themes widgets are drawn with, and optionally its wallpaper. */
@Composable
fun ThemeStudioCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packs = remember { ThemeRepository.packs(context) }
    var selected by remember { mutableStateOf(ThemeRepository.selected(context).id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("theme_studio_card"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("THEME STUDIO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Add widgets from the Themes filter on the Widgets tab, then choose how they look here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        packs.forEach { pack ->
            PackRow(
                pack = pack,
                isSelected = pack.id == selected,
                onUse = {
                    selected = pack.id
                    scope.launch { use(context, pack) }
                },
                onWallpaper = {
                    scope.launch {
                        val ok = setWallpaper(context, pack)
                        Toast.makeText(context, if (ok) "Wallpaper set" else "Couldn't set the wallpaper", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun PackRow(pack: ThemePack, isSelected: Boolean, onUse: () -> Unit, onWallpaper: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("theme_pack_${pack.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PackPreview(pack, "toggle.theme.quick", SizeBucket.S, Modifier.width(88.dp))
            PackPreview(pack, "prod.focus.timer.theme", SizeBucket.M, Modifier.weight(1f))
        }
        Text(pack.name.uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(
            pack.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onUse,
                enabled = !isSelected,
                colors = tonalButtonColors(accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp).testTag("theme_use_${pack.id}")
            ) { Text(if (isSelected) "IN USE" else "USE THEME", style = MaterialTheme.typography.labelLarge) }
            TextButton(onClick = onWallpaper, colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
                Text("SET WALLPAPER", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** A real widget drawn with [pack], whichever pack is currently selected. */
@Composable
private fun PackPreview(pack: ThemePack, widgetId: String, size: SizeBucket, modifier: Modifier) {
    val context = LocalContext.current
    val definition = remember(widgetId) { WidgetRegistry.getById(widgetId) } ?: return
    val spec = remember(pack.id, widgetId) {
        val dims = SizeResolver.resolveDimensions(context, size)
        WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = AccentOption.RED,
            bgMode = BackgroundMode.TRANSPARENT,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 16,
            showLabel = false,
            tapAction = definition.defaultTap,
            configJson = JSONObject().put(ThemeRepository.KEY_PACK, pack.id).toString()
        )
    }
    WidgetBitmapPreview(definition = definition, spec = spec, modifier = modifier)
}

private suspend fun use(context: Context, pack: ThemePack) = withContext(Dispatchers.IO) {
    ThemeRepository.select(context, pack)
    BitmapCache.clear()
    WidgetRenderEngine.updateAllPlacedWidgets(context.applicationContext, force = true)
}

private suspend fun setWallpaper(context: Context, pack: ThemePack): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = context.assets.open("themes/${pack.id}/wallpaper.webp").use { BitmapFactory.decodeStream(it) }
        WallpaperManager.getInstance(context).setBitmap(bitmap)
    }.isSuccess
}
