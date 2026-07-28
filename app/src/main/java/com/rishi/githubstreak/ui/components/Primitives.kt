package com.rishi.githubstreak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishi.githubstreak.ui.theme.LocalGithubPalette
import com.rishi.githubstreak.ui.theme.MonoNumber

/** Primer's bordered box: 1px border, 6dp radius, canvas fill. */
@Composable
fun GhCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalGithubPalette.current
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.canvas)
            .border(1.dp, palette.border, shape),
        content = content,
    )
}

/** Primer "Label" — a small pill with a tinted background and matching text. */
@Composable
fun StatusPill(
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(1.dp, contentColor.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            style = MonoNumber,
            maxLines = 1,
        )
        Text(
            text = label,
            color = LocalGithubPalette.current.fgMuted,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** Stand-in for the GitHub avatar; keeps the app free of an image-loading dependency. */
@Composable
fun InitialAvatar(username: String, size: Int = 32, modifier: Modifier = Modifier) {
    val palette = LocalGithubPalette.current

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(palette.canvasSubtle)
            .border(1.dp, palette.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = username.firstOrNull()?.uppercase() ?: "?",
            color = palette.fgMuted,
            fontSize = (size * 0.45).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
