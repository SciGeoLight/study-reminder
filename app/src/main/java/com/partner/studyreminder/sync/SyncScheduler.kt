package com.partner.studyreminder.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE = "plan-url-sync"

    fun ensure(context: Context) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        val url = com.partner.studyreminder.data.Prefs.syncUrl(app).trim()
        if (url.isEmpty()) {
            wm.cancelUniqueWork(UNIQUE)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        wm.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun replace(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE)
        ensure(context)
    }
}
