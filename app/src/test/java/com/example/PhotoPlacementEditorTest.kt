package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.photos.BackgroundImageStore
import com.example.ui.config.PhotoPlacementEditor
import com.example.ui.theme.DotWidgetsTheme
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
class PhotoPlacementEditorTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `save photo action remains available in the zoom editor`() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        var saved = false
        compose.setContent {
            DotWidgetsTheme {
                PhotoPlacementEditor(
                    image = image,
                    initial = BackgroundImageStore.Choice("test-photo"),
                    widgetAspect = 1f,
                    cornerFraction = 0.1f,
                    accent = Color.Red,
                    onCancel = {},
                    onDone = { saved = true }
                )
            }
        }

        compose.onNodeWithTag("photo_edit_done").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertTrue(saved)
    }
}
