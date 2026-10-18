package com.example.widgets.base

import android.content.Context
import com.example.data.activity.StepRepository
import com.example.data.usage.ScreenTimeRepository

/**
 * Some widget data can't be read synchronously while drawing (sensors, system stats). Before a
 * refresh, read those sources once — and only if a widget that shows them is placed.
 */
object WidgetDataPrefetch {

    private val STEP_WIDGETS = setOf("prod.step.counter", "sensor.fitness.rings")
    private const val SCREEN_TIME_WIDGET = "battery.screen.on"

    suspend fun prefetch(context: Context, placedTypeIds: Set<String>) {
        if (placedTypeIds.any { it in STEP_WIDGETS }) StepRepository.refresh(context)
        if (SCREEN_TIME_WIDGET in placedTypeIds) runCatching { ScreenTimeRepository.refresh(context) }
    }
}
