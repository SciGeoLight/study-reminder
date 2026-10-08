package com.partner.studyreminder.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickerLogicTest {
    @Test
    fun movingStartPastTheEndKeepsTheSpan() {
        assertEquals(12 * 60, endWhenStartMoves(9 * 60, 10 * 60, 11 * 60))
        assertEquals(10 * 60, endWhenStartMoves(9 * 60, 10 * 60, 8 * 60))
        assertNull(endWhenStartMoves(9 * 60, null, 11 * 60))
        assertEquals(11 * 60 + 45, endWhenStartMoves(10 * 60, 10 * 60, 11 * 60))
        assertEquals(25 * 60, endWhenStartMoves(23 * 60, 25 * 60, 22 * 60))
    }

    @Test
    fun movingStartPastTheEndKeepsTheDaySpan() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 3)
        assertEquals(LocalDate.of(2026, 10, 8), endWhenStartDateMoves(start, end, LocalDate.of(2026, 10, 6)))
        assertEquals(end, endWhenStartDateMoves(start, end, LocalDate.of(2026, 10, 2)))
        assertEquals(LocalDate.of(2026, 10, 9), endWhenStartDateMoves(start, start, LocalDate.of(2026, 10, 9)))
        assertEquals(start, endNotBefore(start, start.minusDays(2)))
        assertEquals(end, endNotBefore(start, end))
    }
}
