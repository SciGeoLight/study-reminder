package com.partner.studyreminder.data

import com.partner.studyreminder.parse.ParsedItem
import java.io.File
import java.util.UUID

/**
 * One JSON file in app storage. Import and sync replace only their own source
 * on the dates in the payload, plus untagged legacy rows. Manual rows stay.
 * Writes go to a temp file and then replace the real file.
 */
class PlanRepository(private val file: File) {
    private val lock = Any()

    fun day(date: String): DayPlan = synchronized(lock) {
        read().days[date] ?: DayPlan(date, "", emptyList())
    }

    fun allItems(): List<PlanItem> = synchronized(lock) {
        read().days.values.flatMap { it.items }
    }

    fun countsByDate(): Map<String, Int> = synchronized(lock) {
        read().days.mapValues { it.value.items.size }
    }

    /**
     * Replaces [source] rows and untagged legacy rows on each date in [items].
     * Manual rows, and rows from the other explicit source, stay.
     * An incoming row is skipped when a kept manual row has the same start and
     * title, or an anchor pointing at that start and title.
     * Returns only the rows that were actually removed.
     */
    fun replaceDates(items: List<ParsedItem>, source: String = PlanItem.SOURCE_IMPORT): List<PlanItem> =
        synchronized(lock) {
            val store = read()
            val removed = mutableListOf<PlanItem>()
            val grouped = items.groupBy { it.date }
            for ((date, incoming) in grouped) {
                val existing = store.days[date]
                val kept = existing?.items.orEmpty().filter { item ->
                    val drop = item.source == source || item.source == PlanItem.SOURCE_LEGACY
                    if (drop) removed += item
                    !drop
                }
                val protected = protectedKeys(kept)
                val incomingTitle = incoming.lastOrNull { it.planTitle.isNotBlank() }?.planTitle
                val dayTitle = incomingTitle ?: existing?.title.orEmpty()
                val saved = incoming.mapNotNull { parsed ->
                    val key = slotKey(parsed.startMinutes, parsed.title)
                    if (key in protected) return@mapNotNull null
                    PlanItem(
                        id = UUID.randomUUID().toString(),
                        date = date,
                        startMinutes = parsed.startMinutes,
                        endMinutes = parsed.endMinutes,
                        title = parsed.title,
                        note = parsed.note,
                        allDay = parsed.allDay,
                        source = source,
                    )
                }
                val merged = (kept + saved).sortedBy { it.startMinutes }
                if (merged.isEmpty()) store.days.remove(date)
                else store.days[date] = DayPlan(date, dayTitle, merged)
            }
            write(store)
            removed
        }

    fun addItem(
        date: String,
        startMinutes: Int,
        endMinutes: Int?,
        title: String,
        note: String,
    ): PlanItem = synchronized(lock) {
        val store = read()
        val item = PlanItem(
            id = UUID.randomUUID().toString(),
            date = date,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            title = title,
            note = note,
            allDay = false,
            source = PlanItem.SOURCE_MANUAL,
        )
        val day = store.days[date]
        val items = (day?.items.orEmpty() + item).sortedBy { it.startMinutes }
        store.days[date] = DayPlan(date, day?.title.orEmpty(), items)
        write(store)
        item
    }

    fun updateItem(
        id: String,
        date: String,
        startMinutes: Int,
        endMinutes: Int?,
        title: String,
        note: String,
    ): PlanItem? = synchronized(lock) {
        val store = read()
        var origin: String? = null
        var old: PlanItem? = null
        for ((dayDate, day) in store.days) {
            val found = day.items.firstOrNull { it.id == id } ?: continue
            origin = dayDate
            old = found
            break
        }
        val previous = old ?: return null
        val from = origin ?: return null
        val anchor = if (previous.source == PlanItem.SOURCE_MANUAL) {
            previous.anchor
        } else {
            slotKey(previous.startMinutes, previous.title)
        }
        val updated = previous.copy(
            date = date,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            title = title,
            note = note,
            allDay = false,
            source = PlanItem.SOURCE_MANUAL,
            anchor = anchor,
        )
        val originDay = store.days.getValue(from)
        val left = originDay.items.filterNot { it.id == id }
        if (date == from) {
            store.days[from] = originDay.copy(items = (left + updated).sortedBy { it.startMinutes })
        } else {
            if (left.isEmpty()) store.days.remove(from) else store.days[from] = originDay.copy(items = left)
            val target = store.days[date]
            val items = (target?.items.orEmpty() + updated).sortedBy { it.startMinutes }
            store.days[date] = DayPlan(date, target?.title.orEmpty(), items)
        }
        write(store)
        updated
    }

    /** How many rows on [date] an import or sync of [source] would replace. */
    fun replaceableCount(date: String, source: String): Int = synchronized(lock) {
        read().days[date]?.items.orEmpty().count { item ->
            item.source == source || item.source == PlanItem.SOURCE_LEGACY
        }
    }

    fun deleteItem(id: String): PlanItem? = synchronized(lock) {
        val store = read()
        var removed: PlanItem? = null
        val dates = store.days.keys.toList()
        for (date in dates) {
            val day = store.days.getValue(date)
            val item = day.items.firstOrNull { it.id == id } ?: continue
            removed = item
            val left = day.items.filterNot { it.id == id }
            if (left.isEmpty()) store.days.remove(date) else store.days[date] = day.copy(items = left)
            break
        }
        if (removed != null) write(store)
        removed
    }

    /** Puts [item] back with its original id and source. Used by the home undo bar. */
    fun insertItem(item: PlanItem, dayTitle: String = "") = synchronized(lock) {
        val store = read()
        val already = store.days.values.any { day -> day.items.any { it.id == item.id } }
        if (already) return@synchronized
        val existing = store.days[item.date]
        val title = existing?.title?.takeIf { it.isNotBlank() } ?: dayTitle
        val items = (existing?.items.orEmpty() + item).sortedBy { it.startMinutes }
        store.days[item.date] = DayPlan(item.date, title, items)
        write(store)
    }

    fun clearDay(date: String): List<PlanItem> = synchronized(lock) {
        val store = read()
        val removed = store.days[date]?.items.orEmpty()
        if (removed.isNotEmpty()) {
            store.days.remove(date)
            write(store)
        }
        removed
    }

    fun addSnooze(snooze: Snooze) = synchronized(lock) {
        val store = read()
        store.snoozes += snooze
        write(store)
    }

    fun removeSnooze(id: String) = synchronized(lock) {
        val store = read()
        val before = store.snoozes.size
        store.snoozes.removeAll { it.id == id }
        if (store.snoozes.size != before) write(store)
    }

    /** Drops snoozes that are already more than two minutes late. Returns the rest. */
    fun futureSnoozes(nowMillis: Long): List<Snooze> = synchronized(lock) {
        val store = read()
        val keep = store.snoozes.filter { it.triggerAtMillis > nowMillis - 120_000L }
        if (keep.size != store.snoozes.size) {
            store.snoozes.clear()
            store.snoozes += keep
            write(store)
        }
        keep
    }

    private fun read(): Store {
        if (!file.exists() || file.length() == 0L) return Store()
        val text = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        return try {
            decode(text)
        } catch (_: Exception) {
            val broken = File(file.parentFile, file.name + ".bad")
            runCatching { file.copyTo(broken, overwrite = true) }
            Store()
        }
    }

    private fun write(store: Store) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(MiniJson.write(encode(store)), Charsets.UTF_8)
        if (file.exists()) {
            runCatching { file.copyTo(File(file.path + ".bak"), overwrite = true) }
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private class Store {
        val days: MutableMap<String, DayPlan> = linkedMapOf()
        val snoozes: MutableList<Snooze> = mutableListOf()
    }

    private fun encode(store: Store): Map<String, Any?> {
        val days = linkedMapOf<String, Any?>()
        for ((date, day) in store.days) {
            days[date] = linkedMapOf(
                "date" to day.date,
                "title" to day.title,
                "items" to day.items.map { item ->
                    linkedMapOf(
                        "id" to item.id,
                        "date" to item.date,
                        "start" to item.startMinutes,
                        "end" to item.endMinutes,
                        "title" to item.title,
                        "note" to item.note,
                        "allDay" to item.allDay,
                        "source" to item.source,
                        "anchor" to item.anchor,
                    )
                },
            )
        }
        return linkedMapOf(
            "days" to days,
            "snoozes" to store.snoozes.map { snooze ->
                linkedMapOf(
                    "id" to snooze.id,
                    "at" to snooze.triggerAtMillis,
                    "title" to snooze.title,
                    "note" to snooze.note,
                )
            },
        )
    }

    private fun decode(text: String): Store {
        val root = MiniJson.parse(text) as? Map<*, *> ?: error("根不是对象")
        val store = Store()
        val days = root["days"] as? Map<*, *> ?: emptyMap<Any, Any>()
        for ((key, value) in days) {
            val dayMap = value as? Map<*, *> ?: continue
            val date = (dayMap["date"] as? String) ?: key.toString()
            val title = dayMap["title"] as? String ?: ""
            val items = (dayMap["items"] as? List<*>).orEmpty().mapNotNull { raw ->
                val map = raw as? Map<*, *> ?: return@mapNotNull null
                val id = map["id"] as? String ?: return@mapNotNull null
                val itemTitle = map["title"] as? String ?: return@mapNotNull null
                PlanItem(
                    id = id,
                    date = map["date"] as? String ?: date,
                    startMinutes = asLong(map["start"])?.toInt() ?: 0,
                    endMinutes = asLong(map["end"])?.toInt(),
                    title = itemTitle,
                    note = map["note"] as? String ?: "",
                    allDay = map["allDay"] as? Boolean ?: false,
                    source = when (val raw = map["source"] as? String) {
                        PlanItem.SOURCE_MANUAL, PlanItem.SOURCE_IMPORT, PlanItem.SOURCE_SYNC -> raw
                        else -> PlanItem.SOURCE_LEGACY
                    },
                    anchor = (map["anchor"] as? String)?.takeIf { it.isNotBlank() },
                )
            }
            store.days[date] = DayPlan(date, title, items)
        }
        val snoozes = root["snoozes"] as? List<*> ?: emptyList<Any>()
        for (raw in snoozes) {
            val map = raw as? Map<*, *> ?: continue
            val id = map["id"] as? String ?: continue
            val at = asLong(map["at"]) ?: continue
            store.snoozes += Snooze(
                id = id,
                triggerAtMillis = at,
                title = map["title"] as? String ?: "学习提醒",
                note = map["note"] as? String ?: "",
            )
        }
        return store
    }

    private fun slotKey(startMinutes: Int, title: String) = "$startMinutes|$title"

    private fun protectedKeys(kept: List<PlanItem>): Set<String> {
        val keys = HashSet<String>()
        for (item in kept) {
            if (item.source != PlanItem.SOURCE_MANUAL) continue
            keys += slotKey(item.startMinutes, item.title)
            item.anchor?.let { keys += it }
        }
        return keys
    }

    private fun asLong(value: Any?): Long? = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}
