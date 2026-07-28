package com.rishi.githubstreak.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.parse("2026-07-28")
    private val clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC)

    private fun days(vararg activeOffsets: Int, window: Int = 30): List<ContributionDay> {
        val active = activeOffsets.toSet()
        return (window downTo 0).map { offset ->
            ContributionDay(
                date = today.minusDays(offset.toLong()),
                count = if (offset in active) 1 else 0,
                level = if (offset in active) 1 else 0,
            )
        }
    }

    @Test
    fun `counts the run ending today`() {
        val summary = StreakCalculator.summarize(days(0, 1, 2, 3), clock = clock)

        assertEquals(4, summary.streakDays)
        assertEquals(StreakStatus.ACTIVE_TODAY, summary.status)
        assertEquals(today, summary.anchorDate)
    }

    @Test
    fun `keeps the streak alive on a quiet today`() {
        val summary = StreakCalculator.summarize(days(1, 2, 3), clock = clock)

        assertEquals(3, summary.streakDays)
        assertEquals(StreakStatus.ACTIVE_YESTERDAY, summary.status)
        assertEquals(today.minusDays(1), summary.anchorDate)
    }

    @Test
    fun `resets once today and yesterday are both empty`() {
        val summary = StreakCalculator.summarize(days(2, 3, 4, 5), clock = clock)

        assertEquals(0, summary.streakDays)
        assertEquals(StreakStatus.RESET, summary.status)
        assertEquals(null, summary.anchorDate)
    }

    @Test
    fun `longest streak looks at the whole window, not just the current run`() {
        val summary = StreakCalculator.summarize(days(0, 1, 10, 11, 12, 13, 14), clock = clock)

        assertEquals(2, summary.streakDays)
        assertEquals(5, summary.longestStreak)
    }

    @Test
    fun `prefers the total github reports over the sum of cells`() {
        val summary = StreakCalculator.summarize(days(0, 1), reportedTotal = 3253, clock = clock)

        assertEquals(3253, summary.totalContributions)
    }

    @Test
    fun `falls back to summing counts when github omits the total`() {
        val counted = listOf(
            ContributionDay(today.minusDays(1), count = 4, level = 2),
            ContributionDay(today, count = 6, level = 3),
        )

        assertEquals(10, StreakCalculator.summarize(counted, clock = clock).totalContributions)
    }

    @Test
    fun `a gap in the data breaks the run instead of spanning it`() {
        val withGap = listOf(
            ContributionDay(today.minusDays(5), count = 1, level = 1),
            // days -4..-1 are missing entirely
            ContributionDay(today, count = 1, level = 1),
        )

        assertEquals(1, StreakCalculator.summarize(withGap, clock = clock).longestStreak)
    }
}
