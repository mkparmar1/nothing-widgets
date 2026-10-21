package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.theme.AccentOption
import com.example.ui.components.headerIconTint
import com.example.data.looks.Looks
import kotlinx.coroutines.launch

/** Settings card: one tap restyles every placed widget with a preset Look. */
@Composable
fun LooksCard(accent: AccentOption) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(20.dp))
            .testTag("looks_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = headerIconTint())
                Text("LOOKS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Restyle every widget on your home screen in one tap. Each widget keeps its content and settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Looks.ALL.forEach { look ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !busy) {
                            busy = true
                            scope.launch {
                                val n = Looks.apply(context.applicationContext, look)
                                busy = false
                                Toast.makeText(context, if (n == 0) "No widgets placed yet" else "${look.name} applied to $n widgets", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("look_${look.id}")
                ) {
                    LookThumbnail(look)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(look.name.uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(look.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text("APPLY", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** A wide dot clock drawn in the Look (skin, accent, frame, dot style, background). */
@Composable
private fun LookThumbnail(look: com.example.data.looks.Look) {
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
        modifier = Modifier.width(88.dp).testTag("look_preview_${look.id}")
    )
}
