@file:Suppress("ktlint:standard:no-wildcard-imports", "ktlint:standard:function-naming")

package io.github.ddsimoes.sweet.compose.integration

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.ComposeManager
import io.github.ddsimoes.sweet.compose.ComposeScope
import io.github.ddsimoes.sweet.compose.locals.LocalComposeManager
import io.github.ddsimoes.sweet.compose.locals.LocalDisplay
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

/**
 * Embeds Compose content into an existing SWT Composite.
 * This is the primary integration point for embedding Compose into SWT applications.
 *
 * @param composite The SWT Composite to embed Compose content into
 * @param content The Compose content to embed
 */
fun embedCompose(
    composite: Composite,
    content: @Composable () -> Unit,
) {
    val display = composite.display
    val composeManager = ComposeManager.getOrCreate(display)
    val scope = composeManager.createScope(composite)

    scope.setContent {
        CompositionLocalProvider(
            LocalDisplay provides display,
            LocalComposeManager provides composeManager,
        ) {
            content()
        }
    }
}

/**
 * Embeds an SWT Control into Compose content.
 * This allows native SWT widgets to be used within Compose layouts.
 *
 * @param modifier The modifier to apply to the embedded control
 * @param factory A factory function that creates the SWT Control
 */
@Composable
fun SWTWidget(
    modifier: Modifier = Modifier,
    factory: (Composite) -> Control,
) {
    val applier = LocalSWTNodeApplier.current
    val parentComposite = applier.requireCurrentParent("SWTWidget")

    // Create a container composite for the SWT widget
    val containerComposite =
        remember {
            Composite(parentComposite, SWT.NONE).apply {
                layout = SweetLayout()
            }
        }

    // Create the actual SWT control
    val control = remember { factory(containerComposite) }

    // Handle disposal
    DisposableEffect(control) {
        onDispose {
            if (!control.isDisposed) {
                control.dispose()
            }
            if (!containerComposite.isDisposed) {
                containerComposite.dispose()
            }
        }
    }

    // Apply modifier to the container composite using the standard Sweet modifier pipeline
    SideEffect {
        if (!containerComposite.isDisposed) {
            io.github.ddsimoes.sweet.internal.applySWTModifier(containerComposite, modifier)
        }
    }
}

/**
 * Utility function to get the ComposeScope for a Composite, if one exists.
 *
 * @param composite The Composite to get the scope for
 * @return The ComposeScope for the Composite, or null if none exists
 */
fun getComposeScope(composite: Composite): ComposeScope? = ComposeScope.getForComposite(composite)
