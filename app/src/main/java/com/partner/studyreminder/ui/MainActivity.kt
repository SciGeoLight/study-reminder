package com.partner.studyreminder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.partner.studyreminder.alarm.AlarmContract
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.parse.PlanParsers
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private var viewing by mutableStateOf(PlanTime.today())
    private var pinnedId by mutableStateOf<String?>(null)
    private var pendingText: String? = null
    private var tick by mutableIntStateOf(0)

    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        openPreviewFrom { TextIntents.read(this, uri) }
    }

    private val preview = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        result.data?.getStringExtra(AlarmContract.EXTRA_DATE)?.let { raw ->
            runCatching { viewing = LocalDate.parse(raw) }
        }
        tick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge()
        if (savedInstanceState != null) {
            savedInstanceState.getString(STATE_DATE)?.let { runCatching { viewing = LocalDate.parse(it) } }
            pendingText = savedInstanceState.getString(STATE_PENDING)
        } else {
            applyOpenTarget(intent)
            pendingText = runCatching { TextIntents.fromIntent(this, intent) }.getOrNull()
        }
        setContent {
            StudyTheme {
                MainScreen(
                    viewing = viewing,
                    pinnedId = pinnedId,
                    refreshKey = tick,
                    onPrev = {
                        viewing = viewing.minusDays(1)
                    },
                    onNext = {
                        viewing = viewing.plusDays(1)
                    },
                    onToday = { viewing = PlanTime.today() },
                    onPermissions = { startActivity(Intent(this, PermissionActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onImportClipboard = { importClipboard() },
                    onOpenTodos = { startActivity(Intent(this, TodosActivity::class.java)) },
                    onOpenFile = {
                        openFile.launch(
                            arrayOf("text/calendar", "text/plain", "application/octet-stream", "application/ics"),
                        )
                    },
                    onTestAlarm = { testAlarm() },
                    onClearDay = { clearDay() },
                    onDelete = { deleteItem(it) },
                    onRestore = { item, title -> restoreItem(item, title) },
                    onAdd = { date, start, end, title, note -> addItem(date, start, end, title, note) },
                    onEdit = { item, date, start, end, title, note -> updateItem(item, date, start, end, title, note) },
                )
            }
        }
        if (!Prefs.sawPermissions(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyOpenTarget(intent)
        val text = runCatching { TextIntents.fromIntent(this, intent) }.getOrNull()
        if (!text.isNullOrBlank()) {
            pendingText = text
            if (Prefs.sawPermissions(this)) showPending()
        }
    }

    override fun onResume() {
        super.onResume()
        edgeToEdge()
        tick++
        if (Prefs.sawPermissions(this)) showPending()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DATE, viewing.toString())
        outState.putString(STATE_PENDING, pendingText)
    }

    private fun applyOpenTarget(intent: Intent) {
        intent.getStringExtra(AlarmContract.EXTRA_DATE)?.let { raw ->
            runCatching { viewing = LocalDate.parse(raw) }
        }
        pinnedId = intent.getStringExtra(AlarmContract.EXTRA_ID)?.takeIf { it.isNotBlank() }
    }

    private fun showPending() {
        val text = pendingText ?: return
        pendingText = null
        openPreview(text)
    }

    private fun importClipboard() {
        val text = readClipboard()
        if (text.isNullOrBlank()) {
            toast("剪贴板是空的。先在聊天里复制整段计划，再回到这里点导入。")
            return
        }
        openPreview(text)
    }

    private fun readClipboard(): String? {
        return try {
            val clipboard = getSystemService(ClipboardManager::class.java) ?: return null
            val clip: ClipData = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(this)?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun openPreviewFrom(block: () -> String) {
        val text = try {
            block()
        } catch (e: Exception) {
            toast("读文件失败：" + (e.message ?: "未知错误"))
            return
        }
        openPreview(text)
    }

    private fun openPreview(text: String) {
        if (text.length > 1_000_000) {
            toast("内容太长，没法导入。")
            return
        }
        val parsed = PlanParsers.parseAny(text)
        if (parsed.items.isEmpty() && parsed.todos.isEmpty()) {
            val hint = parsed.warnings.firstOrNull()?.let { " $it" }.orEmpty()
            toast("没有识别到计划。$hint")
            return
        }
        preview.launch(
            Intent(this, ImportPreviewActivity::class.java).putExtra(ImportPreviewActivity.EXTRA_RAW, text),
        )
    }

    private fun testAlarm() {
        if (!AlarmScheduler.canScheduleExact(this)) {
            toast("精确闹钟权限没开，测试可能不会响。先到菜单里的「权限检查」打开。")
        }
        AlarmScheduler.scheduleTest(this)
        toast("5 秒后响铃。可以锁屏试一次，锁屏时也应该弹出。")
    }

    private fun clearDay() {
        val removed = Plans.of(this).clearDay(viewing.toString())
        AlarmScheduler.cancelItems(this, removed)
        AlarmScheduler.rescheduleAll(this)
        tick++
        toast("已清空。")
    }

    private fun addItem(date: LocalDate, start: Int, end: Int?, title: String, note: String) {
        Plans.of(this).addItem(date.toString(), start, end, title, note)
        AlarmScheduler.rescheduleAll(this)
        viewing = date
        tick++
        toast("已添加")
    }

    private fun updateItem(
        item: PlanItem,
        date: LocalDate,
        start: Int,
        end: Int?,
        title: String,
        note: String,
    ) {
        val updated = Plans.of(this).updateItem(item.id, date.toString(), start, end, title, note)
        if (updated == null) {
            toast("没有找到这条计划。")
            return
        }
        AlarmScheduler.cancelItems(this, listOf(item))
        AlarmScheduler.rescheduleAll(this)
        viewing = date
        tick++
        toast("已修改")
    }

    private fun deleteItem(item: PlanItem) {
        val removed = Plans.of(this).deleteItem(item.id)
        if (removed != null) AlarmScheduler.cancelItems(this, listOf(removed))
        AlarmScheduler.rescheduleAll(this)
        tick++
    }

    private fun restoreItem(item: PlanItem, dayTitle: String) {
        Plans.of(this).insertItem(item, dayTitle)
        AlarmScheduler.rescheduleAll(this)
        tick++
    }

    companion object {
        private const val STATE_DATE = "viewing"
        private const val STATE_PENDING = "pending"

        fun zoneMatchesShanghai(): Boolean {
            val now = Instant.now()
            val system = ZoneId.systemDefault().rules.getOffset(now)
            val shanghai = PlanTime.ZONE.rules.getOffset(now)
            return system == shanghai
        }
    }
}
