package com.rishi.githubstreak.widget

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import com.rishi.githubstreak.R
import com.rishi.githubstreak.ui.theme.GithubColorTokens

enum class WidgetThemeOption(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
    ;

    fun isDark(context: Context): Boolean = when (this) {
        SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun from(name: String?): WidgetThemeOption =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** The two widget flavours the app publishes. */
enum class WidgetKind(val title: String, val summary: String) {
    STREAK(
        title = "Streak",
        summary = "Current streak, longest streak and the last two weeks at a glance.",
    ),
    CALENDAR(
        title = "Contribution graph",
        summary = "The GitHub contribution calendar, sized to the widget.",
    ),
    ;

    fun newWidget(): GlanceAppWidget = when (this) {
        STREAK -> StreakWidget()
        CALENDAR -> CalendarWidget()
    }

    val widgetClass: Class<out GlanceAppWidget>
        get() = when (this) {
            STREAK -> StreakWidget::class.java
            CALENDAR -> CalendarWidget::class.java
        }

    fun receiver(context: Context): ComponentName = when (this) {
        STREAK -> ComponentName(context, StreakWidgetReceiver::class.java)
        CALENDAR -> ComponentName(context, CalendarWidgetReceiver::class.java)
    }

    companion object {
        fun fromReceiverClassName(className: String?): WidgetKind? = when (className) {
            StreakWidgetReceiver::class.java.name -> STREAK
            CalendarWidgetReceiver::class.java.name -> CALENDAR
            else -> null
        }
    }
}

data class WidgetConfig(
    val profileId: String? = null,
    val theme: WidgetThemeOption = WidgetThemeOption.SYSTEM,
    val showLabels: Boolean = true,
)

/** Per-widget-instance settings, stored in each widget's own Glance state. */
object WidgetConfigStore {
    private val ProfileIdKey = stringPreferencesKey("bound_profile_id")
    private val ThemeKey = stringPreferencesKey("widget_theme")
    private val ShowLabelsKey = booleanPreferencesKey("show_labels")

    fun read(preferences: Preferences): WidgetConfig = WidgetConfig(
        profileId = preferences[ProfileIdKey]?.takeIf { it.isNotBlank() },
        theme = WidgetThemeOption.from(preferences[ThemeKey]),
        showLabels = preferences[ShowLabelsKey] ?: true,
    )

    suspend fun read(context: Context, glanceId: GlanceId): WidgetConfig =
        read(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId))

    suspend fun write(context: Context, glanceId: GlanceId, config: WidgetConfig) {
        updateAppWidgetState(context, glanceId) { preferences ->
            val profileId = config.profileId
            if (profileId.isNullOrBlank()) {
                preferences.remove(ProfileIdKey)
            } else {
                preferences[ProfileIdKey] = profileId
            }
            preferences[ThemeKey] = config.theme.name
            preferences[ShowLabelsKey] = config.showLabels
        }
    }
}

/** Colours for one widget instance, honouring its light/dark/system override. */
class WidgetColors(private val theme: WidgetThemeOption) {

    val backgroundRes: Int = when (theme) {
        WidgetThemeOption.SYSTEM -> R.drawable.widget_background
        WidgetThemeOption.LIGHT -> R.drawable.widget_background_light
        WidgetThemeOption.DARK -> R.drawable.widget_background_dark
    }

    val fg = provider(R.color.gh_fg, GithubColorTokens.Light.FG, GithubColorTokens.Dark.FG)
    val fgMuted = provider(R.color.gh_fg_muted, GithubColorTokens.Light.FG_MUTED, GithubColorTokens.Dark.FG_MUTED)
    val fgSubtle = provider(R.color.gh_fg_subtle, GithubColorTokens.Light.FG_SUBTLE, GithubColorTokens.Dark.FG_SUBTLE)
    val success = provider(R.color.gh_success, GithubColorTokens.Light.SUCCESS, GithubColorTokens.Dark.SUCCESS)
    val accent = provider(R.color.gh_accent, GithubColorTokens.Light.ACCENT, GithubColorTokens.Dark.ACCENT)
    val danger = provider(R.color.gh_danger, GithubColorTokens.Light.DANGER, GithubColorTokens.Dark.DANGER)
    val attention =
        provider(R.color.gh_attention, GithubColorTokens.Light.ATTENTION, GithubColorTokens.Dark.ATTENTION)

    val levels: List<ColorProvider> = LEVEL_RES.mapIndexed { index, resId ->
        provider(resId, GithubColorTokens.Light.LEVELS[index], GithubColorTokens.Dark.LEVELS[index])
    }

    private fun provider(resId: Int, light: Int, dark: Int): ColorProvider = when (theme) {
        WidgetThemeOption.SYSTEM -> ColorProvider(resId)
        WidgetThemeOption.LIGHT -> ColorProvider(Color(light))
        WidgetThemeOption.DARK -> ColorProvider(Color(dark))
    }

    private companion object {
        val LEVEL_RES = intArrayOf(
            R.color.gh_level_0,
            R.color.gh_level_1,
            R.color.gh_level_2,
            R.color.gh_level_3,
            R.color.gh_level_4,
        )
    }
}
