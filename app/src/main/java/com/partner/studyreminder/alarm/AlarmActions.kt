package com.partner.studyreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.partner.studyreminder.data.Prefs

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmContract.EXTRA_TITLE) ?: "学习提醒"
        val note = intent.getStringExtra(AlarmContract.EXTRA_NOTE).orEmpty()
        when (intent.action) {
            AlarmContract.ACTION_SNOOZE -> {
                AlarmScheduler.scheduleSnooze(context, title, note)
                AlarmActions.stop(context)
                Toast.makeText(context, "5 分钟后再提醒", Toast.LENGTH_SHORT).show()
            }
            AlarmContract.ACTION_ISLAND_REMOVED -> {
                val pending = goAsync()
                val app = context.applicationContext
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        AlarmScheduler.ensureIsland(app)
                    } finally {
                        pending.finish()
                    }
                }, 1_200L)
            }
            AlarmContract.ACTION_DISMISS -> {
                if (Prefs.isIslandMode(context)) AlarmActions.acknowledge(context)
                else AlarmActions.stop(context)
            }
            else -> AlarmActions.stop(context)
        }
    }
}

object AlarmActions {
    fun stop(context: Context) {
        val app = context.applicationContext
        app.stopService(Intent(app, RingingService::class.java))
        NotificationHelper.cancel(app)
        val active = Prefs.activeBlockId(app)
        if (active != null) Prefs.setSuppressedBlockId(app, active)
        Prefs.setActiveBlockId(app, null)
        AlarmScheduler.cancelIslandWatch(app)
        IslandNotifications.cancelAll(app)
    }

    /**
     * Dismiss the banner or the full-screen alarm page, but keep the ongoing
     * countdown. Swiping that notification away brings it back while the block lasts.
     */
    fun acknowledge(context: Context) {
        val app = context.applicationContext
        app.stopService(Intent(app, RingingService::class.java))
        NotificationHelper.cancel(app)
        app.getSystemService(android.app.NotificationManager::class.java)
            ?.cancel(AlarmContract.NOTIF_HEADS)
        AlarmScheduler.ensureIsland(app)
    }

    fun snooze(context: Context, title: String, note: String) {
        AlarmScheduler.scheduleSnooze(context, title, note)
        stop(context)
    }
}
