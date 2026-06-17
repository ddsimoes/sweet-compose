@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createSweetColumn
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent

/**
 * The type of element that can serve as a dropdown menu anchor.
 */
@JvmInline
value class MenuAnchorType private constructor(private val name: String) {
    companion object {
        val PrimaryNotEditable = MenuAnchorType("PrimaryNotEditable")
        val PrimaryEditable = MenuAnchorType("PrimaryEditable")
    }
}

/**
 * A Material Design dropdown menu.
 *
 * Renders inline in a [Column] when [expanded] is true. Full native SWT popup
 * menu (SWT.POP_UP) with proper anchoring and dismiss-on-outside-click is
 * planned for a future release.
 *
 * @param expanded whether the menu is expanded
 * @param onDismissRequest called when the user requests to dismiss the menu
 * @param modifier the [Modifier] to be applied to this menu
 * @param content the content of the menu — typically [DropdownMenuItem] items
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Column(modifier = modifier, content = content)
    }
}

/**
 * Scope for [ExposedDropdownMenuBox].
 */
open class ExposedDropdownMenuBoxScope internal constructor(
    internal val expanded: Boolean,
    internal val onExpandedChange: (Boolean) -> Unit,
) {
    open fun Modifier.menuAnchor(
        type: MenuAnchorType,
        enabled: Boolean = true,
    ): Modifier = this

    open fun Modifier.exposedDropdownSize(
        matchTextFieldWidth: Boolean = true,
    ): Modifier = this

    @ExperimentalMaterial3Api
    @Composable
    open fun ExposedDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        if (expanded) {
            Column(modifier = modifier, content = content)
        }
    }
}

private class ExposedDropdownMenuBoxScopeImpl(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) : ExposedDropdownMenuBoxScope(expanded, onExpandedChange)

/**
 * A Material Design exposed dropdown menu box.
 *
 * Renders as a clickable container that toggles [expanded] via [onExpandedChange].
 */
@ExperimentalMaterial3Api
@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit,
) {
    val applier = LocalSWTNodeApplier.current
    val scope =
        remember(expanded, onExpandedChange) {
            ExposedDropdownMenuBoxScopeImpl(expanded, onExpandedChange)
        }
    val currentExpanded = remember { mutableStateOf(expanded) }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val composite = createSweetColumn(applier)
            val coroutineScope = composite.createWidgetCoroutineScope()
            val listener =
                object : MouseAdapter() {
                    override fun mouseDown(e: MouseEvent) {
                        coroutineScope.launch {
                            onExpandedChange(!currentExpanded.value)
                        }
                    }
                }
            composite.addMouseListener(listener)
            DirectSWTNode(composite)
        },
        update = {
            set(modifier) { if (!this.control.isDisposed) applySWTModifier(this.control, it) }
            set(expanded) { currentExpanded.value = it }
        },
    ) {
        scope.content()
    }
}

/**
 * A Material Design dropdown menu item.
 *
 * Renders as a plain [Button] with the given content. When clicked,
 * the [onClick] handler fires.
 *
 * @param text the label content composable
 * @param onClick called when the item is clicked
 * @param modifier the [Modifier] to be applied to this item
 */
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        text()
    }
}

@Composable
fun DropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = text)
    }
}
