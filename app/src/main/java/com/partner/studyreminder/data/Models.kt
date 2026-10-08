package com.partner.studyreminder.data

data class PlanItem(
    val id: String,
    val date: String,
    val startMinutes: Int,
    val endMinutes: Int?,
    val title: String,
    val note: String,
    val allDay: Boolean = false,
    /** manual, import, sync, or legacy when an older file has no source. */
    val source: String = SOURCE_LEGACY,
    /**
     * Set when a non-manual row is edited. The next import or sync skips an
     * incoming row with the same start and title, so the edit is not duplicated.
     */
    val anchor: String? = null,
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_IMPORT = "import"
        const val SOURCE_SYNC = "sync"
        const val SOURCE_LEGACY = "legacy"
    }
}

data class DayPlan(
    val date: String,
    val title: String,
    val items: List<PlanItem>,
)

data class Snooze(
    val id: String,
    val triggerAtMillis: Long,
    val title: String,
    val note: String,
)
