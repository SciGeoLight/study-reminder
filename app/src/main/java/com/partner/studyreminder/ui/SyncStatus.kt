package com.partner.studyreminder.ui

import com.partner.studyreminder.alarm.AlarmWindow
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.parse.PlanTime
import java.time.Instant
import java.time.ZoneId

/**
 * Settings copy for the last sync. Built from the stored sentence and the
 * alarm count. The raw exception text is never copied into these fields.
 */
internal data class SyncStatusCopy(
    val lastSuccess: String,
    val result: String,
    val suggestion: String?,
    val recognized: String,
    val scheduled: String,
    val overflow: String?,
)

internal fun syncStatusCopy(
    message: String,
    successAtMillis: Long,
    scheduledCount: Int,
    limit: Int = AlarmWindow.LIMIT,
    zone: ZoneId = PlanTime.ZONE,
): SyncStatusCopy {
    val failure = classifySyncFailure(message)
    val idle = message.isBlank() || message.startsWith("还没有同步过")
    val recognizedCount = recognizedCount(message)
    val overflow = message.contains("临近时再排上") || scheduledCount >= limit
    val result = when {
        failure != null -> failure.category
        idle -> "还没有同步过"
        message.startsWith("没有填写") || message.startsWith("网址要以") -> message
        else -> resultLine(message)
    }
    return SyncStatusCopy(
        lastSuccess = formatSyncSuccess(successAtMillis, zone),
        result = result,
        suggestion = failure?.suggestion,
        recognized = if (recognizedCount == null) "—" else "$recognizedCount 条",
        scheduled = "$scheduledCount/$limit",
        overflow = if (overflow) "临近时再排上" else null,
    )
}

internal fun formatSyncSuccess(atMillis: Long, zone: ZoneId = PlanTime.ZONE): String {
    if (atMillis <= 0L) return "还没有成功同步"
    val time = Instant.ofEpochMilli(atMillis).atZone(zone)
    return "${time.monthValue}月${time.dayOfMonth}日 " +
        "%02d:%02d".format(time.hour, time.minute)
}

internal fun recognizedCount(message: String): Int? {
    if (!Prefs.syncMessageRecordsSuccess(message)) return null
    if (message.contains("没有识别到安排")) return 0
    val plans = COUNT_PLAN.find(message)?.groupValues?.get(1)?.toIntOrNull()
    val todos = COUNT_TODO.find(message)?.groupValues?.get(1)?.toIntOrNull()
    if (plans == null && todos == null) return null
    return (plans ?: 0) + (todos ?: 0)
}

private val COUNT_PLAN = Regex("已同步\\s*(\\d+)\\s*条")
private val COUNT_TODO = Regex("待办\\s*(\\d+)\\s*条")

private fun resultLine(message: String): String {
    return message
        .replace("较远的提醒会临近时再排上", "")
        .trim()
        .trimEnd('。', '，', ' ')
        .ifBlank { "已完成" }
}

private data class SyncFailure(val category: String, val suggestion: String)

private fun classifySyncFailure(message: String): SyncFailure? {
    if (!message.startsWith("同步失败")) return null
    val detail = message.removePrefix("同步失败：").lowercase()
    return when {
        detail.contains("2mb") || detail.contains("超过") ->
            SyncFailure("内容太大", "把计划拆短一些，单次不要超过 2MB。")
        detail.contains("http") || detail.contains("response code") ->
            SyncFailure("网址没有正常返回", "检查网址是否还能打开。")
        detail.contains("timeout") ||
            detail.contains("timed out") ||
            detail.contains("unknownhost") ||
            detail.contains("unable to resolve") ||
            detail.contains("failed to connect") ||
            detail.contains("connect") ||
            detail.contains("network") ->
            SyncFailure("网络没有连上", "确认手机有网后再试一次。")
        else ->
            SyncFailure("这次没有同步成功", "过一会儿再试。如果一直失败，检查网址。")
    }
}
