@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.SWTContainerNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import org.eclipse.swt.widgets.Composite

/**
 * SWTContainer creates a composition boundary where pure SWT code can be executed.
 * This enables Compose -> SWT -> Compose intercalation.
 *
 * Example usage:
 * ```
 * SWTContainer { parent ->
 *     // Pure SWT code here
 *     val group = Group(parent, SWT.NONE)
 *     group.text = "SWT Group"
 *
 *     // Can embed more Compose content within SWT
 *     val composeArea = Composite(group, SWT.NONE)
 *     composeArea.embedCompose {
 *         Button("Nested Compose Button") { }
 *     }
 * }
 * ```
 *
 * **Sweet note:** This API is Sweet-specific and does not exist in standard Compose
 * Multiplatform. It provides an escape hatch for direct SWT widget manipulation within
 * a Compose tree.
 */
@Composable
fun SWTContainer(
    modifier: Modifier = Modifier,
    content: (parent: Composite) -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<SWTContainerNode, SWTNodeApplier>(
        factory = {
            SWTContainerNode(
                parentApplier = applier,
                content = content,
                modifier = modifier,
            )
        },
        update = {
            set(modifier) { this.modifier = it }
            set(content) {
                this.content = it
                // Re-materialize content if needed
                if (!this.control.isDisposed) {
                    this.control.children.forEach { it.dispose() }
                    materialize()
                    this.control.layout()
                }
            }
        },
    )
}
