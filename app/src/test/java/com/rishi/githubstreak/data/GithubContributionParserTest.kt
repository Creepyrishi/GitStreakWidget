package com.rishi.githubstreak.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs against markup copied verbatim from github.com, so a change to GitHub's calendar HTML
 * shows up here instead of as an empty widget.
 */
class GithubContributionParserTest {

    private val client = GithubContributionClient()

    private val sample: String by lazy {
        checkNotNull(javaClass.classLoader?.getResourceAsStream("github_contributions_sample.html"))
            .bufferedReader()
            .use { it.readText() }
    }

    @Test
    fun `reads every calendar cell`() {
        assertEquals(8, client.parse(sample).days.size)
    }

    @Test
    fun `reads the yearly total from the heading`() {
        assertEquals(3253, client.parse(sample).reportedTotal)
    }

    @Test
    fun `reads counts from tooltips and levels from attributes`() {
        val byDate = client.parse(sample).days.associateBy { it.date }

        val busiest = byDate.getValue(LocalDate.parse("2025-07-28"))
        assertEquals(61, busiest.count)
        assertEquals(4, busiest.level)
        assertTrue(busiest.active)

        val quiet = byDate.getValue(LocalDate.parse("2025-07-27"))
        assertEquals(3, quiet.count)
        assertEquals(1, quiet.level)
    }

    @Test
    fun `treats a no-contributions tooltip as an inactive day`() {
        val empty = client.parse(sample).days.single { it.date == LocalDate.parse("2025-09-01") }

        assertEquals(0, empty.count)
        assertEquals(0, empty.level)
        assertFalse(empty.active)
    }

    @Test
    fun `rejects markup without a calendar`() {
        val failure = runCatching { client.parse("<html><body>Not found</body></html>") }
        assertTrue(failure.isFailure)
    }

    @Test
    fun `normalizes usernames pasted as profile urls or handles`() {
        assertEquals("torvalds", GithubContributionClient.normalizeUsername("@torvalds"))
        assertEquals("torvalds", GithubContributionClient.normalizeUsername("  torvalds  "))
        assertEquals("torvalds", GithubContributionClient.normalizeUsername("https://github.com/torvalds"))
        assertEquals("torvalds", GithubContributionClient.normalizeUsername("https://github.com/torvalds/"))
    }

    @Test
    fun `rejects usernames github would never issue`() {
        assertFalse(GithubContributionClient.isValidUsername("-leading-dash"))
        assertFalse(GithubContributionClient.isValidUsername("has space"))
        assertFalse(GithubContributionClient.isValidUsername(""))
        assertTrue(GithubContributionClient.isValidUsername("a"))
        assertTrue(GithubContributionClient.isValidUsername("Creepyrishi"))
    }
}
