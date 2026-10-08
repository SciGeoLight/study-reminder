package com.partner.studyreminder.data

data class TodoGroup(
    val id: String,
    val name: String,
    val color: String,
    val source: String,
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_SYNC = "sync"
        val COLORS = listOf("blue", "green", "orange", "red")

        fun nextColor(existing: Int): String = COLORS[existing.coerceAtLeast(0) % COLORS.size]
    }
}

data class Todo(
    val id: String,
    val title: String,
    val note: String,
    val startDate: String,
    val endDate: String,
    val remindDate: String?,
    val remindMinutes: Int?,
    val done: Boolean,
    val source: String,
    val groupId: String? = null,
    /**
     * Set when the person moves this todo into a group (or out of one) by hand.
     * Later syncs keep that choice and only update the words and dates.
     */
    val groupManual: Boolean = false,
    /** Filenames inside the todo's private image folder. Not part of sync. */
    val images: List<String> = emptyList(),
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_SYNC = "sync"
    }
}

data class TodoMerge(
    val added: Int,
    val updated: Int,
    val removed: Int,
)
