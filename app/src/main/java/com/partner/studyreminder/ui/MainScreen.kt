package com.partner.studyreminder.ui

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.R
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassOverlay
import com.partner.studyreminder.ui.glass.LiquidButton
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.liquidPressFeedback
import com.partner.studyreminder.ui.glass.rememberLiquidPress
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.StudyColors
import com.partner.studyreminder.ui.glass.LocalBackdropImage
import com.partner.studyreminder.ui.glass.drawStudyBackdrop
import com.partner.studyreminder.ui.glass.studyColors
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun MainScreen(
    viewing: LocalDate,
    pinnedId: String? = null,
    refreshKey: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPermissions: () -> Unit,
    onSettings: () -> Unit,
    onImportClipboard: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenFile: () -> Unit,
    onTestAlarm: () -> Unit,
    onClearDay: () -> Unit,
    onDelete: (PlanItem) -> Unit,
    onAdd: (LocalDate, Int, Int?, String, String) -> Unit,
    onEdit: (PlanItem, LocalDate, Int, Int?, String, String) -> Unit,
) {
    val context = LocalContext.current
    val colors = studyColors()
    val today = PlanTime.today()
    val nowMin = rememberBeijingMinute()
    val day = remember(viewing, refreshKey) { Plans.of(context).day(viewing.toString()) }
    val upcoming = remember(viewing, refreshKey) { AlarmScheduler.upcomingCount(context) }
    val zoneOff = remember(refreshKey) { !MainActivity.zoneMatchesShanghai() }
    var askClear by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf<PlanItem?>(null) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var menuItem by remember { mutableStateOf<PlanItem?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var scrollingDown by remember { mutableStateOf(false) }
    var collapse by remember { mutableFloatStateOf(0f) }
    var atTop by remember { mutableStateOf(true) }
    var atEnd by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val title = if (viewing == today) "今天" else "${viewing.monthValue}月${viewing.dayOfMonth}日"
    val chrome = menuOpen || !scrollingDown || atTop || atEnd
    val iconOnly = collapse > 0.55f
    val scrimStrength = ((collapse - 0.2f) / 0.45f).coerceIn(0f, 1f)
    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) scrollingDown = true
                else if (available.y > 6f) scrollingDown = false
                return Offset.Zero
            }
        }
    }
    val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dayScope = rememberCoroutineScope()
    val dayDrag = remember { Animatable(0f) }

    LiquidPage { backdrop ->
        AnimatedContent(
            targetState = viewing,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState > initialState
                val edge = if (forward) 1 else -1
                slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { full -> full * edge } togetherWith
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { full -> -full * edge }
            },
            label = "day",
        ) { date ->
            val slotDay = remember(date, refreshKey) { Plans.of(context).day(date.toString()) }
            val listState = rememberLazyListState()
            val collapseDistance = with(density) { 96.dp.toPx() }
            val slotCollapse by remember {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (listState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
                    }
                }
            }
            val slotTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 12 }
            }
            val slotEnd by remember { derivedStateOf { !listState.canScrollForward } }
            val current = slotDay.items.firstOrNull { slotState(it, slotDay.items, date, today, nowMin) == SlotState.CURRENT }
            val next = slotDay.items.firstOrNull { slotState(it, slotDay.items, date, today, nowMin) == SlotState.NEXT }
            val pinnedHere = pinnedId?.takeIf { id -> slotDay.items.any { it.id == id } }
            val focusId = pinnedHere ?: (current ?: next)?.id
            var expanded by remember(date, pinnedHere) { mutableStateOf(setOfNotNull(focusId)) }
            val summary = daySummary(date, today, slotDay.items, current, next, nowMin)
            val weeks = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val slotTitle = if (date == today) "今天" else "${date.monthValue}月${date.dayOfMonth}日"
            val subtitle = "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weeks[date.dayOfWeek.value - 1]}"
            LaunchedEffect(slotCollapse, slotTop, slotEnd, date, viewing) {
                if (date == viewing) {
                    collapse = slotCollapse
                    atTop = slotTop
                    atEnd = slotEnd
                }
            }
            LaunchedEffect(date, focusId, pinnedHere, viewing) {
                if (date != viewing) return@LaunchedEffect
                val index = slotDay.items.indexOfFirst { it.id == focusId }
                if (index >= 0 && (pinnedHere != null || index > 0)) listState.animateScrollToItem(index + 1)
            }
            LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll),
            contentPadding = PaddingValues(
                top = statusPad,
                bottom = navPad + 88.dp,
            ),
        ) {
            item(key = "header") {
                val dragPx = dayDrag.value
                val dragAmount = (abs(dragPx) / with(density) { 96.dp.toPx() }).coerceIn(0f, 1f)
                Column(
                    Modifier
                        .animateItem(placementSpec = Motion.smooth())
                        .daySwipe(dayScope, dayDrag, with(density) { 120.dp.toPx() }, with(density) { 72.dp.toPx() }, onPrev, onNext)
                        .graphicsLayer {
                            translationX = dragPx * 0.45f
                            scaleX = 1f + dragAmount * 0.05f
                            scaleY = 1f - dragAmount * 0.025f
                        }
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = slotTitle,
                        color = colors.label,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                        modifier = if (dragAmount < 0.08f) {
                            Modifier
                        } else {
                            Modifier
                                .liquidGlass(
                                    backdrop,
                                    Capsule(),
                                    colors.glass.copy(alpha = colors.glass.alpha * dragAmount),
                                    blurRadius = 2.dp,
                                    refraction = 24.dp * dragAmount,
                                    clampLens = false,
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = colors.secondary, fontSize = 17.sp)
                    if (date != today) {
                        Text(
                            text = "回到今天",
                            color = colors.blue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(onClick = onToday)
                                .padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    if (slotDay.title.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(slotDay.title, color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = summary, color = colors.secondary, fontSize = 15.sp, lineHeight = 21.sp)
                    if (upcoming > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text("已排好 $upcoming 次未来响铃", color = colors.tertiary, fontSize = 13.sp)
                    }
                    if (zoneOff) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.zone_banner),
                            color = colors.orange,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(backdrop, squircle(22.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                                .padding(14.dp),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    if (slotDay.items.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_day),
                            color = colors.label,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(backdrop, squircle(26.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                                .padding(18.dp),
                        )
                    }
                }
            }
            itemsIndexed(slotDay.items, key = { _, item -> item.id }) { _, item ->
                val state = slotState(item, slotDay.items, date, today, nowMin)
                TimelineRow(
                    item = item,
                    state = state,
                    expanded = item.id in expanded,
                    nowMin = nowMin,
                    end = effectiveEnd(item, slotDay.items),
                    backdrop = backdrop,
                    modifier = Modifier.animateItem(placementSpec = Motion.smooth()),
                    onToggle = {
                        expanded = if (item.id in expanded) expanded - item.id else expanded + item.id
                    },
                    onOpenMenu = { menuItem = item },
                )
            }
        }
        }

        if (scrimStrength > 0.01f) {
            TopFade(statusPad = statusPad, strength = scrimStrength, colors = colors)
        }

        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(Motion.snappy()) +
                slideInVertically(Motion.snappy()) { -it },
            exit = fadeOut(Motion.snappy()) + slideOutVertically { -it },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(onPrev, backdrop, buttonSize = 40.dp) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天", tint = colors.label)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .daySwipe(
                            dayScope,
                            dayDrag,
                            with(density) { 120.dp.toPx() },
                            with(density) { 72.dp.toPx() },
                            onPrev,
                            onNext,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (collapse > 0.62f) {
                        Text(
                            text = title,
                            color = colors.label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .graphicsLayer { alpha = ((collapse - 0.62f) / 0.38f).coerceIn(0f, 1f) }
                                .liquidGlass(backdrop, Capsule(), colors.glass, blurRadius = 6.dp, refraction = 12.dp)
                                .clickable(onClick = onToday)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                GlassIconButton(onNext, backdrop, buttonSize = 40.dp) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天", tint = colors.label)
                }
            }
        }

        if (menuOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { menuOpen = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(Motion.snappy()) +
                slideInVertically(Motion.snappy()) { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 },
        ) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = navPad + 76.dp)
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(vertical = 6.dp),
            ) {
                MenuAction(Icons.Rounded.Checklist, stringResource(R.string.menu_todos), colors.label) {
                    menuOpen = false
                    onOpenTodos()
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.Add, stringResource(R.string.add_slot), colors.label) {
                    menuOpen = false
                    editing = null
                    adding = true
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.FolderOpen, stringResource(R.string.open_file), colors.label) {
                    menuOpen = false
                    onOpenFile()
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.Alarm, stringResource(R.string.test_alarm), colors.label) {
                    menuOpen = false
                    onTestAlarm()
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.Delete, stringResource(R.string.clear_day), colors.red) {
                    menuOpen = false
                    if (day.items.isEmpty()) {
                        Toast.makeText(context, "这一天已经是空的。", Toast.LENGTH_LONG).show()
                    } else {
                        askClear = true
                    }
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.Notifications, stringResource(R.string.menu_permissions), colors.label) {
                    menuOpen = false
                    onPermissions()
                }
                MenuDivider(colors.separator)
                MenuAction(Icons.Rounded.Settings, stringResource(R.string.menu_settings), colors.label) {
                    menuOpen = false
                    onSettings()
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(Motion.snappy()) +
                slideInVertically(Motion.snappy()) { it },
            exit = fadeOut(Motion.snappy()) + slideOutVertically { it },
        ) {
            Row(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    onClick = onOpenTodos,
                    backdrop = backdrop,
                    buttonSize = 48.dp,
                ) {
                    Icon(Icons.Rounded.Checklist, contentDescription = stringResource(R.string.menu_todos), tint = colors.label)
                }
                GlassIconButton(
                    onClick = {
                        editing = null
                        adding = true
                    },
                    backdrop = backdrop,
                    buttonSize = 48.dp,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_slot), tint = colors.label)
                }
                LiquidButton(
                    onClick = onImportClipboard,
                    backdrop = backdrop,
                    tint = colors.blue,
                    height = 48.dp,
                ) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = stringResource(R.string.import_clipboard),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    if (!iconOnly) {
                        Text(
                            stringResource(R.string.import_clipboard),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                GlassIconButton(
                    onClick = { menuOpen = !menuOpen },
                    backdrop = backdrop,
                    buttonSize = 48.dp,
                ) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多", tint = colors.label)
                }
            }
        }
        val opened = menuItem
        if (opened != null) {
            GlassOverlay(backdrop, onDismiss = { menuItem = null }) {
                Text(opened.title, color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("可以改日期、时间和标题，或删掉这一条。", color = colors.secondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    menuItem = null
                    editing = opened
                }) { Text("修改时间和计划", color = colors.blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = {
                    menuItem = null
                    askDelete = opened
                }) { Text("删除", color = colors.red, fontSize = 17.sp) }
            }
        }
        if (askClear) {
            GlassOverlay(backdrop, onDismiss = { askClear = false }) {
                Text("清空这一天？", color = colors.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "会删掉这一天的 ${day.items.size} 条安排，并取消对应的响铃。",
                    color = colors.secondary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { askClear = false }) { Text("取消", color = colors.blue) }
                    TextButton(onClick = {
                        askClear = false
                        onClearDay()
                    }) { Text("清空", color = colors.red) }
                }
            }
        }
        val deleting = askDelete
        if (deleting != null) {
            GlassOverlay(backdrop, onDismiss = { askDelete = null }) {
                Text("删除这条？", color = colors.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(deleting.title, color = colors.secondary, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { askDelete = null }) { Text("取消", color = colors.blue) }
                    TextButton(onClick = {
                        askDelete = null
                        onDelete(deleting)
                    }) { Text("删除", color = colors.red) }
                }
            }
        }
        val editingItem = editing
        if (adding || editingItem != null) {
            PlanEditor(
                item = editingItem,
                initialDate = viewing,
                backdrop = backdrop,
                onDismiss = {
                    adding = false
                    editing = null
                },
                onSave = { date, start, end, title, note ->
                    adding = false
                    editing = null
                    if (editingItem == null) onAdd(date, start, end, title, note)
                    else onEdit(editingItem, date, start, end, title, note)
                },
            )
        }
    }
}

@Composable
private fun BoxScope.TopFade(statusPad: Dp, strength: Float, colors: StudyColors) {
    val cover = statusPad + 46.dp
    val band = cover + 32.dp
    val image = LocalBackdropImage.current
    if (image != null) {
        // Self-picked photo keeps the full-screen backdrop sample so the scrim matches the picture.
        // The default mesh does not use this path.
        BoxWithConstraints(Modifier.matchParentSize()) {
            val screenW = maxWidth
            val screenH = maxHeight
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(band)
                    .graphicsLayer {
                        alpha = strength
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
            ) {
                drawStudyBackdrop(colors, screenW.toPx(), screenH.toPx(), image)
                val coverFrac = (cover.toPx() / size.height).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        coverFrac to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    } else {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(band)
                .graphicsLayer { alpha = strength },
        ) {
            val coverFrac = (cover.toPx() / size.height).coerceIn(0f, 1f)
            val top = colors.base.first()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to top,
                    coverFrac to top,
                    1f to Color.Transparent,
                ),
            )
        }
    }
}

private fun daySummary(
    viewing: LocalDate,
    today: LocalDate,
    items: List<PlanItem>,
    current: PlanItem?,
    next: PlanItem?,
    nowMin: Int,
): String {
    if (items.isEmpty()) return "没有安排"
    val head = if (viewing == today) "今日" else "${viewing.monthValue}月${viewing.dayOfMonth}日"
    val focus = when {
        current != null -> " · 进行中 ${PlanTime.formatMinutes(current.startMinutes)} ${current.title}"
        next != null -> " · 下一节 ${PlanTime.formatMinutes(next.startMinutes)} ${next.title}"
        viewing <= today -> " · 都已结束"
        else -> ""
    }
    val remain = when {
        viewing != today -> ""
        current != null -> " · 剩余 ${(effectiveEnd(current, items) - nowMin).coerceAtLeast(0)} 分钟"
        next != null -> " · 还有 ${(next.startMinutes - nowMin).coerceAtLeast(0)} 分钟"
        else -> ""
    }
    return "$head ${items.size} 项$focus$remain"
}

@Composable
private fun MenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 17.sp)
    }
}

@Composable
private fun MenuDivider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(0.5.dp)
            .background(color),
    )
}

@Composable
private fun TimelineRow(
    item: PlanItem,
    state: SlotState,
    expanded: Boolean,
    nowMin: Int,
    end: Int,
    backdrop: Backdrop,
    onToggle: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimelineBody(item, state, expanded, nowMin, end, backdrop, onToggle, onOpenMenu, modifier)
}

@Composable
private fun TimelineBody(
    item: PlanItem,
    state: SlotState,
    expanded: Boolean,
    nowMin: Int,
    end: Int,
    backdrop: Backdrop,
    onToggle: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = studyColors()
    val press = rememberLiquidPress(captureDrag = false)
    val timeColor = when (state) {
        SlotState.PAST -> colors.tertiary
        SlotState.CURRENT -> colors.blue
        SlotState.NEXT -> colors.green
        SlotState.FUTURE -> colors.label
    }
    val titleColor = if (state == SlotState.PAST) colors.tertiary else colors.label
    val bodyModifier = Modifier
        .liquidGlass(
            backdrop = backdrop,
            shape = squircle(26.dp),
            surface = colors.glass,
            blurRadius = 2.dp,
            refraction = 24.dp,
            chromatic = true,
            press = press,
        )
        .liquidPressFeedback(press)
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, bottom = 14.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .combinedClickable(onClick = onToggle, onLongClick = onOpenMenu),
    ) {
        Column(Modifier.width(68.dp).padding(top = 18.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = PlanTime.formatMinutes(item.startMinutes),
                color = timeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                style = Tabular,
            )
            item.endMinutes?.let { endMin ->
                Text(
                    text = PlanTime.formatMinutes(endMin),
                    color = colors.tertiary,
                    fontSize = 12.sp,
                    style = Tabular,
                )
            }
        }
        Box(Modifier.width(28.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .padding(top = 22.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(timeColor),
            )
        }
        Column(
            bodyModifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = titleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                when (state) {
                    SlotState.CURRENT -> Pill("进行中", colors.blue, backdrop)
                    SlotState.NEXT -> Pill("下一节", colors.green, backdrop)
                    else -> Unit
                }
            }
            AnimatedVisibility(
                visible = expanded && item.note.isNotBlank(),
                enter = expandVertically(Motion.snappy()) + fadeIn(),
                exit = shrinkVertically(Motion.snappy()) + fadeOut(),
            ) {
                Text(
                    item.note,
                    color = if (state == SlotState.PAST) colors.tertiary else colors.secondary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state == SlotState.CURRENT) {
                val span = (end - item.startMinutes).coerceAtLeast(1)
                val progress = ((nowMin - item.startMinutes).toFloat() / span).coerceIn(0f, 1f)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(Capsule())
                        .background(colors.track),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(colors.blue),
                    )
                }
            }
        }
    }
}

private fun Modifier.daySwipe(
    scope: CoroutineScope,
    offset: Animatable<Float, AnimationVector1D>,
    limit: Float,
    threshold: Float,
    onPrev: () -> Unit,
    onNext: () -> Unit,
): Modifier = pointerInput(onPrev, onNext, limit, threshold) {
    val tracker = VelocityTracker()
    var snap: Job? = null
    var dragOffset = offset.value
    fun settle(velocity: Float) {
        val pending = snap
        snap = null
        scope.launch {
            pending?.cancel()
            pending?.join()
            offset.animateTo(0f, Motion.smooth(), initialVelocity = velocity)
        }
    }
    detectHorizontalDragGestures(
        onDragStart = {
            tracker.resetTracking()
            dragOffset = offset.value
        },
        onDragEnd = {
            val velocity = tracker.calculateVelocity().x
            val value = dragOffset
            settle(velocity)
            when {
                velocity > Motion.Fling -> onPrev()
                velocity < -Motion.Fling -> onNext()
                value > threshold -> onPrev()
                value < -threshold -> onNext()
            }
        },
        onDragCancel = { settle(0f) },
        onHorizontalDrag = { change, delta ->
            tracker.addPosition(change.uptimeMillis, change.position)
            dragOffset = resistedDrag(dragOffset, delta, -limit, limit, limit)
            val next = dragOffset
            snap?.cancel()
            snap = scope.launch { offset.snapTo(next) }
        },
    )
}

private fun defaultSlotStart(date: LocalDate): Int {
    if (date != PlanTime.today()) return 8 * 60
    val now = PlanTime.nowMinutes()
    val rounded = if (now % 5 == 0) now else now + (5 - now % 5)
    return rounded.coerceIn(0, 23 * 60 + 55)
}

@Composable
private fun BoxScope.PlanEditor(
    item: PlanItem?,
    initialDate: LocalDate,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, start: Int, end: Int?, title: String, note: String) -> Unit,
) {
    val colors = studyColors()
    val key = item?.id ?: "new"
    val weeks = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    var date by remember(key) { mutableStateOf(item?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() } ?: initialDate) }
    val seedStart = remember(key) { item?.startMinutes ?: defaultSlotStart(initialDate) }
    var startText by remember(key) { mutableStateOf(PlanTime.formatMinutes(seedStart)) }
    var endText by remember(key) {
        mutableStateOf(
            item?.endMinutes?.let(PlanTime::formatMinutes)
                ?: if (item == null) PlanTime.formatMinutes(seedStart + 45) else "",
        )
    }
    var title by remember(key) { mutableStateOf(item?.title.orEmpty()) }
    var note by remember(key) { mutableStateOf(item?.note.orEmpty()) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    val view = LocalView.current
    fun reject(message: String) {
        error = message
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.label,
        unfocusedTextColor = colors.label,
        focusedBorderColor = colors.blue,
        unfocusedBorderColor = colors.separator,
        cursorColor = colors.blue,
        focusedLabelColor = colors.secondary,
        unfocusedLabelColor = colors.secondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )
    val dateTitle = if (date == PlanTime.today()) "今天" else "${date.monthValue}月${date.dayOfMonth}日"
    val dateSub = "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${weeks[date.dayOfWeek.value - 1]}"
    GlassOverlay(backdrop, onDismiss) {
            Text(
                if (item == null) "添加时段" else "修改计划",
                color = colors.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                GlassIconButton(
                    onClick = { date = date.minusDays(1) },
                    backdrop = backdrop,
                    buttonSize = 40.dp,
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "前一天", tint = colors.label)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dateTitle, color = colors.label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(dateSub, color = colors.secondary, fontSize = 13.sp)
                }
                GlassIconButton(
                    onClick = { date = date.plusDays(1) },
                    backdrop = backdrop,
                    buttonSize = 40.dp,
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "后一天", tint = colors.label)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("开始") },
                    placeholder = { Text("08:40") },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    shape = squircle(14.dp),
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("结束") },
                    placeholder = { Text("选填") },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    shape = squircle(14.dp),
                    colors = fieldColors,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = squircle(14.dp),
                colors = fieldColors,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = squircle(14.dp),
                colors = fieldColors,
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error.orEmpty(), color = colors.red, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = colors.blue) }
                TextButton(onClick = {
                    val start = PlanTime.parseClock(startText)
                    if (start == null || start >= 24 * 60) {
                        reject("开始时间写成 08:40 这样。")
                        return@TextButton
                    }
                    val end = if (endText.isBlank()) {
                        null
                    } else {
                        val parsed = PlanTime.parseClock(endText)
                        if (parsed == null) {
                            reject("结束时间写成 09:20，或留空。")
                            return@TextButton
                        }
                        PlanTime.endAfter(start, parsed)
                    }
                    if (title.isBlank()) {
                        reject("写上这一条的计划。")
                        return@TextButton
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSave(date, start, end, title.trim(), note.trim())
                }) { Text("保存", color = colors.blue, fontWeight = FontWeight.SemiBold) }
            }
    }
}

@Composable
private fun Pill(text: String, color: Color, backdrop: Backdrop) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .liquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                surface = color.copy(alpha = 0.20f),
                blurRadius = 2.dp,
                refraction = 6.dp,
                chromatic = false,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
