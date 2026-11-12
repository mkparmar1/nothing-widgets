package com.example.widgets.builder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.activity.StepRepository
import com.example.data.battery.BatteryHelper
import com.example.data.builder.BuilderAlignment
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.BuilderTextColor
import com.example.data.builder.CustomWidget
import com.example.data.calendar.CalendarRepository
import com.example.data.photos.BackgroundImageStore
import com.example.data.weather.WeatherRepository
import com.example.data.weather.WeatherData
import com.example.ui.permissions.AppPermission
import com.example.widgets.base.Category
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object BuilderRenderer {
    fun definition(widget: CustomWidget): WidgetDefinition? {
        if (!widget.id.matches(Regex("[A-Za-z0-9-]{8,64}")) || widget.name.isBlank() || widget.name.length > 40 || widget.supportedSizes.isEmpty()) return null
        val layout = BuilderLayout.parse(widget.layoutJson) ?: return null
        val permissions = layout.blocks.mapNotNull {
            when (it.type) {
                BuilderBlockType.STEPS -> AppPermission.Activity
                BuilderBlockType.NEXT_EVENT -> AppPermission.Calendar
                BuilderBlockType.WEATHER -> AppPermission.Location
                else -> null
            }
        }.toSet()
        return object : WidgetDefinition {
            override val id = widget.widgetTypeId
            override val category = Category.CUSTOM
            override val title = widget.name
            override val description = "${layout.blocks.size} custom blocks"
            override val renderVersion = widget.layoutJson.hashCode()
            override val sizes = widget.supportedSizes
            override val defaultTap = TapAction.OPEN_MAIN
            override val requiredPermissions = permissions
            override fun render(context: Context, spec: WidgetSpec, canvas: Canvas) =
                render(context, spec, canvas, layout)
        }
    }

    fun render(context: Context, spec: WidgetSpec, canvas: Canvas, layout: BuilderLayout) {
        renderInternal(context, spec, canvas, layout, null)
    }

    internal fun renderWithWeather(context: Context, spec: WidgetSpec, canvas: Canvas, layout: BuilderLayout, weather: WeatherData) {
        renderInternal(context, spec, canvas, layout, weather)
    }

    private fun renderInternal(context: Context, spec: WidgetSpec, canvas: Canvas, layout: BuilderLayout, weather: WeatherData?) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val unitW = spec.widthPx / 16f
        val unitH = spec.heightPx / 16f
        for (block in layout.blocks) {
            val rect = RectF(
                block.x * unitW,
                block.y * unitH,
                ((block.x + block.width).coerceAtMost(16)) * unitW,
                ((block.y + block.height).coerceAtMost(16)) * unitH
            )
            if (rect.width() <= 0f || rect.height() <= 0f) continue
            drawBlock(context, canvas, kit, block, rect, weather)
        }
    }

    private fun drawBlock(context: Context, canvas: Canvas, kit: RenderKit, block: BuilderBlock, rect: RectF, weatherOverride: WeatherData?) {
        val color = when (block.color) {
            BuilderTextColor.PRIMARY -> kit.onColor
            BuilderTextColor.MUTED -> kit.greyColor
            BuilderTextColor.ACCENT -> kit.accentColor
        }
        when (block.type) {
            BuilderBlockType.DIVIDER -> {
                val paint = kit.fill(color)
                paint.alpha = 180
                canvas.drawRect(rect.left, rect.centerY() - 1f, rect.right, rect.centerY() + 1f, paint)
            }
            BuilderBlockType.SPACER -> Unit
            BuilderBlockType.IMAGE -> drawImage(context, canvas, block, rect)
            BuilderBlockType.WEATHER -> {
                val weather = weatherOverride ?: WeatherRepository.getWeatherInstant(context)
                val gap = (minOf(rect.height() / 8f, rect.width() / 12f)).coerceAtLeast(2f)
                DotMatrixRenderer.drawWeatherIcon(
                    canvas = canvas,
                    weatherCode = weather.weatherCode,
                    centerX = rect.left + rect.width() * 0.18f,
                    centerY = rect.centerY(),
                    dotGap = gap,
                    dotRadius = gap * 0.32f,
                    onColor = color,
                    offColor = kit.offColor,
                    accentColor = kit.accentColor
                )
                val summary = if (weather.isStale) "--°" else "${weather.temperatureC}° ${weather.description}"
                drawText(canvas, kit, summary, block, RectF(rect.left + rect.width() * 0.34f, rect.top, rect.right, rect.bottom), color, 0.72f)
            }
            BuilderBlockType.PROGRESS -> {
                val value = block.text.toIntOrNull()?.coerceIn(0, 100)
                    ?: BatteryHelper.getBatteryStatus(context).level
                val radius = (minOf(rect.width(), rect.height()) * 0.34f).coerceAtLeast(2f)
                DotMatrixRenderer.drawDotRing(
                    canvas, rect.centerX(), rect.centerY(), radius, 32, (value * 32 / 100),
                    (radius * 0.11f).coerceAtLeast(1.3f), kit.onColor, kit.offColor, kit.accentColor
                )
                drawText(canvas, kit, value.toString() + "%", block, rect, color, 0.48f)
            }
            else -> {
                val text = blockText(context, block)
                if (text.isNotBlank()) drawText(canvas, kit, text, block, rect, color, 0.72f)
            }
        }
    }

    private fun blockText(context: Context, block: BuilderBlock): String = when (block.type) {
        BuilderBlockType.TIME -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        BuilderBlockType.DATE -> runCatching {
            SimpleDateFormat(block.text.ifBlank { "EEE, d MMM" }, Locale.getDefault()).format(Calendar.getInstance().time)
        }.getOrElse { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Calendar.getInstance().time) }
        BuilderBlockType.BATTERY -> "${BatteryHelper.getBatteryStatus(context).level}%"
        BuilderBlockType.WEATHER -> ""
        BuilderBlockType.STEPS -> StepRepository.todaySteps(context)?.let { "$it STEPS" } ?: "STEPS"
        BuilderBlockType.NEXT_EVENT -> CalendarRepository.upcomingToday(context)?.firstOrNull()?.title ?: "NO UPCOMING EVENT"
        BuilderBlockType.TEXT -> block.text.ifBlank { "YOUR TEXT" }
        else -> ""
    }

    private fun drawText(
        canvas: Canvas,
        kit: RenderKit,
        text: String,
        block: BuilderBlock,
        rect: RectF,
        color: Int,
        verticalFraction: Float
    ) {
        val paint = kit.textPaint(color, 12f * block.scale, rect.height() * 0.72f)
        val width = rect.width() * 0.94f
        val measured = paint.measureText(text).coerceAtMost(width)
        val x = when (block.alignment) {
            BuilderAlignment.START -> rect.left + rect.width() * 0.03f
            BuilderAlignment.CENTER -> rect.centerX() - measured / 2f
            BuilderAlignment.END -> rect.right - measured - rect.width() * 0.03f
        }.coerceAtLeast(rect.left)
        val y = rect.centerY() + paint.textSize * verticalFraction * 0.35f
        canvas.save()
        canvas.clipRect(rect)
        DotMatrixRenderer.drawFittedLabel(canvas, text, x, y, width, paint)
        canvas.restore()
    }

    private fun drawImage(context: Context, canvas: Canvas, block: BuilderBlock, rect: RectF) {
        val image = block.imageId?.let { BackgroundImageStore.load(context, it) } ?: return
        val src = crop(image.width, image.height, rect.width(), rect.height())
        canvas.save()
        canvas.clipRect(rect)
        canvas.drawBitmap(image, src, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    private fun crop(width: Int, height: Int, destWidth: Float, destHeight: Float): android.graphics.Rect {
        val sourceRatio = width.toFloat() / height
        val destRatio = destWidth / destHeight
        return if (sourceRatio > destRatio) {
            val cropped = (height * destRatio).toInt().coerceAtLeast(1)
            android.graphics.Rect((width - cropped) / 2, 0, (width + cropped) / 2, height)
        } else {
            val cropped = (width / destRatio).toInt().coerceAtLeast(1)
            android.graphics.Rect(0, (height - cropped) / 2, width, (height + cropped) / 2)
        }
    }
}
