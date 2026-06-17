package io.github.ddsimoes.sweet.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer

/**
 * A composition that doesn't require a shell - used for application-level compositions
 * where Window composables create their own shells.
 */
class ApplicationComposition(
    private val manager: ComposeManager,
    private val recomposer: Recomposer,
) {
    private val applier = EmptyApplier()
    private val composition: Composition = Composition(applier, recomposer)
    private var isDisposed = false

    /**
     * Sets the content of this composition.
     */
    fun setContent(content: @Composable () -> Unit) {
        if (isDisposed) {
            error("ApplicationComposition is disposed")
        }

        composition.setContent(content)
    }

    /**
     * Disposes this ApplicationComposition.
     */
    fun dispose() {
        if (isDisposed) return

        isDisposed = true
        composition.dispose()
    }

    /**
     * Checks if this ApplicationComposition is disposed.
     */
    fun isDisposed(): Boolean = isDisposed
}

/**
 * An applier that doesn't apply anything - used for application-level compositions
 * where the actual UI is created by Window composables.
 */
private class EmptyApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(
        index: Int,
        instance: Unit,
    ) {
        // No-op: application-level compositions don't create actual UI
    }

    override fun insertBottomUp(
        index: Int,
        instance: Unit,
    ) {
        // No-op: application-level compositions don't create actual UI
    }

    override fun remove(
        index: Int,
        count: Int,
    ) {
        // No-op: application-level compositions don't create actual UI
    }

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) {
        // No-op: application-level compositions don't create actual UI
    }

    override fun onClear() {
        // No-op: application-level compositions don't create actual UI
    }
}
