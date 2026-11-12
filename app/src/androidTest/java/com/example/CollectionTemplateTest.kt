package com.example

import android.graphics.Bitmap
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.WidgetInstance
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import com.example.widgets.base.WidgetRenderEngine
import com.example.widgets.collection.CollectionViews
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.collection.StackTimeSlot
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

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

    @Test
    fun automaticStackBuildsRemoteViewsWithSelectedCardAction() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cards = listOf("clock.dotdigital.s", "battery.ring")
        val config = JSONObject()
            .put(CollectionWidgets.STACK_KEY, JSONArray(cards))
            .put(CollectionWidgets.STACK_MODE_KEY, "TIME_OF_DAY")
            .put(
                CollectionWidgets.STACK_TIME_SLOTS_KEY,
                JSONObject().put(StackTimeSlot.forHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)).name, cards[1])
            )
            .toString()
        val instance = WidgetInstance(
            appWidgetId = 4243,
            widgetTypeId = CollectionWidgets.SMART_STACK,
            configJson = config
        )

        try {
            val views = WidgetRenderEngine.buildRemoteViews(context, instance.appWidgetId, instance)
            assertNotNull(views)
            val parcel = Parcel.obtain()
            try {
                views.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                assertNotNull(android.widget.RemoteViews.CREATOR.createFromParcel(parcel))
            } finally {
                parcel.recycle()
            }
        } finally {
            context.getSharedPreferences("smart_stack_selection", android.content.Context.MODE_PRIVATE)
                .edit().remove("widget_${instance.appWidgetId}").apply()
        }
    }
}
