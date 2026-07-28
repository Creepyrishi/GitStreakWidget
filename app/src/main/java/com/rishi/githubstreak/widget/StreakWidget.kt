package com.rishi.githubstreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rishi.githubstreak.MainActivity
import com.rishi.githubstreak.R
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.data.StreakStatus
import com.rishi.githubstreak.worker.RefreshStreakWorker
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

/** Compact widget: one profile's current streak plus the last seven days. */
class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = WidgetConfigStore.read(context, id)
        val data = ProfileRepository.get(context).snapshot()
        val profile = data.resolveForWidget(config.profileId)

        provideContent {
            StreakWidgetContent(profile = profile, colors = WidgetColors(config.theme))
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

/** Refreshes just the profile this widget shows, or everything when it is unbound. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        RefreshStreakWorker.refreshNow(context, parameters[ProfileIdParameter])
    }

    companion object {
        val ProfileIdParameter = ActionParameters.Key<String>("profileId")
    }
}

@Composable
private fun StreakWidgetContent(profile: Profile?, colors: WidgetColors) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(colors.backgroundRes))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (profile == null) {
            EmptyWidgetBody(
                colors = colors,
                message = "Tap to choose a GitHub profile",
            )
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetHeader(username = profile.username, colors = colors, profileId = profile.id)

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = profile.streakDays.toString(),
                        style = TextStyle(
                            color = streakColor(profile.status, colors),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    Text(
                        text = if (profile.streakDays == 1) "day streak" else "days streak",
                        style = TextStyle(color = colors.fgMuted, fontSize = 12.sp),
                    )
                }

                Text(
                    text = statusLine(profile),
                    maxLines = 1,
                    style = TextStyle(color = statusColor(profile.status, colors), fontSize = 11.sp),
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (profile.totalContributions > 0) {
                    Text(
                        text = "${formatCount(profile.totalContributions)} contributions",
                        maxLines = 1,
                        style = TextStyle(color = colors.fgMuted, fontSize = 11.sp),
                    )
                    Spacer(modifier = GlanceModifier.height(5.dp))
                }

                LastWeekStrip(profile = profile, colors = colors)

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = footerLine(profile),
                    maxLines = 1,
                    style = TextStyle(color = colors.fgSubtle, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
internal fun WidgetHeader(
    username: String,
    colors: WidgetColors,
    profileId: String,
    trailing: String? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_github),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.fgMuted),
            modifier = GlanceModifier.size(14.dp),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = username,
            maxLines = 1,
            style = TextStyle(color = colors.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                maxLines = 1,
                style = TextStyle(color = colors.fgSubtle, fontSize = 10.sp),
                modifier = GlanceModifier.padding(end = 6.dp),
            )
        }
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "Refresh",
            colorFilter = ColorFilter.tint(colors.fgSubtle),
            modifier = GlanceModifier
                .size(15.dp)
                .clickable(
                    actionRunCallback<RefreshWidgetAction>(
                        actionParametersOf(RefreshWidgetAction.ProfileIdParameter to profileId),
                    ),
                ),
        )
    }
}

@Composable
internal fun EmptyWidgetBody(colors: WidgetColors, message: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_github),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.fgMuted),
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = message,
            style = TextStyle(color = colors.fgMuted, fontSize = 12.sp),
        )
    }
}

/** Seven cells, oldest on the left, coloured with the GitHub contribution scale. */
@Composable
private fun LastWeekStrip(profile: Profile, colors: WidgetColors) {
    val today = LocalDate.now()

    Row(modifier = GlanceModifier.fillMaxWidth()) {
        (6 downTo 0).forEach { offset ->
            val date = today.minusDays(offset.toLong())
            val level = profile.calendar.levelOn(date).coerceIn(0, colors.levels.lastIndex)
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(12.dp)
                    .padding(end = if (offset == 0) 0.dp else 3.dp),
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(2.dp)
                        .background(colors.levels[level]),
                ) {}
            }
        }
    }
}

private fun streakColor(status: StreakStatus, colors: WidgetColors): ColorProvider = when (status) {
    StreakStatus.ACTIVE_TODAY -> colors.success
    StreakStatus.ACTIVE_YESTERDAY -> colors.attention
    StreakStatus.ERROR -> colors.danger
    else -> colors.fg
}

private fun statusColor(status: StreakStatus, colors: WidgetColors): ColorProvider = when (status) {
    StreakStatus.ACTIVE_TODAY -> colors.success
    StreakStatus.ACTIVE_YESTERDAY -> colors.attention
    StreakStatus.ERROR -> colors.danger
    else -> colors.fgMuted
}

private fun statusLine(profile: Profile): String = when (profile.status) {
    StreakStatus.PENDING -> "Waiting for first refresh"
    StreakStatus.ACTIVE_TODAY -> "Active today"
    StreakStatus.ACTIVE_YESTERDAY -> "Nothing today yet"
    StreakStatus.RESET -> "Streak reset"
    StreakStatus.ERROR -> profile.errorMessage ?: "Refresh failed"
}

private fun footerLine(profile: Profile): String {
    if (profile.lastUpdatedMillis <= 0L) return "Not updated yet"
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(profile.lastUpdatedMillis))
    val longest = profile.longestStreak
    return if (longest > 0) "Best $longest · $time" else "Updated $time"
}
