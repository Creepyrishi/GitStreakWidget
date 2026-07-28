package com.rishi.githubstreak.graph

import com.rishi.githubstreak.data.CALENDAR_DAYS
import com.rishi.githubstreak.data.ContributionCalendar
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Geometry for a GitHub-style contribution grid: 7 rows (Sun..Sat) by N week columns, with the
 * most recent week on the right. Shared by the Compose renderer in the app and the bitmap
 * renderer used by the home-screen widget so both look identical.
 */
object ContributionGraphLayout {

    const val MAX_WEEKS = CALENDAR_DAYS / 7

    data class Cell(
        val left: Float,
        val top: Float,
        val size: Float,
        val level: Int,
    )

    data class Label(
        val x: Float,
        val y: Float,
        val text: String,
    )

    data class Layout(
        val cells: List<Cell>,
        val monthLabels: List<Label>,
        val weekdayLabels: List<Label>,
        val cornerRadius: Float,
        val textSize: Float,
        val width: Float,
        val height: Float,
        /** Width reserved for the Mon/Wed/Fri column, so a scrolling caller can pin it. */
        val weekdayGutter: Float,
    ) {
        val isEmpty: Boolean get() = cells.isEmpty()
    }

    /**
     * Fills the given box. Prefers showing the whole year by shrinking the cells, and only drops
     * week columns (oldest first) once a full year would render smaller than [MIN_FULL_YEAR_STEP_DP].
     * Labels are the first thing sacrificed when the box gets tight.
     */
    fun fit(
        calendar: ContributionCalendar,
        endDate: LocalDate,
        widthPx: Float,
        heightPx: Float,
        density: Float,
        showMonthLabels: Boolean = true,
        showWeekdayLabels: Boolean = true,
    ): Layout {
        if (widthPx <= 0f || heightPx <= 0f) return emptyLayout()

        val labelPasses = if (showMonthLabels || showWeekdayLabels) listOf(true, false) else listOf(false)

        labelPasses.forEach { withLabels ->
            val textSize = if (withLabels) LABEL_SP * density else 0f
            val monthGutter = if (withLabels && showMonthLabels) textSize * 1.6f else 0f
            val weekdayGutter = if (withLabels && showWeekdayLabels) textSize * 2.4f else 0f

            val availableWidth = widthPx - weekdayGutter
            val availableHeight = heightPx - monthGutter
            if (availableWidth <= 0f || availableHeight <= 0f) return@forEach

            // Labels earn their place only while the gutters stay a small share of the box.
            if (withLabels &&
                (weekdayGutter > widthPx * MAX_GUTTER_SHARE || monthGutter > heightPx * MAX_GUTTER_SHARE)
            ) {
                return@forEach
            }

            val stepFromHeight = availableHeight / ROWS
            val stepForFullYear = availableWidth / MAX_WEEKS
            val step: Float
            val weeks: Int

            if (stepForFullYear >= MIN_FULL_YEAR_STEP_DP * density) {
                step = min(stepFromHeight, stepForFullYear)
                weeks = MAX_WEEKS
            } else {
                step = stepFromHeight
                weeks = floor(availableWidth / step).toInt().coerceIn(1, MAX_WEEKS)
            }

            if (step <= 0f) return@forEach

            return build(
                calendar = calendar,
                endDate = endDate,
                weeks = weeks,
                step = step,
                density = density,
                textSize = textSize,
                monthGutter = monthGutter,
                weekdayGutter = weekdayGutter,
                boxWidth = widthPx,
                boxHeight = heightPx,
            )
        }

        return emptyLayout()
    }

    /** Fixed cell pitch; the caller scrolls the result. Used for the full year inside the app. */
    fun fixed(
        calendar: ContributionCalendar,
        endDate: LocalDate,
        stepPx: Float,
        weeks: Int = MAX_WEEKS,
        density: Float,
        showMonthLabels: Boolean = true,
        showWeekdayLabels: Boolean = true,
    ): Layout {
        val textSize = if (showMonthLabels || showWeekdayLabels) LABEL_SP * density else 0f
        val monthGutter = if (showMonthLabels) textSize * 1.6f else 0f
        val weekdayGutter = if (showWeekdayLabels) textSize * 2.4f else 0f
        val safeWeeks = weeks.coerceIn(1, MAX_WEEKS)

        return build(
            calendar = calendar,
            endDate = endDate,
            weeks = safeWeeks,
            step = stepPx,
            density = density,
            textSize = textSize,
            monthGutter = monthGutter,
            weekdayGutter = weekdayGutter,
            boxWidth = weekdayGutter + safeWeeks * stepPx,
            boxHeight = monthGutter + ROWS * stepPx,
        )
    }

    private fun build(
        calendar: ContributionCalendar,
        endDate: LocalDate,
        weeks: Int,
        step: Float,
        density: Float,
        textSize: Float,
        monthGutter: Float,
        weekdayGutter: Float,
        boxWidth: Float,
        boxHeight: Float,
    ): Layout {
        val gap = max(1f * density, step * GAP_RATIO)
        val cellSize = max(1f, step - gap)
        val gridHeight = monthGutter + ROWS * step - gap
        val top = monthGutter + max(0f, (boxHeight - gridHeight) / 2f)

        // Right-most column is the week containing endDate.
        val lastWeekStart = endDate.minusDays(sundayIndex(endDate).toLong())
        val cells = ArrayList<Cell>(weeks * ROWS)
        val monthLabels = ArrayList<Label>()
        var lastLabelledMonth = -1
        var lastLabelColumn = -MONTH_LABEL_MIN_GAP

        for (column in 0 until weeks) {
            val columnStart = lastWeekStart.minusWeeks((weeks - 1 - column).toLong())
            val x = weekdayGutter + column * step

            if (textSize > 0f &&
                columnStart.monthValue != lastLabelledMonth &&
                column - lastLabelColumn >= MONTH_LABEL_MIN_GAP &&
                column <= weeks - MONTH_LABEL_MIN_GAP
            ) {
                lastLabelledMonth = columnStart.monthValue
                lastLabelColumn = column
                monthLabels += Label(
                    x = x,
                    y = monthGutter - textSize * 0.45f,
                    text = columnStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                )
            }

            for (row in 0 until ROWS) {
                val date = columnStart.plusDays(row.toLong())
                if (date.isAfter(endDate) || !calendar.hasDataFor(date)) continue
                cells += Cell(
                    left = x,
                    top = top + row * step,
                    size = cellSize,
                    level = calendar.levelOn(date),
                )
            }
        }

        val weekdayLabels = if (textSize > 0f && weekdayGutter > 0f) {
            WEEKDAY_ROWS.map { (row, text) ->
                Label(
                    x = 0f,
                    y = top + row * step + cellSize * 0.5f + textSize * 0.36f,
                    text = text,
                )
            }
        } else {
            emptyList()
        }

        return Layout(
            cells = cells,
            monthLabels = monthLabels,
            weekdayLabels = weekdayLabels,
            cornerRadius = min(2f * density, cellSize * 0.28f),
            textSize = textSize,
            width = boxWidth,
            height = boxHeight,
            weekdayGutter = weekdayGutter,
        )
    }

    private fun emptyLayout() = Layout(emptyList(), emptyList(), emptyList(), 0f, 0f, 0f, 0f, 0f)

    /** GitHub columns run Sunday..Saturday; LocalDate uses Monday = 1. */
    private fun sundayIndex(date: LocalDate): Int = date.dayOfWeek.value % 7

    private const val ROWS = 7
    private const val GAP_RATIO = 0.16f
    private const val LABEL_SP = 9f

    /** Below this the whole year is unreadable mush, so crop columns instead. */
    private const val MIN_FULL_YEAR_STEP_DP = 3.5f

    /** Largest slice of the widget a label gutter may take before labels are dropped. */
    private const val MAX_GUTTER_SHARE = 0.3f
    private const val MONTH_LABEL_MIN_GAP = 3

    private val WEEKDAY_ROWS = listOf(1 to "Mon", 3 to "Wed", 5 to "Fri")
}
