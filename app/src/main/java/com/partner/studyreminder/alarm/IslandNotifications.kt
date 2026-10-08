package com.partner.studyreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.partner.studyreminder.R
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.ui.MainActivity

/**
 * Heads-up banner plus the ongoing countdown used by 「超级岛 + 悬浮通知」.
 * The full-screen ringing path does not come through here.
 */
object IslandNotifications {
    private const val TAG = "StudyReminder"
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val heads = NotificationChannel(
            AlarmContract.CHANNEL_HEADS,
            "悬浮提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "时间段开始时弹出的横幅。短提示音和一次短震动。"
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 160, 80, 160)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val ongoing = NotificationChannel(
            AlarmContract.CHANNEL_ONGOING,
            "进行中",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "时间段进行中的倒计时。到结束时间自动消失。"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(heads)
        manager.createNotificationChannel(ongoing)
    }

    fun onAlarm(
        context: Context,
        title: String,
        note: String,
        kind: String,
        id: String,
        date: String?,
        startLabel: String,
        startAt: Long,
        endAt: Long,
    ) {
        val app = context.applicationContext
        ensureChannels(app)
        val headline = headsUpTitle(kind, title, startLabel)
        postHeadsUp(app, headline, note.ifBlank { "到点了" }, title, note, id, date)
        if (kind != AlarmContract.KIND_START || endAt <= System.currentTimeMillis()) {
            Log.i(TAG, "heads-up only kind=$kind title=$headline")
            return
        }
        Prefs.setSuppressedBlockId(app, null)
        Prefs.setActiveBlockId(app, id)
        // Let the banner rank on its own before the silent countdown is posted.
        Handler(Looper.getMainLooper()).postDelayed({
            postOngoing(app, title, note, id, date, startLabel, startAt, endAt)
            AlarmScheduler.armBlockEnd(app, id, endAt)
            AlarmScheduler.armIslandWatch(app, endAt)
        }, 1_500L)
        Log.i(TAG, "island start id=$id until=$endAt title=$headline")
    }

    fun onBlockEnded(context: Context, id: String) {
        val app = context.applicationContext
        val active = Prefs.activeBlockId(app)
        val suppressed = Prefs.suppressedBlockId(app)
        if (active != id && suppressed != id) return
        AlarmScheduler.cancelIslandWatch(app)
        cancelOngoing(app)
        Prefs.setActiveBlockId(app, null)
        Prefs.setSuppressedBlockId(app, null)
        Log.i(TAG, "island end id=$id")
    }

    fun restoreOngoing(
        context: Context,
        title: String,
        note: String,
        id: String,
        date: String?,
        startLabel: String,
        startAt: Long,
        endAt: Long,
    ) {
        val app = context.applicationContext
        if (Prefs.suppressedBlockId(app) == id) return
        ensureChannels(app)
        Prefs.setActiveBlockId(app, id)
        postOngoing(app, title, note, id, date, startLabel, startAt, endAt)
        AlarmScheduler.armBlockEnd(app, id, endAt)
        AlarmScheduler.armIslandWatch(app, endAt)
    }

    fun isOngoingPosted(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.activeNotifications.any { it.id == AlarmContract.NOTIF_ONGOING }
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(AlarmContract.NOTIF_HEADS)
        manager.cancel(AlarmContract.NOTIF_ONGOING)
    }

    fun cancelOngoing(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(AlarmContract.NOTIF_ONGOING)
    }

    fun canPostPromoted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canPostPromotedNotifications()
    }

    private fun postHeadsUp(
        context: Context,
        headline: String,
        body: String,
        title: String,
        note: String,
        id: String,
        date: String?,
    ) {
        val open = openIntent(context, id, date, 21)
        val notification = Notification.Builder(context, AlarmContract.CHANNEL_HEADS)
            .setSmallIcon(R.drawable.ic_stat_arc)
            .setColor(0xFF007AFF.toInt())
            .setContentTitle(headline)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_MAX)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setGroup("study_heads_alert")
            .setGroupSummary(true)
            .setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
            .setContentIntent(open)
            .addAction(action(context, "知道了", AlarmContract.ACTION_DISMISS, title, note, 31))
            .addAction(action(context, "5分钟后再提醒", AlarmContract.ACTION_SNOOZE, title, note, 32))
            .build()
        attachFocus(
            context = context,
            notification = notification,
            title = headline,
            content = body,
            ticker = headline,
            aodTitle = headline,
            hintTitle = body,
            remainingMinutes = 0,
            progressPercent = 0,
            timeoutMinutes = 0,
            floatOnPost = true,
        )
        notify(context, AlarmContract.NOTIF_HEADS, notification)
    }

    private fun postOngoing(
        context: Context,
        title: String,
        note: String,
        id: String,
        date: String?,
        startLabel: String,
        startAt: Long,
        endAt: Long,
    ) {
        val now = System.currentTimeMillis()
        val span = (endAt - startAt).coerceAtLeast(1L)
        val elapsed = (now - startAt).coerceIn(0L, span)
        val percent = ((elapsed * 100) / span).toInt().coerceIn(0, 100)
        val remain = (((endAt - now).coerceAtLeast(0L) + 59_999L) / 60_000L).toInt()
        val timeoutMin = (((endAt - now).coerceAtLeast(60_000L)) / 60_000L).toInt().coerceAtLeast(1)
        val body = if (note.isBlank()) "进行中" else note
        val open = openIntent(context, id, date, 22)
        val builder = Notification.Builder(context, AlarmContract.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_arc)
            .setColor(0xFF007AFF.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(if (startLabel.isBlank()) "进行中" else "$startLabel 开始")
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(open)
            .setDeleteIntent(removedIntent(context))
            .addAction(action(context, "知道了", AlarmContract.ACTION_DISMISS, title, note, 33))
            .addAction(action(context, "5分钟后再提醒", AlarmContract.ACTION_SNOOZE, title, note, 34))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val style = Notification.ProgressStyle()
                .setProgress(percent)
                .setStyledByProgress(true)
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(100).setColor(0xFF007AFF.toInt()),
                )
            builder
                .setStyle(style)
                .setShortCriticalText("${remain}分钟")
                .setRequestPromotedOngoing(true)
        } else {
            builder.setProgress(100, percent, false)
        }
        val notification = builder.build()
        attachFocus(
            context = context,
            notification = notification,
            title = if (startLabel.isBlank()) title else "$startLabel 开始 · $title",
            content = body,
            ticker = "${remain}分钟",
            aodTitle = if (startLabel.isBlank()) title else "$startLabel $title",
            hintTitle = if (startLabel.isBlank()) "进行中" else "$startLabel 开始",
            remainingMinutes = remain,
            progressPercent = percent,
            timeoutMinutes = timeoutMin,
            floatOnPost = false,
        )
        notify(context, AlarmContract.NOTIF_ONGOING, notification)
    }

    private fun attachFocus(
        context: Context,
        notification: Notification,
        title: String,
        content: String,
        ticker: String,
        aodTitle: String,
        hintTitle: String,
        remainingMinutes: Int,
        progressPercent: Int,
        timeoutMinutes: Int,
        floatOnPost: Boolean,
    ) {
        if (!FocusParams.isXiaomi(Build.MANUFACTURER, Build.BRAND)) return
        val pics = Bundle()
        pics.putParcelable(
            FocusParams.PIC_ICON,
            Icon.createWithResource(context, R.drawable.ic_stat_arc),
        )
        notification.extras.putBundle("miui.focus.pics", pics)
        notification.extras.putString(
            "miui.focus.param",
            FocusParams.json(
                title = title,
                content = content,
                ticker = ticker,
                aodTitle = aodTitle,
                hintTitle = hintTitle,
                remainingMinutes = remainingMinutes,
                progressPercent = progressPercent,
                timeoutMinutes = timeoutMinutes,
                floatOnPost = floatOnPost,
            ),
        )
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    private fun headsUpTitle(kind: String, title: String, startLabel: String): String = when (kind) {
        AlarmContract.KIND_START ->
            if (startLabel.isBlank()) title else "$startLabel 开始 · $title"
        AlarmContract.KIND_PRE ->
            if (startLabel.isBlank()) "即将开始：$title" else "$startLabel 即将开始 · $title"
        AlarmContract.KIND_SNOOZE -> "再提醒 · $title"
        AlarmContract.KIND_TODO -> "待办：$title"
        else -> title
    }

    private fun openIntent(context: Context, id: String, date: String?, request: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmContract.EXTRA_ID, id)
            if (!date.isNullOrBlank()) putExtra(AlarmContract.EXTRA_DATE, date)
        }
        return PendingIntent.getActivity(context, request, intent, FLAGS)
    }

    private fun removedIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmContract.ACTION_ISLAND_REMOVED
        }
        return PendingIntent.getBroadcast(context, 41, intent, FLAGS)
    }

    private fun action(
        context: Context,
        label: String,
        actionName: String,
        title: String,
        note: String,
        request: Int,
    ): Notification.Action {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = actionName
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
        }
        val pending = PendingIntent.getBroadcast(context, request, intent, FLAGS)
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_arc),
            label,
            pending,
        ).build()
    }
}
