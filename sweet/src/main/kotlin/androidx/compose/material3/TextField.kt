@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createTextField
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.widgets.Text

/**
 * A text input field backed by an SWT Text widget.
 *
 * Controlled-component semantics: [value] is the single source of truth.
 * If [onValueChange] ignores input (no-echo) or transforms it (uppercase),
 * the widget reverts to [value] and the caret is preserved.
 *
 * ### Parameter implementation status:
 * - `value` / `onValueChange` — controlled component with ModifyListener + SideEffect
 * - `placeholder` — rendered as a composable sibling when text is empty
 * - `isError` — visual feedback via background/foreground colors
 * - `readOnly` — SWT Text.setEditable(false)
 * - `singleLine` — SWT Text with SWT.SINGLE flag
 * - `visualTransformation` — [PasswordVisualTransformation] uses SWT.PASSWORD flag
 * - `label` — rendered as a composable above the text field (static, not animated)
 * - `leadingIcon` / `trailingIcon` — rendered as composable slots beside the text field
 * - `enabled` — SWT Text.setEnabled()
 * - `maxLines` / `minLines` — `maxLines <= 1` (or `singleLine`) → SWT.SINGLE; an explicit finite `maxLines` > 1 or `minLines` > 1 → SWT.MULTI | SWT.WRAP (wrapping multi-line). SWT Text exposes no per-line-count clamp, so the exact visible line count stays layout-driven; this selects single- vs multi-line rendering. (SWT styles are immutable, so the choice is fixed at creation.)
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {

    val applier = LocalSWTNodeApplier.current
    val inProgrammaticChange = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            // SWT styles are immutable post-creation, so single vs. multi-line is decided here.
            // `maxLines` defaults to Int.MAX_VALUE, so only an explicit finite value (or minLines > 1)
            // requests multi-line — the default single-line behaviour is unchanged.
            val enforceSingle = singleLine || maxLines <= 1
            val requestMulti = !enforceSingle && (maxLines != Int.MAX_VALUE || minLines > 1)
            var flags = when {
                enforceSingle -> SWT.SINGLE
                requestMulti -> SWT.MULTI or SWT.WRAP
                else -> 0
            }
            if (visualTransformation is PasswordVisualTransformation) flags = flags or SWT.PASSWORD
            val textField = createTextField(applier, flags)
            val scope = textField.createWidgetCoroutineScope()
            DirectSWTNode(
                textField.apply {
                    val listener =
                        ModifyListener { e ->
                            if (inProgrammaticChange.get()) return@ModifyListener
                            val textWidget = e.source as Text
                            val newText = textWidget.text

                            @Suppress("UNCHECKED_CAST")
                            val callback = getData("onValueChange") as? ((String) -> Unit)
                            scope.launch {
                                callback?.invoke(newText)
                            }
                        }
                    addModifyListener(listener)
                },
            )
        },
        update = {
            set(value) {
                val textControl = this.control as Text
                if (!textControl.isDisposed && textControl.text != it) {
                    val selStart = textControl.selection.x
                    val selEnd = textControl.selection.y
                    inProgrammaticChange.set(true)
                    try {
                        textControl.text = it
                        val clampedStart = minOf(selStart, it.length)
                        val clampedEnd = minOf(selEnd, it.length)
                        textControl.selection =
                            org.eclipse.swt.graphics.Point(clampedStart, clampedEnd)
                    } finally {
                        inProgrammaticChange.set(false)
                    }
                }
            }
            set(onValueChange) { (this.control as Text).setData("onValueChange", it) }
            set(modifier) { applySWTModifier(this.control, it) }
            set(enabled) { (this.control as Text).enabled = it }
            set(readOnly) { (this.control as Text).editable = !it }
            set(isError) {
                val text = this.control as Text
                if (it) {
                    text.background = errorBackgroundColor(text)
                    text.foreground = text.display.getSystemColor(SWT.COLOR_DARK_RED)
                } else {
                    text.background = null
                    text.foreground = null
                }
            }
        },
    )

    // Placeholder: composed as a sibling when TextField is empty.
    // The placeholder composable renders into the same parent Composite as the Text widget,
    // appearing as an overlay or adjacent element depending on the parent's layout.
    if (placeholder != null && value.isEmpty()) {
        placeholder()
    }

    // End-of-turn reconciliation
    SideEffect {
        val textWidget =
            applier.current.control as? Text ?: return@SideEffect
        if (!textWidget.isDisposed && textWidget.text != value) {
            val selStart = textWidget.selection.x
            val selEnd = textWidget.selection.y
            inProgrammaticChange.set(true)
            try {
                textWidget.text = value
                val clampedStart = minOf(selStart, value.length)
                val clampedEnd = minOf(selEnd, value.length)
                textWidget.selection =
                    org.eclipse.swt.graphics.Point(clampedStart, clampedEnd)
            } finally {
                inProgrammaticChange.set(false)
            }
        }
    }
}

/**
 * An outlined text field with visual border.
 *
 * Renders the text field inside a Card container with a border,
 * distinct from the filled [TextField]. Leading/trailing icons and
 * label are rendered as composable slots.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val textFieldContent: @Composable () -> Unit = {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            isError = isError,
            placeholder = placeholder,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
        )
    }

    Card(modifier = modifier) {
        if (label != null) {
            label()
        }
        when {
            leadingIcon != null && trailingIcon != null -> {
                androidx.compose.foundation.layout.Row {
                    leadingIcon()
                    textFieldContent()
                    trailingIcon()
                }
            }
            leadingIcon != null -> {
                androidx.compose.foundation.layout.Row {
                    leadingIcon()
                    textFieldContent()
                }
            }
            trailingIcon != null -> {
                androidx.compose.foundation.layout.Row {
                    textFieldContent()
                    trailingIcon()
                }
            }
            else -> textFieldContent()
        }
    }
}

private const val ERROR_BACKGROUND_KEY = "sweet.textfield.errorBackground"

/**
 * Error-state background color for [text], allocated at most once per widget.
 *
 * SWT [org.eclipse.swt.graphics.Color] is a native resource; under the pinned SWT graphics
 * fragment a fresh `Color(display, …)` on every `isError` toggle leaks a handle. The color is
 * cached on the widget and disposed when the widget is disposed.
 */
private fun errorBackgroundColor(text: Text): org.eclipse.swt.graphics.Color {
    (text.getData(ERROR_BACKGROUND_KEY) as? org.eclipse.swt.graphics.Color)?.let { return it }
    val display = text.display
    val sysRed = display.getSystemColor(SWT.COLOR_RED)
    val color =
        org.eclipse.swt.graphics.Color(
            display,
            (sysRed.red + 255) / 2,
            (sysRed.green + 255) / 2,
            (sysRed.blue + 255) / 2,
        )
    text.setData(ERROR_BACKGROUND_KEY, color)
    text.addDisposeListener { if (!color.isDisposed) color.dispose() }
    return color
}
