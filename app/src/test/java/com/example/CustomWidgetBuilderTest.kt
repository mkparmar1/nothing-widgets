package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.CustomWidget
import com.example.data.db.AppDatabase
import com.example.data.weather.WeatherData
import com.example.widgets.base.TapAction
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import com.example.widgets.builder.BuilderRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class CustomWidgetBuilderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `versioned layout round trips and rejects invalid data`() {
        val source = BuilderLayout(
            listOf(
                BuilderBlock(type = BuilderBlockType.TIME, x = 1, y = 2, width = 8, height = 4),
                BuilderBlock(type = BuilderBlockType.TEXT, text = "Hello", x = 2, y = 8, width = 12, height = 3)
            )
        )
        val parsed = BuilderLayout.parse(source.toJson())
        assertNotNull(parsed)
        assertEquals(BuilderLayout.VERSION, parsed!!.version)
        assertEquals(source.blocks, parsed.blocks)
        assertNull(BuilderLayout.parse("""{"v":99,"blocks":[]}"""))
        assertNull(BuilderLayout.parse("not json"))
    }

    @Test
    fun `custom designs persist in room and register as widget definitions`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val widget = CustomWidget(
                id = "test-builder-design",
                name = "Clock + text",
                layoutJson = BuilderLayout(listOf(BuilderBlock(type = BuilderBlockType.TIME))).toJson(),
                sizesCsv = "S,M"
            )
            database.customWidgetDao().upsert(widget)
            assertEquals(widget, database.customWidgetDao().getById(widget.id))
            assertEquals(
                setOf(com.example.core.bitmap.SizeBucket.S, com.example.core.bitmap.SizeBucket.M),
                database.customWidgetDao().getById(widget.id)!!.supportedSizes
            )
        } finally {
            database.close()
        }

        val definition = BuilderRenderer.definition(
            CustomWidget(
                id = "test-builder-render",
                name = "Clock + battery",
                layoutJson = BuilderLayout(
                    listOf(
                        BuilderBlock(type = BuilderBlockType.TIME, x = 1, y = 1, width = 14, height = 5),
                        BuilderBlock(type = BuilderBlockType.BATTERY, x = 1, y = 8, width = 8, height = 3),
                        BuilderBlock(type = BuilderBlockType.PROGRESS, x = 10, y = 8, width = 5, height = 5),
                        BuilderBlock(type = BuilderBlockType.DIVIDER, x = 1, y = 7, width = 14, height = 1),
                        BuilderBlock(type = BuilderBlockType.SPACER, x = 1, y = 14, width = 4, height = 1),
                        BuilderBlock(type = BuilderBlockType.TEXT, x = 1, y = 13, width = 14, height = 2, text = "TODAY"),
                        BuilderBlock(type = BuilderBlockType.DATE, x = 1, y = 6, width = 14, height = 1),
                        BuilderBlock(type = BuilderBlockType.STEPS, x = 1, y = 12, width = 8, height = 1),
                        BuilderBlock(type = BuilderBlockType.NEXT_EVENT, x = 1, y = 11, width = 14, height = 1),
                        BuilderBlock(type = BuilderBlockType.IMAGE, x = 8, y = 13, width = 4, height = 2),
                        BuilderBlock(type = BuilderBlockType.WEATHER, x = 1, y = 10, width = 14, height = 3)
                    )
                ).toJson()
            )
        )!!
        assertEquals(
            setOf(com.example.ui.permissions.AppPermission.Activity, com.example.ui.permissions.AppPermission.Calendar, com.example.ui.permissions.AppPermission.Location),
            definition.requiredPermissions
        )
        val layout = BuilderLayout.parse(
            CustomWidget(
                id = "test-builder-render",
                name = "Clock + battery",
                layoutJson = BuilderLayout(
                    listOf(
                        BuilderBlock(type = BuilderBlockType.TIME),
                        BuilderBlock(type = BuilderBlockType.DATE),
                        BuilderBlock(type = BuilderBlockType.BATTERY),
                        BuilderBlock(type = BuilderBlockType.WEATHER),
                        BuilderBlock(type = BuilderBlockType.STEPS),
                        BuilderBlock(type = BuilderBlockType.NEXT_EVENT),
                        BuilderBlock(type = BuilderBlockType.TEXT, text = "TEST"),
                        BuilderBlock(type = BuilderBlockType.PROGRESS),
                        BuilderBlock(type = BuilderBlockType.IMAGE),
                        BuilderBlock(type = BuilderBlockType.DIVIDER),
                        BuilderBlock(type = BuilderBlockType.SPACER)
                    )
                ).toJson()
            ).layoutJson
        )!!
        for (size in listOf(com.example.core.bitmap.SizeBucket.S, com.example.core.bitmap.SizeBucket.M, com.example.core.bitmap.SizeBucket.L)) {
            val dimensions = SizeResolver.resolveDimensions(context, size)
            val spec = WidgetSpec(
                widthPx = dimensions.widthPx,
                heightPx = dimensions.heightPx,
                density = dimensions.density,
                sizeBucket = size,
                accent = AccentOption.RED,
                bgMode = BackgroundMode.SOLID,
                themeMode = ThemeMode.FORCE_DARK,
                isDark = true,
                cornerRadiusDp = 12,
                showLabel = false,
                tapAction = TapAction.OPEN_MAIN
            )
            val bitmap = Bitmap.createBitmap(spec.widthPx, spec.heightPx, Bitmap.Config.ARGB_8888)
            BuilderRenderer.renderWithWeather(
                context,
                spec,
                Canvas(bitmap),
                layout,
                WeatherData("Test", 21, 1, "Clear", isStale = false)
            )
            assertTrue(bitmap.width > 0 && bitmap.height > 0)
            bitmap.recycle()
        }
    }

    @Test
    fun `invalid custom layout does not register a definition`() {
        assertNull(
            BuilderRenderer.definition(
                CustomWidget(id = "broken", name = "Broken", layoutJson = """{"v":1,"blocks":[{"type":"MISSING"}]}""")
            )
        )
        try {
            WidgetRegistry.replaceCustom(
                listOf(CustomWidget("custombad01", "Broken", """{"v":99,"blocks":[]}"""))
            )
            assertNull(WidgetRegistry.getById("custom.custombad01"))
            assertEquals("clock.dotdigital.s", WidgetRegistry.getAll().first().id)
        } finally {
            WidgetRegistry.replaceCustom(emptyList())
        }
    }

    @Test
    fun `custom definition is dynamically registered in my widgets`() {
        val widget = CustomWidget(
            id = "registered-widget",
            name = "Registered",
            layoutJson = BuilderLayout(listOf(BuilderBlock(type = BuilderBlockType.TIME))).toJson(),
            sizesCsv = "M"
        )
        try {
            WidgetRegistry.replaceCustom(listOf(widget))
            assertNotNull(WidgetRegistry.getById(widget.widgetTypeId))
            assertEquals(setOf(com.example.core.bitmap.SizeBucket.M), WidgetRegistry.getById(widget.widgetTypeId)!!.sizes)
            assertEquals(1, WidgetRegistry.getByCategory(Category.CUSTOM).size)
        } finally {
            WidgetRegistry.replaceCustom(emptyList())
        }
    }
}
