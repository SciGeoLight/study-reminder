package com.partner.studyreminder.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.partner.studyreminder.data.Backgrounds
import com.kyant.backdrop.Backdrop
import com.partner.studyreminder.ui.theme.ThemeMode
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
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
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

fun squircle(radius: Dp): Shape = RoundedRectangle(radius, RoundedCornerStyle.Continuous)

val CardShape: Shape = squircle(26.dp)
val GroupShape: Shape = squircle(28.dp)

@Immutable
data class StudyColors(
    val label: Color,
    val secondary: Color,
    val tertiary: Color,
    val blue: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val glass: Color,
    val glassStrong: Color,
    val card: Color,
    val separator: Color,
    val track: Color,
    val base: List<Color>,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color,
    val orbD: Color,
    val veil: Color,
)

private val LightColors = StudyColors(
    label = Color(0xFF1C1C1E),
    secondary = Color(0xFF3C3C43).copy(alpha = 0.72f),
    tertiary = Color(0xFF3C3C43).copy(alpha = 0.45f),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    orange = Color(0xFFFF9F0A),
    red = Color(0xFFFF3B30),
    glass = Color.White.copy(alpha = 0.18f),
    glassStrong = Color.White.copy(alpha = 0.42f),
    card = Color.White.copy(alpha = 0.30f),
    separator = Color.Black.copy(alpha = 0.08f),
    track = Color(0xFF787880).copy(alpha = 0.22f),
    base = listOf(Color(0xFFB9DCFF), Color(0xFFE7D4FF), Color(0xFFFFE0EC), Color(0xFFC8F3E4)),
    orbA = Color(0xFF5EB0FF).copy(alpha = 0.95f),
    orbB = Color(0xFFC9A6FF).copy(alpha = 0.85f),
    orbC = Color(0xFFFF8FB8).copy(alpha = 0.55f),
    orbD = Color(0xFF5EE0C2).copy(alpha = 0.75f),
    veil = Color.White.copy(alpha = 0.34f),
)

private val DarkColors = StudyColors(
    label = Color.White,
    secondary = Color.White.copy(alpha = 0.72f),
    tertiary = Color.White.copy(alpha = 0.45f),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    orange = Color(0xFFFFD60A),
    red = Color(0xFFFF453A),
    glass = Color.White.copy(alpha = 0.08f),
    glassStrong = Color.White.copy(alpha = 0.16f),
    card = Color.White.copy(alpha = 0.12f),
    separator = Color.White.copy(alpha = 0.12f),
    track = Color(0xFF787880).copy(alpha = 0.36f),
    base = listOf(Color(0xFF0B1020), Color(0xFF1A1440), Color(0xFF123044), Color(0xFF0E1A28)),
    orbA = Color(0xFF3D7DFF).copy(alpha = 0.85f),
    orbB = Color(0xFF9B4DFF).copy(alpha = 0.70f),
    orbC = Color(0xFFFF4D8D).copy(alpha = 0.45f),
    orbD = Color(0xFF1EC8B0).copy(alpha = 0.55f),
    veil = Color.Black.copy(alpha = 0.42f),
)

@Composable
internal inline fun studyColors(): StudyColors {
    val context = LocalContext.current
    ThemeMode.changes()
    return if (ThemeMode.isDark(context, isSystemInDarkTheme())) DarkColors else LightColors
}

val LocalBackdropImage = staticCompositionLocalOf<ImageBitmap?> { null }

/**
 * Process-wide background. Read it in the caller (`val image by rememberSharedBackground()`)
 * so a decode that finishes later recomposes that caller.
 * [revisionHint] lets an Activity stamp force a reload in addition to [Backgrounds.changes].
 */
@Composable
fun rememberSharedBackground(revisionHint: Long = 0L): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val revision = revisionHint xor Backgrounds.changes() xor Backgrounds.stamp(context)
    val image = remember { mutableStateOf(Backgrounds.peek(context)) }
    LaunchedEffect(revision) {
        val hit = Backgrounds.peek(context)
        if (hit != null || Backgrounds.stamp(context) == 0L) {
            image.value = hit
            return@LaunchedEffect
        }
        image.value = withContext(Dispatchers.IO) { Backgrounds.load(context) }
    }
    return image
}

@Composable
fun LiquidPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Backdrop) -> Unit,
) {
    val colors = studyColors()
    val image by rememberSharedBackground()
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            val photo = image
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(colors.veil))
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawStudyBackdrop(colors, size.width, size.height, null)
                }
            }
        }
        CompositionLocalProvider(LocalBackdropImage provides image) {
            content(backdrop)
        }
    }
}

/** Page backdrop in full-screen coordinates, so a top scrim can match a custom photo. */
internal fun DrawScope.drawStudyBackdrop(
    colors: StudyColors,
    width: Float,
    height: Float,
    image: ImageBitmap? = null,
) {
    if (image != null && image.width > 0 && image.height > 0) {
        val scale = maxOf(width / image.width, height / image.height)
        val dw = image.width * scale
        val dh = image.height * scale
        drawImage(
            image = image,
            dstOffset = IntOffset(
                ((width - dw) / 2f).toInt(),
                ((height - dh) / 2f).toInt(),
            ),
            dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Low,
        )
        drawRect(colors.veil, size = Size(width, height))
        return
    }
    drawRect(
        brush = Brush.linearGradient(
            colors = colors.base,
            start = Offset.Zero,
            end = Offset(width, height),
        ),
        size = Size(width, height),
    )
    drawStudyOrbs(colors, width, height)
}

private fun DrawScope.drawStudyOrbs(colors: StudyColors, width: Float, height: Float) {
    fun glow(color: Color, cx: Float, cy: Float, radius: Float) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(cx, cy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(cx, cy),
        )
    }
    val minDim = minOf(width, height)
    glow(colors.orbA, width * 0.08f, height * 0.08f, minDim * 0.72f)
    glow(colors.orbB, width * 0.95f, height * 0.16f, minDim * 0.62f)
    glow(colors.orbC, width * 0.78f, height * 0.72f, minDim * 0.55f)
    glow(colors.orbD, width * 0.12f, height * 0.88f, minDim * 0.58f)
}

/**
 * A panel in the same window as [LiquidPage], so the lens can sample the mesh.
 * A separate dialog window cannot see that layer.
 */
@Composable
fun BoxScope.GlassOverlay(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = studyColors()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.16f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
    )
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .imePadding()
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
            .clickable(interactionSource = null, indication = null, onClick = {})
            .padding(horizontal = 20.dp, vertical = 18.dp),
        content = content,
    )
}

/**
 * Old entry point. Refraction and blur go through [glass] on [GlassTier.Control].
 * [clampLens] is ignored: it used to pin every caller to a 12dp / 24dp lens and a 2dp blur.
 */
@Deprecated(
    message = "Use glass(). Refraction and blur are no longer forced to 24.dp and 2.dp.",
    replaceWith = ReplaceWith(
        "glass(backdrop, GlassTier.Control, shape, surface, press, refraction, blurRadius, chromatic)",
        "com.partner.studyreminder.ui.glass.GlassTier",
    ),
)
@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = Capsule(),
    surface: Color = Color.Unspecified,
    blurRadius: Dp = 2.dp,
    refraction: Dp = 24.dp,
    chromatic: Boolean = true,
    press: InteractiveHighlight? = null,
    clampLens: Boolean = true,
): Modifier = glass(
    backdrop = backdrop,
    tier = GlassTier.Control,
    shape = shape,
    surface = surface,
    press = press,
    refraction = refraction,
    blurRadius = blurRadius,
    chromatic = chromatic,
)

@Composable
fun rememberLiquidPress(captureDrag: Boolean = true): InteractiveHighlight {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope, captureDrag) { InteractiveHighlight(animationScope, captureDrag = captureDrag) }
}

fun Modifier.liquidPressFeedback(press: InteractiveHighlight): Modifier =
    this.then(press.modifier).then(press.gestureModifier)

/** Stretch math from the catalog `LiquidButton`, applied in `layerBlock` so the backdrop stays put. */
internal fun GraphicsLayerScope.applyLiquidPress(highlight: InteractiveHighlight) {
    if (size.minDimension <= 0f || size.height <= 0f || size.width <= 0f) return
    val progress = highlight.pressProgress
    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
    val maxOffset = size.minDimension
    val offset = highlight.offset
    if (maxOffset > 0f) {
        translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
        translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
    }
    val maxDragScale = 4f.dp.toPx() / size.height
    val offsetAngle = atan2(offset.y, offset.x)
    val width = size.width
    val height = size.height
    scaleX = scale +
        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
        (width / height).fastCoerceAtMost(1f)
    scaleY = scale +
        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
        (height / width).fastCoerceAtMost(1f)
}

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val press = rememberLiquidPress()
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 12.dp, alpha = 0.08f) },
                layerBlock = { applyLiquidPress(press) },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.78f))
                    }
                    if (surface.isSpecified) drawRect(surface)
                },
            )
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .liquidPressFeedback(press)
            .height(height)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val colors = studyColors()
    val density = LocalDensity.current
    val dragWidth = with(density) { 24.dp.toPx() }
    val scope = rememberCoroutineScope()
    val checkedNow = rememberUpdatedState(checked)
    val onChangeNow = rememberUpdatedState(onCheckedChange)
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val drag = remember(scope, dragWidth) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = { didDrag = false },
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onChangeNow.value(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (checkedNow.value) 0f else 1f
                    onChangeNow.value(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (dragAmount.x != 0f) didDrag = true
                fraction = (fraction + dragAmount.x / dragWidth).fastCoerceIn(0f, 1f)
            },
        )
    }
    LaunchedEffect(drag) {
        snapshotFlow { fraction }.collectLatest { next ->
            if (drag.targetValue != next) drag.updateValue(next)
        }
    }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            drag.animateToValue(target)
        }
    }
    val trackBackdrop = rememberLayerBackdrop()
    val thumbBackdrop = rememberCombinedBackdrop(
        backdrop,
        rememberBackdrop(trackBackdrop) { drawBackdrop ->
            val progress = drag.pressProgress
            val scaleX = lerp(2f / 3f, 0.75f, progress)
            val scaleY = lerp(0f, 0.75f, progress)
            scale(scaleX, scaleY) { drawBackdrop() }
        },
    )
    val paint = drag.value
    val track = androidx.compose.ui.graphics.lerp(colors.track, colors.green, paint)
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .background(track.copy(alpha = 0.72f))
                .size(56.dp, 32.dp),
        )
        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = lerp(padding, padding + dragWidth, paint)
                }
                .semantics { role = Role.Switch }
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = thumbBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = drag.pressProgress
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(
                            lerp(12f.dp.toPx(), 16f.dp.toPx(), progress),
                            lerp(24f.dp.toPx(), 32f.dp.toPx(), progress),
                            chromaticAberration = false,
                        )
                    },
                    highlight = {
                        val pressed = drag.pressProgress
                        if (pressed < 0.02f) Highlight.Default else Highlight.Ambient.copy(alpha = pressed)
                    },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f)) },
                    innerShadow = {
                        val progress = drag.pressProgress
                        InnerShadow(radius = 4f.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val velocity = drag.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = lerp(0.92f, 0.4f, drag.pressProgress)))
                    },
                )
                .size(28.dp),
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = studyColors()
    val press = rememberLiquidPress()
    Box(
        modifier
            .size(buttonSize)
            .liquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                surface = colors.glass,
                blurRadius = 2.dp,
                refraction = 24.dp,
                press = press,
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .liquidPressFeedback(press),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Glass thumb that swells and stretches while it is dragged.
 * Blur stays thin so the track still reads as liquid, not frost.
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = studyColors()
    val scope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    val trackMetrics = remember { floatArrayOf(1f, 1f) }
    val drag = remember(scope, valueRange) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = value,
            valueRange = valueRange,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = { didDrag = false },
            onDragStopped = {
                if (didDrag) onValueChange(targetValue)
            },
            onDrag = { _, dragAmount ->
                if (dragAmount.x != 0f) didDrag = true
                val width = trackMetrics[0]
                val span = trackMetrics[1]
                val next = (targetValue + span * dragAmount.x / width).coerceIn(valueRange)
                updateValue(next)
                onValueChange(next)
            },
        )
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
        trackMetrics[0] = trackWidth
        trackMetrics[1] = span
        val fraction = drag.progress.fastCoerceIn(0f, 1f)
        LaunchedEffect(drag, value) {
            if (!didDrag && drag.targetValue != value) drag.updateValue(value)
        }
        fun valueAt(x: Float): Float {
            return (valueRange.start + span * (x / trackWidth)).fastCoerceIn(valueRange.start, valueRange.endInclusive)
        }
        val trackBackdrop = rememberLayerBackdrop()
        val thumbBackdrop = rememberCombinedBackdrop(
            backdrop,
            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                val progress = drag.pressProgress
                val scaleX = lerp(2f / 3f, 1f, progress)
                val scaleY = lerp(0f, 1f, progress)
                scale(scaleX, scaleY) { drawBackdrop() }
            },
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .layerBackdrop(trackBackdrop)
                .fillMaxWidth()
                .height(6.dp),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(Capsule())
                    .background(colors.track)
                    .pointerInput(enabled, valueRange, trackWidth) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { position ->
                            val next = valueAt(position.x)
                            drag.animateToValue(next)
                            onValueChange(next)
                        }
                    },
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(Capsule())
                    .background(colors.blue),
            )
        }
        val thumbWidth = 40.dp
        Box(
            Modifier
                .offset {
                    val travel = (trackWidth - thumbWidth.toPx()).coerceAtLeast(0f)
                    IntOffset((travel * fraction).fastRoundToInt(), 0)
                }
                .size(thumbWidth, 24.dp)
                .drawBackdrop(
                    backdrop = thumbBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val pressed = drag.pressProgress
                        blur(2f.dp.toPx())
                        lens(
                            lerp(12f.dp.toPx(), 16f.dp.toPx(), pressed),
                            lerp(24f.dp.toPx(), 32f.dp.toPx(), pressed),
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val pressed = drag.pressProgress
                        if (pressed < 0.02f) Highlight.Default else Highlight.Ambient.copy(alpha = pressed)
                    },
                    shadow = { Shadow(radius = 8.dp, alpha = 0.08f) },
                    innerShadow = {
                        val pressed = drag.pressProgress
                        InnerShadow(radius = 4f.dp * pressed, alpha = pressed)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val velocity = drag.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = lerp(0.72f, 0.28f, drag.pressProgress)))
                    },
                )
                .then(if (enabled) drag.modifier else Modifier),
        )
    }
}
