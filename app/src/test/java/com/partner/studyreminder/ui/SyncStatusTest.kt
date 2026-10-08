package com.partner.studyreminder.ui

import com.partner.studyreminder.data.Prefs
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStatusTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun successCopyReadsCountsAndDropsTheOverflowClause() {
        val copy = syncStatusCopy(
            message = "已同步 4 条（2026-10-08）。待办 2 条（新增 1，更新 1）。较远的提醒会临近时再排上",
            successAtMillis = 1_759_900_000_000L,
            scheduledCount = 12,
            zone = zone,
        )
        assertEquals("6 条", copy.recognized)
        assertEquals("12/80", copy.scheduled)
        assertEquals("临近时再排上", copy.overflow)
        assertFalse(copy.result.contains("临近时再排上"))
        assertTrue(copy.result.contains("已同步 4 条"))
        assertNull(copy.suggestion)
        assertFalse(copy.lastSuccess == "还没有成功同步")
    }

    @Test
    fun emptyRecognitionIsZero() {
        val copy = syncStatusCopy(
            message = "同步完成，但没有识别到安排",
            successAtMillis = 0L,
            scheduledCount = 0,
        )
        assertEquals("0 条", copy.recognized)
        assertEquals("还没有成功同步", copy.lastSuccess)
        assertNull(copy.overflow)
    }

    @Test
    fun failureShowsACategoryAndHidesTheException() {
        val secret = "HTTP 500 token=abc.example"
        val copy = syncStatusCopy(
            message = "同步失败：$secret",
            successAtMillis = 0L,
            scheduledCount = 3,
        )
        val shown = listOf(copy.lastSuccess, copy.result, copy.suggestion, copy.recognized, copy.scheduled, copy.overflow)
            .joinToString(" ")
        assertEquals("网址没有正常返回", copy.result)
        assertEquals("检查网址是否还能打开。", copy.suggestion)
        assertEquals("—", copy.recognized)
        assertFalse(shown.contains("token"))
        assertFalse(shown.contains("500"))
        assertFalse(shown.contains(secret))
    }

    @Test
    fun fullAlarmWindowExplainsTheCap() {
        val copy = syncStatusCopy(
            message = "已同步 1 条（2026-10-08）",
            successAtMillis = 0L,
            scheduledCount = 80,
        )
        assertEquals("临近时再排上", copy.overflow)
        assertEquals("1 条", copy.recognized)
    }

    @Test
    fun successClockIgnoresFailures() {
        assertTrue(Prefs.syncMessageRecordsSuccess("已同步 1 条（2026-10-08）"))
        assertTrue(Prefs.syncMessageRecordsSuccess("待办 2 条（新增 2，更新 0）"))
        assertTrue(Prefs.syncMessageRecordsSuccess("同步完成，但没有识别到安排"))
        assertFalse(Prefs.syncMessageRecordsSuccess("同步失败：boom"))
        assertFalse(Prefs.syncMessageRecordsSuccess("没有填写网址，不会自动同步"))
        assertFalse(Prefs.syncMessageRecordsSuccess("网址要以 http:// 或 https:// 开头"))
    }
}
