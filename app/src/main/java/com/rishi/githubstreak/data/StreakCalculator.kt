package com.rishi.githubstreak.data

import java.time.Clock
import java.time.LocalDate

object StreakCalculator {

    fun summarize(
        snapshot: ContributionSnapshot,
        clock: Clock = Clock.systemDefaultZone(),
    ): StreakSummary = summarize(snapshot.days, snapshot.reportedTotal, clock)

    fun summarize(
        days: List<ContributionDay>,
        reportedTotal: Int? = null,
        clock: Clock = Clock.systemDefaultZone(),
    ): StreakSummary {
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

        return StreakSummary(
            streakDays = anchorDate?.let { countBackwardsFrom(it, activeByDate) } ?: 0,
            longestStreak = longestRun(days),
            totalContributions = reportedTotal ?: days.sumOf { it.count },
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

    /** Longest run of consecutive active days anywhere in the fetched window. */
    private fun longestRun(days: List<ContributionDay>): Int {
        var longest = 0
        var current = 0
        var previousDate: LocalDate? = null

        days.sortedBy { it.date }.forEach { day ->
            current = when {
                !day.active -> 0
                previousDate?.plusDays(1) == day.date -> current + 1
                else -> 1
            }
            if (current > longest) longest = current
            previousDate = day.date
        }

        return longest
    }
}
