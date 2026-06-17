package androidx.compose.animation.core

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A value holder that can animate its value over time using [AnimationSpec]s.
 *
 * [Animatable] acts as a coroutine-based animation primitive. Calling [animateTo]
 * launches a suspend function that ticks the value frame by frame until the
 * target is reached.
 *
 * @param initialValue the starting value
 */
class Animatable<T, V : AnimationVector>(
    initialValue: T,
    private val typeConverter: TwoWayConverter<T, V>,
) {
    // Backed by snapshot state so reads during composition are tracked and writes drive
    // recomposition (matches AOSP's Animatable, which uses mutableStateOf for `value`).
    private val valueState = mutableStateOf(initialValue)
    var value: T
        get() = valueState.value
        private set(newValue) {
            valueState.value = newValue
        }

    var isRunning: Boolean = false
        private set

    suspend fun snapTo(targetValue: T) {
        value = targetValue
    }

    /**
     * Animates the value from its current value to [targetValue].
     *
     * Drives the animation frame by frame using the [MonotonicFrameClock] from the coroutine
     * context. The clock's frame timestamp (not wall-clock [System.nanoTime]) paces the
     * animation, so values stay in sync with frame delivery.
     *
     * - [TweenSpec]: interpolates over [TweenSpec.durationMillis] using [TweenSpec.easing].
     * - [SpringSpec]: integrates spring physics via [SpringSimulation] (critically-damped by
     *   default) and snaps to [targetValue] once displacement and velocity fall below
     *   [Spring.DefaultDisplacementThreshold].
     */
    suspend fun animateTo(
        targetValue: T,
        animationSpec: AnimationSpec<T> = spring(),
    ) {
        val clock = currentCoroutineContext()[MonotonicFrameClock]
            ?: throw IllegalStateException(
                "Animatable.animateTo requires a MonotonicFrameClock in the coroutine context."
            )
        isRunning = true
        try {
            when (animationSpec) {
                is TweenSpec<T> -> animateWithTween(clock, targetValue, animationSpec)
                is SpringSpec<T> -> animateWithSpring(clock, targetValue, animationSpec)
            }
            value = targetValue
        } catch (e: CancellationException) {
            // Animation cancelled — keep current interpolated value, but rethrow
            // for proper coroutine cancellation per AOSP.
            throw e
        } finally {
            isRunning = false
        }
    }

    private suspend fun animateWithTween(
        clock: MonotonicFrameClock,
        targetValue: T,
        spec: TweenSpec<T>,
    ) {
        val startVector = typeConverter.convertToVector(value)
        val targetVector = typeConverter.convertToVector(targetValue)
        val size = startVector.size
        val durationNanos = spec.durationMillis * MillisToNanos

        var startTimeNanos = -1L
        while (true) {
            val frameNanos = clock.withFrameNanos { nanos -> nanos }
            if (startTimeNanos < 0) startTimeNanos = frameNanos

            val playTimeNanos = frameNanos - startTimeNanos
            val fractionRaw = if (durationNanos > 0) {
                (playTimeNanos.toFloat() / durationNanos).coerceIn(0f, 1f)
            } else {
                1f
            }
            val fraction = spec.easing.transform(fractionRaw)

            val currentVector = startVector.newVector()
            for (i in 0 until size) {
                currentVector[i] = startVector[i] + (targetVector[i] - startVector[i]) * fraction
            }
            @Suppress("UNCHECKED_CAST")
            value = typeConverter.convertFromVector(currentVector as V)

            if (fractionRaw >= 1f) break
            currentCoroutineContext().ensureActive()
        }
    }

    private suspend fun animateWithSpring(
        clock: MonotonicFrameClock,
        targetValue: T,
        spec: SpringSpec<T>,
    ) {
        val startVector = typeConverter.convertToVector(value)
        val targetVector = typeConverter.convertToVector(targetValue)
        val size = startVector.size

        // One solver per dimension; each carries its own position + velocity.
        val solvers = Array(size) { i ->
            SpringSimulation(targetVector[i]).also {
                it.stiffness = spec.stiffness
                it.dampingRatio = spec.dampingRatio
            }
        }
        val positions = FloatArray(size) { startVector[it] }
        val velocities = FloatArray(size) { 0f }
        val threshold = Spring.DefaultDisplacementThreshold

        var startTimeNanos = -1L
        var lastFrameNanos = 0L
        var iterations = 0
        while (true) {
            val frameNanos = clock.withFrameNanos { nanos -> nanos }
            if (startTimeNanos < 0) {
                // First frame establishes the baseline; emit the start value, no delta yet.
                startTimeNanos = frameNanos
                lastFrameNanos = frameNanos
                continue
            }
            val deltaMillis = (frameNanos - lastFrameNanos).toFloat() / MillisToNanos
            lastFrameNanos = frameNanos

            val currentVector = startVector.newVector()
            var settled = true
            for (i in 0 until size) {
                val motion = solvers[i].updateValues(positions[i], velocities[i], deltaMillis)
                positions[i] = motion.value
                velocities[i] = motion.velocity
                currentVector[i] = motion.value
                if (abs(motion.value - targetVector[i]) > threshold ||
                    abs(motion.velocity) > threshold
                ) {
                    settled = false
                }
            }
            @Suppress("UNCHECKED_CAST")
            value = typeConverter.convertFromVector(currentVector as V)

            if (settled) break
            // Safety net: a degenerate stiffness/damping combo could fail to converge.
            if (++iterations >= 1000) break // ~16s at 60fps
            currentCoroutineContext().ensureActive()
        }
    }
}

/**
 * A bidirectional converter between a type [T] and an [AnimationVector] subtype [V].
 */
interface TwoWayConverter<T, V : AnimationVector> {
    val convertToVector: (T) -> V
    val convertFromVector: (V) -> T
}

/**
 * Two-way converter for Float values (uses AnimationVector1D).
 */
val FloatConverter: TwoWayConverter<Float, AnimationVector1D> =
    object : TwoWayConverter<Float, AnimationVector1D> {
        override val convertToVector: (Float) -> AnimationVector1D =
            { AnimationVector1D(it) }
        override val convertFromVector: (AnimationVector1D) -> Float =
            { it.value }
    }
