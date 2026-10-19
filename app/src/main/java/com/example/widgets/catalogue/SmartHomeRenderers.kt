package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.home.HomeAssistantRepository
import com.example.data.home.HomeEntity
import com.example.widgets.base.WidgetSpec
import java.util.Locale

/**
 * Phase 7 Smart Home family. Binds to a Home Assistant server when configured and always renders
 * the last-known cached state; when unconfigured it shows a single clean "connect" prompt rather
 * than pretending to know anything about the home.
 */
object SmartHomeRenderers {

    private fun connectPrompt(kit: RenderKit, title: String) {
        kit.header(title)
        kit.labelCentered("CONNECT HOME ASSISTANT IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    private fun entities(context: Context, spec: WidgetSpec): List<HomeEntity> {
        val ids = spec.options.string("entities", "").split(",").map { it.trim() }.filter { it.isNotBlank() }
        HomeAssistantRepository.refresh(context, ids)
        return HomeAssistantRepository.entities(context, ids)
    }

    // ---------------------------------------------------------------- device toggle

    fun renderDeviceToggle(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val title = spec.options.string("label", "DEVICE").uppercase()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, title); return
        }
        val id = spec.options.string("entity", "switch.lamp")
        val e = HomeAssistantRepository.entity(context, id)
        kit.header(title)
        if (e == null) {
            kit.labelCentered("WAITING FOR STATE", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val on = e.isOn
        val cx = kit.width / 2f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width, kit.height) * 0.26f
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 20,
            filledDots = if (on) 20 else 0, dotRadius = radius * 0.15f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor
        )
        kit.labelCentered(if (on) "ON" else "OFF", cy + kit.textPaint(kit.onColor, 11f, 15f).textSize / 3f, kit.textPaint(if (on) kit.accentColor else kit.greyColor, 12f, 16f))
    }

    // ---------------------------------------------------------------- room status grid

    fun renderRoomStatus(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "ROOMS"); return
        }
        val list = entities(context, spec)
        kit.header("ROOM STATUS")
        if (list.isEmpty()) {
            kit.labelCentered("WAITING FOR STATES", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val cols = if (kit.maxW > kit.height) 2 else 1
        val rows = ((list.size + cols - 1) / cols).coerceAtLeast(1)
        val top = kit.pad + 24f * kit.d
        val cellW = kit.maxW / cols
        val cellH = (kit.height - top - kit.pad) / rows
        list.take(cols * rows).forEachIndexed { i, e ->
            val x = kit.pad + (i % cols) * cellW
            val y = top + (i / cols) * cellH + cellH * 0.6f
            val on = e.isOn
            canvas.drawCircle(x + 3f * kit.d, y - 3f * kit.d, 3f * kit.d, kit.fill(if (on) kit.accentColor else kit.offColor))
            val nameW = cellW - 14f * kit.d
            DotMatrixRenderer.drawFittedLabel(canvas, e.friendlyName.uppercase(), x + 10f * kit.d, y, nameW, kit.textPaint(kit.onColor, 10f, 13f))
        }
    }

    fun renderLightsCount(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "LIGHTS"); return
        }
        val list = entities(context, spec)
        kit.header(spec.options.string("label", "LIGHTS").uppercase())
        val on = list.count { it.isOn }
        kit.dotTextCentered("$on", kit.height * 0.50f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (on > 0) setOf(0) else emptySet())
        kit.labelCentered("OF ${list.size} ON", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    // ---------------------------------------------------------------- thermostat

    fun renderThermostat(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "THERMOSTAT"); return
        }
        val id = spec.options.string("entity", "climate.thermostat")
        val unit = spec.options.string("unit", "°C")
        val e = HomeAssistantRepository.entity(context, id)
        kit.header("THERMOSTAT")
        val temp = e?.numeric
        if (temp == null) {
            val display = e?.state?.uppercase() ?: "WAITING FOR STATE"
            kit.labelCentered(display, kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val cx = kit.width / 2f
        val cy = kit.height * 0.52f
        val radius = minOf(kit.width, kit.height) * 0.26f
        // Dial: major ticks every 6 dots; filled arc roughly tracks the 10..30° range.
        val frac = ((temp - 10.0) / 20.0).coerceIn(0.0, 1.0)
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 24,
            filledDots = (frac * 24).toInt(), dotRadius = radius * 0.13f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor
        )
        val paint = kit.textPaint(kit.onColor, 13f, 18f)
        val str = String.format(Locale.US, "%.1f%s", temp, unit)
        canvas.drawText(str, cx - paint.measureText(str) / 2f, cy + paint.textSize / 3f, paint)
    }

    fun renderSceneButton(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val name = spec.options.string("scene", "MOVIE NIGHT").uppercase()
        kit.header("SCENE")
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "SCENE"); return
        }
        val cx = kit.width / 2f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width, kit.height) * 0.24f
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 16,
            filledDots = 0, dotRadius = radius * 0.16f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor
        )
        kit.labelCentered(name, cy + kit.textPaint(kit.greyColor, 10f, 13f).textSize / 3f, kit.textPaint(kit.accentColor, 11f, 15f))
    }

    fun renderEnergyToday(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "ENERGY"); return
        }
        val id = spec.options.string("entity", "sensor.energy_today")
        val currency = spec.options.string("currency", "kWh")
        val e = HomeAssistantRepository.entity(context, id)
        kit.header("ENERGY TODAY")
        val value = e?.numeric
        if (value == null) {
            kit.labelCentered("WAITING FOR STATE", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        kit.dotTextCentered(String.format(Locale.US, "%.1f", value), kit.height * 0.52f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered(currency, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderSensorTemp(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "TEMP"); return
        }
        val id = spec.options.string("entity", "sensor.room_temperature")
        val e = HomeAssistantRepository.entity(context, id)
        kit.header(e?.friendlyName?.uppercase() ?: "ROOM TEMP")
        val value = e?.numeric
        if (value == null) {
            kit.labelCentered("WAITING FOR STATE", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        kit.dotTextCentered(String.format(Locale.US, "%.1f", value), kit.height * 0.54f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = setOf(1))
        kit.labelCentered(e.unit.ifBlank { "°C" }, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }

    fun renderHumidity(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "HUMIDITY"); return
        }
        val id = spec.options.string("entity", "sensor.humidity")
        val e = HomeAssistantRepository.entity(context, id)
        kit.header("HUMIDITY")
        val value = e?.numeric
        if (value == null) {
            kit.labelCentered("WAITING FOR STATE", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        kit.dotTextCentered("${value.toInt()}%", kit.height * 0.50f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = setOf(2))
        val frac = (value / 100.0).coerceIn(0.0, 1.0)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = (frac * 20).toInt(), max = 20, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 4f * kit.d,
            dotRadius = 2.4f * kit.d, dotGap = kit.maxW / 19f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderDoorStatus(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "DOORS"); return
        }
        val list = entities(context, spec)
        kit.header("DOORS & WINDOWS")
        if (list.isEmpty()) {
            kit.labelCentered("WAITING FOR STATES", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val open = list.count { it.isOn }
        kit.dotTextCentered("$open", kit.height * 0.42f, kit.height * 0.34f, 11f * kit.d,
            accentIndices = if (open > 0) setOf(0) else emptySet())
        kit.labelCentered(if (open == 0) "ALL CLOSED" else "OPEN NOW", kit.height * 0.74f, kit.textPaint(if (open == 0) kit.greyColor else kit.accentColor, 10f, 13f))
        val rowY = kit.height - kit.pad - 4f * kit.d
        val g = kit.maxW / list.size.coerceAtLeast(1)
        list.take(8).forEachIndexed { i, e ->
            val cx = kit.pad + i * g + g / 2f
            canvas.drawCircle(cx, rowY, 2.6f * kit.d, kit.fill(if (e.isOn) kit.accentColor else kit.offColor))
        }
    }

    fun renderPresence(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        if (!HomeAssistantRepository.isConfigured(context)) {
            connectPrompt(kit, "PRESENCE"); return
        }
        val list = entities(context, spec)
        kit.header("PRESENCE")
        if (list.isEmpty()) {
            kit.labelCentered("WAITING FOR STATES", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 14f))
            return
        }
        val home = list.count { it.isOn }
        kit.dotTextCentered("$home", kit.height * 0.50f, kit.height * 0.40f, 12f * kit.d,
            accentIndices = if (home > 0) setOf(0) else emptySet())
        kit.labelCentered("$home OF ${list.size} HOME", kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
    }
}
