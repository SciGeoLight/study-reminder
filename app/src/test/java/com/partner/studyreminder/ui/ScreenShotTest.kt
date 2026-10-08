package com.partner.studyreminder.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanTime
import java.io.File
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
