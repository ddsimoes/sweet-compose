package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import org.eclipse.swt.widgets.Control

internal interface SWTModifierElement : Modifier.Element {
    /**
     * Single-shot application of this modifier element to a control.
     *
     * The reconciler ([applySWTModifier]) routes recognized elements through dedicated
     * reconciliation functions (background, foreground, clickable, draw, etc.). This method
     * is only invoked for unmodeled/marker elements (scroll config, menu anchor) that reach
     * the [SWTModifierElement] catch-all branch. Defaults to no-op so that recognized
     * elements don't carry dead fallback code.
     */
    fun apply(control: Control) { }
}

/**
 * A modifier element whose effect is a **pure** transform of [SweetCompositionData].
 *
 * Layout-affecting modifiers (fill, size, padding, weight, offset, constraints, per-child
 * alignment) implement this so that [applySWTModifier] can derive the control's full
 * composition data by folding the *whole current chain* over fresh defaults (reset → fold →
 * assign). This makes modifier application idempotent and removal-safe.
 *
 * Folding rules implemented by each modifier:
 * - padding / offset: **additive** (`padding(8).padding(4)` == 12).
 * - size / weight / aspect-ratio / per-child alignment: **last-wins**.
 * - size constraints (`sizeIn`): **intersected** (min = max of mins, max = min of maxes).
 */
internal interface LayoutDataModifier : SWTModifierElement {
    fun applyTo(data: SweetCompositionData): SweetCompositionData

    /**
     * Fallback single-shot application used only when a modifier is applied outside
     * [applySWTModifier]. The reconciling path never calls this; it folds [applyTo] instead.
     */
    override fun apply(control: Control) {
        control.updateSweetCompositionData { applyTo(this) }
    }
}

/**
 * A modifier element that sets a background color override. The color is reconciled with
 * reset-when-absent semantics: when no background element remains in the chain, the
 * modifier-owned background is cleared (back to the SWT default).
 */
internal interface BackgroundColorElement : SWTModifierElement {
    val backgroundColor: Color
}

/** Mirror of [BackgroundColorElement] for foreground (text/content) color. */
internal interface ForegroundColorElement : SWTModifierElement {
    val foregroundColor: Color
}

/** A modifier element that installs a click handler. The action is swapped on each apply and
 * disabled (set to null) when no clickable element remains in the chain. [clickEnabled] is
 * reconciled the same way: stored on the control and re-read on every mouse event. */
internal interface ClickableElement : SWTModifierElement {
    val clickAction: () -> Unit
    val clickEnabled: Boolean get() = true
}

/** A modifier element that records an alignment-line block (stored generically to avoid a
 * dependency on the layout package). */
internal interface AlignByBlockElement : SWTModifierElement {
    val alignByBlockData: Any
}

/** A modifier element that contributes a draw callback for [reconcileDraw] to install
 * as a PaintListener on the owning [Composite]. Multiple elements are chained in order. */
internal interface DrawModifierElement : SWTModifierElement {
    val drawCallback: DrawScope.() -> Unit
}

/** A modifier element that clips the widget to a [Shape] via SWT [Region]. */
internal interface ClipModifierElement : SWTModifierElement {
    val shape: Shape
}

/** A modifier element that tracks focus gain/loss on a control. */
internal interface FocusChangedElement : SWTModifierElement {
    val focusAction: (Boolean) -> Unit
}

/** A modifier element that handles double-click events on a control. */
internal interface DoubleClickElement : SWTModifierElement {
    val doubleClickAction: () -> Unit
}

/** A modifier element that handles right-click / context menu events on a control. */
internal interface ContextMenuElement : SWTModifierElement {
    val contextMenuAction: () -> Unit
}

/** A modifier element that tracks hover enter/exit on a control. */
internal interface HoverableElement : SWTModifierElement {
    val hoverAction: (Boolean) -> Unit
}
