package io.github.ddsimoes.sweet.internal

import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.layout.Constraints
import io.github.ddsimoes.sweet.layout.LayoutSpec
import io.github.ddsimoes.sweet.layout.Size
import org.eclipse.swt.widgets.Control

/**
 * Shadow layout tree node. Built and maintained by [SWTNodeApplier] alongside the
 * existing Compose node tree. In M0 (this commit): structure-only, no measure/place
 * pass runs. In M1: drives layout via [layoutPass].
 *
 * Each node corresponds to a Compose node that owns a [Control] (or a virtual
 * grouping in the future per doc 14).
 */
internal class SweetLayoutNode(
    /** The SWT control this node represents, or null for virtual nodes. */
    val control: Control?,
    /**
     * [LayoutSpec] from the Compose node's factory; set by the applier when the node
     * is created for a Row/Column/Box. Null for leaf widgets.
     */
    var spec: LayoutSpec?,
    /**
     * Layout-affecting modifier data. Set by [applySWTModifier] during recomposition.
     * Stored directly until doc 12 introduces ordered modifier chains.
     */
    var modifierData: SweetCompositionData,
) {
    /** Parent in the shadow tree. Root has parent == null. */
    var parent: SweetLayoutNode? = null

    /** Children in Compose composition order (not z-order). */
    val children = mutableListOf<SweetLayoutNode>()

    // ── Measurement state (used from M1 onward) ────────────────────────

    /** Last measured size; invalidated by [needsMeasure]. */
    var measuredSize: Size = Size.Zero

    /** Last constraints used for measurement; cache key for [needsMeasure]. */
    var lastConstraints: Constraints? = null

    /** Set to true when the node's content or modifier chain changed measured size. */
    var needsMeasure: Boolean = true

    /** Set to true when position changed (child re-order, parent resize). */
    var needsPlace: Boolean = true

    /** Re-entrancy guard — set during [layoutPass] on the root. */
    var inPass: Boolean = false

    // ── Tree maintenance ───────────────────────────────────────────────

    fun addChild(
        child: SweetLayoutNode,
        index: Int = children.size,
    ) {
        children.add(index, child)
        child.parent = this
    }

    fun removeChild(child: SweetLayoutNode) {
        children.remove(child)
        child.parent = null
    }

    fun removeChildren(
        index: Int,
        count: Int,
    ) {
        children.subList(index, index + count).forEach { it.parent = null }
        children.subList(index, index + count).clear()
    }

    /** Walk up to the root, calling [block] on each ancestor. */
    fun walkAncestors(block: (SweetLayoutNode) -> Unit) {
        var node: SweetLayoutNode? = parent
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    override fun toString(): String =
        "SweetLayoutNode(ctrl=${control?.javaClass?.simpleName}[${control?.hashCode()?.toString(16)}], spec=$spec, children=${children.size})"
}
