package androidx.compose.foundation.gestures

/**
 * Receiver scope for performing scroll actions.
 */
interface ScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Float): Float
}
