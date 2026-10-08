package com.partner.studyreminder.ui

import com.partner.studyreminder.data.PlanItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSlotsTest {
    private val day = LocalDate.of(2026, 9, 27)

    @Test
    fun gapsOfFifteenMinutesOrMoreBecomeBreaks() {
        val items = listOf(
            slot("a", 8 * 60, 9 * 60),
            slot("b", 9 * 60 + 14, 10 * 60),
            slot("c", 10 * 60 + 15, 11 * 60),
        )
        val entries = timelineEntries(items)
        assertEquals(4, entries.size)
        assertTrue(entries[0] is TimelineEntry.Slot)
        assertTrue(entries[1] is TimelineEntry.Slot)
        val gap = entries[2] as TimelineEntry.Gap
        assertEquals(15, gap.minutes)
        assertEquals("b", gap.afterId)
        assertTrue(entries[3] is TimelineEntry.Slot)
    }

    @Test
    fun finishedCountUsesPastSlots() {
        val items = listOf(slot("a", 8 * 60, 9 * 60), slot("b", 10 * 60, 11 * 60))
        assertEquals(2, finishedCount(items, day, day.plusDays(1), 0))
        assertEquals(0, finishedCount(items, day, day.minusDays(1), 0))
        assertEquals(1, finishedCount(items, day, day, 9 * 60 + 30))
    }

    @Test
    fun nextLabelGivesWayToTheDayLength() {
        val items = listOf(slot("a", 8 * 60, 9 * 60), slot("b", 10 * 60, 11 * 60 + 30))
        assertEquals("下一段 08:00", nextOrTotalLabel(items, day, day.minusDays(1), 0))
        assertEquals("共 2小时30分钟", nextOrTotalLabel(items, day, day.plusDays(1), 12 * 60))
        assertEquals("没有安排", nextOrTotalLabel(emptyList(), day, day, 0))
    }

    private fun slot(id: String, start: Int, end: Int) = PlanItem(
        id = id,
        date = day.toString(),
        startMinutes = start,
        endMinutes = end,
        title = id,
        note = "",
        source = PlanItem.SOURCE_MANUAL,
    )
}
