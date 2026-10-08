package com.partner.studyreminder.ui

import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.parse.PlanTime
import java.time.LocalDate

sealed interface TimelineEntry {
    data class Slot(val item: PlanItem) : TimelineEntry
    data class Gap(val minutes: Int, val afterId: String) : TimelineEntry
}

enum class SlotState { PAST, CURRENT, NEXT, FUTURE }

fun slotState(
    item: PlanItem,
    items: List<PlanItem>,
    viewing: LocalDate,
    today: LocalDate,
    nowMin: Int,
): SlotState {
    if (viewing < today) return SlotState.PAST
    if (viewing > today) return SlotState.FUTURE
    val current = items
        .filter { it.startMinutes <= nowMin && nowMin < effectiveEnd(it, items) }
        .maxByOrNull { it.startMinutes }
    if (current?.id == item.id) return SlotState.CURRENT
    if (current == null) {
        val next = items.filter { it.startMinutes > nowMin }.minByOrNull { it.startMinutes }
        if (next?.id == item.id) return SlotState.NEXT
    }
    return if (item.startMinutes > nowMin) SlotState.FUTURE else SlotState.PAST
}

fun effectiveEnd(item: PlanItem, items: List<PlanItem>): Int {
    item.endMinutes?.let { return it }
    val next = items.filter { it.startMinutes > item.startMinutes }.minOfOrNull { it.startMinutes }
    return next ?: (item.startMinutes + 45)
}

/** A break of at least 15 minutes between two slots becomes its own row. */
fun timelineEntries(items: List<PlanItem>): List<TimelineEntry> {
    val sorted = items.sortedBy { it.startMinutes }
    val out = ArrayList<TimelineEntry>(sorted.size * 2)
    sorted.forEachIndexed { index, item ->
        if (index > 0) {
            val prev = sorted[index - 1]
            val gap = item.startMinutes - effectiveEnd(prev, sorted)
            if (gap >= 15) out += TimelineEntry.Gap(gap, prev.id)
        }
        out += TimelineEntry.Slot(item)
    }
    return out
}

fun finishedCount(
    items: List<PlanItem>,
    viewing: LocalDate,
    today: LocalDate,
    nowMin: Int,
): Int = items.count { slotState(it, items, viewing, today, nowMin) == SlotState.PAST }

/** Next upcoming slot, or the day's total length once nothing is left. */
fun nextOrTotalLabel(
    items: List<PlanItem>,
    viewing: LocalDate,
    today: LocalDate,
    nowMin: Int,
): String {
    if (items.isEmpty()) return "没有安排"
    val sorted = items.sortedBy { it.startMinutes }
    val next = when {
        viewing < today -> null
        viewing > today -> sorted.first()
        else -> sorted.firstOrNull { it.startMinutes > nowMin }
    }
    if (next != null) return "下一段 ${PlanTime.formatMinutes(next.startMinutes)}"
    val total = sorted.sumOf { (effectiveEnd(it, sorted) - it.startMinutes).coerceAtLeast(0) }
    return "共 ${formatSpan(total)}"
}

private fun formatSpan(minutes: Int): String {
    if (minutes < 60) return "${minutes}分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours}小时" else "${hours}小时${rest}分钟"
}

fun dateText(date: LocalDate, today: LocalDate): String {
    val weeks = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val label = "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weeks[date.dayOfWeek.value - 1]}"
    return if (date == today) "今天 · $label" else label
}
