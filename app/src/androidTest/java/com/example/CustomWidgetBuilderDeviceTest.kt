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
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.CustomWidget
import com.example.data.db.AppDatabase
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import com.example.widgets.builder.BuilderRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomWidgetBuilderDeviceTest {
    @Test
    fun customDesignPersistsRegistersAndRendersAtEachSupportedSize() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = AppDatabase.getInstance(context).customWidgetDao()
        val id = "device-test-builder"
        val widget = CustomWidget(
            id = id,
            name = "Device Builder Test",
            sizesCsv = "S,M,L",
            layoutJson = BuilderLayout(
                listOf(
                    BuilderBlock(type = BuilderBlockType.TIME, x = 1, y = 1, width = 14, height = 5),
                    BuilderBlock(type = BuilderBlockType.DATE, x = 1, y = 6, width = 10, height = 2),
                    BuilderBlock(type = BuilderBlockType.BATTERY, x = 1, y = 9, width = 7, height = 3),
                    BuilderBlock(type = BuilderBlockType.TEXT, x = 1, y = 13, width = 14, height = 2, text = "ON DEVICE"),
                    BuilderBlock(type = BuilderBlockType.PROGRESS, x = 10, y = 8, width = 5, height = 5),
                    BuilderBlock(type = BuilderBlockType.DIVIDER, x = 1, y = 12, width = 14, height = 1)
                )
            ).toJson()
        )
        try {
            dao.upsert(widget)
            assertEquals(widget, dao.getById(id))
            WidgetRegistry.replaceCustom(dao.getAll())
            val definition = WidgetRegistry.getById(widget.widgetTypeId)
            assertNotNull("custom definition was not registered", definition)

            for (size in SizeBucket.entries) {
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
                    tapAction = TapAction.OPEN_MAIN
                )
                val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    definition!!.render(context, spec, Canvas(bitmap))
                } finally {
                    bitmap.recycle()
                }
            }
        } finally {
            dao.getById(id)?.let { dao.delete(it) }
            WidgetRegistry.replaceCustom(dao.getAll())
        }
    }
}
