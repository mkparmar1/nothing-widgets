package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.weather.WeatherPlace
import com.example.data.weather.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherRepositoryTest {

    @Test
    fun `clearing chosen place preserves weather refresh preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("dot_widgets_weather", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        WeatherRepository.setNetworkEnabled(context, false)
        WeatherRepository.setRefreshMinutes(context, 60)
        WeatherRepository.setPlace(
            context,
            WeatherPlace("Test City", 1.0, 2.0, "UTC")
        )
        WeatherRepository.setPlace(context, null)

        assertFalse(WeatherRepository.hasChosenPlace(context))
        assertFalse(WeatherRepository.isNetworkEnabled(context))
        assertEquals(60, WeatherRepository.refreshMinutes(context))
    }
}
