package com.rishi.githubstreak.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

/** A widget instance the user has actually placed on a home screen. */
data class PlacedWidget(
    val appWidgetId: Int,
    val kind: WidgetKind,
    val config: WidgetConfig,
)

object WidgetInventory {

    suspend fun load(context: Context): List<PlacedWidget> {
        val manager = GlanceAppWidgetManager(context)

        return WidgetKind.entries.flatMap { kind ->
            manager.getGlanceIds(kind.widgetClass).map { glanceId ->
                PlacedWidget(
                    appWidgetId = manager.getAppWidgetId(glanceId),
                    kind = kind,
                    config = WidgetConfigStore.read(context, glanceId),
                )
            }
        }.sortedBy { it.appWidgetId }
    }

    suspend fun apply(context: Context, appWidgetId: Int, kind: WidgetKind, config: WidgetConfig) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        WidgetConfigStore.write(context, glanceId, config)
        kind.newWidget().update(context, glanceId)
    }

    /** Redraws every placed widget of both kinds. */
    suspend fun refreshAll(context: Context) {
        StreakWidget().updateAll(context)
        CalendarWidget().updateAll(context)
    }

    fun canPin(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun requestPin(context: Context, kind: WidgetKind) {
        AppWidgetManager.getInstance(context)
            .requestPinAppWidget(kind.receiver(context), null, null)
    }
}
