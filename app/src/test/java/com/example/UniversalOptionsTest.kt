package com.example

import com.example.core.config.UniversalOptions
import com.example.core.config.WidgetOptions
import com.example.data.looks.Looks
import com.example.widgets.base.WidgetRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UniversalOptionsTest {

    @Test
    fun `every widget has at least two options`() {
        val short = WidgetRegistry.getAll().filter { it.options.size < 2 }.map { it.id }
        assertTrue("Widgets with fewer than 2 options: $short", short.isEmpty())
    }

    @Test
    fun `text size and corner dot read back from config`() {
        val config = JSONObject().put(WidgetOptions.KEY, JSONObject().put(UniversalOptions.TEXT_SIZE, "LARGE").put(UniversalOptions.CORNER_DOT, false)).toString()
        val options = WidgetOptions.parse(config)
        assertEquals(1.2f, UniversalOptions.textScale(options), 0.001f)
        assertFalse(UniversalOptions.cornerDot(options))
        // Defaults when unset
        assertEquals(1f, UniversalOptions.textScale(WidgetOptions.EMPTY), 0.001f)
        assertTrue(UniversalOptions.cornerDot(WidgetOptions.EMPTY))
    }

    @Test
    fun `looks have unique ids`() {
        assertEquals(Looks.ALL.size, Looks.ALL.map { it.id }.toSet().size)
    }
}
