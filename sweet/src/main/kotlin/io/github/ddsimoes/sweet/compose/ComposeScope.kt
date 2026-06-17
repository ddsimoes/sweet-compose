package io.github.ddsimoes.sweet.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import io.github.ddsimoes.sweet.compose.locals.LocalComposeManager
import io.github.ddsimoes.sweet.compose.locals.LocalComposeScope
import io.github.ddsimoes.sweet.compose.locals.LocalDisplay
import io.github.ddsimoes.sweet.compose.locals.LocalDisplayDensity
import io.github.ddsimoes.sweet.compose.locals.LocalSWTComposite
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.getSweetDensity
import org.eclipse.swt.widgets.Composite

/**
 * Manages a Compose composition within a specific SWT Composite.
 * Uses Composite.setData() to store the association.
 */
class ComposeScope(
    private val rootComposite: Composite,
    private val manager: ComposeManager,
    private val recomposer: Recomposer,
) {
    private val applier = SWTNodeApplier(rootComposite)
    private val composition: Composition = Composition(applier, recomposer)
    private var isDisposed = false

    init {
        rootComposite.addDisposeListener {
            if (!isDisposed) {
                dispose()
            }
        }
    }

    /**
     * Sets the content of this composition.
     */
    fun setContent(content: @Composable () -> Unit) {
        if (isDisposed) {
            error("ComposeScope is disposed")
        }

        composition.setContent {
            CompositionLocalProvider(
                LocalSWTComposite provides rootComposite,
                LocalComposeScope provides this@ComposeScope,
                LocalSWTNodeApplier provides applier,
                LocalDisplay provides rootComposite.display,
                LocalDisplayDensity provides rootComposite.display.getSweetDensity(),
                LocalComposeManager provides manager,
            ) {
                content()
            }
        }
    }

    /**
     * Gets the root Composite for this scope.
     */
    fun getRootComposite(): Composite = rootComposite

    /**
     * Gets the ComposeManager that owns this scope.
     */
    fun getManager(): ComposeManager = manager

    /**
     * Disposes this ComposeScope and all its resources.
     */
    fun dispose() {
        if (isDisposed) return

        isDisposed = true
        composition.dispose()
        manager.removeScope(rootComposite)
    }

    /**
     * Checks if this ComposeScope is disposed.
     */
    fun isDisposed(): Boolean = isDisposed

    companion object {
        const val COMPOSE_SCOPE_KEY = "sweet.compose.scope"

        /**
         * Gets the ComposeScope associated with the given Composite.
         */
        fun getForComposite(composite: Composite): ComposeScope? {
            return composite.getData(COMPOSE_SCOPE_KEY) as? ComposeScope
        }

        /**
         * Gets the ComposeScope associated with the given Composite, or creates one if none exists.
         */
        fun getOrCreateForComposite(composite: Composite): ComposeScope {
            return getForComposite(composite)
                ?: ComposeManager.getOrCreate(composite.display).createScope(composite)
        }
    }
}
