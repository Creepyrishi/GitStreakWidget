package com.rishi.githubstreak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishi.githubstreak.data.ContributionCalendar
import com.rishi.githubstreak.graph.ContributionGraphLayout
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import java.time.LocalDate

/**
 * The GitHub contribution grid. The grid scrolls horizontally so a full year keeps GitHub's cell
 * size instead of shrinking to phone width, while the Mon/Wed/Fri column stays pinned like it does
 * on github.com.
 */
@Composable
fun ContributionGraph(
    calendar: ContributionCalendar,
    modifier: Modifier = Modifier,
    endDate: LocalDate = LocalDate.now(),
    step: Dp = 13.dp,
    weeks: Int = ContributionGraphLayout.MAX_WEEKS,
) {
    val palette = LocalGithubPalette.current
    val density = LocalDensity.current
    val layout = ContributionGraphLayout.fixed(
        calendar = calendar,
        endDate = endDate,
        stepPx = with(density) { step.toPx() },
        weeks = weeks,
        density = density.density,
    )

    val labelColor = palette.fgMuted.toArgb()
    val heightDp = with(density) { layout.height.toDp() }
    val gutterDp = with(density) { layout.weekdayGutter.toDp() }
    val gridWidthDp = with(density) { (layout.width - layout.weekdayGutter).toDp() }

    Row(modifier = modifier) {
        if (layout.weekdayLabels.isNotEmpty()) {
            Canvas(modifier = Modifier.width(gutterDp).height(heightDp)) {
                drawLabels(layout.weekdayLabels, layout.textSize, labelColor)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState(), reverseScrolling = true),
        ) {
            Canvas(modifier = Modifier.width(gridWidthDp).height(heightDp)) {
                layout.cells.forEach { cell ->
                    drawRoundRect(
                        color = palette.levels[cell.level.coerceIn(0, palette.levels.lastIndex)],
                        topLeft = Offset(cell.left - layout.weekdayGutter, cell.top),
                        size = Size(cell.size, cell.size),
                        cornerRadius = CornerRadius(layout.cornerRadius, layout.cornerRadius),
                    )
                }
                drawLabels(
                    labels = layout.monthLabels.map { it.copy(x = it.x - layout.weekdayGutter) },
                    textSize = layout.textSize,
                    argb = labelColor,
                )
            }
        }
    }
}

private fun DrawScope.drawLabels(
    labels: List<ContributionGraphLayout.Label>,
    textSize: Float,
    argb: Int,
) {
    if (textSize <= 0f || labels.isEmpty()) return

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = argb
            this.textSize = textSize
        }
        labels.forEach { label ->
            canvas.nativeCanvas.drawText(label.text, label.x, label.y, paint)
        }
    }
}

@Composable
fun ContributionLegend(modifier: Modifier = Modifier) {
    val palette = LocalGithubPalette.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "Less",
            fontSize = 11.sp,
            color = palette.fgMuted,
        )
        Spacer(modifier = Modifier.width(1.dp))
        palette.levels.forEach { color ->
            Spacer(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
        Spacer(modifier = Modifier.width(1.dp))
        Text(
            text = "More",
            fontSize = 11.sp,
            color = palette.fgMuted,
        )
    }
}
