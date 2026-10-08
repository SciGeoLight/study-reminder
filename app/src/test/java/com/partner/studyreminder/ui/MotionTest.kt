package com.partner.studyreminder.ui

import com.partner.studyreminder.parse.PlanTime
import java.time.ZonedDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun tokensMatchDampingAndStiffness() {
        val smooth = Motion.smooth<Float>()
        assertEquals(1f, smooth.dampingRatio, 0.001f)
        assertEquals(170f, smooth.stiffness, 0.001f)
        val snappy = Motion.snappy<Float>()
        assertEquals(0.85f, snappy.dampingRatio, 0.001f)
        assertEquals(300f, snappy.stiffness, 0.001f)
        val bouncy = Motion.bouncy<Float>()
        assertEquals(0.7f, bouncy.dampingRatio, 0.001f)
        assertEquals(170f, bouncy.stiffness, 0.001f)
        assertEquals(900f, Motion.Fling, 0.001f)
    }

    @Test
    fun dragInsideTheRangeTracksTheFinger() {
        assertEquals(15f, resistedDrag(10f, 5f, 0f, 100f, 80f), 0.01f)
    }

    @Test
    fun fartherPastTheBoundMovesLess() {
        val fromEdge = resistedDrag(0f, -20f, 0f, 100f, 80f)
        val fromOutside = resistedDrag(-30f, -20f, 0f, 100f, 80f)
        val edgeTravel = abs(fromEdge - 0f)
        val outsideTravel = abs(fromOutside - (-30f))
        assertTrue(edgeTravel < 20f)
        assertTrue(outsideTravel < edgeTravel)
    }

    @Test
    fun returningIntoRangeIsUndamped() {
        assertEquals(4f, resistedDrag(-16f, 20f, 0f, 100f, 80f), 0.01f)
    }

    @Test
    fun minuteDelayAlignsToTheNextBeijingMinute() {
        val half = ZonedDateTime.of(2026, 10, 8, 14, 30, 30, 500_000_000, PlanTime.ZONE).toInstant()
        assertEquals(29_500L, millisUntilNextBeijingMinute(half))
        val boundary = ZonedDateTime.of(2026, 10, 8, 14, 31, 0, 0, PlanTime.ZONE).toInstant()
        assertEquals(60_000L, millisUntilNextBeijingMinute(boundary))
    }

    @Test
    fun ringsFollowSystemAnimationScales() {
        assertTrue(systemAnimationsEnabled(1f, 1f))
        assertTrue(systemAnimationsEnabled(0.5f, 1f))
        assertFalse(systemAnimationsEnabled(0f, 1f))
        assertFalse(systemAnimationsEnabled(1f, 0f))
    }

    @Test
    fun viewerScrimFadesAsThePhotoIsDraggedDown() {
        assertEquals(0.92f, viewerScrimAlpha(0f, 1000f), 0.001f)
        assertEquals(0.46f, viewerScrimAlpha(500f, 1000f), 0.001f)
        assertEquals(0f, viewerScrimAlpha(1000f, 1000f), 0.001f)
        assertEquals(0.92f, viewerScrimAlpha(-20f, 1000f), 0.001f)
    }
}
