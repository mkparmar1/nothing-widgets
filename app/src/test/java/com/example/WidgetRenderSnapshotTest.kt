package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every widget at every supported size with real (native) graphics.
 * Fails if any renderer throws, and writes PNGs to build/widget-snapshots for visual review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderSnapshotTest {

    @Test
    fun `every widget renders at every size without errors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outDir = File("build/widget-snapshots").apply { deleteRecursively(); mkdirs() }
        val failures = mutableListOf<String>()
        val byCategory = linkedMapOf<String, MutableList<Pair<String, Bitmap>>>()

        for (definition in WidgetRegistry.getAll()) {
            for (size in definition.sizes) {
                val dims = SizeResolver.resolveDimensions(context, size)
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
                    tapAction = definition.defaultTap
                )
                val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    definition.render(context, spec, Canvas(bitmap))
                    File(outDir, "${definition.id}_${size.name}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    byCategory.getOrPut(definition.category.name) { mutableListOf() } += "${definition.id} [$size]" to bitmap
                } catch (e: Throwable) {
                    failures += "${definition.id} [$size]: $e"
                }
            }
        }

        byCategory.forEach { (category, items) -> writeContactSheet(File(outDir, "_sheet_$category.png"), items) }

        assertTrue("Widgets failed to render:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** All of a category's renders on one labelled grid, for quick visual review. */
    private fun writeContactSheet(file: File, items: List<Pair<String, Bitmap>>) {
        val cellW = 500
        val cellH = 300
        val cols = 3
        val rows = (items.size + cols - 1) / cols
        val sheet = Bitmap.createBitmap(cols * cellW, rows * cellH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(0xFF2A2A2A.toInt())
        val label = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFF00.toInt()
            textSize = 18f
        }
        items.forEachIndexed { i, (name, bmp) ->
            val x = (i % cols) * cellW
            val y = (i / cols) * cellH
            val scale = minOf((cellW - 20f) / bmp.width, (cellH - 40f) / bmp.height)
            val dst = android.graphics.RectF(x + 10f, y + 30f, x + 10f + bmp.width * scale, y + 30f + bmp.height * scale)
            canvas.drawBitmap(bmp, null, dst, null)
            canvas.drawText(name, x + 10f, y + 22f, label)
        }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
