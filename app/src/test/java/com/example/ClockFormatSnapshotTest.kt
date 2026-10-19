package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.render.DotMatrixRenderer
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.core.time.ClockOptions
import com.example.core.time.DateFormat
import com.example.core.time.DotStyle
import com.example.core.time.TimeFormat
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the digital clock with every time format, date format and dot style for visual review. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClockFormatSnapshotTest {

    @Test
    fun `clock formats and dot styles render`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val definition = WidgetRegistry.getById("clock.dotdigital.m")!!
        val dims = SizeResolver.resolveDimensions(context, SizeBucket.M)

        val variants = TimeFormat.entries.map { "time: ${it.name}" to config(time = it) } +
            DateFormat.entries.map { "date: ${it.name}" to config(date = it) } +
            DotStyle.entries.map { "dots: ${it.name}" to config(dots = it) }

        val cellW = 500
        val cellH = 290
        val cols = 3
        val sheet = Bitmap.createBitmap(cols * cellW, ((variants.size + cols - 1) / cols) * cellH, Bitmap.Config.ARGB_8888)
        val sheetCanvas = Canvas(sheet).apply { drawColor(0xFF2A2A2A.toInt()) }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFF00.toInt(); textSize = 18f }

        variants.forEachIndexed { i, (name, json) ->
            val spec = WidgetSpec(
                widthPx = dims.widthPx, heightPx = dims.heightPx, density = dims.density, sizeBucket = SizeBucket.M,
                accent = AccentOption.RED, bgMode = BackgroundMode.SOLID, themeMode = ThemeMode.FORCE_DARK,
                isDark = true, cornerRadiusDp = 20, showLabel = true, tapAction = TapAction.DEFAULT, configJson = json
            )
            val bmp = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
            DotMatrixRenderer.withDotStyle(ClockOptions.dotStyle(json)) { definition.render(context, spec, Canvas(bmp)) }
            val x = (i % cols) * cellW
            val y = (i / cols) * cellH
            val scale = minOf((cellW - 20f) / bmp.width, (cellH - 40f) / bmp.height)
            sheetCanvas.drawBitmap(bmp, null, RectF(x + 10f, y + 30f, x + 10f + bmp.width * scale, y + 30f + bmp.height * scale), null)
            sheetCanvas.drawText(name, x + 10f, y + 22f, label)
        }
        File("build/widget-snapshots").mkdirs()
        File("build/widget-snapshots/_sheet_CLOCK_FORMATS.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun `clock options read back from config`() {
        val opts = ClockOptions.from(config(time = TimeFormat.H12_SECONDS, date = DateFormat.DD_MM_YY))
        assertEquals(TimeFormat.H12_SECONDS, opts.time)
        assertEquals(DateFormat.DD_MM_YY, opts.date)
        assertEquals(DotStyle.SQUARE, ClockOptions.dotStyle(config(dots = DotStyle.SQUARE)))
        // Missing keys fall back to the defaults the caller passes.
        assertEquals(DateFormat.NONE, ClockOptions.from("{}", defaultDate = DateFormat.NONE).date)
    }

    private fun config(time: TimeFormat? = null, date: DateFormat? = null, dots: DotStyle? = null) = JSONObject().apply {
        time?.let { put(ClockOptions.KEY_TIME, it.name) }
        date?.let { put(ClockOptions.KEY_DATE, it.name) }
        dots?.let { put(ClockOptions.KEY_DOT_STYLE, it.name) }
    }.toString()
}
