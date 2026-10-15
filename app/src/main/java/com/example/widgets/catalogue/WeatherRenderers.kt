package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.example.core.bitmap.SizeBucket
import com.example.core.render.DotMatrixRenderer
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.WidgetSpec

object WeatherRenderers {

    fun renderCurrentWeather(context: Context, spec: WidgetSpec, canvas: Canvas, cityNameOverride: String? = null) {
        val city = cityNameOverride ?: "London"
        val data = WeatherRepository.getWeatherInstant(context, city)

        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        // Stale guard: if data is stale >90 min, show dash "--" indicator
        val tempStr = if (data.isStale) "--°" else "${data.temperatureC}°"

        // Draw City name top left
        if (spec.showLabel) {
            val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (data.isStale) 0xFFE02B2B.toInt() else 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            val label = if (data.isStale) "${data.cityName.uppercase()} (STALE)" else data.cityName.uppercase()
            DotMatrixRenderer.drawFittedLabel(canvas, label, pad, pad + 12f * spec.density, maxW * 0.70f, paintLabel)
        }

        if (spec.sizeBucket == SizeBucket.S) {
            // S Size: Temperature left-aligned, Weather icon right-aligned (guaranteed non-overlapping)
            val iconRadius = 18f * spec.density
            val iconCx = spec.widthPx - pad - iconRadius
            val iconCy = spec.heightPx * 0.44f

            // Safe temperature width strictly bounded away from the icon
            val maxTempW = iconCx - iconRadius - pad - (10f * spec.density)
            val maxTempH = spec.heightPx * 0.36f
            val dotGap = DotMatrixRenderer.fitDotGap(tempStr.length, maxTempW, maxTempH, 8.5f * spec.density)
            val dotRadius = dotGap * 0.38f

            val startX = pad
            val startY = spec.heightPx * 0.35f

            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = tempStr,
                startX = startX,
                startY = startY,
                dotRadius = dotRadius,
                dotGap = dotGap,
                onColor = onColor,
                offColor = offColor,
                accentIndices = setOf(tempStr.length - 1),
                accentColor = accentColor
            )

            // Weather icon
            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = data.weatherCode,
                centerX = iconCx,
                centerY = iconCy,
                dotGap = 4.5f * spec.density,
                dotRadius = 1.8f * spec.density,
                onColor = onColor,
                offColor = offColor,
                accentColor = accentColor
            )

            // High / Low and Condition below with auto-fit and ellipsize
            val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(13f)
                isFakeBoldText = true
            }
            val subText = "${data.description.uppercase()} • H:${data.highC}° L:${data.lowC}°"
            val subY = spec.heightPx - pad - (8f * spec.density)
            DotMatrixRenderer.drawFittedLabel(canvas, subText, pad, subY, maxW, paintSub)

        } else {
            // M and L Sizes: Wide layout with temperature on left and hero icon on right
            val iconCx = spec.widthPx - pad - (60f * spec.density)
            val iconCy = spec.heightPx * 0.46f

            val maxTempW = spec.widthPx * 0.42f
            val maxTempH = spec.heightPx * 0.45f
            val dotGap = DotMatrixRenderer.fitDotGap(tempStr.length, maxTempW, maxTempH, 12.0f * spec.density)
            val dotRadius = dotGap * 0.38f

            val startX = pad
            val startY = spec.heightPx * 0.33f

            DotMatrixRenderer.drawText(
                canvas = canvas,
                text = tempStr,
                startX = startX,
                startY = startY,
                dotRadius = dotRadius,
                dotGap = dotGap,
                onColor = onColor,
                offColor = offColor,
                accentIndices = setOf(tempStr.length - 1),
                accentColor = accentColor
            )

            // Hero Weather icon
            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = data.weatherCode,
                centerX = iconCx,
                centerY = iconCy,
                dotGap = 6.2f * spec.density,
                dotRadius = 2.4f * spec.density,
                onColor = onColor,
                offColor = offColor,
                accentColor = accentColor
            )

            // High / Low and Condition below
            val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A8A8A.toInt()
                textSize = (11f * spec.density).coerceAtMost(15f)
                isFakeBoldText = true
            }
            val subText = "${data.description.uppercase()} • HIGH ${data.highC}° • LOW ${data.lowC}° • WIND ${data.windKmh}KM/H"
            val subY = spec.heightPx - pad - (8f * spec.density)
            DotMatrixRenderer.drawFittedLabel(canvas, subText, pad, subY, maxW, paintSub)
        }
    }

    fun renderForecast5Day(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "5-DAY FORECAST", pad, pad + 12f * spec.density, maxW, paintLabel)

        val days = listOf(
            Triple("TODAY", 19, 1),
            Triple("TUE", 22, 0),
            Triple("WED", 18, 61),
            Triple("THU", 21, 2),
            Triple("FRI", 24, 0)
        )

        val colW = maxW / 5f
        val startY = pad + (32f * spec.density)

        days.forEachIndexed { i, (day, temp, code) ->
            val cx = pad + (i * colW) + (colW / 2f)

            // Day label
            val paintDay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (i == 0) accentColor else 0xFF8A8A8A.toInt()
                textSize = (10f * spec.density).coerceAtMost(13f)
                isFakeBoldText = true
            }
            val dw = paintDay.measureText(day)
            canvas.drawText(day, cx - (dw / 2f), startY, paintDay)

            // Weather icon
            DotMatrixRenderer.drawWeatherIcon(
                canvas = canvas,
                weatherCode = code,
                centerX = cx,
                centerY = startY + 28f * spec.density,
                dotGap = 3.6f * spec.density,
                dotRadius = 1.3f * spec.density,
                onColor = onColor,
                offColor = offColor,
                accentColor = accentColor
            )

            // Temperature
            val paintTemp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = onColor
                textSize = (11f * spec.density).coerceAtMost(14f)
                isFakeBoldText = true
            }
            val tStr = "$temp°"
            val tw = paintTemp.measureText(tStr)
            canvas.drawText(tStr, cx - (tw / 2f), startY + 58f * spec.density, paintTemp)
        }
    }

    fun renderSunriseSunsetArc(spec: WidgetSpec, canvas: Canvas) {
        val onColor = if (spec.isDark) 0xFFFFFFFF.toInt() else 0xFF0A0A0A.toInt()
        val offColor = if (spec.isDark) 0x1EFFFFFF.toInt() else 0x1E0A0A0A.toInt()
        val accentColor = spec.accent.hex

        val pad = spec.widthPx * 0.08f
        val maxW = spec.widthPx - (2 * pad)

        DotMatrixRenderer.drawBackground(
            canvas = canvas,
            width = spec.widthPx.toFloat(),
            height = spec.heightPx.toFloat(),
            cornerRadius = spec.cornerRadiusDp * spec.density,
            bgMode = spec.bgMode,
            isDark = spec.isDark,
            accentColor = accentColor
        )

        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(14f)
            isFakeBoldText = true
        }
        DotMatrixRenderer.drawFittedLabel(canvas, "SUN TRACKER", pad, pad + 12f * spec.density, maxW, paintLabel)

        val cx = spec.widthPx / 2f
        val cy = spec.heightPx - pad - (20f * spec.density)
        val arcRadius = minOf(spec.widthPx * 0.34f, spec.heightPx * 0.45f)

        // Semicircular arc of dots
        val totalArcDots = 15
        val sunProgressIndex = 9 // afternoon

        val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = onColor }
        val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offColor }
        val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        for (i in 0 until totalArcDots) {
            val angleDeg = 180f + (i * (180f / (totalArcDots - 1)))
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = cx + (arcRadius * Math.cos(angleRad)).toFloat()
            val y = cy + (arcRadius * Math.sin(angleRad)).toFloat()

            if (i == sunProgressIndex) {
                // Sun position
                canvas.drawCircle(x, y, 6.0f * spec.density, paintAccent)
            } else if (i < sunProgressIndex) {
                canvas.drawCircle(x, y, 3.2f * spec.density, paintOn)
            } else {
                canvas.drawCircle(x, y, 2.4f * spec.density, paintOff)
            }
        }

        // Times
        val paintTime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8A8A.toInt()
            textSize = (10f * spec.density).coerceAtMost(13f)
            isFakeBoldText = true
        }
        canvas.drawText("06:42", pad, cy + 16f * spec.density, paintTime)
        val sunsetW = paintTime.measureText("18:15")
        canvas.drawText("18:15", spec.widthPx - pad - sunsetW, cy + 16f * spec.density, paintTime)
    }
}
