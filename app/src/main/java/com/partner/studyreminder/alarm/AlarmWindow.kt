package com.partner.studyreminder.alarm

/**
 * Which alarms are actually registered with the system.
 * Android allows 500 concurrent alarms per app. A semester file can be larger
 * than that, so only the soonest triggers are registered. The rest stay in
 * storage and move into the window the next time an alarm fires or the app
 * reschedules.
 */
data class AlarmSlot(
    val key: String,
    val triggerAt: Long,
)

object AlarmWindow {
    const val LIMIT = 80

    fun select(
        candidates: List<AlarmSlot>,
        now: Long,
        limit: Int = LIMIT,
        recentGraceMillis: Long = 0L,
    ): List<AlarmSlot> {
        val cutoff = now - recentGraceMillis
        return candidates
            .filter { it.triggerAt > cutoff }
            .sortedWith(compareBy({ it.triggerAt }, { it.key }))
            .distinctBy { it.key }
            .take(limit)
    }
}
