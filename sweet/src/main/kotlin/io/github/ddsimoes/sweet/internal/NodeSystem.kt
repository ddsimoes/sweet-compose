@file:Suppress("UnusedParameter")

package io.github.ddsimoes.sweet.internal

import androidx.compose.runtime.AbstractApplier
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.data.sweetLayoutSpec
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.layout.LayoutCoordinator
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import java.util.ConcurrentModificationException

/**
 * Base interface for all Sweet Compose nodes
 */
internal sealed interface SweetNode {
    val control: Control?
    val layoutNode: SweetLayoutNode?

    fun dispose()
}

/**
 * Regular SWT Control node
 */
internal class DirectSWTNode(
    override val control: Control,
) : SweetNode {
    override var layoutNode: SweetLayoutNode? = null

    override fun dispose() {
        if (!control.isDisposed) control.dispose()
    }
}

/**
 * Container node that creates composition boundaries for SWT -> Compose -> SWT intercalation
 */
internal class SWTContainerNode(
    parentApplier: SWTNodeApplier,
    var content: (Composite) -> Unit,
    var modifier: Modifier = Modifier,
) : SweetNode {
    override val control: Composite = createComposite(parentApplier.requireCurrentParent("SWTContainer"))
    override var layoutNode: SweetLayoutNode? = null
    private var isContentSet = false

    init {
        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "SWTContainerNode",
                "Created container: ${control.javaClass.simpleName}[${control.hashCode().toString(16)}]",
            )
        }
    }

    fun materialize() {
        if (!isContentSet && !control.isDisposed) {
            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log(
                    "SWTContainerNode",
                    "Materializing content for container: ${control.javaClass.simpleName}[${
                        control.hashCode().toString(16)
                    }]",
                )
            }
            content(control)
            isContentSet = true
        }
    }

    override fun dispose() {
        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "SWTContainerNode",
                "Disposing container: ${control.javaClass.simpleName}[${control.hashCode().toString(16)}]",
            )
        }
        if (!control.isDisposed) {
            control.dispose()
        }
    }

    private fun createComposite(parent: Composite): Composite =
        Composite(parent, SWT.NONE).apply {
            // No SWT layout data; this container is managed by Compose
            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log(
                    "SWTContainerNode",
                    "Composite created for container: ${this.javaClass.simpleName}[${this.hashCode().toString(16)}]",
                )
            }
        }
}

internal class SWTNodeApplier(
    private val rootComposite: Composite,
) : AbstractApplier<SweetNode>(DirectSWTNode(rootComposite)) {
    val shadowRoot: SweetLayoutNode = SweetLayoutNode(rootComposite, null, SweetCompositionData())

    init {
        // Publish the shadow root on the boundary composite so that, when the boundary itself
        // carries a SweetLayout (the windowed path, where the boundary's SWT parent is a Shell
        // rather than a foreign layout), SweetLayout.layout can locate the root node and drive
        // the pass. In the embedCompose+FillLayout path the boundary never runs SweetLayout, so
        // this is inert there; child linking already falls back to shadowRoot regardless.
        try {
            rootComposite.setData("sweet.m1.layoutNode", shadowRoot)
        } catch (_: ConcurrentModificationException) {
        }
    }

    private val dirtyComposites = mutableSetOf<Composite>()

    private fun markCompositeDirty(composite: Composite?) {
        if (composite != null && !composite.isDisposed) {
            dirtyComposites.add(layoutRequestTarget(composite))
        }
    }

    private fun layoutRequestTarget(composite: Composite): Composite {
        var target = composite
        while (target.layout !is SweetLayout) {
            val parent = target.parent as? Composite ?: break
            if (parent.isDisposed) break
            target = parent
        }
        return target
    }

    private fun childHostComposite(node: SweetNode): Composite? =
        when (node) {
            is DirectSWTNode -> node.control as? Composite
            is SWTContainerNode -> node.control
            is SWTScrollableNode -> node.contentComposite
        }

    private fun shadowParentFor(node: SweetNode): SweetLayoutNode =
        childHostComposite(node)?.getData("sweet.m1.layoutNode") as? SweetLayoutNode ?: shadowRoot

    private fun <T> MutableList<T>.moveRange(
        from: Int,
        to: Int,
        count: Int,
    ) {
        if (from == to || (count == 1 && to == size && from == to - 1)) return
        val dest = if (from > to) to else to - count
        if (count == 1) {
            if (from == to + 1 || from == to - 1) {
                val fromElement = get(from)
                val toElement = set(to, fromElement)
                set(from, toElement)
            } else {
                val fromElement = removeAt(from)
                add(dest, fromElement)
            }
        } else {
            val subView = subList(from, from + count)
            val subCopy = subView.toMutableList()
            subView.clear()
            addAll(dest, subCopy)
        }
    }

    private fun reorderNativeChildren(
        parentComposite: Composite,
        desiredOrder: List<Control>,
    ) {
        var anchor: Control? = null
        desiredOrder.asReversed().forEach { control ->
            if (!control.isDisposed) {
                if (anchor == null) {
                    control.moveBelow(null)
                } else {
                    control.moveAbove(anchor)
                }
                anchor = control
            }
        }
    }

    override fun insertTopDown(
        index: Int,
        instance: SweetNode,
    ) {
        // Ensure the layoutNode is created BEFORE children are composed
        // so that children can find their parent's layoutNode via getData.
        ensureLayoutNode(instance)
    }

    override fun insertBottomUp(
        index: Int,
        instance: SweetNode,
    ) {
        val current = current
        if (SweetDebugger.assertionEnabled) {
            if (index < 0) {
                throw AssertionError("SWTNodeApplier.insertBottomUp: Invalid index $index (must be >= 0)")
            }

            // Validate that parent is a valid container
            val parentControl =
                when (current) {
                    is DirectSWTNode -> current.control as? Composite
                    is SWTContainerNode -> current.control
                    is SWTScrollableNode -> current.contentComposite
                }
            if (parentControl == null) {
                throw AssertionError(
                    "SWTNodeApplier.insertBottomUp: Parent ${current::class.simpleName} is not a valid container for child insertion",
                )
            }

            // Validate that the instance has a valid control
            val instanceControl =
                instance.control
                    ?: throw AssertionError("SWTNodeApplier.insertBottomUp: Instance ${instance::class.simpleName} has null control")

            // Validate that the instance's parent matches our composite
            if (instanceControl.parent != parentControl) {
                throw AssertionError(
                    "SWTNodeApplier.insertBottomUp: Instance control parent mismatch. " +
                        "Expected parent: ${parentControl.javaClass.simpleName}[${
                            parentControl.hashCode().toString(16)
                        }], " +
                        "but instance parent is: ${instanceControl.parent?.javaClass?.simpleName}[${
                            instanceControl.parent?.hashCode()?.toString(16)
                        }]",
                )
            }
        }

        when (instance) {
            is DirectSWTNode -> {
                // Regular SWT control - position at correct index
                val currentControl =
                    when (current) {
                        is DirectSWTNode -> current.control as? Composite
                        is SWTContainerNode -> current.control
                        is SWTScrollableNode -> current.contentComposite
                    }
                currentControl?.let { composite ->
                    val childrenBefore = composite.children.size
                    val instanceControl = instance.control

                    // Position the widget at the correct index using SWT's moveAbove/moveBelow
                    if (index < childrenBefore) {
                        val targetControl = composite.children[index]
                        if (instanceControl != targetControl) {
                            instanceControl.moveAbove(targetControl)
                        }
                    } else if (index > 0) {
                        // Position at end - move below the last widget
                        if (childrenBefore > 0) {
                            instanceControl.moveBelow(composite.children[childrenBefore - 1])
                        }
                    }

                    // Defer layout updates to onEndChanges
                    markCompositeDirty(composite)

                    val childrenAfter = composite.children.size
                    if (SweetDebugger.assertionEnabled) {
                        // ASSERTION: Validate that children count is consistent
                        if (childrenAfter != childrenBefore) {
                            SweetDebugger.log(
                                "SWTNodeApplier",
                                "WARNING: Children count changed during layout from $childrenBefore to $childrenAfter",
                            )
                        }
                    }
                }
            }

            is SWTContainerNode -> {
                // Container node - materialize SWT content
                val currentControl =
                    when (current) {
                        is DirectSWTNode -> current.control as? Composite
                        is SWTContainerNode -> current.control
                        is SWTScrollableNode -> current.control as? Composite
                    }
                currentControl?.let { composite ->
                    val childrenBefore = composite.children.size

                    instance.materialize()
                    // Defer layout updates to onEndChanges
                    markCompositeDirty(composite)
                    // Also mark parent chain so outer composites get relaid out
                    var parent = composite.parent
                    while (parent is Composite && !parent.isDisposed) {
                        markCompositeDirty(parent)
                        parent = parent.parent
                    }

                    val childrenAfter = composite.children.size
                    if (SweetDebugger.assertionEnabled) {
                        SweetDebugger.log(
                            "SWTNodeApplier",
                            "Container materialized and layout adjusted. Children: $childrenBefore -> $childrenAfter",
                        )

                        // ASSERTION: For container nodes, materialization might add children
                        // We just log this for now but could add more specific validation later
                        if (childrenAfter < childrenBefore) {
                            throw AssertionError(
                                "SWTNodeApplier.insertBottomUp: Container materialization reduced children count from $childrenBefore to $childrenAfter, this should not happen",
                            )
                        }
                    }
                }
            }

            is SWTScrollableNode -> {
                // Scrollable node inserted: defer the viewport/content layout to onEndChanges
                val scNode = instance
                val content = scNode.contentComposite
                markCompositeDirty(content)

                // Also mark parent composites so layout propagates up
                var parent: Composite? = content.parent as? Composite
                while (parent != null && !parent.isDisposed) {
                    markCompositeDirty(parent)
                    parent = parent.parent as? Composite
                }

                if (SweetDebugger.assertionEnabled) {
                    SweetDebugger.log(
                        "SWTNodeApplier",
                        "Scrollable node inserted; content laid out and minSize updated",
                    )
                }
            }
        }

        // ── M0: Shadow tree maintenance ────────────────────────────────────────
        ensureLayoutNode(instance)
        // Find parent via SWT widget tree (control.parent), NOT via `current`
        val parentControl = instance.control?.parent
        val parentLayoutNode = (parentControl as? Composite)?.getData("sweet.m1.layoutNode") as? SweetLayoutNode
        val effectiveParent = parentLayoutNode ?: shadowRoot
        instance.layoutNode?.let { childNode ->
            if (childNode.parent == null) {
                effectiveParent.addChild(childNode, index.coerceAtMost(effectiveParent.children.size))
                nullInteriorLayout(childNode)
            }
        }
    }

    private fun nullInteriorLayout(childNode: SweetLayoutNode) {
        val composite = childNode.control as? Composite ?: return
        if (childNode.parent === shadowRoot) return
        if (composite.layout is SweetLayout) {
            composite.layout = null
        }
    }

    override fun remove(
        index: Int,
        count: Int,
    ) {
        val currentNode = current

        // Get the SWT composite that holds the child widgets
        val parentComposite =
            when (currentNode) {
                is DirectSWTNode -> currentNode.control as? Composite
                is SWTContainerNode -> currentNode.control
                is SWTScrollableNode -> currentNode.contentComposite
            }

        // Capture children count before removal for assertion
        var childrenBefore = 0

        // ASSERTION: Validate removal parameters before operation
        parentComposite?.let { composite ->
            childrenBefore = composite.children.size
            if (SweetDebugger.assertionEnabled) {
                // Validate removal bounds
                if (index < 0) {
                    throw AssertionError("SWTNodeApplier.remove: Invalid index $index (must be >= 0)")
                }
                if (count < 0) {
                    throw AssertionError("SWTNodeApplier.remove: Invalid count $count (must be >= 0)")
                }
                if (index + count > childrenBefore) {
                    throw AssertionError(
                        "SWTNodeApplier.remove: Removal range [$index, ${index + count}) exceeds children count $childrenBefore",
                    )
                }
            }
        }

        // Remove and dispose the actual SWT child widgets
        parentComposite?.let { composite ->
            val swtChildren = composite.children
            // Use reverse iteration to avoid index shifting issues
            for (i in (count - 1) downTo 0) {
                val childIndex = index + i
                if (childIndex < swtChildren.size) {
                    swtChildren[childIndex].dispose()
                }
            }
        }

        // Update layout if current is a composite
        val composite =
            when (currentNode) {
                is DirectSWTNode -> currentNode.control as? Composite
                is SWTContainerNode -> currentNode.control
                is SWTScrollableNode -> currentNode.control as? Composite
            }

        composite?.let {
            // Defer layout updates to onEndChanges
            markCompositeDirty(it)
            // Also mark parent composites so layout propagates up
            var parent = it.parent
            while (parent is Composite && !parent.isDisposed) {
                markCompositeDirty(parent)
                parent = parent.parent
            }

            val childrenAfter = it.children.size
            if (SweetDebugger.assertionEnabled) {
                // ASSERTION: Validate that the correct number of children were removed
                val expectedChildrenAfter = childrenBefore - count
                if (childrenAfter != expectedChildrenAfter) {
                    val actualRemoved = childrenBefore - childrenAfter
                    throw AssertionError(
                        "SWTNodeApplier.remove: Expected to remove $count children but actually removed $actualRemoved. " +
                            "Before: $childrenBefore, After: $childrenAfter, Expected after: $expectedChildrenAfter. " +
                            "This indicates a bug in the removal logic where widgets are not being properly disposed or the array is not being updated correctly.",
                    )
                }
            }
        }

        // ── M0: Shadow tree removal ───────────────────────────────────────────
        val parentLayoutNode = shadowParentFor(currentNode)
        if (index < parentLayoutNode.children.size) {
            val removeCount = count.coerceAtMost(parentLayoutNode.children.size - index)
            parentLayoutNode.removeChildren(index, removeCount)
        }
    }

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) {
        // Get the parent composite that contains the child widgets
        val currentNode = current
        val composite = childHostComposite(currentNode)

        composite?.let { parentComposite ->
            val children = parentComposite.children.toList()

            // Validate bounds
            if (from < 0 || to < 0 || to > children.size || from + count > children.size || count <= 0) {
                if (SweetDebugger.assertionEnabled) {
                    SweetDebugger.log(
                        "SWTNodeApplier",
                        "Invalid move parameters: from=$from, to=$to, count=$count, childrenSize=${children.size}",
                    )
                }
                return
            }

            val desiredOrder = children.toMutableList()
            desiredOrder.moveRange(from, to, count)
            reorderNativeChildren(parentComposite, desiredOrder)

            // Defer layout updates to onEndChanges
            markCompositeDirty(parentComposite)
            // Also mark parent composites so layout propagates up
            var parent = parentComposite.parent
            while (parent is Composite && !parent.isDisposed) {
                markCompositeDirty(parent)
                parent = parent.parent
            }
        }

        val parentLayoutNode = shadowParentFor(currentNode)
        val canMoveShadowChildren =
            from >= 0 &&
                to >= 0 &&
                to <= parentLayoutNode.children.size &&
                from + count <= parentLayoutNode.children.size &&
                count > 0
        if (canMoveShadowChildren) {
            parentLayoutNode.children.moveRange(from, to, count)
        }
    }

    override fun onClear() {
        // When the composition is cleared, dispose all children of the root composite
        // to ensure SWT resources are released and no stale widgets remain attached.
        if (!rootComposite.isDisposed) {
            val children = rootComposite.children
            children.forEach { child ->
                if (!child.isDisposed) {
                    try {
                        child.dispose()
                    } catch (_: Exception) {
                        // Best-effort cleanup; ignore disposal errors.
                    }
                }
            }
        }

        // Reset dirty state so no deferred layout work runs after clear.
        dirtyComposites.clear()
        shadowRoot.removeChildren(0, shadowRoot.children.size)
    }

    override fun onEndChanges() {
        super.onEndChanges()
        if (dirtyComposites.isEmpty()) return

        // All Applier callbacks run on the SWT UI thread via the SWT dispatcher,
        // so it is safe to call SWT layout APIs (or coordinator) directly here.
        val composites = dirtyComposites.toList()
        dirtyComposites.clear()

        composites.forEach { composite ->
            if (!composite.isDisposed) {
                LayoutCoordinator.forDisplay(composite.display)
                    .requestLayout(composite)
            }
        }
    }

    // Provide current parent for scoped factory functions
    private fun getCurrentParentOrNull(): Composite? =
        when (val currentNode = current) {
            is DirectSWTNode -> currentNode.control as? Composite
            is SWTContainerNode -> currentNode.control
            is SWTScrollableNode -> currentNode.contentComposite
        }

    fun requireCurrentParent(factoryName: String): Composite {
        val parent =
            getCurrentParentOrNull()
                ?: throw IllegalStateException(
                    "$factoryName cannot materialize an SWT control without an active Composite parent. " +
                        "Use it from a Sweet composition that provides LocalSWTNodeApplier.",
                )

        if (parent.isDisposed) {
            error("$factoryName cannot materialize an SWT control under a disposed parent")
        }

        return parent
    }

    // ── M0: Shadow tree helpers ─────────────────────────────────────────────

    private fun storeLayoutNode(
        control: Control,
        ln: SweetLayoutNode,
    ) {
        try {
            control.setData("sweet.m1.layoutNode", ln)
        } catch (_: ConcurrentModificationException) {
        }
    }

    private fun ensureLayoutNode(node: SweetNode) {
        if (node.layoutNode != null) return
        when (node) {
            is DirectSWTNode -> {
                val ln = SweetLayoutNode(node.control, node.control.sweetLayoutSpec, SweetCompositionData())
                node.layoutNode = ln
                storeLayoutNode(node.control, ln)
            }
            is SWTContainerNode -> {
                val ln = SweetLayoutNode(node.control, node.control.sweetLayoutSpec, SweetCompositionData())
                node.layoutNode = ln
                storeLayoutNode(node.control, ln)
            }
            is SWTScrollableNode -> {
                // The outer-facing node is the SCROLLER (the real SWT child of the parent
                // container), treated as an opaque island leaf: the outer pass measures it via
                // computeSize and places it with a single setBounds. Its modifiers (fillMaxSize
                // etc.) live on the scroller per ScrollContainer.
                val scrollerLn = SweetLayoutNode(node.control, node.control.sweetLayoutSpec, SweetCompositionData())
                node.layoutNode = scrollerLn
                storeLayoutNode(node.control, scrollerLn)
                // The content composite is a SEPARATE inner pass root (its SWT parent is the
                // ScrollViewport, not a SweetLayout, so SweetLayout.layout drives it directly).
                // Inner Sweet children link to this node via getData on the content composite.
                if (node.contentComposite.getData("sweet.m1.layoutNode") == null) {
                    val contentLn =
                        SweetLayoutNode(
                            node.contentComposite,
                            node.contentComposite.sweetLayoutSpec,
                            SweetCompositionData(),
                        )
                    storeLayoutNode(node.contentComposite, contentLn)
                }
            }
        }
    }
}
