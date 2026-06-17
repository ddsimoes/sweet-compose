package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.window.KeyEvent
import androidx.compose.ui.window.routeKeyEvent
import androidx.compose.ui.window.swtEventToKeyEvent
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Shell

/**
 * Installs SWT key-down routing on the shell that delegates to Compose-style
 * [onPreviewKeyEvent] / [onKeyEvent] callbacks via [routeKeyEvent].
 */
internal fun Shell.configureShellKeyEvents(
    onPreviewKeyEvent: (KeyEvent) -> Boolean,
    onKeyEvent: (KeyEvent) -> Boolean,
) {
    addListener(SWT.KeyDown) { e ->
        val swtEvent = e as org.eclipse.swt.widgets.Event
        val keyEvent = swtEventToKeyEvent(swtEvent)
        routeKeyEvent(
            event = swtEvent,
            keyEvent = keyEvent,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
        )
    }
}
