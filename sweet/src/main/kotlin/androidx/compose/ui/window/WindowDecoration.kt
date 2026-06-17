@file:Suppress("FunctionName")

package androidx.compose.ui.window

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sweet Compose window decoration API for SWT integration.
 * Maps to SWT shell styles and provides undecorated window support.
 *
 * TODO: Replace with proper ExperimentalComposeUiApi when available
 *
 */

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalComposeUiApi

@ExperimentalComposeUiApi
sealed interface WindowDecoration {
    /**
     * Use system default window decorations (SWT.SHELL_TRIM)
     */
    data object SystemDefault : WindowDecoration

    companion object {
        /**
         * Create undecorated window with optional resizer thickness
         * Maps to SWT.NO_TRIM with custom resize handling
         */
        fun Undecorated(resizerThickness: Dp = WindowDecorationDefaults.ResizerThickness): WindowDecoration = UndecoratedWindowDecoration(resizerThickness)
    }
}

@ExperimentalComposeUiApi
object WindowDecorationDefaults {
    val ResizerThickness: Dp = 8.dp
}

/**
 * Internal implementation for undecorated windows
 */
internal data class UndecoratedWindowDecoration(
    val resizerThickness: Dp,
) : WindowDecoration

/**
 * Internal helper to convert legacy boolean to decoration
 */
internal fun windowDecorationFromFlag(undecorated: Boolean): WindowDecoration =
    if (undecorated) {
        WindowDecoration.Undecorated()
    } else {
        WindowDecoration.SystemDefault
    }
