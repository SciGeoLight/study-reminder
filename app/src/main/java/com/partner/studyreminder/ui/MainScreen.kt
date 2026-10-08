package com.partner.studyreminder.ui

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.R
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.alarm.AlarmWindow
import com.partner.studyreminder.data.PlanItem
import com.partner.studyreminder.data.Plans
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassOverlay
import com.partner.studyreminder.ui.glass.GlassSheet
import com.partner.studyreminder.ui.glass.GlassTier
import com.partner.studyreminder.ui.glass.SheetHeader
import com.partner.studyreminder.ui.glass.GlassUndoBar
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.glass
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
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    onRestore: (PlanItem, String) -> Unit,
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
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var contextItem by remember { mutableStateOf<PlanItem?>(null) }
    var contextAnchor by remember { mutableStateOf<CardBounds?>(null) }
    var undo by remember { mutableStateOf<Pair<PlanItem, String>?>(null) }
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    var scrollingDown by remember { mutableStateOf(false) }
    var collapse by remember { mutableFloatStateOf(0f) }
    var atTop by remember { mutableStateOf(true) }
    var atEnd by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val title = if (viewing == today) "今天" else "${viewing.monthValue}月${viewing.dayOfMonth}日"
    val chrome = menuOpen || contextItem != null || !scrollingDown || atTop || atEnd
    LaunchedEffect(viewing) {
        contextItem = null
        contextAnchor = null
    }
    LaunchedEffect(undo?.first?.id) {
        val snapshot = undo ?: return@LaunchedEffect
        delay(4_000)
        if (undo?.first?.id == snapshot.first.id) undo = null
    }
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
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { pageOrigin = it.positionInRoot() },
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
            val entries = remember(slotDay.items) { timelineEntries(slotDay.items) }
            val pinnedHere = pinnedId?.takeIf { id -> slotDay.items.any { it.id == id } }
            val focusId = pinnedHere ?: (current ?: next)?.id
            var expanded by remember(date, pinnedHere) { mutableStateOf(setOfNotNull(focusId)) }
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
            LaunchedEffect(date, focusId, pinnedHere, viewing, entries) {
                if (date != viewing) return@LaunchedEffect
                val index = entries.indexOfFirst { it is TimelineEntry.Slot && it.item.id == focusId }
                if (index >= 0 && (pinnedHere != null || index > 0)) listState.animateScrollToItem(index + 1)
            }
            LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll),
            contentPadding = PaddingValues(
                top = statusPad,
                bottom = navPad + 12.dp + 56.dp + 16.dp,
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
                    Spacer(Modifier.height(56.dp))
                    Text(
                        text = slotTitle,
                        color = colors.label,
                        fontSize = 34.sp,
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
                    Spacer(Modifier.height(10.dp))
                    DayChips(
                        items = slotDay.items,
                        viewing = date,
                        today = today,
                        nowMin = nowMin,
                        upcoming = upcoming,
                        backdrop = backdrop,
                        onPermissions = onPermissions,
                    )
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
                                .glass(backdrop, GlassTier.Card, squircle(26.dp))
                                .padding(18.dp),
                        )
                    }
                }
            }
            itemsIndexed(
                entries,
                key = { _, entry ->
                    when (entry) {
                        is TimelineEntry.Slot -> entry.item.id
                        is TimelineEntry.Gap -> "gap-${entry.minutes}-${entry.afterId}"
                    }
                },
            ) { index, entry ->
                val lineAbove = index > 0
                val lineBelow = index < entries.lastIndex
                when (entry) {
                    is TimelineEntry.Gap -> GapRow(
                        minutes = entry.minutes,
                        lineColor = colors.separator,
                        modifier = Modifier.animateItem(placementSpec = Motion.smooth()),
                    )
                    is TimelineEntry.Slot -> {
                        val item = entry.item
                        val state = slotState(item, slotDay.items, date, today, nowMin)
                        val slotOrdinal = entries.take(index + 1).count { it is TimelineEntry.Slot } - 1
                        TimelineRow(
                            item = item,
                            state = state,
                            expanded = item.id in expanded,
                            lifted = contextItem?.id == item.id,
                            nowMin = nowMin,
                            end = effectiveEnd(item, slotDay.items),
                            backdrop = backdrop,
                            lineAbove = lineAbove,
                            lineBelow = lineBelow,
                            lensEnabled = slotOrdinal < 5,
                            modifier = Modifier.animateItem(placementSpec = Motion.smooth()),
                            onToggle = {
                                expanded = if (item.id in expanded) expanded - item.id else expanded + item.id
                            },
                            onOpenMenu = { bounds ->
                                menuOpen = false
                                contextItem = item
                                contextAnchor = bounds
                            },
                            onPosition = { bounds ->
                                if (contextItem?.id == item.id) contextAnchor = bounds
                            },
                        )
                    }
                }
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
                GlassIconButton(onPrev, backdrop) {
                    Icon(StudyIcons.ChevronLeft, contentDescription = "前一天", tint = colors.label)
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
                GlassIconButton(onNext, backdrop) {
                    Icon(StudyIcons.ChevronRight, contentDescription = "后一天", tint = colors.label)
                }
            }
        }

        if (menuOpen || contextItem != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            menuOpen = false
                            contextItem = null
                            contextAnchor = null
                        },
                    ),
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(Motion.snappy()) +
                slideInVertically(Motion.snappy()) { it / 3 },
            exit = fadeOut(Motion.snappy()) + slideOutVertically(Motion.snappy()) { it / 3 },
        ) {
            Column(
                Modifier
                    .padding(end = 88.dp, bottom = navPad + 12.dp + 56.dp + 8.dp)
                    .width(240.dp)
                    .glass(backdrop, GlassTier.Float, squircle(22.dp), surface = colors.glass)
                    .padding(vertical = 6.dp),
            ) {
                MenuAction(StudyIcons.FolderOpen, stringResource(R.string.open_file), colors.label) {
                    menuOpen = false
                    onOpenFile()
                }
                MenuDivider(colors.separator)
                MenuAction(StudyIcons.Alarm, stringResource(R.string.test_alarm), colors.label) {
                    menuOpen = false
                    onTestAlarm()
                }
                MenuDivider(colors.separator)
                MenuAction(StudyIcons.Delete, stringResource(R.string.clear_day), colors.red) {
                    menuOpen = false
                    if (day.items.isEmpty()) {
                        Toast.makeText(context, "这一天已经是空的。", Toast.LENGTH_LONG).show()
                    } else {
                        askClear = true
                    }
                }
                MenuDivider(colors.separator)
                MenuAction(StudyIcons.Notifications, stringResource(R.string.menu_permissions), colors.label) {
                    menuOpen = false
                    onPermissions()
                }
                MenuDivider(colors.separator)
                MenuAction(StudyIcons.Settings, stringResource(R.string.menu_settings), colors.label) {
                    menuOpen = false
                    onSettings()
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(Motion.snappy()) +
                slideInVertically(Motion.snappy()) { it },
            exit = fadeOut(Motion.snappy()) + slideOutVertically(Motion.snappy()) { it },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .glass(backdrop, GlassTier.Float, Capsule()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarSlot(StudyIcons.Checklist, stringResource(R.string.menu_todos), onClick = onOpenTodos)
                    BarSlot(
                        icon = StudyIcons.ContentPaste,
                        label = "导入",
                        contentDescription = stringResource(R.string.import_clipboard),
                        onClick = onImportClipboard,
                    )
                    BarSlot(StudyIcons.MoreHoriz, "更多") {
                        contextItem = null
                        contextAnchor = null
                        menuOpen = !menuOpen
                    }
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.blue)
                        .clickable(interactionSource = null, indication = null) {
                            menuOpen = false
                            contextItem = null
                            contextAnchor = null
                            editing = null
                            adding = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        StudyIcons.Add,
                        contentDescription = stringResource(R.string.add_slot),
                        tint = Color.White,
                    )
                }
            }
        }
        val opened = contextItem
        val anchor = contextAnchor
        if (opened != null && anchor != null) {
            val menuWidth = 176.dp
            val menuHeight = 100.dp
            Column(
                Modifier
                    .offset {
                        val widthPx = menuWidth.toPx()
                        val heightPx = menuHeight.toPx()
                        val left = (anchor.right - pageOrigin.x - widthPx).coerceAtLeast(12.dp.toPx())
                        val top = (anchor.top - pageOrigin.y - heightPx - 8.dp.toPx()).coerceAtLeast(8.dp.toPx())
                        IntOffset(left.roundToInt(), top.roundToInt())
                    }
                    .width(menuWidth)
                    .glass(backdrop, GlassTier.Float, squircle(22.dp), surface = colors.glass)
                    .padding(vertical = 4.dp),
            ) {
                ContextMenuRow("修改", colors.label) {
                    contextItem = null
                    contextAnchor = null
                    editing = opened
                }
                MenuDivider(colors.separator, 16.dp)
                ContextMenuRow("删除", colors.red) {
                    val keptTitle = day.title
                    contextItem = null
                    contextAnchor = null
                    undo = opened to keptTitle
                    onDelete(opened)
                }
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
        GlassUndoBar(
            visible = undo != null && editing == null && !adding,
            backdrop = backdrop,
            onUndo = {
                val pending = undo ?: return@GlassUndoBar
                undo = null
                onRestore(pending.first, pending.second)
            },
            bottom = 12.dp + 56.dp + 8.dp,
        )
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
internal fun BoxScope.TopFade(statusPad: Dp, strength: Float, colors: StudyColors) {
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

private data class CardBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

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
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 17.sp)
    }
}

@Composable
private fun MenuDivider(color: Color, inset: Dp = 50.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(color),
    )
}

@Composable
private fun ContextMenuRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun RowScope.BarSlot(
    icon: ImageVector,
    label: String,
    contentDescription: String = label,
    onClick: () -> Unit,
) {
    val colors = studyColors()
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.label, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(1.dp))
        Text(label, color = colors.label, fontSize = 11.sp)
    }
}

@Composable
private fun DayChips(
    items: List<PlanItem>,
    viewing: LocalDate,
    today: LocalDate,
    nowMin: Int,
    upcoming: Int,
    backdrop: Backdrop,
    onPermissions: () -> Unit,
) {
    val colors = studyColors()
    val done = finishedCount(items, viewing, today, nowMin)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoChip(backdrop) {
            ProgressRing(done, items.size, colors.blue, colors.track)
            Text(
                "$done/${items.size}",
                color = colors.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = Tabular,
            )
        }
        InfoChip(backdrop, onClick = onPermissions) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (upcoming > 0) colors.green else colors.orange),
            )
            Text(
                "已排 $upcoming/${AlarmWindow.LIMIT}",
                color = colors.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = Tabular,
            )
        }
        InfoChip(backdrop) {
            Text(
                nextOrTotalLabel(items, viewing, today, nowMin),
                color = colors.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InfoChip(
    backdrop: Backdrop,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = studyColors()
    Row(
        Modifier
            .glass(backdrop, GlassTier.Control, Capsule(), surface = colors.glass)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun ProgressRing(done: Int, total: Int, color: Color, track: Color) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Canvas(Modifier.size(14.dp)) {
        val strokePx = 2.dp.toPx()
        val radius = (size.minDimension - strokePx) / 2f
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
        drawCircle(color = track, radius = radius, style = stroke)
        if (fraction > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(size.width - strokePx, size.height - strokePx),
                style = stroke,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    item: PlanItem,
    state: SlotState,
    expanded: Boolean,
    lifted: Boolean,
    nowMin: Int,
    end: Int,
    backdrop: Backdrop,
    lineAbove: Boolean,
    lineBelow: Boolean,
    lensEnabled: Boolean,
    onToggle: () -> Unit,
    onOpenMenu: (CardBounds) -> Unit,
    onPosition: (CardBounds) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = studyColors()
    val press = rememberLiquidPress(captureDrag = false)
    val lift by animateFloatAsState(if (lifted) 1.04f else 1f, Motion.snappy(), label = "lift")
    var latest by remember { mutableStateOf<CardBounds?>(null) }
    val timeColor = when (state) {
        SlotState.PAST -> colors.tertiary
        SlotState.CURRENT -> colors.blue
        SlotState.NEXT -> colors.green
        SlotState.FUTURE -> colors.label
    }
    val titleColor = if (state == SlotState.PAST) colors.tertiary else colors.label
    val shape = squircle(26.dp)
    val stroke = when (state) {
        SlotState.CURRENT -> 1.5.dp to colors.blue
        SlotState.NEXT -> 1.dp to colors.green
        else -> null
    }
    Row(
        modifier
            .zIndex(if (lifted) 2f else 0f)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp)
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = { latest?.let(onOpenMenu) },
            ),
    ) {
        Column(
            Modifier.width(68.dp).padding(top = 18.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.End,
        ) {
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
        DotColumn(
            state = state,
            lineAbove = lineAbove,
            lineBelow = lineBelow,
            color = timeColor,
            lineColor = colors.separator,
            modifier = Modifier.width(28.dp).fillMaxHeight(),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
                .onGloballyPositioned { coords ->
                    val p = coords.positionInRoot()
                    val next = CardBounds(
                        p.x,
                        p.y,
                        p.x + coords.size.width.toFloat(),
                        p.y + coords.size.height.toFloat(),
                    )
                    latest = next
                    if (lifted) onPosition(next)
                }
                .graphicsLayer {
                    scaleX = lift
                    scaleY = lift
                }
                .glass(backdrop, GlassTier.Card, shape, press = press, lensEnabled = lensEnabled)
                .then(
                    if (stroke == null) {
                        Modifier
                    } else {
                        Modifier.border(stroke.first, stroke.second, shape)
                    },
                )
                .liquidPressFeedback(press)
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

private enum class PlanPick { Date, Start, End }

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
    var date by remember(key) { mutableStateOf(item?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() } ?: initialDate) }
    val seedStart = remember(key) { item?.startMinutes ?: defaultSlotStart(initialDate) }
    var start by remember(key) { mutableIntStateOf(seedStart) }
    var end by remember(key) { mutableStateOf(item?.endMinutes ?: if (item == null) seedStart + 45 else null) }
    var title by remember(key) { mutableStateOf(item?.title.orEmpty()) }
    var note by remember(key) { mutableStateOf(item?.note.orEmpty()) }
    var open by remember(key) { mutableStateOf<PlanPick?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    val view = LocalView.current
    fun reject(message: String) {
        error = message
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
    fun toggle(pick: PlanPick) {
        open = if (open == pick) null else pick
    }
    GlassSheet(
        backdrop = backdrop,
        onDismiss = onDismiss,
        header = {
            SheetHeader(
                title = if (item == null) "添加时段" else "修改计划",
                confirmLabel = if (item == null) "添加" else "完成",
                confirmEnabled = title.isNotBlank(),
                backdrop = backdrop,
                onClose = onDismiss,
                onConfirm = {
                    if (start < 0 || start >= 24 * 60) {
                        reject("开始时间写成 08:40 这样。")
                        return@SheetHeader
                    }
                    val savedEnd = end?.let { value ->
                        if (value >= 24 * 60) value else PlanTime.endAfter(start, value)
                    }
                    if (savedEnd != null && savedEnd <= start && savedEnd < 24 * 60) {
                        reject("结束时间写成 09:20，或留空。")
                        return@SheetHeader
                    }
                    if (title.isBlank()) {
                        reject("写上这一条的计划。")
                        return@SheetHeader
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSave(date, start, savedEnd, title.trim(), note.trim())
                },
            )
        },
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp),
        ) {
            if (error != null) {
                Text(error.orEmpty(), color = colors.red, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
            }
            GlassSection(backdrop) {
                DateRow("日期", date, open == PlanPick.Date, "plan-date") { toggle(PlanPick.Date) }
                if (open == PlanPick.Date) {
                    InlineCalendar(date, "plan-calendar") { date = it }
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassSection(backdrop) {
                ValueRow("开始", PlanTime.formatMinutes(start), open == PlanPick.Start, "plan-start") {
                    toggle(PlanPick.Start)
                }
                if (open == PlanPick.Start) {
                    TimeWheels(start) { picked ->
                        end = endWhenStartMoves(start, end, picked)
                        start = picked
                    }
                }
                Hairline()
                ValueRow(
                    "结束",
                    end?.let(PlanTime::formatMinutes) ?: "选填",
                    open == PlanPick.End,
                    "plan-end",
                ) { toggle(PlanPick.End) }
                if (open == PlanPick.End) {
                    TimeWheels(end ?: start) { picked ->
                        end = PlanTime.endAfter(start, picked)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DurationChoices.forEach { minutes ->
                    val selected = end?.minus(start) == minutes
                    Text(
                        "${minutes}分钟",
                        color = if (selected) colors.blue else colors.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(Capsule())
                            .background(if (selected) colors.blue.copy(alpha = 0.20f) else colors.track)
                            .clickable {
                                end = start + minutes
                                error = null
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassSection(backdrop) {
                BorderlessField(title, "标题", colors) {
                    title = it
                    error = null
                }
                Hairline()
                BorderlessField(note, "备注", colors) { note = it }
            }
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
            .glass(
                backdrop,
                GlassTier.Control,
                Capsule(),
                surface = color.copy(alpha = 0.20f),
                refraction = 6.dp,
                chromatic = false,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun DotColumn(
    state: SlotState?,
    lineAbove: Boolean,
    lineBelow: Boolean,
    color: Color,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val dot = when (state) {
        SlotState.PAST -> 6.dp
        SlotState.CURRENT -> 10.dp
        null -> 0.dp
        else -> 8.dp
    }
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = 26.dp.toPx()
        val stroke = 2.dp.toPx()
        val cover = if (state == SlotState.CURRENT) 9.dp.toPx() else dot.toPx() / 2f
        if (lineAbove) {
            drawLine(lineColor, Offset(cx, 0f), Offset(cx, (cy - cover).coerceAtLeast(0f)), strokeWidth = stroke)
        }
        if (lineBelow) {
            drawLine(
                lineColor,
                Offset(cx, (cy + cover).coerceAtMost(size.height)),
                Offset(cx, size.height),
                strokeWidth = stroke,
            )
        }
        if (state == SlotState.CURRENT) {
            drawCircle(
                color = color.copy(alpha = 0.28f),
                radius = 9.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        if (state != null) {
            drawCircle(color = color, radius = dot.toPx() / 2f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun GapRow(minutes: Int, lineColor: Color, modifier: Modifier = Modifier) {
    val colors = studyColors()
    Row(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(68.dp))
        DotColumn(
            state = null,
            lineAbove = true,
            lineBelow = true,
            color = lineColor,
            lineColor = lineColor,
            modifier = Modifier.width(28.dp).fillMaxHeight(),
        )
        Text(
            text = "间隔 $minutes 分钟",
            color = colors.tertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}
