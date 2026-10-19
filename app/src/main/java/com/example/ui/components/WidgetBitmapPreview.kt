package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.bitmap.SizeBucket
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec

@Composable
fun WidgetBitmapPreview(
    definition: WidgetDefinition,
    spec: WidgetSpec,
    modifier: Modifier = Modifier,
    /** Bump to force a redraw after content the spec doesn't capture (notes, to-dos...) changes. */
    refreshKey: Int = 0
) {
    val context = LocalContext.current
    var bitmap by remember(definition.id, spec.sizeBucket) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(definition.id, spec, refreshKey) {
        try {
            bitmap = WidgetRenderEngine.renderBitmap(context, definition, spec, 0)
        } catch (e: Exception) {
            android.util.Log.e("WidgetBitmapPreview", "Failed to render preview for ${definition.id}", e)
        }
    }

    val aspect = when (spec.sizeBucket) {
        SizeBucket.S -> 1.0f
        SizeBucket.M -> 2.1f
        SizeBucket.L -> 1.0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(spec.cornerRadiusDp.dp))
            .background(if (spec.isDark) Color(0xFF000000) else Color(0xFFF2F2F2))
            .border(1.dp, Color(0x338A8A8A), RoundedCornerShape(spec.cornerRadiusDp.dp)),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = definition.title,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                color = Color(spec.accentColor),
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
