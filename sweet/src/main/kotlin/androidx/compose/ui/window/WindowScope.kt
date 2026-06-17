package androidx.compose.ui.window

import androidx.compose.runtime.Stable
import org.eclipse.swt.widgets.Shell

/**
 * Sweet Compose window scopes for SWT integration.
 * Provides access to underlying SWT Shell for platform-specific operations.
 *
 * Base scope for all window types
 */
@Stable
interface WindowScope {
    val window: Any // Platform window (SWT Shell)
}

/**
 * Scope for main application windows
 */
@Stable
interface FrameWindowScope : WindowScope {
    override val window: Shell // SWT Shell for main windows
}

/**
 * Scope for modal dialog windows
 */
@Stable
interface DialogWindowScope : WindowScope {
    override val window: Shell // SWT Shell for dialogs
}
