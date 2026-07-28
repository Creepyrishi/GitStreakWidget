package com.rishi.githubstreak.ui.theme

/**
 * GitHub Primer colour tokens as plain ARGB ints, so the Compose theme, the Glance widgets and
 * the bitmap graph renderer all draw from one source.
 */
object GithubColorTokens {

    object Light {
        const val CANVAS = 0xFFFFFFFF.toInt()
        const val CANVAS_SUBTLE = 0xFFF6F8FA.toInt()
        const val CANVAS_INSET = 0xFFF6F8FA.toInt()
        const val BORDER = 0xFFD1D9E0.toInt()
        const val BORDER_MUTED = 0xFFD8DEE4.toInt()
        const val FG = 0xFF1F2328.toInt()
        const val FG_MUTED = 0xFF59636E.toInt()
        const val FG_SUBTLE = 0xFF818B98.toInt()
        const val ACCENT = 0xFF0969DA.toInt()
        const val ACCENT_SUBTLE = 0xFFDDF4FF.toInt()
        const val SUCCESS = 0xFF1A7F37.toInt()
        const val SUCCESS_EMPHASIS = 0xFF1F883D.toInt()
        const val SUCCESS_SUBTLE = 0xFFDAFBE1.toInt()
        const val ATTENTION = 0xFF9A6700.toInt()
        const val ATTENTION_SUBTLE = 0xFFFFF8C5.toInt()
        const val DANGER = 0xFFD1242F.toInt()
        const val DANGER_SUBTLE = 0xFFFFEBE9.toInt()
        const val ON_EMPHASIS = 0xFFFFFFFF.toInt()

        val LEVELS = intArrayOf(
            0xFFEBEDF0.toInt(),
            0xFF9BE9A8.toInt(),
            0xFF40C463.toInt(),
            0xFF30A14E.toInt(),
            0xFF216E39.toInt(),
        )
    }

    object Dark {
        const val CANVAS = 0xFF0D1117.toInt()
        const val CANVAS_SUBTLE = 0xFF161B22.toInt()
        const val CANVAS_INSET = 0xFF010409.toInt()
        const val BORDER = 0xFF3D444D.toInt()
        const val BORDER_MUTED = 0xFF2F3742.toInt()
        const val FG = 0xFFF0F6FC.toInt()
        const val FG_MUTED = 0xFF9198A1.toInt()
        const val FG_SUBTLE = 0xFF6E7681.toInt()
        const val ACCENT = 0xFF4493F8.toInt()
        const val ACCENT_SUBTLE = 0xFF121D2F.toInt()
        const val SUCCESS = 0xFF3FB950.toInt()
        const val SUCCESS_EMPHASIS = 0xFF238636.toInt()
        const val SUCCESS_SUBTLE = 0xFF0F2E1B.toInt()
        const val ATTENTION = 0xFFD29922.toInt()
        const val ATTENTION_SUBTLE = 0xFF272115.toInt()
        const val DANGER = 0xFFF85149.toInt()
        const val DANGER_SUBTLE = 0xFF25171C.toInt()
        const val ON_EMPHASIS = 0xFFFFFFFF.toInt()

        val LEVELS = intArrayOf(
            0xFF151B23.toInt(),
            0xFF033A16.toInt(),
            0xFF196C2E.toInt(),
            0xFF2EA043.toInt(),
            0xFF56D364.toInt(),
        )
    }

    fun levels(dark: Boolean): IntArray = if (dark) Dark.LEVELS else Light.LEVELS

    fun fgMuted(dark: Boolean): Int = if (dark) Dark.FG_MUTED else Light.FG_MUTED
}
