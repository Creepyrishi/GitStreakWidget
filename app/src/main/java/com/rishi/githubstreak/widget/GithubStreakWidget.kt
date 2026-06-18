package com.rishi.githubstreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rishi.githubstreak.MainActivity
import com.rishi.githubstreak.data.AppState
import com.rishi.githubstreak.data.SettingsRepository
import com.rishi.githubstreak.data.StreakStatus
import com.rishi.githubstreak.worker.RefreshStreakWorker
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.first

class GithubStreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = SettingsRepository.get(context).appState.first()

        provideContent {
            GithubStreakWidgetContent(state = state)
        }
    }
}

class GithubStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GithubStreakWidget()
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        RefreshStreakWorker.refreshNow(context)
    }
}

@Composable
private fun GithubStreakWidgetContent(state: AppState) {
    val palette = paletteFor(state.status)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .padding(14.dp),
    ) {
        Text(
            text = if (state.username.isBlank()) "GitHub Streak" else "@${state.username}",
            style = TextStyle(
                color = palette.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = mascotFor(state),
            style = TextStyle(
                color = palette.accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Text(
            text = state.streakDays.toString(),
            style = TextStyle(
                color = palette.primary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Text(
            text = statusLine(state),
            style = TextStyle(
                color = palette.text,
                fontSize = 12.sp,
            ),
        )

        Text(
            text = lastUpdatedLine(state.lastUpdatedMillis),
            style = TextStyle(
                color = palette.text,
                fontSize = 11.sp,
            ),
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (state.username.isBlank()) {
            Button(
                text = "Open",
                onClick = actionStartActivity<MainActivity>(),
            )
        } else {
            Button(
                text = "Refresh",
                onClick = actionRunCallback<RefreshWidgetAction>(),
            )
        }
    }
}

private data class WidgetPalette(
    val background: ColorProvider,
    val primary: ColorProvider,
    val accent: ColorProvider,
    val text: ColorProvider,
)

private fun paletteFor(status: StreakStatus): WidgetPalette = when (status) {
    StreakStatus.ACTIVE_TODAY -> WidgetPalette(
        background = ColorProvider(Color(0xFFFFF0C2)),
        primary = ColorProvider(Color(0xFF225B34)),
        accent = ColorProvider(Color(0xFFE58A2F)),
        text = ColorProvider(Color(0xFF554B3D)),
    )
    StreakStatus.ACTIVE_YESTERDAY -> WidgetPalette(
        background = ColorProvider(Color(0xFFFFF7DC)),
        primary = ColorProvider(Color(0xFF6A4A00)),
        accent = ColorProvider(Color(0xFFB86B00)),
        text = ColorProvider(Color(0xFF5F5548)),
    )
    StreakStatus.RESET -> WidgetPalette(
        background = ColorProvider(Color(0xFFEFF2F8)),
        primary = ColorProvider(Color(0xFF3D4A62)),
        accent = ColorProvider(Color(0xFF6D7890)),
        text = ColorProvider(Color(0xFF4D5668)),
    )
    StreakStatus.ERROR -> WidgetPalette(
        background = ColorProvider(Color(0xFFFFE8E0)),
        primary = ColorProvider(Color(0xFF8A2F20)),
        accent = ColorProvider(Color(0xFFB34A35)),
        text = ColorProvider(Color(0xFF67473F)),
    )
    StreakStatus.NEEDS_USERNAME,
    StreakStatus.PENDING,
    -> WidgetPalette(
        background = ColorProvider(Color(0xFFFFF9EA)),
        primary = ColorProvider(Color(0xFF2F6F3E)),
        accent = ColorProvider(Color(0xFF9B6A20)),
        text = ColorProvider(Color(0xFF5E564A)),
    )
}

private fun mascotFor(state: AppState): String {
    val secondFrame = state.lastUpdatedMillis > 0L && (state.lastUpdatedMillis / 1000L) % 2L == 0L
    return when (state.status) {
        StreakStatus.NEEDS_USERNAME -> if (secondFrame) "(?o?)" else "(?_?)"
        StreakStatus.PENDING -> if (secondFrame) "(._.)" else "(. .)"
        StreakStatus.ACTIVE_TODAY -> if (secondFrame) "(^o^)" else "(^_^)"
        StreakStatus.ACTIVE_YESTERDAY -> if (secondFrame) "(o_o;)" else "(o_o)"
        StreakStatus.RESET -> if (secondFrame) "(-_-)" else "(z_z)"
        StreakStatus.ERROR -> if (secondFrame) "(x_x)" else "(!)"
    }
}

private fun statusLine(state: AppState): String = when (state.status) {
    StreakStatus.NEEDS_USERNAME -> "Set username"
    StreakStatus.PENDING -> "Waiting refresh"
    StreakStatus.ACTIVE_TODAY -> if (state.streakDays == 1) "active day" else "active days"
    StreakStatus.ACTIVE_YESTERDAY -> "1 day left"
    StreakStatus.RESET -> "streak reset"
    StreakStatus.ERROR -> "refresh failed"
}

private fun lastUpdatedLine(millis: Long): String {
    if (millis <= 0L) return "Not updated yet"
    return "Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))}"
}
