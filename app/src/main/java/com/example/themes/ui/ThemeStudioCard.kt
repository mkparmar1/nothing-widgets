package com.example.themes.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.ui.components.DotButton
import com.example.ui.components.DotButtonStyle
import com.example.ui.components.DotCard
import com.example.ui.components.DotLabel
import com.example.ui.components.WidgetBitmapPreview
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
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
    var animate by remember { mutableStateOf(ThemeRepository.animationsEnabled(context)) }

    DotCard(modifier = Modifier.fillMaxWidth().testTag("theme_studio_card")) {
        DotLabel("Themes")
        Text("Theme packs", style = DotType.CardTitle, color = DotTheme.colors.textPrimary)
        Text(
            "Add widgets from the Themes filter on the Widgets tab, then choose how they look here.",
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Animations", style = MaterialTheme.typography.titleMedium, color = DotTheme.colors.textPrimary)
                Text(
                    "Light sweeps and pulses on theme widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = DotTheme.colors.textSecondary
                )
            }
            Switch(
                checked = animate,
                onCheckedChange = {
                    animate = it
                    ThemeRepository.setAnimationsEnabled(context, it)
                    scope.launch { refresh(context) }
                },
                modifier = Modifier.testTag("theme_animations")
            )
        }
        packs.forEach { pack ->
            Spacer(Modifier.height(DotSpacing.md))
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
    Column(verticalArrangement = Arrangement.spacedBy(DotSpacing.sm), modifier = Modifier.testTag("theme_pack_${pack.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PackPreview(pack, "toggle.theme.quick", SizeBucket.S, Modifier.width(88.dp))
            PackPreview(pack, "prod.focus.timer.theme", SizeBucket.M, Modifier.weight(1f))
        }
        Text(pack.name, style = MaterialTheme.typography.titleMedium, color = DotTheme.colors.textPrimary)
        Text(
            pack.description,
            style = MaterialTheme.typography.bodySmall,
            color = DotTheme.colors.textSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            DotButton(
                text = if (isSelected) "In use" else "Use theme",
                onClick = onUse,
                enabled = !isSelected,
                style = DotButtonStyle.Secondary,
                modifier = Modifier.weight(1f).testTag("theme_use_${pack.id}")
            )
            DotButton(
                text = "Wallpaper",
                onClick = onWallpaper,
                style = DotButtonStyle.Ghost,
                modifier = Modifier.weight(1f)
            )
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
    refresh(context)
}

private suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
    BitmapCache.clear()
    WidgetRenderEngine.updateAllPlacedWidgets(context.applicationContext, force = true)
}

private suspend fun setWallpaper(context: Context, pack: ThemePack): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = context.assets.open("themes/${pack.id}/wallpaper.webp").use { BitmapFactory.decodeStream(it) }
        WallpaperManager.getInstance(context).setBitmap(bitmap)
    }.isSuccess
}
