package com.rishi.githubstreak.graph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.rishi.githubstreak.data.ContributionCalendar
import com.rishi.githubstreak.ui.theme.GithubColorTokens
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Draws a contribution grid onto a plain [Canvas]. Glance cannot compose 371 individual views,
 * so the widget renders the grid to a bitmap instead.
 */
object ContributionGraphRenderer {

    /** Keeps the RemoteViews payload small enough for any launcher. */
    private const val MAX_BITMAP_WIDTH = 1400
    private const val MAX_BITMAP_HEIGHT = 500

    /**
     * Empty cells are drawn as a translucent neutral rather than the theme's own token. A widget
     * bitmap cannot re-colour itself when the system flips light/dark, and a grid of opaque
     * light-grey squares on a dark card is glaring; this reads correctly on either canvas and lands
     * within a shade of GitHub's #EBEDF0 / #151B23 on the matching background.
     */
    private const val EMPTY_CELL_ARGB = 0x24808080

    fun draw(
        canvas: Canvas,
        layout: ContributionGraphLayout.Layout,
        levelColors: IntArray,
        labelColor: Int,
    ) {
        if (layout.isEmpty) return

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        layout.cells.forEach { cell ->
            cellPaint.color = levelColors[cell.level.coerceIn(0, levelColors.lastIndex)]
            canvas.drawRoundRect(
                cell.left,
                cell.top,
                cell.left + cell.size,
                cell.top + cell.size,
                layout.cornerRadius,
                layout.cornerRadius,
                cellPaint,
            )
        }

        if (layout.textSize <= 0f) return

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = layout.textSize
            typeface = Typeface.DEFAULT
        }
        (layout.monthLabels + layout.weekdayLabels).forEach { label ->
            canvas.drawText(label.text, label.x, label.y, textPaint)
        }
    }

    /** Returns null when the target box is too small to draw anything meaningful. */
    fun renderBitmap(
        calendar: ContributionCalendar,
        endDate: LocalDate,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        dark: Boolean,
        showMonthLabels: Boolean = true,
        showWeekdayLabels: Boolean = true,
    ): Bitmap? {
        val width = widthPx.coerceAtMost(MAX_BITMAP_WIDTH)
        val height = heightPx.coerceAtMost(MAX_BITMAP_HEIGHT)
        if (width < 8 || height < 8) return null

        val scale = minOf(1f, width.toFloat() / widthPx, height.toFloat() / heightPx)
        val layout = ContributionGraphLayout.fit(
            calendar = calendar,
            endDate = endDate,
            widthPx = width.toFloat(),
            heightPx = height.toFloat(),
            density = density * scale,
            showMonthLabels = showMonthLabels,
            showWeekdayLabels = showWeekdayLabels,
        )
        if (layout.isEmpty) return null

        val levelColors = GithubColorTokens.levels(dark).copyOf()
        levelColors[0] = EMPTY_CELL_ARGB

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(
            canvas = Canvas(bitmap),
            layout = layout,
            levelColors = levelColors,
            labelColor = GithubColorTokens.fgMuted(dark),
        )
        return bitmap
    }
}
