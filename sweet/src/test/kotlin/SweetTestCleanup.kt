package io.github.ddsimoes.sweet.test

import io.github.ddsimoes.sweet.layout.SweetLayout

/**
 * Resets per-test mutable state for deterministic test runs.
 *
 * Call inside `autoSWT { }` at the start of each UI test:
 * ```
 * @Test fun myTest() = autoSWT {
 *     sweetTestCleanup()
 *     testShell(...) { ... }.test { ... }
 * }
 * ```
 *
 * Resets:
 * 1. [SweetLayout.measureCounts] — cleared
 * 2. [SweetLayout.measureCountEnabled] — set false (tests opt in explicitly)
 *
 * LayoutCoordinator per-Display isolation (Task 2) handles frame state leakage.
 * SnapshotManager disposal is handled by ComposeManager.dispose() (Task QW-11).
 */
fun sweetTestCleanup() {
    SweetLayout.resetMeasureCounts()
    SweetLayout.measureCountEnabled = false
}
