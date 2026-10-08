package com.partner.studyreminder.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SliderReadoutTest {
    @Test
    fun plateStaysSolidAndClearsThreeToOne() {
        val light = sliderReadout(light = true)
        val dark = sliderReadout(light = false)
        assertEquals(Color.White, light.plate)
        assertTrue(light.plate.alpha >= 0.70f)
        assertEquals(0x0B / 255f, dark.plate.red, 0.01f)
        assertEquals(0x10 / 255f, dark.plate.green, 0.01f)
        assertEquals(0x20 / 255f, dark.plate.blue, 0.01f)
        assertTrue(dark.plate.alpha >= 0.70f)
        assertTrue(contrastRatio(light.digit, light.plate) >= 3f)
        assertTrue(contrastRatio(light.caption, light.plate) >= 3f)
        assertTrue(contrastRatio(dark.digit, dark.plate) >= 3f)
        assertTrue(contrastRatio(dark.caption, dark.plate) >= 3f)
    }
}
