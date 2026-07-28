package com.rishi.githubstreak.data

import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Parsed contribution calendar for one user. */
data class ContributionSnapshot(
    val days: List<ContributionDay>,
    val reportedTotal: Int?,
)

class GithubContributionClient(
    private val okHttpClient: OkHttpClient = sharedClient,
) {
    suspend fun fetchPublicContributions(username: String): ContributionSnapshot = withContext(Dispatchers.IO) {
        val safeUsername = validateUsername(username)
        // No date parameters: GitHub answers a `to=` request with that whole calendar year
        // (future days included), while the bare URL returns the trailing 53 weeks ending today.
        val url = "https://github.com/".toHttpUrl().newBuilder()
            .addPathSegment("users")
            .addPathSegment(safeUsername)
            .addPathSegment("contributions")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", "GithubStreakWidget/2.0")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 404) {
                throw IllegalArgumentException("GitHub user not found")
            }
            if (!response.isSuccessful) {
                throw IOException("GitHub request failed: HTTP ${response.code}")
            }

            parse(response.body?.string().orEmpty())
        }
    }

    internal fun parse(html: String): ContributionSnapshot {
        val days = parseContributionDays(html)
        check(days.isNotEmpty()) { "Could not read GitHub public contribution calendar" }
        return ContributionSnapshot(days = days, reportedTotal = parseReportedTotal(html))
    }

    private fun parseContributionDays(html: String): List<ContributionDay> {
        val document = Jsoup.parse(html)
        val tooltipByTargetId = document.select("tool-tip[for]")
            .associate { element -> element.attr("for") to element.text() }

        return document.select("[data-date]")
            .mapNotNull { element ->
                val date = element.attr("data-date")
                    .takeIf { it.isNotBlank() }
                    ?.let { dateText -> runCatching { LocalDate.parse(dateText) }.getOrNull() }
                    ?: return@mapNotNull null

                val (count, level) = readCountAndLevel(
                    element = element,
                    tooltip = tooltipByTargetId[element.id()],
                )
                ContributionDay(date = date, count = count, level = level)
            }
            .groupBy { it.date }
            .map { (date, entries) ->
                ContributionDay(
                    date = date,
                    count = entries.maxOf { it.count },
                    level = entries.maxOf { it.level },
                )
            }
            .sortedBy { it.date }
    }

    /** GitHub renders "1,234 contributions in the last year" above the calendar. */
    private fun parseReportedTotal(html: String): Int? = Regex(
        "([0-9][0-9,]*)\\s+contributions?\\s+in\\s+the\\s+last\\s+year",
        RegexOption.IGNORE_CASE,
    )
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.toContributionIntOrNull()

    /**
     * Returns the contribution count and the 0..4 colour level for a calendar cell. GitHub has
     * moved these between attributes and tooltips over the years, so every known source is tried.
     */
    private fun readCountAndLevel(element: Element, tooltip: String?): Pair<Int, Int> {
        val attributeCount = listOf("data-count", "data-contribution-count")
            .firstNotNullOfOrNull { attribute -> element.attr(attribute).toContributionIntOrNull() }

        val label = sequenceOf(
            element.attr("aria-label"),
            element.attr("title"),
            tooltip.orEmpty(),
        ).firstOrNull { it.isNotBlank() }

        val count = attributeCount ?: label?.contributionCountOrNull()

        val level = element.attr("data-level").toIntOrNull()
            ?: element.className().lowercase(Locale.US).let { className ->
                Regex("contribution-level-([0-4])").find(className)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
            }
            ?: count?.let { estimateLevel(it) }
            ?: 0

        return (count ?: if (level > 0) 1 else 0) to level.coerceIn(0, MAX_CONTRIBUTION_LEVEL)
    }

    /** Fallback bucketing when GitHub does not expose data-level. */
    private fun estimateLevel(count: Int): Int = when {
        count <= 0 -> 0
        count < 3 -> 1
        count < 6 -> 2
        count < 10 -> 3
        else -> 4
    }

    private fun String.toContributionIntOrNull(): Int? = trim()
        .replace(",", "")
        .toIntOrNull()

    private fun String.contributionCountOrNull(): Int? {
        val normalized = trim().lowercase(Locale.US)
        if (normalized.isBlank()) return null
        if (normalized.startsWith("no contribution")) return 0

        val count = Regex("(^|\\s)([0-9][0-9,]*)\\s+contribution")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(2)
            ?.toContributionIntOrNull()
        if (count != null) return count

        if (normalized.contains("no contributions")) return 0
        return null
    }

    companion object {
        private val usernamePattern = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?\$")

        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        fun normalizeUsername(username: String): String = username.trim()
            .removePrefix("@")
            .removeSuffix("/")
            .substringAfterLast('/')

        fun isValidUsername(username: String): Boolean = usernamePattern.matches(normalizeUsername(username))

        fun validateUsername(username: String): String {
            val normalized = normalizeUsername(username)
            require(usernamePattern.matches(normalized)) { "Invalid GitHub username" }
            return normalized
        }
    }
}
