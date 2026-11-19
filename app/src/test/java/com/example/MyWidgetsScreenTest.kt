package com.example

import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.theme.AccentOption
import com.example.data.builder.BuilderBlock
import com.example.data.builder.BuilderBlockType
import com.example.data.builder.BuilderLayout
import com.example.data.builder.CustomWidget
import com.example.data.db.AppDatabase
import com.example.data.db.WidgetInstance
import com.example.ui.mywidgets.MyWidgetsScreen
import com.example.ui.theme.DotWidgetsTheme
import com.example.widgets.base.WidgetDefinition
import com.example.widgets.base.WidgetRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** My Widgets actions: duplicate a saved design, and edit a widget already on the home screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MyWidgetsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val customDao by lazy { AppDatabase.getInstance(context).customWidgetDao() }
    private val instanceDao by lazy { AppDatabase.getInstance(context).widgetInstanceDao() }

    private val widget = CustomWidget(
        id = "test-" + UUID.randomUUID().toString(),
        name = "Test widget",
        layoutJson = BuilderLayout(
            listOf(BuilderBlock(type = BuilderBlockType.TEXT, text = "Hi"))
        ).toJson()
    )
    private val placedId = 987_654
    private val placedDefinition: WidgetDefinition = WidgetRegistry.getAll().first()

    @Before
    fun seed() = runBlocking {
        customDao.upsert(widget)
        instanceDao.insertOrUpdate(
            WidgetInstance(
                appWidgetId = placedId,
                widgetTypeId = placedDefinition.id,
                configJson = """{"configured":true}"""
            )
        )
    }

    @After
    fun clean() = runBlocking {
        customDao.getAll().filter { it.name.startsWith("Test widget") }.forEach { customDao.delete(it) }
        instanceDao.deleteById(placedId)
        // The screen pushes custom definitions into the global registry; drop them so catalog-wide
        // audits (WidgetLayoutLintTest) do not pick up throwaway test designs.
        WidgetRegistry.replaceCustom(emptyList())
    }

    private fun launch(onEditInstance: (WidgetInstance, WidgetDefinition) -> Unit = { _, _ -> }) {
        compose.setContent {
            DotWidgetsTheme(accentOption = AccentOption.RED) {
                MyWidgetsScreen(
                    onCreate = {},
                    onEditCustom = {},
                    onEditInstance = onEditInstance
                )
            }
        }
    }

    private fun openRowMenu(rowTag: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag(rowTag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(
            hasTestTag("my_widgets_menu") and hasAnyAncestor(hasTestTag(rowTag)),
            useUnmergedTree = true
        ).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun clickMenuAction(actionTag: String) {
        compose.onNode(hasTestTag(actionTag), useUnmergedTree = true).performClick()
    }

    @Test
    fun `saved widget can be duplicated`() {
        launch()
        openRowMenu("my_widget_saved_${widget.id}")
        clickMenuAction("my_widgets_duplicate")

        compose.waitUntil(5_000) {
            runBlocking { customDao.getAll() }.any { it.name == "Test widget copy" }
        }
        val copy = runBlocking { customDao.getAll() }.first { it.name == "Test widget copy" }
        assertEquals(widget.layoutJson, copy.layoutJson)
        assertTrue(copy.id != widget.id)
    }

    @Test
    fun `placed built-in widget opens the instance editor`() {
        var edited: Pair<WidgetInstance, WidgetDefinition>? = null
        launch { instance, definition -> edited = instance to definition }

        openRowMenu("my_widget_placed_$placedId")
        clickMenuAction("my_widgets_edit")

        assertEquals(placedId, edited?.first?.appWidgetId)
        assertEquals(placedDefinition.id, edited?.second?.id)
    }
}
