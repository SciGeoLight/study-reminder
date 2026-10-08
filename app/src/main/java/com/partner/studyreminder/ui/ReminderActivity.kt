package com.partner.studyreminder.ui

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.partner.studyreminder.alarm.AlarmActions
import com.partner.studyreminder.alarm.AlarmContract
import com.partner.studyreminder.alarm.NotificationHelper
import com.partner.studyreminder.parse.PlanTime
import com.partner.studyreminder.ui.glass.GlassTier
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.glass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        edgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = AudioManager.STREAM_ALARM
        edgeToEdge()

        val title = intent.getStringExtra(AlarmContract.EXTRA_TITLE) ?: "学习提醒"
        val note = intent.getStringExtra(AlarmContract.EXTRA_NOTE).orEmpty()
        val kind = intent.getStringExtra(AlarmContract.EXTRA_KIND) ?: AlarmContract.KIND_START
        val whenLabel = intent.getStringExtra(AlarmContract.EXTRA_WHEN).orEmpty().ifBlank { "到点了" }
        val headline = NotificationHelper.headline(kind, title)
        val body = note.ifBlank { "开始这一段。" }

        setContent {
            StudyTheme {
                AlarmScreen(
                    whenLabel = whenLabel,
                    headline = headline,
                    note = body,
                    onDismiss = {
                        AlarmActions.stop(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmActions.snooze(this, title, note)
                        toast("5 分钟后再提醒", long = false)
                        finish()
                    },
                )
            }
        }
    }
}

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun AlarmScreen(
    whenLabel: String,
    headline: String,
    note: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val colors = studyColors()
    val clock = remember(whenLabel) {
        Regex("(\\d{2}:\\d{2})").find(whenLabel)?.groupValues?.get(1)
            ?: PlanTime.formatMinutes(PlanTime.nowMinutes())
    }
    LiquidPage { backdrop ->
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(backdrop, GlassTier.Card, squircle(32.dp))
                    .padding(horizontal = 18.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("学习提醒", color = colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(148.dp), contentAlignment = Alignment.Center) {
                    if (rememberSystemAnimationsEnabled()) {
                        ExpandingRings(Modifier.fillMaxSize())
                    }
                    Text(
                        text = clock,
                        color = colors.label,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        style = Tabular,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = headline,
                    color = colors.label,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note,
                    color = colors.secondary,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
                if (whenLabel.isNotBlank() && whenLabel != clock) {
                    Spacer(Modifier.height(8.dp))
                    Text(whenLabel, color = colors.tertiary, fontSize = 15.sp, style = Tabular)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .glass(backdrop, GlassTier.Control, Capsule())
                    .clickable(interactionSource = null, indication = null, onClick = onSnooze),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("5 分钟后再提醒", color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            SlideToDismiss(backdrop, onDismiss)
        }
    }
}

@Composable
private fun SlideToDismiss(backdrop: com.kyant.backdrop.Backdrop, onDismiss: () -> Unit) {
    val colors = studyColors()
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    var stretch by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .glass(backdrop, GlassTier.Control, Capsule())
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                var snap: Job? = null
                var dragOffset = drag.value
                fun maxTravel(): Float = (width - 72.dp.toPx()).coerceAtLeast(0f)
                fun settle(velocity: Float) {
                    val max = maxTravel()
                    val strongBack = velocity < -Motion.Fling
                    val dismiss = !strongBack && (velocity > Motion.Fling || dragOffset > width * 0.72f)
                    val target = if (dismiss) max else 0f
                    val pending = snap
                    snap = null
                    scope.launch {
                        pending?.cancel()
                        pending?.join()
                        drag.animateTo(target, Motion.snappy(), initialVelocity = velocity)
                        if (dismiss) onDismiss()
                    }
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                        dragOffset = drag.value
                    },
                    onDragEnd = {
                        stretch = 0f
                        settle(tracker.calculateVelocity().x)
                    },
                    onDragCancel = {
                        stretch = 0f
                        settle(0f)
                    },
                    onHorizontalDrag = { change, delta ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        stretch = (abs(delta) / 28f).coerceIn(0f, 1f)
                        val max = maxTravel()
                        dragOffset = resistedDrag(dragOffset, delta, 0f, max, max)
                        val next = dragOffset
                        snap?.cancel()
                        snap = scope.launch { drag.snapTo(next) }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "滑动关闭",
            color = colors.secondary,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            Modifier
                .offset { IntOffset(drag.value.roundToInt(), 0) }
                .padding(6.dp)
                .size(60.dp)
                .graphicsLayer {
                    scaleX = 1f + stretch * 0.28f
                    scaleY = 1f - stretch * 0.1f
                }
                .glass(backdrop, GlassTier.Control, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(StudyIcons.ChevronRight, contentDescription = "滑动关闭", tint = colors.label)
        }
    }
}

@Composable
private fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            val resolver = context.contentResolver
            systemAnimationsEnabled(
                Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
                Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f),
            )
        }.getOrDefault(true)
    }
}

@Composable
private fun ExpandingRings(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
        ),
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val stroke = Stroke(width = 2.dp.toPx())
        repeat(2) { index ->
            val shifted = phase + index * 0.5f
            val progress = if (shifted >= 1f) shifted - 1f else shifted
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = maxRadius * (0.28f + 0.72f * progress),
                center = center,
                style = stroke,
            )
        }
    }
}
