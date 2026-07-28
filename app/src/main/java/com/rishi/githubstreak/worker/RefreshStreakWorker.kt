package com.rishi.githubstreak.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rishi.githubstreak.data.ContributionCalendar
import com.rishi.githubstreak.data.GithubContributionClient
import com.rishi.githubstreak.data.Profile
import com.rishi.githubstreak.data.ProfileRepository
import com.rishi.githubstreak.data.StreakCalculator
import com.rishi.githubstreak.widget.WidgetInventory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Refreshes one profile, or every profile when no id is supplied. */
class RefreshStreakWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val repository = ProfileRepository.get(appContext)
    private val githubContributionClient = GithubContributionClient()

    override suspend fun doWork(): Result {
        val requestedId = inputData.getString(KEY_PROFILE_ID)
        val profiles = repository.snapshot().profiles
            .let { all -> if (requestedId == null) all else all.filter { it.id == requestedId } }

        if (profiles.isEmpty()) {
            WidgetInventory.refreshAll(applicationContext)
            return Result.success()
        }

        var transientFailure = false
        var permanentFailure = false

        profiles.forEach { profile ->
            when (refresh(profile)) {
                Outcome.OK -> Unit
                Outcome.RETRY -> transientFailure = true
                Outcome.FAILED -> permanentFailure = true
            }
        }

        WidgetInventory.refreshAll(applicationContext)

        return when {
            transientFailure -> Result.retry()
            permanentFailure -> Result.failure()
            else -> Result.success()
        }
    }

    private suspend fun refresh(profile: Profile): Outcome = try {
        val snapshot = githubContributionClient.fetchPublicContributions(profile.username)
        repository.saveSummary(
            id = profile.id,
            summary = StreakCalculator.summarize(snapshot),
            calendar = ContributionCalendar.fromDays(snapshot.days),
        )
        Outcome.OK
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IOException) {
        repository.saveError(profile.id, exception.userMessage())
        Outcome.RETRY
    } catch (exception: IllegalArgumentException) {
        // A bad username will never succeed, so do not schedule a retry for it.
        repository.saveError(profile.id, exception.userMessage())
        Outcome.OK
    } catch (exception: Exception) {
        repository.saveError(profile.id, exception.userMessage())
        Outcome.FAILED
    }

    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() }
        ?: javaClass.simpleName

    private enum class Outcome { OK, RETRY, FAILED }

    companion object {
        private const val PERIODIC_WORK_NAME = "github_streak_refresh_periodic"
        private const val MANUAL_WORK_NAME = "github_streak_refresh_manual"
        private const val KEY_PROFILE_ID = "profile_id"
        private const val REFRESH_INTERVAL_HOURS = 4L

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshStreakWorker>(
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun refreshNow(context: Context, profileId: String? = null) {
            val request = OneTimeWorkRequestBuilder<RefreshStreakWorker>()
                .setConstraints(networkConstraints())
                .setInputData(
                    Data.Builder()
                        .apply { if (profileId != null) putString(KEY_PROFILE_ID, profileId) }
                        .build(),
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /** True while a manual refresh is queued or running, for the in-app indicator. */
        fun isRefreshing(context: Context): Flow<Boolean> =
            WorkManager.getInstance(context.applicationContext)
                .getWorkInfosForUniqueWorkFlow(MANUAL_WORK_NAME)
                .map { infos -> infos.any { !it.state.isFinished } }

        private fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
