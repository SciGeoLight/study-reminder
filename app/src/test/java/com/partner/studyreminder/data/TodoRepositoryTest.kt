package com.partner.studyreminder.data

import com.partner.studyreminder.parse.ParsedTodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TodoRepositoryTest {
    @Test
    fun syncUpdatesTextAndKeepsTheCheck() {
        val repo = TodoRepository(temp())
        repo.mergeSync(listOf(sample("read", "旧标题")))
        val stored = repo.all().single()
        repo.setDone(stored.id, true)
        val merge = repo.mergeSync(listOf(sample("read", "新标题", note = "改过")))
        assertEquals(1, merge.updated)
        val after = repo.all().single()
        assertEquals("新标题", after.title)
        assertEquals("改过", after.note)
        assertTrue(after.done)
        assertEquals(Todo.SOURCE_SYNC, after.source)
    }

    @Test
    fun syncLeavesHandMadeItemsAlone() {
        val repo = TodoRepository(temp())
        val manual = repo.add("自己写的", "", "2026-10-01", "2026-10-01", null, null)
        repo.mergeSync(listOf(sample(manual.id, "想覆盖")))
        assertEquals("自己写的", repo.all().single().title)
        assertEquals(Todo.SOURCE_MANUAL, repo.all().single().source)
        repo.mergeSync(listOf(sample("other", "同步来的")))
        assertEquals(2, repo.all().size)
        assertTrue(repo.all().any { it.id == manual.id && it.title == "自己写的" })
    }

    @Test
    fun syncCreatesAGroupByNameAndDoesNotDuplicateIt() {
        val repo = TodoRepository(temp())
        repo.mergeSync(listOf(sample("read", "阅读", group = "阅读"), sample("lab", "作业", group = "阅读")))
        assertEquals(listOf("阅读"), repo.groups().map { it.name })
        assertEquals(TodoGroup.SOURCE_SYNC, repo.groups().single().source)
        assertEquals(setOf(repo.groups().single().id), repo.all().map { it.groupId }.toSet())
        repo.mergeSync(listOf(sample("read", "阅读", group = "阅读"), sample("lab", "作业", group = "阅读")))
        assertEquals(1, repo.groups().size)
    }

    @Test
    fun syncLeavesHandBuiltGroupsAndAManualAssignmentAlone() {
        val repo = TodoRepository(temp())
        val mine = repo.addGroup("阅读")!!
        repo.recolorGroup(mine.id, "red")
        repo.renameGroup(mine.id, "我的阅读")
        val moved = repo.addGroup("生活")!!
        repo.mergeSync(listOf(sample("read", "阅读", group = "阅读")))
        val synced = repo.all().single()
        assertEquals("阅读", repo.groups().first { it.id == synced.groupId }.name)
        repo.update(
            id = synced.id,
            title = synced.title,
            note = synced.note,
            startDate = synced.startDate,
            endDate = synced.endDate,
            remindDate = synced.remindDate,
            remindMinutes = synced.remindMinutes,
            done = false,
            groupId = moved.id,
            images = listOf("keep.jpg"),
            groupChanged = true,
        )
        repo.mergeSync(listOf(sample("read", "新标题", group = "作业")))
        val after = repo.all().single()
        assertEquals("新标题", after.title)
        assertEquals(moved.id, after.groupId)
        assertTrue(after.groupManual)
        assertEquals(listOf("keep.jpg"), after.images)
        assertEquals(listOf("我的阅读", "生活", "阅读"), repo.groups().map { it.name })
        assertEquals("red", repo.groups().first { it.name == "我的阅读" }.color)
    }

    @Test
    fun missingGroupFieldDoesNotClearAnExistingGroup() {
        val repo = TodoRepository(temp())
        repo.mergeSync(listOf(sample("read", "阅读", group = "阅读")))
        val groupId = repo.all().single().groupId
        repo.mergeSync(listOf(sample("read", "阅读")))
        assertEquals(groupId, repo.all().single().groupId)
        assertEquals(1, repo.groups().size)
    }

    @Test
    fun deletingAGroupCanMoveOrRemoveItsTodos() {
        val repo = TodoRepository(temp())
        val group = repo.addGroup("阅读")!!
        repo.add("留下", "", "2026-10-01", "2026-10-01", null, null, groupId = group.id)
        repo.add("丢掉", "", "2026-10-02", "2026-10-02", null, null, groupId = group.id)
        val other = repo.addGroup("生活")!!
        repo.deleteGroup(group.id, deleteTodos = false)
        assertTrue(repo.groups().none { it.id == group.id })
        assertEquals(2, repo.all().size)
        assertTrue(repo.all().all { it.groupId == null && it.groupManual })
        val again = repo.addGroup("阅读")!!
        repo.add("甲", "", "2026-10-01", "2026-10-01", null, null, groupId = again.id)
        repo.add("乙", "", "2026-10-01", "2026-10-01", null, null, groupId = other.id)
        repo.deleteGroup(again.id, deleteTodos = true)
        assertEquals(listOf("留下", "丢掉", "乙"), repo.all().map { it.title })
        assertEquals(listOf("生活"), repo.groups().map { it.name })
    }

    @Test
    fun missingUndoneSyncItemsGoAwayAndDoneOnesStay() {
        val repo = TodoRepository(temp())
        repo.mergeSync(listOf(sample("open", "未完成"), sample("shut", "已完成")))
        repo.setDone("shut", true)
        val merge = repo.mergeSync(emptyList())
        assertEquals(1, merge.removed)
        assertEquals(listOf("shut"), repo.all().map { it.id })
        assertTrue(repo.all().single().done)
        assertFalse(repo.all().single().title.isEmpty())
    }

    @Test
    fun oldFileWithoutGroupsStillLoads() {
        val file = temp()
        file.writeText("""{"todos":[{"id":"a","title":"旧","start":"2026-10-01","done":true,"source":"manual"}]}""")
        val repo = TodoRepository(file)
        val todo = repo.all().single()
        assertEquals("旧", todo.title)
        assertTrue(todo.done)
        assertNull(todo.groupId)
        assertTrue(todo.images.isEmpty())
        assertTrue(repo.groups().isEmpty())
    }

    private fun sample(id: String, title: String, note: String = "", group: String? = null) = ParsedTodo(
        id = id,
        title = title,
        note = note,
        startDate = "2026-10-01",
        endDate = "2026-10-07",
        remindDate = "2026-10-03",
        remindMinutes = 9 * 60,
        groupName = group,
    )

    private fun temp(): File = File.createTempFile("todos", ".json").also { it.delete() }
}
