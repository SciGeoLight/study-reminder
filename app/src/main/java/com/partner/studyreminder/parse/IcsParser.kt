package com.partner.studyreminder.parse

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Reads VEVENT blocks. UTC (`Z`), TZID, and floating local times are supported.
 * Floating times are read as Asia/Shanghai. Everything is stored as Beijing local time.
 * VALARM blocks are ignored so their DESCRIPTION does not overwrite the event note.
 */
object IcsParser {
    private val basic = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val basicShort = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
    private val dateOnly = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val durationRe = Regex(
        """^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): ParseResult {
        val warnings = mutableListOf<String>()
        val unfolded = unfold(text)
        val calendarName = unfolded.firstNotNullOfOrNull { line ->
            val prop = parseProperty(line) ?: return@firstNotNullOfOrNull null
            if (prop.name == "X-WR-CALNAME") unescape(prop.value).trim() else null
        }.orEmpty()

        val items = mutableListOf<ParsedItem>()
        for (eventLines in extractEvents(unfolded)) {
            val props = linkedMapOf<String, Property>()
            for (line in eventLines) {
                val prop = parseProperty(line) ?: continue
                if (prop.name !in props) props[prop.name] = prop
            }
            val startProp = props["DTSTART"]
            if (startProp == null) {
                warnings += "有一条日程没有开始时间，已跳过"
                continue
            }
            val start = parseStamp(startProp.value, startProp.params)
            if (start == null) {
                warnings += "无法解析开始时间：${startProp.value}"
                continue
            }
            val endProp = props["DTEND"]
            val endStamp = if (endProp != null) parseStamp(endProp.value, endProp.params) else null
            val durationMinutes = props["DURATION"]?.let { parseDurationMinutes(it.value) }
            val (endMinutes, allDay) = resolveEnd(start, endStamp, durationMinutes)
            val summary = unescape(props["SUMMARY"]?.value.orEmpty()).trim().ifEmpty { "未命名" }
            val description = unescape(props["DESCRIPTION"]?.value.orEmpty()).trim()
            items += ParsedItem(
                date = start.date.toString(),
                startMinutes = if (start.allDay) 0 else start.minuteOfDay,
                endMinutes = endMinutes,
                title = summary,
                note = description,
                planTitle = calendarName,
                allDay = allDay,
            )
        }
        return ParseResult(items, warnings)
    }

    private fun resolveEnd(
        start: Stamp,
        end: Stamp?,
        durationMinutes: Int?,
    ): Pair<Int?, Boolean> {
        if (start.allDay) {
            return null to true
        }
        if (end != null && !end.allDay) {
            val startDateTime = start.date.atStartOfDay().plusMinutes(start.minuteOfDay.toLong())
            val endDateTime = end.date.atStartOfDay().plusMinutes(end.minuteOfDay.toLong())
            val between = ChronoUnit.MINUTES.between(startDateTime, endDateTime).toInt()
            if (between > 0) return (start.minuteOfDay + between) to false
        }
        if (durationMinutes != null && durationMinutes > 0) {
            return (start.minuteOfDay + durationMinutes) to false
        }
        return null to false
    }

    private data class Stamp(val date: LocalDate, val minuteOfDay: Int, val allDay: Boolean)

    private fun parseStamp(rawValue: String, params: Map<String, String>): Stamp? {
        val value = rawValue.trim()
        if (value.isEmpty()) return null
        val valueType = params["VALUE"]?.uppercase()
        val dateValue = value.length == 8 && value.all { it.isDigit() }
        if (valueType == "DATE" || dateValue) {
            return try {
                Stamp(LocalDate.parse(value.take(8), dateOnly), 0, true)
            } catch (_: DateTimeException) {
                null
            }
        }
        val utc = value.endsWith("Z", ignoreCase = true)
        val bare = if (utc) value.dropLast(1) else value
        val local = parseLocal(bare) ?: return null
        val zone = when {
            utc -> ZoneOffset.UTC
            params["TZID"] != null -> resolveZone(params.getValue("TZID"))
            else -> PlanTime.ZONE
        }
        val shanghai = ZonedDateTime.of(local, zone).withZoneSameInstant(PlanTime.ZONE)
        return Stamp(
            date = shanghai.toLocalDate(),
            minuteOfDay = shanghai.hour * 60 + shanghai.minute,
            allDay = false,
        )
    }

    private fun parseLocal(value: String): LocalDateTime? {
        return try {
            when (value.length) {
                15 -> LocalDateTime.parse(value, basic)
                13 -> LocalDateTime.parse(value, basicShort)
                else -> null
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun resolveZone(raw: String): ZoneId {
        val tzid = raw.trim().trim('"')
        val alias = when (tzid.lowercase()) {
            "china standard time", "cst", "prc" -> "Asia/Shanghai"
            else -> tzid
        }
        return try {
            ZoneId.of(alias)
        } catch (_: DateTimeException) {
            PlanTime.ZONE
        }
    }

    private fun parseDurationMinutes(raw: String): Int? {
        val match = durationRe.matchEntire(raw.trim()) ?: return null
        val days = match.groupValues[1].toIntOrNull() ?: 0
        val hours = match.groupValues[2].toIntOrNull() ?: 0
        val minutes = match.groupValues[3].toIntOrNull() ?: 0
        val seconds = match.groupValues[4].toIntOrNull() ?: 0
        return days * 24 * 60 + hours * 60 + minutes + if (seconds >= 30) 1 else 0
    }

    private fun unfold(text: String): List<String> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = mutableListOf<String>()
        for (line in lines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + line.substring(1)
            } else if (line.isNotEmpty()) {
                out.add(line)
            }
        }
        return out
    }

    private fun extractEvents(lines: List<String>): List<List<String>> {
        val events = mutableListOf<List<String>>()
        var depth = 0
        var capturing = false
        var captureDepth = 0
        var skippingAlarm = false
        val current = mutableListOf<String>()
        for (line in lines) {
            val begin = componentName(line, "BEGIN")
            val end = componentName(line, "END")
            if (begin != null) {
                if (begin.equals("VEVENT", true) && !capturing) {
                    capturing = true
                    captureDepth = depth
                    current.clear()
                } else if (begin.equals("VALARM", true) && capturing) {
                    skippingAlarm = true
                }
                depth++
                continue
            }
            if (end != null) {
                depth--
                if (skippingAlarm && end.equals("VALARM", true)) {
                    skippingAlarm = false
                } else if (capturing && end.equals("VEVENT", true) && depth == captureDepth) {
                    events += current.toList()
                    capturing = false
                    skippingAlarm = false
                    current.clear()
                }
                continue
            }
            if (capturing && !skippingAlarm) current += line
        }
        return events
    }

    private fun componentName(line: String, keyword: String): String? {
        val prefix = "$keyword:"
        if (!line.startsWith(prefix, ignoreCase = true)) return null
        return line.substring(prefix.length).trim()
    }

    private data class Property(val name: String, val params: Map<String, String>, val value: String)

    private fun parseProperty(line: String): Property? {
        val splitAt = splitColon(line) ?: return null
        val (left, value) = splitAt
        if (left.isEmpty()) return null
        val parts = splitSemicolons(left)
        val name = parts.first().trim().uppercase()
        if (name.isEmpty()) return null
        val params = linkedMapOf<String, String>()
        for (part in parts.drop(1)) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val key = part.substring(0, eq).trim().uppercase()
            var paramValue = part.substring(eq + 1).trim()
            if (paramValue.length >= 2 && paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                paramValue = paramValue.substring(1, paramValue.length - 1)
            }
            params[key] = paramValue
        }
        return Property(name, params, value)
    }

    private fun splitColon(line: String): Pair<String, String>? {
        var quoted = false
        for (index in line.indices) {
            when (line[index]) {
                '"' -> quoted = !quoted
                ':' -> if (!quoted) return line.substring(0, index) to line.substring(index + 1)
            }
        }
        return null
    }

    private fun splitSemicolons(left: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        for (ch in left) {
            when {
                ch == '"' -> {
                    quoted = !quoted
                    current.append(ch)
                }
                ch == ';' && !quoted -> {
                    parts += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        parts += current.toString()
        return parts
    }

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> out.append('\n')
                    '\\', ',', ';' -> out.append(next)
                    else -> out.append(next)
                }
                index += 2
            } else {
                out.append(ch)
                index++
            }
        }
        return out.toString()
    }
}
