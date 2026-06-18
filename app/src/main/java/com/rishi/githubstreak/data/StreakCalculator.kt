package com.rishi.githubstreak.data

import java.time.Clock
import java.time.LocalDate

object StreakCalculator {
    fun calculate(
        username: String,
        days: List<ContributionDay>,
        clock: Clock = Clock.systemDefaultZone(),
    ): StreakResult {
        val activeByDate = days.associate { it.date to it.active }
        val today = LocalDate.now(clock)
        val yesterday = today.minusDays(1)

        val anchorDate = when {
            activeByDate[today] == true -> today
            activeByDate[yesterday] == true -> yesterday
            else -> null
        }

        val status = when (anchorDate) {
            today -> StreakStatus.ACTIVE_TODAY
            yesterday -> StreakStatus.ACTIVE_YESTERDAY
            else -> StreakStatus.RESET
        }

        return StreakResult(
            username = username,
            streakDays = anchorDate?.let { countBackwardsFrom(it, activeByDate) } ?: 0,
            status = status,
            anchorDate = anchorDate,
        )
    }

    private fun countBackwardsFrom(
        startDate: LocalDate,
        activeByDate: Map<LocalDate, Boolean>,
    ): Int {
        var date = startDate
        var count = 0

        while (activeByDate[date] == true) {
            count += 1
            date = date.minusDays(1)
        }

        return count
    }
}
