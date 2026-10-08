package com.partner.studyreminder.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.partner.studyreminder.parse.PlanTime
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Shared motion. Content displacement uses [smooth], chrome and sheets use [snappy],
 * the completion check uses [bouncy].
 */
object Motion {
    /** px/s. A flick at least this fast wins over the distance threshold. */
    const val Fling: Float = 900f

    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 170f)

    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 300f)

    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = 170f)
}

/**
 * Applies [delta] to [current], 1:1 inside [min, max].
 * Past either end the same finger travel moves less the farther out it already is.
 * Coming back toward the range is undamped until the bound.
 */
fun resistedDrag(current: Float, delta: Float, min: Float, max: Float, span: Float): Float {
    if (delta == 0f) return current
    val band = span.coerceAtLeast(1f)
    val low = minOf(min, max)
    val high = maxOf(min, max)
    if (current < low && delta > 0f) {
        val room = low - current
        if (delta <= room) return current + delta
        return resistedDrag(low, delta - room, low, high, band)
    }
    if (current > high && delta < 0f) {
        val room = current - high
        if (-delta <= room) return current + delta
        return resistedDrag(high, delta + room, low, high, band)
    }
    if (current in low..high) {
        val next = current + delta
        if (next in low..high) return next
        val edge = if (next > high) high else low
        val over = abs(next - edge)
        val resisted = over * (band / (band + over))
        return if (next > high) edge + resisted else edge - resisted
    }
    val overflow = if (current > high) current - high else low - current
    return current + delta * (band / (band + overflow))
}

/** Milliseconds until the next Asia/Shanghai minute. An exact boundary waits a full minute. */
fun millisUntilNextBeijingMinute(now: Instant = Instant.now()): Long {
    val local = now.atZone(PlanTime.ZONE)
    val next = local.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    val wait = Duration.between(local, next).toMillis()
    return if (wait <= 0L) 60_000L else wait
}

/** In-progress labels and the progress bar share this clock, aligned to the Beijing minute. */
@Composable
fun rememberBeijingMinute(): Int {
    var minutes by remember { mutableIntStateOf(PlanTime.nowMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextBeijingMinute())
            minutes = PlanTime.nowMinutes()
        }
    }
    return minutes
}
