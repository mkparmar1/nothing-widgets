package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.core.theme.AccentOption
import com.example.ui.browse.WidgetDetailsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.styles.StyleSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetDetailsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val definition = WidgetRegistry.getAll().first { it.sizes.size > 1 }

    @Test
    fun `details show every size and hand off to customize and back`() {
        var customized: WidgetDefinition? = null
        var backed = false

        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                WidgetDetailsScreen(
                    definition = definition,
                    onBack = { backed = true },
                    onCustomize = { customized = it }
                )
            }
        }

        compose.onNodeWithTag("widget_details_screen").assertIsDisplayed()
        compose.onNodeWithText(definition.title).assertIsDisplayed()
        definition.sizes.forEach { size ->
            compose.onNodeWithTag("details_size_${size.name.lowercase()}").assertExists()
        }

        compose.onNodeWithTag("details_customize").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(definition, customized)

        compose.onNodeWithTag("details_back").performScrollTo().performClick()
        assertTrue(backed)
    }

    @Test
    fun `picking a style selects it and carries it into customize`() {
        val styled = WidgetRegistry.getById("battery.ring")!!
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                WidgetDetailsScreen(definition = styled, onBack = {}, onCustomize = {})
            }
        }
        compose.onNodeWithTag("details_style_picker").performScrollTo()
        compose.onNodeWithTag("style_bold_ring").assertIsSelected()
        compose.onNodeWithTag("style_soft_ring").performClick()
        compose.onNodeWithTag("style_soft_ring").assertIsSelected()
        compose.onNodeWithTag("details_customize").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("soft_ring", StyleSelection.take("battery.ring"))
    }
}
