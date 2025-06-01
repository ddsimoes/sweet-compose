@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.ddsimoes.sweet.coroutines

import kotlinx.coroutines.*
import org.eclipse.swt.widgets.Display
import kotlin.coroutines.CoroutineContext

// Debug utility - inline to avoid dependency issues - DISABLED for cleaner output
private fun debugLog(component: String, message: String) {
    // Disabled for cleaner output
}

@InternalCoroutinesApi
class SWTImmediateDispatcher(
    private val display: Display
) : MainCoroutineDispatcher(), Delay {
    override val immediate: MainCoroutineDispatcher
        get() = throw UnsupportedOperationException()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val isUIThread = display.thread == Thread.currentThread()
        debugLog("IMMEDIATE_DISPATCHER", "dispatch() called, isUIThread=$isUIThread")
        
        if (isUIThread) {
            debugLog("IMMEDIATE_DISPATCHER", "Executing block immediately on UI thread")
            try {
                block.run()
            } catch (e: Exception) {
                debugLog("IMMEDIATE_DISPATCHER", "Exception in immediate block: ${e.message}")
                e.printStackTrace()
            }
        } else {
            debugLog("IMMEDIATE_DISPATCHER", "Dispatching block to UI thread via asyncExec")
            display.asyncExec {
                debugLog("IMMEDIATE_DISPATCHER", "Executing async block on UI thread")
                try {
                    block.run()
                } catch (e: Exception) {
                    debugLog("IMMEDIATE_DISPATCHER", "Exception in async block: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return display.thread != Thread.currentThread()
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val runnable = Runnable {
            with(continuation) { resumeUndispatched(Unit) }
        }

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