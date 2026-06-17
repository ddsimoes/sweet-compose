@file:Suppress("MatchingDeclarationName")

package io.github.ddsimoes.sweet.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.debug.SwtDebugger
import io.github.ddsimoes.sweet.layout.SweetLayout
import io.github.ddsimoes.sweet.drawing.rasterizeToImage
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ControlAdapter
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

data class WindowConfig(
    val title: String = "Compose App",
    val width: Int = 800,
    val height: Int = 600,
    val resizable: Boolean = true,
    val enabled: Boolean = true,
    val focusable: Boolean = true,
    val alwaysOnTop: Boolean = false,
    val visible: Boolean = true,
    val icon: Painter? = null,
    val decoration: Any? = null, // WindowDecoration
    val onCloseRequest: (() -> Unit)? = null,
    val windowState: androidx.compose.ui.window.WindowState? = null, // For bidirectional sync
)

/**
 * Maps a [androidx.compose.ui.window.WindowDecoration] and resizable flag to the corresponding
 * SWT shell style bits. Shared between [runCompose] and the [androidx.compose.ui.window.Window]
 * composable so shell creation is consistent across both window paths.
 */
internal fun shellStyleFromDecoration(
    decoration: Any?, // androidx.compose.ui.window.WindowDecoration?
    resizable: Boolean,
): Int {
    return when (decoration) {
        is androidx.compose.ui.window.WindowDecoration.SystemDefault,
        null,
        -> {
            if (resizable) {
                SWT.SHELL_TRIM
            } else {
                SWT.TITLE or SWT.CLOSE or SWT.MIN or SWT.MAX
            }
        }
        is androidx.compose.ui.window.UndecoratedWindowDecoration -> SWT.NO_TRIM
        else -> SWT.SHELL_TRIM
    }
}

fun runCompose(
    config: WindowConfig = WindowConfig(),
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log("runCompose", "Starting Sweet Compose application")
        SwtDebugger.debug("runCompose called with config: title='${config.title}', size=${config.width}x${config.height}")
    }

    val display = Display.getDefault() ?: Display()

    val density = display.getSweetDensity()

    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "runCompose",
            "Display created: ${display.javaClass.simpleName}, density: ${density.density}",
        )
    }

    try {
        val shellStyle = shellStyleFromDecoration(config.decoration, config.resizable)

        val shell =
            Shell(display, shellStyle).apply {
                SweetDebugger.log("runCompose", "Creating shell")

                text = config.title
                setSize(config.width, config.height)
                layout = SweetLayout()
                isEnabled = config.enabled

                // Apply focusable window state
                if (!config.focusable) {
                    // SWT doesn't have a direct way to make windows non-focusable
                    // This would require more complex implementation with shell styles
                }

                configureShellAlwaysOnTop(display, config.alwaysOnTop)

                // Rasterize the window icon Painter to an SWT Image and apply it. The shell does
                // not own the Image, so dispose it when the shell goes down.
                config.icon?.let { painter ->
                    val icon = painter.rasterizeToImage(display)
                    if (icon != null) {
                        setImage(icon)
                        addDisposeListener { if (!icon.isDisposed) icon.dispose() }
                    } else if (SweetDebugger.assertionEnabled) {
                        SweetDebugger.log("WindowSystem", "Window icon Painter has no intrinsic size; ignored")
                    }
                }

                // Handle close request callback
                config.onCloseRequest?.let { closeCallback ->
                    addShellListener(
                        object : ShellAdapter() {
                            override fun shellClosed(e: ShellEvent) {
                                // Prevent default close and call our callback
                                e.doit = false
                                closeCallback()
                            }
                        },
                    )
                }

                // Set up bidirectional WindowState synchronization
                val windowStateToSync = config.windowState
                windowStateToSync?.let { windowState ->
                    setupWindowStateSync(windowState, density)
                }
            }

        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log("runCompose", "Shell configured, creating ComposeScope")
        }

        val composeManager =
            io.github.ddsimoes.sweet.compose.ComposeManager
                .getOrCreate(display)
        val composeScope = composeManager.createScope(shell)

        try {
            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log("runCompose", "Setting composition content")
            }
            composeScope.setContent(content)

            // Handle window visibility
            if (config.visible) {
                shell.open()

                val bounds = display.primaryMonitor.bounds
                val shellBounds = shell.bounds
                shell.location =
                    Point(
                        (bounds.width - shellBounds.width) / 2,
                        (bounds.height - shellBounds.height) / 2,
                    )
            } else {
                // Create but don't show the window
                shell.pack()
            }

            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log("runCompose", "Entering main event loop")
            }

            while (!shell.isDisposed) {
                display.dispatchSwtEventsOrSleep("runCompose")
            }

            if (SweetDebugger.assertionEnabled) {
                SweetDebugger.log("runCompose", "Event loop ended, shell disposed")
            }
        } finally {
            composeScope.dispose()
        }
    } finally {
        if (!display.isDisposed) {
            display.dispose()
        }
    }
}

/**
 * Sets up bidirectional synchronization between WindowState and SWT Shell
 */
internal fun Shell.setupWindowStateSync(
    windowState: androidx.compose.ui.window.WindowState,
    density: Density,
) {
    // Convert SWT coordinates to Compose units
    fun swtToDp(pixels: Int): androidx.compose.ui.unit.Dp = (pixels / density.density).dp

    // Convert Compose units to SWT coordinates
    fun dpToSwt(dp: androidx.compose.ui.unit.Dp): Int = (dp.value * density.density).toInt()

    // Shell → WindowState sync: Listen to SWT shell events
    addControlListener(
        object : ControlAdapter() {
            override fun controlResized(e: ControlEvent?) {
                if (!isDisposed) {
                    val size = size
                    windowState.size = DpSize(swtToDp(size.x), swtToDp(size.y))
                }
            }

            override fun controlMoved(e: ControlEvent?) {
                if (!isDisposed) {
                    val location = location
                    windowState.position =
                        androidx.compose.ui.window.WindowPosition.Absolute(
                            swtToDp(location.x),
                            swtToDp(location.y),
                        )
                }
            }
        },
    )

    // Handle placement changes (maximize/minimize/restore)
    addShellListener(
        object : ShellAdapter() {
            override fun shellIconified(e: ShellEvent?) {
                windowState.isMinimized = true
            }

            override fun shellDeiconified(e: ShellEvent?) {
                windowState.isMinimized = false
            }
        },
    )

    // Initial sync from WindowState → Shell (if needed)
    // Note: Most properties are already applied during shell creation,
    // but we can add dynamic updates here if the WindowState changes after creation
}

/**
 * Installs a best-effort "always-on-top" listener on the shell. When another window is
 * activated, the shell forces itself back to the front via [Shell.forceActive] and
 * [Shell.forceFocus]. Shared between [runCompose] and the Window composable.
 */
internal fun Shell.configureShellAlwaysOnTop(
    display: Display,
    alwaysOnTop: Boolean,
) {
    if (!alwaysOnTop) return
    addShellListener(
        object : ShellAdapter() {
            override fun shellDeactivated(e: ShellEvent) {
                if (!isDisposed) {
                    display.asyncExec {
                        if (!isDisposed) {
                            forceActive()
                            forceFocus()
                        }
                    }
                }
            }
        },
    )
}
