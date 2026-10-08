package com.partner.studyreminder.ui

import com.partner.studyreminder.data.Todo
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoChipTest {
    @Test
    fun chipsFollowBucketOf() {
        val today = LocalDate.of(2026, 10, 8)
        val counts = todoChipCounts(
            listOf(
                todo("late", today.minusDays(3), today.minusDays(1), done = false),
                todo("due", today.minusDays(2), today, done = false),
                todo("later", today.plusDays(1), today.plusDays(1), done = false),
                todo("finished", today.minusDays(4), today.minusDays(1), done = true),
            ),
            today,
        )
        assertEquals(3, counts.open)
        assertEquals(1, counts.dueToday)
        assertEquals(1, counts.overdue)
    }

    private fun todo(id: String, start: LocalDate, end: LocalDate, done: Boolean) = Todo(
        id = id,
        title = id,
        note = "",
        startDate = start.toString(),
        endDate = end.toString(),
        remindDate = null,
        remindMinutes = null,
        done = done,
        source = Todo.SOURCE_MANUAL,
    )
}
