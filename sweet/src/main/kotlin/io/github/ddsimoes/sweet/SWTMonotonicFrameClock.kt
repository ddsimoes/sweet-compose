package io.github.ddsimoes.sweet

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import io.github.ddsimoes.sweet.coroutines.runOnUIThread
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.eclipse.swt.widgets.Display
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Thread-safe frame clock implementation for SWT that properly handles multiple awaiters
 */
class SWTMonotonicFrameClock(
    private val display: Display
) : MonotonicFrameClock, CoroutineContext.Element {

    override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock

    private val frameClock = BroadcastFrameClock()
    private val isRunning = AtomicBoolean(false)
    private var frameJob: Job? = null

    // Channel para comunicação thread-safe entre threads
    private val frameRequestChannel = Channel<Unit>(Channel.UNLIMITED)

    init {
        SweetDebugger.log("FRAME_CLOCK", "SWTMonotonicFrameClock created for display: $display")
    }

    /**
     * Start the frame clock - should be called once when composition starts
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            SweetDebugger.log("FRAME_CLOCK", "start() called but frame clock already running")
            return
        }

        SweetDebugger.log("FRAME_CLOCK", "Starting frame clock")

        // Start frame processing coroutine no contexto do UI dispatcher
        frameJob = CoroutineScope(Dispatchers.Main).launch {
            SweetDebugger.log("FRAME_CLOCK", "Frame processing coroutine started on ${Thread.currentThread().name}")

            // Processa requisições de frame de forma thread-safe
            frameRequestChannel.consumeEach {
                try {
                    if (display.isDisposed) {
                        SweetDebugger.log("FRAME_CLOCK", "Display disposed, stopping frame processing")
                        return@launch
                    }

                    val timeNanos = System.nanoTime()
                    SweetDebugger.log("FRAME_CLOCK", "Processing frame request, sending frame at $timeNanos")

                    // Envia frame sempre na UI thread
                    display.runOnUIThread {
                        frameClock.sendFrame(timeNanos)
                        SweetDebugger.log("FRAME_CLOCK", "Frame sent successfully at $timeNanos")
                    }
                } catch (e: Exception) {
                    SweetDebugger.log("FRAME_CLOCK", "Exception processing frame", e)
                }
            }
        }

        // Envia frame inicial
        requestFrame()
    }

    /**
     * Stop the frame clock
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            SweetDebugger.log("FRAME_CLOCK", "stop() called but frame clock not running")
            return
        }

        SweetDebugger.log("FRAME_CLOCK", "Stopping frame clock")
        frameJob?.cancel()
        frameJob = null
        frameRequestChannel.close()
    }

    /**
     * Request a frame to be sent. Thread-safe method.
     */
    fun requestFrame() {
        if (!isRunning.get()) {
            SweetDebugger.log("FRAME_CLOCK", "Frame requested but clock not running")
            return
        }

        SweetDebugger.log("FRAME_CLOCK", "Frame requested from ${Thread.currentThread().name}")
        // Envia requisição de forma não-bloqueante
        frameRequestChannel.trySend(Unit)
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        SweetDebugger.log("FRAME_CLOCK", "withFrameNanos called on ${Thread.currentThread().name} - awaiting frame")

        return frameClock.withFrameNanos { frameTimeNanos ->
            SweetDebugger.log("FRAME_CLOCK", "Frame callback invoked with time $frameTimeNanos on ${Thread.currentThread().name}")
            val result = onFrame(frameTimeNanos)
            SweetDebugger.log("FRAME_CLOCK", "Frame callback completed, result: $result")
            result
        }
    }

    /**
     * Check if the frame clock has awaiters
     */
    val hasAwaiters: Boolean
        get() = frameClock.hasAwaiters
}

/**
 * Extension to create a frame clock for a display
 */
fun Display.createFrameClock(): SWTMonotonicFrameClock {
    return SWTMonotonicFrameClock(this)
}