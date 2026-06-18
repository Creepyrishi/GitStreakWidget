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

class GithubContributionClient(
    private val okHttpClient: OkHttpClient = sharedClient,
) {
    suspend fun fetchPublicContributionDays(username: String): List<ContributionDay> = withContext(Dispatchers.IO) {
        val safeUsername = validateUsername(username)
        val url = "https://github.com/".toHttpUrl().newBuilder()
            .addPathSegment("users")
            .addPathSegment(safeUsername)
            .addPathSegment("contributions")
            .addQueryParameter("to", LocalDate.now().toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", "GithubStreakWidget/1.0")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 404) {
                throw IllegalArgumentException("GitHub user not found")
            }
            if (!response.isSuccessful) {
                throw IOException("GitHub request failed: HTTP ${response.code}")
            }

            val html = response.body?.string().orEmpty()
            val days = parseContributionDays(html)
            check(days.isNotEmpty()) { "Could not read GitHub public contribution calendar" }
            days
        }
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

                ContributionDay(
                    date = date,
                    active = isActiveDay(
                        element = element,
                        tooltip = tooltipByTargetId[element.id()],
                    ),
                )
            }
            .groupBy { it.date }
            .map { (date, entries) -> ContributionDay(date, entries.any { it.active }) }
            .sortedBy { it.date }
    }

    private fun isActiveDay(element: Element, tooltip: String?): Boolean {
        val count = listOf("data-count", "data-contribution-count")
            .firstNotNullOfOrNull { attribute -> element.attr(attribute).toContributionIntOrNull() }
        if (count != null) {
            return count > 0
        }

        val level = element.attr("data-level").toIntOrNull()
        if (level != null) {
            return level > 0
        }

        val className = element.className().lowercase(Locale.US)
        if (className.contains("contribution-level-0")) {
            return false
        }
        if (Regex("contribution-level-[1-4]").containsMatchIn(className)) {
            return true
        }

        val label = sequenceOf(
            element.attr("aria-label"),
            element.attr("title"),
            tooltip.orEmpty(),
        ).firstOrNull { it.isNotBlank() }

        return label?.meansActiveContributionDay() == true
    }

    private fun String.toContributionIntOrNull(): Int? = trim()
        .replace(",", "")
        .toIntOrNull()

    private fun String.meansActiveContributionDay(): Boolean {
        val normalized = trim().lowercase(Locale.US)
        if (normalized.isBlank() || normalized.startsWith("no contribution")) {
            return false
        }

        val count = Regex("(^|\\s)([0-9][0-9,]*)\\s+contribution")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(2)
            ?.replace(",", "")
            ?.toIntOrNull()

        if (count != null) {
            return count > 0
        }

        return normalized.contains("contribution") && !normalized.contains("no contributions")
    }

    companion object {
        private val usernamePattern = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?\$")

        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        fun normalizeUsername(username: String): String = username.trim().removePrefix("@")

        fun isValidUsername(username: String): Boolean = usernamePattern.matches(normalizeUsername(username))

        fun validateUsername(username: String): String {
            val normalized = normalizeUsername(username)
            require(usernamePattern.matches(normalized)) { "Invalid GitHub username" }
            return normalized
        }
    }
}
