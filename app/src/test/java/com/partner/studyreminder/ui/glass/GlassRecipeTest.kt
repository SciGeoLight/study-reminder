package com.partner.studyreminder.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassRecipeTest {
    @Test
    fun callerRefractionAndBlurAreNotForcedTo24() {
        val recipe = glassRecipe(
            GlassTier.Control,
            isDark = false,
            refraction = 12.dp,
            blurRadius = 6.dp,
            chromatic = false,
        )
        assertEquals(12.dp, recipe.refractionAmount)
        assertEquals(6.dp, recipe.refractionHeight)
        assertEquals(6.dp, recipe.blur)
        assertFalse(recipe.chromaticAberration)
    }

    @Test
    fun cardWhiteStaysAtOrBelowQuarterAndSkipsVibrancy() {
        val light = glassRecipe(GlassTier.Card, isDark = false)
        val dark = glassRecipe(GlassTier.Card, isDark = true)
        assertTrue(light.veil.alpha <= CardWhiteTintMax)
        assertTrue(dark.veil.alpha <= CardWhiteTintMax)
        assertEquals(1f, light.veil.red, 0.001f)
        assertEquals(1f, dark.veil.red, 0.001f)
        assertFalse(light.vibrancy)
        assertFalse(dark.vibrancy)
        assertTrue(light.hairline)
        assertTrue(light.plain)
        assertFalse(light.chromaticAberration)
    }

    @Test
    fun tiersDoNotShareOneLens() {
        val card = glassRecipe(GlassTier.Card, isDark = false)
        val control = glassRecipe(GlassTier.Control, isDark = false)
        val floatTier = glassRecipe(GlassTier.Float, isDark = false)
        val panel = glassRecipe(GlassTier.Panel, isDark = false)
        assertTrue(card.refractionAmount < control.refractionAmount)
        assertTrue(panel.refractionAmount < control.refractionAmount)
        assertEquals(control.refractionHeight, floatTier.refractionHeight)
        assertEquals(control.refractionAmount, floatTier.refractionAmount)
        assertTrue(floatTier.shadowRadius!! > control.shadowRadius!!)
        assertTrue(panel.veil.alpha > card.veil.alpha)
        assertTrue(panel.veil.alpha < 0.5f)
        assertFalse(panel.vibrancy)
        assertTrue(control.chromaticAberration)
        assertTrue(control.pressBoost)
        assertFalse(control.veil.isSpecified)
    }

    @Test
    fun thickWhiteOnACardIsCapped() {
        val capped = Color.White.copy(alpha = 0.8f).cappedCardWhite()
        assertEquals(CardWhiteTintMax, capped.alpha, 0.001f)
        val tint = Color(0xFF34C759).copy(alpha = 0.8f).cappedCardWhite()
        assertEquals(0.8f, tint.alpha, 0.001f)
    }

    @Test
    fun lensBudgetIsEight() {
        assertEquals(8, GlassLensBudget)
    }
}
