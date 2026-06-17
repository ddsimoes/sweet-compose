@file:Suppress("ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createButton
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent

internal val LocalTabTextCapture = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * A scrollable tab row for when you have too many tabs to fit on screen.
 *
 * **Sweet note:** Currently renders identically to [TabRow] — the `scrollable` parameter
 * is accepted but the tab row does not actually scroll. Scrolling support is planned.
 *
 * @param selectedTabIndex The index of the currently selected tab
 * @param modifier The modifier to be applied to the tab row
 * @param tabs The tabs to display in the tab row
 */
@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    TabRowImpl(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        scrollable = true,
        tabs = tabs,
    )
}

/**
 * A fixed tab row for when you have a small number of tabs
 *
 * @param selectedTabIndex The index of the currently selected tab
 * @param modifier The modifier to be applied to the tab row
 * @param tabs The tabs to display in the tab row
 */
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    TabRowImpl(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        scrollable = false,
        tabs = tabs,
    )
}

@Composable
private fun TabRowImpl(
    selectedTabIndex: Int,
    modifier: Modifier,
    scrollable: Boolean,
    tabs: @Composable () -> Unit,
) {
    // Use Row directly with minimal nesting
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        tabs()
    }
}

/**
 * A Material Design tab backed by an SWT Button.
 *
 * **Sweet note:** Only text content is supported via text-capture (LocalTabTextCapture) —
 * the same pattern as Button. Non-Text content is silently dropped. Icons are accepted
 * for API compatibility but ignored. This pattern will be replaced after doc 11
 * (layout engine rewrite). Selected tabs get blue background / white text styling.
 *
 * @param selected Whether this tab is currently selected
 * @param onClick Callback for when this tab is clicked
 * @param modifier The modifier to be applied to the tab
 * @param text The text content of the tab
 * @param icon The optional icon content of the tab (currently ignored)
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val applier = LocalSWTNodeApplier.current
    var capturedText by remember { mutableStateOf("Tab") }

    // Capture text content using the same mechanism as Button
    CompositionLocalProvider(
        LocalTabTextCapture provides { capturedText = it },
    ) {
        text?.invoke()
    }

    // Create actual SWT Button for reliable clicking
    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val button = createButton(applier)
            val scope = button.createWidgetCoroutineScope()
            DirectSWTNode(
                button.apply {
                    val listener =
                        object : SelectionAdapter() {
                            override fun widgetSelected(e: SelectionEvent) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val callback = getData("__sweet_onClick") as? (() -> Unit)
                                    scope.launch {
                                        callback?.invoke()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    addSelectionListener(listener)
                },
            )
        },
        update = {
            set(onClick) {
                val btn = this.control as org.eclipse.swt.widgets.Button
                if (!btn.isDisposed) btn.setData("__sweet_onClick", it)
            }
            set(modifier.weight(1f)) { applySWTModifier(this.control, it) }
            set(capturedText) { (this.control as org.eclipse.swt.widgets.Button).text = it }
            set(selected) {
                val button = this.control as org.eclipse.swt.widgets.Button
                if (it) {
                    // Selected tab - blue background, white text
                    button.background = button.display.getSystemColor(org.eclipse.swt.SWT.COLOR_BLUE)
                    button.foreground = button.display.getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE)
                } else {
                    // Unselected tab - default system colors
                    button.background = null
                    button.foreground = null
                }
            }
        },
    )
}

/**
 * Tab with only text content (convenience function)
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        text = text,
        icon = null,
    )
}
