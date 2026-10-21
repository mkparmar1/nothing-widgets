package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.render.Frame
import com.example.core.render.Skin
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.core.theme.WidgetAccent
import com.example.core.time.ClockSupport
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.WidgetSetupContent
import com.example.ui.config.WidgetSetupState
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Drives every style control on the customization screen and checks the state it produces. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetSetupScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val definition = WidgetRegistry.getAll().first {
        SizeBucket.S in it.sizes && SizeBucket.M in it.sizes && ClockSupport.timeFormats(it.id).isNotEmpty()
    }

    private var latest: WidgetSetupState? = null
    private var saved: WidgetSetupState? = null

    private fun launch() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val initial = WidgetSetupState.defaults(definition, PreferencesManager.getInstance(context))
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                WidgetSetupContent(
                    definition = definition,
                    initial = initial,
                    lockedSize = null,
                    saveLabel = "ADD TO HOME SCREEN",
                    onStateChange = { latest = it },
                    onSave = { saved = it }
                )
            }
        }
        compose.onNodeWithTag("config_live_preview").assertIsDisplayed()
    }

    /** Scrolls the page to [tag] (performScrollTo only scrolls the nearest row), then taps it. */
    private fun tap(tag: String) {
        reveal(tag)
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun reveal(tag: String) {
        val page = compose.onNodeWithTag("setup_scroll")
        val pageTop = page.fetchSemanticsNode().boundsInRoot.top
        val nodeTop = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
        page.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, nodeTop - pageTop - 40f) }
        compose.waitForIdle()
    }

    @Test
    fun `every skin frame and size is selectable`() {
        launch()
        Skin.entries.forEach { skin ->
            tap("skin_${skin.name.lowercase()}")
            assertEquals(skin, latest?.skin)
        }
        Frame.entries.forEach { frame ->
            tap("frame_${frame.name.lowercase()}")
            assertEquals(frame, latest?.frame)
        }
        listOf(SizeBucket.M, SizeBucket.S).forEach { size ->
            tap("size_${size.name.lowercase()}")
            assertEquals(size, latest?.size)
        }
    }

    @Test
    fun `accents background opacity and theme update the state`() {
        launch()
        AccentOption.entries.forEach { accent ->
            tap("accent_option_${accent.name.lowercase()}")
            assertEquals(accent, latest?.accent)
            assertNull(latest?.customAccent)
        }

        tap("accent_option_custom")
        compose.onNodeWithTag("custom_color_apply").performClick()
        compose.waitForIdle()
        assertNotNull("custom colour applied", latest?.customAccent)

        mapOf(
            "background_transparent" to BackgroundMode.TRANSPARENT,
            "background_scrim" to BackgroundMode.SCRIM,
            "background_solid" to BackgroundMode.SOLID
        ).forEach { (tag, mode) ->
            tap(tag)
            assertEquals(mode, latest?.background)
        }

        reveal("opacity_slider")
        compose.onNodeWithTag("opacity_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.waitForIdle()
        assertEquals(0.5f, latest!!.bgAlpha, 0.02f)

        mapOf(
            "theme_dark" to ThemeMode.FORCE_DARK,
            "theme_light" to ThemeMode.FORCE_LIGHT,
            "theme_system" to ThemeMode.AUTO
        ).forEach { (tag, mode) ->
            tap(tag)
            assertEquals(mode, latest?.theme)
        }

        compose.onNodeWithTag("pin_widget_button").performClick()
        compose.waitForIdle()
        val result = saved!!
        // The custom colour is written to the widget config and read back.
        val json = result.toConfigJson(definition.id)
        assertEquals(result.customAccent, WidgetAccent.custom(json))
        assertTrue(json.contains("\"bg_alpha\":50"))
    }
}
