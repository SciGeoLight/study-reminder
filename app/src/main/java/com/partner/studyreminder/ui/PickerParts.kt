package com.partner.studyreminder.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.glass.StudyColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

internal val DurationChoices = intArrayOf(15, 25, 45, 60, 90)

/**
 * When [newStart] passes [end], keep the previous span (or 45 minutes).
 * An end that is still later stays where it is, including a next-day end.
 */
internal fun endWhenStartMoves(start: Int, end: Int?, newStart: Int): Int? {
    if (end == null) return null
    if (end > newStart) return end
    val span = (end - start).takeIf { it > 0 } ?: 45
    return newStart + span
}

@Composable
internal fun GlassSection(backdrop: Backdrop, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(backdrop, squircle(12.dp), studyColors().glass, blurRadius = 2.dp, refraction = 20.dp),
    ) { content() }
}

@Composable
internal fun Hairline() {
    Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(studyColors().separator))
}

@Composable
internal fun BorderlessField(
    value: String,
    placeholder: String,
    colors: StudyColors,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = colors.label, fontSize = 17.sp),
        cursorBrush = SolidColor(colors.blue),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = colors.tertiary, fontSize = 17.sp)
                inner()
            }
        },
    )
}

internal fun dateLabel(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

@Composable
internal fun ValueRow(label: String, value: String, open: Boolean, tag: String, onClick: () -> Unit) {
    val colors = studyColors()
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = if (open) Color.White else colors.label,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(com.kyant.shapes.Capsule())
                .background(if (open) colors.blue else colors.track)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun DateRow(label: String, date: LocalDate, open: Boolean, tag: String, onClick: () -> Unit) {
    ValueRow(label, dateLabel(date), open, tag, onClick)
}

@Composable
internal fun InlineCalendar(selected: LocalDate, tag: String, onPick: (LocalDate) -> Unit) {
    val colors = studyColors()
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }
    val weeks = listOf("一", "二", "三", "四", "五", "六", "日")
    val today = PlanTime.today()
    Column(Modifier.testTag(tag).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${month.year}年${month.monthValue}月",
                color = colors.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Text(
                "‹",
                color = colors.blue,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null) { month = month.minusMonths(1) }
                    .padding(8.dp),
            )
            Text(
                "›",
                color = colors.blue,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null) { month = month.plusMonths(1) }
                    .padding(8.dp),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            weeks.forEach { day ->
                Text(
                    day,
                    color = colors.tertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        val lead = month.atDay(1).dayOfWeek.value - 1
        val count = month.lengthOfMonth()
        val rows = (lead + count + 6) / 7
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val day = row * 7 + column - lead + 1
                    Box(Modifier.weight(1f).height(36.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..count) {
                            val cell = month.atDay(day)
                            val on = cell == selected
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (on) colors.blue else Color.Transparent)
                                    .clickable(interactionSource = null, indication = null) { onPick(cell) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    color = when {
                                        on -> Color.White
                                        cell == today -> colors.blue
                                        else -> colors.label
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = if (on || cell == today) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimeWheels(minutes: Int, onChange: (Int) -> Unit) {
    val colors = studyColors()
    val clock = Math.floorMod(minutes, 24 * 60)
    val hour = clock / 60
    val minute = clock % 60
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("小时", color = colors.secondary, fontSize = 13.sp)
            LoopWheel(value = hour, count = 24) { onChange(it * 60 + minute) }
        }
        Text(":", color = colors.label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("分钟", color = colors.secondary, fontSize = 13.sp)
            LoopWheel(value = minute, count = 60) { onChange(hour * 60 + it) }
        }
    }
}

private const val WheelLoops = 80
private val WheelItemHeight = 36.dp

@Composable
private fun LoopWheel(value: Int, count: Int, onSnap: (Int) -> Unit) {
    val colors = studyColors()
    val view = LocalView.current
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(listState, SnapPosition.Center)
    val total = WheelLoops * count
    val onSnapState = rememberUpdatedState(onSnap)
    var ready by remember { mutableStateOf(false) }
    var programmatic by remember { mutableIntStateOf(0) }
    val wanted = Math.floorMod(value, count)

    suspend fun centerOn(index: Int) {
        programmatic++
        try {
            listState.scrollToItem(index.coerceIn(0, total - 1))
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val delta = (item.offset + item.size / 2) - viewportCenter
            if (delta != 0) listState.scroll { scrollBy(delta.toFloat()) }
            kotlinx.coroutines.yield()
        } finally {
            programmatic--
        }
    }

    LaunchedEffect(wanted) {
        val shown = listState.centeredIndex()?.let { Math.floorMod(it, count) }
        if (!ready || shown != wanted) {
            centerOn((WheelLoops / 2) * count + wanted)
        }
        ready = true
    }
    LaunchedEffect(listState) {
        var moved = false
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!ready) return@collect
            if (scrolling) {
                if (programmatic == 0) moved = true
            } else if (moved && programmatic == 0) {
                moved = false
                val index = listState.centeredIndex() ?: return@collect
                val picked = Math.floorMod(index, count)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onSnapState.value(picked)
                if (index < count || index >= total - count) {
                    centerOn((WheelLoops / 2) * count + picked)
                }
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(WheelItemHeight * 5)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.22f to Color.Black,
                        0.78f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .height(WheelItemHeight)
                .clip(squircle(10.dp))
                .background(colors.track),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(total) { index ->
                WheelDigit(number = Math.floorMod(index, count), index = index, state = listState)
            }
        }
    }
}

@Composable
private fun WheelDigit(number: Int, index: Int, state: LazyListState) {
    val colors = studyColors()
    val emphasis by remember(index) {
        derivedStateOf {
            val info = state.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@derivedStateOf 0.35f
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val itemCenter = item.offset + item.size / 2f
            val distance = abs(itemCenter - viewportCenter) / item.size.coerceAtLeast(1)
            (1f - distance * 0.28f).coerceIn(0.35f, 1f)
        }
    }
    val selected = emphasis > 0.92f
    Box(Modifier.fillMaxWidth().height(WheelItemHeight), contentAlignment = Alignment.Center) {
        Text(
            text = "%02d".format(number),
            color = if (selected) colors.label else colors.tertiary,
            fontSize = if (selected) 22.sp else 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.graphicsLayer { alpha = emphasis },
        )
    }
}

private fun LazyListState.centeredIndex(): Int? {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return visible.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index
}
