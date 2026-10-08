package com.partner.studyreminder.data

import android.content.Context
import java.io.File

object Plans {
    @Volatile
    private var repository: PlanRepository? = null

    fun of(context: Context): PlanRepository {
        repository?.let { return it }
        return synchronized(this) {
            repository ?: PlanRepository(
                File(context.applicationContext.filesDir, "plans.json"),
            ).also { repository = it }
        }
    }
}
