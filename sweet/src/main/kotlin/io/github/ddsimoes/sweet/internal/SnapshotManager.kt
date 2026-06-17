package io.github.ddsimoes.sweet.internal

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.swt.widgets.Display
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Overridable action for test probes; defaults to the real Snapshot.sendApplyNotifications().
 */
internal var snapshotNotifyAction: () -> Unit = { Snapshot.sendApplyNotifications() }
private const val SNAPSHOT_MANAGER_KEY = "sweet.compose.snapshot.manager"

internal class SWTSnapshotManager(
    private val display: Display,
    private val uiScope: CoroutineScope,
    private val frameClock: SWTMonotonicFrameClock,
) {
    private val started = AtomicBoolean(false)
    private var observerHandle: ObserverHandle? = null
    private val scheduled = AtomicBoolean(false)

    init {
        start()
    }

    private fun start() {
        if (started.compareAndSet(false, true)) {
            observerHandle =
                Snapshot.registerGlobalWriteObserver {
                    if (display.isDisposed) return@registerGlobalWriteObserver
                    if (scheduled.compareAndSet(false, true)) {
                        uiScope.launch(Dispatchers.Main) {
                            try {
                                scheduled.set(false)
                                snapshotNotifyAction()
                                frameClock.requestFrame()
                            } catch (e: Exception) {
                                if (SweetDebugger.assertionEnabled) {
                                    SweetDebugger.log("SWTSnapshotManager", "Error in snapshot write observer", e)
                                }
                                e.printStackTrace()
                            }
                        }
                    }
                }
        }
    }

    val isActive: Boolean get() = started.get() && observerHandle != null

    fun dispose() {
        observerHandle?.dispose()
        observerHandle = null
    }

    companion object {
        fun ensureStarted(
            display: Display,
            uiScope: CoroutineScope,
            frameClock: SWTMonotonicFrameClock,
        ): SWTSnapshotManager {
            val existingManager = display.getData(SNAPSHOT_MANAGER_KEY) as? SWTSnapshotManager
            if (existingManager != null) return existingManager
            val manager = SWTSnapshotManager(display, uiScope, frameClock)
            display.setData(SNAPSHOT_MANAGER_KEY, manager)
            return manager
        }
    }
}

/**
 * Helper function to ensure snapshot manager is started - for use by ComposeManager
 */
internal fun ensureSnapshotManager(
    display: Display,
    uiScope: CoroutineScope,
    frameClock: SWTMonotonicFrameClock,
): SWTSnapshotManager {
    return SWTSnapshotManager.ensureStarted(display, uiScope, frameClock)
}
