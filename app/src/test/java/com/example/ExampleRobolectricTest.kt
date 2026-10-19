package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.render.GlyphTable
import com.example.core.theme.AccentOption
import com.example.data.db.WidgetInstance
import com.example.data.setups.CommunityRepository
import com.example.data.setups.SetupRepository
import com.example.data.setups.SetupShareRepository
import com.example.data.wallpapers.WallpaperManagerHelper
import com.example.widgets.base.Category
import com.example.widgets.base.WidgetRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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

    @Test
    fun `setup share code round-trips widget configs and accent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val code = SetupShareRepository.encode(
            listOf(
                WidgetInstance(1, "clock.dotdigital.m", 1, """{"accent":"AMBER","bg_alpha":80}"""),
                WidgetInstance(2, "battery.ring", 1, """{"accent":"CYAN"}""")
            ),
            AccentOption.LIME
        )
        assertTrue("code must carry the DWSET1 prefix", code.startsWith("DWSET1:"))

        val result = SetupShareRepository.import(context, code)
        assertEquals(2, result.widgetCount)
        assertEquals(2, result.knownIds)
        assertEquals(AccentOption.LIME, result.accent)
        assertTrue(SetupShareRepository.hasImported(context))
        assertEquals(AccentOption.LIME, com.example.data.prefs.PreferencesManager.getInstance(context).getSavedAccent())
        val reconfigured = SetupShareRepository.importedConfig(context, "clock.dotdigital.m")
        assertNotNull(reconfigured)
        assertEquals("AMBER", reconfigured!!.optString("accent"))
        assertEquals(80, reconfigured.optInt("bg_alpha"))
    }

    @Test
    fun `setup import rejects a malformed code`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { SetupShareRepository.import(context, "not-a-setup-code") }
        }
    }

    @Test
    fun `community feed parses bundled asset and references real widgets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val picks = CommunityRepository.picks(context)
        assertTrue("community feed should ship several picks", picks.size >= 6)
        picks.forEach { pick ->
            assertTrue("pick ${pick.id} needs a title", pick.title.isNotBlank())
            assertTrue("pick ${pick.id} needs widgets", pick.widgetIds.isNotEmpty())
            pick.widgetIds.forEach { id ->
                assertNotNull("pick ${pick.id} references unknown widget $id", WidgetRegistry.getById(id))
            }
        }
    }

    @Test
    fun `lock-safe widgets are registered and useful`() {
        val lockSafe = WidgetRegistry.lockSafeDefs()
        assertTrue(lockSafe.size >= 3)
        val ids = lockSafe.map { it.id }
        assertTrue(ids.contains("clock.dotdigital.m"))
        assertTrue(ids.contains("calendar.day.date"))
        assertTrue(ids.contains("battery.ring"))
        // Lock widgets must not require a network or runtime permission to be glanceable.
        assertTrue(lockSafe.all { !it.requiresNetwork && it.requiredPermission == null })
    }
}
