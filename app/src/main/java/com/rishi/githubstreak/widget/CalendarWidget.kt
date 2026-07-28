package com.rishi.githubstreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rishi.githubstreak.MainActivity
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.graph.ContributionGraphRenderer
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.roundToInt

/** Home-screen version of the GitHub contribution calendar. */
class CalendarWidget : GlanceAppWidget() {

    // Exact sizing lets the grid pick how many weeks it can show for the widget's real size.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = WidgetConfigStore.read(context, id)
        val data = ProfileRepository.get(context).snapshot()
        val profile = data.resolveForWidget(config.profileId)

        provideContent {
            CalendarWidgetContent(
                profile = profile,
                colors = WidgetColors(config.theme),
                dark = config.theme.isDark(context),
                showHeader = config.showLabels,
            )
        }
    }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

private const val PADDING_DP = 10f
private const val HEADER_DP = 20f
private const val HEADER_GAP_DP = 6f
private const val FOOTER_DP = 14f

/**
 * A year of square cells is only 7/53 as tall as it is wide, so a widget taller than this has room
 * left over. Spend it on a streak line and the level legend instead of leaving it blank.
 */
private const val FOOTER_MIN_AVAILABLE_DP = 84f

@Composable
private fun CalendarWidgetContent(
    profile: Profile?,
    colors: WidgetColors,
    dark: Boolean,
    showHeader: Boolean,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(colors.backgroundRes))
            .padding(PADDING_DP.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (profile == null) {
            EmptyWidgetBody(colors = colors, message = "Tap to choose a GitHub profile")
        } else {
            val availableHeightDp = LocalSize.current.height.value - PADDING_DP * 2 -
                if (showHeader) HEADER_DP + HEADER_GAP_DP else 0f
            val showFooter = showHeader && availableHeightDp >= FOOTER_MIN_AVAILABLE_DP

            Column(modifier = GlanceModifier.fillMaxSize()) {
                if (showHeader) {
                    CalendarHeader(profile = profile, colors = colors)
                    Spacer(modifier = GlanceModifier.height(HEADER_GAP_DP.dp))
                }
                Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ContributionGraphImage(
                        profile = profile,
                        colors = colors,
                        dark = dark,
                        showLabels = showHeader,
                        heightDp = availableHeightDp - if (showFooter) FOOTER_DP else 0f,
                    )
                }
                if (showFooter) {
                    CalendarFooter(profile = profile, colors = colors)
                }
            }
        }
    }
}

@Composable
private fun CalendarFooter(profile: Profile, colors: WidgetColors) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(FOOTER_DP.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = footerLine(profile),
            maxLines = 1,
            style = TextStyle(color = colors.fgMuted, fontSize = 10.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        colors.levels.forEach { level ->
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .padding(start = 2.dp)
                    .cornerRadius(2.dp)
                    .background(level),
            ) {}
        }
    }
}

private fun footerLine(profile: Profile): String {
    val streak = if (profile.streakDays == 1) {
        "1 day streak"
    } else {
        "${profile.streakDays} day streak"
    }
    return if (profile.longestStreak > 0) "$streak · best ${profile.longestStreak}" else streak
}

@Composable
private fun CalendarHeader(profile: Profile, colors: WidgetColors) {
    WidgetHeader(
        username = profile.username,
        colors = colors,
        profileId = profile.id,
        trailing = profile.totalContributions
            .takeIf { it > 0 }
            ?.let { total -> "${formatCount(total)} contributions" },
    )
}

@Composable
private fun GraphFallback(message: String, colors: WidgetColors) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, style = TextStyle(color = colors.fgMuted, fontSize = 11.sp))
    }
}

@Composable
private fun ContributionGraphImage(
    profile: Profile,
    colors: WidgetColors,
    dark: Boolean,
    showLabels: Boolean,
    heightDp: Float,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val availableWidthDp = LocalSize.current.width.value - PADDING_DP * 2

    val bitmap = if (availableWidthDp <= 0f || heightDp <= 0f || profile.calendar.isEmpty) {
        null
    } else {
        ContributionGraphRenderer.renderBitmap(
            calendar = profile.calendar,
            endDate = LocalDate.now(),
            widthPx = (availableWidthDp * density).roundToInt(),
            heightPx = (heightDp * density).roundToInt(),
            density = density,
            dark = dark,
            showMonthLabels = showLabels,
            showWeekdayLabels = showLabels,
        )
    }

    if (bitmap == null) {
        GraphFallback(
            message = if (profile.calendar.isEmpty) "No contribution data yet" else "Widget is too small",
            colors = colors,
        )
    } else {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "${profile.username} contribution graph",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
}

internal fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)
