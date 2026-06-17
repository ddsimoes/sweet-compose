package androidx.compose.animation.core

/**
 * Creates a spring-based [AnimationSpec] using the provided spring parameters.
 *
 * @param dampingRatio the damping ratio of the spring
 * @param stiffness the stiffness of the spring
 * @param visibilityThreshold the visibility threshold for the animated value.
 *   When the distance between the current value and the target is less than
 *   this threshold, the animation is considered complete.
 */
fun <T> spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null,
): SpringSpec<T> = SpringSpec(dampingRatio, stiffness, visibilityThreshold)

/**
 * Creates a duration-based [AnimationSpec] using the provided duration and easing.
 *
 * @param durationMillis the duration of the animation in milliseconds
 * @param easing the easing curve used for the animation
 */
fun <T> tween(
    durationMillis: Int = 300,
    easing: Easing = FastOutSlowInEasing,
): TweenSpec<T> = TweenSpec(durationMillis, easing)

/**
 * A specification for animating a value from a start to a target.
 */
sealed interface AnimationSpec<T>

/**
 * A spring-based animation specification.
 */
data class SpringSpec<T>(
    val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    val stiffness: Float = Spring.StiffnessMedium,
    val visibilityThreshold: T? = null,
) : AnimationSpec<T>

/**
 * A duration-based animation specification with an easing curve.
 */
data class TweenSpec<T>(
    val durationMillis: Int = 300,
    val easing: Easing = FastOutSlowInEasing,
    val delayMillis: Int = 0,
) : AnimationSpec<T>
