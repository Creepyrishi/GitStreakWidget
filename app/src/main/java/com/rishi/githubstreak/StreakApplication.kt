package com.rishi.githubstreak

import android.app.Application
import com.rishi.githubstreak.worker.RefreshStreakWorker

class StreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RefreshStreakWorker.enqueuePeriodic(this)
    }
}
