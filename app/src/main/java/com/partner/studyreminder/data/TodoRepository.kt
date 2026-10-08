package com.partner.studyreminder.data

import com.partner.studyreminder.parse.ParsedTodo
import java.io.File
import java.util.UUID

/**
 * Hand-made to-dos and ones synced from a #TODO block live in one file.
 * Sync updates text and dates for the same id, and never clears a done check.
 * A synced id that leaves the file is removed only while it is still undone.
 *
 * Groups are local. Sync may create a group the first time a line names it,
 * and will not rename, reorder, recolor, or delete a group. If the person
 * has moved a synced todo into a group by hand, later syncs leave that
 * assignment alone. A line without `组:` does not clear a group already set.
 * Photos are stored beside this file and are never part of sync.
 */
class TodoRepository(private val file: File) {
    private val lock = Any()

    fun all(): List<Todo> = synchronized(lock) { readStore().todos.toList() }

    fun groups(): List<TodoGroup> = synchronized(lock) { readStore().groups.toList() }

    fun add(
        title: String,
        note: String,
        startDate: String,
        endDate: String,
        remindDate: String?,
        remindMinutes: Int?,
        groupId: String? = null,
        images: List<String> = emptyList(),
        id: String = UUID.randomUUID().toString(),
    ): Todo = synchronized(lock) {
        val store = readStore()
        val todo = Todo(
            id = id,
            title = title,
            note = note,
            startDate = startDate,
            endDate = endDate,
            remindDate = remindDate,
            remindMinutes = remindMinutes,
            done = false,
            source = Todo.SOURCE_MANUAL,
            groupId = groupId?.takeIf { gid -> store.groups.any { it.id == gid } },
            groupManual = groupId != null,
            images = images.filter { TodoImages.safeName(it) },
        )
        store.todos += todo
        writeStore(store)
        todo
    }

    fun insert(todo: Todo) = synchronized(lock) {
        val store = readStore()
        if (store.todos.any { it.id == todo.id }) return
        store.todos += todo
        writeStore(store)
    }

    fun update(
        id: String,
        title: String,
        note: String,
        startDate: String,
        endDate: String,
        remindDate: String?,
        remindMinutes: Int?,
        done: Boolean,
        groupId: String?,
        images: List<String>,
        groupChanged: Boolean,
    ): Todo? = synchronized(lock) {
        val store = readStore()
        val index = store.todos.indexOfFirst { it.id == id }
        if (index < 0) return null
        val current = store.todos[index]
        val resolvedGroup = groupId?.takeIf { gid -> store.groups.any { it.id == gid } }
        val updated = current.copy(
            title = title,
            note = note,
            startDate = startDate,
            endDate = endDate,
            remindDate = remindDate,
            remindMinutes = remindMinutes,
            done = done,
            groupId = resolvedGroup,
            groupManual = current.groupManual || groupChanged,
            images = images.filter { TodoImages.safeName(it) },
        )
        store.todos[index] = updated
        writeStore(store)
        updated
    }

    fun setDone(id: String, done: Boolean): Todo? = synchronized(lock) {
        val store = readStore()
        val index = store.todos.indexOfFirst { it.id == id }
        if (index < 0) return null
        val updated = store.todos[index].copy(done = done)
        store.todos[index] = updated
        writeStore(store)
        updated
    }

    fun delete(id: String): Todo? = synchronized(lock) {
        val store = readStore()
        val removed = store.todos.firstOrNull { it.id == id } ?: return null
        store.todos.removeAll { it.id == id }
        writeStore(store)
        removed
    }

    fun purgeImages(id: String) {
        TodoImages.deleteAll(file.parentFile ?: return, id)
    }

    fun addGroup(name: String, color: String? = null): TodoGroup? = synchronized(lock) {
        val clean = cleanName(name) ?: return null
        val store = readStore()
        if (store.groups.any { it.name == clean }) return null
        val group = TodoGroup(
            id = UUID.randomUUID().toString(),
            name = clean,
            color = color?.takeIf { it in TodoGroup.COLORS } ?: TodoGroup.nextColor(store.groups.size),
            source = TodoGroup.SOURCE_MANUAL,
        )
        store.groups += group
        writeStore(store)
        group
    }

    fun renameGroup(id: String, name: String): Boolean = synchronized(lock) {
        val clean = cleanName(name) ?: return false
        val store = readStore()
        if (store.groups.any { it.id != id && it.name == clean }) return false
        val index = store.groups.indexOfFirst { it.id == id }
        if (index < 0) return false
        store.groups[index] = store.groups[index].copy(name = clean)
        writeStore(store)
        true
    }

    fun recolorGroup(id: String, color: String): Boolean = synchronized(lock) {
        if (color !in TodoGroup.COLORS) return false
        val store = readStore()
        val index = store.groups.indexOfFirst { it.id == id }
        if (index < 0) return false
        store.groups[index] = store.groups[index].copy(color = color)
        writeStore(store)
        true
    }

    fun moveGroup(id: String, direction: Int): Boolean = synchronized(lock) {
        val store = readStore()
        val index = store.groups.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in store.groups.indices) return false
        val item = store.groups.removeAt(index)
        store.groups.add(target, item)
        writeStore(store)
        true
    }

    /**
     * @param deleteTodos true removes the todos in the group; false clears their
     * group and marks the choice as manual so a later sync does not put them back.
     */
    fun deleteGroup(id: String, deleteTodos: Boolean) = synchronized(lock) {
        val store = readStore()
        if (store.groups.none { it.id == id }) return
        store.groups.removeAll { it.id == id }
        if (deleteTodos) {
            val gone = store.todos.filter { it.groupId == id }
            store.todos.removeAll { it.groupId == id }
            writeStore(store)
            gone.forEach { purgeImages(it.id) }
        } else {
            for (index in store.todos.indices) {
                val todo = store.todos[index]
                if (todo.groupId == id) {
                    store.todos[index] = todo.copy(groupId = null, groupManual = true)
                }
            }
            writeStore(store)
        }
    }

    fun mergeSync(incoming: List<ParsedTodo>): TodoMerge = synchronized(lock) {
        val store = readStore()
        val incomingIds = incoming.map { it.id }.toSet()
        var added = 0
        var updated = 0
        var removed = 0
        for (parsed in incoming) {
            val index = store.todos.indexOfFirst { it.id == parsed.id }
            if (index < 0) {
                val groupId = if (parsed.groupName != null) groupIdFor(store, parsed.groupName) else null
                store.todos += Todo(
                    id = parsed.id,
                    title = parsed.title,
                    note = parsed.note,
                    startDate = parsed.startDate,
                    endDate = parsed.endDate,
                    remindDate = parsed.remindDate,
                    remindMinutes = parsed.remindMinutes,
                    done = false,
                    source = Todo.SOURCE_SYNC,
                    groupId = groupId,
                    groupManual = false,
                    images = emptyList(),
                )
                added++
                continue
            }
            val current = store.todos[index]
            if (current.source != Todo.SOURCE_SYNC) continue
            val groupId = when {
                current.groupManual -> current.groupId
                parsed.groupName != null -> groupIdFor(store, parsed.groupName)
                else -> current.groupId
            }
            store.todos[index] = current.copy(
                title = parsed.title,
                note = parsed.note,
                startDate = parsed.startDate,
                endDate = parsed.endDate,
                remindDate = parsed.remindDate,
                remindMinutes = parsed.remindMinutes,
                groupId = groupId,
            )
            updated++
        }
        val drop = store.todos.filter { it.source == Todo.SOURCE_SYNC && it.id !in incomingIds && !it.done }
        if (drop.isNotEmpty()) {
            val dropIds = drop.map { it.id }.toSet()
            store.todos.removeAll { it.id in dropIds }
            removed = drop.size
        }
        writeStore(store)
        drop.forEach { purgeImages(it.id) }
        TodoMerge(added, updated, removed)
    }

    private fun groupIdFor(store: Store, name: String): String? {
        val clean = cleanName(name) ?: return null
        val found = store.groups.firstOrNull { it.name == clean }
        if (found != null) return found.id
        val group = TodoGroup(
            id = UUID.randomUUID().toString(),
            name = clean,
            color = TodoGroup.nextColor(store.groups.size),
            source = TodoGroup.SOURCE_SYNC,
        )
        store.groups += group
        return group.id
    }

    private fun cleanName(raw: String): String? {
        val name = raw.trim().replace(Regex("\\s+"), " ")
        if (name.isEmpty() || name.length > 24) return null
        if (name.any { it == '|' || it == '｜' || it == '\n' }) return null
        return name
    }

    private class Store(
        val groups: MutableList<TodoGroup> = mutableListOf(),
        val todos: MutableList<Todo> = mutableListOf(),
    )

    private fun readStore(): Store {
        if (!file.exists() || file.length() == 0L) return Store()
        val text = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        return try {
            decode(text)
        } catch (_: Exception) {
            Store()
        }
    }

    private fun writeStore(store: Store) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(MiniJson.write(encode(store)), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun encode(store: Store): Map<String, Any?> {
        return linkedMapOf(
            "groups" to store.groups.map { group ->
                linkedMapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "color" to group.color,
                    "source" to group.source,
                )
            },
            "todos" to store.todos.map { todo ->
                linkedMapOf(
                    "id" to todo.id,
                    "title" to todo.title,
                    "note" to todo.note,
                    "start" to todo.startDate,
                    "end" to todo.endDate,
                    "remindDate" to todo.remindDate,
                    "remindMinutes" to todo.remindMinutes,
                    "done" to todo.done,
                    "source" to todo.source,
                    "groupId" to todo.groupId,
                    "groupManual" to todo.groupManual,
                    "images" to todo.images,
                )
            },
        )
    }

    private fun decode(text: String): Store {
        val root = MiniJson.parse(text) as? Map<*, *> ?: return Store()
        val groups = (root["groups"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val name = map["name"] as? String ?: return@mapNotNull null
            TodoGroup(
                id = id,
                name = name,
                color = (map["color"] as? String)?.takeIf { it in TodoGroup.COLORS } ?: "blue",
                source = if (map["source"] == TodoGroup.SOURCE_SYNC) TodoGroup.SOURCE_SYNC else TodoGroup.SOURCE_MANUAL,
            )
        }
        val todos = (root["todos"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val title = map["title"] as? String ?: return@mapNotNull null
            val start = map["start"] as? String ?: return@mapNotNull null
            val groupId = map["groupId"] as? String
            Todo(
                id = id,
                title = title,
                note = map["note"] as? String ?: "",
                startDate = start,
                endDate = map["end"] as? String ?: start,
                remindDate = map["remindDate"] as? String,
                remindMinutes = when (val minutes = map["remindMinutes"]) {
                    is Int -> minutes
                    is Long -> minutes.toInt()
                    else -> null
                },
                done = map["done"] as? Boolean ?: false,
                source = if (map["source"] == Todo.SOURCE_SYNC) Todo.SOURCE_SYNC else Todo.SOURCE_MANUAL,
                groupId = groupId?.takeIf { gid -> groups.any { it.id == gid } },
                groupManual = map["groupManual"] as? Boolean ?: false,
                images = (map["images"] as? List<*>).orEmpty().mapNotNull { name ->
                    (name as? String)?.takeIf { TodoImages.safeName(it) }
                },
            )
        }
        return Store(groups.toMutableList(), todos.toMutableList())
    }
}
