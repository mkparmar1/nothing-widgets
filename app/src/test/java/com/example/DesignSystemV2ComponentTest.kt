package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.theme.AccentOption
import com.example.ui.components.BottomNav
import com.example.ui.components.ColorDots
import com.example.ui.components.DotNavItem
import com.example.ui.components.DotSetupCard
import com.example.ui.components.HeroCard
import com.example.ui.components.QuickAccessTile
import com.example.ui.components.SettingsRow
import com.example.ui.components.SegmentTabs
import com.example.ui.components.StatusHero
import com.example.ui.components.WallpaperCard
import com.example.ui.components.WidgetCard
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignSystemV2ComponentTest {

    @get:Rule
    val compose = createComposeRule()
    private val sampleDefinition: WidgetDefinition by lazy {
        WidgetRegistry.getAll().first { it.sizes.isNotEmpty() }
    }

    @Test
    fun `v2 components render without crashing`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                    HeroCard(
                        title = "Make your home screen smarter",
                        subtitle = "Widgets, setups, wallpapers and a builder.",
                        ctaLabel = "Explore Widgets",
                        onCta = {},
                        preview = {}
                    )
                    QuickAccessTile(
                        icon = Icons.Filled.Add,
                        label = "Widgets",
                        onClick = {},
                        selected = true
                    )
                    WidgetCard(
                        definition = sampleDefinition,
                        onClick = {},
                        accent = AccentOption.RED,
                        badges = listOf("LOCK")
                    )
                    DotSetupCard(
                        title = "Minimal Home",
                        caption = "Clock, date and weather on one tile.",
                        accent = Color.Red,
                        onApply = {}
                    )
                    WallpaperCard(
                        name = "Dot Matrix",
                        selected = true,
                        onClick = {},
                        image = {}
                    )
                    SettingsRow(
                        icon = Icons.Filled.DarkMode,
                        title = "Appearance",
                        onClick = {},
                        caption = "Themes, accent and widget style"
                    )
                    SegmentTabs(
                        items = listOf("Skin", "Shape", "Accent", "Size"),
                        selectedIndex = 1,
                        onSelect = {},
                        tagPrefix = "seg"
                    )
                    ColorDots(
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        selected = Color.Green,
                        onSelect = {},
                        onAdd = {}
                    )
                    BottomNav(
                        items = listOf(
                            DotNavItem("Home", Icons.Filled.AddHome),
                            DotNavItem("Explore", Icons.Filled.Add),
                            DotNavItem("My Widgets", Icons.Filled.Add),
                            DotNavItem("Settings", Icons.Filled.Add)
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                        onPlus = {}
                    )
                    StatusHero(
                        icon = Icons.Filled.CheckCircle,
                        title = "All widgets working",
                        caption = "Last checked just now."
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNodeWithText("Make your home screen smarter").assertExists()
        compose.onNodeWithText("Widgets").assertExists()
        compose.onNodeWithText("Minimal Home").assertExists()
        compose.onNodeWithText("Dot Matrix").assertExists()
        compose.onNodeWithText("Appearance").assertExists()
        compose.onNodeWithText("Shape").assertExists()
        compose.onNodeWithText("All widgets working").assertExists()
        assertTrue(sampleDefinition.title.isNotEmpty())
    }

    @Test
    fun `bottom nav plus and tabs respond to clicks`() {
        var plus = false
        var picked = -1
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                BottomNav(
                    items = listOf(
                        DotNavItem("Home", Icons.Filled.AddHome),
                        DotNavItem("Explore", Icons.Filled.Add),
                        DotNavItem("My Widgets", Icons.Filled.Add),
                        DotNavItem("Settings", Icons.Filled.Add)
                    ),
                    selectedIndex = 0,
                    onSelect = { picked = it },
                    onPlus = { plus = true }
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Settings").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Home").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Create widget").performClick()
        compose.mainClock.advanceTimeBy(500)

        assertEquals(0, picked)
        assertTrue(plus)
    }
}
