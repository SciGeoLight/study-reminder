package com.partner.studyreminder.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.UnevenRoundedRectangle
import com.partner.studyreminder.ui.Motion
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.ui.resistedDrag
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Full-screen sheet under the status bar. The plan editor and the todo editor
 * share this shell; neither defaults to a half-height detent.
 */
@Composable
fun BoxScope.GlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = studyColors()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shape = UnevenRoundedRectangle(36.dp, 36.dp, 0.dp, 0.dp, RoundedCornerStyle.Continuous)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetHeight = maxHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val sheetPx = with(density) { sheetHeight.toPx() }
        val offset = remember(sheetPx) { Animatable(0f) }
        val scrim = ((sheetPx - offset.value) / sheetPx).coerceIn(0f, 1f) * 0.34f
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        )
        Column(
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .imePadding()
                .glass(backdrop, GlassTier.Float, shape, surface = colors.glass)
                .pointerInput(Unit) {
                    // Hit-target for empty glass so the scrim does not dismiss the sheet.
                    // A clickable here would merge descendant semantics and hide picker tags.
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            event.changes.forEach { change ->
                                if (!change.isConsumed) change.consume()
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("sheet-handle")
                    .pointerInput(sheetPx) {
                        val tracker = VelocityTracker()
                        var dragOffset = offset.value
                        var snap: Job? = null
                        fun settle(current: Float, velocity: Float) {
                            val draggedDown = current > with(density) { 96.dp.toPx() }
                            val target = if (velocity > Motion.Fling || draggedDown) sheetPx else 0f
                            val pending = snap
                            snap = null
                            scope.launch {
                                pending?.cancel()
                                pending?.join()
                                offset.animateTo(target, Motion.snappy(), initialVelocity = velocity)
                                if (target >= sheetPx - 1f) onDismiss()
                            }
                        }
                        detectVerticalDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                dragOffset = offset.value
                            },
                            onVerticalDrag = { change, drag ->
                                tracker.addPosition(change.uptimeMillis, change.position)
                                dragOffset = resistedDrag(dragOffset, drag, 0f, sheetPx, sheetPx)
                                val next = dragOffset
                                snap?.cancel()
                                snap = scope.launch { offset.snapTo(next) }
                            },
                            onDragEnd = { settle(dragOffset, tracker.calculateVelocity().y) },
                            onDragCancel = { settle(dragOffset, 0f) },
                        )
                    }
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .glass(
                            backdrop,
                            GlassTier.Control,
                            Capsule(),
                            surface = colors.glass,
                            refraction = 8.dp,
                            chromatic = false,
                        ),
                )
            }
            header()
            content()
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = studyColors()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        GlassIconButton(
            onClick = onClose,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(StudyIcons.Close, contentDescription = "关闭", tint = colors.label)
        }
        Text(
            text = title,
            color = colors.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 108.dp),
        )
        Text(
            text = confirmLabel,
            color = if (confirmEnabled) Color.White else colors.tertiary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(Capsule())
                .background(if (confirmEnabled) colors.blue else colors.track)
                .then(
                    if (confirmEnabled) {
                        Modifier.clickable(interactionSource = null, indication = null, onClick = onConfirm)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
