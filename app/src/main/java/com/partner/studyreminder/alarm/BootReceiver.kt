package com.partner.studyreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.partner.studyreminder.sync.SyncScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
        SyncScheduler.ensure(context)
    }
}
