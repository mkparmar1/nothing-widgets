package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.theme.AccentOption
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.LockScreenGuideDialog
import com.example.ui.config.LockScreenWidgets
import com.example.ui.config.WidgetSetupContent
import com.example.ui.config.WidgetSetupState
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockScreenWidgetsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `pending design is handed over once`() {
        LockScreenWidgets.savePending(context, "clock.dotdigital.s", "{\"accent\":\"CYAN\"}")
        assertEquals("clock.dotdigital.s" to "{\"accent\":\"CYAN\"}", LockScreenWidgets.takePending(context))
        assertNull(LockScreenWidgets.takePending(context))
    }

    @Test
    fun `phones before Android 16 have no lock-screen widgets`() {
        assertFalse(LockScreenWidgets.isSupported(context))
    }

    @Test
    fun `lock-safe widgets offer add to lock screen with the current settings`() {
        val definition = WidgetRegistry.getAll().first { it.lockSafe }
        val initial = WidgetSetupState.defaults(definition, PreferencesManager.getInstance(context))
        var requested: WidgetSetupState? = null
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                WidgetSetupContent(
                    definition = definition,
                    initial = initial,
                    lockedSize = null,
                    saveLabel = "ADD TO HOME SCREEN",
                    onAddToLockScreen = { requested = it },
                    onSave = {}
                )
            }
        }
        compose.onNodeWithTag("lock_screen_button").performClick()
        assertNotNull(requested)
        assertEquals(initial.skin, requested!!.skin)
    }

    @Test
    fun `unsupported phones get an honest message and a home-screen option`() {
        val definition = WidgetRegistry.getAll().first { it.lockSafe }
        var home = false
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                LockScreenGuideDialog(definition, supported = false, onAddToHomeScreen = { home = true }, onDismiss = {})
            }
        }
        compose.onNodeWithText("Lock screen not available").assertIsDisplayed()
        compose.onNodeWithTag("lock_guide_home").performClick()
        assertEquals(true, home)
    }
}
