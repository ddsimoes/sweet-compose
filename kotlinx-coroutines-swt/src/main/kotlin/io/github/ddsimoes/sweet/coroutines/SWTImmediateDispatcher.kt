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
}

@InternalCoroutinesApi
class SWTImmediateDispatcher(
    private val display: Display,
) : MainCoroutineDispatcher(),
    Delay {
    override val immediate: MainCoroutineDispatcher get() = this

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (display.isDisposed) {
            // Display gone: drop the work rather than reading display.thread (which throws
            // ERROR_DEVICE_DISPOSED) or scheduling onto a dead display.
            return
        }
        val isUIThread = display.thread == Thread.currentThread()
        debugLog("IMMEDIATE_DISPATCHER", "dispatch() called, isUIThread=$isUIThread")

        if (isUIThread) {
            debugLog("IMMEDIATE_DISPATCHER", "Executing block immediately on UI thread")
            @Suppress("TooGenericExceptionCaught")
            try {
                block.run()
            } catch (e: Exception) {
                logDispatcherWarningOnce(
                    context = "SWTImmediateDispatcher.dispatch.immediate",
                    message = "Exception in SWTImmediateDispatcher immediate block; suppressing repeated dispatcher exception logs.",
                    throwable = e,
                )
            }
        } else {
            debugLog("IMMEDIATE_DISPATCHER", "Dispatching block to UI thread via asyncExec")
            @Suppress("TooGenericExceptionCaught")
            display.asyncExec {
                debugLog("IMMEDIATE_DISPATCHER", "Executing async block on UI thread")
                try {
                    block.run()
                } catch (e: Exception) {
                    logDispatcherWarningOnce(
                        context = "SWTImmediateDispatcher.dispatch.async",
                        message = "Exception in SWTImmediateDispatcher async block; suppressing repeated dispatcher exception logs.",
                        throwable = e,
                    )
                }
            }
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        // On a disposed display, route through dispatch() (which drops) instead of reading
        // display.thread, which throws ERROR_DEVICE_DISPOSED, or running inline on an arbitrary thread.
        if (display.isDisposed) return true
        return display.thread != Thread.currentThread()
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>,
    ) {
        if (display.isDisposed) return
        val runnable = Runnable { with(continuation) { resumeUndispatched(Unit) } }

        if (display.thread == Thread.currentThread()) {
            display.timerExec(timeMillis.toInt(), runnable)
        } else {
            display.asyncExec {
                display.timerExec(timeMillis.toInt(), runnable)
            }
        }
    }

    override fun toString(): String = "SWTImmediateDispatcher"
}
