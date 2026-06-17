package io.github.ddsimoes.sweet.compose

import androidx.compose.runtime.Recomposer
import io.github.ddsimoes.sweet.internal.SWTSnapshotManager
import io.github.ddsimoes.sweet.internal.createFrameClock
import io.github.ddsimoes.sweet.internal.ensureSnapshotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages all Compose compositions at the Display level.
 * Uses Display.setData() to store a singleton instance per Display.
 */
class ComposeManager private constructor(
    private val display: Display,
) {
    private val composeScopes = ConcurrentHashMap<Composite, ComposeScope>()
    private val frameClock = display.createFrameClock()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + frameClock)
    private val recomposer = Recomposer(uiScope.coroutineContext)
    private val snapshotManager: SWTSnapshotManager

    init {
        // Initialize the SWT snapshot manager and frame clock - this is what makes recomposition work!
        snapshotManager = ensureSnapshotManager(display, uiScope, frameClock)
        frameClock.start(uiScope)

        // Start the recomposer
        uiScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    companion object {
        private const val COMPOSE_MANAGER_KEY = "sweet.compose.manager"

        /**
         * Gets or creates the ComposeManager for the given Display.
         */
        fun getOrCreate(display: Display): ComposeManager =
            display.getData(COMPOSE_MANAGER_KEY) as? ComposeManager
                ?: ComposeManager(display).also { manager ->
                    display.setData(COMPOSE_MANAGER_KEY, manager)

                    // Dispose manager when display is disposed
                    display.addListener(org.eclipse.swt.SWT.Dispose) {
                        manager.dispose()
                    }
                }
    }

    /**
     * Creates a new ComposeScope for the given Composite.
     */
    fun createScope(composite: Composite): ComposeScope {
        val scope = ComposeScope(composite, this, recomposer)
        composeScopes[composite] = scope
        composite.setData(ComposeScope.COMPOSE_SCOPE_KEY, scope)

        // Remove scope when composite is disposed
        composite.addDisposeListener {
            removeScope(composite)
        }

        return scope
    }

    /**
     * Creates an application-level composition that doesn't require a shell.
     * This is used by the application() function to run the root composition.
     */
    fun createApplicationComposition(): ApplicationComposition = ApplicationComposition(this, recomposer)

    /**
     * Removes a ComposeScope for the given Composite.
     */
    fun removeScope(composite: Composite) {
        composeScopes.remove(composite)?.dispose()
        composite.setData(ComposeScope.COMPOSE_SCOPE_KEY, null)
    }

    /**
     * Gets the Recomposer for this ComposeManager.
     */
    fun getRecomposer(): Recomposer = recomposer

    /**
     * Disposes all resources associated with this ComposeManager.
     */
    fun dispose() {
        // Dispose snapshot observer before cancelling scopes (F5)
        snapshotManager.dispose()

        // Dispose all scopes
        composeScopes.values.forEach { it.dispose() }
        composeScopes.clear()

        // Stop the frame clock
        frameClock.stop()
        // Cancel uiScope and recomposer
        uiScope.cancel()
        recomposer.cancel()

        // Remove from display
        display.setData(COMPOSE_MANAGER_KEY, null)
    }

    /**
     * Checks if the ComposeManager is disposed.
     */
    fun isDisposed(): Boolean = display.isDisposed
}
