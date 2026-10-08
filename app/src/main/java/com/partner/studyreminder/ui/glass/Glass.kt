package com.partner.studyreminder.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * One screen should keep lens surfaces at or below this count.
 * Lazy rows past the budget pass `lensEnabled = false` so they skip the
 * refraction shader. Backdrop 2.0.1 has no cheaper lens; `drawPlainBackdrop`
 * only drops highlight and shadow.
 */
const val GlassLensBudget = 8

/** Card white tint must stay at or below this. Not a user-facing "less glass" switch. */
const val CardWhiteTintMax = 0.25f

enum class GlassTier {
    /** Light lens, thin white, hairline. No vibrancy. */
    Card,

    /** Full refraction and dispersion. Press deepens the lens. */
    Control,

    /** [Control] plus a soft drop shadow. */
    Float,

    /** Weaker refraction, a thicker veil so text stays readable. */
    Panel,
}

/**
 * Resolved glass numbers. Callers pass `refraction` / `blur` through;
 * nothing here pins every tier to 24dp.
 *
 * Heights and amounts start from Backdrop 2.0.1:
 * `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)`
 * and the catalog `LiquidButton` (`blur(2.dp)`, `lens(12.dp, 24.dp)`).
 * Effect order is vibrancy, then blur, then lens.
 * The sheet tutorial's `lens(24.dp, 48.dp, depthEffect = true)` and 50% white
 * are not used for [GlassTier.Panel].
 */
internal data class GlassRecipe(
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val chromaticAberration: Boolean,
    val vibrancy: Boolean,
    val veil: Color,
    val hairline: Boolean,
    val plain: Boolean,
    val pressBoost: Boolean,
    val shadowRadius: Dp?,
    val shadowAlpha: Float,
    val shadowColor: Color,
)

internal fun glassRecipe(
    tier: GlassTier,
    isDark: Boolean,
    refraction: Dp? = null,
    blurRadius: Dp? = null,
    chromatic: Boolean? = null,
): GlassRecipe {
    val base = baseRecipe(tier, isDark)
    val amount = refraction ?: base.refractionAmount
    // The old unclamped lens used height = amount / 2. Keep that when a caller passes refraction.
    val height = if (refraction != null) refraction * 0.5f else base.refractionHeight
    return base.copy(
        blur = blurRadius ?: base.blur,
        refractionHeight = height,
        refractionAmount = amount,
        chromaticAberration = chromatic ?: base.chromaticAberration,
    )
}

/** Caps a near-white veil. Tinted pills (green, red) are left alone. */
internal fun Color.cappedCardWhite(maxAlpha: Float = CardWhiteTintMax): Color {
    if (!isSpecified) return this
    val neutral = red >= 0.95f && green >= 0.95f && blue >= 0.95f
    return if (neutral && alpha > maxAlpha) copy(alpha = maxAlpha) else this
}

private fun baseRecipe(tier: GlassTier, isDark: Boolean): GlassRecipe {
    val cardWhite = (if (isDark) 0.10f else 0.18f).coerceAtMost(CardWhiteTintMax)
    return when (tier) {
        GlassTier.Card -> GlassRecipe(
            blur = 2.dp,
            refractionHeight = 8.dp,
            refractionAmount = 16.dp,
            chromaticAberration = false,
            vibrancy = false,
            veil = Color.White.copy(alpha = cardWhite),
            hairline = true,
            plain = true,
            pressBoost = false,
            shadowRadius = null,
            shadowAlpha = 0f,
            shadowColor = Color.Transparent,
        )
        GlassTier.Control -> GlassRecipe(
            blur = 2.dp,
            refractionHeight = 12.dp,
            refractionAmount = 24.dp,
            chromaticAberration = true,
            vibrancy = true,
            veil = Color.Unspecified,
            hairline = false,
            plain = false,
            pressBoost = true,
            shadowRadius = 12.dp,
            shadowAlpha = 0.08f,
            shadowColor = Color.Black.copy(alpha = 0.1f),
        )
        GlassTier.Float -> GlassRecipe(
            blur = 2.dp,
            refractionHeight = 12.dp,
            refractionAmount = 24.dp,
            chromaticAberration = true,
            vibrancy = true,
            veil = Color.Unspecified,
            hairline = false,
            plain = false,
            pressBoost = true,
            shadowRadius = 28.dp,
            shadowAlpha = 1f,
            shadowColor = Color.Black.copy(alpha = 0.16f),
        )
        GlassTier.Panel -> GlassRecipe(
            blur = 3.dp,
            refractionHeight = 6.dp,
            refractionAmount = 10.dp,
            chromaticAberration = false,
            vibrancy = false,
            veil = if (isDark) Color(0xFF0B1020).copy(alpha = 0.58f) else Color.White.copy(alpha = 0.38f),
            hairline = false,
            plain = true,
            pressBoost = false,
            shadowRadius = null,
            shadowAlpha = 0f,
            shadowColor = Color.Transparent,
        )
    }
}

/**
 * Tiered liquid glass. Parameters live here, not in each screen.
 *
 * [lensEnabled] skips blur and `lens()` for rows past [GlassLensBudget].
 * Card and Panel use [drawPlainBackdrop] (no highlight, no shadow).
 * Control and Float use [drawBackdrop] with [Highlight.Default].
 * Press on Control / Float scales refraction from the catalog button
 * (12dp / 24dp) toward 16dp / 32dp.
 */
@Composable
fun Modifier.glass(
    backdrop: Backdrop,
    tier: GlassTier,
    shape: Shape = Capsule(),
    surface: Color = Color.Unspecified,
    press: InteractiveHighlight? = null,
    refraction: Dp? = null,
    blurRadius: Dp? = null,
    chromatic: Boolean? = null,
    lensEnabled: Boolean = true,
): Modifier {
    val isDark = studyColors().label.red > 0.5f
    val recipe = glassRecipe(tier, isDark, refraction, blurRadius, chromatic)
    val veil = when {
        surface.isSpecified && tier == GlassTier.Card -> surface.cappedCardWhite()
        surface.isSpecified -> surface
        tier == GlassTier.Card || tier == GlassTier.Panel -> recipe.veil
        else -> Color.Unspecified
    }
    val layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)? =
        if (press == null) null else ({ applyLiquidPress(press) })
    val effects: BackdropEffectScope.() -> Unit = effects@{
        if (!lensEnabled) return@effects
        // Documented order: color filter, blur, lens.
        if (recipe.vibrancy) vibrancy()
        blur(recipe.blur.toPx())
        val boost = if (recipe.pressBoost) (press?.pressProgress ?: 0f) else 0f
        val height = lerp(recipe.refractionHeight.toPx(), recipe.refractionHeight.toPx() * (4f / 3f), boost)
        val amount = lerp(recipe.refractionAmount.toPx(), recipe.refractionAmount.toPx() * (4f / 3f), boost)
        // `refractionAmount` must stay inside the element. Do not pin it to 24dp.
        val capped = amount.coerceIn(0f, size.minDimension)
        if (height > 0f && capped > 0f) {
            lens(
                refractionHeight = height,
                refractionAmount = capped,
                depthEffect = false,
                chromaticAberration = recipe.chromaticAberration,
            )
        }
    }
    val onSurface: DrawScope.() -> Unit = {
        if (veil.isSpecified) drawRect(veil)
    }
    val onFront: (DrawScope.() -> Unit)? = if (recipe.hairline) {
        { drawGlassHairline(shape, isDark) }
    } else {
        null
    }
    val plain = recipe.plain || !lensEnabled
    return if (plain) {
        drawPlainBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = effects,
            layerBlock = layerBlock,
            onDrawSurface = onSurface,
            onDrawFront = onFront,
        )
    } else {
        val radius = recipe.shadowRadius
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = effects,
            highlight = { Highlight.Default },
            shadow = if (radius == null) {
                null
            } else {
                {
                    Shadow(
                        radius = radius,
                        alpha = recipe.shadowAlpha,
                        color = recipe.shadowColor,
                    )
                }
            },
            layerBlock = layerBlock,
            onDrawSurface = onSurface,
            onDrawFront = onFront,
        )
    }
}

private fun DrawScope.drawGlassHairline(shape: Shape, isDark: Boolean) {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val brush = if (isDark) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.42f),
            0.45f to Color.White.copy(alpha = 0.06f),
            1f to Color.White.copy(alpha = 0.28f),
        )
    } else {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.70f),
            0.42f to Color.White.copy(alpha = 0.10f),
            1f to Color.White.copy(alpha = 0.46f),
        )
    }
    drawPath(path = path, brush = brush, style = Stroke(width = 1.dp.toPx()))
}
