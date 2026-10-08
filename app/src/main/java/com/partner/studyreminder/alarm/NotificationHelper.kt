package com.partner.studyreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.partner.studyreminder.R
import com.partner.studyreminder.ui.ReminderActivity

object NotificationHelper {
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val silent = NotificationChannel(
            AlarmContract.CHANNEL_SILENT,
            "学习提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "到点弹出的学习提醒。声音由响铃服务循环播放。"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val loud = NotificationChannel(
            AlarmContract.CHANNEL_LOUD,
            "学习提醒（补响）",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "响铃服务起不来时，用这条通道补一次闹钟声。"
            setSound(
                alarmUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 400, 700, 400, 1000)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(silent)
        manager.createNotificationChannel(loud)
    }

    fun build(
        context: Context,
        title: String,
        note: String,
        kind: String,
        whenLabel: String,
        ongoing: Boolean,
        loud: Boolean = false,
    ): Notification {
        ensureChannels(context)
        val headline = headline(kind, title)
        val body = buildString {
            if (whenLabel.isNotBlank()) append(whenLabel)
            if (note.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(note)
            }
            if (isEmpty()) append("到点了")
        }
        val fullScreen = PendingIntent.getActivity(
            context,
            7,
            reminderIntent(context, title, note, kind, whenLabel),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = actionIntent(context, AlarmContract.ACTION_DISMISS, title, note, 11)
        val snooze = actionIntent(context, AlarmContract.ACTION_SNOOZE, title, note, 12)
        return NotificationCompat.Builder(
            context,
            if (loud) AlarmContract.CHANNEL_LOUD else AlarmContract.CHANNEL_SILENT,
        )
            .setSmallIcon(R.drawable.ic_stat_arc)
            .setColor(0xFFC4553A.toInt())
            .setContentTitle(headline)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(ongoing)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "知道了", dismiss)
            .addAction(0, "5分钟后再提醒", snooze)
            .build()
    }

    fun notify(context: Context, notification: Notification) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(AlarmContract.NOTIF_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(AlarmContract.NOTIF_ID)
    }

    fun playFallbackSound(context: Context) {
        // Re-post on the loud channel so the system plays the alarm tone once.
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val current = if (Build.VERSION.SDK_INT >= 23) {
            manager.activeNotifications.firstOrNull { it.id == AlarmContract.NOTIF_ID }
        } else {
            null
        }
        val title = current?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: "学习提醒"
        val text = current?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            .orEmpty()
        notify(
            context,
            build(
                context = context,
                title = title,
                note = text,
                kind = AlarmContract.KIND_START,
                whenLabel = "",
                ongoing = true,
                loud = true,
            ),
        )
    }

    fun headline(kind: String, title: String): String = when (kind) {
        AlarmContract.KIND_PRE -> "即将开始：$title"
        AlarmContract.KIND_TODO -> "待办：$title"
        else -> title
    }

    private fun reminderIntent(
        context: Context,
        title: String,
        note: String,
        kind: String,
        whenLabel: String,
    ): Intent {
        return Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
            putExtra(AlarmContract.EXTRA_KIND, kind)
            putExtra(AlarmContract.EXTRA_WHEN, whenLabel)
        }
    }

    private fun actionIntent(
        context: Context,
        action: String,
        title: String,
        note: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
