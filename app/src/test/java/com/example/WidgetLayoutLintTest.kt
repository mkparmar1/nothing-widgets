package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.render.WidgetLayoutRecorder
import com.example.core.render.RenderKit
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.demo.DemoData
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetLayoutLintTest {

    private val baselineAllowlist = mapOf(
        "battery.combo.devices" to "L permission prompt is ellipsized and sub-9sp; replace with a designed permission state.",
        "battery.temp.gauge" to "Status label crosses the large numeric text box; reserve a caption band.",
        "battery.fuel.gauge" to "Battery value overlaps its supporting caption; reserve a label band.",
        "calendar.birthdays" to "Small dot title escapes the widget bounds; fit or shorten the title.",
        "calendar.countdown.date" to "Countdown caption touches the dot number; separate the vertical bands.",
        "calendar.countdown.multi" to "Countdown labels collide with their dot numbers; separate the vertical bands.",
        "calendar.day.date" to "Month caption intersects the date number; reserve a header band.",
        "calendar.india.tithi" to "Long tithi label is truncated; use a short label or a second line.",
        "calendar.quarter.progress" to "Quarter caption intersects the progress number; separate content bands.",
        "clock.dual.meridiem" to "AM/PM labels overlap the indicator; reposition the labels and indicator.",
        "clock.hijri" to "Alternate date text overlaps the clock block; reserve a date band.",
        "clock.iso.dots" to "Week label overlaps the dot number; separate the label and number.",
        "clock.moon.time" to "Moon caption intersects the clock block; separate the caption band.",
        "health.meditation" to "Long action hint is truncated; replace with concise actionable copy.",
        "health.steps.ring" to "Permission action is truncated; use the designed permission state.",
        "home.device.toggle" to "Connection hint is truncated; use concise actionable copy.",
        "home.door.status" to "Connection hint is truncated; use concise actionable copy.",
        "home.humidity" to "Connection hint is truncated; use concise actionable copy.",
        "home.lights.count" to "Connection hint is truncated; use concise actionable copy.",
        "home.presence" to "Connection hint is truncated; use concise actionable copy.",
        "home.scene.button" to "Scene label collides with content and the connection hint is truncated.",
        "home.sensor.temp" to "Connection hint is truncated; use concise actionable copy.",
        "learn.flashcard.daily" to "Answer text collides with the large word; reserve a response band.",
        "prod.project.burndown" to "Progress caption intersects the dot-number block; move the caption below it.",
        "prod.smart.stack" to "Stack title and empty hint are truncated; shorten the title and use an empty state.",
        "sensor.battery.quad" to "Narrow battery permission layout uses sub-9sp text and truncates its hint.",
        "sensor.compass.heading" to "Unsupported-sensor message is truncated; show an icon and concise hint.",
        "sensor.cpu.load" to "CPU value and caption collide; allocate a separate label band.",
        "sensor.fitness.rings" to "Permission text is truncated; use the designed permission state.",
        "sensor.lunar.moon" to "Moon captions are truncated; pair the icon with compact labels.",
        "sports.table.mini" to "Unconfigured league content lacks an illustrated actionable empty state.",
        "travel.weather.city" to "Weather condition caption overlaps the icon; reserve a caption band.",
        "weather.aqi.dial" to "AQI number and explanatory band overlap; separate value and status.",
        "weather.feels.humidity" to "Feels-like label and dot value overlap; reserve a header band.",
        "weather.hourly.graph" to "Graph labels fall below the readable 9sp minimum.",
        "weather.moon.phase" to "Three short captions are ellipsized; use a single icon and compact summary.",
        "weather.pressure.gauge" to "Pressure value and supporting label overlap; allocate separate bands.",
        "weather.wind.compass" to "Wind value and unit overlap; use a dedicated unit baseline.",
        "fun.dice.roll" to "Long top label enters the corner-dot reserve.",
        "clock.radial.24" to "Supporting caption intersects the ring geometry.",
        "calendar.life.dots" to "Completion caption overlays the dot matrix.",
        "battery.health.percent" to "Battery health text exceeds the small widget safe width.",
        "battery.ring" to "Percentage digits overlap the ring dots at S.",
        "battery.percentage" to "Percentage digits overlap the ring dots at S.",
        "battery.minimal.pill" to "Percentage digits overlap the ring dots at S.",
        "weather.city.london" to "Condition and high/low text encroaches on the temperature hero.",
        "weather.city.tokyo" to "Condition and high/low text encroaches on the temperature hero."
    )

    private val baselineIssueAllowlist = mapOf(
        "battery.combo.devices" to setOf("below-9sp", "truncated"),
        "battery.fuel.gauge" to setOf("text-overlap"),
        "battery.temp.gauge" to setOf("text-overlap"),
        "calendar.birthdays" to setOf("out-of-bounds"),
        "calendar.countdown.date" to setOf("text-overlap"),
        "calendar.countdown.multi" to setOf("text-overlap"),
        "calendar.day.date" to setOf("text-overlap"),
        "calendar.india.tithi" to setOf("truncated"),
        "calendar.quarter.progress" to setOf("text-overlap"),
        "clock.dual.meridiem" to setOf("text-overlap"),
        "clock.hijri" to setOf("text-overlap"),
        "clock.iso.dots" to setOf("text-overlap"),
        "clock.moon.time" to setOf("text-overlap"),
        "health.meditation" to setOf("truncated"),
        "home.device.toggle" to setOf("truncated"),
        "home.door.status" to setOf("truncated"),
        "home.humidity" to setOf("truncated"),
        "home.lights.count" to setOf("truncated"),
        "home.presence" to setOf("truncated"),
        "home.scene.button" to setOf("text-overlap", "truncated"),
        "home.sensor.temp" to setOf("truncated"),
        "learn.flashcard.daily" to setOf("text-overlap"),
        "prod.project.burndown" to setOf("text-overlap"),
        "prod.smart.stack" to setOf("truncated"),
        "sensor.battery.quad" to setOf("below-9sp", "truncated"),
        "sensor.compass.heading" to setOf("truncated"),
        "sensor.cpu.load" to setOf("text-overlap"),
        "sensor.lunar.moon" to setOf("truncated"),
        "sports.table.mini" to setOf("text-overlap"),
        "travel.weather.city" to setOf("text-overlap"),
        "weather.aqi.dial" to setOf("text-overlap"),
        "weather.city.london" to setOf("text-overlap"),
        "weather.feels.humidity" to setOf("text-overlap"),
        "weather.hourly.graph" to setOf("below-9sp"),
        "weather.moon.phase" to setOf("truncated"),
        "weather.pressure.gauge" to setOf("text-overlap"),
        "weather.city.tokyo" to setOf("text-overlap"),
        "weather.wind.compass" to setOf("text-overlap")
    )

    @Test
    fun `layout recorder audits every widget size and theme`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recordedWidgets = mutableSetOf<String>()
        val findings = linkedMapOf<String, MutableSet<String>>()
        val diagnostics = linkedMapOf<String, MutableList<String>>()

        DemoData.withSnapshot {
            WidgetRegistry.getAll().forEach { definition ->
                definition.sizes.forEach { size ->
                    for (isDark in listOf(true, false)) {
                        val dimensions = SizeResolver.resolveDimensions(context, size)
                        val spec = WidgetSpec(
                            widthPx = dimensions.widthPx,
                            heightPx = dimensions.heightPx,
                            density = dimensions.density,
                            sizeBucket = size,
                            accent = AccentOption.RED,
                            bgMode = BackgroundMode.SOLID,
                            themeMode = if (isDark) ThemeMode.FORCE_DARK else ThemeMode.FORCE_LIGHT,
                            isDark = isDark,
                            cornerRadiusDp = 20,
                            showLabel = true,
                            tapAction = definition.defaultTap
                        )
                        val capture = WidgetLayoutRecorder.begin(dimensions.density)
                        val bitmap = Bitmap.createBitmap(
                            dimensions.widthPx,
                            dimensions.heightPx,
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            definition.render(context, spec, Canvas(bitmap))
                        } finally {
                            capture.close()
                            bitmap.recycle()
                        }

                        if (capture.blocks.isNotEmpty()) recordedWidgets += definition.id
                        capture.blocks.forEachIndexed { index, block ->
                            val kinds = findings.getOrPut(definition.id) { linkedSetOf() }
                            if (block.bounds.left < 0f || block.bounds.top < 0f ||
                                block.bounds.right > dimensions.widthPx || block.bounds.bottom > dimensions.heightPx
                            ) {
                                kinds += "out-of-bounds"
                                diagnostics.getOrPut(definition.id) { mutableListOf() } +=
                                    "${size.name}/${if (isDark) "dark" else "light"}: '${block.text}' outside bounds"
                            }
                            if (overlapsRoundedCorner(
                                    block.bounds,
                                    dimensions.widthPx.toFloat(),
                                    dimensions.heightPx.toFloat(),
                                    spec.cornerRadiusDp * dimensions.density
                                )
                            ) kinds += "corner-safe-area"
                            if (block.textSizePx / block.density < 9f) {
                                kinds += "below-9sp"
                                diagnostics.getOrPut(definition.id) { mutableListOf() } +=
                                    "${size.name}/${if (isDark) "dark" else "light"}: '${block.text}' is ${block.textSizePx / block.density}sp"
                            }
                            if (block.wasTruncated) {
                                kinds += "truncated"
                                diagnostics.getOrPut(definition.id) { mutableListOf() } +=
                                    "${size.name}/${if (isDark) "dark" else "light"}: '${block.text}' truncated"
                            }
                            capture.blocks.drop(index + 1).forEach { other ->
                                if (RectF.intersects(block.bounds, other.bounds)) {
                                    kinds += "text-overlap"
                                    diagnostics.getOrPut(definition.id) { mutableListOf() } +=
                                        "${size.name}/${if (isDark) "dark" else "light"}: '${block.text}' overlaps '${other.text}'"
                                }
                            }
                        }
                    }
                }
            }
        }

        val unrecorded = WidgetRegistry.getAll().map { it.id }.toSet() - recordedWidgets
        assertEquals(
            setOf(
                "battery.theme.level", "fun.theme.clock", "fun.theme.date", "fun.theme.greeting",
                "fun.theme.music", "prod.focus.timer.theme", "toggle.grid.2x2", "toggle.theme.buttons",
                "toggle.theme.levels", "toggle.theme.quick"
            ),
            unrecorded
        )
        val baseline = findings.filterValues { it.isNotEmpty() }
            .entries.sortedBy { it.key }
            .joinToString("\n") { (id, issues) -> "$id: ${issues.joinToString()}" }
        val report = buildString {
            appendLine("Recorded text blocks for ${recordedWidgets.size} of ${WidgetRegistry.getAll().size} widgets.")
            appendLine("Unrecorded widgets: ${unrecorded.sorted()}")
            appendLine("Widget layout findings:")
            appendLine(baseline)
            appendLine("Finding details:")
            diagnostics.forEach { (id, details) -> appendLine("$id: ${details.distinct().take(6).joinToString("; ")}") }
            appendLine("Known visual findings not fully detectable from text geometry:")
            baselineAllowlist.filterKeys { it !in findings }.forEach { (id, reason) ->
                appendLine("$id: $reason")
            }
        }
        File("build/widget-layout-lint.txt").writeText(report)
        val unexpected = findings.flatMap { (id, issues) ->
            issues.filterNot { it in baselineIssueAllowlist[id].orEmpty() }.map { "$id: $it" }
        }
        assertTrue("Unreviewed layout findings:\n${unexpected.joinToString("\n")}", unexpected.isEmpty())
    }

    @Test
    fun `shared fitting shortens dot labels and reserves the corner dot`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val definition = WidgetRegistry.getAll().first()
        val dimensions = SizeResolver.resolveDimensions(context, definition.sizes.first())
        val spec = WidgetSpec(
            widthPx = dimensions.widthPx,
            heightPx = dimensions.heightPx,
            density = dimensions.density,
            sizeBucket = definition.sizes.first(),
            accent = AccentOption.RED,
            bgMode = BackgroundMode.SOLID,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = definition.defaultTap
        )
        val bitmap = Bitmap.createBitmap(dimensions.widthPx, dimensions.heightPx, Bitmap.Config.ARGB_8888)
        val capture = WidgetLayoutRecorder.begin(dimensions.density)
        try {
            val kit = RenderKit(spec, Canvas(bitmap))
            kit.dotTextCentered(
                "UNRECOGNIZABLE", kit.height * 0.4f, 28f * kit.d, 10f * kit.d,
                maxWidth = 20f * kit.d, shortText = "AB", fallbackText = "?"
            )
            kit.dotTextCentered(
                "UNRECOGNIZABLE", kit.height * 0.6f, 28f * kit.d, 10f * kit.d,
                maxWidth = 40f * kit.d, shortText = "AB", fallbackText = "?"
            )
            kit.header("A VERY LONG WIDGET HEADER THAT MUST AVOID THE CORNER INDICATOR", widthFraction = 0.99f)
        } finally {
            capture.close()
            bitmap.recycle()
        }

        assertEquals(listOf("?", "AB"), capture.blocks.take(2).map { it.text })
        val header = capture.blocks.last()
        assertTrue(header.bounds.right <= dimensions.widthPx - spec.widthPx * 0.08f - 18f * dimensions.density + 1f)
    }

    private fun overlapsRoundedCorner(bounds: RectF, width: Float, height: Float, radius: Float): Boolean {
        val corners = listOf(
            RectF(0f, 0f, radius, radius) to (radius to radius),
            RectF(width - radius, 0f, width, radius) to (width - radius to radius),
            RectF(0f, height - radius, radius, height) to (radius to height - radius),
            RectF(width - radius, height - radius, width, height) to (width - radius to height - radius)
        )
        return corners.any { (corner, center) ->
            val left = maxOf(bounds.left, corner.left)
            val top = maxOf(bounds.top, corner.top)
            val right = minOf(bounds.right, corner.right)
            val bottom = minOf(bounds.bottom, corner.bottom)
            if (left >= right || top >= bottom) return@any false
            val farthestX = maxOf(kotlin.math.abs(left - center.first), kotlin.math.abs(right - center.first))
            val farthestY = maxOf(kotlin.math.abs(top - center.second), kotlin.math.abs(bottom - center.second))
            farthestX * farthestX + farthestY * farthestY > radius * radius
        }
    }
}
