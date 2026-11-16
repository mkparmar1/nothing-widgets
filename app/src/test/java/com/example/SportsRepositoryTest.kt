package com.example

import com.example.data.sports.SportsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SportsRepositoryTest {

    @Test
    fun `sports feed accepts snapshot and league table`() {
        val feed = SportsRepository.parseFeed(
            """{
                "team":"Mumbai","opponent":"Delhi","league":"League",
                "homeScore":2,"awayScore":1,"live":true,"kickoffMs":1234,"form":"WDL?",
                "table":[{"team":"Mumbai","points":7,"played":3}]
            }"""
        )

        assertNotNull(feed)
        assertEquals("Mumbai", feed?.snapshot?.team)
        assertEquals(2, feed?.snapshot?.homeScore)
        assertEquals(listOf('W', 'D', 'L'), feed?.snapshot?.form)
        assertEquals(7, feed?.table?.single()?.points)
    }

    @Test
    fun `invalid sports feed is rejected`() {
        assertNull(SportsRepository.parseFeed("""{"message":"not a sports feed"}"""))
        assertNull(SportsRepository.parseFeed("not json"))
    }

    @Test
    fun `next race uses only F1 data and never wraps to a past race`() {
        val beforeFinalRaces = date(2026, Calendar.NOVEMBER, 15)
        val afterSeason = date(2026, Calendar.DECEMBER, 7)

        val next = SportsRepository.nextRaceForDate("F1", beforeFinalRaces)
        assertEquals("Las Vegas GP", next.name)
        assertEquals(6, next.daysAway)

        val unavailable = SportsRepository.nextRaceForDate("MotoGP", beforeFinalRaces)
        assertEquals("Schedule unavailable", unavailable.name)

        val nextSeason = SportsRepository.nextRaceForDate("F1", afterSeason)
        assertEquals("2027 season", nextSeason.name)
        assertEquals("TBA", nextSeason.date)
    }

    private fun date(year: Int, month: Int, day: Int) =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.time
}
