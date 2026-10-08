package com.partner.studyreminder.alarm

object AlarmContract {
    const val ACTION_FIRE = "com.partner.studyreminder.action.FIRE"
    const val ACTION_DISMISS = "com.partner.studyreminder.action.DISMISS"
    const val ACTION_SNOOZE = "com.partner.studyreminder.action.SNOOZE"
    const val ACTION_ISLAND_REMOVED = "com.partner.studyreminder.action.ISLAND_REMOVED"

    const val EXTRA_TITLE = "title"
    const val EXTRA_NOTE = "note"
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"
    const val EXTRA_WHEN = "whenLabel"
    const val EXTRA_DATE = "date"
    const val EXTRA_START_LABEL = "startLabel"
    const val EXTRA_START_AT = "startAt"
    const val EXTRA_END_AT = "endAt"

    const val NOTIF_ID = 7101
    const val NOTIF_HEADS = 7102
    const val NOTIF_ONGOING = 7103
    const val CHANNEL_SILENT = "study_alarm_v1"
    const val CHANNEL_LOUD = "study_alarm_loud_v1"
    const val CHANNEL_HEADS = "study_heads_v1"
    const val CHANNEL_ONGOING = "study_ongoing_v1"

    const val KIND_START = "start"
    const val KIND_PRE = "pre"
    const val KIND_SNOOZE = "snooze"
    const val KIND_TEST = "test"
    const val KIND_END = "end"
    const val KIND_WATCH = "watch"
    const val KIND_TODO = "todo"
}
