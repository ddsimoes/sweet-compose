@file:Suppress("ktlint:standard:filename")

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.internal.ContextMenuElement
import io.github.ddsimoes.sweet.internal.DoubleClickElement
import io.github.ddsimoes.sweet.internal.FocusChangedElement
import io.github.ddsimoes.sweet.internal.HoverableElement
import io.github.ddsimoes.sweet.internal.SWTModifierElement
import io.github.ddsimoes.sweet.internal.reconcileFocusEvents
import org.eclipse.swt.widgets.Control

// ── Focus change modifier ───────────────────────────────────────────────────

/**
 * Focus-change modifier element. The reconciler installs FocusIn/FocusOut listeners
 * lazily via [reconcileFocusEvents].
 */
internal class FocusChangedModifier(
    private val onFocusChanged: (Boolean) -> Unit,
) : FocusChangedElement {
    override val focusAction: (Boolean) -> Unit get() = onFocusChanged
}

/**
 * Adds focus-change handling to a component.
 *
 * @param onFocusChanged Called with `true` when the component gains focus, `false` when it loses focus.
 */
@Stable
fun Modifier.onFocusChanged(onFocusChanged: (Boolean) -> Unit): Modifier {
    return this.then(FocusChangedModifier(onFocusChanged))
}

// ── Focus requester ──────────────────────────────────────────────────────────

/**
 * An object that can be used to request focus for a component.
 * Must be paired with [Modifier.focusRequester].
 *
 * Matches MPP's [androidx.compose.ui.focus.FocusRequester] shape.
 */
@Stable
class FocusRequester {
    /**
     * Requests focus on the associated control.
     * Has no effect if not yet associated with a control via [Modifier.focusRequester].
     */
    fun requestFocus() {
        associatedControl?.setFocus()
    }

    internal var associatedControl: Control? = null
}

internal class FocusRequesterElement(
    val focusRequester: FocusRequester,
) : SWTModifierElement {
    override fun apply(control: Control) {
        focusRequester.associatedControl = control
    }
}

/**
 * Associates a [FocusRequester] with this component so [FocusRequester.requestFocus]
 * can move focus to it programmatically.
 */
@Stable
fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier {
    return this.then(FocusRequesterElement(focusRequester))
}

// ── Focusable modifier ───────────────────────────────────────────────────────

internal class FocusableElement : SWTModifierElement {
    override fun apply(control: Control) {
        // SWT controls are natively focusable; this is a no-op marker.
    }
}

// ── Hover modifier ───────────────────────────────────────────────────────────

internal class HoverableModifier(
    private val onHover: (Boolean) -> Unit,
) : HoverableElement {
    override val hoverAction: (Boolean) -> Unit get() = onHover
}

/**
 * Adds hover enter/exit handling to a component.
 * Uses SWT MouseEnter/MouseExit listeners.
 *
 * @param onHover Called with `true` on enter, `false` on exit.
 */
@Stable
fun Modifier.hoverable(onHover: (Boolean) -> Unit): Modifier {
    return this.then(HoverableModifier(onHover))
}

/**
 * Makes the component focusable. SWT controls are natively focusable;
 * this modifier is a no-op for standard widgets but serves as a marker
 * for accessibility tooling.
 */
@Stable
fun Modifier.focusable(): Modifier {
    return this.then(FocusableElement())
}

// ── Double-click modifier ──────────────────────────────────────────────────

internal class DoubleClickModifier(
    private val onDoubleClick: () -> Unit,
) : DoubleClickElement {
    override val doubleClickAction: () -> Unit get() = onDoubleClick
}

@Stable
fun Modifier.onDoubleClick(onDoubleClick: () -> Unit): Modifier {
    return this.then(DoubleClickModifier(onDoubleClick))
}

// ── Context menu modifier ──────────────────────────────────────────────────

internal class ContextMenuModifier(
    private val onContextMenu: () -> Unit,
) : ContextMenuElement {
    override val contextMenuAction: () -> Unit get() = onContextMenu
}

@Stable
fun Modifier.onContextMenu(onContextMenu: () -> Unit): Modifier {
    return this.then(ContextMenuModifier(onContextMenu))
}
