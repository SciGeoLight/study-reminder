package com.partner.studyreminder.ui

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
}
