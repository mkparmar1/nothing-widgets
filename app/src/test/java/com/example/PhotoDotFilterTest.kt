package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.core.render.PhotoDotFilter
import com.example.data.photos.PhotoFilter
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The dot filter is checked on a known image so the brightness-to-dot mapping stays predictable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoDotFilterTest {

    /** Black on top, white on the bottom: mono dots belong to the dark half only. */
    @Test
    fun `mono filter draws white dots on dark areas and leaves bright areas empty`() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (x in 0 until 64) for (y in 0 until 64) {
            image.setPixel(x, y, if (y < 32) Color.BLACK else Color.WHITE)
        }
        val out = PhotoDotFilter.filteredWhole(image, PhotoFilter.MONO, 1f, Color.WHITE)!!

        assertTrue("dark half should get dots", litPixels(out, 0, 20) > 20)
        assertTrue("bright half should stay empty", litPixels(out, 44, 64) == 0)
        assertTrue("gaps between dots stay transparent", transparentPixels(out) > 0)
        for (x in 0 until out.width step 2) for (y in 0 until 20 step 2) {
            val px = out.getPixel(x, y)
            if (Color.alpha(px) > 128) {
                assertTrue("mono dots are white", Color.red(px) > 200 && Color.green(px) > 200 && Color.blue(px) > 200)
            }
        }
    }

    @Test
    fun `accent filter uses the widget accent colour`() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(64, 64, 64)) }
        val out = PhotoDotFilter.filteredWhole(image, PhotoFilter.ACCENT, 1f, Color.rgb(255, 0, 0))!!
        var checked = 0
        for (x in 0 until out.width) for (y in 0 until out.height) {
            val px = out.getPixel(x, y)
            if (Color.alpha(px) > 128) {
                assertTrue("accent dot is red", Color.red(px) > 180 && Color.green(px) < 60 && Color.blue(px) < 60)
                checked++
            }
        }
        assertTrue("expected accent dots", checked > 20)
    }

    @Test
    fun `colour filter keeps the photo colours`() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (x in 0 until 64) for (y in 0 until 64) {
            image.setPixel(x, y, if (x < 32) Color.RED else Color.BLUE)
        }
        val out = PhotoDotFilter.filteredWhole(image, PhotoFilter.COLOUR, 1f, Color.WHITE)!!
        val left = out.getPixel(4, 4)
        val right = out.getPixel(60, 4)
        assertTrue("left dots stay red", Color.red(left) > 180 && Color.blue(left) < 60)
        assertTrue("right dots stay blue", Color.blue(right) > 180 && Color.red(right) < 60)
    }

    @Test
    fun `off returns no filtered image`() {
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        assertNull(PhotoDotFilter.filteredWhole(image, PhotoFilter.OFF, 1f, Color.WHITE))
    }

    private fun litPixels(bitmap: Bitmap, fromY: Int, toY: Int): Int {
        var count = 0
        for (x in 0 until bitmap.width) for (y in fromY until toY) {
            if (Color.alpha(bitmap.getPixel(x, y)) > 128) count++
        }
        return count
    }

    private fun transparentPixels(bitmap: Bitmap): Int {
        var count = 0
        for (x in 0 until bitmap.width) for (y in 0 until bitmap.height) {
            if (Color.alpha(bitmap.getPixel(x, y)) == 0) count++
        }
        return count
    }
}
