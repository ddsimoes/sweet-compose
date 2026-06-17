@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.ddsimoes.sweet.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import org.eclipse.swt.widgets.Display
import kotlin.coroutines.CoroutineContext

// Debug utility - inline to avoid dependency issues - DISABLED for cleaner output
@Suppress("UnusedParameter")
private fun debugLog(
    component: String,
    message: String,
) {
    // Disabled for cleaner output
    // val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
    // val threadName = Thread.currentThread().name
    // val threadId = Thread.currentThread().id
    // val isUIThread = try { Display.getCurrent() != null } catch (e: Exception) { false }
    // val threadInfo = "$threadName[$threadId]${if (isUIThread) "[UI]" else "[BG]"}"
    // println("[$timestamp] [$component] [$threadInfo] $message")
}

@InternalCoroutinesApi
class SWTMainCoroutineDispatcher(
    private val display: Display,
) : MainCoroutineDispatcher(),
    Delay {
    private val immediateDispatcher = SWTImmediateDispatcher(display)

    init {
        debugLog(
            "DISPATCHER",
            "SWTMainCoroutineDispatcher created with display: $display",
        )
    }

    override val immediate: MainCoroutineDispatcher get() = immediateDispatcher

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        val dispatchNeeded = isDispatchNeeded(context)

        if (!dispatchNeeded) {
            block.run()
            return
        }

        debugLog(
            "DISPATCHER",
            "dispatch() called, dispatchNeeded=$dispatchNeeded, display.isDisposed=${display.isDisposed}",
        )

        if (display.isDisposed) {
            debugLog("DISPATCHER", "WARNING: Attempting to dispatch on disposed display!")
            return
        }

        @Suppress("TooGenericExceptionCaught")
        display.asyncExec {
            debugLog("DISPATCHER", "Executing dispatched block on UI thread")
            try {
                block.run()
            } catch (e: Exception) {
                logDispatcherWarningOnce(
                    context = "SWTMainCoroutineDispatcher.dispatch",
                    message = "Exception in SWTMainCoroutineDispatcher dispatched block; suppressing repeated dispatcher exception logs.",
                    throwable = e,
                )
            }
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        // Never return false for a disposed display — that would cause the
        // continuation to run inline on whatever thread is current (F6).
        // Return true instead so the work is routed through dispatch(), which
        // drops it. We must short-circuit before reading display.thread because
        // Display.getThread() throws ERROR_DEVICE_DISPOSED on a disposed display.
        if (display.isDisposed) return true
        val currentThread = Thread.currentThread()
        val displayThread = display.thread
        val needed = displayThread != currentThread
        debugLog(
            "DISPATCHER",
            "isDispatchNeeded() currentThread=$currentThread, displayThread=$displayThread, needed=$needed",
        )
        return needed
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>,
    ) {
        if (display.isDisposed) {
            debugLog("DISPATCHER", "scheduleResumeAfterDelay: display disposed, dropping")
            return
        }
        debugLog(
            "DISPATCHER",
            "scheduleResumeAfterDelay() timeMillis=$timeMillis",
        )
        val runnable =
            Runnable {
                debugLog(
                    "DISPATCHER",
                    "Resuming continuation after delay of ${timeMillis}ms",
                )
                with(continuation) {
                    resumeUndispatched(Unit)
                }
            }
        display.timerExec(timeMillis.toInt(), runnable)
    }

    override fun toString(): String = "SWTMainDispatcher"
}
