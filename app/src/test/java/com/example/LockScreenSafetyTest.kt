package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.core.bitmap.SizeResolver
import com.example.core.theme.AccentOption
import com.example.core.theme.BackgroundMode
import com.example.core.theme.ThemeMode
import com.example.widgets.base.Category
import com.example.widgets.base.LockScreenSafety
import com.example.widgets.base.WidgetProviders
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

/**
 * Lock-screen (Android 16 QPR2+) guard rails: only glanceable, non-private designs are allowed,
 * and a transparent design gets a scrim so it stays readable over any wallpaper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockScreenSafetyTest {

    @Test
    fun `transparent becomes scrim on the lock screen only`() {
        assertEquals(
            BackgroundMode.SCRIM,
            LockScreenSafety.effectiveBackground(BackgroundMode.TRANSPARENT, onLockScreen = true)
        )
        assertEquals(
            BackgroundMode.TRANSPARENT,
            LockScreenSafety.effectiveBackground(BackgroundMode.TRANSPARENT, onLockScreen = false)
        )
        assertEquals(
            BackgroundMode.SOLID,
            LockScreenSafety.effectiveBackground(BackgroundMode.SOLID, onLockScreen = true)
        )
    }

    @Test
    fun `lock-safe widgets never expose personal data`() {
        val lockSafe = WidgetRegistry.lockSafeDefs()
        assertTrue("no lock-safe widgets registered", lockSafe.isNotEmpty())

        val privateCategories = setOf(
            Category.CONTACTS, Category.MEDIA, Category.PHOTO, Category.HEALTH,
            Category.FINANCE, Category.SMART_HOME, Category.SPORTS, Category.LEARNING,
            Category.TRAVEL
        )
        assertTrue(
            "private categories on the lock screen: " +
                lockSafe.filter { it.category in privateCategories }.map { it.id },
            lockSafe.none { it.category in privateCategories }
        )
        assertTrue(
            "private widgets on the lock screen: " + lockSafe.map { it.id },
            lockSafe.none { LockScreenSafety.showsPrivateData(it.id) }
        )

        val realPrivate = WidgetRegistry.getAll().filter { LockScreenSafety.showsPrivateData(it.id) }
        assertTrue("private-data deny list matched no real widgets", realPrivate.isNotEmpty())
        assertTrue(
            "widgets marked lock-safe despite showing private data: " +
                realPrivate.filter { it.lockSafe }.map { it.id },
            realPrivate.none { it.lockSafe }
        )
    }

    @Test
    fun `lock-screen configs drop private photo backgrounds`() {
        val clean = LockScreenSafety.sanitizedConfig(
            """{"bg_image":"abc123","bg_image_dim":50,"bg_image_zoom":1.4,"accent":"AMBER","bg_alpha":80}"""
        )
        assertFalse("photo id survived lock-screen sanitising: $clean", clean.contains("bg_image"))
        assertFalse("photo dim survived lock-screen sanitising: $clean", clean.contains("bg_image_dim"))
        assertTrue("unrelated config was discarded: $clean", clean.contains("AMBER"))
        assertTrue("unrelated config was discarded: $clean", clean.contains("bg_alpha"))
        assertEquals("{}", LockScreenSafety.sanitizedConfig("not json"))
    }

    @Test
    fun `lock providers default to safe designs`() {
        WidgetProviders.ALL.filter { it.lockScreen }.forEach { entry ->
            val definition = WidgetRegistry.getById(entry.defaultWidgetTypeId)
            assertTrue(
                "lock provider ${entry.providerClass.simpleName} defaults to unsafe ${entry.defaultWidgetTypeId}",
                LockScreenSafety.safeForLockScreen(definition)
            )
        }
    }

    @Test
    fun `lock-safe widgets stay readable over light and dark wallpapers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outDir = File("build/lock-screen-safety").apply { deleteRecursively(); mkdirs() }
        val failures = mutableListOf<String>()

        for (definition in WidgetRegistry.lockSafeDefs()) {
            val size = definition.sizes.first()
            val dims = SizeResolver.resolveDimensions(context, size)
            for (isDark in listOf(true, false)) {
                val spec = WidgetSpec(
                    widthPx = dims.widthPx,
                    heightPx = dims.heightPx,
                    density = dims.density,
                    sizeBucket = size,
                    accent = AccentOption.RED,
                    accentColor = AccentOption.RED.hex,
                    // The user picked transparent; the lock-screen guard turns it into a scrim.
                    bgMode = LockScreenSafety.effectiveBackground(
                        BackgroundMode.TRANSPARENT, onLockScreen = true
                    ),
                    themeMode = if (isDark) ThemeMode.FORCE_DARK else ThemeMode.FORCE_LIGHT,
                    isDark = isDark,
                    cornerRadiusDp = 20,
                    showLabel = true,
                    tapAction = definition.defaultTap
                )
                val widget = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    definition.render(context, spec, Canvas(widget))
                } catch (e: Throwable) {
                    failures += "${definition.id} $size dark=$isDark: $e"
                    continue
                }

                for (wallpaper in listOf("light" to Color.WHITE, "dark" to Color.BLACK)) {
                    val composite = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(composite)
                    canvas.drawColor(wallpaper.second)
                    canvas.drawBitmap(widget, 0f, 0f, null)
                    val file = File(outDir, "${definition.id}_${size.name}_${if (isDark) "dark" else "light"}_on_${wallpaper.first}.png")
                    file.outputStream().use { composite.compress(Bitmap.CompressFormat.PNG, 100, it) }

                    val contrast = luminanceRange(composite)
                    if (contrast < 0.12f) {
                        failures += "${definition.id} $size dark=$isDark on ${wallpaper.first} wallpaper: contrast $contrast"
                    }
                }
            }
        }

        assertTrue("Lock-screen contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** Spread between the darkest and brightest pixels, ignoring the outer frame. */
    private fun luminanceRange(bitmap: Bitmap): Float {
        val x0 = bitmap.width / 10
        val y0 = bitmap.height / 10
        val x1 = bitmap.width - x0
        val y1 = bitmap.height - y0
        val row = IntArray(x1 - x0)
        var min = 1f
        var max = 0f
        for (y in y0 until y1) {
            bitmap.getPixels(row, 0, row.size, x0, y, row.size, 1)
            for (pixel in row) {
                val l = 0.2126f * Color.red(pixel) / 255f +
                    0.7152f * Color.green(pixel) / 255f +
                    0.0722f * Color.blue(pixel) / 255f
                if (l < min) min = l
                if (l > max) max = l
            }
        }
        return abs(max - min)
    }
}
