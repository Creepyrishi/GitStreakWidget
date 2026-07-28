package com.rishi.githubstreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "github_streak_settings",
)

/** Outcome of trying to add a profile, so the UI can show a precise message. */
sealed interface AddProfileResult {
    data class Added(val profile: Profile) : AddProfileResult
    data class Duplicate(val profile: Profile) : AddProfileResult
    data object InvalidUsername : AddProfileResult
}

class ProfileRepository private constructor(context: Context) {
    private val appContext = context.applicationContext

    val appData: Flow<AppData> = appContext.streakDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences.toAppData() }

    suspend fun snapshot(): AppData = appData.first()

    suspend fun addProfile(rawUsername: String): AddProfileResult {
        val username = GithubContributionClient.normalizeUsername(rawUsername)
        if (!GithubContributionClient.isValidUsername(username)) return AddProfileResult.InvalidUsername

        var result: AddProfileResult = AddProfileResult.InvalidUsername
        updateProfiles { profiles ->
            val existing = profiles.firstOrNull { it.username.equals(username, ignoreCase = true) }
            if (existing != null) {
                result = AddProfileResult.Duplicate(existing)
                profiles
            } else {
                val profile = Profile(id = UUID.randomUUID().toString(), username = username)
                result = AddProfileResult.Added(profile)
                profiles + profile
            }
        }
        return result
    }

    suspend fun removeProfile(id: String) {
        updateProfiles { profiles -> profiles.filterNot { it.id == id } }
    }

    suspend fun moveProfile(id: String, offset: Int) {
        updateProfiles { profiles ->
            val index = profiles.indexOfFirst { it.id == id }
            val target = index + offset
            if (index < 0 || target !in profiles.indices) {
                profiles
            } else {
                profiles.toMutableList().apply { add(target, removeAt(index)) }
            }
        }
    }

    suspend fun saveSummary(id: String, summary: StreakSummary, calendar: ContributionCalendar) {
        updateProfiles { profiles ->
            profiles.map { profile ->
                if (profile.id != id) {
                    profile
                } else {
                    profile.copy(
                        streakDays = summary.streakDays,
                        longestStreak = summary.longestStreak,
                        totalContributions = summary.totalContributions,
                        status = summary.status,
                        anchorDate = summary.anchorDate,
                        lastUpdatedMillis = System.currentTimeMillis(),
                        errorMessage = null,
                        calendar = calendar,
                    )
                }
            }
        }
    }

    suspend fun saveError(id: String, message: String) {
        updateProfiles { profiles ->
            profiles.map { profile ->
                if (profile.id != id) {
                    profile
                } else {
                    profile.copy(
                        status = StreakStatus.ERROR,
                        errorMessage = message,
                        lastUpdatedMillis = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    private suspend fun updateProfiles(transform: (List<Profile>) -> List<Profile>) {
        appContext.streakDataStore.edit { preferences ->
            val current = preferences.readProfiles()
            preferences[Keys.Profiles] = ProfileCodec.encode(transform(current))
            LegacyKeys.removeFrom(preferences)
        }
    }

    private fun Preferences.readProfiles(): List<Profile> {
        val stored = this[Keys.Profiles]
        if (stored != null) return ProfileCodec.decode(stored)
        return legacyProfile()?.let(::listOf) ?: emptyList()
    }

    /** Carries a v1.0 single-username install over to the multi-profile format. */
    private fun Preferences.legacyProfile(): Profile? {
        val username = this[LegacyKeys.Username]?.takeIf { it.isNotBlank() } ?: return null
        val status = this[LegacyKeys.Status]
            ?.let { value -> runCatching { StreakStatus.valueOf(value) }.getOrNull() }
            ?: StreakStatus.PENDING

        return Profile(
            id = LEGACY_PROFILE_ID,
            username = username,
            streakDays = this[LegacyKeys.StreakDays] ?: 0,
            status = status,
            anchorDate = this[LegacyKeys.AnchorDate]
                ?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() },
            lastUpdatedMillis = this[LegacyKeys.LastUpdatedMillis] ?: 0L,
            errorMessage = this[LegacyKeys.ErrorMessage],
        )
    }

    private fun Preferences.toAppData(): AppData = AppData(profiles = readProfiles(), loaded = true)

    private object Keys {
        val Profiles = stringPreferencesKey("profiles_json")
    }

    private object LegacyKeys {
        val Username = stringPreferencesKey("username")
        val StreakDays = intPreferencesKey("streak_days")
        val Status = stringPreferencesKey("status")
        val AnchorDate = stringPreferencesKey("anchor_date")
        val LastUpdatedMillis = longPreferencesKey("last_updated_millis")
        val ErrorMessage = stringPreferencesKey("error_message")

        fun removeFrom(preferences: androidx.datastore.preferences.core.MutablePreferences) {
            preferences.remove(Username)
            preferences.remove(StreakDays)
            preferences.remove(Status)
            preferences.remove(AnchorDate)
            preferences.remove(LastUpdatedMillis)
            preferences.remove(ErrorMessage)
        }
    }

    companion object {
        private const val LEGACY_PROFILE_ID = "legacy-profile"

        @Volatile
        private var instance: ProfileRepository? = null

        fun get(context: Context): ProfileRepository = instance ?: synchronized(this) {
            instance ?: ProfileRepository(context.applicationContext).also { instance = it }
        }
    }
}
