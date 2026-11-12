package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.weather.WeatherRepository
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WeatherWidgetsDeviceTest {

    @Test
    fun hourlyWidgetsRenderWithDeviceWeatherData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val weather = WeatherRepository.getWeather(context)
        val output = File(context.getExternalFilesDir(null), "p3-weather").apply { mkdirs() }
        val widgetIds = listOf(
            "weather.hourly.graph",
            "weather.hourly.strip",
            "weather.precipitation",
            "weather.precip.bar"
        )

        assertTrue("No hourly forecast data was returned", weather.hourly.isNotEmpty())
        widgetIds.forEach { id ->
            val definition = WidgetRegistry.getById(id)
            assertNotNull("$id should be registered", definition)
            definition!!
            listOf(SizeBucket.M, SizeBucket.L).filter { it in definition.sizes }.forEach { size ->
                val dims = SizeResolver.resolveDimensions(context, size)
                val spec = WidgetSpec(
                    widthPx = dims.widthPx,
                    heightPx = dims.heightPx,
                    density = dims.density,
                    sizeBucket = size,
                    accent = AccentOption.RED,
                    bgMode = BackgroundMode.SOLID,
                    themeMode = ThemeMode.FORCE_DARK,
                    isDark = true,
                    cornerRadiusDp = 20,
                    showLabel = true,
                    tapAction = TapAction.OPEN_WEATHER
                )
                val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    definition.render(context, spec, Canvas(bitmap))
                    val contentPixels = (0 until bitmap.height).sumOf { y ->
                        (0 until bitmap.width).count { x -> bitmap.getPixel(x, y) != bitmap.getPixel(0, 0) }
                    }
                    assertTrue("$id [$size] rendered no visible content", contentPixels > bitmap.width * bitmap.height / 100)
                    File(output, "${id.replace('.', '-')}-${size.name}.png").outputStream().use {
                        assertTrue("Failed to save $id [$size] snapshot", bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
