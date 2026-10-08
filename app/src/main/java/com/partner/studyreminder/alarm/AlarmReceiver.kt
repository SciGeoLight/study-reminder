package com.partner.studyreminder.alarm

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.ui.ReminderActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val title = intent.getStringExtra(AlarmContract.EXTRA_TITLE) ?: "学习提醒"
        val note = intent.getStringExtra(AlarmContract.EXTRA_NOTE).orEmpty()
        val kind = intent.getStringExtra(AlarmContract.EXTRA_KIND) ?: AlarmContract.KIND_START
        val id = intent.getStringExtra(AlarmContract.EXTRA_ID).orEmpty()
        val whenLabel = intent.getStringExtra(AlarmContract.EXTRA_WHEN).orEmpty()
        val date = intent.getStringExtra(AlarmContract.EXTRA_DATE)
        val startLabel = intent.getStringExtra(AlarmContract.EXTRA_START_LABEL).orEmpty()
        val startAt = intent.getLongExtra(AlarmContract.EXTRA_START_AT, 0L)
        val endAt = intent.getLongExtra(AlarmContract.EXTRA_END_AT, 0L)
        if (kind == AlarmContract.KIND_END) {
            IslandNotifications.onBlockEnded(app, id)
            return
        }
        if (kind == AlarmContract.KIND_WATCH) {
            AlarmScheduler.ensureIsland(app)
            return
        }
        if (kind == AlarmContract.KIND_SNOOZE && id.isNotBlank()) {
            Plans.of(app).removeSnooze(id)
        }
        try {
            deliver(app, title, note, kind, id, whenLabel, date, startLabel, startAt, endAt)
        } finally {
            if (kind != AlarmContract.KIND_WATCH) {
                AlarmScheduler.rescheduleAll(app, fromFire = true)
            }
        }
    }

    private fun deliver(
        app: Context,
        title: String,
        note: String,
        kind: String,
        id: String,
        whenLabel: String,
        date: String?,
        startLabel: String,
        startAt: Long,
        endAt: Long,
    ) {
        if (Prefs.isIslandMode(app)) {
            IslandNotifications.onAlarm(
                context = app,
                title = title,
                note = note,
                kind = kind,
                id = id,
                date = date,
                startLabel = startLabel,
                startAt = startAt,
                endAt = endAt,
            )
            return
        }
        val power = app.getSystemService(PowerManager::class.java)
        val wakeLock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "studyreminder:alarm")
        wakeLock?.acquire(20_000L)

        val notification = NotificationHelper.build(
            context = app,
            title = title,
            note = note,
            kind = kind,
            whenLabel = whenLabel,
            ongoing = true,
        )
        NotificationHelper.notify(app, notification)
        // The alarm broadcast may open the page. A later start from the
        // foreground service is blocked on Android 14+ while the screen is in use.
        openReminder(app, title, note, kind, whenLabel)

        val service = Intent(app, RingingService::class.java).apply {
            action = RingingService.ACTION_START
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
            putExtra(AlarmContract.EXTRA_KIND, kind)
            putExtra(AlarmContract.EXTRA_ID, id)
            putExtra(AlarmContract.EXTRA_WHEN, whenLabel)
        }
        try {
            ContextCompat.startForegroundService(app, service)
        } catch (e: Exception) {
            Log.e("StudyReminder", "foreground service blocked, notification only", e)
            NotificationHelper.playFallbackSound(app)
        }
    }

    private fun openReminder(
        context: Context,
        title: String,
        note: String,
        kind: String,
        whenLabel: String,
    ) {
        val ui = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
            putExtra(AlarmContract.EXTRA_KIND, kind)
            putExtra(AlarmContract.EXTRA_WHEN, whenLabel)
        }
        val create = ActivityOptions.makeBasic()
        val send = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            create.setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS,
            )
            send.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            create.setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
            send.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        val pending = PendingIntent.getActivity(
            context,
            9,
            ui,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            create.toBundle(),
        )
        try {
            pending.send(context, 0, null, null, null, null, send.toBundle())
        } catch (e: Exception) {
            Log.w("StudyReminder", "reminder page blocked", e)
        }
    }
}
