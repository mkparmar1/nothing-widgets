package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.photos.BackgroundImageStore
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.WidgetSetupState
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.WidgetSpec
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackgroundImageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `background photo survives a save and reload of the widget settings`() {
        val def = WidgetRegistry.getAll().first()
        val state = WidgetSetupState.defaults(def, PreferencesManager.getInstance(context))
            .copy(backgroundPhoto = BackgroundImageStore.Choice("abcd1234-ef56", dim = 0.6f, zoom = 2.5f, cx = 0.3f, cy = 0.7f))
        val json = state.toConfigJson(def.id)
        val back = WidgetSetupState.defaults(def, PreferencesManager.getInstance(context), importedConfigJson = json)
        val photo = back.backgroundPhoto!!
        assertEquals("abcd1234-ef56", photo.id)
        assertEquals(0.6f, photo.dim, 0.01f)
        assertEquals(2.5f, photo.zoom, 0.001f)
        assertEquals(0.3f, photo.cx, 0.001f)
        assertEquals(0.7f, photo.cy, 0.001f)

        // Removing it clears the keys, so the widget goes back to its plain background.
        val cleared = back.copy(backgroundPhoto = null).toConfigJson(def.id, existingConfigJson = json)
        assertNull(BackgroundImageStore.fromConfig(cleared))
    }

    @Test
    fun `odd ids in config are ignored`() {
        assertNull(BackgroundImageStore.fromConfig("""{"bg_image":"../../secret"}"""))
        assertNull(BackgroundImageStore.fromConfig("not json"))
    }

    /** Zooming in on one side of the photo shows that side, the same way the editor shows it. */
    @Test
    fun `widget shows the part of the photo the user placed`() {
        // Left half red, right half blue.
        val photo = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        for (x in 0 until 200) for (y in 0 until 100) photo.setPixel(x, y, if (x < 100) Color.RED else Color.BLUE)
        val id = BackgroundImageStore.save(context, photo)
        val def = WidgetRegistry.getById("clock.dotdigital.s")!!
        fun cornerColour(cx: Float): Int {
            val dims = SizeResolver.resolveDimensions(context, def.sizes.first())
            val spec = WidgetSpec(
                widthPx = dims.widthPx, heightPx = dims.heightPx, density = dims.density, sizeBucket = def.sizes.first(),
                accent = AccentOption.RED, bgMode = BackgroundMode.SOLID, themeMode = ThemeMode.FORCE_DARK,
                isDark = true, cornerRadiusDp = 0, showLabel = false, tapAction = def.defaultTap,
                configJson = """{"bg_image":"$id","bg_image_dim":0,"bg_image_zoom":2.0,"bg_image_cx":$cx,"bg_image_cy":0.5}"""
            )
            val bmp = runBlocking { WidgetRenderEngine.renderBitmap(context, def, spec, (cx * 1000).toInt()) }
            return bmp.getPixel(8, bmp.height - 8)
        }
        val left = cornerColour(0.1f)
        val right = cornerColour(0.9f)
        assertTrue("focused left but saw ${Integer.toHexString(left)}", Color.red(left) > 180 && Color.blue(left) < 90)
        assertTrue("focused right but saw ${Integer.toHexString(right)}", Color.blue(right) > 180 && Color.red(right) < 90)
    }

    @Test
    fun `framing never leaves empty space`() {
        // Focus asked for the very edge: it's pulled in so the photo still covers the frame.
        val b = com.example.core.render.PhotoFraming.bounds(1000, 500, 0f, 0f, 300f, 300f, 1f, 0f, 1f)
        assertTrue(b[0] <= 0.01f && b[1] <= 0.01f && b[2] >= 299.99f && b[3] >= 299.99f)
    }

    /** Every widget, drawn over a bright green photo: the photo should show around the content. */
    @Test
    fun `widgets show the custom photo behind their content`() {
        val photo = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(0, 255, 0)) }
        val id = BackgroundImageStore.save(context, photo)
        val outDir = File("build/widget-snapshots/bg-image").apply { deleteRecursively(); mkdirs() }
        val missing = mutableListOf<String>()
        // Theme widgets paint their own full artwork and don't offer a photo.
        val all = WidgetRegistry.getAll().filter { it.category != com.example.widgets.base.Category.THEMES }
        assertTrue(all.size > 200)
        for (def in all) {
            val size = def.sizes.first()
            val dims = SizeResolver.resolveDimensions(context, size)
            val spec = WidgetSpec(
                widthPx = dims.widthPx, heightPx = dims.heightPx, density = dims.density, sizeBucket = size,
                accent = AccentOption.RED, bgMode = BackgroundMode.SOLID, themeMode = ThemeMode.FORCE_LIGHT,
                isDark = false, cornerRadiusDp = 20, showLabel = true, tapAction = def.defaultTap,
                configJson = """{"bg_image":"$id","bg_image_dim":0}"""
            )
            val bmp = runBlocking { WidgetRenderEngine.renderBitmap(context, def, spec, 9000 + all.indexOf(def)) }
            // Count clearly green pixels on a grid; the photo shows wherever the content doesn't cover it.
            var green = 0
            var total = 0
            for (x in 0 until bmp.width step 6) for (y in 0 until bmp.height step 6) {
                val c = bmp.getPixel(x, y)
                total++
                if (Color.green(c) > 180 && Color.red(c) < 90 && Color.blue(c) < 90) green++
            }
            if (green < total / 10 || all.indexOf(def) % 40 == 0) {
                if (green < total / 10) missing += def.id
                File(outDir, "${def.id}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        println("Background photo shows on ${all.size - missing.size}/${all.size} widgets")
        if (missing.isNotEmpty()) println("Not showing it:\n" + missing.joinToString("\n"))
        assertTrue("photo missing on ${missing.size} widgets:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
