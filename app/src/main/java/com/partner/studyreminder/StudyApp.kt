package com.partner.studyreminder

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.alarm.NotificationHelper
import com.partner.studyreminder.sync.SyncScheduler

class StudyApp : Application() {
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) {
                    try {
                        AlarmScheduler.ensureIsland(this@StudyApp)
                    } catch (e: Exception) {
                        Log.e("StudyReminder", "background island restore failed", e)
                    }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        try {
            NotificationHelper.ensureChannels(this)
            AlarmScheduler.rescheduleAll(this)
            SyncScheduler.ensure(this)
        } catch (e: Exception) {
            Log.e("StudyReminder", "startup schedule failed", e)
        }
    }
}
