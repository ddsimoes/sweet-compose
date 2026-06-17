package androidx.compose.animation.core

import kotlin.math.abs
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [FloatExponentialDecaySpec] nanosecond precision.
 *
 * Guards against a regression where [getValueFromNanos] / [getVelocityFromNanos]
 * truncated sub-millisecond play time via integer division (`playTimeNanos / MillisToNanos`)
 * before feeding it to [exp]. The fix computes the exponent from float seconds
 * (`playTimeNanos.toFloat() / 1_000_000_000f`), matching AOSP's `FloatExponentialDecaySpec`.
 *
 * `FloatExponentialDecaySpec()` defaults: frictionMultiplier = 1 -> friction = -4.2f,
 * absVelocityThreshold = 0.1f.
 */
class FloatDecayAnimationTest {

    private val friction = -4.2f

    /**
     * At 1.5 ms (1_500_000 ns) — a non-round-millisecond value — the value must equal the
     * float-seconds formula. With the truncation bug, 1_500_000 ns collapsed to 1 ms and the
     * value was off by ~0.5, so a 1e-4 tolerance cleanly distinguishes fixed from buggy.
     */
    @Test
    fun `getValueFromNanos uses sub-millisecond float precision`() {
        val spec = FloatExponentialDecaySpec()
        val initialValue = 0f
        val initialVelocity = 1000f
        val playTimeNanos = 1_500_000L // 1.5 ms

        val expected = initialValue - initialVelocity / friction +
            initialVelocity / friction * exp(friction * playTimeNanos.toFloat() / 1_000_000_000f)

        val actual = spec.getValueFromNanos(playTimeNanos, initialValue, initialVelocity)
        assertTrue(
            abs(expected - actual) < 1e-4f,
            "expected $expected but got $actual (delta ${abs(expected - actual)} >= 1e-4)"
        )
    }

    /** At t = 0 the value is exactly [initialValue] (exp(0f) == 1f, so the velocity terms cancel). */
    @Test
    fun `getValueFromNanos at zero nanos equals initialValue exactly`() {
        val spec = FloatExponentialDecaySpec()
        val initialValue = 42f
        val initialVelocity = 1000f

        assertEquals(
            initialValue,
            spec.getValueFromNanos(0L, initialValue, initialVelocity)
        )
    }

    /** Velocity magnitude strictly decreases as time advances (exponential decay toward 0). */
    @Test
    fun `getVelocityFromNanos decays toward zero`() {
        val spec = FloatExponentialDecaySpec()
        val initialValue = 0f
        val initialVelocity = 1000f

        val t1 = 1_000_000L // 1 ms
        val t2 = 10_000_000L // 10 ms

        val v1 = spec.getVelocityFromNanos(t1, initialValue, initialVelocity)
        val v2 = spec.getVelocityFromNanos(t2, initialValue, initialVelocity)

        assertTrue(abs(v2) < abs(v1), "velocity should decay: |v2|=$v2 should be < |v1|=$v1")
    }
}
