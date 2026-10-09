package com.partner.studyreminder.data

import android.content.Context

object Prefs {
    private const val NAME = "study_reminder_prefs"
    private const val SAW = "saw_permissions"
    private const val PRE_MINUTES = "pre_minutes"
    private const val SYNC_URL = "sync_url"
    private const val SYNC_MESSAGE = "sync_message"
    private const val AUTOSTART = "autostart_confirmed"
    private const val REMINDER_MODE = "reminder_mode"
    private const val ACTIVE_BLOCK = "active_block"
    private const val SUPPRESSED_BLOCK = "suppressed_block"
    private const val THEME = "theme"
    private const val SCHEDULED_ALARMS = "scheduled_alarms"
    private const val TODO_BAR_GROUPS = "todo_bar_groups"

    /** How many groups stay on the bottom capsule, besides 全部 and …. */
    const val TODO_BAR_LIMIT = 3

    const val MODE_ISLAND = "island"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val MODE_FULLSCREEN = "fullscreen"

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun sawPermissions(context: Context): Boolean = sp(context).getBoolean(SAW, false)

    fun setSawPermissions(context: Context, seen: Boolean) {
        sp(context).edit().putBoolean(SAW, seen).apply()
    }

    /** 0 means the pre-reminder is off. */
    fun preMinutes(context: Context): Int = sp(context).getInt(PRE_MINUTES, 0).coerceIn(0, 180)

    fun setPreMinutes(context: Context, minutes: Int) {
        sp(context).edit().putInt(PRE_MINUTES, minutes.coerceIn(0, 180)).apply()
    }

    fun syncUrl(context: Context): String = sp(context).getString(SYNC_URL, "").orEmpty()

    fun setSyncUrl(context: Context, url: String) {
        sp(context).edit().putString(SYNC_URL, url.trim()).apply()
    }

    fun syncMessage(context: Context): String = sp(context).getString(SYNC_MESSAGE, "").orEmpty()

    fun setSyncMessage(context: Context, message: String) {
        sp(context).edit().putString(SYNC_MESSAGE, message).apply()
    }

    fun autostartConfirmed(context: Context): Boolean = sp(context).getBoolean(AUTOSTART, false)

    fun setAutostartConfirmed(context: Context, confirmed: Boolean) {
        sp(context).edit().putBoolean(AUTOSTART, confirmed).apply()
    }

    /** Default is the heads-up + Super Island path. */
    fun reminderMode(context: Context): String =
        sp(context).getString(REMINDER_MODE, MODE_ISLAND) ?: MODE_ISLAND

    fun isIslandMode(context: Context): Boolean = reminderMode(context) != MODE_FULLSCREEN

    fun setReminderMode(context: Context, mode: String) {
        val value = if (mode == MODE_FULLSCREEN) MODE_FULLSCREEN else MODE_ISLAND
        sp(context).edit().putString(REMINDER_MODE, value).apply()
    }

    fun activeBlockId(context: Context): String? =
        sp(context).getString(ACTIVE_BLOCK, null)?.takeIf { it.isNotBlank() }

    fun setActiveBlockId(context: Context, id: String?) {
        sp(context).edit().putString(ACTIVE_BLOCK, id.orEmpty()).apply()
    }

    fun suppressedBlockId(context: Context): String? =
        sp(context).getString(SUPPRESSED_BLOCK, null)?.takeIf { it.isNotBlank() }

    fun setSuppressedBlockId(context: Context, id: String?) {
        sp(context).edit().putString(SUPPRESSED_BLOCK, id.orEmpty()).apply()
    }

    /** system, light, or dark. Missing and unknown values follow the system. */
    fun theme(context: Context): String {
        return when (sp(context).getString(THEME, THEME_SYSTEM)) {
            THEME_LIGHT -> THEME_LIGHT
            THEME_DARK -> THEME_DARK
            else -> THEME_SYSTEM
        }
    }

    fun setTheme(context: Context, theme: String) {
        val value = when (theme) {
            THEME_LIGHT -> THEME_LIGHT
            THEME_DARK -> THEME_DARK
            else -> THEME_SYSTEM
        }
        sp(context).edit().putString(THEME, value).apply()
    }

    fun scheduledAlarmKeys(context: Context): Set<String> =
        sp(context).getStringSet(SCHEDULED_ALARMS, emptySet()).orEmpty()

    fun setScheduledAlarmKeys(context: Context, keys: Set<String>) {
        sp(context).edit().putStringSet(SCHEDULED_ALARMS, keys.toSet()).apply()
    }

    /**
     * Ordered group ids pinned on the todo filter bar.
     * Null means the person has never chosen, so the caller uses the first groups.
     */
    fun todoBarGroups(context: Context): List<String>? {
        if (!sp(context).contains(TODO_BAR_GROUPS)) return null
        return sp(context).getString(TODO_BAR_GROUPS, "").orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(TODO_BAR_LIMIT)
    }

    fun setTodoBarGroups(context: Context, ids: List<String>) {
        val value = ids.distinct().take(TODO_BAR_LIMIT).joinToString(",")
        sp(context).edit().putString(TODO_BAR_GROUPS, value).apply()
    }
}
