package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.core.theme.AccentOption
import com.example.ui.setups.SetupsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.ui.wallpapers.WallpapersScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupsWallpapersScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `setups filter by style and keep the share actions`() {
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                SetupsScreen()
            }
        }

        compose.onNodeWithTag("setups_screen").assertIsDisplayed()
        compose.onNodeWithTag("export_setup").assertExists()
        compose.onNodeWithTag("import_setup").assertExists()
        compose.onNodeWithTag("setup_card_setup_minimal_red").assertExists()

        compose.onNodeWithTag("setup_filter_minimal").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("setup_filter_minimal").assertIsSelected()
        compose.onNodeWithTag("setup_card_setup_minimal_red").assertExists()

        compose.onNodeWithTag("setup_filter_all").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("setup_filter_all").assertIsSelected()
    }

    @Test
    fun `wallpapers feature a card and confirm before applying`() {
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                WallpapersScreen()
            }
        }

        compose.onNodeWithTag("wallpapers_screen").assertIsDisplayed()
        compose.onNodeWithTag("wallpaper_featured").assertExists()
        compose.onNodeWithTag("apply_wallpaper_featured").assertExists()
        compose.onNodeWithTag("wallpaper_item_1").assertExists()

        compose.onNodeWithTag("wallpaper_tag_MONO").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("wallpaper_tag_MONO").assertIsSelected()
        compose.onNodeWithTag("wallpaper_item_7").assertExists()

        compose.onNodeWithTag("apply_wallpaper_featured").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("apply_wallpaper_button").assertExists()
    }
}
