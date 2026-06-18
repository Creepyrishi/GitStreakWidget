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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "github_streak_settings",
)

class SettingsRepository private constructor(
    context: Context,
) {
    private val appContext = context.applicationContext

    val appState: Flow<AppState> = appContext.streakDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toAppState() }

    suspend fun saveUsername(rawUsername: String) {
        val username = GithubContributionClient.normalizeUsername(rawUsername)
        appContext.streakDataStore.edit { preferences ->
            if (username.isBlank()) {
                preferences.remove(Keys.Username)
                preferences[Keys.Status] = StreakStatus.NEEDS_USERNAME.name
            } else {
                preferences[Keys.Username] = username
                preferences[Keys.Status] = StreakStatus.PENDING.name
            }

            preferences[Keys.StreakDays] = 0
            preferences[Keys.LastUpdatedMillis] = 0L
            preferences.remove(Keys.AnchorDate)
            preferences.remove(Keys.ErrorMessage)
        }
    }

    suspend fun saveStreak(result: StreakResult) {
        appContext.streakDataStore.edit { preferences ->
            preferences[Keys.Username] = result.username
            preferences[Keys.StreakDays] = result.streakDays
            preferences[Keys.Status] = result.status.name
            preferences[Keys.LastUpdatedMillis] = result.lastUpdatedMillis

            val anchorDate = result.anchorDate
            if (anchorDate == null) {
                preferences.remove(Keys.AnchorDate)
            } else {
                preferences[Keys.AnchorDate] = anchorDate.toString()
            }

            val errorMessage = result.errorMessage
            if (errorMessage.isNullOrBlank()) {
                preferences.remove(Keys.ErrorMessage)
            } else {
                preferences[Keys.ErrorMessage] = errorMessage
            }
        }
    }

    suspend fun saveRefreshError(message: String) {
        appContext.streakDataStore.edit { preferences ->
            preferences[Keys.Status] = StreakStatus.ERROR.name
            preferences[Keys.ErrorMessage] = message
            preferences[Keys.LastUpdatedMillis] = System.currentTimeMillis()
        }
    }

    private fun Preferences.toAppState(): AppState {
        val username = this[Keys.Username].orEmpty()
        val storedStatus = this[Keys.Status]
        val status = storedStatus
            ?.let { value -> runCatching { StreakStatus.valueOf(value) }.getOrNull() }
            ?: if (username.isBlank()) StreakStatus.NEEDS_USERNAME else StreakStatus.PENDING

        return AppState(
            username = username,
            streakDays = this[Keys.StreakDays] ?: 0,
            status = status,
            anchorDate = this[Keys.AnchorDate]
                ?.let { dateText -> runCatching { LocalDate.parse(dateText) }.getOrNull() },
            lastUpdatedMillis = this[Keys.LastUpdatedMillis] ?: 0L,
            errorMessage = this[Keys.ErrorMessage],
        )
    }

    private object Keys {
        val Username = stringPreferencesKey("username")
        val StreakDays = intPreferencesKey("streak_days")
        val Status = stringPreferencesKey("status")
        val AnchorDate = stringPreferencesKey("anchor_date")
        val LastUpdatedMillis = longPreferencesKey("last_updated_millis")
        val ErrorMessage = stringPreferencesKey("error_message")
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(context.applicationContext).also { instance = it }
        }
    }
}
