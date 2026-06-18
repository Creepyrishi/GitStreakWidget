package com.rishi.githubstreak.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rishi.githubstreak.data.GithubContributionClient
import com.rishi.githubstreak.data.SettingsRepository
import com.rishi.githubstreak.data.StreakCalculator
import com.rishi.githubstreak.widget.GithubStreakWidget
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class RefreshStreakWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val repository = SettingsRepository.get(appContext)
    private val githubContributionClient = GithubContributionClient()

    override suspend fun doWork(): Result {
        val username = GithubContributionClient.normalizeUsername(
            repository.appState.first().username,
        )

        if (username.isBlank()) {
            repository.saveUsername("")
            updateWidgets()
            return Result.success()
        }

        return try {
            val days = githubContributionClient.fetchPublicContributionDays(username)
            val result = StreakCalculator.calculate(username = username, days = days)
            repository.saveStreak(result)
            updateWidgets()
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            repository.saveRefreshError(exception.userMessage())
            updateWidgets()
            Result.retry()
        } catch (exception: IllegalArgumentException) {
            repository.saveRefreshError(exception.userMessage())
            updateWidgets()
            Result.success()
        } catch (exception: Exception) {
            repository.saveRefreshError(exception.userMessage())
            updateWidgets()
            Result.failure()
        }
    }

    private suspend fun updateWidgets() {
        GithubStreakWidget().updateAll(applicationContext)
    }

    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() }
        ?: javaClass.simpleName

    companion object {
        private const val PERIODIC_WORK_NAME = "github_streak_refresh_periodic"
        private const val MANUAL_WORK_NAME = "github_streak_refresh_manual"
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

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshStreakWorker>()
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
