package com.partner.studyreminder.ui

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.data.Todo
import com.partner.studyreminder.data.TodoGroup
import com.partner.studyreminder.data.TodoImages
import com.partner.studyreminder.data.Todos
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassSheet
import com.partner.studyreminder.ui.glass.SheetHeader
import com.partner.studyreminder.ui.glass.GlassUndoBar
import com.partner.studyreminder.ui.glass.LiquidBottomTab
import com.partner.studyreminder.ui.glass.LiquidBottomTabs
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.LiquidSwitch
import com.partner.studyreminder.ui.glass.StudyColors
import com.partner.studyreminder.ui.glass.applyLiquidPress
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.liquidPressFeedback
import com.partner.studyreminder.ui.glass.rememberLiquidPress
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class DraftPhoto(
    val key: String,
    val storedName: String? = null,
    val uri: Uri? = null,
    val file: File? = null,
)

private const val FILTER_ALL = "*"
private const val FILTER_NONE = "-"
private const val FILTER_MORE = "more"
private enum class Bucket(val label: String) { OVERDUE("已过期"), TODAY("今天"), UPCOMING("即将到来"), DONE("已完成") }
private enum class DateTarget { START, END, REMIND }

private fun bucketOf(todo: Todo, today: LocalDate): Bucket {
    if (todo.done) return Bucket.DONE
    val start = runCatching { LocalDate.parse(todo.startDate) }.getOrNull() ?: today
    val end = runCatching { LocalDate.parse(todo.endDate) }.getOrNull() ?: start
    return when {
        end < today -> Bucket.OVERDUE
        start <= today -> Bucket.TODAY
        else -> Bucket.UPCOMING
    }
}

private fun StudyColors.dot(color: String): Color = when (color) {
    "green" -> green
    "orange" -> orange
    "red" -> red
    else -> blue
}

@Composable
internal fun TodosScreen(
    refreshKey: Int,
    onBack: () -> Unit,
    onToggle: (Todo) -> Unit,
    onSave: (Todo?, String, String, String, String, String?, Int?, String?, List<DraftPhoto>) -> Unit,
    onDelete: (Todo) -> Unit,
    onUndo: (Todo) -> Unit,
    onForget: (Todo) -> Unit,
    onAddGroup: (String) -> Boolean,
    onRenameGroup: (String, String) -> Boolean,
    onRecolorGroup: (String) -> Unit,
    onMoveGroup: (String, Int) -> Unit,
    onDeleteGroup: (String, Boolean) -> Unit,
    onRemoveStoredImage: (Todo, String) -> Unit,
    onPickImages: ((List<Uri>) -> Unit) -> Unit,
    onTakePhoto: ((File?) -> Unit) -> Unit,
) {
    val colors = studyColors()
    val view = LocalView.current
    val context = LocalContext.current
    val today = PlanTime.today()
    var todos by remember(refreshKey) { mutableStateOf(Todos.of(context).all()) }
    var groups by remember(refreshKey) { mutableStateOf(Todos.of(context).groups()) }
    LaunchedEffect(refreshKey) {
        todos = Todos.of(context).all()
        groups = Todos.of(context).groups()
    }
    var filter by remember { mutableStateOf(FILTER_ALL) }
    var showOverflow by remember { mutableStateOf(false) }
    var barEpoch by remember { mutableIntStateOf(0) }
    var pinned by remember { mutableStateOf(Prefs.todoBarGroups(context)) }
    if (filter != FILTER_ALL && filter != FILTER_NONE && groups.none { it.id == filter }) filter = FILTER_ALL
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Todo?>(null) }
    var undo by remember { mutableStateOf<Todo?>(null) }
    var showGroups by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val listState = rememberLazyListState()
    val collapsed = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 36
    val visible = todos.filter { todo ->
        when (filter) {
            FILTER_ALL -> true
            FILTER_NONE -> todo.groupId == null
            else -> todo.groupId == filter
        }
    }
    val openCount = visible.count { !it.done }
    val sections = Bucket.entries.map { bucket ->
        bucket to visible.filter { bucketOf(it, today) == bucket }
            .sortedWith(compareBy({ it.endDate }, { it.startDate }, { it.title }))
    }.filter { it.second.isNotEmpty() }

    LiquidPage { backdrop ->
        if (showGroups) {
            GroupsPage(
                groups = groups,
                todos = todos,
                backdrop = backdrop,
                onBack = { showGroups = false },
                onOpen = { id ->
                    filter = id
                    showGroups = false
                },
                onAdd = onAddGroup,
                onRename = onRenameGroup,
                onRecolor = onRecolorGroup,
                onMove = onMoveGroup,
                onDelete = onDeleteGroup,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("reminders-list"),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp,
                ),
            ) {
                item(key = "head") {
                    Column(Modifier.animateItem(placementSpec = Motion.smooth()).padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassIconButton(onBack, backdrop, buttonSize = 36.dp) {
                                Icon(StudyIcons.ChevronLeft, contentDescription = "返回", tint = colors.blue)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "分组",
                                color = colors.blue,
                                fontSize = 17.sp,
                                modifier = Modifier
                                    .clickable(interactionSource = null, indication = null) { showGroups = true }
                                    .padding(8.dp),
                            )
                        }
                        Text("待办", color = colors.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                        Text(
                            if (visible.isEmpty()) "没有待办" else "未完成 $openCount 件",
                            color = colors.secondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "点右下角加号写一件。",
                            color = colors.secondary,
                            fontSize = 17.sp,
                            modifier = Modifier.animateItem(placementSpec = Motion.smooth()).padding(horizontal = 32.dp, vertical = 12.dp),
                        )
                    }
                }
                sections.forEach { (bucket, rows) ->
                    item(key = "h-${filter}-${bucket.name}") {
                        Text(
                            bucket.label,
                            color = colors.label,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.animateItem(placementSpec = Motion.smooth()).padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                    items(rows, key = { "t-$filter-${it.id}" }) { todo ->
                        TodoRow(
                            modifier = Modifier.animateItem(placementSpec = Motion.smooth()),
                            todo = todo,
                            group = groups.firstOrNull { it.id == todo.groupId },
                            backdrop = backdrop,
                            onToggle = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onToggle(todo)
                            },
                            onOpen = { editing = todo },
                            onDelete = {
                                undo = todo
                                onDelete(todo)
                            },
                            onOpenImage = { image -> viewer = todo.id to image },
                        )
                    }
                }
            }
            if (collapsed) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(colors.glass.copy(alpha = 0.92f))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(onBack, backdrop, buttonSize = 36.dp) {
                        Icon(StudyIcons.ChevronLeft, contentDescription = "返回", tint = colors.blue)
                    }
                    Text(
                        "待办",
                        color = colors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        "分组",
                        color = colors.blue,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .clickable(interactionSource = null, indication = null) { showGroups = true }
                            .padding(8.dp),
                    )
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentedFilters(
                    groups = groups,
                    filter = filter,
                    pinned = pinned,
                    settleKey = barEpoch,
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    onMore = { showOverflow = true },
                ) { filter = it }
                GlassIconButton(
                    onClick = { adding = true },
                    backdrop = backdrop,
                    modifier = Modifier.testTag("add-todo"),
                    buttonSize = 52.dp,
                ) {
                    Icon(StudyIcons.Add, contentDescription = "添加待办", tint = colors.label)
                }
            }
            if (showOverflow) {
                FilterOverflowSheet(
                    groups = groups,
                    todos = todos,
                    pinned = pinned,
                    backdrop = backdrop,
                    onPinned = { next ->
                        pinned = next
                        Prefs.setTodoBarGroups(context, next)
                    },
                    onPick = { id ->
                        filter = id
                        showOverflow = false
                        barEpoch++
                    },
                    onDismiss = {
                        showOverflow = false
                        barEpoch++
                    },
                )
            }
        }
        GlassUndoBar(
            visible = undo != null && editing == null && !adding,
            backdrop = backdrop,
            onUndo = {
                undo?.let(onUndo)
                undo = null
            },
        )
        if (adding || editing != null) {
            TodoEditorSheet(
                existing = editing,
                groups = groups,
                backdrop = backdrop,
                onDismiss = {
                    adding = false
                    editing = null
                },
                onSave = { title, note, start, end, remindDate, remindMinutes, groupId, photos ->
                    onSave(editing, title, note, start, end, remindDate, remindMinutes, groupId, photos)
                    adding = false
                    editing = null
                },
                onPickImages = onPickImages,
                onTakePhoto = onTakePhoto,
            )
        }
        val viewing = viewer
        if (viewing != null) {
            val live = todos.firstOrNull { it.id == viewing.first }
            if (live == null || live.images.isEmpty()) viewer = null
            else {
                PhotoViewer(
                    photos = live.images.map { DraftPhoto(it, storedName = it) },
                    start = viewing.second.coerceIn(0, live.images.lastIndex),
                    todoId = live.id,
                    backdrop = backdrop,
                    onClose = { viewer = null },
                    onRemove = { name -> onRemoveStoredImage(live, name) },
                )
            }
        }
    }
    LaunchedEffect(undo?.id) {
        val snapshot = undo ?: return@LaunchedEffect
        delay(4_000)
        if (undo?.id == snapshot.id) {
            onForget(snapshot)
            undo = null
        }
    }
}

private data class FilterChip(val id: String, val label: String, val dot: Color?)

private fun exposedGroups(groups: List<TodoGroup>, pinned: List<String>?): List<TodoGroup> {
    if (pinned == null) return groups.take(Prefs.TODO_BAR_LIMIT)
    val byId = groups.associateBy { it.id }
    return pinned.mapNotNull { byId[it] }.take(Prefs.TODO_BAR_LIMIT)
}

@Composable
private fun SegmentedFilters(
    groups: List<TodoGroup>,
    filter: String,
    pinned: List<String>?,
    settleKey: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onMore: () -> Unit,
    onFilter: (String) -> Unit,
) {
    val colors = studyColors()
    val lightTheme = colors.label.red < 0.5f
    val selectedColor = if (lightTheme) colors.label else Color(0xFF1C1C1E)
    val idleColor = if (lightTheme) colors.secondary else Color.White.copy(alpha = 0.84f)
    val exposed = exposedGroups(groups, pinned)
    val hiddenGroup = filter != FILTER_ALL && filter != FILTER_NONE && exposed.none { it.id == filter }
    val moreLabel = if (hiddenGroup) groups.firstOrNull { it.id == filter }?.name ?: "…" else "…"
    val options = buildList {
        add(FilterChip(FILTER_ALL, "全部", null))
        exposed.forEach { add(FilterChip(it.id, it.name, colors.dot(it.color))) }
        add(FilterChip(FILTER_MORE, moreLabel, if (hiddenGroup) groups.firstOrNull { it.id == filter }?.let { colors.dot(it.color) } else null))
    }
    val selectedIndex = when {
        filter == FILTER_ALL -> 0
        exposed.indexOfFirst { it.id == filter } >= 0 -> exposed.indexOfFirst { it.id == filter } + 1
        else -> options.lastIndex
    }
    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = { index ->
            val chip = options.getOrNull(index) ?: return@LiquidBottomTabs
            if (chip.id == FILTER_MORE) onMore() else onFilter(chip.id)
        },
        backdrop = backdrop,
        tabsCount = options.size,
        modifier = modifier,
        barHeight = 52.dp,
        settleKey = settleKey,
    ) {
        options.forEachIndexed { index, chip ->
            val selected = index == selectedIndex
            LiquidBottomTab(
                onClick = {
                    if (chip.id == FILTER_MORE) onMore() else onFilter(chip.id)
                },
                modifier = Modifier.testTag(if (chip.id == FILTER_MORE) "filter-more" else "filter-${chip.label}"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (chip.dot != null) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(chip.dot))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        chip.label,
                        color = if (selected) selectedColor else idleColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterOverflowSheet(
    groups: List<TodoGroup>,
    todos: List<Todo>,
    pinned: List<String>?,
    backdrop: Backdrop,
    onPinned: (List<String>) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = studyColors()
    val exposed = exposedGroups(groups, pinned)
    val exposedIds = exposed.map { it.id }
    val hidden = groups.filter { it.id !in exposedIds }
    val showUngrouped = todos.any { it.groupId == null }
    var hint by remember { mutableStateOf<String?>(null) }
    fun write(next: List<String>) {
        hint = null
        onPinned(next.take(Prefs.TODO_BAR_LIMIT))
    }
    Box(Modifier.fillMaxSize().testTag("filter-overflow")) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp)
                .liquidGlass(backdrop, squircle(22.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                .clickable(interactionSource = null, indication = null) {},
        ) {
            Text(
                "更多分组",
                color = colors.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            )
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (showUngrouped) {
                    OverflowPickRow("未分组", null, colors) { onPick(FILTER_NONE) }
                }
                hidden.forEach { group ->
                    OverflowPickRow(group.name, colors.dot(group.color), colors) { onPick(group.id) }
                }
                if (!showUngrouped && hidden.isEmpty()) {
                    Text(
                        "没有收起来的分组。",
                        color = colors.secondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                Text(
                    "外露在底栏",
                    color = colors.secondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                )
                Text(
                    "全部始终在最左。底栏最多再外露 ${Prefs.TODO_BAR_LIMIT} 个。",
                    color = colors.tertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
                )
                if (hint != null) {
                    Text(hint.orEmpty(), color = colors.red, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp))
                }
                groups.forEach { group ->
                    val on = group.id in exposedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clickable(interactionSource = null, indication = null) {
                                    val next = exposedIds.toMutableList()
                                    if (on) {
                                        next.remove(group.id)
                                        write(next)
                                    } else if (next.size >= Prefs.TODO_BAR_LIMIT) {
                                        hint = "底栏最多外露 ${Prefs.TODO_BAR_LIMIT} 个，先取消一个。"
                                    } else {
                                        next.add(group.id)
                                        write(next)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.dot(group.color)))
                            Spacer(Modifier.width(8.dp))
                            Text(group.name, color = colors.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            if (on) Icon(StudyIcons.Check, contentDescription = "已外露", tint = colors.blue, modifier = Modifier.size(18.dp))
                        }
                        if (on) {
                            val index = exposedIds.indexOf(group.id)
                            Icon(
                                StudyIcons.KeyboardArrowUp,
                                contentDescription = "上移",
                                tint = colors.tertiary,
                                modifier = Modifier.size(22.dp).clickable(interactionSource = null, indication = null) {
                                    if (index > 0) {
                                        val next = exposedIds.toMutableList()
                                        val item = next.removeAt(index)
                                        next.add(index - 1, item)
                                        write(next)
                                    }
                                },
                            )
                            Icon(
                                StudyIcons.KeyboardArrowDown,
                                contentDescription = "下移",
                                tint = colors.tertiary,
                                modifier = Modifier.size(22.dp).clickable(interactionSource = null, indication = null) {
                                    if (index >= 0 && index < exposedIds.lastIndex) {
                                        val next = exposedIds.toMutableList()
                                        val item = next.removeAt(index)
                                        next.add(index + 1, item)
                                        write(next)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Text(
                "取消",
                color = colors.blue,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun OverflowPickRow(label: String, dot: Color?, colors: StudyColors, onClick: () -> Unit) {
    val press = rememberLiquidPress(captureDrag = false)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { applyLiquidPress(press) }
            .liquidPressFeedback(press)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = colors.label, fontSize = 17.sp)
    }
}

@Composable
private fun TodoRow(
    modifier: Modifier = Modifier,
    todo: Todo,
    group: TodoGroup?,
    backdrop: Backdrop,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onOpenImage: (Int) -> Unit,
) {
    val colors = studyColors()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val reveal = with(density) { 84.dp.toPx() }
    val accent = group?.let { colors.dot(it.color) } ?: colors.blue
    var expanded by remember(todo.id) { mutableStateOf(false) }
    val press = rememberLiquidPress(captureDrag = false)
    val shift = offset.value
    Box(
        modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
            .fillMaxWidth(),
    ) {
        Box(Modifier.matchParentSize()) {
            if (shift > 2f) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(with(density) { shift.toDp() })
                        .fillMaxHeight()
                        .clip(squircle(26.dp))
                        .background(colors.green)
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text("完成", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (shift < -2f) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(with(density) { (-shift).toDp() })
                        .fillMaxHeight()
                        .clip(squircle(26.dp))
                        .background(colors.red)
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text("删除", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(
            Modifier
                .offset { IntOffset(shift.roundToInt(), 0) }
                .fillMaxWidth()
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
                .pointerInput(todo.id, reveal) {
                    val tracker = VelocityTracker()
                    var snap: Job? = null
                    var dragOffset = offset.value
                    fun settle(velocity: Float, after: () -> Unit = {}) {
                        val pending = snap
                        snap = null
                        scope.launch {
                            pending?.cancel()
                            pending?.join()
                            offset.animateTo(0f, Motion.smooth(), initialVelocity = velocity)
                            after()
                        }
                    }
                    detectHorizontalDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            dragOffset = offset.value
                        },
                        onHorizontalDrag = { change, delta ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            dragOffset = resistedDrag(dragOffset, delta, -reveal, reveal, reveal)
                            val next = dragOffset
                            snap?.cancel()
                            snap = scope.launch { offset.snapTo(next) }
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            val value = dragOffset
                            when {
                                velocity > Motion.Fling || (abs(velocity) <= Motion.Fling && value > reveal * 0.55f) -> settle(velocity, onToggle)
                                velocity < -Motion.Fling || (abs(velocity) <= Motion.Fling && value < -reveal * 0.55f) -> settle(velocity, onDelete)
                                else -> settle(velocity)
                            }
                        },
                        onDragCancel = { settle(0f) },
                    )
                }
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { expanded = !expanded },
                    onLongClick = onOpen,
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemindCircle(todo.done, accent, backdrop, onToggle)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        color = if (todo.done) colors.tertiary else colors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        textDecoration = if (todo.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildString {
                        append(if (todo.startDate == todo.endDate) compact(todo.startDate) else "${compact(todo.startDate)}–${compact(todo.endDate)}")
                        if (group != null) append(" · ").append(group.name)
                    }
                    Text(meta, color = colors.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            AnimatedVisibility(
                visible = expanded && (todo.note.isNotBlank() || todo.images.isNotEmpty()),
                enter = expandVertically(Motion.snappy()) + fadeIn(),
                exit = shrinkVertically(Motion.snappy()) + fadeOut(),
            ) {
                Column {
                    if (todo.note.isNotBlank()) {
                        Text(
                            todo.note,
                            color = if (todo.done) colors.tertiary else colors.secondary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 34.dp, top = 4.dp),
                        )
                    }
                    if (todo.images.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 34.dp)) {
                            todo.images.take(4).forEachIndexed { index, name ->
                                Thumb(DraftPhoto(name, storedName = name), todo.id, 44.dp) { onOpenImage(index) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindCircle(done: Boolean, accent: Color, backdrop: Backdrop, onToggle: () -> Unit) {
    val fill by animateFloatAsState(if (done) 1f else 0f, Motion.bouncy(), label = "fill")
    val scale by animateFloatAsState(if (done) 1f else 0.6f, Motion.bouncy(), label = "check")
    Box(
        Modifier
            .size(22.dp)
            .graphicsLayer { scaleX = 0.92f + 0.08f * fill; scaleY = 0.92f + 0.08f * fill }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default },
                onDrawSurface = {
                    if (fill > 0.04f) {
                        drawCircle(accent.copy(alpha = fill))
                    } else {
                        drawCircle(accent, style = Stroke(width = 1.6.dp.toPx()))
                    }
                },
            )
            .clickable(interactionSource = null, indication = null, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (!done) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color.Transparent))
        }
        if (fill > 0.2f) {
            Icon(
                StudyIcons.Check,
                contentDescription = "完成",
                tint = Color.White.copy(alpha = fill),
                modifier = Modifier.size(14.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

@Composable
private fun BoxScope.TodoEditorSheet(
    existing: Todo?,
    groups: List<TodoGroup>,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?, Int?, String?, List<DraftPhoto>) -> Unit,
    onPickImages: ((List<Uri>) -> Unit) -> Unit,
    onTakePhoto: ((File?) -> Unit) -> Unit,
) {
    val colors = studyColors()
    val today = PlanTime.today()
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var start by remember(existing?.id) {
        mutableStateOf(existing?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today)
    }
    var end by remember(existing?.id) {
        mutableStateOf(existing?.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start)
    }
    var remindOn by remember(existing?.id) { mutableStateOf(existing?.remindDate != null) }
    var remindDay by remember(existing?.id) {
        mutableStateOf(existing?.remindDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start)
    }
    var remindMinutes by remember(existing?.id) { mutableIntStateOf(existing?.remindMinutes ?: 9 * 60) }
    var groupId by remember(existing?.id) { mutableStateOf(existing?.groupId) }
    var photos by remember(existing?.id) {
        mutableStateOf(existing?.images.orEmpty().map { DraftPhoto(it, storedName = it) })
    }
    var openDate by remember(existing?.id) { mutableStateOf<DateTarget?>(null) }
    var openGroup by remember(existing?.id) { mutableStateOf(false) }
    var photoMenu by remember(existing?.id) { mutableStateOf(false) }
    var error by remember(existing?.id) { mutableStateOf<String?>(null) }
    val view = LocalView.current
    fun reject(message: String) {
        error = message
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
    fun dismiss() {
        photos.forEach { it.file?.delete() }
        onDismiss()
    }
    GlassSheet(
        backdrop = backdrop,
        onDismiss = { dismiss() },
        modifier = Modifier.testTag("todo-sheet"),
        header = {
            SheetHeader(
                title = if (existing == null) "添加待办" else "修改待办",
                confirmLabel = if (existing == null) "添加" else "完成",
                confirmEnabled = title.isNotBlank(),
                backdrop = backdrop,
                onClose = { dismiss() },
                onConfirm = {
                    if (title.isBlank()) {
                        reject("写上标题。")
                        return@SheetHeader
                    }
                    if (end < start) {
                        reject("结束日期不能早于开始。")
                        return@SheetHeader
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onSave(
                        title.trim(),
                        note.trim(),
                        start.toString(),
                        end.toString(),
                        if (remindOn) remindDay.toString() else null,
                        if (remindOn) remindMinutes else null,
                        groupId,
                        photos,
                    )
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
                    Text(error.orEmpty(), color = colors.red, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                GlassSection(backdrop) {
                    BorderlessField(title, "标题", colors) { title = it }
                    Hairline()
                    BorderlessField(note, "备注", colors) { note = it }
                }
                Spacer(Modifier.height(16.dp))
                GlassSection(backdrop) {
                    DateRow("开始", start, openDate == DateTarget.START, "date-start") {
                        openDate = if (openDate == DateTarget.START) null else DateTarget.START
                    }
                    if (openDate == DateTarget.START) InlineCalendar(start, "inline-calendar") { start = it }
                    Hairline()
                    DateRow("结束", end, openDate == DateTarget.END, "date-end") {
                        openDate = if (openDate == DateTarget.END) null else DateTarget.END
                    }
                    if (openDate == DateTarget.END) InlineCalendar(end, "inline-calendar-end") { end = it }
                }
                Spacer(Modifier.height(16.dp))
                GlassSection(backdrop) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("提醒", color = colors.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        LiquidSwitch(checked = remindOn, onCheckedChange = { remindOn = it }, backdrop = backdrop)
                    }
                    if (remindOn) {
                        Hairline()
                        DateRow("提醒日", remindDay, openDate == DateTarget.REMIND, "date-remind") {
                            openDate = if (openDate == DateTarget.REMIND) null else DateTarget.REMIND
                        }
                        if (openDate == DateTarget.REMIND) InlineCalendar(remindDay, "inline-calendar-remind") { remindDay = it }
                        Hairline()
                        TimeWheels(remindMinutes) { remindMinutes = it }
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassSection(backdrop) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .testTag("group-row")
                            .clickable(interactionSource = null, indication = null) { openGroup = !openGroup }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("分组", color = colors.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        val chosen = groups.firstOrNull { it.id == groupId }
                        if (chosen != null) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.dot(chosen.color)))
                            Spacer(Modifier.width(6.dp))
                            Text(chosen.name, color = colors.secondary, fontSize = 17.sp)
                        } else {
                            Text("未分组", color = colors.secondary, fontSize = 17.sp)
                        }
                    }
                    if (openGroup) {
                        Column(Modifier.testTag("group-picker")) {
                            GroupChoice(null, "未分组", null, groupId == null) { groupId = null }
                            groups.forEach { group ->
                                Hairline()
                                GroupChoice(group.id, group.name, colors.dot(group.color), groupId == group.id) {
                                    groupId = group.id
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassSection(backdrop) {
                    if (photos.isNotEmpty()) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            photos.forEach { photo ->
                                Box {
                                    Thumb(photo, existing?.id, 72.dp) {}
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .clickable(interactionSource = null, indication = null) {
                                                photo.file?.delete()
                                                photos = photos.filterNot { it.key == photo.key }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(StudyIcons.Close, contentDescription = "移除图片", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                        Hairline()
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = null, indication = null) { photoMenu = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(StudyIcons.Add, contentDescription = "添加图片", tint = colors.blue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加图片", color = colors.blue, fontSize = 17.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (photoMenu) {
            ActionSheet(backdrop, onDismiss = { photoMenu = false }) {
                ActionRow("相册", StudyIcons.PhotoLibrary, colors.blue) {
                    photoMenu = false
                    val room = TodoImages.MAX_COUNT - photos.size
                    if (room > 0) {
                        onPickImages { uris ->
                            photos = photos + uris.take(room).map { DraftPhoto(it.toString(), uri = it) }
                        }
                    } else {
                        reject("最多 ${TodoImages.MAX_COUNT} 张图片。")
                    }
                }
                Hairline()
                ActionRow("拍照", StudyIcons.PhotoCamera, colors.blue) {
                    photoMenu = false
                    if (photos.size < TodoImages.MAX_COUNT) {
                        onTakePhoto { file ->
                            if (file != null) photos = photos + DraftPhoto(file.absolutePath, file = file)
                        }
                    } else {
                        reject("最多 ${TodoImages.MAX_COUNT} 张图片。")
                    }
                }
                Spacer(Modifier.height(8.dp))
                ActionRow("取消", StudyIcons.Close, colors.label) { photoMenu = false }
            }
        }
}

@Composable
private fun GroupChoice(
    id: String?,
    name: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = studyColors()
    val press = rememberLiquidPress(captureDrag = false)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { applyLiquidPress(press) }
            .liquidPressFeedback(press)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupDot(color ?: colors.tertiary, if (id == null) null else name.take(1))
        Spacer(Modifier.width(12.dp))
        Text(name, color = colors.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(StudyIcons.Check, contentDescription = "已选", tint = colors.blue, modifier = Modifier.size(18.dp))
        if (id == null) Spacer(Modifier.width(0.dp))
    }
}

@Composable
private fun GroupsPage(
    groups: List<TodoGroup>,
    todos: List<Todo>,
    backdrop: Backdrop,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: (String) -> Boolean,
    onRename: (String, String) -> Boolean,
    onRecolor: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onDelete: (String, Boolean) -> Unit,
) {
    val colors = studyColors()
    var name by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<TodoGroup?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "完成",
                color = colors.blue,
                fontSize = 17.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null, onClick = onBack).padding(8.dp),
            )
        }
        Text("分组", color = colors.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .liquidGlass(backdrop, squircle(16.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp),
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) Hairline()
                val press = rememberLiquidPress(captureDrag = false)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { applyLiquidPress(press) }
                        .liquidPressFeedback(press)
                        .clickable(interactionSource = null, indication = null) { onOpen(group.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.clickable(interactionSource = null, indication = null) { onRecolor(group.id) }) {
                        GroupDot(colors.dot(group.color), group.name.take(1))
                    }
                    Spacer(Modifier.width(12.dp))
                    if (renaming == group.id) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it.take(24) },
                            textStyle = TextStyle(color = colors.label, fontSize = 17.sp),
                            cursorBrush = SolidColor(colors.blue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (onRename(group.id, draft)) renaming = null
                            }),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            group.name,
                            color = colors.label,
                            fontSize = 17.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(interactionSource = null, indication = null) {
                                    draft = group.name
                                    renaming = group.id
                                },
                        )
                    }
                    Text("${todos.count { it.groupId == group.id }}", color = colors.secondary, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        StudyIcons.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = colors.tertiary,
                        modifier = Modifier.size(22.dp).clickable(interactionSource = null, indication = null) { onMove(group.id, -1) },
                    )
                    Icon(
                        StudyIcons.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = colors.tertiary,
                        modifier = Modifier.size(22.dp).clickable(interactionSource = null, indication = null) { onMove(group.id, 1) },
                    )
                    Icon(
                        StudyIcons.Delete,
                        contentDescription = "删除分组",
                        tint = colors.red,
                        modifier = Modifier.size(20.dp).clickable(interactionSource = null, indication = null) { confirm = group },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassSection(backdrop) {
            BorderlessField(name, "新分组", colors) { name = it }
        }
        Text(
            "添加分组",
            color = colors.blue,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clickable(interactionSource = null, indication = null) {
                    if (onAdd(name)) name = ""
                },
        )
        Text(
            "点颜色圆点换颜色。点名字可以改名，点这一行进入这一组。",
            color = colors.secondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
    val deleting = confirm
    if (deleting != null) {
        ActionSheet(backdrop, onDismiss = { confirm = null }) {
            ActionRow("移到未分组", StudyIcons.Check, colors.blue) {
                confirm = null
                onDelete(deleting.id, false)
            }
            Hairline()
            ActionRow("连待办一起删除", StudyIcons.Delete, colors.red) {
                confirm = null
                onDelete(deleting.id, true)
            }
            Spacer(Modifier.height(8.dp))
            ActionRow("取消", StudyIcons.Close, colors.label) { confirm = null }
        }
    }
    }
}

@Composable
private fun ActionSheet(backdrop: Backdrop, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val colors = studyColors()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable(interactionSource = null, indication = null, onClick = onDismiss))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp)
                .fillMaxWidth()
                .liquidGlass(backdrop, squircle(16.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                .clickable(interactionSource = null, indication = null) {},
        ) { content() }
    }
}

@Composable
private fun ActionRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    val press = rememberLiquidPress()
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { applyLiquidPress(press) }
            .liquidPressFeedback(press)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, fontSize = 17.sp)
    }
}

@Composable
private fun Thumb(photo: DraftPhoto, todoId: String?, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(photo.key, todoId) {
        when {
            photo.storedName != null && todoId != null ->
                TodoImages.thumb(TodoImages.file(context.filesDir, todoId, photo.storedName), 256)
            photo.uri != null -> TodoImages.thumb(context, photo.uri, 256)
            photo.file != null -> TodoImages.thumb(photo.file, 256)
            else -> null
        }?.asImageBitmap()
    }
    Box(
        Modifier
            .size(size)
            .clip(squircle(10.dp))
            .background(studyColors().track)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "待办图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun BoxScope.PhotoViewer(
    photos: List<DraftPhoto>,
    start: Int,
    todoId: String,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var index by remember { mutableIntStateOf(start) }
    val photo = photos.getOrNull(index) ?: return
    var zoom by remember(photo.key) { mutableFloatStateOf(1f) }
    var pan by remember(photo.key) { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { _, zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 4f)
        pan = if (zoom <= 1.02f) Offset.Zero else pan + panChange
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
        val context = LocalContext.current
        val bitmap = remember(photo.key) {
            photo.storedName?.let { TodoImages.thumb(TodoImages.file(context.filesDir, todoId, it), 1600)?.asImageBitmap() }
        }
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = "待办图片",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x
                        translationY = pan.y
                    }
                    .transformable(transform)
                    .pointerInput(photo.key, photos.size, index) {
                        var accum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accum = 0f },
                            onHorizontalDrag = { _, delta ->
                                if (zoom > 1.05f) return@detectHorizontalDragGestures
                                accum += delta
                                val step = 72.dp.toPx()
                                if (accum > step && index > 0) {
                                    index -= 1
                                    accum = 0f
                                } else if (accum < -step && index < photos.lastIndex) {
                                    index += 1
                                    accum = 0f
                                }
                            },
                        )
                    },
                contentScale = ContentScale.Fit,
            )
        }
        GlassIconButton(
            onClose,
            backdrop,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
        ) { Icon(StudyIcons.Close, contentDescription = "关闭", tint = studyColors().label) }
        if (photo.storedName != null) {
            GlassIconButton(
                { onRemove(photo.storedName) },
                backdrop,
                modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
            ) { Icon(StudyIcons.Delete, contentDescription = "移除图片", tint = studyColors().red) }
        }
    }
}

@Composable
private fun GroupDot(color: Color, glyph: String?) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (!glyph.isNullOrEmpty()) {
            Text(glyph, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun compact(iso: String): String {
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    return "${date.monthValue}/${date.dayOfMonth}"
}
