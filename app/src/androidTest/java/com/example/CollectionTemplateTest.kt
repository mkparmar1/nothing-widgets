package com.example

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import com.example.widgets.collection.CollectionViews
import com.example.widgets.collection.CollectionWidgets
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * List and stack widgets build their views on a real device. On Android 14+ a mutable row template
 * with an implicit intent throws when it is created, which broke Favourite Contacts and Week Agenda.
 */
@RunWith(AndroidJUnit4::class)
class CollectionTemplateTest {

    @Test
    fun everyCollectionWidgetBuildsItsViews() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val failures = mutableListOf<String>()
        val collections = WidgetRegistry.getAll().filter { CollectionWidgets.isCollection(it.id) }
        assertTrue("no collection widgets found", collections.isNotEmpty())
        collections.forEach { def ->
            val size = def.sizes.first()
            val dims = SizeResolver.resolveDimensions(context, size)
            val spec = WidgetSpec(
                widthPx = dims.widthPx, heightPx = dims.heightPx, density = dims.density, sizeBucket = size,
                accent = AccentOption.RED, bgMode = BackgroundMode.SOLID, themeMode = ThemeMode.FORCE_DARK,
                isDark = true, cornerRadiusDp = 20, showLabel = true, tapAction = TapAction.DEFAULT
            )
            val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
            try {
                CollectionViews.build(context, 4242, def.id, spec, bitmap)
            } catch (t: Throwable) {
                failures += "${def.id}: ${t.javaClass.simpleName}: ${t.message}"
            }
        }
        assertTrue("Collection widgets that failed to build:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
