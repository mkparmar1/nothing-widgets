package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeBucket
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.themes.ThemeAnimations
import com.example.themes.ThemeRepository
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.AnimatedOverlays
import com.example.widgets.base.WidgetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class SettingsPagesTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `settings is a menu and each group opens on its own page`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { DotWidgetsTheme(accentOption = AccentOption.RED) { SettingsScreen() } }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("theme_studio_card").assertDoesNotExist()

        compose.onNodeWithTag("settings_row_appearance").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("theme_studio_card").assertIsDisplayed()

        compose.onNodeWithTag("settings_back").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("theme_studio_card").assertDoesNotExist()

        compose.onNodeWithTag("settings_row_about").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("non_affiliation").assertExists()
    }

    private fun spec(): WidgetSpec {
        val dims = SizeResolver.resolveDimensions(context, SizeBucket.S)
        return WidgetSpec(
            widthPx = dims.widthPx,
            heightPx = dims.heightPx,
            density = dims.density,
            sizeBucket = SizeBucket.S,
            accent = AccentOption.RED,
            bgMode = BackgroundMode.TRANSPARENT,
            themeMode = ThemeMode.FORCE_DARK,
            isDark = true,
            cornerRadiusDp = 20,
            showLabel = true,
            tapAction = com.example.widgets.base.TapAction.DEFAULT
        )
    }

    @Test
    fun `theme widgets animate, others do not, and animations can be turned off`() {
        ThemeRepository.setAnimationsEnabled(context, true)
        val animation = ThemeAnimations.frames(context, "fun.theme.clock", spec())
        assertNotNull(animation)
        assertEquals(AnimatedOverlays.FRAMES, animation!!.frames.size)
        assertTrue(animation.frames.all { it.config == Bitmap.Config.ALPHA_8 })
        assertNull(ThemeAnimations.frames(context, "clock.dotdigital.s", spec()))

        ThemeRepository.setAnimationsEnabled(context, false)
        assertNull(ThemeAnimations.frames(context, "fun.theme.clock", spec()))
        assertFalse(ThemeRepository.animationsEnabled(context))
    }
}
