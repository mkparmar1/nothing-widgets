package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.photos.BackgroundImageStore
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** On-device check that a 4×4 widget with the dot filter is fast once the filtered bitmap is cached. */
@RunWith(AndroidJUnit4::class)
class PhotoDotFilterDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun fourByFourWidgetDrawsUnder150msAfterFirstDraw() {
        val def = WidgetRegistry.getById("battery.fuel.gauge")!!
        val size = SizeBucket.L
        val photo = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        for (x in 0 until photo.width) for (y in 0 until photo.height) {
            val v = (x * 255 / photo.width)
            photo.setPixel(x, y, Color.rgb(v, 255 - v, 128))
        }
        val id = BackgroundImageStore.save(context, photo)
        try {
            val dims = SizeResolver.resolveDimensions(context, size)
            val config = JSONObject()
                .put("bg_image", id)
                .put("bg_image_filter", "mono")
                .put("bg_image_dot", 1.0)
                .put("bg_image_dim", 45)
                .toString()
            val spec = WidgetSpec(
                widthPx = dims.widthPx,
                heightPx = dims.heightPx,
                density = dims.density,
                sizeBucket = size,
                accent = AccentOption.RED,
                bgMode = BackgroundMode.SOLID,
                themeMode = ThemeMode.FORCE_DARK,
                isDark = true,
                cornerRadiusDp = 20,
                showLabel = true,
                tapAction = def.defaultTap,
                configJson = config
            )
            // First draw builds and caches the filtered bitmap (allowed to be slower).
            assertNotNull(runBlocking { WidgetRenderEngine.renderBitmap(context, def, spec, 424242) })

            val start = SystemClock.elapsedRealtimeNanos()
            val second = runBlocking { WidgetRenderEngine.renderBitmap(context, def, spec, 424242) }
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0

            assertNotNull(second)
            assertTrue("cached 4×4 dot-filter draw took ${elapsedMs}ms", elapsedMs < 150.0)
        } finally {
            BackgroundImageStore.delete(context, id)
        }
    }
}
