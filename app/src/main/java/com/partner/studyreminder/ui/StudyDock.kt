package com.partner.studyreminder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.R
import com.partner.studyreminder.data.TodoGroup
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassTier
import com.partner.studyreminder.ui.glass.glass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.icons.StudyIcons

/** Shared bottom stack: group slider, glass plus, then the action capsule. */
internal object StudyDockMetrics {
    val slider: Dp = 52.dp
    val actions: Dp = 56.dp
    val gap: Dp = 8.dp
    val bottom: Dp = 12.dp
    val listGap: Dp = 16.dp
    val listPadding: Dp = bottom + slider + gap + actions + listGap
    val undoBottom: Dp = bottom + slider + gap + actions + 8.dp
    val menuBottom: Dp = bottom + slider + gap + actions + 8.dp
}

@Composable
internal fun StudyBottomChrome(
    backdrop: Backdrop,
    groups: List<TodoGroup>,
    filter: String,
    pinned: List<String>?,
    settleKey: Int,
    onFilter: (String) -> Unit,
    onMoreGroups: () -> Unit,
    onAdd: () -> Unit,
    addContentDescription: String,
    onTodos: () -> Unit,
    onImport: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    addTag: String? = null,
) {
    val colors = studyColors()
    Column(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = StudyDockMetrics.bottom),
        verticalArrangement = Arrangement.spacedBy(StudyDockMetrics.gap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StudyDockMetrics.gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentedFilters(
                groups = groups,
                filter = filter,
                pinned = pinned,
                settleKey = settleKey,
                backdrop = backdrop,
                modifier = Modifier.weight(1f),
                onMore = onMoreGroups,
                onFilter = onFilter,
            )
            GlassIconButton(
                onClick = onAdd,
                backdrop = backdrop,
                modifier = if (addTag != null) Modifier.testTag(addTag) else Modifier,
                buttonSize = StudyDockMetrics.slider,
            ) {
                Icon(StudyIcons.Add, contentDescription = addContentDescription, tint = colors.label)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(StudyDockMetrics.actions)
                .glass(backdrop, GlassTier.Float, Capsule()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockSlot(StudyIcons.Checklist, stringResource(R.string.menu_todos), onClick = onTodos)
            DockSlot(
                icon = StudyIcons.ContentPaste,
                label = "导入",
                contentDescription = stringResource(R.string.import_clipboard),
                onClick = onImport,
            )
            DockSlot(StudyIcons.MoreHoriz, "更多", onClick = onMore)
        }
    }
}

@Composable
internal fun BoxScope.StudyMoreMenu(
    visible: Boolean,
    backdrop: Backdrop,
    onOpenFile: () -> Unit,
    onTestAlarm: () -> Unit,
    onClearDay: () -> Unit,
    onPermissions: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = studyColors()
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = fadeIn(Motion.snappy()) + slideInVertically(Motion.snappy()) { it / 3 },
        exit = fadeOut(Motion.snappy()) + slideOutVertically(Motion.snappy()) { it / 3 },
    ) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp, bottom = StudyDockMetrics.menuBottom)
                .width(240.dp)
                .glass(backdrop, GlassTier.Float, squircle(22.dp), surface = colors.glass)
                .padding(vertical = 6.dp),
        ) {
            DockMenuRow(StudyIcons.FolderOpen, stringResource(R.string.open_file), colors.label, onOpenFile)
            DockMenuDivider(colors.separator)
            DockMenuRow(StudyIcons.Alarm, stringResource(R.string.test_alarm), colors.label, onTestAlarm)
            DockMenuDivider(colors.separator)
            DockMenuRow(StudyIcons.Delete, stringResource(R.string.clear_day), colors.red, onClearDay)
            DockMenuDivider(colors.separator)
            DockMenuRow(StudyIcons.Notifications, stringResource(R.string.menu_permissions), colors.label, onPermissions)
            DockMenuDivider(colors.separator)
            DockMenuRow(StudyIcons.Settings, stringResource(R.string.menu_settings), colors.label, onSettings)
        }
    }
}

@Composable
private fun RowScope.DockSlot(
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
        Text(label, color = colors.label, fontSize = 11.sp)
    }
}

@Composable
private fun DockMenuRow(
    icon: ImageVector,
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
internal fun GlassStatChip(
    backdrop: Backdrop,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = studyColors()
    Row(
        Modifier
            .height(32.dp)
            .glass(backdrop, GlassTier.Control, Capsule(), surface = colors.glass)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun DockMenuDivider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(0.5.dp)
            .background(color),
    )
}
