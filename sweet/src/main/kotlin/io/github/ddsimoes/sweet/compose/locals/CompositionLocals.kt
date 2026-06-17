package io.github.ddsimoes.sweet.compose.locals

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Density
import io.github.ddsimoes.sweet.compose.ComposeManager
import io.github.ddsimoes.sweet.compose.ComposeScope
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import kotlin.system.exitProcess

/**
 * CompositionLocal for accessing the current SWT Composite.
 * This provides access to the parent Composite where Compose content is being rendered.
 */
val LocalSWTComposite =
    compositionLocalOf<Composite> {
        error("No SWT Composite provided. Make sure you are inside a Compose composition with SWT integration.")
    }

/**
 * CompositionLocal for accessing the current Display.
 * This provides access to the SWT Display instance.
 */
val LocalDisplay =
    compositionLocalOf<Display> {
        error("No SWT Display provided. Make sure you are inside a Compose composition with SWT integration.")
    }

/**
 * CompositionLocal for accessing the current ComposeScope.
 * This provides access to the ComposeScope managing the current composition.
 */
val LocalComposeScope =
    compositionLocalOf<ComposeScope> {
        error("No ComposeScope provided. Make sure you are inside a Compose composition with SWT integration.")
    }

/**
 * CompositionLocal for accessing the active SWT node applier.
 * SWT controls must use this scoped applier for parent lookup instead of process-global state.
 */
internal val LocalSWTNodeApplier =
    compositionLocalOf<SWTNodeApplier> {
        error("No SWTNodeApplier provided. Make sure you are inside a Compose composition with SWT integration.")
    }

/**
 * CompositionLocal for accessing the display density associated with the current composition.
 */
internal val LocalDisplayDensity =
    compositionLocalOf<Density> {
        Density.Default
    }

/**
 * CompositionLocal for accessing the current ComposeManager.
 * This provides access to the ComposeManager for the current Display.
 */
val LocalComposeManager =
    compositionLocalOf<ComposeManager> {
        error("No ComposeManager provided. Make sure you are inside a Compose composition with SWT integration.")
    }

/**
 * CompositionLocal for accessing the current ApplicationScope.
 * This provides access to application-level operations.
 */
val LocalApplicationScope =
    compositionLocalOf<ApplicationScope> {
        error("No ApplicationScope provided. Make sure you are inside an application composition.")
    }

/**
 * CompositionLocal for accessing the current WindowScope.
 * This provides access to window-level operations.
 */
val LocalWindowScope =
    compositionLocalOf<WindowScope> {
        error("No WindowScope provided. Make sure you are inside a Window composition.")
    }

/**
 * Scope for application-level operations.
 */
interface ApplicationScope {
    val display: Display
    val manager: ComposeManager

    /**
     * Close all windows created inside the application and cancel all launched effects
     * (they launch via [LaunchedEffect] and [rememberCoroutineScope]).
     */
    fun exitApplication()
}

/**
 * Scope for window-level operations.
 */
interface WindowScope {
    val window: Shell
}

/**
 * Implementation of ApplicationScope.
 */
class ApplicationScopeImpl(
    override val display: Display,
    override val manager: ComposeManager,
) : ApplicationScope {
    override fun exitApplication() {
        exitProcess(0)
    }
}

/**
 * Implementation of WindowScope.
 */
class WindowScopeImpl(
    override val window: Shell,
) : WindowScope
