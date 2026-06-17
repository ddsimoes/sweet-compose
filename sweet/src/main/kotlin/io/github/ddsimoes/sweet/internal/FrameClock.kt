@file:Suppress("MatchingDeclarationName")

package io.github.ddsimoes.sweet.internal

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import io.github.ddsimoes.sweet.coroutines.runOnUIThread
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.layout.LayoutCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.eclipse.swt.widgets.Display
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal class SWTMonotonicFrameClock(
    private val display: Display,
) : MonotonicFrameClock,
    CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock

    /** Target frame interval in milliseconds. Overridable for tests. */
    @Volatile var targetFrameMs: Long = 16L

    private val frameClock = BroadcastFrameClock()
    private val isRunning = AtomicBoolean(false)
    private var frameJob: Job? = null

    // CONFLATED: multiple requestFrame() calls before dispatch collapse to one frame
    private val frameRequestChannel = Channel<Unit>(Channel.CONFLATED)

    fun start(scope: CoroutineScope) {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }

        frameJob =
            scope.launch {
                frameRequestChannel.consumeEach {
                    try {
                        if (display.isDisposed) {
                            if (SweetDebugger.assertionEnabled) {
                                SweetDebugger.log("SWTMonotonicFrameClock", "Display disposed, stopping frame clock")
                            }
                            return@launch
                        }

                        val timeNanos = System.nanoTime()
                        val coordinator = LayoutCoordinator.forDisplay(display)

                        // Dispatch inline when already on the display thread (e.g. during
                        // initial composition) — avoids deadlock where runOnUIThread posts
                        // to the event queue but composition hasn't yielded yet.
                        val dispatchFrame: () -> Unit = {
                            coordinator.beginFrame()
                            try {
                                frameClock.sendFrame(timeNanos)
                                frameCount++
                                lastFrameTimeNanos = timeNanos
                            } finally {
                                coordinator.endFrame()
                            }
                        }

                        if (display.thread == Thread.currentThread()) {
                            dispatchFrame()
                        } else {
                            display.runOnUIThread { dispatchFrame() }
                        }
                        // If there are awaiters (animation loops), schedule the next frame
                        // at the target interval to prevent uncapped busy-looping.
                        if (frameClock.hasAwaiters) {
                            val elapsedMs = (System.nanoTime() - timeNanos) / 1_000_000
                            val delay = (targetFrameMs - elapsedMs).coerceIn(1L, targetFrameMs)
                            if (!display.isDisposed) {
                                display.timerExec(delay.toInt()) {
                                    if (!display.isDisposed) {
                                        requestFrame()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (SweetDebugger.assertionEnabled) {
                            SweetDebugger.log("SWTMonotonicFrameClock", "Error in frame processing", e)
                        }
                        e.printStackTrace()
                    }
                }
            }

        requestFrame()
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }

        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log("SWTMonotonicFrameClock", "Stopping frame clock")
        }

        frameJob?.cancel()
        frameJob = null
        frameRequestChannel.close()
    }

    fun requestFrame() {
        if (!isRunning.get()) {
            return
        }
        frameRequestChannel.trySend(Unit)
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        // Ensure a frame is dispatched for this new awaiter — even if no
        // state write triggers one (fixes the stall when only animation
        // awaiters are present with no recomposition).
        requestFrame()
        return frameClock.withFrameNanos(onFrame)
    }

    val hasAwaiters: Boolean
        get() = frameClock.hasAwaiters

    // ── Test probes ──────────────────────────────────────────────────────

    /** Number of frames dispatched since start or last reset. */
    @Volatile var frameCount: Int = 0
        private set

    /** Last frame time in nanos, or -1 if no frame dispatched yet. */
    @Volatile var lastFrameTimeNanos: Long = -1
        private set

    fun resetFrameCount() {
        frameCount = 0
        lastFrameTimeNanos = -1
    }
}

internal fun Display.createFrameClock(): SWTMonotonicFrameClock = SWTMonotonicFrameClock(this)
