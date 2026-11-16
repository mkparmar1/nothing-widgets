package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.data.looks.Look
import com.example.data.looks.Looks
import com.example.ui.components.DotCard
import com.example.ui.components.DotLabel
import com.example.ui.theme.DotShape
import com.example.ui.theme.DotSpacing
import com.example.ui.theme.DotTheme
import com.example.ui.theme.DotType
import kotlinx.coroutines.launch

/** Settings card: one tap restyles every placed widget with a preset Look. */
@Composable
fun LooksCard(accent: AccentOption) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val colors = DotTheme.colors

    DotCard(modifier = Modifier.fillMaxWidth().testTag("looks_card")) {
        DotLabel("Widget style")
        Text("Looks", style = DotType.CardTitle, color = colors.textPrimary)
        Text(
            "Restyle every widget on your home screen in one tap. Each keeps its content and settings.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(DotSpacing.sm))
        val rows = Looks.ALL.chunked(2)
        rows.forEachIndexed { index, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing.sm)) {
                pair.forEach { look ->
                    LookTile(
                        look = look,
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                val n = Looks.apply(context.applicationContext, look)
                                busy = false
                                Toast.makeText(
                                    context,
                                    if (n == 0) "No widgets placed yet" else "${look.name} applied to $n widgets",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            if (index < rows.lastIndex) Spacer(Modifier.height(DotSpacing.sm))
        }
    }
}

@Composable
private fun LookTile(
    look: Look,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DotTheme.colors
    Column(
        modifier = modifier
            .clip(DotShape.button)
            .background(colors.surfaceSecondary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(DotSpacing.sm)
            .testTag("look_${look.id}"),
        verticalArrangement = Arrangement.spacedBy(DotSpacing.xs)
    ) {
        LookThumbnail(look, Modifier.fillMaxWidth())
        Text(look.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(
            look.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        DotLabel("Apply", color = MaterialTheme.colorScheme.primary)
    }
}

/** A wide dot clock drawn in the Look (skin, accent, frame, dot style, background). */
@Composable
private fun LookThumbnail(look: Look, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val definition = remember { com.example.widgets.base.WidgetRegistry.getById("clock.dotdigital.m") } ?: return
    val dims = remember { com.example.core.bitmap.SizeResolver.resolveDimensions(context, com.example.core.bitmap.SizeBucket.M) }
    val spec = remember(look.id) {
        val config = org.json.JSONObject()
            .put(com.example.core.render.Skin.KEY, look.skin.name)
            .put(com.example.core.render.Frame.KEY, look.frame.name)
            .put(com.example.core.time.ClockOptions.KEY_DOT_STYLE, look.dotStyle.name)
            .put("accent", look.accent.name)
            .put("background", look.background.name)
            .toString()
        com.example.widgets.base.WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = com.example.core.bitmap.SizeBucket.M,
            accent = look.accent,
            accentColor = com.example.core.theme.WidgetAccent.resolve(context, look.accent, config),
            bgMode = look.background,
            themeMode = com.example.core.theme.ThemeMode.FORCE_DARK,
            isDark = look.skin.resolveDark(true),
            cornerRadiusDp = 16,
            showLabel = false,
            tapAction = definition.defaultTap,
            configJson = config
        )
    }
    com.example.ui.components.WidgetBitmapPreview(
        definition = definition,
        spec = spec,
        modifier = modifier.testTag("look_preview_${look.id}")
    )
}
