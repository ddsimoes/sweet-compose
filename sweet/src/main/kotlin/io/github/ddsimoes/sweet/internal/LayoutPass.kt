@file:Suppress("MatchingDeclarationName")

package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.Alignment
import io.github.ddsimoes.sweet.layout.Constraints
import io.github.ddsimoes.sweet.layout.LayoutSpec
import io.github.ddsimoes.sweet.layout.SWTControlMeasurable
import io.github.ddsimoes.sweet.layout.Size
import io.github.ddsimoes.sweet.layout.delegate.LayoutDelegateRegistry
import io.github.ddsimoes.sweet.layout.delegate.MeasureBundle
import org.eclipse.swt.graphics.Point

/**
 * Wraps a [SweetLayoutNode] as a measurable for the shadow layout pass.
 *
 * It **extends** [SWTControlMeasurable] so that the full modifier pipeline (padding,
 * offset, fillMax, fixed size, sizeIn, aspect ratio) is applied uniformly to every node —
 * containers and leaves alike. The only thing it overrides is the *natural content size*
 * source: a container measures its children through the appropriate layout delegate
 * instead of querying SWT's `computeSize`. Leaves fall back to the inherited SWT behavior.
 *
 * Placement reuses [SWTControlMeasurable.place] (padding/offset inset, single `setBounds`)
 * and then recurses into container children via [placeChildren]. Child coordinates are
 * parent-relative — each container's children are positioned within that container's own
 * client area, matching SWT's setBounds semantics for nested composites.
 */
internal class NodeMeasurable(
    val node: SweetLayoutNode,
    /**
     * True only for the root of a [layoutPass]. A spec-less node that still has shadow
     * children (e.g. the embedCompose boundary, or a scroll content composite) acts as a
     * layout container *only* when it is the pass root; reached as a descendant of another
     * pass it is an opaque island measured via SWT `computeSize` and never recursed into
     * (it drives its own independent pass).
     */
    private val isRoot: Boolean = false,
) : SWTControlMeasurable(node.control ?: error("NodeMeasurable requires a non-null control")) {
    /** Whether this node lays out its children via a delegate (vs. being an opaque leaf/island). */
    private val isContainer: Boolean get() = node.spec != null || (isRoot && node.children.isNotEmpty())

    /** Measured size is stored on the shadow node so SweetLayout.computeSize can read it. */
    override var measuredSize: Size
        get() = node.measuredSize
        set(value) {
            node.measuredSize = value
        }

    /** Delegate result from the most recent [measureNaturalContent]; consumed by [placeChildren]. */
    private var bundle: MeasureBundle? = null
    private var childMeasurables: List<NodeMeasurable> = emptyList()

    override fun measureNaturalContent(constraints: Constraints): Point {
        if (isContainer) {
            val delegate = LayoutDelegateRegistry.forSpec(node.spec)
            val effectiveSpec = node.spec ?: LayoutSpec.Box(Alignment.TopStart)
            val children = node.children.map { NodeMeasurable(it) }
            val b = delegate.measure(children, constraints, effectiveSpec)
            bundle = b
            childMeasurables = children
            return Point(b.parentSize.width, b.parentSize.height)
        }
        bundle = null
        childMeasurables = emptyList()
        return super.measureNaturalContent(constraints)
    }

    /**
     * Places this node's children relative to its client area, then recurses.
     * Must be called after this node has been measured.
     *
     * [originX]/[originY] shift every child by the container's content origin. This is
     * non-zero only for the **pass root**, whose own control is not inset by a Sweet
     * parent (it is filled by a foreign layout or is the boundary), so its padding must
     * be realised by offsetting its children. Interior containers are already inset by
     * their parent's [SWTControlMeasurable.place], so their children use origin (0,0).
     */
    fun placeChildren(
        originX: Int = 0,
        originY: Int = 0,
    ) {
        val b = bundle ?: return
        val effectiveSpec = node.spec ?: LayoutSpec.Box(Alignment.TopStart)
        val delegate = LayoutDelegateRegistry.forSpec(node.spec)
        delegate.place(childMeasurables, b.parentSize.width, b.parentSize.height, effectiveSpec, b)
        if (originX != 0 || originY != 0) {
            childMeasurables.forEach { cm ->
                val ctrl = cm.control
                if (!ctrl.isDisposed) {
                    val r = ctrl.bounds
                    ctrl.setBounds(r.x + originX, r.y + originY, r.width, r.height)
                }
            }
        }
        childMeasurables.forEach { it.placeChildren() }
    }
}

/**
 * Runs the full measure + place pass rooted at [root].
 *
 * The root's own control bounds are owned by the SWT host and are left untouched — only
 * the root is measured (to answer `computeSize`) and its descendant controls are placed.
 *
 * Not re-entrant on the same root — guarded by [SweetLayoutNode.inPass]. When SWT's layout
 * protocol recursively re-enters on the same root (computeSize → setBounds → layout), the
 * re-entrant call returns immediately and the outer pass result stands.
 */
internal fun layoutPass(
    root: SweetLayoutNode,
    constraints: Constraints,
) {
    if (root.inPass) return
    root.inPass = true
    try {
        val measurable = NodeMeasurable(root, isRoot = true)
        measurable.measure(constraints)
        // The pass root's control is not inset by a Sweet parent, so realise its own
        // padding by offsetting its children into the content box. Read chain-first
        // (with flat-field fallback) like every other placement path.
        val ctrl = root.control
        val originX = ctrl?.chainPaddingStart ?: 0
        val originY = ctrl?.chainPaddingTop ?: 0
        measurable.placeChildren(originX, originY)
    } finally {
        root.inPass = false
    }
}
