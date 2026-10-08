package com.partner.studyreminder.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundScaleTest {
    @Test
    fun sampleSizeKeepsTheLongEdgeNearTheLimit() {
        assertEquals(1, BackgroundScale.sampleSize(800, 600, 1600))
        assertEquals(1, BackgroundScale.sampleSize(1600, 900, 1600))
        assertEquals(2, BackgroundScale.sampleSize(3200, 2000, 1600))
        assertEquals(4, BackgroundScale.sampleSize(8000, 6000, 1600))
        assertEquals(1, BackgroundScale.sampleSize(0, 100, 1600))
    }
}