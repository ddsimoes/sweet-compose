@file:Suppress("ktlint:standard:no-wildcard-imports", "MatchingDeclarationName", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.MessageBox

/**
 * Contains default values used by [AlertDialog].
 */
object AlertDialogDefaults {
    /** Default shape for an alert dialog. */
    val shape: Shape
        @Composable get() = RoundedCornerShape(0.dp)

    /** Default container color for an alert dialog. */
    val containerColor: Color
        @Composable get() = Color.White

    /** Default icon content color for an alert dialog. */
    val iconContentColor: Color
        @Composable get() = Color.Black

    /** Default title content color for an alert dialog. */
    val titleContentColor: Color
        @Composable get() = Color.Black

    /** Default text content color for an alert dialog. */
    val textContentColor: Color
        @Composable get() = Color.Black

    /** Default tonal elevation for an alert dialog. */
    val TonalElevation: Dp = Dp(6f)
}

/**
 * A Material Design alert dialog.
 *
 * **Sweet note:** Renders as a native SWT MessageBox. The [title], [text], and [icon]
 * content composables are accepted for API compatibility but are not rendered visually
 * in the native dialog. Rich content rendering is planned.
 *
 * @param onDismissRequest called when the dialog is dismissed
 * @param confirmButton the confirm button composable
 * @param modifier the [Modifier] to be applied to this dialog
 * @param dismissButton the dismiss button composable (when non-null, dialog shows OK/Cancel)
 * @param icon the optional icon composable (not rendered in native dialog)
 * @param title the optional title composable (not rendered; native title shown instead)
 * @param text the optional text composable (not rendered; native message shown instead)
 * @param shape the shape of the dialog (unused in native dialog)
 * @param containerColor the container color (unused in native dialog)
 * @param iconContentColor the icon content color (unused in native dialog)
 * @param titleContentColor the title content color (unused in native dialog)
 * @param textContentColor the text content color (unused in native dialog)
 * @param tonalElevation the tonal elevation (unused in native dialog)
 * @param properties dialog properties for platform-specific configuration
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "AlertDialog",
            "AlertDialog renders as native SWT MessageBox; rich title/text/icon content not yet rendered",
        )
    }

    val display =
        org.eclipse.swt.widgets.Display.getCurrent()
            ?: org.eclipse.swt.widgets.Display.getDefault()
    val activeShell = display.activeShell ?: display.shells.firstOrNull()

    if (activeShell != null && !activeShell.isDisposed) {
        val style = if (dismissButton != null) SWT.OK or SWT.CANCEL else SWT.OK
        val box = MessageBox(activeShell, style)
        box.text = "Alert"
        box.message = ""
        box.open()
        onDismissRequest()
    } else {
        onDismissRequest()
    }
}
