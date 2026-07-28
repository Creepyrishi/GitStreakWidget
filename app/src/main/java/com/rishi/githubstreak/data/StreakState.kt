package com.rishi.githubstreak.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Number of colour buckets GitHub uses on the contribution calendar (0 = empty). */
const val MAX_CONTRIBUTION_LEVEL = 4

/** Days GitHub keeps on the public calendar (53 weeks). */
const val CALENDAR_DAYS = 53 * 7

data class ContributionDay(
    val date: LocalDate,
    val count: Int = 0,
    val level: Int = 0,
) {
    val active: Boolean get() = count > 0 || level > 0
}

enum class StreakStatus {
    PENDING,
    ACTIVE_TODAY,
    ACTIVE_YESTERDAY,
    RESET,
    ERROR,
}

/**
 * Contribution levels for a contiguous run of days, stored as one digit per day so the whole
 * calendar fits in a short string inside DataStore.
 */
data class ContributionCalendar(
    val startDate: LocalDate? = null,
    val levels: List<Int> = emptyList(),
) {
    val isEmpty: Boolean get() = startDate == null || levels.isEmpty()

    val endDate: LocalDate? get() = startDate?.plusDays((levels.size - 1).toLong())

    fun levelOn(date: LocalDate): Int {
        val start = startDate ?: return 0
        val index = ChronoUnit.DAYS.between(start, date)
        if (index < 0 || index >= levels.size) return 0
        return levels[index.toInt()]
    }

    fun hasDataFor(date: LocalDate): Boolean {
        val start = startDate ?: return false
        val index = ChronoUnit.DAYS.between(start, date)
        return index >= 0 && index < levels.size
    }

    fun encodeLevels(): String = levels.joinToString(separator = "") { level ->
        ('0' + level.coerceIn(0, MAX_CONTRIBUTION_LEVEL)).toString()
    }

    companion object {
        val Empty = ContributionCalendar()

        fun fromDays(days: List<ContributionDay>, maxDays: Int = CALENDAR_DAYS): ContributionCalendar {
            if (days.isEmpty()) return Empty

            val levelByDate = days.associate { day ->
                day.date to day.level.coerceIn(0, MAX_CONTRIBUTION_LEVEL)
                    .let { level -> if (level == 0 && day.count > 0) 1 else level }
            }

            val last = levelByDate.keys.max()
            val first = maxOf(levelByDate.keys.min(), last.minusDays((maxDays - 1).toLong()))
            val span = ChronoUnit.DAYS.between(first, last).toInt() + 1

            return ContributionCalendar(
                startDate = first,
                levels = List(span) { offset -> levelByDate[first.plusDays(offset.toLong())] ?: 0 },
            )
        }

        fun decode(startDate: String?, encodedLevels: String?): ContributionCalendar {
            val start = startDate
                ?.takeIf { it.isNotBlank() }
                ?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() }
                ?: return Empty
            val levels = encodedLevels.orEmpty()
                .map { char -> (char - '0').coerceIn(0, MAX_CONTRIBUTION_LEVEL) }
            if (levels.isEmpty()) return Empty
            return ContributionCalendar(start, levels)
        }
    }
}

/** Everything the app knows about one tracked GitHub account. */
data class Profile(
    val id: String,
    val username: String,
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val totalContributions: Int = 0,
    val status: StreakStatus = StreakStatus.PENDING,
    val anchorDate: LocalDate? = null,
    val lastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val calendar: ContributionCalendar = ContributionCalendar.Empty,
)

/** Result of recalculating a profile from a freshly fetched calendar. */
data class StreakSummary(
    val streakDays: Int,
    val longestStreak: Int,
    val totalContributions: Int,
    val status: StreakStatus,
    val anchorDate: LocalDate?,
)

data class AppData(
    val profiles: List<Profile> = emptyList(),
    val loaded: Boolean = false,
) {
    fun byId(id: String?): Profile? = id?.let { wanted -> profiles.firstOrNull { it.id == wanted } }

    /**
     * Profile a widget should render. An unbound widget falls back to the first profile so it is
     * useful straight away; a widget bound to a deleted profile stays explicitly unresolved.
     */
    fun resolveForWidget(boundId: String?): Profile? =
        if (boundId == null) profiles.firstOrNull() else byId(boundId)
}
