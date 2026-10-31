package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.themes.ThemeRepository
import com.example.themes.ThemeWidgets
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemePackTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val requiredImages = listOf(
        "tile_square", "tile_quad", "tile_wide", "tile_pill", "tile_tall", "tile_tall_fill",
        "button_on", "button_off", "wallpaper"
    )

    @Test
    fun `every pack has all its images`() {
        val packs = ThemeRepository.packs(context)
        assertTrue("no packs found", packs.isNotEmpty())
        packs.forEach { pack ->
            requiredImages.forEach { assertNotNull("${pack.id}/$it", ThemeRepository.image(context, pack, it)) }
        }
    }

    @Test
    fun `selecting a pack sticks and a pinned pack wins`() {
        val (first, second) = ThemeRepository.packs(context)
        ThemeRepository.select(context, second)
        assertEquals(second.id, ThemeRepository.selected(context).id)
        val pinned = JSONObject().put(ThemeRepository.KEY_PACK, first.id).toString()
        assertEquals(first.id, ThemeRepository.forConfig(context, pinned).id)
    }

    @Test
    fun `theme widgets are registered and draw in every pack and size`() {
        val out = File("build/theme-snapshots").apply { mkdirs() }
        ThemeWidgets.ALL.forEach { widget ->
            assertNotNull(widget.id, WidgetRegistry.getById(widget.id))
            ThemeRepository.packs(context).forEach { pack ->
                widget.sizes.forEach { size ->
                    val bitmap = draw(widget.id, pack.id, size)
                    assertTrue("${widget.id} in ${pack.id} is blank", visiblePixels(bitmap) > bitmap.width * bitmap.height / 20)
                    File(out, "${pack.id}_${widget.id}_$size.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
        }
    }

    @Test
    fun `tap cells match the drawn buttons`() {
        assertEquals(listOf(2, 2), ThemeWidgets.regions(context, 7, "toggle.theme.quick")!!.map { it.size })
        assertEquals(listOf(1, 3), ThemeWidgets.regions(context, 7, "fun.theme.music")!!.map { it.size })
        assertEquals(listOf(2), ThemeWidgets.regions(context, 7, "toggle.theme.levels")!!.map { it.size })
        assertEquals(listOf(4), ThemeWidgets.regions(context, 7, "toggle.theme.buttons")!!.map { it.size })
        assertNotNull(ThemeWidgets.single(context, 7, "prod.focus.timer.theme"))
    }

    private fun draw(id: String, packId: String, size: SizeBucket): Bitmap {
        val def = WidgetRegistry.getById(id)!!
        val dims = SizeResolver.resolveDimensions(context, size)
        val spec = WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = size,
            accent = AccentOption.RED,
            bgMode = BackgroundMode.TRANSPARENT,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = def.defaultTap,
            configJson = JSONObject().put(ThemeRepository.KEY_PACK, packId).toString()
        )
        val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
        def.render(context, spec, Canvas(bitmap))
        return bitmap
    }

    private fun visiblePixels(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { (it ushr 24) > 16 }
    }
}
