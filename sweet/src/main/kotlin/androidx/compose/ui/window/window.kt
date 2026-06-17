@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toPxInt
import io.github.ddsimoes.sweet.compose.ComposeManager
import io.github.ddsimoes.sweet.compose.locals.ApplicationScope
import io.github.ddsimoes.sweet.compose.locals.ApplicationScopeImpl
import io.github.ddsimoes.sweet.compose.locals.LocalApplicationScope
import io.github.ddsimoes.sweet.compose.locals.LocalComposeManager
import io.github.ddsimoes.sweet.compose.locals.LocalDisplay
import io.github.ddsimoes.sweet.compose.locals.LocalWindowScope
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.configureShellAlwaysOnTop
import io.github.ddsimoes.sweet.internal.configureShellKeyEvents
import io.github.ddsimoes.sweet.internal.dispatchSwtEventsOrSleep
import io.github.ddsimoes.sweet.internal.getSweetDensity
import io.github.ddsimoes.sweet.internal.setupWindowStateSync
import io.github.ddsimoes.sweet.internal.shellStyleFromDecoration
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import kotlin.system.exitProcess

/**
 * Minimal key event abstraction for Sweet window APIs.
 *
 * Wraps the underlying SWT [org.eclipse.swt.widgets.Event] and exposes a subset of
 * commonly used properties. This can be evolved toward Compose's KeyEvent model later.
 */
data class KeyEvent(
    val keyCode: Int,
    val character: Char?,
    val stateMask: Int,
    val isAltDown: Boolean,
    val isCtrlDown: Boolean,
    val isShiftDown: Boolean,
    val isMetaDown: Boolean,
    val rawEvent: org.eclipse.swt.widgets.Event,
)

internal fun swtEventToKeyEvent(event: org.eclipse.swt.widgets.Event): KeyEvent =
    KeyEvent(
        keyCode = event.keyCode,
        character = event.character.takeIf { it.code != 0 },
        stateMask = event.stateMask,
        isAltDown = (event.stateMask and SWT.ALT) != 0,
        isCtrlDown = (event.stateMask and SWT.CTRL) != 0,
        isShiftDown = (event.stateMask and SWT.SHIFT) != 0,
        isMetaDown = (event.stateMask and SWT.COMMAND) != 0,
        rawEvent = event,
    )

/**
 * Core routing logic for key events. Shared between the window listener and tests so we can
 * precisely validate how SWT events are neutralized when consumed.
 */
internal fun routeKeyEvent(
    event: org.eclipse.swt.widgets.Event,
    keyEvent: KeyEvent,
    onPreviewKeyEvent: (KeyEvent) -> Boolean,
    onKeyEvent: (KeyEvent) -> Boolean,
) {
    // Preview handler gets first chance. If it returns true, treat the event
    // as consumed by neutralizing it for downstream listeners.
    if (onPreviewKeyEvent(keyEvent)) {
        event.type = SWT.None
        event.doit = false
        return
    }

    // Then regular handler
    if (onKeyEvent(keyEvent)) {
        event.type = SWT.None
        event.doit = false
    }
}

internal fun applyWindowStateToShell(
    shell: Shell,
    state: WindowState,
    density: Density,
) {
    if (shell.isDisposed) return

    // Placement: map to SWT maximized / fullscreen flags
    when (state.placement) {
        WindowPlacement.Floating -> {
            shell.fullScreen = false
            shell.maximized = false
        }

        WindowPlacement.Maximized -> {
            shell.fullScreen = false
            shell.maximized = true
        }

        WindowPlacement.Fullscreen -> {
            shell.fullScreen = true
        }
    }

    // Minimized flag
    shell.minimized = state.isMinimized

    // Size: Shell → WindowState sync is one-way (see setupWindowStateSync).
    // We do NOT write size back to the Shell because it creates an amplifying
    // feedback loop: WM resize → controlResized → state.size change →
    // LaunchedEffect → setSize → gtk_window_resize → WM may compute different
    // height (text re-wrap) → controlResized → repeat, with monotonic height
    // growth during horizontal-only resize. The Shell size is managed by the
    // WM; WindowState.size is only updated by the WM callback, never forced
    // back onto the Shell from Compose state.

    // Position
    when (val position = state.position) {
        is WindowPosition.Absolute -> {
            if (position.isSpecified) {
                val targetX = (position.x.value * density.density).toInt()
                val targetY = (position.y.value * density.density).toInt()
                val currentLocation = shell.location
                if (currentLocation.x != targetX || currentLocation.y != targetY) {
                    shell.location =
                        org.eclipse.swt.graphics
                            .Point(targetX, targetY)
                }
            }
        }

        is WindowPosition.Aligned -> {
            val display = shell.display ?: Display.getDefault()
            val bounds = display.primaryMonitor.bounds
            val shellSize = shell.size

            val centerX = bounds.x + (bounds.width - shellSize.x) / 2
            val centerY = bounds.y + (bounds.height - shellSize.y) / 2

            val alignment = position.alignment

            val targetX =
                when (alignment) {
                    Alignment.TopStart,
                    Alignment.BottomStart,
                    -> bounds.x

                    Alignment.TopCenter,
                    Alignment.BottomCenter,
                    Alignment.Center,
                    -> centerX

                    Alignment.TopEnd,
                    Alignment.BottomEnd,
                    -> bounds.x + bounds.width - shellSize.x

                    else -> centerX
                }
            val targetY =
                when (alignment) {
                    Alignment.TopStart,
                    Alignment.TopCenter,
                    Alignment.TopEnd,
                    -> bounds.y

                    Alignment.BottomStart,
                    Alignment.BottomCenter,
                    Alignment.BottomEnd,
                    -> bounds.y + bounds.height - shellSize.y

                    else -> centerY
                }

            val currentLocation = shell.location
            if (currentLocation.x != targetX || currentLocation.y != targetY) {
                shell.location =
                    org.eclipse.swt.graphics
                        .Point(targetX, targetY)
            }
        }

        WindowPosition.PlatformDefault -> {
            // Platform-default positioning; no explicit location applied.
        }
    }
}

/**
 * Sweet Compose WindowState with mutable properties for bidirectional sync
 */
interface WindowState {
    var placement: WindowPlacement
    var isMinimized: Boolean
    var position: WindowPosition
    var size: DpSize
}

/**
 * Factory function for creating WindowState instances
 */
fun WindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: DpSize = DpSize(800.dp, 600.dp),
): WindowState = WindowStateImpl(placement, isMinimized, position, size)

/**
 * Internal mutable implementation of WindowState
 */
private class WindowStateImpl(
    placement: WindowPlacement,
    isMinimized: Boolean,
    position: WindowPosition,
    size: DpSize,
) : WindowState {
    override var placement by mutableStateOf(placement)
    override var isMinimized by mutableStateOf(isMinimized)
    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)
}

@Composable
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: DpSize = DpSize(800.dp, 600.dp),
): WindowState =
    remember {
        WindowState(placement, isMinimized, position, size)
    }

@Composable
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    width: Dp = 800.dp,
    height: Dp = 600.dp,
): WindowState =
    remember {
        WindowState(placement, isMinimized, position, DpSize(width, height))
    }

enum class WindowPlacement {
    /**
     * Window don't occupy the all available space and can be moved and resized by the user.
     */
    Floating,

    /**
     * The window is maximized and occupies all available space on the screen excluding
     * the space that is occupied by the screen insets (taskbar/dock and top-level application menu
     * on macOs).
     */
    Maximized,

    /**
     * The window is in fullscreen mode and occupies all available space of the screen,
     * including the space that is occupied by the screen insets (taskbar/dock and top-level
     * application menu on macOs).
     */
    Fullscreen,
}

/**
 * Modern Window API with WindowDecoration parameter
 */
@ExperimentalComposeUiApi
@Composable
fun ApplicationScope.Window(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration = WindowDecoration.SystemDefault,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    val composeManager = manager
    val density = display.getSweetDensity()

    // Keep latest key event handlers across recompositions
    val currentPreviewKeyEvent by rememberUpdatedState(onPreviewKeyEvent)
    val currentKeyEvent by rememberUpdatedState(onKeyEvent)

    val shell =
        remember {
            val style = shellStyleFromDecoration(decoration, resizable)

            Shell(display, style).apply {
                SweetDebugger.log("Windows", "Creating shell")

                // Basic properties
                text = title
                isEnabled = enabled

                // Initial size from WindowState
                setSize(state.size.width.toPxInt(density), state.size.height.toPxInt(density))

                // Layout root
                layout = SweetLayout()

                // Close handling
                addShellListener(
                    object : ShellAdapter() {
                        override fun shellClosed(e: ShellEvent) {
                            onCloseRequest()
                        }
                    },
                )

                configureShellAlwaysOnTop(display, alwaysOnTop)
                setupWindowStateSync(state, density)
                configureShellKeyEvents(
                    onPreviewKeyEvent = currentPreviewKeyEvent,
                    onKeyEvent = currentKeyEvent,
                )
            }
        }

    // Create a composite inside the shell for proper layout
    val composite =
        remember {
            org.eclipse.swt.widgets.Composite(shell, SWT.NONE).apply {
                layout = SweetLayout()
                // Ensure the root composite fills the window client area
                updateSweetCompositionData {
                    withLayoutData { copy(fillMaxWidth = true, fillMaxHeight = true) }
                }
            }
        }

    // Setup window-specific compose scope using the composite
    val windowScope =
        remember {
            composeManager.createScope(composite)
        }

    // Apply WindowState changes back to the Shell (WindowState → Shell sync).
    // NOTE: state.size is intentionally NOT a key — size sync is one-way
    // (Shell → WindowState only). Writing size back to the Shell causes an
    // amplifying feedback loop during user resize.
    LaunchedEffect(state.placement, state.isMinimized, state.position) {
        applyWindowStateToShell(shell, state, density)
    }

    LaunchedEffect(visible) {
        if (visible) shell.open() else shell.setVisible(false)
    }

    DisposableEffect(shell) {
        onDispose {
            if (!shell.isDisposed) {
                shell.dispose()
            }
        }
    }

    // Set content for this window
    SideEffect {
        windowScope.setContent {
            val frameScope =
                object : FrameWindowScope {
                    override val window: Shell get() = shell
                }
            CompositionLocalProvider(
                LocalWindowScope provides
                    io.github.ddsimoes.sweet.compose.locals
                        .WindowScopeImpl(shell),
                LocalApplicationScope provides this@Window,
                LocalDisplay provides this@Window.display,
                LocalComposeManager provides composeManager,
            ) {
                frameScope.content()
            }
        }
    }
}

/**
 * Compose Multiplatform compatible application function
 */
fun application(
    exitProcessOnExit: Boolean = true,
    content: @Composable ApplicationScope.() -> Unit,
) {
    // Check if we're already in a display context
    val existingDisplay = Display.getCurrent()

    if (existingDisplay != null) {
        // We're in an embedded context - just run the content
        runApplicationContent(existingDisplay, exitProcessOnExit, content)
    } else {
        // Normal application startup
        val display = Display()
        runApplicationContent(display, exitProcessOnExit, content)

        // Event loop with compose integration
        while (!display.isDisposed) {
            display.dispatchSwtEventsOrSleep("application")
        }

        if (!display.isDisposed) {
            display.dispose()
        }
    }
}

private fun runApplicationContent(
    display: Display,
    exitProcessOnExit: Boolean,
    content: @Composable ApplicationScope.() -> Unit,
) {
    val composeManager = ComposeManager.getOrCreate(display)
    val scope = ApplicationScopeImpl(display, composeManager)

    // Store the application context in display data
    display.setData("sweet.application.scope", scope)

    // Instead of creating a shell, we'll run the composition directly
    // using the ComposeManager. This will allow Window composables to create their own shells
    // without requiring a parent shell.

    // Create a direct composition without requiring a shell
    val applicationComposition = composeManager.createApplicationComposition()

    applicationComposition.setContent {
        CompositionLocalProvider(
            LocalApplicationScope provides scope,
            LocalDisplay provides display,
        ) {
            content(scope)
        }
    }
}

/**
 * Compose Multiplatform compatible application function with external display management
 */
fun application(
    display: Display,
    exitProcessOnExit: Boolean = true,
    content: @Composable ApplicationScope.() -> Unit,
) {
    runApplicationContent(display, exitProcessOnExit, content)
    // Note: Event loop is managed externally when display is provided
}

/**
 * Exit the application
 */
fun exitApplication() {
    exitProcess(0)
}

/**
 * Single window application helper functions for Compose Desktop compatibility
 */
@ExperimentalComposeUiApi
fun singleWindowApplication(
    state: WindowState = WindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    decoration: WindowDecoration = WindowDecoration.SystemDefault,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    exitProcessOnExit: Boolean = true,
    content: @Composable FrameWindowScope.() -> Unit,
) {
    application(exitProcessOnExit = exitProcessOnExit) {
        Window(
            onCloseRequest = { exitApplication() },
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            decoration = decoration,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            content = content,
        )
    }
}
