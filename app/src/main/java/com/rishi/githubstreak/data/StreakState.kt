package com.rishi.githubstreak.data

import java.time.LocalDate

data class ContributionDay(
    val date: LocalDate,
    val active: Boolean,
)

enum class StreakStatus {
    NEEDS_USERNAME,
    PENDING,
    ACTIVE_TODAY,
    ACTIVE_YESTERDAY,
    RESET,
    ERROR,
}

data class StreakResult(
    val username: String,
    val streakDays: Int,
    val status: StreakStatus,
    val anchorDate: LocalDate?,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
)

data class AppState(
    val username: String = "",
    val streakDays: Int = 0,
    val status: StreakStatus = StreakStatus.NEEDS_USERNAME,
    val anchorDate: LocalDate? = null,
    val lastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
)
