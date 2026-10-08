package com.partner.studyreminder.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Segmented track from the AndroidLiquidGlass catalog `LiquidBottomTabs`.
 * The selection capsule is a translucent wash, not an opaque white plate,
 * so labels stay dark in light mode and light in dark mode.
 */
internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 52.dp,
    /** Bump this to pull the thumb back onto [selectedTabIndex] after a menu closes. */
    settleKey: Int = 0,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = studyColors()
    val isLightTheme = colors.label.red < 0.5f
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)
    val count = tabsCount.coerceAtLeast(1)
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val tabWidthPx = remember { floatArrayOf(1f) }
    val animationScope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val offsetAnimation = remember { Animatable(0f) }
    var currentIndex by remember(count) { mutableIntStateOf(selectedTabIndex().coerceIn(0, count - 1)) }
    val indexHolder = remember { intArrayOf(currentIndex) }
    indexHolder[0] = currentIndex
    val dampedDragAnimation = remember(animationScope, count) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedTabIndex().toFloat().coerceIn(0f, (count - 1).toFloat()),
            valueRange = 0f..(count - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> },
        )
    }
    LaunchedEffect(count) {
        snapshotFlow { selectedTabIndex().coerceIn(0, count - 1) }
            .collectLatest { index -> currentIndex = index }
    }
    LaunchedEffect(settleKey) {
        if (settleKey == 0) return@LaunchedEffect
        val index = selectedTabIndex().coerceIn(0, count - 1)
        currentIndex = index
        dampedDragAnimation.slideTo(index.toFloat())
        offsetAnimation.snapTo(0f)
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDragAnimation.slideTo(index.toFloat())
                onTabSelected(index)
            }
    }

    BoxWithConstraints(
        modifier.height(barHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / count
        }
        tabWidthPx[0] = tabWidth
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.toFloat().coerceAtLeast(1f)).fastCoerceIn(-1f, 1f)
                with(density) { 4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        val shape = Capsule()
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .clip(shape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width.coerceAtLeast(1f), progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .height(barHeight)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {}
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides { 1f },
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx() * progress, 24f.dp.toPx() * progress)
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .height(barHeight - 8.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer { colorFilter = ColorFilter.tint(accentColor) },
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        dampedDragAnimation.value * tabWidth + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    }
                }
                .clip(shape)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { shape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8f.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    },
                )
                .height(barHeight - 8.dp)
                .fillMaxWidth(1f / count),
        )
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .testTag("segmented-filters")
                .pointerInput(count, isLtr, touchSlop) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var past = false
                        var totalX = 0f
                        var totalY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                if (past) {
                                    val targetIndex = dampedDragAnimation.targetValue
                                        .fastRoundToInt()
                                        .fastCoerceIn(0, count - 1)
                                    dampedDragAnimation.release()
                                    if (indexHolder[0] != targetIndex) {
                                        currentIndex = targetIndex
                                    } else {
                                        dampedDragAnimation.slideTo(targetIndex.toFloat())
                                    }
                                    animationScope.launch {
                                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                                    }
                                }
                                break
                            }
                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y
                            if (!past && (abs(totalX) > touchSlop || abs(totalY) > touchSlop)) {
                                if (abs(totalY) > abs(totalX)) break
                                past = true
                                dampedDragAnimation.press()
                            }
                            if (past) {
                                change.consume()
                                val width = tabWidthPx[0].coerceAtLeast(1f)
                                dampedDragAnimation.updateValue(
                                    (dampedDragAnimation.targetValue + delta.x / width * if (isLtr) 1f else -1f)
                                        .fastCoerceIn(0f, (count - 1).toFloat()),
                                )
                                animationScope.launch {
                                    offsetAnimation.snapTo(offsetAnimation.value + delta.x)
                                }
                            }
                        }
                    }
                }
                .height(barHeight)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val next = scale()
                scaleX = next
                scaleY = next
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
