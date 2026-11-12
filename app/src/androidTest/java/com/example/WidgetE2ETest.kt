package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.core.bitmap.SizeResolver
import com.example.data.db.AppDatabase
import com.example.data.prefs.PreferencesManager
import com.example.ui.config.WidgetSetupState
import com.example.ui.config.pinWidgetToHomeScreen
import com.example.widgets.base.DotWidgetProviderS
import com.example.widgets.base.WidgetAlarmScheduler
import com.example.widgets.base.WidgetRegistry
import com.example.widgets.base.WidgetRenderEngine
import java.io.File
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * End-to-end widget checks on a real device or emulator: drawing with real graphics, and adding,
 * refreshing and tapping a widget through the launcher. Screenshots go to the app's external files
 * directory (Android/data/<package>/files/e2e).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WidgetE2ETest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val pkg = context.packageName
    private val widgetId = "clock.dotdigital.s"

    private fun placedSmall(): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, DotWidgetProviderS::class.java))

    private fun screenshot(name: String) {
        val dir = File(context.getExternalFilesDir(null), "e2e").apply { mkdirs() }
        device.takeScreenshot(File(dir, "$name.png"))
    }

    /** Every widget, at every size it supports, draws without throwing on the device's real graphics. */
    @Test
    fun a_everyWidgetDrawsOnDevice() {
        val failures = mutableListOf<String>()
        var drawn = 0
        WidgetRegistry.getAll().forEach { def ->
            def.sizes.forEach { size ->
                val dims = SizeResolver.resolveDimensions(context, size)
                val spec = com.example.widgets.base.WidgetSpec(
                    widthPx = dims.widthPx,
                    heightPx = dims.heightPx,
                    density = dims.density,
                    sizeBucket = size,
                    accent = com.example.core.theme.AccentOption.RED,
                    bgMode = com.example.core.theme.BackgroundMode.SOLID,
                    themeMode = com.example.core.theme.ThemeMode.FORCE_DARK,
                    isDark = true,
                    cornerRadiusDp = 20,
                    showLabel = true,
                    tapAction = def.defaultTap
                )
                val bitmap = Bitmap.createBitmap(dims.widthPx, dims.heightPx, Bitmap.Config.ARGB_8888)
                try {
                    def.render(context, spec, Canvas(bitmap))
                    drawn++
                } catch (t: Throwable) {
                    failures += "${def.id} [$size]: ${t.javaClass.simpleName}: ${t.message}"
                } finally {
                    bitmap.recycle()
                }
            }
        }
        assertTrue("Widgets that failed to draw:\n" + failures.joinToString("\n"), failures.isEmpty())
        assertTrue("drew only $drawn widget sizes", drawn > 200)
    }

    /** Add to home screen goes through the launcher's dialog and leaves a configured, visible widget. */
    @Test
    fun b_addToHomeScreenPlacesAWorkingWidget() {
        val before = placedSmall().toSet()
        ActivityScenario.launch(MainActivity::class.java).use {
            val definition = WidgetRegistry.getById(widgetId) ?: error("$widgetId is not registered")
            val state = WidgetSetupState.defaults(definition, PreferencesManager.getInstance(context))
            // The app calls this from the UI (main) thread; it shows a toast, which needs a looper.
            instrumentation.runOnMainSync { runBlocking { pinWidgetToHomeScreen(context, definition, state) } }

            // The launcher's confirmation dialog: its button text differs between launchers.
            val add = device.wait(Until.findObject(By.text(Pattern.compile("(?i)(add( to home screen)?|add automatically)"))), 10_000)
            screenshot("01_pin_dialog")
            assertNotNull("launcher did not show the add-widget dialog", add)
            add.click()
        }

        val start = System.currentTimeMillis()
        var added: Set<Int> = emptySet()
        while (System.currentTimeMillis() - start < 15_000) {
            added = placedSmall().toSet() - before
            if (added.isNotEmpty()) break
            Thread.sleep(250)
        }
        assertEquals("expected exactly one new small widget", 1, added.size)

        // Saved with the chosen design.
        val instance = runBlocking { AppDatabase.getInstance(context).widgetInstanceDao().getById(added.first()) }
        assertNotNull("no saved settings for the new widget", instance)
        assertEquals(widgetId, instance!!.widgetTypeId)

        device.pressHome()
        val image = device.wait(Until.findObject(By.res(pkg, "widget_image_view")), 10_000)
        screenshot("02_home_with_widget")
        assertNotNull("the widget's image is not on the home screen", image)
    }

    /** A refresh redraws placed widgets without removing them. */
    @Test
    fun c_refreshKeepsWidgetsPlaced() {
        val before = placedSmall().size
        assertTrue("needs a placed widget (run b first)", before > 0)
        context.sendBroadcast(Intent(WidgetAlarmScheduler.ACTION_REFRESH_WIDGETS).setPackage(pkg))
        runBlocking { WidgetRenderEngine.updateAllPlacedWidgets(context, force = true) }
        device.pressHome()
        assertNotNull(device.wait(Until.findObject(By.res(pkg, "widget_image_view")), 10_000))
        assertEquals(before, placedSmall().size)
        screenshot("03_after_refresh")
    }

    /** Tapping the clock widget leaves the launcher (opens the clock app by default). */
    @Test
    fun d_tappingTheWidgetOpensSomething() {
        device.pressHome()
        val launcher = device.currentPackageName
        val image = device.wait(Until.findObject(By.res(pkg, "widget_image_view")), 10_000)
        assertNotNull("needs a placed widget (run b first)", image)
        image.click()
        device.wait(Until.gone(By.pkg(launcher).depth(0)), 5_000)
        screenshot("04_after_tap")
        assertNotEquals("tap stayed on the launcher", launcher, device.currentPackageName)
        device.pressHome()
    }
}
