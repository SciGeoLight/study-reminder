package com.partner.studyreminder.parse

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Plan clock. Every stored time is Beijing time (Asia/Shanghai), no daylight saving. */
object PlanTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    fun iso(year: Int, month: Int, day: Int): String = "%04d-%02d-%02d".format(year, month, day)

    fun formatMinutes(total: Int): String {
        val days = Math.floorDiv(total, 24 * 60)
        val minutes = Math.floorMod(total, 24 * 60)
        val clock = "%02d:%02d".format(minutes / 60, minutes % 60)
        return if (days > 0) "次日$clock" else clock
    }

    fun formatRange(start: Int, end: Int?, allDay: Boolean = false): String {
        if (allDay) return "全天"
        if (end == null) return formatMinutes(start)
        return formatMinutes(start) + "–" + formatMinutes(end)
    }

    fun toEpochMillis(date: String, minutes: Int): Long {
        val day = LocalDate.parse(date)
        return ZonedDateTime.of(day, LocalTime.MIDNIGHT, ZONE)
            .plusMinutes(minutes.toLong())
            .toInstant()
            .toEpochMilli()
    }

    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(ZONE).toLocalDate()

    fun nowMinutes(now: Instant = Instant.now()): Int {
        val local = now.atZone(ZONE)
        return local.hour * 60 + local.minute
    }

    private val clockPattern = Regex("""^(次日)?\s*(\d{1,2}):(\d{2})$""")

    /** `08:40` or `次日00:10`. Returns null when the text is not a clock. */
    fun parseClock(text: String): Int? {
        val match = clockPattern.matchEntire(text.trim()) ?: return null
        val hour = match.groupValues[2].toInt()
        val minute = match.groupValues[3].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        val base = hour * 60 + minute
        return if (match.groupValues[1].isNotEmpty()) base + 24 * 60 else base
    }

    /**
     * Same rule as the text importer: an end at or before the start crosses midnight.
     * An end already marked 次日 is left as written.
     */
    fun endAfter(startMinutes: Int, endClock: Int): Int {
        return if (endClock <= startMinutes) endClock + 24 * 60 else endClock
    }
}
