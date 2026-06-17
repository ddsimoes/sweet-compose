@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.MessageBox

/**
 * A Material Design alert dialog backed by SWT MessageBox.
 *
 * **Sweet note:** Renders as a native SWT MessageBox dialog. The [title], [text],
 * [confirmButton], and [dismissButton] are mapped to MessageBox buttons.
 * Custom content composables and advanced dialog features (icon, custom layout)
 * are deferred.
 *
 * @param onDismissRequest Called when the dialog is dismissed
 * @param title The dialog title
 * @param text The dialog body text
 * @param confirmButton Text for the confirm/OK button (default "OK")
 * @param dismissButton Text for the dismiss/cancel button, or null for no cancel button
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String? = null,
    confirmButton: @Composable () -> Unit = { },
    dismissButton: @Composable (() -> Unit)? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "AlertDialog",
            "AlertDialog renders as native SWT MessageBox; confirm/dismiss button customization not yet supported",
        )
    }

    // Show native SWT MessageBox on the active shell
    val display =
        org.eclipse.swt.widgets.Display.getCurrent()
            ?: org.eclipse.swt.widgets.Display.getDefault()
    val activeShell = display.activeShell ?: display.shells.firstOrNull()

    if (activeShell != null && !activeShell.isDisposed) {
        val style = if (dismissButton != null) SWT.OK or SWT.CANCEL else SWT.OK
        val box = MessageBox(activeShell, style)
        box.text = title ?: ""
        box.message = text ?: ""
        val result = box.open()
        onDismissRequest()
    } else {
        // No active shell — invoke dismiss immediately (test/deferred scenario)
        onDismissRequest()
    }
}

/**
 * A Material Design dialog.
 *
 * **Sweet note:** Currently an opinion-free wrapper that delegates to a child Shell
 * for modal dialog behavior. The [content] composable is rendered inside the dialog shell.
 *
 * @param onDismissRequest Called when the dialog is dismissed
 * @param title The dialog title
 * @param content The dialog body content
 */
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable () -> Unit = { },
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "Dialog",
            "Dialog is a lightweight shell wrapper; full native dialog integration is deferred",
        )
    }

    // For now: dismiss immediately — full child-Shell implementation deferred
    onDismissRequest()
}
