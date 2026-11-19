package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.theme.AccentOption
import com.example.ui.onboarding.APP_TOUR_STEPS
import com.example.ui.onboarding.AppTourOverlay
import com.example.ui.onboarding.hasSeenAppTour
import com.example.ui.onboarding.markAppTourSeen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DotWidgetsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AppTourTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun firstStepAdvancesSkipsAndHidesBack() {
        var next = 0
        var skip = 0
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                AppTourOverlay(
                    step = APP_TOUR_STEPS.first(),
                    index = 0,
                    count = APP_TOUR_STEPS.size,
                    onNext = { next++ },
                    onBack = {},
                    onSkip = { skip++ }
                )
            }
        }

        compose.onNodeWithText("STEP 1 OF ${APP_TOUR_STEPS.size}").assertExists()
        compose.onNodeWithTag("app_tour_back").assertDoesNotExist()
        compose.onNodeWithTag("app_tour_next").performClick()
        assertEquals(1, next)
        compose.onNodeWithTag("app_tour_skip").performClick()
        assertEquals(1, skip)
    }

    @Test
    fun lastStepShowsFinishAndBack() {
        var back = 0
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                AppTourOverlay(
                    step = APP_TOUR_STEPS.last(),
                    index = APP_TOUR_STEPS.lastIndex,
                    count = APP_TOUR_STEPS.size,
                    onNext = {},
                    onBack = { back++ },
                    onSkip = {}
                )
            }
        }

        compose.onNodeWithText("STEP ${APP_TOUR_STEPS.size} OF ${APP_TOUR_STEPS.size}").assertExists()
        compose.onNodeWithText("Finish").assertExists()
        compose.onNodeWithTag("app_tour_back").performClick()
        assertEquals(1, back)
    }

    @Test
    fun helpPageStartsTheTour() {
        var started = 0
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                SettingsScreen(onStartTour = { started++ })
            }
        }

        compose.onNodeWithTag("settings_row_help").performScrollTo().performClick()
        compose.onNodeWithTag("help_start_tour").assertIsDisplayed().performClick()
        assertEquals(1, started)
    }

    @Test
    fun seenFlagStartsFalseAndPersists() {
        context.getSharedPreferences("dot_widgets_onboarding", Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertFalse(hasSeenAppTour(context))
        markAppTourSeen(context)
        assertTrue(hasSeenAppTour(context))
    }
}
