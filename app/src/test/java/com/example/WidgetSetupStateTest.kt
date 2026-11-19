package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.render.Frame
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.WidgetSetupState
import com.example.widgets.base.ShapePoint
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetShape
import com.example.widgets.collection.CollectionWidgets
import com.example.widgets.collection.StackMode
import com.example.widgets.collection.StackTimeSlot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSetupStateTest {

    @Test
    fun `setup state round trips through a configured widget instance`() {
        val definition = WidgetRegistry.getById("clock.analog.ring")!!
        val preferences = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val state = WidgetSetupState(
            size = SizeBucket.M,
            accent = AccentOption.CYAN,
            background = BackgroundMode.SCRIM,
            theme = ThemeMode.FORCE_LIGHT,
            cornerRadius = 12,
            showLabel = false,
            tapAction = TapAction.OPEN_MAIN,
            shape = WidgetShape.SQUIRCLE
        )
        val instance = WidgetInstance(
            appWidgetId = 42,
            widgetTypeId = definition.id,
            configJson = state.toConfigJson(definition.id)
        )

        assertTrue(WidgetSetupState.isConfigured(instance))
        // Reading back fills in option defaults (every widget has the universal text size / corner dot).
        assertEquals(
            state.copy(optionValues = WidgetSetupState.resolveOptions(definition, null)),
            WidgetSetupState.fromInstance(instance, definition, preferences)
        )
        val existingConfig = JSONObject(instance.configJson).put("future_option", "preserved").toString()
        assertEquals(
            "preserved",
            JSONObject(state.toConfigJson(definition.id, existingConfig)).getString("future_option")
        )
    }

    @Test
    fun `custom shape points round trip through widget config`() {
        val definition = WidgetRegistry.getById("clock.analog.ring")!!
        val preferences = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val points = listOf(
            ShapePoint(0.2f, 0.1f), ShapePoint(0.8f, 0.1f), ShapePoint(0.5f, 0.9f)
        )
        val state = WidgetSetupState.defaults(definition, preferences).copy(
            shape = WidgetShape.CUSTOM,
            customPoints = points
        )
        val instance = WidgetInstance(
            appWidgetId = 43,
            widgetTypeId = definition.id,
            configJson = state.toConfigJson(definition.id)
        )
        val restored = WidgetSetupState.fromInstance(instance, definition, preferences)

        assertEquals(WidgetShape.CUSTOM, restored.shape)
        assertEquals(points, restored.customPoints)
        assertEquals("0.2,0.1;0.8,0.1;0.5,0.9", JSONObject(instance.configJson).getString("shape_points"))
    }

    @Test
    fun `shape fill border colour and border style round trip`() {
        val definition = WidgetRegistry.getById("clock.analog.ring")!!
        val preferences = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val state = WidgetSetupState.defaults(definition, preferences).copy(
            shapeFillColor = 0xFF123456.toInt(),
            shapeBorderColor = 0xFFABCDEF.toInt(),
            shapeBorderStyle = Frame.DOTTED
        )
        val instance = WidgetInstance(
            appWidgetId = 45,
            widgetTypeId = definition.id,
            configJson = state.toConfigJson(definition.id)
        )
        val restored = WidgetSetupState.fromInstance(instance, definition, preferences)

        assertEquals(0xFF123456.toInt(), restored.shapeFillColor)
        assertEquals(0xFFABCDEF.toInt(), restored.shapeBorderColor)
        assertEquals(Frame.DOTTED, restored.shapeBorderStyle)

        val json = JSONObject(instance.configJson)
        assertEquals("#123456", json.getString("shape_fill"))
        assertEquals("#ABCDEF", json.getString("shape_border"))
        assertEquals("DOTTED", json.getString("shape_border_style"))
    }

    @Test
    fun `default provider instance is not treated as configured`() {
        assertFalse(
            WidgetSetupState.isConfigured(
                WidgetInstance(appWidgetId = 42, widgetTypeId = "clock.dotdigital.s")
            )
        )
    }

    @Test
    fun `smart stack mode and slot choices round trip through widget config`() {
        val definition = WidgetRegistry.getById(CollectionWidgets.SMART_STACK)!!
        val preferences = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val state = WidgetSetupState.defaults(definition, preferences).copy(
            stackItems = listOf("clock.dotdigital.s", "calendar.week.progress"),
            stackMode = StackMode.SMART,
            stackTimeSlots = mapOf(
                StackTimeSlot.MORNING to "clock.dotdigital.s",
                StackTimeSlot.DAY to "calendar.week.progress"
            )
        )
        val instance = WidgetInstance(
            appWidgetId = 43,
            widgetTypeId = definition.id,
            configJson = state.toConfigJson(definition.id)
        )

        val restored = WidgetSetupState.fromInstance(instance, definition, preferences)
        assertEquals(state.stackMode, restored.stackMode)
        assertEquals(state.stackTimeSlots, restored.stackTimeSlots)
        assertEquals(state.stackItems, restored.stackItems)
    }

    @Test
    fun `open app tap action round trips its package`() {
        val definition = WidgetRegistry.getById("clock.analog.ring")!!
        val preferences = PreferencesManager.getInstance(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val state = WidgetSetupState.defaults(definition, preferences).copy(
            tapAction = TapAction.OPEN_APP,
            tapAppPackage = "com.google.android.youtube"
        )
        val instance = WidgetInstance(
            appWidgetId = 44,
            widgetTypeId = definition.id,
            configJson = state.toConfigJson(definition.id)
        )

        val restored = WidgetSetupState.fromInstance(instance, definition, preferences)
        assertEquals(TapAction.OPEN_APP, restored.tapAction)
        assertEquals("com.google.android.youtube", restored.tapAppPackage)

        val cleared = state.copy(tapAppPackage = null).toConfigJson(definition.id)
        assertFalse(JSONObject(cleared).has("tap_app_package"))
    }
}
