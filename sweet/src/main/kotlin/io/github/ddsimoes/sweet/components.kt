package io.github.ddsimoes.sweet

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*

/**
 * A text component that displays text using SWT Label
 */
@Composable
fun Text(
    text: String,
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<TextNode, SWTNodeApplier>(
        factory = { TextNode() },
        update = {
            set(text) { this.text = text }
            set(modifier) { this.modifier = modifier }
        }
    )
}

/**
 * Node implementation for Text component
 */
class TextNode : SWTControlNode() {
    override var widget: Label? = null
        private set

    var text: String = ""
        set(value) {
            field = value
            widget?.let { label ->
                label.text = value
                // Force layout update when text changes
                label.parent?.layout(true, true)
            }
        }

    fun create(parent: Composite) {
        if (widget == null) {
            SweetDebugger.log("NODE", "Creating TextNode widget with text: '$text'")
            widget = Label(parent, SWT.NONE).apply {
                text = this@TextNode.text
            }
            applyModifier()
            SweetDebugger.log("NODE", "TextNode widget created: ${widget}")
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }
}

/**
 * A button component using SWT Button
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit = {},
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<ButtonNode, SWTNodeApplier>(
        factory = { ButtonNode() },
        update = {
            set(text) { this.text = text }
            set(onClick) { this.onClick = onClick }
            set(modifier) { this.modifier = modifier }
        }
    )
}

/**
 * Node implementation for Button component
 */
class ButtonNode : SWTControlNode() {
    override var widget: Button? = null
        private set

    var text: String = ""
        set(value) {
            field = value
            widget?.text = value
        }

    var onClick: () -> Unit = {}
        set(value) {
            field = value
            updateClickListener()
        }

    fun create(parent: Composite) {
        if (widget == null) {
            SweetDebugger.log("NODE", "Creating ButtonNode widget with text: '$text'")
            widget = Button(parent, SWT.PUSH).apply {
                text = this@ButtonNode.text
            }
            updateClickListener()
            applyModifier()
            SweetDebugger.log("NODE", "ButtonNode widget created: ${widget}")
        }
    }

    private fun updateClickListener() {
        widget?.let { button ->
            SweetDebugger.log("BUTTON", "Updating click listener for button '${text}'")
            button.removeSelectionListener(selectionListener)
            button.addSelectionListener(selectionListener)
            SweetDebugger.log("BUTTON", "Click listener updated for button '${text}'")
        }
    }

    private val selectionListener = object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            SweetDebugger.log("BUTTON", "Button '${text}' clicked, invoking onClick handler")
            try {
                onClick()
                SweetDebugger.log("BUTTON", "Button '${text}' onClick handler completed successfully")
            } catch (e: Exception) {
                SweetDebugger.log("BUTTON", "Exception in button '${text}' onClick handler", e)
            }
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }

    override fun dispose() {
        if (widget?.isDisposed == true) {
            return
        }
        widget?.removeSelectionListener(selectionListener)
        super.dispose()
    }
}


/**
 * A column layout that arranges children vertically
 */
@Composable
fun Column(
    modifier: SWTModifier = SWTModifier,
    content: @Composable () -> Unit
) {
    ComposeNode<CompositeNode, SWTNodeApplier>(
        factory = { CompositeNode(LayoutType.COLUMN) },
        update = {
            set(modifier) { this.modifier = modifier }
        },
        content = content
    )
}

/**
 * A row layout that arranges children horizontally
 */
@Composable
fun Row(
    modifier: SWTModifier = SWTModifier,
    content: @Composable () -> Unit
) {
    ComposeNode<CompositeNode, SWTNodeApplier>(
        factory = { CompositeNode(LayoutType.ROW) },
        update = {
            set(modifier) { this.modifier = modifier }
        },
        content = content
    )
}

/**
 * A generic container
 */
@Composable
fun Box(
    modifier: SWTModifier = SWTModifier,
    content: @Composable () -> Unit
) {
    ComposeNode<CompositeNode, SWTNodeApplier>(
        factory = { CompositeNode(LayoutType.BOX) },
        update = {
            set(modifier) { this.modifier = modifier }
        },
        content = content
    )
}

/**
 * Layout types for CompositeNode
 */
enum class LayoutType {
    COLUMN, ROW, BOX
}

/**
 * Node implementation for layout composables
 */
open class CompositeNode(
    private val layoutType: LayoutType
) : SWTControlNode() {
    override var widget: Composite? = null
        protected set

    fun create(parent: Composite) {
        if (widget == null) {
            SweetDebugger.log("NODE", "Creating CompositeNode widget with layoutType: $layoutType")
            widget = Composite(parent, SWT.NONE).apply {
                layout = when (layoutType) {
                    LayoutType.COLUMN -> GridLayout(1, false)
                    LayoutType.ROW -> RowLayout(SWT.HORIZONTAL)
                    LayoutType.BOX -> GridLayout(1, false)
                }
            }
            applyModifier()
            SweetDebugger.log("NODE", "CompositeNode widget created: ${widget} with layout: ${widget?.layout}")
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        val parentWidget = when (val parentNode = parent) {
            is CompositeNode -> parentNode.widget
            is RootCompositeNode -> parentNode.widget
            else -> null
        }

        parentWidget?.let { parent ->
            if (widget == null) {
                create(parent)
                // After creating this composite widget, trigger child widget creation
                SweetDebugger.log("NODE", "CompositeNode widget created, triggering child widget creation")
                children.forEach { child ->
                    child.applyChanges()
                }
            }
        }
    }

    override fun layout() {
        super.layout()
        widget?.layout(true, true)
    }
}

/**
 * Special root composite node
 */
class RootCompositeNode(override val widget: Composite) : SWTControlNode()


/**
 * A text input field using SWT Text widget
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit = {},
    modifier: SWTModifier = SWTModifier,
    placeholder: String = ""
) {
    ComposeNode<TextFieldNode, SWTNodeApplier>(
        factory = { TextFieldNode() },
        update = {
            set(value) { this.value = value }
            set(onValueChange) { this.onValueChange = onValueChange }
            set(modifier) { this.modifier = modifier }
            set(placeholder) { this.placeholder = placeholder }
        }
    )
}

/**
 * Node implementation for TextField component
 */
class TextFieldNode : SWTControlNode() {
    override var widget: Text? = null
        private set

    var value: String = ""
        set(newValue) {
            if (field != newValue) {
                field = newValue
                widget?.let { textWidget ->
                    if (textWidget.text != newValue) {
                        textWidget.text = newValue
                    }
                }
            }
        }

    var onValueChange: (String) -> Unit = {}

    var placeholder: String = ""
        set(value) {
            field = value
            updatePlaceholder()
        }

    fun create(parent: Composite) {
        if (widget == null) {
            widget = Text(parent, SWT.BORDER).apply {
                text = this@TextFieldNode.value
                addModifyListener(modifyListener)
            }
            updatePlaceholder()
            applyModifier()
        }
    }

    private fun updatePlaceholder() {
        widget?.let { textWidget ->
            if (placeholder.isNotEmpty()) {
                textWidget.message = placeholder
            }
        }
    }

    private val modifyListener = ModifyListener { e ->
        val newText = (e.source as Text).text
        if (value != newText) {
            value = newText
            onValueChange(newText)
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }

    override fun dispose() {
        widget?.removeModifyListener(modifyListener)
        super.dispose()
    }
}

/**
 * A spacer component that takes up space
 */
@Composable
fun Spacer(
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<SpacerNode, SWTNodeApplier>(
        factory = { SpacerNode() },
        update = {
            set(modifier) { this.modifier = modifier }
        }
    )
}

/**
 * Node implementation for Spacer component
 */
class SpacerNode : SWTControlNode() {
    override var widget: Label? = null
        private set

    fun create(parent: Composite) {
        if (widget == null) {
            widget = Label(parent, SWT.NONE).apply {
                // Empty label acts as spacer
                text = ""
                layoutData = GridData(GridData.FILL_BOTH)
            }
            applyModifier()
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }
}

/**
 * Modifier system for SWT widgets, similar to Compose modifiers
 */
@Stable
interface SWTModifier {

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R
    fun any(predicate: (Element) -> Boolean): Boolean
    fun all(predicate: (Element) -> Boolean): Boolean

    infix fun then(other: SWTModifier): SWTModifier =
        if (other === SWTModifier) this else CombinedModifier(this, other)

    interface Element : SWTModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)
        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)

        /**
         * Apply this modifier element to a SWT Control
         */
        fun apply(control: Control)
    }

    companion object : SWTModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override fun any(predicate: (Element) -> Boolean): Boolean = false
        override fun all(predicate: (Element) -> Boolean): Boolean = true
        override fun toString(): String = "SWTModifier"
    }
}

/**
 * Combined modifier that chains multiple modifiers
 */
class CombinedModifier(
    private val outer: SWTModifier,
    private val inner: SWTModifier
) : SWTModifier {
    override fun <R> foldIn(initial: R, operation: (R, SWTModifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (SWTModifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (SWTModifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (SWTModifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

    override fun toString(): String = "[$outer, $inner]"
}

// Modifier extensions

@Stable
fun SWTModifier.fillMaxWidth(): SWTModifier = this.then(FillMaxWidthModifier)

@Stable
fun SWTModifier.fillMaxHeight(): SWTModifier = this.then(FillMaxHeightModifier)

@Stable
fun SWTModifier.size(width: Int, height: Int): SWTModifier =
    this.then(SizeModifier(width, height))

@Stable
fun SWTModifier.padding(padding: Int): SWTModifier =
    this.then(PaddingModifier(padding, padding, padding, padding))


// Modifier implementations

private object FillMaxWidthModifier : SWTModifier.Element {
    override fun apply(control: Control) {
        val parent = control.parent
        when (parent.layout) {
            is GridLayout -> {
                val layoutData = control.layoutData as? GridData ?: GridData()
                layoutData.horizontalAlignment = GridData.FILL
                layoutData.grabExcessHorizontalSpace = true
                control.layoutData = layoutData
            }
            is RowLayout -> {
                // RowLayout doesn't support fill - it auto-sizes children
                // So we just leave the default RowData
            }
        }
    }
    override fun toString() = "FillMaxWidth"
}

private object FillMaxHeightModifier : SWTModifier.Element {
    override fun apply(control: Control) {
        val parent = control.parent
        when (parent.layout) {
            is GridLayout -> {
                val layoutData = control.layoutData as? GridData ?: GridData()
                layoutData.verticalAlignment = GridData.FILL
                layoutData.grabExcessVerticalSpace = true
                control.layoutData = layoutData
            }
            is RowLayout -> {
                // RowLayout doesn't support fill - it auto-sizes children
                // So we just leave the default RowData
            }
        }
    }
    override fun toString() = "FillMaxHeight"
}

private class SizeModifier(
    private val width: Int,
    private val height: Int
) : SWTModifier.Element {
    override fun apply(control: Control) {
        control.setSize(width, height)
    }
    override fun toString() = "Size($width, $height)"
}

private class PaddingModifier(
    private val left: Int,
    private val top: Int,
    private val right: Int,
    private val bottom: Int
) : SWTModifier.Element {
    override fun apply(control: Control) {
        val parent = control.parent
        when (parent.layout) {
            is GridLayout -> {
                val layoutData = control.layoutData as? GridData ?: GridData()
                layoutData.horizontalIndent = left
                layoutData.verticalIndent = top
                control.layoutData = layoutData
            }
            is RowLayout -> {
                // RowLayout doesn't support padding through layout data
                // SWT handles this differently for RowLayout
            }
        }
    }
    override fun toString() = "Padding($left, $top, $right, $bottom)"
}

/**
 * Base class for all SWT-backed Compose nodes
 */
abstract class SWTNode {
    val children = mutableListOf<SWTNode>()
    var parent: SWTNode? = null

    abstract val widget: Widget?

    /**
     * Layout this node and its children
     */
    open fun layout() {
        children.forEach { it.layout() }
    }

    /**
     * Apply any pending updates to the SWT widget
     */
    open fun applyChanges() {
        children.forEach { it.applyChanges() }
    }

    /**
     * Dispose of SWT resources
     */
    open fun dispose() {
        children.forEach { it.dispose() }
        widget?.dispose()
    }
}

/**
 * Node that wraps a SWT Control widget
 */
abstract class SWTControlNode : SWTNode() {
    abstract override val widget: Control?

    var modifier: SWTModifier = SWTModifier
        set(value) {
            field = value
            applyModifier()
        }

    protected open fun applyModifier() {
        widget?.let { control ->
            modifier.foldIn(Unit) { _, element ->
                element.apply(control)
            }
        }
    }
}

/**
 * Checkbox component
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    text: String = "",
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<CheckboxNode, SWTNodeApplier>(
        factory = { CheckboxNode() },
        update = {
            set(checked) { this.checked = checked }
            set(onCheckedChange) { this.onCheckedChange = onCheckedChange }
            set(text) { this.text = text }
            set(modifier) { this.modifier = modifier }
        }
    )
}

class CheckboxNode : SWTControlNode() {
    override var widget: Button? = null
        private set

    var checked: Boolean = false
        set(value) {
            field = value
            widget?.selection = value
        }

    var onCheckedChange: (Boolean) -> Unit = {}

    var text: String = ""
        set(value) {
            field = value
            widget?.text = value
        }

    fun create(parent: Composite) {
        if (widget == null) {
            widget = Button(parent, SWT.CHECK).apply {
                text = this@CheckboxNode.text
                selection = this@CheckboxNode.checked
                addSelectionListener(selectionListener)
            }
            applyModifier()
        }
    }

    private val selectionListener = object : SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            val newChecked = (e.source as Button).selection
            if (checked != newChecked) {
                checked = newChecked
                onCheckedChange(newChecked)
            }
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }

    override fun dispose() {
        widget?.removeSelectionListener(selectionListener)
        super.dispose()
    }
}

/**
 * Card component for better visual organization
 */
@Composable
fun Card(
    modifier: SWTModifier = SWTModifier,
    backgroundColor: RGB? = null,
    content: @Composable () -> Unit
) {
    ComposeNode<CardNode, SWTNodeApplier>(
        factory = { CardNode() },
        update = {
            set(modifier) { this.modifier = modifier }
            set(backgroundColor) { this.backgroundColor = backgroundColor }
        },
        content = content
    )
}

class CardNode : CompositeNode(LayoutType.COLUMN) {
    var backgroundColor: RGB? = null
        set(value) {
            field = value
            updateBackground()
        }

    private fun updateBackground() {
        widget?.let { composite ->
            backgroundColor?.let { rgb ->
                val color = Color(composite.display, rgb)
                composite.background = color
            }
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        updateBackground()
    }
}

/**
 * Divider component
 */
@Composable
fun Divider(
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<DividerNode, SWTNodeApplier>(
        factory = { DividerNode() },
        update = {
            set(modifier) { this.modifier = modifier }
        }
    )
}

class DividerNode : SWTControlNode() {
    override var widget: Label? = null
        private set

    fun create(parent: Composite) {
        if (widget == null) {
            widget = Label(parent, SWT.SEPARATOR or SWT.HORIZONTAL).apply {
                layoutData = GridData(GridData.FILL_HORIZONTAL)
            }
            applyModifier()
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }
}

/**
 * Enhanced Text with color support
 */
@Composable
fun StyledText(
    text: String,
    color: RGB? = null,
    modifier: SWTModifier = SWTModifier
) {
    ComposeNode<StyledTextNode, SWTNodeApplier>(
        factory = { StyledTextNode() },
        update = {
            set(text) { this.text = text }
            set(color) { this.color = color }
            set(modifier) { this.modifier = modifier }
        }
    )
}

class StyledTextNode : SWTControlNode() {
    override var widget: Label? = null
        private set

    var text: String = ""
        set(value) {
            field = value
            widget?.let { label ->
                label.text = value
                label.parent?.layout(true, true)
            }
        }

    var color: RGB? = null
        set(value) {
            field = value
            updateColor()
        }

    private fun updateColor() {
        widget?.let { label ->
            color?.let { rgb ->
                val textColor = Color(label.display, rgb)
                label.foreground = textColor
            }
        }
    }

    fun create(parent: Composite) {
        if (widget == null) {
            widget = Label(parent, SWT.NONE).apply {
                text = this@StyledTextNode.text
            }
            applyModifier()
            updateColor()
        }
    }

    override fun applyChanges() {
        super.applyChanges()
        (parent as? CompositeNode)?.widget?.let { parentWidget ->
            if (widget == null) {
                create(parentWidget)
            }
        }
    }
}

// Modifier extensions for colors and styling
@Stable
fun SWTModifier.backgroundColor(rgb: RGB): SWTModifier =
    this.then(BackgroundColorModifier(rgb))

@Stable
fun SWTModifier.foregroundColor(rgb: RGB): SWTModifier =
    this.then(ForegroundColorModifier(rgb))

// Additional padding extensions to match common usage patterns
@Stable
fun SWTModifier.padding(
    horizontal: Int = 0,
    vertical: Int = 0
): SWTModifier = this.then(PaddingModifier(horizontal, vertical, horizontal, vertical))

@Stable
fun SWTModifier.padding(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0
): SWTModifier = this.then(PaddingModifier(left, top, right, bottom))

private class BackgroundColorModifier(private val rgb: RGB) : SWTModifier.Element {
    override fun apply(control: Control) {
        val color = Color(control.display, rgb)
        control.background = color
    }
    override fun toString() = "BackgroundColor($rgb)"
}

private class ForegroundColorModifier(private val rgb: RGB) : SWTModifier.Element {
    override fun apply(control: Control) {
        val color = Color(control.display, rgb)
        control.foreground = color
    }
    override fun toString() = "ForegroundColor($rgb)"
}