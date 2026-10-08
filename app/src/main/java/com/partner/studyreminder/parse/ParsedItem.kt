package com.partner.studyreminder.parse

data class ParsedItem(
    val date: String,
    val startMinutes: Int,
    val endMinutes: Int?,
    val title: String,
    val note: String,
    val planTitle: String,
    val allDay: Boolean = false,
)

data class ParsedTodo(
    val id: String,
    val title: String,
    val note: String,
    val startDate: String,
    val endDate: String,
    val remindDate: String?,
    val remindMinutes: Int?,
    /** Null when the line has no `组:` field. Sync then leaves the current group alone. */
    val groupName: String? = null,
)

data class ParseResult(
    val items: List<ParsedItem>,
    val warnings: List<String>,
    val todos: List<ParsedTodo> = emptyList(),
    /** True when the file contained a #TODO heading, even if no row parsed. */
    val hasTodoSection: Boolean = false,
)
