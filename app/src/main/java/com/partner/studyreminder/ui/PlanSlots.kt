package com.partner.studyreminder.ui

import com.partner.studyreminder.data.PlanItem
import java.time.LocalDate

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

fun dateText(date: LocalDate, today: LocalDate): String {
    val weeks = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val label = "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weeks[date.dayOfWeek.value - 1]}"
    return if (date == today) "今天 · $label" else label
}
