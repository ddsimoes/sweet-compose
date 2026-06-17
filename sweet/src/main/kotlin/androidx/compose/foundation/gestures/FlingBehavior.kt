package androidx.compose.foundation.gestures

/**
 * Interface to specify fling behavior.
 *
 * When drag has ended with velocity in scrollable components, [performFling] is invoked to
 * perform fling animation and update state to reflect this change.
 *
 * Sweet note: gesture-driven flinging is not implemented yet (planned with the animation
 * foundation, doc 21); this type exists for API compatibility of scroll modifiers.
 */
interface FlingBehavior {
    /**
     * Performs settling via fling animation with given velocity and suspends until fling has
     * finished.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     *   [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     * @return remaining velocity after fling operation has ended
     */
    suspend fun ScrollScope.performFling(initialVelocity: Float): Float
}
