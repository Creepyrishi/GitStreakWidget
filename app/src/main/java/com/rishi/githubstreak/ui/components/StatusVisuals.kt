package com.rishi.githubstreak.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.StreakStatus
import com.rishi.githubstreak.ui.theme.GithubPalette
import com.rishi.githubstreak.ui.theme.LocalGithubPalette

data class StatusVisuals(
    val label: String,
    val message: String,
    val foreground: Color,
    val background: Color,
)

fun statusVisuals(profile: Profile, palette: GithubPalette): StatusVisuals = when (profile.status) {
    StreakStatus.ACTIVE_TODAY -> StatusVisuals(
        label = "Active today",
        message = "Today already counts. The streak is safe.",
        foreground = palette.success,
        background = palette.successSubtle,
    )
    StreakStatus.ACTIVE_YESTERDAY -> StatusVisuals(
        label = "At risk",
        message = "No public activity today yet — yesterday is still holding the streak.",
        foreground = palette.attention,
        background = palette.attentionSubtle,
    )
    StreakStatus.RESET -> StatusVisuals(
        label = "No streak",
        message = "Today and yesterday are both empty, so the streak is 0.",
        foreground = palette.fgMuted,
        background = palette.canvasSubtle,
    )
    StreakStatus.PENDING -> StatusVisuals(
        label = "Pending",
        message = "Waiting for the first refresh.",
        foreground = palette.accent,
        background = palette.accentSubtle,
    )
    StreakStatus.ERROR -> StatusVisuals(
        label = "Error",
        message = profile.errorMessage ?: "Refresh failed.",
        foreground = palette.danger,
        background = palette.dangerSubtle,
    )
}

@Composable
fun rememberStatusVisuals(profile: Profile): StatusVisuals =
    statusVisuals(profile, LocalGithubPalette.current)
