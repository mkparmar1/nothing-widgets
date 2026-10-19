package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.render.GlyphTable
import com.example.data.db.WidgetInstance
import com.example.data.setups.SetupRepository
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dot Widgets", appName)
    }

    @Test
    fun `widget catalogue contains 80 or more widgets`() {
        val all = WidgetRegistry.getAll()
        assertTrue("Must contain at least 80 widgets in catalogue", all.size >= 80)
    }

    @Test
    fun `categories cover all required widget types`() {
        val clocks = WidgetRegistry.getByCategory(Category.CLOCK)
        val calendars = WidgetRegistry.getByCategory(Category.CALENDAR)
        val batteries = WidgetRegistry.getByCategory(Category.BATTERY)
        val weathers = WidgetRegistry.getByCategory(Category.WEATHER)
        val toggles = WidgetRegistry.getByCategory(Category.TOGGLES)

        assertTrue(clocks.size >= 14)
        assertTrue(calendars.size >= 10)
        assertTrue(batteries.size >= 8)
        assertTrue(weathers.size >= 10)
        assertTrue(toggles.size >= 10)
    }

    @Test
    fun `glyph table returns valid 35 bit array for digits`() {
        val glyph0 = GlyphTable.getGlyph('0')
        assertEquals(35, glyph0.size)
        // '0' has first row mostly 1s
        assertTrue(glyph0[0])
        assertTrue(glyph0[1])
        assertTrue(glyph0[2])
    }

    @Test
    fun `widget instance config migration and unknown key preservation`() {
        val instance = WidgetInstance(
            appWidgetId = 42,
            widgetTypeId = "clock.dotdigital.v1",
            schemaVersion = 1,
            configJson = """{"accent":"AMBER","future_custom_key":999}"""
        )
        assertEquals("AMBER", instance.getAccent())
        assertEquals(999, instance.getInt("future_custom_key"))
    }

    @Test
    fun `setups and wallpapers match specifications`() {
        assertTrue(SetupRepository.SETUPS.size >= 8)
        assertTrue(WallpaperManagerHelper.WALLPAPERS.size >= 100)
    }
}
