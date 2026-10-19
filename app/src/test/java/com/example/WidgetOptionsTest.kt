package com.example

import com.example.core.config.WidgetOption
import com.example.core.config.WidgetOptions
import com.example.core.render.ColorRule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Phase 8: the advanced-option schema must round-trip and never corrupt unrelated config. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetOptionsTest {

    @Test
    fun `write then parse round-trips every option type`() {
        val original = JSONObject().apply { put("accent", "AMBER") }.toString()
        val written = WidgetOptions.write(
            original,
            mapOf(
                "goal" to 12000,
                "show_pct" to false,
                "unit" to "km",
                "variant" to "bar"
            )
        )

        // Core config keys are preserved untouched.
        assertEquals("AMBER", JSONObject(written).optString("accent"))

        val parsed = WidgetOptions.parse(written)
        assertEquals(12000, parsed.int("goal"))
        assertFalse(parsed.boolean("show_pct", true))
        assertEquals("km", parsed.string("unit"))
        assertEquals("bar", parsed.string("variant"))
    }

    @Test
    fun `parse of missing or malformed config yields defaults not crashes`() {
        val empty = WidgetOptions.parse("{}")
        assertEquals(7, empty.int("missing", 7))
        assertTrue(empty.boolean("missing", true))
        assertEquals("d", empty.string("missing", "d"))
        // Garbage json must not throw.
        assertEquals(3, WidgetOptions.parse("not json").int("x", 3))
    }

    @Test
    fun `write preserves unknown future option keys`() {
        val withFuture = WidgetOptions.write(null, mapOf("future_opt" to 999, "known" to "a"))
        val updated = WidgetOptions.write(withFuture, mapOf("known" to "b"))
        val parsed = WidgetOptions.parse(updated)
        assertEquals("b", parsed.string("known"))
        assertEquals(999, parsed.int("future_opt"))
    }

    @Test
    fun `removing an option deletes only that key`() {
        val written = WidgetOptions.write(null, mapOf("a" to 1, "b" to 2))
        val removed = WidgetOptions.write(written, mapOf("a" to null))
        val parsed = WidgetOptions.parse(removed)
        assertEquals(0, parsed.int("a", 0)) // gone -> default
        assertEquals(2, parsed.int("b"))
    }

    @Test
    fun `choice schema reports its values`() {
        val choice = WidgetOption.Choice.of("layout", "Layout", listOf("ring", "bar"), "ring")
        assertEquals(listOf("ring" to "ring", "bar" to "bar"), choice.options)
        assertEquals("ring", choice.default)
    }

    @Test
    fun `color rule paints warning below the ratio and accent otherwise`() {
        val accent = 0xFF00C2D1.toInt()
        // Battery at 15% is below a 20% warning ratio -> warn colour.
        assertEquals(ColorRule.WARN, ColorRule.paint(15, 100, 0.2f, accent))
        // Battery at 60% is fine -> accent.
        assertEquals(accent, ColorRule.paint(60, 100, 0.2f, accent))
        // Disabled rule always uses accent.
        assertEquals(accent, ColorRule.paint(15, 100, 0.2f, accent, enabled = false))
        // A "warning when exceeding" rule (spend over budget) fires above the ratio.
        assertEquals(ColorRule.WARN, ColorRule.paint(120, 100, 1f, accent, belowGoal = false))
    }
}
