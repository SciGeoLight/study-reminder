package com.partner.studyreminder.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsParserTest {
    @Test
    fun sampleFileIsFifteenBeijingItems() {
        val text = javaClass.classLoader!!.getResourceAsStream("sample-20260927.ics")!!
            .bufferedReader(Charsets.UTF_8)
            .readText()
        val result = PlanParsers.parseAny(text)
        assertTrue(result.warnings.isEmpty())
        assertEquals(15, result.items.size)
        assertTrue(result.items.all { it.date == "2026-09-27" })
        assertTrue(result.items.all { it.planTitle == "周日安排" })
        assertTrue(result.items.all { !it.allDay })

        val starts = listOf(
            8 * 60, 8 * 60 + 20, 8 * 60 + 40, 9 * 60 + 40, 10 * 60 + 40,
            11 * 60 + 40, 13 * 60 + 30, 14 * 60 + 30, 15 * 60 + 30, 16 * 60 + 30,
            17 * 60 + 30, 19 * 60 + 30, 20 * 60 + 40, 21 * 60 + 20, 21 * 60 + 50,
        )
        val ends = listOf(
            8 * 60 + 20, 8 * 60 + 40, 9 * 60 + 30, 10 * 60 + 30, 11 * 60 + 40,
            13 * 60 + 30, 14 * 60 + 20, 15 * 60 + 20, 16 * 60 + 30, 17 * 60 + 30,
            19 * 60 + 30, 20 * 60 + 30, 21 * 60 + 20, 21 * 60 + 50, 22 * 60 + 5,
        )
        val titles = listOf(
            "晨间阅读",
            "阅读：标出进度",
            "阅读：第一段",
            "阅读：第二段",
            "作业：书面练习",
            "午饭和休息",
            "作业：整理要点",
            "作业：核对步骤",
            "作业：再做几题",
            "散步",
            "晚饭",
            "阅读：再看一遍",
            "作业：合上书复述",
            "阅读或散步",
            "收尾",
        )
        assertEquals(starts, result.items.map { it.startMinutes })
        assertEquals(ends, result.items.map { it.endMinutes })
        assertEquals(titles, result.items.map { it.title })
        assertEquals("读完一节就停", result.items.first().note)
        assertEquals("写下明天要做的三件事，然后休息", result.items.last().note)
        assertEquals("标出已读和未读", result.items[1].note)
    }

    @Test
    fun utcTzidFloatingFoldedLinesAndValarm() {
        val text = """
            BEGIN:VCALENDAR
            X-WR-CALNAME:混合
            BEGIN:VTIMEZONE
            TZID:Asia/Shanghai
            BEGIN:STANDARD
            DTSTART:19700101T000000
            TZOFFSETFROM:+0800
            TZOFFSETTO:+0800
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VEVENT
            DTSTART:20260927T160000Z
            SUMMARY:跨日
            DESCRIPTION:这是一段很长的备注
             继续的内容
            BEGIN:VALARM
            ACTION:DISPLAY
            DESCRIPTION:不要用这段当备注
            TRIGGER:PT0M
            END:VALARM
            END:VEVENT
            BEGIN:VEVENT
            DTSTART;TZID=Asia/Shanghai:20260928T213000
            DTEND;TZID="Asia/Shanghai":20260928T220000
            SUMMARY:上海时区
            DESCRIPTION:第一行\n第二行\,测试
            END:VEVENT
            BEGIN:VEVENT
            DTSTART:20260928T093000
            DURATION:PT45M
            SUMMARY:浮动本地
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val result = IcsParser.parse(text)
        assertEquals(3, result.items.size)

        val utc = result.items[0]
        assertEquals("2026-09-28", utc.date)
        assertEquals(0, utc.startMinutes)
        assertNull(utc.endMinutes)
        assertEquals("跨日", utc.title)
        assertEquals("这是一段很长的备注继续的内容", utc.note)
        assertFalse(utc.note.contains("不要用这段"))
        assertEquals("混合", utc.planTitle)

        val tz = result.items[1]
        assertEquals("2026-09-28", tz.date)
        assertEquals(21 * 60 + 30, tz.startMinutes)
        assertEquals(22 * 60, tz.endMinutes)
        assertEquals("第一行\n第二行,测试", tz.note)

        val floating = result.items[2]
        assertEquals("2026-09-28", floating.date)
        assertEquals(9 * 60 + 30, floating.startMinutes)
        assertEquals(9 * 60 + 30 + 45, floating.endMinutes)
        assertEquals("浮动本地", floating.title)
    }
}
