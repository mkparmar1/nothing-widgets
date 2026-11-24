package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.core.time.ClockSupport
import com.example.core.time.TimeFormat
import com.example.widgets.base.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClockSupportTest {

    @Test
    fun `every clock offers formats, with seconds only where they tick live`() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
        val clocks = WidgetRegistry.getAll().filter { ClockSupport.isClockLike(it.id) }
        assertTrue(clocks.isNotEmpty())
        clocks.forEach { definition ->
            val expected = if (definition.id in ClockSupport.SECONDS_CAPABLE) TimeFormat.entries
                else TimeFormat.entries.filterNot { it.hasSeconds }
            assertEquals(expected, ClockSupport.timeFormats(definition.id))
            assertTrue(ClockSupport.showsDate(definition.id))
        }
    }

    @Test
    fun `every clock either renders formats itself or has a caption`() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
        val selfRendering = setOf(
            "clock.dotdigital.s", "clock.dotdigital.m", "clock.dotdigital.l",
            "clock.world.london", "clock.world.tokyo", "clock.world.nyc", "clock.world.custom",
            "clock.seconds.live", "clock.glyph.ring"
        )
        WidgetRegistry.getAll()
            .filter { ClockSupport.isClockLike(it.id) }
            .forEach { definition ->
                assertTrue(
                    "${definition.id} must render formats or have a caption",
                    definition.id in selfRendering || definition.id in ClockSupport.CAPTION_WIDGETS
                )
            }
    }

    @Test
    fun `non clock widgets have no clock formats`() {
        assertFalse(ClockSupport.isClockLike("battery.percentage"))
        assertEquals(emptyList<TimeFormat>(), ClockSupport.timeFormats("battery.percentage"))
        assertFalse(ClockSupport.showsDate("battery.percentage"))
    }
}
