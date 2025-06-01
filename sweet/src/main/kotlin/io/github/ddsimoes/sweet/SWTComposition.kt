package io.github.ddsimoes.sweet

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import io.github.ddsimoes.sweet.coroutines.runOnUIThreadSync
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.*
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SWT Composition with proper thread safety and dispatcher management
 */
class SWTComposition(
    private val shell: Shell
) {
    private val display = shell.display

    // Use UI scope consistently - all composition work should be on Main dispatcher
    private val frameClock = display.createFrameClock()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + frameClock)

    private val applier = SWTNodeApplier(RootCompositeNode(shell))
    private val recomposer = Recomposer(uiScope.coroutineContext)
    private val composition = Composition(applier, recomposer)

    private val snapshotManager = SWTSnapshotManager()

    init {
        SweetDebugger.log("COMPOSITION", "EnhancedSWTComposition init - shell: $shell, display: $display")

        // Initialize snapshot manager with UI scope to maintain consistency
        SweetDebugger.trace("COMPOSITION", "Starting snapshot manager") {
            snapshotManager.ensureStarted(uiScope, frameClock)
        }

        SweetDebugger.trace("COMPOSITION", "Starting frame clock") {
            frameClock.start()
        }

        // Start recomposer on UI thread with proper context
        SweetDebugger.log("COMPOSITION", "Starting recomposer on UI thread")
        uiScope.launch {
            SweetDebugger.log("COMPOSITION", "Recomposer UI coroutine started on ${Thread.currentThread().name}")
            SweetDebugger.log("COMPOSITION", "CoroutineContext: ${coroutineContext}")
            SweetDebugger.log("COMPOSITION", "Has MonotonicFrameClock: ${coroutineContext[MonotonicFrameClock] != null}")

            try {
                SweetDebugger.log("COMPOSITION", "Calling runRecomposeAndApplyChanges")
                recomposer.runRecomposeAndApplyChanges()
                SweetDebugger.log("COMPOSITION", "Recomposer runRecomposeAndApplyChanges completed")
            } catch (e: Exception) {
                SweetDebugger.log("COMPOSITION", "Exception in recomposer", e)
                throw e
            }
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        SweetDebugger.trace("COMPOSITION", "setContent") {
            // Ensure we're on UI thread for setting content
            display.runOnUIThreadSync {
                SweetDebugger.log("COMPOSITION", "Setting composition content on ${Thread.currentThread().name}")
                composition.setContent(content)

                // Initial layout pass
                SweetDebugger.log("COMPOSITION", "Performing initial layout")
                applier.root.layout()
                shell.layout(true, true)

                SweetDebugger.log("COMPOSITION", "Content set and layout complete")
                SweetDebugger.logWidgetTree(display)
            }
        }
    }

    fun dispose() {
        SweetDebugger.log("COMPOSITION", "Disposing composition")

        // Stop frame clock first
        frameClock.stop()

        // Dispose composition on UI thread
        uiScope.launch {
            try {
                composition.dispose()
                recomposer.close()
                recomposer.join()
            } finally {
                uiScope.cancel()
            }
        }
    }
}

/**
 * Thread-safe snapshot manager using consistent UI dispatcher
 */
private class SWTSnapshotManager {
    private val started = AtomicBoolean(false)

    fun ensureStarted(uiScope: CoroutineScope, frameClock: SWTMonotonicFrameClock) {
        if (started.compareAndSet(false, true)) {
            SweetDebugger.log("SNAPSHOT", "Starting SWT snapshot manager")

            Snapshot.registerGlobalWriteObserver {
                SweetDebugger.log("SNAPSHOT", "Global write observer triggered on ${Thread.currentThread().name}")

                // Always process snapshot changes on UI thread for consistency
                uiScope.launch {
                    try {
                        SweetDebugger.log("SNAPSHOT", "Processing snapshot notifications on ${Thread.currentThread().name}")
                        Snapshot.sendApplyNotifications()
                        SweetDebugger.log("SNAPSHOT", "Apply notifications sent successfully")

                        // Request frame to trigger recomposition
                        SweetDebugger.log("SNAPSHOT", "Requesting frame for recomposition")
                        frameClock.requestFrame()
                        SweetDebugger.log("SNAPSHOT", "Frame requested successfully")
                    } catch (e: Exception) {
                        SweetDebugger.log("SNAPSHOT", "Exception in snapshot processing", e)
                    }
                }
            }
            SweetDebugger.log("SNAPSHOT", "Global write observer registered")
        }
    }
}

/**
 * Fixed version of runComposeSWT using consistent thread management
 */
fun runComposeSWTWithSWTDispatcher(
    config: SWTWindowConfig = SWTWindowConfig(),
    content: @Composable () -> Unit
) {
    SweetDebugger.log("APP", "Starting runComposeSWTWithSWTDispatcher with config: $config")

    val display = Display.getDefault() ?: Display()
    SweetDebugger.log("APP", "Created display: $display, thread: ${display.thread}")

    try {
        val shell = Shell(display, SWT.SHELL_TRIM).apply {
            text = config.title
            setSize(config.width, config.height)
            layout = GridLayout(1, false)
        }
        SweetDebugger.log("APP", "Created shell: $shell with layout: ${shell.layout}")

        val composition = SWTComposition(shell)

        try {
            // Set content - this will use UI thread internally
            SweetDebugger.trace("APP", "Setting composition content") {
                composition.setContent(content)
            }

            SweetDebugger.log("APP", "Opening shell")
            shell.open()

            // Center the shell on screen
            val bounds = display.primaryMonitor.bounds
            val shellBounds = shell.bounds
            shell.location = org.eclipse.swt.graphics.Point(
                (bounds.width - shellBounds.width) / 2,
                (bounds.height - shellBounds.height) / 2
            )
            SweetDebugger.log("APP", "Shell centered and opened")

            // Standard SWT message loop
            var loopCount = 0
            while (!shell.isDisposed) {
                try {
                    if (!display.readAndDispatch()) {
                        display.sleep()
                    }
                    loopCount++
                    if (loopCount % 1000 == 0) {
                        SweetDebugger.log("APP", "Message loop iteration $loopCount")
                    }
                } catch (e: Exception) {
                    SweetDebugger.log("APP", "Exception in message loop", e)
                    e.printStackTrace()
                }
            }
            SweetDebugger.log("APP", "Message loop ended, shell disposed")

        } finally {
            SweetDebugger.log("APP", "Disposing composition")
            composition.dispose()
            if (!shell.isDisposed) {
                shell.dispose()
            }
        }
    } finally {
        if (!display.isDisposed) {
            SweetDebugger.log("APP", "Disposing display")
            display.dispose()
        }
    }
}