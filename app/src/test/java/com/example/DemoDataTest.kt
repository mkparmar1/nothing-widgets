package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.activity.StepRepository
import com.example.data.battery.BatteryHelper
import com.example.data.calendar.CalendarRepository
import com.example.data.demo.DemoData
import com.example.data.media.MediaRepository
import com.example.data.system.DeviceStatsRepository
import com.example.data.usage.ScreenTimeRepository
import com.example.data.weather.WeatherRepository
import com.example.ui.permissions.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoDataTest {

    @Test
    fun `snapshot scope provides realistic values without leaking into normal renders`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNull(DemoData.current())
        assertFalse(AppPermission.MediaAccess.isGranted(context))

        DemoData.withSnapshot {
            assertEquals(76, BatteryHelper.getBatteryStatus(context).level)
            assertEquals("Mumbai", WeatherRepository.currentPlace(context).name)
            assertEquals(27, WeatherRepository.getWeatherInstant(context).temperatureC)
            assertEquals(3, CalendarRepository.today(context)?.size)
            assertEquals(3, CalendarRepository.upcoming(context, 7)?.size)
            assertEquals("Midnight City", MediaRepository.nowPlaying(context)?.title)
            assertEquals(6_240, StepRepository.todaySteps(context))
            assertEquals(7, ScreenTimeRepository.current(context)?.lastSevenDaysMs?.size)
            assertEquals(8, DeviceStatsRepository.batteryHistory(context).size)
            assertTrue(AppPermission.MediaAccess.isGranted(context))
        }

        assertNull(DemoData.current())
        assertFalse(AppPermission.MediaAccess.isGranted(context))
    }
}
