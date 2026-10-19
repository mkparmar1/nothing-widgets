package com.example

import android.graphics.Color
import com.example.core.theme.MaterialYouPalette
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialYouPaletteTest {

    @Test
    fun `very dark wallpaper colour is brightened into a legible range`() {
        val out = MaterialYouPalette.harmonize(Color.rgb(10, 10, 12))
        val hsv = FloatArray(3).also { Color.colorToHSV(out, it) }
        assertTrue("brightness should be raised, was ${hsv[2]}", hsv[2] >= 0.60f)
    }

    @Test
    fun `very pale wallpaper colour is kept below white`() {
        val out = MaterialYouPalette.harmonize(Color.rgb(252, 252, 252))
        val hsv = FloatArray(3).also { Color.colorToHSV(out, it) }
        assertTrue("brightness should be capped, was ${hsv[2]}", hsv[2] <= 0.94f)
    }

    @Test
    fun `saturated mid colour keeps a useful saturation`() {
        val out = MaterialYouPalette.harmonize(Color.rgb(0, 120, 90))
        val hsv = FloatArray(3).also { Color.colorToHSV(out, it) }
        assertTrue("saturation should stay usable, was ${hsv[1]}", hsv[1] >= 0.55f)
    }
}
