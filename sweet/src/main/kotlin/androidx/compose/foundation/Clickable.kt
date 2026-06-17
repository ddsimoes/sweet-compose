package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.github.ddsimoes.sweet.internal.ClickableElement
import io.github.ddsimoes.sweet.internal.SWTModifierElement
import org.eclipse.swt.widgets.Control

// ── Clickable modifier ──────────────────────────────────────────────────────

/**
 * Clickable modifier that handles mouse click events.
 *
 * The listener + control-scoped coroutine scope are installed once by the reconciler
 * ([applySWTModifier]); this element only carries the current [onClick] action and [enabled]
 * state, which the reconciler swaps on every apply and disables when the modifier is removed.
 */
internal class ClickableModifier(
    private val onClick: () -> Unit,
    private val enabled: Boolean,
) : ClickableElement {
    override val clickAction: () -> Unit get() = onClick
    override val clickEnabled: Boolean get() = enabled
}

/**
 * Makes a component clickable and responds to click events.
 *
 * Signature mirrors MPP `Modifier.clickable(enabled, onClickLabel, role, onClick)`.
 * [onClickLabel] and [role] are accepted for API compatibility; SWT native widgets carry
 * their own platform accessibility metadata, so they are not consumed yet.
 *
 * @param enabled whether the click handler responds to input. When false, presses are
 *   ignored; the value is re-read on every event, so toggling it via recomposition works.
 */
@Stable
fun Modifier.clickable(
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") onClickLabel: String? = null,
    @Suppress("UNUSED_PARAMETER") role: Role? = null,
    onClick: () -> Unit,
): Modifier {
    return this.then(ClickableModifier(onClick, enabled))
}

// ── Menu anchor modifier ────────────────────────────────────────────────────

/**
 * Menu anchor modifier stub for dropdown menu positioning.
 *
 * This is currently a no-op compatibility shim; Sweet's menu system does not
 * yet use this flag to position dropdowns. It may become functional in a
 * future version once menu anchoring is implemented.
 */
internal object MenuAnchorModifier : SWTModifierElement {
    override fun apply(control: Control) {
        if (io.github.ddsimoes.sweet.debug.SweetDebugger.assertionEnabled) {
            io.github.ddsimoes.sweet.debug.SweetDebugger.log(
                "MenuAnchorModifier",
                "menuAnchor() is currently a no-op; dropdown positioning is not yet implemented.",
            )
        }
    }
}

/**
 * Marks a component as a menu anchor for dropdown positioning
 */
@Stable
fun Modifier.menuAnchor(): Modifier {
    return this.then(MenuAnchorModifier)
}
