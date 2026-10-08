package com.partner.studyreminder.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.alarm.AlarmContract
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.ParseResult
import com.partner.studyreminder.parse.ParsedItem
import com.partner.studyreminder.parse.PlanParsers
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassTier
import com.partner.studyreminder.ui.glass.LiquidButton
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.glass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast

class ImportPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge()
        val raw = intent.getStringExtra(EXTRA_RAW)
        if (raw.isNullOrBlank()) {
            finish()
            return
        }
        val parsed = PlanParsers.parseAny(raw)
        if (parsed.items.isEmpty() && parsed.todos.isEmpty()) {
            toast("没有识别到计划")
            finish()
            return
        }
        setContent {
            StudyTheme {
                ImportScreen(
                    parsed = parsed,
                    summary = summary(parsed),
                    onCancel = { finish() },
                    onConfirm = { confirm(parsed) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        edgeToEdge()
    }

    private fun summary(parsed: ParseResult): String {
        val counts = Plans.of(this).countsByDate()
        val lines = mutableListOf<String>()
        if (parsed.items.isNotEmpty()) {
            lines += "共 ${parsed.items.size} 条。再次导入只替换那一天从导入来的安排，手动添加的会留下来。"
        }
        if (parsed.todos.isNotEmpty()) {
            lines += "待办 ${parsed.todos.size} 条。同一个编号会更新内容，已勾完成的会保留。"
        }
        for ((date, items) in parsed.items.groupBy { it.date }) {
            val title = items.lastOrNull { it.planTitle.isNotBlank() }?.planTitle.orEmpty()
            val heading = if (title.isBlank()) date else "$date · $title"
            val old = Plans.of(this).replaceableCount(date, PlanItem.SOURCE_IMPORT)
            val kept = (counts[date] ?: 0) - old
            val replace = if (old > 0) "，将替换这一天已导入的 $old 条" else ""
            val stay = if (kept > 0) "，保留手动或同步的 $kept 条" else ""
            lines += "$heading：${items.size} 条$replace$stay"
        }
        return lines.joinToString("\n")
    }

    private fun confirm(parsed: ParseResult) {
        if (parsed.items.isNotEmpty()) {
            val removed = Plans.of(this).replaceDates(parsed.items, PlanItem.SOURCE_IMPORT)
            AlarmScheduler.cancelItems(this, removed)
        }
        if (parsed.hasTodoSection) Todos.of(this).mergeSync(parsed.todos)
        AlarmScheduler.rescheduleAll(this)
        val first = parsed.items.minOfOrNull { it.date }
        val note = buildString {
            if (parsed.items.isNotEmpty()) append("已导入 ${parsed.items.size} 条")
            if (parsed.todos.isNotEmpty()) {
                if (isNotEmpty()) append("，")
                append("待办 ${parsed.todos.size} 条")
            }
        }
        toast(note)
        if (first != null) setResult(RESULT_OK, Intent().putExtra(AlarmContract.EXTRA_DATE, first))
        else setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_RAW = "raw"
    }
}

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun ImportScreen(
    parsed: ParseResult,
    summary: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = studyColors()
    val items = parsed.items.sortedWith(compareBy({ it.date }, { it.startMinutes }))
    LiquidPage { backdrop ->
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                GlassIconButton(onCancel, backdrop) {
                    Icon(StudyIcons.ChevronLeft, contentDescription = "返回", tint = colors.label)
                }
                Spacer(Modifier.height(12.dp))
                Text("确认导入", color = colors.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = summary,
                    color = colors.label,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(backdrop, GlassTier.Card, squircle(22.dp))
                        .padding(16.dp),
                )
                if (parsed.warnings.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val shown = parsed.warnings.take(6)
                    val extra = parsed.warnings.size - shown.size
                    val tail = if (extra > 0) "\n另有 $extra 行没认出来。" else ""
                    Text(
                        text = shown.joinToString("\n") + tail,
                        color = colors.orange,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(backdrop, GlassTier.Card, squircle(22.dp))
                            .padding(14.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(backdrop, GlassTier.Card, squircle(22.dp)),
                ) {
                    var lastDate = ""
                    items.forEachIndexed { index, item ->
                        if (item.date != lastDate) {
                            lastDate = item.date
                            Text(
                                text = item.date,
                                color = colors.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                            )
                        }
                        ImportRow(item)
                        if (index != items.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                                    .height(0.5.dp)
                                    .background(colors.separator),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                LiquidButton(
                    onClick = onConfirm,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    tint = colors.blue,
                    height = 56.dp,
                ) {
                    Text("确认导入", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "取消",
                    color = colors.blue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = null, indication = null, onClick = onCancel)
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ImportRow(item: ParsedItem) {
    val colors = studyColors()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = PlanTime.formatMinutes(item.startMinutes),
            color = colors.blue,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            style = Tabular,
            modifier = Modifier.width(58.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(item.title, color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            if (item.note.isNotBlank()) {
                Text(item.note, color = colors.secondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
