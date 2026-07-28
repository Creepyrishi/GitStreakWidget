package com.rishi.githubstreak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Primer tokens that Material 3's colour scheme has no slot for. */
@Immutable
data class GithubPalette(
    val isDark: Boolean,
    val canvas: Color,
    val canvasSubtle: Color,
    val canvasInset: Color,
    val border: Color,
    val borderMuted: Color,
    val fg: Color,
    val fgMuted: Color,
    val fgSubtle: Color,
    val accent: Color,
    val accentSubtle: Color,
    val success: Color,
    val successEmphasis: Color,
    val successSubtle: Color,
    val attention: Color,
    val attentionSubtle: Color,
    val danger: Color,
    val dangerSubtle: Color,
    val levels: List<Color>,
)

private val LightPalette = with(GithubColorTokens.Light) {
    GithubPalette(
        isDark = false,
        canvas = Color(CANVAS),
        canvasSubtle = Color(CANVAS_SUBTLE),
        canvasInset = Color(CANVAS_INSET),
        border = Color(BORDER),
        borderMuted = Color(BORDER_MUTED),
        fg = Color(FG),
        fgMuted = Color(FG_MUTED),
        fgSubtle = Color(FG_SUBTLE),
        accent = Color(ACCENT),
        accentSubtle = Color(ACCENT_SUBTLE),
        success = Color(SUCCESS),
        successEmphasis = Color(SUCCESS_EMPHASIS),
        successSubtle = Color(SUCCESS_SUBTLE),
        attention = Color(ATTENTION),
        attentionSubtle = Color(ATTENTION_SUBTLE),
        danger = Color(DANGER),
        dangerSubtle = Color(DANGER_SUBTLE),
        levels = LEVELS.map(::Color),
    )
}

private val DarkPalette = with(GithubColorTokens.Dark) {
    GithubPalette(
        isDark = true,
        canvas = Color(CANVAS),
        canvasSubtle = Color(CANVAS_SUBTLE),
        canvasInset = Color(CANVAS_INSET),
        border = Color(BORDER),
        borderMuted = Color(BORDER_MUTED),
        fg = Color(FG),
        fgMuted = Color(FG_MUTED),
        fgSubtle = Color(FG_SUBTLE),
        accent = Color(ACCENT),
        accentSubtle = Color(ACCENT_SUBTLE),
        success = Color(SUCCESS),
        successEmphasis = Color(SUCCESS_EMPHASIS),
        successSubtle = Color(SUCCESS_SUBTLE),
        attention = Color(ATTENTION),
        attentionSubtle = Color(ATTENTION_SUBTLE),
        danger = Color(DANGER),
        dangerSubtle = Color(DANGER_SUBTLE),
        levels = LEVELS.map(::Color),
    )
}

val LocalGithubPalette = staticCompositionLocalOf { LightPalette }

/** GitHub uses a plain system sans stack; numbers read better in the mono face. */
private val GithubTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val MonoNumber = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

@Composable
fun GithubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.success,
            background = palette.canvas,
            onBackground = palette.fg,
            surface = palette.canvas,
            onSurface = palette.fg,
            surfaceVariant = palette.canvasSubtle,
            onSurfaceVariant = palette.fgMuted,
            outline = palette.border,
            outlineVariant = palette.borderMuted,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.success,
            background = palette.canvas,
            onBackground = palette.fg,
            surface = palette.canvas,
            onSurface = palette.fg,
            surfaceVariant = palette.canvasSubtle,
            onSurfaceVariant = palette.fgMuted,
            outline = palette.border,
            outlineVariant = palette.borderMuted,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(LocalGithubPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GithubTypography,
            content = content,
        )
    }
}
