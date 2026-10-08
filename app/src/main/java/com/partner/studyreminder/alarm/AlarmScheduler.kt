package com.partner.studyreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.data.Snooze
import com.partner.studyreminder.data.Todo
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.MainActivity
import com.partner.studyreminder.ui.TodosActivity
import java.util.UUID

data class ScheduleResult(
    val scheduled: Int,
    val deferred: Int,
    val limitHit: Boolean,
)

/**
 * Schedules every future slot with [AlarmManager.setAlarmClock]. That API is the
 * one most OEMs still honor while the phone is dozing or the app is not open.
 * Alarms are rewritten on boot, clock changes, app update, and every cold start.
 */
object AlarmScheduler {
    private const val TAG = "StudyReminder"
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    private const val WATCH_MS = 60_000L

    fun rescheduleAll(context: Context, fromFire: Boolean = false): ScheduleResult {
        val app = context.applicationContext
        val repo = Plans.of(app)
        val now = System.currentTimeMillis()
        val pre = Prefs.preMinutes(app)
        val items = repo.allItems()
        val candidates = mutableListOf<PendingAlarm>()
        val known = mutableSetOf<String>()
        for (item in items) {
            val startKey = "start:${item.id}"
            val preKey = "pre:${item.id}"
            known += startKey
            known += preKey
            val startAt = PlanTime.toEpochMillis(item.date, item.startMinutes)
            val endAt = endMillis(item, items)
            candidates += PendingAlarm(
                key = startKey,
                triggerAt = startAt,
                title = item.title,
                note = item.note,
                kind = AlarmContract.KIND_START,
                id = item.id,
                whenLabel = PlanTime.formatRange(item.startMinutes, item.endMinutes, item.allDay),
                date = item.date,
                startLabel = PlanTime.formatMinutes(item.startMinutes),
                startAt = startAt,
                endAt = endAt,
            )
            if (pre > 0) {
                candidates += PendingAlarm(
                    key = preKey,
                    triggerAt = startAt - pre * 60_000L,
                    title = item.title,
                    note = item.note,
                    kind = AlarmContract.KIND_PRE,
                    id = item.id,
                    whenLabel = "还有${pre}分钟 · ${PlanTime.formatMinutes(item.startMinutes)}",
                    date = item.date,
                    startLabel = PlanTime.formatMinutes(item.startMinutes),
                )
            }
        }
        for (snooze in repo.futureSnoozes(now)) {
            val key = "snooze:${snooze.id}"
            known += key
            candidates += snooze.toPending(key)
        }
        for (todo in Todos.of(app).all()) {
            val key = "todo:${todo.id}"
            known += key
            val remindAt = todoRemindAt(todo) ?: continue
            if (todo.done) continue
            candidates += PendingAlarm(
                key = key,
                triggerAt = remindAt,
                title = todo.title,
                note = todo.note,
                kind = AlarmContract.KIND_TODO,
                id = todo.id,
                whenLabel = "待办",
                date = todo.remindDate,
                startLabel = todo.remindMinutes?.let(PlanTime::formatMinutes).orEmpty(),
            )
        }
        val selected = AlarmWindow.select(
            candidates.map { AlarmSlot(it.key, it.triggerAt) },
            now = now,
            recentGraceMillis = if (fromFire) 0L else 120_000L,
        )
        val chosen = selected.map { it.key }.toSet()
        val byKey = candidates.associateBy { it.key }
        val previous = Prefs.scheduledAlarmKeys(app)
        for (key in (previous + known) - chosen) cancelKey(app, key)
        val kept = mutableSetOf<String>()
        var limitHit = false
        for (slot in selected) {
            val alarm = byKey[slot.key] ?: continue
            val ok = schedule(
                context = app,
                key = alarm.key,
                triggerAt = maxOf(alarm.triggerAt, now + 1_000L),
                title = alarm.title,
                note = alarm.note,
                kind = alarm.kind,
                id = alarm.id,
                whenLabel = alarm.whenLabel,
                date = alarm.date,
                startLabel = alarm.startLabel,
                startAt = alarm.startAt,
                endAt = alarm.endAt,
            )
            if (!ok) {
                limitHit = true
                break
            }
            kept += alarm.key
        }
        Prefs.setScheduledAlarmKeys(app, kept)
        restoreOngoing(app)
        val deferred = (candidates.size - selected.size).coerceAtLeast(0)
        Log.i(TAG, "rescheduled ${kept.size} alarms, deferred=$deferred, limitHit=$limitHit, preMinutes=$pre")
        return ScheduleResult(kept.size, deferred, limitHit)
    }

    private data class PendingAlarm(
        val key: String,
        val triggerAt: Long,
        val title: String,
        val note: String,
        val kind: String,
        val id: String,
        val whenLabel: String,
        val date: String?,
        val startLabel: String = "",
        val startAt: Long = 0L,
        val endAt: Long = 0L,
    )

    private fun Snooze.toPending(key: String) = PendingAlarm(
        key = key,
        triggerAt = triggerAtMillis,
        title = title,
        note = note,
        kind = AlarmContract.KIND_SNOOZE,
        id = id,
        whenLabel = "再提醒",
        date = null,
    )

    private fun todoRemindAt(todo: Todo): Long? {
        val date = todo.remindDate ?: return null
        val minutes = todo.remindMinutes ?: return null
        return runCatching { PlanTime.toEpochMillis(date, minutes) }.getOrNull()
    }

    /**
     * If a block is in progress and its notification was swiped away, post it again.
     * Does nothing when the user asked to snooze, or when the notification is already up.
     */
    fun ensureIsland(context: Context) {
        val app = context.applicationContext
        if (!Prefs.isIslandMode(app)) {
            cancelIslandWatch(app)
            return
        }
        val open = currentBlock(app)
        if (open == null) {
            cancelIslandWatch(app)
            if (Prefs.activeBlockId(app) != null) {
                IslandNotifications.cancelOngoing(app)
                Prefs.setActiveBlockId(app, null)
            }
            return
        }
        if (Prefs.suppressedBlockId(app) == open.item.id) {
            cancelIslandWatch(app)
            return
        }
        if (!IslandNotifications.isOngoingPosted(app)) {
            Log.i(TAG, "island restore id=${open.item.id} title=${open.item.title}")
            IslandNotifications.restoreOngoing(
                context = app,
                title = open.item.title,
                note = open.item.note,
                id = open.item.id,
                date = open.item.date,
                startLabel = PlanTime.formatMinutes(open.item.startMinutes),
                startAt = open.startAt,
                endAt = open.endAt,
            )
        } else {
            armIslandWatch(app, open.endAt)
        }
    }

    fun armIslandWatch(context: Context, endAt: Long) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = watchIntent(app)
        val next = System.currentTimeMillis() + WATCH_MS
        if (next >= endAt) {
            alarmManager.cancel(pi)
            return
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } catch (security: SecurityException) {
            Log.e(TAG, "island watch denied", security)
        }
    }

    fun cancelIslandWatch(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(watchIntent(app))
    }

    fun cancelItems(context: Context, items: List<PlanItem>) {
        for (item in items) {
            cancelKey(context, "start:${item.id}")
            cancelKey(context, "pre:${item.id}")
        }
    }

    fun scheduleTest(context: Context) {
        val at = System.currentTimeMillis() + 5_000L
        schedule(
            context = context.applicationContext,
            key = "test",
            triggerAt = at,
            title = "测试提醒",
            note = "听到声音、感觉到震动，并看到这条，说明提醒是通的。",
            kind = AlarmContract.KIND_TEST,
            id = "test",
            whenLabel = "测试",
            date = null,
        )
    }

    fun scheduleSnooze(context: Context, title: String, note: String) {
        val app = context.applicationContext
        val id = UUID.randomUUID().toString()
        val at = System.currentTimeMillis() + 5 * 60_000L
        Plans.of(app).addSnooze(Snooze(id, at, title, note))
        schedule(
            context = app,
            key = "snooze:$id",
            triggerAt = at,
            title = title,
            note = note,
            kind = AlarmContract.KIND_SNOOZE,
            id = id,
            whenLabel = "再提醒",
            date = null,
        )
    }

    fun upcomingCount(context: Context): Int {
        val now = System.currentTimeMillis()
        val pre = Prefs.preMinutes(context)
        var count = 0
        for (item in Plans.of(context).allItems()) {
            val startAt = PlanTime.toEpochMillis(item.date, item.startMinutes)
            if (startAt > now) count++
            if (pre > 0 && startAt - pre * 60_000L > now) count++
        }
        count += Plans.of(context).futureSnoozes(now).count { it.triggerAtMillis > now }
        return count
    }

    fun armBlockEnd(context: Context, id: String, endAt: Long) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(app, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            data = Uri.parse("studyreminder://alarm/block-end")
            putExtra(AlarmContract.EXTRA_KIND, AlarmContract.KIND_END)
            putExtra(AlarmContract.EXTRA_ID, id)
        }
        val pi = PendingIntent.getBroadcast(app, requestCode("block-end"), intent, FLAGS)
        if (endAt <= System.currentTimeMillis()) {
            alarmManager.cancel(pi)
            return
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
        } catch (security: SecurityException) {
            Log.e(TAG, "block end alarm denied", security)
        }
    }

    private fun endMillis(item: PlanItem, items: List<PlanItem>): Long {
        val endMin = item.endMinutes
            ?: items
                .filter { it.date == item.date && it.startMinutes > item.startMinutes }
                .minOfOrNull { it.startMinutes }
            ?: (item.startMinutes + 45)
        return PlanTime.toEpochMillis(item.date, endMin)
    }

    private fun restoreOngoing(context: Context) {
        if (!Prefs.isIslandMode(context)) return
        val open = currentBlock(context)
        if (open == null) {
            cancelIslandWatch(context)
            if (Prefs.activeBlockId(context) != null) {
                IslandNotifications.cancelOngoing(context)
                Prefs.setActiveBlockId(context, null)
            }
            return
        }
        IslandNotifications.restoreOngoing(
            context = context,
            title = open.item.title,
            note = open.item.note,
            id = open.item.id,
            date = open.item.date,
            startLabel = PlanTime.formatMinutes(open.item.startMinutes),
            startAt = open.startAt,
            endAt = open.endAt,
        )
    }

    private data class OpenBlock(val item: PlanItem, val startAt: Long, val endAt: Long)

    private fun currentBlock(context: Context): OpenBlock? {
        val items = Plans.of(context).allItems()
        val now = System.currentTimeMillis()
        val item = items
            .filter { candidate ->
                val start = PlanTime.toEpochMillis(candidate.date, candidate.startMinutes)
                val end = endMillis(candidate, items)
                now in start until end
            }
            .maxByOrNull { it.startMinutes }
            ?: return null
        return OpenBlock(
            item = item,
            startAt = PlanTime.toEpochMillis(item.date, item.startMinutes),
            endAt = endMillis(item, items),
        )
    }

    private fun watchIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            data = Uri.parse("studyreminder://alarm/island-watch")
            putExtra(AlarmContract.EXTRA_KIND, AlarmContract.KIND_WATCH)
        }
        return PendingIntent.getBroadcast(context, requestCode("island-watch"), intent, FLAGS)
    }

    private fun schedule(
        context: Context,
        key: String,
        triggerAt: Long,
        title: String,
        note: String,
        kind: String,
        id: String,
        whenLabel: String,
        date: String?,
        startLabel: String = "",
        startAt: Long = 0L,
        endAt: Long = 0L,
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val operation = operationIntent(
            context, key, title, note, kind, id, whenLabel, date, startLabel, startAt, endAt,
        )
        val operationPi = PendingIntent.getBroadcast(context, requestCode(key), operation, FLAGS)
        val show = if (kind == AlarmContract.KIND_TODO) {
            Intent(context, TodosActivity::class.java)
        } else {
            Intent(context, MainActivity::class.java)
        }.apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (date != null) putExtra(AlarmContract.EXTRA_DATE, date)
        }
        val showPi = PendingIntent.getActivity(context, requestCode("show:$key"), show, FLAGS)
        return try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), operationPi)
            true
        } catch (security: SecurityException) {
            Log.e(TAG, "setAlarmClock denied for $key", security)
            false
        } catch (limit: IllegalStateException) {
            Log.e(TAG, "setAlarmClock stopped at $key", limit)
            false
        } catch (error: Exception) {
            Log.e(TAG, "setAlarmClock failed for $key", error)
            false
        }
    }

    private fun cancelKey(context: Context, key: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val operation = operationIntent(context, key, "", "", AlarmContract.KIND_START, "", "")
        val pi = PendingIntent.getBroadcast(context, requestCode(key), operation, FLAGS)
        alarmManager.cancel(pi)
    }

    private fun operationIntent(
        context: Context,
        key: String,
        title: String,
        note: String,
        kind: String,
        id: String,
        whenLabel: String,
        date: String? = null,
        startLabel: String = "",
        startAt: Long = 0L,
        endAt: Long = 0L,
    ): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmContract.ACTION_FIRE
            data = Uri.parse("studyreminder://alarm/${Uri.encode(key)}")
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
            putExtra(AlarmContract.EXTRA_KIND, kind)
            putExtra(AlarmContract.EXTRA_ID, id)
            putExtra(AlarmContract.EXTRA_WHEN, whenLabel)
            if (!date.isNullOrBlank()) putExtra(AlarmContract.EXTRA_DATE, date)
            putExtra(AlarmContract.EXTRA_START_LABEL, startLabel)
            putExtra(AlarmContract.EXTRA_START_AT, startAt)
            putExtra(AlarmContract.EXTRA_END_AT, endAt)
        }
    }

    private fun requestCode(key: String): Int = key.hashCode()

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }
}
