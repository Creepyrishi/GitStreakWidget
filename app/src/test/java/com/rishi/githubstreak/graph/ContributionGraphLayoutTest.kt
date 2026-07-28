package com.rishi.githubstreak.graph

import com.rishi.githubstreak.data.CALENDAR_DAYS
import com.rishi.githubstreak.data.ContributionCalendar
import com.rishi.githubstreak.data.ContributionDay
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionGraphLayoutTest {

    private val today = LocalDate.parse("2026-07-28")

    private val fullYear = ContributionCalendar.fromDays(
        (0 until CALENDAR_DAYS).map { offset ->
            ContributionDay(
                date = today.minusDays((CALENDAR_DAYS - 1 - offset).toLong()),
                count = offset % 5,
                level = offset % 5,
            )
        },
    )

    @Test
    fun `a wide widget shows the whole year`() {
        val layout = ContributionGraphLayout.fit(
            calendar = fullYear,
            endDate = today,
            widthPx = 1200f,
            heightPx = 200f,
            density = 3f,
        )

        // 53 columns, minus the days of the current week that have not happened yet.
        assertEquals(ContributionGraphLayout.MAX_WEEKS, distinctColumns(layout))
    }

    @Test
    fun `a narrow widget drops the oldest weeks instead of shrinking to nothing`() {
        val layout = ContributionGraphLayout.fit(
            calendar = fullYear,
            endDate = today,
            widthPx = 300f,
            heightPx = 200f,
            density = 3f,
        )

        val columns = distinctColumns(layout)
        assertTrue("expected fewer than a full year, got $columns", columns < ContributionGraphLayout.MAX_WEEKS)
        assertTrue("expected something to draw, got $columns", columns > 0)
    }

    @Test
    fun `never draws a day that has not happened`() {
        // 2026-07-28 is a Tuesday, so Wed..Sat of the last column must stay blank.
        val layout = ContributionGraphLayout.fixed(
            calendar = fullYear,
            endDate = today,
            stepPx = 12f,
            density = 3f,
        )

        val lastColumnX = layout.cells.maxOf { it.left }
        val lastColumnCells = layout.cells.count { it.left == lastColumnX }

        assertEquals(3, lastColumnCells)
    }

    @Test
    fun `stays inside the box it was given`() {
        val layout = ContributionGraphLayout.fit(
            calendar = fullYear,
            endDate = today,
            widthPx = 420f,
            heightPx = 150f,
            density = 2.5f,
        )

        layout.cells.forEach { cell ->
            assertTrue("cell overflows right", cell.left + cell.size <= 420f + 0.5f)
            assertTrue("cell overflows bottom", cell.top + cell.size <= 150f + 0.5f)
            assertTrue("cell above the box", cell.top >= 0f)
        }
    }

    @Test
    fun `drops labels rather than squeezing a tiny widget`() {
        val tiny = ContributionGraphLayout.fit(
            calendar = fullYear,
            endDate = today,
            widthPx = 160f,
            heightPx = 56f,
            density = 3f,
        )

        assertTrue(tiny.monthLabels.isEmpty())
        assertTrue(tiny.weekdayLabels.isEmpty())
        assertTrue(tiny.cells.isNotEmpty())
    }

    @Test
    fun `an empty box draws nothing`() {
        val layout = ContributionGraphLayout.fit(
            calendar = fullYear,
            endDate = today,
            widthPx = 0f,
            heightPx = 100f,
            density = 3f,
        )

        assertTrue(layout.isEmpty)
    }

    @Test
    fun `a calendar with no data draws nothing`() {
        val layout = ContributionGraphLayout.fit(
            calendar = ContributionCalendar.Empty,
            endDate = today,
            widthPx = 600f,
            heightPx = 160f,
            density = 3f,
        )

        assertTrue(layout.cells.isEmpty())
    }

    private fun distinctColumns(layout: ContributionGraphLayout.Layout): Int =
        layout.cells.map { it.left }.distinct().size
}
