package com.partner.studyreminder.data

import com.partner.studyreminder.parse.TextPlanParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlanRepositoryTest {
    @Test
    fun replacingADateKeepsOtherDays() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(
            TextPlanParser.parse(
                """
                #PLAN 2026-09-27 第一版
                08:00-08:20 阅读 | 旧
                09:00 作业
                """.trimIndent(),
            ).items,
        )
        val removed = repo.replaceDates(
            TextPlanParser.parse(
                """
                #PLAN 2026-09-27 第二版
                10:00-11:00 散步 | 新
                """.trimIndent(),
            ).items,
        )
        assertEquals(2, removed.size)
        val day = repo.day("2026-09-27")
        assertEquals("第二版", day.title)
        assertEquals(1, day.items.size)
        assertEquals("散步", day.items[0].title)
        assertEquals("新", day.items[0].note)
        assertEquals(10 * 60, day.items[0].startMinutes)
        assertEquals(11 * 60, day.items[0].endMinutes)

        repo.replaceDates(TextPlanParser.parse("#PLAN 2026-09-28 另一天\n08:00 家务 | 引号\"和换行").items)
        assertEquals("散步", repo.day("2026-09-27").items.single().title)
        assertEquals("家务", repo.day("2026-09-28").items.single().title)

        val reloaded = PlanRepository(file)
        assertEquals("家务", reloaded.day("2026-09-28").items.single().title)
        assertTrue(reloaded.day("2026-09-28").items.single().note.contains("引号\""))
    }

    @Test
    fun deleteAndClear() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(TextPlanParser.parse("#PLAN 2026-09-27 计划\n08:00 甲\n09:00 乙").items)
        val first = repo.day("2026-09-27").items.first()
        assertEquals("甲", repo.deleteItem(first.id)?.title)
        assertEquals(listOf("乙"), repo.day("2026-09-27").items.map { it.title })
        assertEquals(listOf("乙"), repo.clearDay("2026-09-27").map { it.title })
        assertTrue(repo.day("2026-09-27").items.isEmpty())
        assertNull(repo.deleteItem("missing"))
    }

    @Test
    fun updateKeepsIdAndResortsByStart() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(TextPlanParser.parse("#PLAN 2026-09-27 计划\n08:00-08:20 甲 | 旧\n09:00 乙").items)
        val first = repo.day("2026-09-27").items.first()
        val updated = repo.updateItem(first.id, "2026-09-27", 10 * 60, 11 * 60, "甲改", "新备注")
        assertEquals(first.id, updated?.id)
        assertEquals(PlanItem.SOURCE_MANUAL, updated?.source)
        assertEquals("${8 * 60}|甲", updated?.anchor)
        assertEquals(listOf("乙", "甲改"), repo.day("2026-09-27").items.map { it.title })
        val moved = repo.day("2026-09-27").items.last()
        assertEquals(10 * 60, moved.startMinutes)
        assertEquals(11 * 60, moved.endMinutes)
        assertEquals("新备注", moved.note)
        assertEquals(false, moved.allDay)
        val reloaded = PlanRepository(file)
        assertEquals("甲改", reloaded.day("2026-09-27").items.last().title)
        assertNull(repo.updateItem("missing", "2026-09-27", 8 * 60, null, "无", ""))
    }

    @Test
    fun manualSurvivesImportAndSync() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 导入日\n08:00 阅读").items,
            PlanItem.SOURCE_IMPORT,
        )
        val manual = repo.addItem("2026-09-27", 9 * 60, 10 * 60, "手写", "备注")
        repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 同步日\n08:30 作业").items,
            PlanItem.SOURCE_SYNC,
        )
        assertEquals(listOf("阅读", "作业", "手写"), repo.day("2026-09-27").items.map { it.title })
        assertEquals(PlanItem.SOURCE_IMPORT, repo.day("2026-09-27").items[0].source)
        assertEquals(PlanItem.SOURCE_SYNC, repo.day("2026-09-27").items[1].source)
        assertEquals(PlanItem.SOURCE_MANUAL, repo.day("2026-09-27").items[2].source)
        assertEquals(manual.id, repo.day("2026-09-27").items[2].id)

        val removed = repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 再同步\n12:00 散步").items,
            PlanItem.SOURCE_SYNC,
        )
        assertEquals(listOf("作业"), removed.map { it.title })
        val after = repo.day("2026-09-27").items
        assertEquals(listOf("阅读", "手写", "散步"), after.map { it.title })
        assertEquals(PlanItem.SOURCE_MANUAL, after[1].source)

        repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 再导入\n07:00 晨读").items,
            PlanItem.SOURCE_IMPORT,
        )
        val mixed = repo.day("2026-09-27").items
        assertEquals(listOf("晨读", "手写", "散步"), mixed.map { it.title })
        assertEquals(PlanItem.SOURCE_MANUAL, mixed[1].source)
        assertEquals(PlanItem.SOURCE_SYNC, mixed[2].source)
        val reloaded = PlanRepository(file)
        assertEquals("手写", reloaded.day("2026-09-27").items[1].title)
        assertEquals(PlanItem.SOURCE_MANUAL, reloaded.day("2026-09-27").items[1].source)
    }

    @Test
    fun legacyIsReplacedAndOtherSourceStays() {
        val file = tempFile()
        file.writeText(
            """
            {"days":{"2026-09-27":{"date":"2026-09-27","title":"旧","items":[
            {"id":"a","date":"2026-09-27","start":480,"end":500,"title":"旧项","note":"","allDay":false},
            {"id":"b","date":"2026-09-27","start":600,"title":"已同步","note":"","allDay":false,"source":"sync"}
            ]}},"snoozes":[]}
            """.trimIndent(),
        )
        val repo = PlanRepository(file)
        assertEquals(PlanItem.SOURCE_LEGACY, repo.day("2026-09-27").items.first().source)
        val removed = repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 新\n10:00 新项").items,
            PlanItem.SOURCE_IMPORT,
        )
        assertEquals(listOf("旧项"), removed.map { it.title })
        val items = repo.day("2026-09-27").items
        assertEquals(listOf("已同步", "新项"), items.map { it.title })
        assertEquals(PlanItem.SOURCE_SYNC, items[0].source)
        assertEquals(PlanItem.SOURCE_IMPORT, items[1].source)
    }

    @Test
    fun anchorSkipsTheOriginalIncomingRow() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 同步\n08:00-09:00 阅读").items,
            PlanItem.SOURCE_SYNC,
        )
        val item = repo.day("2026-09-27").items.single()
        val updated = repo.updateItem(item.id, "2026-09-27", 8 * 60, 9 * 60, "阅读改", "改过")
        assertEquals("${8 * 60}|阅读", updated?.anchor)
        val removed = repo.replaceDates(
            TextPlanParser.parse("#PLAN 2026-09-27 同步\n08:00-09:00 阅读\n11:00 作业").items,
            PlanItem.SOURCE_SYNC,
        )
        assertTrue(removed.isEmpty())
        val items = repo.day("2026-09-27").items
        assertEquals(listOf("阅读改", "作业"), items.map { it.title })
        assertEquals(PlanItem.SOURCE_MANUAL, items[0].source)
        assertEquals(item.id, items[0].id)
        assertEquals(PlanItem.SOURCE_SYNC, items[1].source)
    }

    @Test
    fun addItemPersistsAndUpdateMovesTheDay() {
        val file = tempFile()
        val repo = PlanRepository(file)
        val added = repo.addItem("2026-09-27", 8 * 60, 8 * 60 + 45, "自习", "")
        assertEquals(PlanItem.SOURCE_MANUAL, added.source)
        assertNull(added.anchor)
        val reloaded = PlanRepository(file)
        assertEquals("自习", reloaded.day("2026-09-27").items.single().title)
        assertEquals(PlanItem.SOURCE_MANUAL, reloaded.day("2026-09-27").items.single().source)
        val moved = reloaded.updateItem(added.id, "2026-09-29", 9 * 60, null, "自习", "换天")
        assertEquals(added.id, moved?.id)
        assertEquals(PlanItem.SOURCE_MANUAL, moved?.source)
        assertNull(moved?.anchor)
        assertTrue(reloaded.day("2026-09-27").items.isEmpty())
        assertEquals("换天", reloaded.day("2026-09-29").items.single().note)
        assertEquals("自习", PlanRepository(file).day("2026-09-29").items.single().title)
    }

    @Test
    fun insertItemPutsTheSameRowBack() {
        val file = tempFile()
        val repo = PlanRepository(file)
        repo.replaceDates(TextPlanParser.parse("#PLAN 2026-09-27 标题\n08:00-09:00 自习 | 笔记").items)
        val item = repo.day("2026-09-27").items.single()
        assertEquals("自习", repo.deleteItem(item.id)?.title)
        assertTrue(repo.day("2026-09-27").items.isEmpty())
        repo.insertItem(item, "标题")
        val restored = repo.day("2026-09-27")
        assertEquals("标题", restored.title)
        assertEquals(item.id, restored.items.single().id)
        assertEquals(item.source, restored.items.single().source)
        assertEquals(item.anchor, restored.items.single().anchor)
        assertEquals("笔记", restored.items.single().note)
        repo.insertItem(item, "别的标题")
        assertEquals(1, repo.day("2026-09-27").items.size)
        assertEquals("标题", repo.day("2026-09-27").title)
    }

    @Test
    fun jsonRoundTripKeepsNewlineInNote() {
        val parsed = MiniJson.parse(MiniJson.write(linkedMapOf("note" to "一\n二\"三"))) as Map<*, *>
        assertEquals("一\n二\"三", parsed["note"])
    }

    private fun tempFile(): File = File.createTempFile("plans", ".json").also { it.delete() }
}
