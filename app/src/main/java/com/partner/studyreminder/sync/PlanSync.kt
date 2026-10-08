package com.partner.studyreminder.sync

import android.content.Context
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanParsers
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object PlanSync {
    fun run(context: Context): String {
        val url = Prefs.syncUrl(context).trim()
        if (url.isEmpty()) {
            val message = "没有填写网址，不会自动同步"
            Prefs.setSyncMessage(context, message)
            return message
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            val message = "网址要以 http:// 或 https:// 开头"
            Prefs.setSyncMessage(context, message)
            return message
        }
        return try {
            val text = fetch(url)
            val parsed = PlanParsers.parseAny(text)
            if (parsed.items.isEmpty() && parsed.todos.isEmpty()) {
                val message = "同步完成，但没有识别到安排"
                Prefs.setSyncMessage(context, message)
                message
            } else {
                val parts = mutableListOf<String>()
                if (parsed.items.isNotEmpty()) {
                    val removed = Plans.of(context).replaceDates(parsed.items, PlanItem.SOURCE_SYNC)
                    AlarmScheduler.cancelItems(context, removed)
                    val dates = parsed.items.map { it.date }.distinct().sorted().joinToString("、")
                    parts += "已同步 ${parsed.items.size} 条（$dates）"
                }
                if (parsed.hasTodoSection) {
                    val merge = Todos.of(context).mergeSync(parsed.todos)
                    parts += "待办 ${parsed.todos.size} 条（新增 ${merge.added}，更新 ${merge.updated}）"
                }
                val scheduled = AlarmScheduler.rescheduleAll(context)
                if (scheduled.deferred > 0 || scheduled.limitHit) {
                    parts += "较远的提醒会临近时再排上"
                }
                val message = parts.joinToString("。")
                Prefs.setSyncMessage(context, message)
                message
            }
        } catch (e: Exception) {
            val message = "同步失败：" + (e.message ?: "未知错误")
            Prefs.setSyncMessage(context, message)
            message
        }
    }

    fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "StudyReminder/1.0")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            val charset = charsetOf(connection.contentType)
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > 2_000_000) error("文件超过 2MB")
                    out.write(buffer, 0, read)
                }
                return out.toByteArray().toString(charset).removePrefix("\uFEFF")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun charsetOf(contentType: String?): Charset {
        if (contentType.isNullOrBlank()) return Charsets.UTF_8
        val match = Regex("""charset=([^;]+)""", RegexOption.IGNORE_CASE).find(contentType) ?: return Charsets.UTF_8
        val name = match.groupValues[1].trim().trim('"')
        return try {
            Charset.forName(name)
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }
}
