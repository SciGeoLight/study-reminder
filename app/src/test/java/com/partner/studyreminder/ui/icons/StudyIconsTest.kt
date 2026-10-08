package com.partner.studyreminder.ui.icons

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyIconsTest {
    @Test
    fun everyUsedSymbolIsA24DpGlyph() {
        val icons = listOf(
            StudyIcons.Add,
            StudyIcons.Alarm,
            StudyIcons.Check,
            StudyIcons.Checklist,
            StudyIcons.ChevronLeft,
            StudyIcons.ChevronRight,
            StudyIcons.Close,
            StudyIcons.ContentPaste,
            StudyIcons.Delete,
            StudyIcons.FolderOpen,
            StudyIcons.KeyboardArrowDown,
            StudyIcons.KeyboardArrowUp,
            StudyIcons.MoreHoriz,
            StudyIcons.Notifications,
            StudyIcons.PhotoCamera,
            StudyIcons.PhotoLibrary,
            StudyIcons.Settings,
        )
        assertEquals(17, icons.size)
        icons.forEach { icon ->
            assertEquals(24.dp, icon.defaultWidth)
            assertEquals(24.dp, icon.defaultHeight)
            assertEquals(24f, icon.viewportWidth, 0.001f)
            assertEquals(24f, icon.viewportHeight, 0.001f)
            assertTrue(icon.root.size > 0)
        }
    }
}
