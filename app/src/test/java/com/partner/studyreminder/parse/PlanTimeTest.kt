package com.partner.studyreminder.parse

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PlanTimeTest {
    @Test
    fun eightAmBeijingIsUtcMidnight() {
        assertEquals(
            Instant.parse("2026-09-27T00:00:00Z").toEpochMilli(),
            PlanTime.toEpochMillis("2026-09-27", 8 * 60),
        )
    }

    @Test
    fun lastSlotIs2205Beijing() {
        assertEquals(
            Instant.parse("2026-09-27T14:05:00Z").toEpochMilli(),
            PlanTime.toEpochMillis("2026-09-27", 22 * 60 + 5),
        )
        assertEquals("22:05", PlanTime.formatMinutes(22 * 60 + 5))
        assertEquals("次日00:10", PlanTime.formatMinutes(24 * 60 + 10))
        assertEquals("08:00–08:20", PlanTime.formatRange(8 * 60, 8 * 60 + 20))
        assertEquals("全天", PlanTime.formatRange(0, null, allDay = true))
    }

    @Test
    fun clockTextRoundTripsAndCrossesMidnight() {
        assertEquals(8 * 60 + 40, PlanTime.parseClock("08:40"))
        assertEquals(8 * 60 + 40, PlanTime.parseClock("8:40"))
        assertEquals(24 * 60 + 10, PlanTime.parseClock("次日00:10"))
        assertEquals(null, PlanTime.parseClock("25:00"))
        assertEquals(null, PlanTime.parseClock("八点"))
        assertEquals(9 * 60, PlanTime.endAfter(8 * 60, 9 * 60))
        assertEquals(24 * 60 + 10, PlanTime.endAfter(23 * 60 + 50, 10))
        assertEquals(24 * 60 + 10, PlanTime.endAfter(23 * 60 + 50, 24 * 60 + 10))
    }
}
