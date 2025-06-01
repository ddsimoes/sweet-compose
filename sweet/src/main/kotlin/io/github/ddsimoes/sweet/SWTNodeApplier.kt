package io.github.ddsimoes.sweet

import androidx.compose.runtime.AbstractApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * Node applier with proper SWT widget management
 */
class SWTNodeApplier(root: SWTNode) : AbstractApplier<SWTNode>(root) {

    override fun insertTopDown(index: Int, instance: SWTNode) {
        // We build the tree bottom-up for SWT
    }

    override fun insertBottomUp(index: Int, instance: SWTNode) {
        SweetDebugger.log("APPLIER", "insertBottomUp: index=$index, instance=${instance::class.simpleName}, current=${current::class.simpleName}")
        
        current.children.add(index, instance)
        instance.parent = current

        // Create SWT widgets if needed
        SweetDebugger.trace("APPLIER", "Creating widget for ${instance::class.simpleName}") {
            createWidgetIfNeeded(instance)
        }

        // Apply any pending changes
        SweetDebugger.trace("APPLIER", "Applying changes for ${instance::class.simpleName}") {
            instance.applyChanges()
        }

        // Layout if we're dealing with composites
        if (current is CompositeNode || current is RootCompositeNode) {
            SweetDebugger.trace("APPLIER", "Laying out ${current::class.simpleName}") {
                current.layout()
            }
        }
        
        SweetDebugger.log("APPLIER", "insertBottomUp complete for ${instance::class.simpleName}")
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            val node = current.children.removeAt(index)
            node.parent = null
            node.dispose()
        }

        // Re-layout after removal
        if (current is CompositeNode || current is RootCompositeNode) {
            current.layout()
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)

        // Re-layout after move
        if (current is CompositeNode || current is RootCompositeNode) {
            current.layout()
        }
    }

    override fun onClear() {
        current.children.forEach { it.dispose() }
        current.children.clear()
    }

    private fun createWidgetIfNeeded(node: SWTNode) {
        val nodeType = node::class.simpleName
        val currentType = current::class.simpleName
        
        SweetDebugger.log("APPLIER", "createWidgetIfNeeded: node=$nodeType, current=$currentType")
        
        when (node) {
            is TextNode -> {
                (current as? CompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating TextNode widget with CompositeNode parent")
                    node.create(parent)
                }
                (current as? RootCompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating TextNode widget with RootCompositeNode parent")
                    node.create(parent)
                }
            }
            is ButtonNode -> {
                (current as? CompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating ButtonNode widget with CompositeNode parent")
                    node.create(parent)
                }
                (current as? RootCompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating ButtonNode widget with RootCompositeNode parent")
                    node.create(parent)
                }
            }
            is TextFieldNode -> {
                (current as? CompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating TextFieldNode widget with CompositeNode parent")
                    node.create(parent)
                }
                (current as? RootCompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating TextFieldNode widget with RootCompositeNode parent")
                    node.create(parent)
                }
            }
            is SpacerNode -> {
                (current as? CompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating SpacerNode widget with CompositeNode parent")
                    node.create(parent)
                }
                (current as? RootCompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating SpacerNode widget with RootCompositeNode parent")
                    node.create(parent)
                }
            }
            is CompositeNode -> {
                (current as? CompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating CompositeNode widget with CompositeNode parent")
                    node.create(parent)
                }
                (current as? RootCompositeNode)?.widget?.let { parent ->
                    SweetDebugger.log("APPLIER", "Creating CompositeNode widget with RootCompositeNode parent")
                    node.create(parent)
                }
            }
        }
        
        SweetDebugger.log("APPLIER", "createWidgetIfNeeded complete for $nodeType, widget=${node.widget}")
    }
}