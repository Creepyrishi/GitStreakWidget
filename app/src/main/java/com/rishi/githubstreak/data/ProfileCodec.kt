package com.rishi.githubstreak.data

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialises the profile list into the single DataStore string that backs the app.
 * Uses org.json so the app keeps zero extra dependencies.
 */
object ProfileCodec {

    fun encode(profiles: List<Profile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            val json = JSONObject()
            json.put(KEY_ID, profile.id)
            json.put(KEY_USERNAME, profile.username)
            json.put(KEY_STREAK, profile.streakDays)
            json.put(KEY_LONGEST, profile.longestStreak)
            json.put(KEY_TOTAL, profile.totalContributions)
            json.put(KEY_STATUS, profile.status.name)
            json.put(KEY_ANCHOR, profile.anchorDate?.toString() ?: JSONObject.NULL)
            json.put(KEY_UPDATED, profile.lastUpdatedMillis)
            json.put(KEY_ERROR, profile.errorMessage ?: JSONObject.NULL)
            json.put(KEY_CALENDAR_START, profile.calendar.startDate?.toString() ?: JSONObject.NULL)
            json.put(KEY_CALENDAR_LEVELS, profile.calendar.encodeLevels())
            array.put(json)
        }
        return array.toString()
    }

    fun decode(raw: String?): List<Profile> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val username = json.optString(KEY_USERNAME).takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val id = json.optString(KEY_ID).takeIf { it.isNotBlank() } ?: username.lowercase()

            Profile(
                id = id,
                username = username,
                streakDays = json.optInt(KEY_STREAK, 0),
                longestStreak = json.optInt(KEY_LONGEST, 0),
                totalContributions = json.optInt(KEY_TOTAL, 0),
                status = json.optStringOrNull(KEY_STATUS)
                    ?.let { value -> runCatching { StreakStatus.valueOf(value) }.getOrNull() }
                    ?: StreakStatus.PENDING,
                anchorDate = json.optStringOrNull(KEY_ANCHOR)
                    ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() },
                lastUpdatedMillis = json.optLong(KEY_UPDATED, 0L),
                errorMessage = json.optStringOrNull(KEY_ERROR),
                calendar = ContributionCalendar.decode(
                    startDate = json.optStringOrNull(KEY_CALENDAR_START),
                    encodedLevels = json.optStringOrNull(KEY_CALENDAR_LEVELS),
                ),
            )
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private const val KEY_ID = "id"
    private const val KEY_USERNAME = "username"
    private const val KEY_STREAK = "streak"
    private const val KEY_LONGEST = "longest"
    private const val KEY_TOTAL = "total"
    private const val KEY_STATUS = "status"
    private const val KEY_ANCHOR = "anchor"
    private const val KEY_UPDATED = "updated"
    private const val KEY_ERROR = "error"
    private const val KEY_CALENDAR_START = "calStart"
    private const val KEY_CALENDAR_LEVELS = "calLevels"
}
