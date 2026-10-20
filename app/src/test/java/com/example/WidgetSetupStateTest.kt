package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.data.db.WidgetInstance
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.WidgetSetupState
import com.example.widgets.base.TapAction
import com.example.widgets.base.WidgetRegistry
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
            tapAction = TapAction.OPEN_MAIN
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
    fun `default provider instance is not treated as configured`() {
        assertFalse(
            WidgetSetupState.isConfigured(
                WidgetInstance(appWidgetId = 42, widgetTypeId = "clock.dotdigital.s")
            )
        )
    }
}
