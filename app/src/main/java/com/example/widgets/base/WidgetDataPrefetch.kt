package com.example.widgets.base

import android.content.Context
import com.example.data.activity.StepRepository
import com.example.data.system.DeviceStatsRepository
import com.example.data.sensor.CompassRepository
import com.example.data.usage.ScreenTimeRepository

/**
 * Some widget data can't be read synchronously while drawing (sensors, system stats). Before a
 * refresh, read those sources once — and only if a widget that shows them is placed.
 */
object WidgetDataPrefetch {

    private val STEP_WIDGETS = setOf("prod.step.counter", "sensor.fitness.rings")
    private const val SCREEN_TIME_WIDGET = "battery.screen.on"
    private const val COMPASS_WIDGET = "sensor.compass.heading"

    suspend fun prefetch(context: Context, placedTypeIds: Set<String>) {
        // Cheap samples that build history over time; taken whenever their widget is placed.
        if ("sys.network.speed" in placedTypeIds) DeviceStatsRepository.sampleNetwork(context)
        if ("battery.history.24h" in placedTypeIds) DeviceStatsRepository.sampleBattery(context)
        if ("sys.mobile.data" in placedTypeIds) runCatching { DeviceStatsRepository.refreshMobileData(context) }
        if ("sys.top.apps" in placedTypeIds) runCatching { DeviceStatsRepository.refreshTopApps(context) }
        if (placedTypeIds.any { it in STEP_WIDGETS }) StepRepository.refresh(context)
        if (SCREEN_TIME_WIDGET in placedTypeIds) runCatching { ScreenTimeRepository.refresh(context) }
        if (COMPASS_WIDGET in placedTypeIds) runCatching { CompassRepository.refresh(context) }
    }
}
