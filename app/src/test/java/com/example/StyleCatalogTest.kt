package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.config.WidgetOption
import com.example.core.config.WidgetOptions
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.demo.DemoData
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.StyleCatalog
import com.example.widgets.styles.StyleSelection
import com.example.core.render.WidgetLayoutRecorder
import com.example.widgets.base.WidgetRenderEngine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the "existing widgets are untouched" promise of the style system and renders every new style.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleCatalogTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun spec(size: SizeBucket, isDark: Boolean, configJson: String): WidgetSpec {
        val dims = SizeResolver.resolveDimensions(context, size)
        return WidgetSpec(
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
            tapAction = WidgetRegistry.getAll().first().defaultTap,
            configJson = configJson
        )
    }

    private fun render(def: WidgetDefinition, spec: WidgetSpec): Bitmap =
        Bitmap.createBitmap(spec.widthPx, spec.heightPx, Bitmap.Config.ARGB_8888).also { def.render(context, spec, Canvas(it)) }

    private fun styleJson(id: String) = WidgetOptions.write("{}", mapOf(StyleCatalog.KEY to id))

    @Test
    fun `every catalogued widget id exists and gets a Style option with Classic as default`() {
        for (id in StyleCatalog.styledWidgetIds()) {
            val def = WidgetRegistry.getById(id)
            assertNotNull("Style catalogue lists unknown widget $id", def)
            val option = def!!.options.firstOrNull { it.key == StyleCatalog.KEY } as? WidgetOption.Choice
            assertNotNull("$id has no Style option", option)
            assertEquals(StyleCatalog.CLASSIC, option!!.default)
            assertEquals(StyleCatalog.CLASSIC, option.options.first().first)
            val ids = option.options.map { it.first }
            assertEquals("$id has duplicate style ids", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `widgets without styles are not wrapped`() {
        val styled = StyleCatalog.styledWidgetIds()
        WidgetRegistry.getAll().filter { it.id !in styled }.forEach { def ->
            assertTrue("${def.id} unexpectedly has a Style option", def.options.none { it.key == StyleCatalog.KEY })
        }
    }

    @Test
    fun `classic, missing and unknown style render the original design identically`() {
        DemoData.withSnapshot {
            for (id in StyleCatalog.styledWidgetIds()) {
                val def = WidgetRegistry.getById(id)!!
                for (size in def.sizes) {
                    val none = render(def, spec(size, true, "{}"))
                    val classic = render(def, spec(size, true, styleJson(StyleCatalog.CLASSIC)))
                    val unknown = render(def, spec(size, true, styleJson("no_such_style")))
                    assertTrue("$id [$size] classic differs from no style", none.sameAs(classic))
                    assertTrue("$id [$size] unknown style differs from classic", none.sameAs(unknown))
                }
            }
        }
    }

    @Test
    fun `every style renders at every size in light and dark`() {
        val outDir = File("build/widget-snapshots/styles").apply { deleteRecursively(); mkdirs() }
        val failures = mutableListOf<String>()
        val sheets = linkedMapOf<String, MutableList<Pair<String, Bitmap>>>()
        DemoData.withSnapshot {
            for (id in StyleCatalog.styledWidgetIds()) {
                val def = WidgetRegistry.getById(id) ?: continue
                for (style in StyleCatalog.stylesFor(id)) {
                    for (isDark in listOf(true, false)) {
                        for (size in def.sizes) {
                            try {
                                val bmp = render(def, spec(size, isDark, styleJson(style.id)))
                                val theme = if (isDark) "dark" else "light"
                                File(outDir, "${id}__${style.id}_${size}_$theme.png").outputStream().use {
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                                }
                                sheets.getOrPut("${id.substringBefore('.')}_$theme") { mutableListOf() } += "$id ${style.id} $size" to bmp
                            } catch (e: Throwable) {
                                failures += "$id/${style.id} [$size] dark=$isDark: $e"
                            }
                        }
                    }
                }
            }
        }
        sheets.forEach { (name, items) -> writeSheet(File(outDir, "_sheet_$name.png"), items) }
        assertTrue("Styles failed to render:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `no two designs of a widget look the same in light or dark`() {
        val same = mutableListOf<String>()
        DemoData.withSnapshot {
            for (id in StyleCatalog.styledWidgetIds()) {
                val def = WidgetRegistry.getById(id)!!
                for (size in def.sizes) for (isDark in listOf(true, false)) {
                    val shots = (listOf(StyleCatalog.CLASSIC) + StyleCatalog.stylesFor(id).map { it.id })
                        .map { it to render(def, spec(size, isDark, styleJson(it))) }
                    for (i in shots.indices) for (j in i + 1 until shots.size) {
                        if (shots[i].second.sameAs(shots[j].second)) same += "$id [$size dark=$isDark] ${shots[i].first} = ${shots[j].first}"
                    }
                }
            }
        }
        assertTrue(same.joinToString("\n"), same.isEmpty())
    }

    private fun blocks(def: WidgetDefinition, spec: WidgetSpec) =
        WidgetLayoutRecorder.begin(spec.density).let { capture ->
            try { render(def, spec) } finally { capture.close() }
            capture.blocks
        }

    @Test
    fun `style text stays inside the card, uncut and not overlapping`() {
        val issues = sortedSetOf<String>()
        DemoData.withSnapshot {
            for (id in StyleCatalog.styledWidgetIds()) {
                val def = WidgetRegistry.getById(id)!!
                for (style in StyleCatalog.stylesFor(id)) for (size in def.sizes) {
                    val sp = spec(size, true, styleJson(style.id))
                    val all = blocks(def, sp)
                    all.forEachIndexed { i, b ->
                        val at = "$id/${style.id} [$size] '${b.text}'"
                        if (b.bounds.left < 0 || b.bounds.right > sp.widthPx || b.bounds.bottom > sp.heightPx || b.bounds.top < -2f) issues += "$at outside"
                        if (b.wasTruncated) issues += "$at cut off"
                        all.drop(i + 1).forEach { o ->
                            // Font boxes include line spacing, so only count glyphs that really collide.
                            val w = minOf(b.bounds.right, o.bounds.right) - maxOf(b.bounds.left, o.bounds.left)
                            val h = minOf(b.bounds.bottom, o.bounds.bottom) - maxOf(b.bounds.top, o.bounds.top)
                            if (w > 1f && h > 0.35f * minOf(b.bounds.height(), o.bounds.height())) issues += "$at overlaps '${o.text}'"
                        }
                    }
                }
            }
        }
        assertTrue(issues.joinToString("\n"), issues.isEmpty())
    }

    @Test
    fun `weather styles never show invented readings without data`() {
        // Network off and a place nothing has cached: the repository returns its stale placeholder.
        com.example.data.weather.WeatherRepository.setNetworkEnabled(context, false)
        com.example.data.weather.WeatherRepository.setPlace(
            context, com.example.data.weather.WeatherPlace("Nowhere", 1.23, 4.56, "UTC")
        )
        val ids = listOf(
            "weather.current.temp", "weather.uv.index", "weather.aqi.dial", "weather.pressure.gauge",
            "weather.wind.compass", "weather.feels.humidity", "weather.hourly.strip"
        )
        for (id in ids) {
            val def = WidgetRegistry.getById(id)!!
            for (style in StyleCatalog.stylesFor(id)) for (size in def.sizes) {
                val texts = blocks(def, spec(size, true, styleJson(style.id))).map { it.text }
                assertTrue("$id/${style.id} [$size] shows no -- in $texts", texts.any { it.contains("--") })
                assertTrue("$id/${style.id} [$size] shows a number in $texts", texts.none { t -> t.any(Char::isDigit) })
            }
        }
    }

    /** The widget plus the engine's clock caption step, drawn on this thread so its text is recorded. */
    private fun blocksWithCaption(def: WidgetDefinition, spec: WidgetSpec): List<String> {
        val capture = WidgetLayoutRecorder.begin(spec.density)
        val caption = WidgetRenderEngine::class.java.getDeclaredMethod(
            "drawClockFormatCaption", Canvas::class.java, WidgetSpec::class.java, String::class.java, Boolean::class.java
        ).apply { isAccessible = true }
        try {
            val canvas = Canvas(Bitmap.createBitmap(spec.widthPx, spec.heightPx, Bitmap.Config.ARGB_8888))
            def.render(context, spec, canvas)
            caption.invoke(WidgetRenderEngine, canvas, spec, def.id, false)
        } finally {
            capture.close()
        }
        return capture.blocks.map { it.text }
    }

    @Test
    fun `clock captions only appear for an explicitly chosen format and never on a style`() {
        val def = WidgetRegistry.getById("clock.analog.ring")!!
        val iso = "{\"date_format\":\"ISO\"}"
        fun count(json: String) = blocksWithCaption(def, spec(SizeBucket.M, true, json)).size
        assertEquals(blocks(def, spec(SizeBucket.M, true, "{}")).size, count("{}"))
        assertEquals(count("{}") + 1, count(iso))
        assertEquals(count(styleJson("classic_light")), count(WidgetOptions.write(iso, mapOf(StyleCatalog.KEY to "classic_light"))))
    }

    @Test
    fun `new widgets get the listed design while existing configs stay classic`() {
        val prefs = com.example.data.prefs.PreferencesManager.getInstance(context)
        val def = WidgetRegistry.getById("weather.uv.index")!!
        val plain = com.example.ui.config.WidgetSetupState.defaults(def, prefs)
        assertEquals(StyleCatalog.CLASSIC, plain.optionValues[StyleCatalog.KEY])
        val fresh = plain.withNewStyle(def.id, StyleCatalog.listingStyle(def.id))
        assertEquals("soft_gauge", fresh.optionValues[StyleCatalog.KEY])
        assertEquals(ThemeMode.FORCE_LIGHT, fresh.theme)
        assertEquals(StyleCatalog.CLASSIC, StyleCatalog.listingStyle("clock.binary"))
    }

    @Test
    fun `dark and light widgets drawn at the same time keep their own colours`() {
        val def = WidgetRegistry.getById("clock.dotdigital.s")!!
        val bold = spec(SizeBucket.S, false, styleJson("bold_time"))
        val soft = spec(SizeBucket.S, false, styleJson("soft_time"))
        fun corner(sp: WidgetSpec) = render(def, sp).getPixel(sp.widthPx / 2, 6)
        val expected = listOf(corner(bold), corner(soft))
        val wrong = java.util.concurrent.atomic.AtomicInteger()
        listOf(0, 1, 0, 1).map { which ->
            Thread { repeat(300) { if (corner(if (which == 0) bold else soft) != expected[which]) wrong.incrementAndGet() } }
        }.onEach { it.start() }.forEach { it.join() }
        assertEquals(0, wrong.get())
    }

    @Test
    fun `yesterdays festival is not shown as today`() {
        val day = java.util.Calendar.getInstance().apply { clear(); set(2026, 10, 25) }
        val f = com.example.data.india.IndiaCalendar.Festival("2026-11-24", "X", true)
        assertEquals(-1, f.daysAway(day))
    }

    @Test
    fun `wind label follows the reported direction`() {
        fun from(deg: Int?) = com.example.data.weather.WeatherData("X", 1, 0, "", windDirectionDeg = deg).windFrom
        assertEquals(listOf("N", "E", "SW", "N", null), listOf(from(5), from(90), from(225), from(350), from(null)))
    }

    @Test
    fun `every design has its own list entry with a real widget`() {
        val designs = StyleCatalog.allDesigns()
        assertEquals(StyleCatalog.styledWidgetIds().sumOf { StyleCatalog.stylesFor(it).size }, designs.size)
        assertEquals(designs.size, designs.map { (id, style) -> "$id/${style.id}" }.toSet().size)
        assertTrue(designs.all { (id, _) -> WidgetRegistry.getById(id) != null })
    }

    @Test
    fun `a picked style is handed to setup exactly once`() {
        StyleSelection.set("battery.ring", "soft_ring")
        assertEquals("soft_ring", StyleSelection.take("battery.ring"))
        assertEquals(StyleCatalog.CLASSIC, StyleSelection.take("battery.ring"))
    }

    /** Lays the renders out in a labelled grid for quick visual review. */
    private fun writeSheet(file: File, items: List<Pair<String, Bitmap>>) {
        val cell = 300
        val label = 26
        val cols = 6
        val rows = (items.size + cols - 1) / cols
        val sheet = Bitmap.createBitmap(cols * cell, rows * (cell + label), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(0xFF808080.toInt())
        val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 15f }
        items.forEachIndexed { i, (name, bmp) ->
            val x = (i % cols) * cell
            val y = (i / cols) * (cell + label)
            val scale = minOf((cell - 10f) / bmp.width, (cell - 10f) / bmp.height)
            val w = (bmp.width * scale).toInt()
            val h = (bmp.height * scale).toInt()
            canvas.drawBitmap(bmp, null, android.graphics.Rect(x + (cell - w) / 2, y + (cell - h) / 2, x + (cell + w) / 2, y + (cell + h) / 2), null)
            canvas.drawText(name, x + 6f, y + cell + 18f, text)
        }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
