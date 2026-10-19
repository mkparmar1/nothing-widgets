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
        val outDir = File("build/widget-snapshots/all").apply { deleteRecursively(); mkdirs() }
        val failures = mutableListOf<String>()
        val byCategory = linkedMapOf<String, MutableList<Pair<String, Bitmap>>>()

        for (isDark in listOf(true, false)) {
            val themeKey = if (isDark) "dark" else "light"
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
                        themeMode = if (isDark) ThemeMode.FORCE_DARK else ThemeMode.FORCE_LIGHT,
                        isDark = isDark,
                        cornerRadiusDp = 20,
                        showLabel = true,
                        tapAction = definition.defaultTap
                    )
                    val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                    try {
                        definition.render(context, spec, Canvas(bitmap))
                        File(outDir, "${definition.id}_${size.name}_$themeKey.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        byCategory.getOrPut("${definition.category.name}_$themeKey") { mutableListOf() } +=
                            "${definition.id} [$size]" to bitmap
                    } catch (e: Throwable) {
                        failures += "${definition.id} [$size] $themeKey: $e"
                    }
                }
            }
        }

        byCategory.forEach { (category, items) -> writeContactSheet(File(outDir, "_sheet_$category.png"), items) }

        assertTrue("Widgets failed to render:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /**
     * Phase 2b crispness pass: render the Tier 1/2 widgets at the real dp sizes a Nothing Phone
     * (3a) Pro reports from the launcher (2×2 ≈ 155dp, 4×2 ≈ 329×155dp, 4×4 ≈ 329×329dp). Fails on
     * any crash so a widget that breaks at a real aspect ratio is caught before shipping, and writes
     * sheets to build/widget-snapshots for eyeballing tiny text or empty space.
     */
    @Test
    fun `tier 1 and 2 widgets render at real device sizes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outDir = File("build/widget-snapshots/real").apply { mkdirs() }
        val failures = mutableListOf<String>()
        val items = mutableListOf<Pair<String, Bitmap>>()

        // (dp width, dp height) pairs the launcher actually reports on the target phone.
        val realSizes = listOf(
            SizeBucket.S to (155 to 155),
            SizeBucket.M to (329 to 155),
            SizeBucket.L to (329 to 329)
        )

        val bySize = linkedMapOf<SizeBucket, MutableList<Pair<String, Bitmap>>>()
        for (id in TIER_1_AND_2) {
            val def = WidgetRegistry.getById(id) ?: continue
            for ((bucket, dp) in realSizes) {
                if (bucket !in def.sizes) continue
                val dims = SizeResolver.resolveDimensions(context, bucket, dp.first, dp.second)
                val spec = WidgetSpec(
                    widthPx = dims.widthPx,
                    heightPx = dims.heightPx,
                    density = dims.density,
                    sizeBucket = bucket,
                    accent = AccentOption.RED,
                    bgMode = BackgroundMode.SOLID,
                    themeMode = ThemeMode.FORCE_DARK,
                    isDark = true,
                    cornerRadiusDp = 20,
                    showLabel = true,
                    tapAction = def.defaultTap
                )
                val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    def.render(context, spec, Canvas(bitmap))
                    items += "$id [$bucket @ ${dp.first}×${dp.second}dp]" to bitmap
                    bySize.getOrPut(bucket) { mutableListOf() } += "$id [$bucket]" to bitmap
                } catch (e: Throwable) {
                    failures += "$id [$bucket ${dp.first}×${dp.second}dp]: $e"
                }
            }
        }

        bySize.forEach { (bucket, list) ->
            writeContactSheet(File(outDir, "_sheet_real_${bucket.name}.png"), list)
        }

        assertTrue("Widgets failed at real device sizes:\n" + failures.joinToString("\n"), failures.isEmpty())
        assertTrue("Tier 1/2 render produced no snapshots", items.isNotEmpty())
    }

    /** Tier 1 + Tier 2 widget ids from the build plan, used by the real-size crispness pass. */
    private val TIER_1_AND_2 = listOf(
        // Tier 1
        "clock.dotdigital.s", "clock.dotdigital.m", "clock.dotdigital.l", "clock.analog.ring",
        "calendar.day.date", "weather.current.temp", "battery.ring", "toggle.grid.2x2",
        "media.nowplaying.compact", "prod.step.counter", "sensor.compass.heading",
        "photo.single.frame",
        // Tier 2
        "clock.world.london", "clock.world.custom", "weather.hourly.strip", "weather.5day.strip",
        "calendar.agenda.3", "battery.screen.on", "battery.screen.week", "prod.notes.pin",
        "prod.todo.3", "contact.favorite.single", "prod.search.bar", "utility.folder.grid",
        "weather.moon.phase", "weather.uv.index", "weather.sunrise.arc"
    )

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
