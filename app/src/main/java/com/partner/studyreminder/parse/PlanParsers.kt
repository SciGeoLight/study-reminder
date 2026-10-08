package com.partner.studyreminder.parse

object PlanParsers {
    fun parseAny(text: String): ParseResult {
        val head = text.trimStart().take(4000)
        return if (head.contains("BEGIN:VCALENDAR") || head.contains("BEGIN:VEVENT")) {
            IcsParser.parse(text)
        } else {
            TextPlanParser.parse(text)
        }
    }
}
