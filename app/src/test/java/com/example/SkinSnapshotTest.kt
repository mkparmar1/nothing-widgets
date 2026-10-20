package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.Skin
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.core.time.DotStyle
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders a representative set of widgets in every [Skin] (one contact sheet per skin) and fails
 * if any skin crashes a renderer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkinSnapshotTest {

    private val sample = listOf(
        "clock.dotdigital.s", "clock.dotdigital.m", "clock.flip.style", "clock.analog.ring",
        "clock.text.words", "calendar.day.date", "calendar.month.grid", "calendar.iso.week",
        "battery.ring", "battery.segmented", "weather.current.temp", "weather.5day.strip",
        "sensor.ram.meter", "sensor.year.progress", "prod.focus.timer", "prod.step.counter",
        "toggle.wifi", "fun.dice.roll"
    )

    @Test
    fun `every skin renders the sample widgets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val failures = mutableListOf<String>()
        File("build/widget-snapshots").mkdirs()

        for (skin in Skin.entries) {
            val renders = mutableListOf<Pair<String, Bitmap>>()
            val config = JSONObject().put(Skin.KEY, skin.name).toString()
            for (id in sample) {
                val definition = WidgetRegistry.getById(id) ?: continue
                val size = definition.sizes.first()
                val dims = SizeResolver.resolveDimensions(context, size)
                val spec = WidgetSpec(
                    widthPx = dims.widthPx, heightPx = dims.heightPx, density = dims.density, sizeBucket = size,
                    accent = AccentOption.RED, bgMode = BackgroundMode.SOLID, themeMode = ThemeMode.FORCE_DARK,
                    isDark = skin.resolveDark(true), cornerRadiusDp = 20, showLabel = true,
                    tapAction = definition.defaultTap, configJson = config
                )
                val bmp = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    DotMatrixRenderer.withStyle(DotStyle.ROUND, skin) { definition.render(context, spec, Canvas(bmp)) }
                    renders += id to bmp
                } catch (e: Throwable) {
                    failures += "$id [$skin]: $e"
                }
            }
            writeSheet(File("build/widget-snapshots/_sheet_SKIN_${skin.name}.png"), renders)
        }
        assertTrue("Skins failed to render:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun writeSheet(file: File, items: List<Pair<String, Bitmap>>) {
        val cellW = 420
        val cellH = 260
        val cols = 3
        val sheet = Bitmap.createBitmap(cols * cellW, ((items.size + cols - 1) / cols) * cellH, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet).apply { drawColor(0xFF2A2A2A.toInt()) }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFF00.toInt(); textSize = 16f }
        items.forEachIndexed { i, (name, bmp) ->
            val x = (i % cols) * cellW
            val y = (i / cols) * cellH
            val scale = minOf((cellW - 20f) / bmp.width, (cellH - 36f) / bmp.height)
            c.drawBitmap(bmp, null, RectF(x + 10f, y + 26f, x + 10f + bmp.width * scale, y + 26f + bmp.height * scale), null)
            c.drawText(name, x + 10f, y + 18f, label)
        }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
