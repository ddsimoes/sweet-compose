package androidx.compose.ui.window

import androidx.compose.runtime.Immutable

/**
 * Properties to configure a dialog.
 *
 * @param dismissOnBackPress whether the dialog should be dismissed on back press
 * @param dismissOnClickOutside whether the dialog should be dismissed on click outside
 * @param usePlatformDefaultWidth whether the dialog content width should be limited to the platform default
 */
@Immutable
data class DialogProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
    val usePlatformDefaultWidth: Boolean = true,
)
