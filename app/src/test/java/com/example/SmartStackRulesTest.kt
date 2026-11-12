package com.example

import com.example.widgets.base.Category
import com.example.widgets.collection.SmartStackRules
import com.example.widgets.collection.StackMode
import com.example.widgets.collection.StackSmartSignals
import com.example.widgets.collection.StackTimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartStackRulesTest {

    private val cards = listOf("clock", "calendar", "battery")
    private val categories = mapOf(
        "clock" to Category.CLOCK,
        "calendar" to Category.CALENDAR,
        "battery" to Category.BATTERY
    )
    private val slots = mapOf(
        StackTimeSlot.MORNING to "clock",
        StackTimeSlot.DAY to "calendar",
        StackTimeSlot.EVENING to "battery",
        StackTimeSlot.NIGHT to "clock"
    )

    @Test
    fun timeSlotsCoverTheWholeDayWithoutGaps() {
        assertEquals(StackTimeSlot.NIGHT, StackTimeSlot.forHour(4))
        assertEquals(StackTimeSlot.MORNING, StackTimeSlot.forHour(5))
        assertEquals(StackTimeSlot.MORNING, StackTimeSlot.forHour(10))
        assertEquals(StackTimeSlot.DAY, StackTimeSlot.forHour(11))
        assertEquals(StackTimeSlot.DAY, StackTimeSlot.forHour(16))
        assertEquals(StackTimeSlot.EVENING, StackTimeSlot.forHour(17))
        assertEquals(StackTimeSlot.EVENING, StackTimeSlot.forHour(21))
        assertEquals(StackTimeSlot.NIGHT, StackTimeSlot.forHour(22))
        assertEquals(StackTimeSlot.NIGHT, StackTimeSlot.forHour(23))
    }

    @Test
    fun nextBoundaryIsStrictlyInTheFutureAndUsesLocalSlotTimes() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 5, 10, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expected = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        assertEquals(expected, StackTimeSlot.nextBoundaryMillis(now))

        val atBoundary = Calendar.getInstance().apply {
            timeInMillis = expected
        }.timeInMillis
        val next = Calendar.getInstance().apply {
            timeInMillis = expected
            set(Calendar.HOUR_OF_DAY, 17)
        }.timeInMillis
        assertEquals(next, StackTimeSlot.nextBoundaryMillis(atBoundary))
    }

    @Test
    fun manualAndTimeOfDayModesSelectConfiguredCardsWithSafeFallbacks() {
        assertEquals(
            "clock",
            SmartStackRules.selectCard(cards, StackMode.MANUAL, 12, slots, categories)
        )
        assertEquals(
            "calendar",
            SmartStackRules.selectCard(cards, StackMode.TIME_OF_DAY, 12, slots, categories)
        )
        assertEquals(
            "clock",
            SmartStackRules.selectCard(cards, StackMode.TIME_OF_DAY, 4, emptyMap(), categories)
        )
        assertNull(SmartStackRules.selectCard(emptyList(), StackMode.SMART, 12, slots, categories))
    }

    @Test
    fun smartModePrioritizesUpcomingEventsThenLowOrChargingBattery() {
        assertEquals(
            "calendar",
            SmartStackRules.selectCard(
                cards, StackMode.SMART, 18, slots, categories,
                StackSmartSignals(minutesUntilNextEvent = 30, batteryLevelPercent = 5)
            )
        )
        assertEquals(
            "battery",
            SmartStackRules.selectCard(
                cards, StackMode.SMART, 12, slots, categories,
                StackSmartSignals(minutesUntilNextEvent = 31, batteryLevelPercent = 19)
            )
        )
        assertEquals(
            "battery",
            SmartStackRules.selectCard(
                cards, StackMode.SMART, 12, slots, categories,
                StackSmartSignals(batteryLevelPercent = 20, isCharging = true)
            )
        )
        assertEquals(
            "calendar",
            SmartStackRules.selectCard(
                cards, StackMode.SMART, 12, slots, categories,
                StackSmartSignals(batteryLevelPercent = 20)
            )
        )
    }
}
