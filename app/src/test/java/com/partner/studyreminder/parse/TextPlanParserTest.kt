package com.partner.studyreminder.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPlanParserTest {
    @Test
    fun parsesTheChatFormat() {
        val text = """
            #PLAN 2026-09-27 周日安排
            08:00-08:20 晨间阅读 | 读完一节就停
            08:20-08:40 作业：书面练习 | 写完练习就停
            21:50-22:05 收尾
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertTrue(result.warnings.isEmpty())
        assertEquals(3, result.items.size)
        val first = result.items[0]
        assertEquals("2026-09-27", first.date)
        assertEquals(8 * 60, first.startMinutes)
        assertEquals(8 * 60 + 20, first.endMinutes)
        assertEquals("晨间阅读", first.title)
        assertEquals("读完一节就停", first.note)
        assertEquals("周日安排", first.planTitle)
        assertEquals("作业：书面练习", result.items[1].title)
        assertEquals("写完练习就停", result.items[1].note)
        assertEquals("收尾", result.items[2].title)
        assertEquals("", result.items[2].note)
        assertEquals(21 * 60 + 50, result.items[2].startMinutes)
        assertEquals(22 * 60 + 5, result.items[2].endMinutes)
    }

    @Test
    fun toleratesFullWidthPunctuationSpacesAndOptionalEnd() {
        val text = """
            #PLAN 2026-09-28　测试
            08：00－08：20　阅读｜备注
            ０９：００ 只写开始
            - 10:00~10:30 带项目符号 | 说明
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertEquals(emptyList<String>(), result.warnings)
        assertEquals(3, result.items.size)
        assertEquals(8 * 60, result.items[0].startMinutes)
        assertEquals(8 * 60 + 20, result.items[0].endMinutes)
        assertEquals("阅读", result.items[0].title)
        assertEquals("备注", result.items[0].note)
        assertEquals(9 * 60, result.items[1].startMinutes)
        assertNull(result.items[1].endMinutes)
        assertEquals("只写开始", result.items[1].title)
        assertEquals("带项目符号", result.items[2].title)
        assertEquals("说明", result.items[2].note)
    }

    @Test
    fun multiplePlanBlocksAndBlankLines() {
        val text = """
            #PLAN 2026-09-27 上午

            08:00-08:20 阅读

            #PLAN 2026-09-28 第二天
            14:00 作业 | 写完练习
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertTrue(result.warnings.isEmpty())
        assertEquals(listOf("2026-09-27", "2026-09-28"), result.items.map { it.date })
        assertEquals("上午", result.items[0].planTitle)
        assertEquals("第二天", result.items[1].planTitle)
    }

    @Test
    fun overnightEndRollsToNextDayMinutes() {
        val result = TextPlanParser.parse("#PLAN 2026-09-27 夜\n23:50-00:10 夜读")
        assertEquals(23 * 60 + 50, result.items.single().startMinutes)
        assertEquals(24 * 60 + 10, result.items.single().endMinutes)
    }

    @Test
    fun warnsOnGarbageAndMissingDate() {
        val result = TextPlanParser.parse("随便写一行\n08:00 还没写日期\n#PLAN 2026-13-40 坏\n#PLAN 2026-09-27 好\n不是时间")
        assertTrue(result.items.isEmpty())
        assertTrue(result.warnings.size >= 4)
    }

    @Test
    fun todoBlockLivesBesidePlanAndKeepsIds() {
        val text = """
            #PLAN 2026-10-01 周三
            08:00-08:40 阅读 | 一章
            #TODO
            read | 阅读一章 | 2026-10-01 | 2026-10-07 | 2026-10-03 09:00 | 带着问题
            walk | 去散步 | 2026-10-08
            #PLAN 2026-10-02 周四
            09:00 作业
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertTrue(result.hasTodoSection)
        assertEquals(listOf("阅读", "作业"), result.items.map { it.title })
        assertEquals(2, result.todos.size)
        val read = result.todos[0]
        assertEquals("read", read.id)
        assertEquals("2026-10-01", read.startDate)
        assertEquals("2026-10-07", read.endDate)
        assertEquals("2026-10-03", read.remindDate)
        assertEquals(9 * 60, read.remindMinutes)
        assertEquals("带着问题", read.note)
        assertNull(read.groupName)
        val lab = result.todos[1]
        assertEquals("walk", lab.id)
        assertEquals("2026-10-08", lab.startDate)
        assertEquals("2026-10-08", lab.endDate)
        assertNull(lab.remindDate)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun todoGroupFieldIsOptionalAndStaysOutOfTheNote() {
        val text = """
            #TODO
            read | 阅读一章 | 2026-10-01 | 2026-10-07 | 2026-10-03 09:00 | 带着问题 | 组:阅读
            walk | 去散步 | 2026-10-08 | 组：生活
            hand | 交作业 | 2026-10-09
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertTrue(result.warnings.isEmpty())
        assertEquals("阅读", result.todos[0].groupName)
        assertEquals("带着问题", result.todos[0].note)
        assertEquals("生活", result.todos[1].groupName)
        assertEquals("", result.todos[1].note)
        assertNull(result.todos[2].groupName)
    }

    @Test
    fun ignoresCodeFences() {
        val text = """
            ```
            #PLAN 2026-09-27 计划
            08:00 阅读
            ```
        """.trimIndent()
        val result = TextPlanParser.parse(text)
        assertEquals(1, result.items.size)
        assertEquals("阅读", result.items[0].title)
    }
}
