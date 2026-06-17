package io.github.ddsimoes.sweet.layout

import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display

/**
 * Coordinates SWT layout calls with Compose frames.
 *
 * High level contract:
 * - During a Compose frame (`beginFrame` / `endFrame`), calls to [requestLayout] for
 *   Sweet-managed composites are batched and flushed once at frame end. This ensures
 *   layout runs after recomposition and applier changes, and avoids redundant layouts.
 * - Outside frames (pure SWT events such as shell resize), [requestLayout] performs
 *   an immediate `layout(true, true)` to preserve normal SWT behavior.
 *
 * Only Sweet-managed code should call [requestLayout] for Compose content. Tests and
 * external SWT code should continue to call `layout()` directly where appropriate.
 *
 * Each [Display] owns its own [LayoutCoordinator] instance, stored via
 * `display.getData(KEY)`. Use [LayoutCoordinator.forDisplay] to obtain it.
 */
class LayoutCoordinator private constructor(
    @Suppress("unused") private val display: Display?,
) {
    /** Depth counter for re-entrant frame support. Layout is flushed only when depth reaches 0. */
    private var frameDepth: Int = 0
    private val pendingRoots = mutableSetOf<Composite>()

    fun beginFrame() {
        frameDepth++
    }

    fun endFrame() {
        if (frameDepth == 0) {
            SweetDebugger.log("LayoutCoordinator", "WARNING: endFrame called without matching beginFrame — ignoring")
            return
        }
        frameDepth--
        if (frameDepth == 0) {
            flushLayout()
        }
    }

    fun isInFrame(): Boolean = frameDepth > 0

    /**
     * Asserts that the coordinator is in a balanced state (not mid-frame, no pending roots).
     * Under `-ea`, logs a warning and self-heals if violated.
     * Call at the start of each test to catch leaked frame state.
     */
    fun assertBalanced() {
        if (frameDepth != 0 || pendingRoots.isNotEmpty()) {
            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log(
                    "LayoutCoordinator",
                    "WARNING: assertBalanced failed — depth=$frameDepth, pending=${pendingRoots.size}. Self-healing.",
                )
            }
            frameDepth = 0
            pendingRoots.clear()
        }
    }

    /**
     * Request layout for [composite]. If called during a Compose frame,
     * the composite (or its nearest Sweet-managed ancestor) is recorded
     * and laid out at frame end. Otherwise, layout is executed
     * immediately.
     */
    fun requestLayout(composite: Composite) {
        if (composite.isDisposed) {
            return
        }

        if (!isInFrame()) {
            // Outside Compose frames we preserve SWT's immediate semantics.
            safeLayout(composite)
            return
        }

        pendingRoots.add(composite)
    }

    private fun flushLayout() {
        if (pendingRoots.isEmpty()) {
            return
        }

        val candidates =
            pendingRoots
                .filter { !it.isDisposed }
                .sortedBy { it.depth() }
        pendingRoots.clear()

        val laidOut = mutableSetOf<Composite>()
        candidates.forEach { root ->
            if (root.isDisposed) {
                return@forEach
            }
            if (laidOut.any { root.isDescendantOf(it) }) {
                return@forEach
            }
            safeLayout(root)
            laidOut.add(root)
        }
    }

    private fun safeLayout(composite: Composite) {
        try {
            composite.layout(true, true)
        } catch (e: Exception) {
            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log(
                    "LayoutCoordinator",
                    "Error during coordinated layout for ${composite.javaClass.simpleName}[${composite.hashCode().toString(16)}]",
                    e,
                )
            }
        }
    }

    private fun Composite.depth(): Int {
        var d = 0
        var p = parent
        while (p is Composite) {
            d++
            p = p.parent
        }
        return d
    }

    private fun Composite.isDescendantOf(ancestor: Composite): Boolean {
        var p = parent
        while (p is Composite) {
            if (p === ancestor) {
                return true
            }
            p = p.parent
        }
        return false
    }

    companion object {
        private const val KEY = "sweet.layout.coordinator"

        /**
         * Returns the per-Display [LayoutCoordinator], creating it on first access.
         * The instance is cleaned up automatically when the display is disposed.
         */
        fun forDisplay(display: Display): LayoutCoordinator {
            val existing = display.getData(KEY) as? LayoutCoordinator
            if (existing != null) return existing
            val coordinator = LayoutCoordinator(display)
            display.setData(KEY, coordinator)
            display.disposeExec {
                display.setData(KEY, null)
            }
            return coordinator
        }

        /** Creates a standalone [LayoutCoordinator] not tied to any display. For unit testing only. */
        fun createForTest(): LayoutCoordinator = LayoutCoordinator(null)
    }
}
