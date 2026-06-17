@file:Suppress("MatchingDeclarationName")

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import io.github.ddsimoes.sweet.internal.ClipModifierElement
import org.eclipse.swt.widgets.Control

// ── Clip modifier ───────────────────────────────────────────────────────────

/**
 * Clips the control to [shape] via SWT [Region].
 */
internal class ClipModifier(
    override val shape: Shape,
) : ClipModifierElement {
    override fun apply(control: Control) {
        // Handled by [reconcileClip].
    }
}

/**
 * Clip the composable and its children to [shape] via SWT [Region] clipping.
 */
@Stable
fun Modifier.clip(shape: Shape): Modifier {
    return this.then(ClipModifier(shape))
}
