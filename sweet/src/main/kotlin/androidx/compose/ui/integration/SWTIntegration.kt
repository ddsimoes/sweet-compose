@file:Suppress("ktlint:standard:function-naming", "MatchingDeclarationName")

package androidx.compose.ui.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.ddsimoes.sweet.compose.ComposeScope
import io.github.ddsimoes.sweet.compose.locals.LocalDisplay
import io.github.ddsimoes.sweet.compose.locals.LocalSWTComposite
import org.eclipse.swt.widgets.Composite

/**
 * Public interface for embedded compositions
 */
interface CompositionHandle {
    fun dispose()
}

/**
 * Extension function to embed Compose content within an existing SWT Composite.
 * This enables SWT -> Compose integration for deep intercalation scenarios.
 *
 * Example usage:
 * ```
 * // In SWT code
 * val swtComposite = Composite(parent, SWT.NONE)
 * swtComposite.embedCompose {
 *     Column {
 *         Text("Embedded Compose Content")
 *         Button("Compose Button") { }
 *     }
 * }
 * ```
 *
 * @param content The composable content to embed within this composite
 * @return EmbeddedComposition handle for lifecycle management
 */
fun Composite.embedCompose(content: @Composable () -> Unit): CompositionHandle {
    val parentComposite = this
    val display = this.display

    // Create or reuse a ComposeScope for this Composite via the
    // Display-scoped ComposeManager. This ensures embedded content
    // participates in the same recomposer / frame clock pipeline as
    // top-level windows.
    val scope = ComposeScope.getOrCreateForComposite(parentComposite)

    val wrappedContent: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalDisplay provides display,
            LocalSWTComposite provides parentComposite,
        ) {
            content()
        }
    }

    scope.setContent(wrappedContent)

    return object : CompositionHandle {
        override fun dispose() {
            scope.dispose()
        }
    }
}

/**
 * Composable version that handles lifecycle automatically within a composable context.
 * Use this when embedding Compose content from within another composable.
 */
@Composable
fun Composite.EmbedCompose(
    content: @Composable () -> Unit,
) {
    val composition =
        remember(this) {
            embedCompose(content)
        }

    DisposableEffect(this) {
        onDispose {
            composition.dispose()
        }
    }
}
