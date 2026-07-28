package com.rishi.githubstreak.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionCalendarTest {

    private val start = LocalDate.parse("2026-07-01")

    @Test
    fun `fills the days github left out of the response`() {
        val calendar = ContributionCalendar.fromDays(
            listOf(
                ContributionDay(start, count = 5, level = 3),
                ContributionDay(start.plusDays(3), count = 1, level = 1),
            ),
        )

        assertEquals(start, calendar.startDate)
        assertEquals(4, calendar.levels.size)
        assertEquals(listOf(3, 0, 0, 1), calendar.levels)
    }

    @Test
    fun `keeps only the most recent window`() {
        val days = (0 until 500).map { offset ->
            ContributionDay(start.plusDays(offset.toLong()), count = 1, level = 1)
        }

        val calendar = ContributionCalendar.fromDays(days, maxDays = CALENDAR_DAYS)

        assertEquals(CALENDAR_DAYS, calendar.levels.size)
        assertEquals(start.plusDays(499), calendar.endDate)
    }

    @Test
    fun `a day with contributions but no level still shows as level one`() {
        val calendar = ContributionCalendar.fromDays(
            listOf(ContributionDay(start, count = 7, level = 0)),
        )

        assertEquals(1, calendar.levelOn(start))
    }

    @Test
    fun `survives a round trip through storage`() {
        val original = ContributionCalendar.fromDays(
            (0 until 40).map { offset ->
                ContributionDay(start.plusDays(offset.toLong()), count = offset, level = offset % 5)
            },
        )

        val restored = ContributionCalendar.decode(
            startDate = original.startDate.toString(),
            encodedLevels = original.encodeLevels(),
        )

        assertEquals(original, restored)
    }

    @Test
    fun `reports which dates it actually covers`() {
        val calendar = ContributionCalendar.fromDays(
            listOf(
                ContributionDay(start, count = 1, level = 1),
                ContributionDay(start.plusDays(2), count = 1, level = 1),
            ),
        )

        assertTrue(calendar.hasDataFor(start.plusDays(1)))
        assertFalse(calendar.hasDataFor(start.minusDays(1)))
        assertFalse(calendar.hasDataFor(start.plusDays(3)))
        assertEquals(0, calendar.levelOn(start.plusDays(99)))
    }

    @Test
    fun `an empty response yields an empty calendar`() {
        assertTrue(ContributionCalendar.fromDays(emptyList()).isEmpty)
        assertTrue(ContributionCalendar.decode(null, null).isEmpty)
        assertTrue(ContributionCalendar.decode("not-a-date", "012").isEmpty)
    }
}
