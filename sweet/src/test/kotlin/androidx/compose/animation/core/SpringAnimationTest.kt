package androidx.compose.animation.core

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavioral tests for spring-based animation.
 *
 * Verifies that [spring] / [SpringSpec] drives real spring physics via [SpringSimulation],
 * and that [Animatable.animateTo] settles at its target — closing the gap where a SpringSpec
 * previously fell through to a 300 ms linear tween (its damping ratio / stiffness ignored).
 */
class SpringAnimationTest {

    /** Frame clock whose frames are posted manually, so animation timing is deterministic. */
    private class ManualFrameClock : MonotonicFrameClock {
        private val frames = Channel<Long>(Channel.UNLIMITED)
        fun post(nanos: Long) { frames.trySend(nanos) }
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(frames.receive())
    }

    @Test
    fun `critically damped spring approaches target monotonically without overshoot`() {
        val sim = SpringSimulation(100f).also {
            it.stiffness = Spring.StiffnessMedium
            it.dampingRatio = Spring.DampingRatioNoBouncy // 1.0 — critically damped
        }
        var pos = 0f
        var vel = 0f
        val samples = mutableListOf<Float>()
        repeat(100) {
            val m = sim.updateValues(pos, vel, 16f) // 16 ms steps
            pos = m.value
            vel = m.velocity
            samples += pos
        }
        assertTrue(samples.last() > 99f, "should approach target, last=${samples.last()}")
        // Critically damped: never overshoots the target.
        assertTrue(samples.all { it <= 100.001f }, "critically damped must not overshoot: $samples")
        // Monotonically non-decreasing toward the target.
        for (i in 1 until samples.size) {
            assertTrue(samples[i] >= samples[i - 1] - 0.001f, "should be monotonic: $samples")
        }
    }

    @Test
    fun `underdamped spring overshoots target`() {
        val sim = SpringSimulation(100f).also {
            it.stiffness = Spring.StiffnessMedium
            it.dampingRatio = Spring.DampingRatioMediumBouncy // 0.5 < 1 — underdamped
        }
        var pos = 0f
        var vel = 0f
        var max = 0f
        repeat(200) {
            val m = sim.updateValues(pos, vel, 16f)
            pos = m.value
            vel = m.velocity
            if (pos > max) max = pos
        }
        assertTrue(max > 100f, "underdamped spring should overshoot 100, max=$max")
    }

    @Test
    fun `Animatable animateTo with default spring settles at target`() {
        val clock = ManualFrameClock()
        val animatable = Animatable(0f, FloatConverter)
        // Pre-post enough frames for a critically-damped medium spring to settle (~30 frames;
        // 200 is generous headroom). With frames queued, animateTo never externally suspends.
        runBlocking(clock) {
            var t = 0L
            repeat(200) { t += 16_000_000L; clock.post(t) }
            animatable.animateTo(100f) // default spring()
        }
        assertTrue(abs(animatable.value - 100f) < 1f, "spring should settle near 100, got ${animatable.value}")
    }
}
