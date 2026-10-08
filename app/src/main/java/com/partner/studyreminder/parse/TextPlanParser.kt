package com.partner.studyreminder.parse

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Plain-text plans the assistant pastes into chat.
 *
 * ```
 * #PLAN 2026-09-27 周日安排
 * 08:00-08:20 晨间阅读 | 读完一节就停
 * 08:00 只有开始时间
 * ```
 *
 * A `#PLAN` line sets the date for the item lines after it. Importing the same
 * date again replaces that day (the repository does the replace).
 */
object TextPlanParser {
    private val headerRe = Regex(
        """^[#＃]\s*PLAN\s+([0-9０-９]{4})\s*[-－—–./．]\s*([0-9０-９]{1,2})\s*[-－—–./．]\s*([0-9０-９]{1,2})\s*(.*)$""",
        RegexOption.IGNORE_CASE,
    )

    private val itemRe = Regex(
        """^([0-9０-９]{1,2})\s*[:：]\s*([0-9０-９]{1,2})(?:\s*[-－—–~～至到]+\s*([0-9０-９]{1,2})\s*[:：]\s*([0-9０-９]{1,2}))?\s+(.+)$""",
    )

    private val noteSplit = Regex("""\s*[|｜│]\s*""")
    private val todoHeaderRe = Regex("""^[#＃]\s*TODO\s*$""", RegexOption.IGNORE_CASE)
    private val dateRe = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    private val remindRe = Regex("""^(\d{4}-\d{2}-\d{2})[ T](\d{1,2}):(\d{2})$""")
    private val groupRe = Regex("""^组[:：]\s*(.+)$""")

    fun parse(text: String): ParseResult {
        val warnings = mutableListOf<String>()
        val items = mutableListOf<ParsedItem>()
        val todos = mutableListOf<ParsedTodo>()
        var date: String? = null
        var planTitle = ""
        var todoMode = false
        var sawTodo = false

        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        for (raw in normalized.split('\n')) {
            var line = raw.trim { it.isWhitespace() || it == '\uFEFF' }
            if (line.isEmpty() || line.startsWith("```")) continue
            line = line.replace('\u3000', ' ').replace('\u00A0', ' ')
            line = line.replaceFirst(Regex("""^[-*•]\s+"""), "")

            if (todoHeaderRe.matches(line)) {
                todoMode = true
                sawTodo = true
                date = null
                continue
            }

            val header = headerRe.matchEntire(line)
            if (header != null) {
                todoMode = false
                val year = digits(header.groupValues[1])
                val month = digits(header.groupValues[2])
                val day = digits(header.groupValues[3])
                try {
                    LocalDate.of(year, month, day)
                    date = PlanTime.iso(year, month, day)
                    planTitle = header.groupValues[4].trim()
                } catch (_: DateTimeException) {
                    date = null
                    warnings += "日期无效：${short(line)}"
                }
                continue
            }

            if (todoMode) {
                parseTodo(line, todos, warnings)
                continue
            }

            val item = itemRe.matchEntire(line)
            if (item == null) {
                warnings += "无法识别：${short(line)}"
                continue
            }
            val currentDate = date
            if (currentDate == null) {
                warnings += "这一行前面没有日期：${short(line)}"
                continue
            }

            val hour = digits(item.groupValues[1])
            val minute = digits(item.groupValues[2])
            if (hour !in 0..23 || minute !in 0..59) {
                warnings += "时间无效：${short(line)}"
                continue
            }
            val start = hour * 60 + minute
            var end: Int? = null
            if (item.groupValues[3].isNotEmpty()) {
                val endHour = digits(item.groupValues[3])
                val endMinute = digits(item.groupValues[4])
                if (endHour !in 0..23 || endMinute !in 0..59) {
                    warnings += "结束时间无效：${short(line)}"
                } else {
                    var endMinutes = endHour * 60 + endMinute
                    if (endMinutes <= start) endMinutes += 24 * 60
                    end = endMinutes
                }
            }

            val body = item.groupValues[5].trim()
            val parts = body.split(noteSplit, limit = 2)
            val title = parts[0].trim()
            val note = parts.getOrNull(1)?.trim().orEmpty()
            if (title.isEmpty()) {
                warnings += "缺少标题：${short(line)}"
                continue
            }
            items += ParsedItem(
                date = currentDate,
                startMinutes = start,
                endMinutes = end,
                title = title,
                note = note,
                planTitle = planTitle,
            )
        }
        return ParseResult(items, warnings, todos, sawTodo)
    }

    private fun parseTodo(line: String, todos: MutableList<ParsedTodo>, warnings: MutableList<String>) {
        val parts = line.split(noteSplit).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 3) {
            warnings += "待办写不全：${short(line)}"
            return
        }
        val id = parts[0]
        if (id.any { it.isWhitespace() } || id.length > 40) {
            warnings += "待办编号无效：${short(line)}"
            return
        }
        val title = parts[1]
        val start = canonicalDate(parts[2])
        if (title.isEmpty() || start == null) {
            warnings += "待办日期无效：${short(line)}"
            return
        }
        val startDate: String = start
        var end: String = startDate
        var remindDate: String? = null
        var remindMinutes: Int? = null
        var groupName: String? = null
        val notes = mutableListOf<String>()
        for (part in parts.drop(3)) {
            val group = groupRe.matchEntire(part)
            val remind = remindRe.matchEntire(part)
            val asDate = canonicalDate(part)
            if (group != null) {
                val name = group.groupValues[1].trim().replace(Regex("\\s+"), " ")
                if (name.isNotEmpty() && name.length <= 24) groupName = name
            } else if (remind != null && remindDate == null) {
                val hour = remind.groupValues[2].toInt()
                val minute = remind.groupValues[3].toInt()
                val day = canonicalDate(remind.groupValues[1])
                if (day == null || hour !in 0..23 || minute !in 0..59) {
                    warnings += "提醒时间无效：${short(line)}"
                } else {
                    remindDate = day
                    remindMinutes = hour * 60 + minute
                }
            } else if (asDate != null && end == startDate && remindDate == null) {
                end = asDate
            } else {
                notes += part
            }
        }
        if (end < startDate) {
            warnings += "结束日期早于开始：${short(line)}"
            return
        }
        todos += ParsedTodo(
            id = id,
            title = title,
            note = notes.joinToString(" | "),
            startDate = startDate,
            endDate = end,
            remindDate = remindDate,
            remindMinutes = remindMinutes,
            groupName = groupName,
        )
    }

    private fun canonicalDate(raw: String): String? {
        val match = dateRe.matchEntire(raw.trim()) ?: return null
        return try {
            val year = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val day = match.groupValues[3].toInt()
            java.time.LocalDate.of(year, month, day)
            PlanTime.iso(year, month, day)
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun digits(raw: String): Int {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            sb.append(if (ch in '０'..'９') '0' + (ch - '０') else ch)
        }
        return sb.toString().toInt()
    }

    private fun short(line: String): String = if (line.length > 42) line.take(42) + "…" else line
}
