package com.example.widgets.styles

import android.content.Context
import android.graphics.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import com.example.core.astro.Astronomy
import com.example.data.india.IndiaCalendar
import com.example.widgets.base.WidgetSpec
import com.example.widgets.styles.kit.StyleKit
import com.example.widgets.styles.kit.StyleKit.Align
import com.example.widgets.styles.kit.StyleKit.Weight

/** Extra designs for the Indian festival and tithi widgets (same data as the classic ones). */
object IndiaStyles {

    val ALL: Map<String, List<WidgetStyle>> = mapOf(
        "calendar.india.festivals" to listOf(
            WidgetStyle("soft_festivals", "Festival Card", StyleFamily.SOFT) { c, s, cv -> renderFestivals(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_festivals", "Bold Festivals", StyleFamily.BOLD) { c, s, cv -> renderFestivals(c, s, cv, StyleFamily.BOLD) }
        ),
        "calendar.india.tithi" to listOf(
            WidgetStyle("soft_tithi", "Tithi Card", StyleFamily.SOFT) { c, s, cv -> renderTithi(c, s, cv, StyleFamily.SOFT) },
            WidgetStyle("bold_tithi", "Bold Tithi", StyleFamily.BOLD) { c, s, cv -> renderTithi(c, s, cv, StyleFamily.BOLD) }
        )
    )

    private fun away(days: Int) = when (days) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "$days days"
    }

    fun renderFestivals(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        k.title("Festivals")
        val list = IndiaCalendar.upcoming(if (k.isLarge) 5 else 2)
        if (list.isEmpty()) {
            k.text("No dates available", k.left, k.cy + 4f * k.u, 12f, k.sub)
            return
        }
        val top = k.contentTop + 2f * k.u
        val rowH = (k.bottom - top) / list.size
        list.forEachIndexed { i, f ->
            val y = top + rowH * i + rowH / 2f
            val icon = minOf(rowH * 0.5f, 18f * k.u)
            k.icon(Icons.Filled.Celebration, k.left + icon / 2f, y, icon, if (i == 0) k.accent else k.sub)
            val tx = k.left + icon + 5f * k.u
            val w = k.contentW * 0.58f
            k.text(f.name, tx, y - 1f * k.u, if (k.isLarge) 10f else 11.5f, k.ink, Weight.SEMIBOLD, maxWidth = w)
            // "≈" marks lunar dates that can shift a day by region.
            val date = StyleData.titleCase(IndiaCalendar.formatDate(f.date))
            k.text(if (f.approximate) "≈ $date" else date, tx, y + 12f * k.u, 9.5f, k.sub, Weight.MEDIUM, maxWidth = w)
            k.text(away(f.daysAway()), k.right, y + 4f * k.u, 11.5f, if (i == 0) k.accent else k.ink, Weight.SEMIBOLD, Align.RIGHT, k.contentW * 0.24f)
        }
    }

    fun renderTithi(context: Context, spec: WidgetSpec, canvas: Canvas, family: StyleFamily) {
        val k = StyleKit(context, spec, canvas, family)
        k.card()
        val t = IndiaCalendar.tithi()
        val moon = Astronomy.moonPhase()
        val lit = if (k.dark) 0xFFE8E8EA.toInt() else 0xFFFFFFFF.toInt()
        val shade = if (k.dark) 0xFF3A3A3C.toInt() else 0xFF8E8E93.toInt()
        val name = StyleData.titleCase(t.name)
        val paksha = "${StyleData.titleCase(t.paksha)} Paksha"
        if (k.isWide) {
            val r = (k.bottom - k.top) * 0.4f
            WeatherStyles.drawMoon(k, k.left + r, k.cy, r, moon.illumination, moon.isWaxing, lit, shade)
            val tx = k.left + 2 * r + 8f * k.u
            k.text(name, tx, k.cy + 1f * k.u, 22f, k.ink, Weight.SEMIBOLD, maxWidth = k.right - tx)
            k.text("$paksha · ≈", tx, k.cy + 19f * k.u, 11f, k.sub, Weight.MEDIUM, maxWidth = k.right - tx)
            return
        }
        val r = (k.bottom - k.top) * 0.25f
        WeatherStyles.drawMoon(k, k.cx, k.top + r + 2f * k.u, r, moon.illumination, moon.isWaxing, lit, shade)
        k.text(name, k.cx, k.bottom - 15f * k.u, 14f, k.ink, Weight.SEMIBOLD, Align.CENTER)
        k.text("$paksha · ≈", k.cx, k.bottom - 1f * k.u, 9.5f, k.sub, Weight.MEDIUM, Align.CENTER)
    }
}
