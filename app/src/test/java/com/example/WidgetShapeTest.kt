package com.example

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.render.Frame
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.WidgetInstance
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetShape
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetShapeTest {

    private fun spec(
        context: Context,
        shape: WidgetShape,
        customPoints: List<ShapePoint> = emptyList(),
        configJson: String = ""
    ): WidgetSpec {
        val dims = SizeResolver.resolveDimensions(context, SizeBucket.M)
        return WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = SizeBucket.M,
            accent = AccentOption.RED,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = WidgetRegistry.getById(DEFINITION)!!.defaultTap,
            shape = shape,
            customPoints = customPoints,
            configJson = configJson
        )
    }

    @Test
    fun `every shape renders content and clipped shapes leave their corners transparent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val definition = WidgetRegistry.getById(DEFINITION)!!
        val clippedCorners = setOf(
            WidgetShape.ROUNDED, WidgetShape.PILL, WidgetShape.CIRCLE, WidgetShape.ELLIPSE,
            WidgetShape.SQUIRCLE, WidgetShape.CUT, WidgetShape.BEVEL, WidgetShape.DIAMOND,
            WidgetShape.TRIANGLE, WidgetShape.HEXAGON, WidgetShape.STAR_5, WidgetShape.GEAR,
            WidgetShape.HEART, WidgetShape.BLOB, WidgetShape.LEAF, WidgetShape.DROP,
            WidgetShape.CRESCENT, WidgetShape.LENS, WidgetShape.CUSTOM
        )
        for (shape in WidgetShape.entries) {
            val bitmap = WidgetRenderEngine.renderBitmap(context, definition, spec(context, shape))
            var paintedSamples = 0
            for (xStep in 1..9) {
                for (yStep in 1..9) {
                    val pixel = bitmap.getPixel(bitmap.width * xStep / 10, bitmap.height * yStep / 10)
                    if (Color.alpha(pixel) > 0) paintedSamples++
                }
            }
            assertTrue("$shape should draw content", paintedSamples > 0)
            if (shape == WidgetShape.SQUARE) {
                assertTrue("Square should fill its corner", Color.alpha(bitmap.getPixel(5, 5)) > 0)
            } else if (shape in clippedCorners) {
                assertEquals("$shape should leave the corner transparent", 0, Color.alpha(bitmap.getPixel(2, 2)))
            }
        }
    }

    @Test
    fun `custom points round trip through encoding`() {
        val points = listOf(
            ShapePoint(0f, 0f),
            ShapePoint(0.5f, 0.25f),
            ShapePoint(1f, 0f),
            ShapePoint(0.75f, 1f),
            ShapePoint(0.25f, 1f)
        )
        assertEquals(points, WidgetShape.decodePoints(WidgetShape.encodePoints(points)))
        assertEquals(emptyList<ShapePoint>(), WidgetShape.decodePoints("0,0;1,1"))
        assertEquals(emptyList<ShapePoint>(), WidgetShape.decodePoints("bad;2,2"))
        assertEquals(6, WidgetShape.defaultCustomPoints().size)
    }

    @Test
    fun `custom outline clips rendering to the drawn polygon`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val definition = WidgetRegistry.getById(DEFINITION)!!
        val rectangle = listOf(
            ShapePoint(0f, 0f), ShapePoint(1f, 0f), ShapePoint(1f, 1f), ShapePoint(0f, 1f)
        )
        val filled = WidgetRenderEngine.renderBitmap(
            context, definition,
            spec(context, WidgetShape.CUSTOM, rectangle, "custom-rectangle")
        )
        assertTrue("Custom rectangle should fill its corner", Color.alpha(filled.getPixel(6, 6)) > 0)

        val triangle = listOf(ShapePoint(0f, 0f), ShapePoint(1f, 0f), ShapePoint(0.5f, 1f))
        val clipped = WidgetRenderEngine.renderBitmap(
            context, definition,
            spec(context, WidgetShape.CUSTOM, triangle, "custom-triangle")
        )
        assertEquals(0, Color.alpha(clipped.getPixel(clipped.width - 6, 6)))
    }

    @Test
    fun `shape fill colour override is rendered`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val definition = WidgetRegistry.getById(DEFINITION)!!
        val spec = spec(context, WidgetShape.SQUARE).copy(
            configJson = "fill-test",
            shapeFillColor = Color.RED,
            shapeBorderStyle = Frame.NONE
        )
        val bitmap = WidgetRenderEngine.renderBitmap(context, definition, spec)
        assertEquals(Color.RED, bitmap.getPixel(bitmap.width / 2, 12))
    }

    @Test
    fun `instance reads the persisted shape and falls back to rounded`() {
        val instance = WidgetInstance(
            appWidgetId = 7,
            widgetTypeId = DEFINITION,
            configJson = """{"shape":"CIRCLE"}"""
        )
        assertEquals("CIRCLE", instance.getShape())
        assertEquals(WidgetShape.CIRCLE, WidgetShape.fromName(instance.getShape()))
        assertEquals(WidgetShape.ROUNDED, WidgetShape.fromName(WidgetInstance(1, DEFINITION).getShape()))
        assertEquals(WidgetShape.ROUNDED, WidgetShape.fromName("bogus"))
    }

    private companion object {
        const val DEFINITION = "clock.analog.ring"
    }
}
