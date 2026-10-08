package com.partner.studyreminder.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusParamsTest {
    @Test
    fun xiaomiFamilyOnly() {
        assertTrue(FocusParams.isXiaomi("Xiaomi", "Xiaomi"))
        assertTrue(FocusParams.isXiaomi("Xiaomi", "Redmi"))
        assertTrue(FocusParams.isXiaomi("Xiaomi", "POCO"))
        assertTrue(FocusParams.isXiaomi("redmi", "Redmi"))
        assertFalse(FocusParams.isXiaomi("Google", "google"))
        assertFalse(FocusParams.isXiaomi("samsung", "samsung"))
    }

    @Test
    fun jsonUsesParamV2FocusFields() {
        val json = FocusParams.json(
            title = "08:40 开始 · 阅读：第一段",
            content = "读完这一段",
            ticker = "阅读",
            aodTitle = "08:40 阅读",
            hintTitle = "08:40–09:30",
            remainingMinutes = 18,
            progressPercent = 10,
            timeoutMinutes = 18,
            floatOnPost = true,
        )
        assertTrue(json.contains("\"param_v2\""))
        assertTrue(json.contains("\"baseInfo\""))
        assertTrue(json.contains("\"hintInfo\""))
        assertTrue(json.contains("\"ticker\":\"阅读\""))
        assertTrue(json.contains("\"progress\":10"))
        assertTrue(json.contains("\"progressInfo\":{\"progress\":10}"))
        assertTrue(json.contains("\"param_island\""))
        assertTrue(json.contains("\"smallIslandArea\""))
        assertTrue(json.contains(FocusParams.PIC_ICON))
        assertTrue(json.contains("08:40 开始 · 阅读：第一段"))
        assertFalse(json.contains("chatInfo"))
    }

    @Test
    fun quotesInTitlesAreEscaped() {
        val json = FocusParams.json(
            title = "他说\"开始\"",
            content = "a\\b",
            ticker = "t",
            aodTitle = "a",
            hintTitle = "h",
            remainingMinutes = 1,
            progressPercent = 0,
            timeoutMinutes = 1,
            floatOnPost = false,
        )
        assertTrue(json.contains("他说\\\"开始\\\""))
        assertTrue(json.contains("a\\\\b"))
        assertTrue(json.contains("\"enableFloat\":false"))
    }
}
