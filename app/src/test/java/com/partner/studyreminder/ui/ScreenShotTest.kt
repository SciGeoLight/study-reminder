package com.partner.studyreminder.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.SegmentedIndicatorIndexKey
import com.partner.studyreminder.ui.glass.SegmentedIndicatorOffsetPxKey
import com.partner.studyreminder.ui.glass.SegmentedTabWidthPxKey
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the 1.5.0 glass screens without an emulator.
 * Frames land in /opt/cursor/artifacts when that directory exists, and also under build/shots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenShotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test(timeout = 240_000)
    fun mainAndTodosLightAndDark() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        seed(context)
        Prefs.setSawPermissions(context, true)
        capture("main", "light", Prefs.THEME_LIGHT, MainActivity::class.java)
        capture("main", "dark", Prefs.THEME_DARK, MainActivity::class.java)
        captureTodos("light", Prefs.THEME_LIGHT)
        captureTodos("dark", Prefs.THEME_DARK)
    }

    @Test(timeout = 180_000)
    fun clickingATabSlidesTheIndicatorOntoThatCell() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        seed(context)
        Prefs.setSawPermissions(context, true)
        Prefs.setTheme(context, Prefs.THEME_LIGHT)
        com.partner.studyreminder.ui.theme.ThemeMode.notifyChanged()

        val todos = ActivityScenario.launch(TodosActivity::class.java)
        compose.waitForIdle()
        assertIndicatorAligned(0)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("filter-阅读").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(48)
        val moving = indicatorIndex()
        assertTrue("点选后指示器应在移向第 2 格的途中，实际 $moving", moving > 0.02f && moving < 0.98f)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertIndicatorAligned(1)
        compose.onNodeWithTag("filter-生活").performClick()
        compose.waitForIdle()
        assertIndicatorAligned(2)
        compose.onNodeWithTag("filter-全部").performClick()
        compose.waitForIdle()
        assertIndicatorAligned(0)
        compose.onAllNodesWithText("导入").assertCountEquals(0)
        todos.close()

        val main = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        assertIndicatorAligned(0)
        compose.onAllNodesWithText("全部").assertCountEquals(0)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("plan-tab-更多").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(48)
        val planMoving = indicatorIndex()
        assertTrue("计划页点「更多」后指示器应在移向第 3 格的途中，实际 $planMoving", planMoving > 0.05f && planMoving < 1.9f)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertIndicatorAligned(2)
        compose.onNodeWithText("打开文件").assertIsDisplayed()
        main.close()
    }

    private fun seed(context: android.content.Context) {
        val today = PlanTime.today()
        val plans = Plans.of(context)
        if (plans.day(today.toString()).items.isEmpty()) {
            plans.addItem(today.toString(), 8 * 60, 9 * 60 + 20, "阅读：第一段", "读完这一段")
            plans.addItem(today.toString(), 9 * 60 + 30, 10 * 60 + 50, "作业", "写完练习")
            plans.addItem(today.toString(), 11 * 60, 12 * 60, "散步", "")
            plans.addItem(today.toString(), 14 * 60, 15 * 60 + 30, "作业：书面练习", "写在纸上")
            plans.addItem(today.toString(), 19 * 60, 20 * 60 + 30, "阅读：再看一遍", "标出未读")
        }
        val todos = Todos.of(context)
        val cover = File(context.filesDir, "todo-images/read")
        cover.mkdirs()
        val pixels = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        pixels.eraseColor(android.graphics.Color.rgb(0, 122, 255))
        FileOutputStream(File(cover, "cover.jpg")).use { pixels.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        pixels.recycle()
        if (todos.all().isEmpty()) {
            val research = todos.addGroup("阅读")!!
            val life = todos.addGroup("生活")!!
            todos.addGroup("作业")
            todos.addGroup("家务")
            val yesterday = today.minusDays(1).toString()
            val weekAgo = today.minusDays(7).toString()
            val soon = today.plusDays(4).toString()
            todos.add("交作业", "截止昨天", weekAgo, yesterday, null, null, groupId = research.id, id = "lab")
            todos.add(
                "阅读一章",
                "带着问题",
                today.minusDays(6).toString(),
                today.toString(),
                today.toString(),
                9 * 60,
                groupId = research.id,
                images = listOf("cover.jpg"),
                id = "read",
            )
            todos.add("去散步", "", soon, soon, null, null, groupId = life.id, id = "review")
            val notes = todos.add("整理笔记", "", today.minusDays(2).toString(), today.minusDays(2).toString(), null, null)
            todos.setDone(notes.id, true)
        }
    }

    private fun captureTodos(theme: String, themePref: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Prefs.setTheme(context, themePref)
        com.partner.studyreminder.ui.theme.ThemeMode.notifyChanged()
        val scenario = ActivityScenario.launch(TodosActivity::class.java)
        compose.waitForIdle()
        compose.onNodeWithText("未完成").assertIsDisplayed()
        compose.onNodeWithText("今天到期").assertIsDisplayed()
        compose.onAllNodesWithText("已逾期").assertCountEquals(2)
        compose.onNodeWithTag("tile-今天").assertIsDisplayed()
        compose.onNodeWithTag("tile-已逾期").assertIsDisplayed()
        compose.onNodeWithTag("tile-全部").assertIsDisplayed()
        compose.onNodeWithTag("tile-已完成").assertIsDisplayed()
        compose.onNodeWithText("阅读一章").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("带着问题").assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-todos-$theme.png")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("segmented-filters").performTouchInput {
            down(center)
            moveBy(Offset(220f, 0f))
        }
        compose.mainClock.advanceTimeByFrame()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-segment-drag-$theme.png")
        compose.onNodeWithTag("segmented-filters").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("filter-阅读").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("filter-阅读").assertIsDisplayed()
        assertIndicatorAligned(1)
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-segment-settled-$theme.png")
        compose.onNodeWithTag("filter-more").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("更多分组").assertIsDisplayed()
        compose.onNodeWithText("未分组").assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-segment-more-$theme.png")
        compose.onNodeWithText("取消").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("filter-全部").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("add-todo").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("添加待办").assertIsDisplayed()
        compose.onNodeWithText("添加图片").assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-todos-add-$theme.png")
        compose.onNodeWithContentDescription("关闭").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("阅读一章").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("完成").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭").assertIsDisplayed()
        compose.onNodeWithText("添加图片").assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-todos-edit-large-$theme.png")
        compose.onNodeWithTag("date-start").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("inline-calendar").assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-todos-calendar-$theme.png")
        compose.onNodeWithTag("date-start").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("group-row").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("group-picker").performScrollTo().assertIsDisplayed()
        writePng(compose.onRoot().captureToImage().asAndroidBitmap(), "学习提醒-v1.7.8-todos-groups-$theme.png")
        scenario.close()
    }

    @Test(timeout = 120_000)
    fun settingsAndPermissionsShowTheirTitles() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Prefs.setSawPermissions(context, true)
        Prefs.setTheme(context, Prefs.THEME_LIGHT)
        com.partner.studyreminder.ui.theme.ThemeMode.notifyChanged()
        // Robolectric does not run WorkManagerInitializer. Closing settings saves and schedules sync.
        try {
            WorkManager.getInstance(context)
        } catch (_: IllegalStateException) {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        val settings = ActivityScenario.launch(SettingsActivity::class.java)
        compose.waitForIdle()
        compose.onNodeWithText("设置").assertIsDisplayed()
        compose.onNodeWithText("外观").assertIsDisplayed()
        compose.onNodeWithText("提前提醒").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("保存").assertCountEquals(0)
        settings.close()
        val permissions = ActivityScenario.launch(PermissionActivity::class.java)
        compose.waitForIdle()
        compose.onNodeWithText("权限").assertIsDisplayed()
        compose.onNodeWithText("通知").assertIsDisplayed()
        compose.onNodeWithText("我已允许").performScrollTo().assertIsDisplayed()
        permissions.close()
    }

    private fun indicatorIndex(): Float {
        return compose.onNodeWithTag("segmented-indicator").fetchSemanticsNode().config[SegmentedIndicatorIndexKey]
    }

    private fun assertIndicatorAligned(index: Int) {
        val config = compose.onNodeWithTag("segmented-indicator").fetchSemanticsNode().config
        val value = config[SegmentedIndicatorIndexKey]
        val offset = config[SegmentedIndicatorOffsetPxKey]
        val width = config[SegmentedTabWidthPxKey]
        assertEquals(index.toFloat(), value, 0.05f)
        assertTrue(width > 1f)
        assertEquals(index * width, offset, 1.5f)
        assertTrue(abs(offset - index * width) < 1.5f)
    }

    private fun capture(name: String, theme: String, themePref: String, activity: Class<out android.app.Activity>) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Prefs.setTheme(context, themePref)
        com.partner.studyreminder.ui.theme.ThemeMode.notifyChanged()
        val scenario = ActivityScenario.launch(activity)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        writePng(bitmap, "学习提醒-v1.7.8-$name-$theme.png")
        scenario.close()
    }

    private fun writePng(bitmap: Bitmap, name: String) {
        val dirs = listOf(
            File("/opt/cursor/artifacts"),
            File("build/shots"),
        )
        for (dir in dirs) {
            if (!dir.exists() && !dir.mkdirs()) continue
            FileOutputStream(File(dir, name)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
}
