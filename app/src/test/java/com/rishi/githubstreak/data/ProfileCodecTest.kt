package com.rishi.githubstreak.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCodecTest {

    private val profiles = listOf(
        Profile(
            id = "a",
            username = "torvalds",
            streakDays = 12,
            longestStreak = 40,
            totalContributions = 3253,
            status = StreakStatus.ACTIVE_TODAY,
            anchorDate = LocalDate.parse("2026-07-28"),
            lastUpdatedMillis = 1_722_000_000_000L,
            calendar = ContributionCalendar.fromDays(
                (0 until 10).map { offset ->
                    ContributionDay(
                        date = LocalDate.parse("2026-07-01").plusDays(offset.toLong()),
                        count = offset,
                        level = offset % 5,
                    )
                },
            ),
        ),
        Profile(
            id = "b",
            username = "octocat",
            status = StreakStatus.ERROR,
            errorMessage = "GitHub user not found",
        ),
    )

    @Test
    fun `survives a round trip`() {
        assertEquals(profiles, ProfileCodec.decode(ProfileCodec.encode(profiles)))
    }

    @Test
    fun `keeps profile order`() {
        val decoded = ProfileCodec.decode(ProfileCodec.encode(profiles))

        assertEquals(listOf("torvalds", "octocat"), decoded.map { it.username })
    }

    @Test
    fun `ignores corrupt storage instead of crashing`() {
        assertTrue(ProfileCodec.decode(null).isEmpty())
        assertTrue(ProfileCodec.decode("").isEmpty())
        assertTrue(ProfileCodec.decode("{not json").isEmpty())
        assertTrue(ProfileCodec.decode("""[{"id":"x"}]""").isEmpty())
    }

    @Test
    fun `an unknown status falls back to pending rather than dropping the profile`() {
        val decoded = ProfileCodec.decode("""[{"id":"x","username":"octocat","status":"WAT"}]""")

        assertEquals(1, decoded.size)
        assertEquals(StreakStatus.PENDING, decoded.single().status)
    }
}
