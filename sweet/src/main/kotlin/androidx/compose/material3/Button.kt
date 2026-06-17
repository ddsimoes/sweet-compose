@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createButton
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Button as SWTButton

internal val LocalButtonTextCapture = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * A Material Design button backed by an SWT Button.
 *
 * **Content limitation:** Uses text-capture pattern (LocalButtonTextCapture) — any Text()
 * inside content() routes its string to the native SWT Button label and does NOT render
 * a separate Label widget. Non-Text content (icons, Row, custom composables) is silently
 * dropped. This pattern will be replaced when the applier supports content composites
 * (gated on doc 11 layout engine rewrite). See consolidation workstream 41 C1.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    val capturedText = remember { mutableStateOf("") }
    val buttonRef = remember { mutableStateOf<SWTButton?>(null) }

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
                                    val callback = getData("onClick") as? (() -> Unit)
                                    scope.launch {
                                        callback?.invoke()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    addSelectionListener(listener)
                    buttonRef.value = this
                    text = capturedText.value
                },
            )
        },
        update = {
            set(onClick) {
                val btn = this.control as SWTButton
                if (!btn.isDisposed) btn.setData("onClick", it)
            }
            set(modifier) { if (!this.control.isDisposed) applySWTModifier(this.control, it) }
            set(enabled) {
                val btn = this.control as SWTButton
                if (!btn.isDisposed) btn.enabled = it
            }
            update(Unit) {
                buttonRef.value = this.control as SWTButton
            }
        },
    )

    CompositionLocalProvider(
        LocalButtonTextCapture provides { text ->
            if (capturedText.value != text) {
                capturedText.value = text
                buttonRef.value?.let { button ->
                    if (!button.isDisposed) {
                        // Directly set; avoid reading .text to prevent SWTException on disposed widgets
                        button.text = text
                        button.requestLayout()
                        button.parent?.let { parent ->
                            io.github.ddsimoes.sweet.layout.LayoutCoordinator.forDisplay(button.display)
                                .requestLayout(parent as org.eclipse.swt.widgets.Composite)
                        }
                    }
                }
            }
        },
    ) {
        content()
    }
}

/**
 * An outlined button variant.
 *
 * **Sweet note:** Currently delegates to [Button] — renders identically to a standard
 * Button with no outline styling. This is a compatibility shim.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("OutlinedButton", "OutlinedButton currently renders as a plain Button; outline styling is not yet implemented")
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * A text button variant.
 *
 * **Sweet note:** Currently delegates to [Button] — renders identically to a standard
 * Button with no text-only styling. This is a compatibility shim.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("TextButton", "TextButton currently renders as a plain Button; text-only styling is not yet implemented")
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * An elevated button variant.
 *
 * **Sweet note:** Currently delegates to [Button] — renders identically to a standard
 * Button with no elevation shadow. This is a compatibility shim.
 */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("ElevatedButton", "ElevatedButton currently renders as a plain Button; elevation shadow is not yet implemented")
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * A filled tonal button variant.
 *
 * **Sweet note:** Currently delegates to [Button] — renders identically to a standard
 * Button with no tonal fill color. This is a compatibility shim.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("FilledTonalButton", "FilledTonalButton currently renders as a plain Button; tonal fill color is not yet implemented")
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * A floating action button variant.
 *
 * **Sweet note:** Currently delegates to [Button] — renders identically to a standard
 * Button with no floating/FAB styling. This is a compatibility shim.
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("FloatingActionButton", "FloatingActionButton currently renders as a plain Button; FAB styling is not yet implemented")
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        content = content,
    )
}
