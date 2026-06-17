@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.DirectoryDialog
import org.eclipse.swt.widgets.FileDialog

/**
 * Opens a native file picker dialog via SWT FileDialog.
 *
 * @param title The dialog title
 * @param initialDirectory Starting directory path, or null for default
 * @param filterExtensions File extension filters (e.g. listOf("*.txt", "*.md"))
 * @param onResult Called with the selected file path, or null if cancelled
 */
@Composable
fun FilePickerDialog(
    title: String = "Open File",
    initialDirectory: String? = null,
    filterExtensions: List<String>? = null,
    onResult: (String?) -> Unit,
) {
    val display =
        org.eclipse.swt.widgets.Display.getCurrent()
            ?: org.eclipse.swt.widgets.Display.getDefault()
    val activeShell = display.activeShell ?: display.shells.firstOrNull()

    if (activeShell != null && !activeShell.isDisposed) {
        val dialog = FileDialog(activeShell, SWT.OPEN)
        dialog.text = title
        if (initialDirectory != null) dialog.filterPath = initialDirectory
        if (filterExtensions != null) {
            dialog.filterExtensions = filterExtensions.toTypedArray()
        }
        val result = dialog.open()
        onResult(result)
    } else {
        onResult(null)
    }
}

/**
 * Opens a native directory picker dialog via SWT DirectoryDialog.
 *
 * @param title The dialog title
 * @param initialDirectory Starting directory path, or null for default
 * @param onResult Called with the selected directory path, or null if cancelled
 */
@Composable
fun DirectoryPickerDialog(
    title: String = "Select Directory",
    initialDirectory: String? = null,
    onResult: (String?) -> Unit,
) {
    val display =
        org.eclipse.swt.widgets.Display.getCurrent()
            ?: org.eclipse.swt.widgets.Display.getDefault()
    val activeShell = display.activeShell ?: display.shells.firstOrNull()

    if (activeShell != null && !activeShell.isDisposed) {
        val dialog = DirectoryDialog(activeShell, SWT.OPEN)
        dialog.text = title
        if (initialDirectory != null) dialog.filterPath = initialDirectory
        val result = dialog.open()
        onResult(result)
    } else {
        onResult(null)
    }
}

/**
 * Opens a native file save dialog via SWT FileDialog in SAVE mode.
 *
 * @param title The dialog title
 * @param initialDirectory Starting directory path, or null for default
 * @param filterExtensions File extension filters
 * @param onResult Called with the selected file path, or null if cancelled
 */
@Composable
fun FileSaveDialog(
    title: String = "Save File",
    initialDirectory: String? = null,
    filterExtensions: List<String>? = null,
    onResult: (String?) -> Unit,
) {
    val display =
        org.eclipse.swt.widgets.Display.getCurrent()
            ?: org.eclipse.swt.widgets.Display.getDefault()
    val activeShell = display.activeShell ?: display.shells.firstOrNull()

    if (activeShell != null && !activeShell.isDisposed) {
        val dialog = FileDialog(activeShell, SWT.SAVE)
        dialog.text = title
        if (initialDirectory != null) dialog.filterPath = initialDirectory
        if (filterExtensions != null) {
            dialog.filterExtensions = filterExtensions.toTypedArray()
        }
        val result = dialog.open()
        onResult(result)
    } else {
        onResult(null)
    }
}
