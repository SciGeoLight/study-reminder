package com.partner.studyreminder.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        val message = PlanSync.run(applicationContext)
        return if (message.startsWith("同步失败") && runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
