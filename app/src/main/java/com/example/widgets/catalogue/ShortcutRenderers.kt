package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.GlyphTable
import com.example.core.render.RenderKit
import com.example.data.shortcuts.Shortcut
import com.example.data.shortcuts.ShortcutStore
import com.example.widgets.base.WidgetSpec

/** Contact, app and web shortcut widgets. Each slot is its own tap target (see [grid]). */
object ShortcutRenderers {

    /** Rows x columns of slots for a shortcut widget at a given size. */
    fun grid(widgetId: String, size: SizeBucket): Pair<Int, Int> {
        val count = ShortcutStore.WIDGETS[widgetId]?.count ?: 1
        return when {
            count == 1 -> 1 to 1
            count == 2 -> 1 to 2
            count == 4 && size == SizeBucket.L -> 2 to 2
            count == 4 -> 1 to 4
            else -> 2 to 3
        }
    }

    fun render(context: Context, spec: WidgetSpec, canvas: Canvas, widgetId: String) {
        val kit = RenderKit(spec, canvas)
        val d = kit.d
        kit.background()
        val slots = ShortcutStore.get(context, widgetId)
        val kind = ShortcutStore.WIDGETS[widgetId]?.kind ?: Shortcut.Kind.CALL
        kit.header(
            when (widgetId) {
                "shortcut.direct.dial" -> "SPEED DIAL"
                "shortcut.direct.sms" -> "MESSAGE"
                "shortcut.web.favorite" -> "BOOKMARK"
                else -> if (kind == Shortcut.Kind.APP) "APPS" else "FAVORITES"
            }
        )

        if (slots.all { it == null }) {
            val what = when (kind) {
                Shortcut.Kind.APP -> "PICK APPS IN THE APP"
                Shortcut.Kind.WEB -> "ADD A LINK IN THE APP"
                else -> "PICK A CONTACT IN THE APP"
            }
            kit.labelCentered(what, kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }

        val (rows, cols) = grid(widgetId, spec.sizeBucket)
        val top = kit.pad + 22f * d
        val cellW = kit.maxW / cols
        val cellH = (kit.height - top - kit.pad * 0.6f) / rows
        slots.forEachIndexed { i, slot ->
            val left = kit.pad + (i % cols) * cellW
            val cellTop = top + (i / cols) * cellH
            drawSlot(context, kit, slot, kind, left, cellTop, cellW, cellH, single = slots.size == 1)
        }
    }

    private fun drawSlot(
        context: Context, kit: RenderKit, slot: Shortcut?, kind: Shortcut.Kind,
        left: Float, top: Float, w: Float, h: Float, single: Boolean
    ) {
        val d = kit.d
        val cx = left + w / 2f
        val iconSize = minOf(w * 0.62f, h * 0.58f)
        val iconCy = top + h * 0.42f
        val labelPaint = kit.textPaint(if (slot == null) kit.greyColor else kit.onColor, if (single) 12f else 9.5f, if (single) 16f else 12f)

        if (slot == null) {
            // Empty slot: a ghost "+" made of dots
            for (k in -2..2) {
                kit.canvas.drawCircle(cx + k * iconSize * 0.12f, iconCy, iconSize * 0.04f, kit.fill(kit.offColor))
                kit.canvas.drawCircle(cx, iconCy + k * iconSize * 0.12f, iconSize * 0.04f, kit.fill(kit.offColor))
            }
            kit.labelCentered("ADD", top + h * 0.88f, labelPaint, w * 0.9f)
            return
        }

        val appIcon = if (slot.kind == Shortcut.Kind.APP) {
            runCatching { context.packageManager.getApplicationIcon(slot.value).toBitmap(20, 20) }.getOrNull()
        } else null

        if (appIcon != null) {
            drawColorDots(kit, appIcon, cx - iconSize / 2f, iconCy - iconSize / 2f, iconSize)
        } else {
            val text = when (slot.kind) {
                Shortcut.Kind.CALL -> if (slot.label.equals("EMERGENCY", ignoreCase = true)) "SOS" else initials(slot.label)
                Shortcut.Kind.MESSAGE -> initials(slot.label)
                Shortcut.Kind.WEB -> initials(Uri.parse(if (slot.value.contains("://")) slot.value else "https://${slot.value}").host?.removePrefix("www.") ?: slot.label)
                Shortcut.Kind.APP -> initials(slot.label)
            }
            val gap = DotMatrixRenderer.fitDotGap(text.length, iconSize, iconSize * 0.8f, 7f * d)
            val textW = DotMatrixRenderer.measureTextWidth(text.length, gap)
            DotMatrixRenderer.drawText(
                canvas = kit.canvas,
                text = text,
                startX = cx - textW / 2f,
                startY = iconCy - (GlyphTable.HEIGHT - 1) * gap / 2f,
                dotRadius = gap * 0.38f,
                dotGap = gap,
                onColor = kit.onColor,
                offColor = kit.offColor
            )
        }

        kit.labelCentered(slot.label.uppercase(), top + h * 0.88f, labelPaint, w * 0.92f)
        if (single) {
            val action = when (slot.kind) {
                Shortcut.Kind.CALL -> "TAP TO CALL ${slot.value}"
                Shortcut.Kind.MESSAGE -> "TAP TO MESSAGE"
                Shortcut.Kind.WEB -> "TAP TO OPEN"
                Shortcut.Kind.APP -> "TAP TO OPEN"
            }
            kit.labelCentered(action, top + h * 0.88f + 16f * d, kit.textPaint(kit.accentColor, 9.5f, 12f), w * 0.92f)
        }
    }

    /** App icon sampled into coloured dots; transparent pixels are skipped. */
    private fun drawColorDots(kit: RenderKit, icon: Bitmap, left: Float, top: Float, size: Float) {
        val grid = icon.width
        val step = size / grid
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                val px = icon.getPixel(x, y)
                if (Color.alpha(px) < 80) continue
                paint.color = px or 0xFF000000.toInt()
                kit.canvas.drawCircle(left + step * (x + 0.5f), top + step * (y + 0.5f), step * 0.42f, paint)
            }
        }
    }

    private fun initials(name: String): String {
        val words = name.trim().uppercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val letters = when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}"
            words.isNotEmpty() -> words[0].take(2)
            else -> "?"
        }
        return letters.filter { it.isLetterOrDigit() }.ifEmpty { "#" }
    }
}
