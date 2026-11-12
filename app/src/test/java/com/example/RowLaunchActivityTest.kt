package com.example

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.widgets.collection.CollectionWidgetService
import com.example.widgets.collection.RowAction
import com.example.widgets.collection.RowLaunchActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RowLaunchActivityTest {

    private fun launch(action: RowAction, data: String?) =
        Robolectric.buildActivity(
            RowLaunchActivity::class.java,
            Intent(ApplicationProvider.getApplicationContext(), RowLaunchActivity::class.java)
                .putExtra(CollectionWidgetService.EXTRA_ROW_ACTION, action.name)
                .apply { if (data != null) setData(Uri.parse(data)) }
        ).setup().get()

    @Test
    fun `favourite row opens the dialler with the number`() {
        val activity = launch(RowAction.CALL, "tel:%2B911234567890")
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL, started.action)
        assertEquals("tel:%2B911234567890", started.dataString)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `agenda row opens the calendar at the event time`() {
        val activity = launch(RowAction.OPEN_CALENDAR, "content://com.android.calendar/time/1700000000000")
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("content://com.android.calendar/time/1700000000000", started.dataString)
    }

    @Test
    fun `other rows open nothing`() {
        val activity = launch(RowAction.TOGGLE_TODO, null)
        assertNull(shadowOf(activity).nextStartedActivity)
        assertTrue(activity.isFinishing)
    }
}
