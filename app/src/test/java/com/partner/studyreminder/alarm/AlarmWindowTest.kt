package com.partner.studyreminder.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmWindowTest {
    @Test
    fun aThousandItemsStayInsideTheBudget() {
        val now = 1_700_000_000_000L
        val candidates = (0 until 1_200).map { index ->
            AlarmSlot("start:$index", now + index * 60_000L + 1_000L)
        }
        val selected = AlarmWindow.select(candidates, now)
        assertEquals(AlarmWindow.LIMIT, selected.size)
        assertEquals("start:0", selected.first().key)
        assertEquals("start:${AlarmWindow.LIMIT - 1}", selected.last().key)
        assertTrue(selected.all { it.triggerAt > now })
    }

    @Test
    fun pastAlarmsDropUnlessTheyJustStarted() {
        val now = 5_000_000L
        val candidates = listOf(
            AlarmSlot("old", now - 10 * 60_000L),
            AlarmSlot("just", now - 30_000L),
            AlarmSlot("next", now + 60_000L),
        )
        assertEquals(listOf("next"), AlarmWindow.select(candidates, now).map { it.key })
        assertEquals(
            listOf("just", "next"),
            AlarmWindow.select(candidates, now, recentGraceMillis = 120_000L).map { it.key },
        )
    }

    @Test
    fun duplicateKeysKeepTheEarlierTrigger() {
        val now = 0L
        val selected = AlarmWindow.select(
            listOf(
                AlarmSlot("start:a", 5_000L),
                AlarmSlot("start:a", 9_000L),
                AlarmSlot("pre:a", 1_000L),
            ),
            now,
        )
        assertEquals(listOf("pre:a", "start:a"), selected.map { it.key })
        assertEquals(5_000L, selected[1].triggerAt)
    }
}
