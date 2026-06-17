package io.github.ddsimoes.sweet.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Widget

fun Display.coroutineScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

fun Widget.coroutineScope(): CoroutineScope {
    val scope = display.coroutineScope()
    addDisposeListener { scope.cancel("Widget disposed") }
    return scope
}

inline fun Display.runOnUIThread(crossinline action: () -> Unit) {
    if (thread == Thread.currentThread()) {
        action()
    } else {
        asyncExec { action() }
    }
}

inline fun Display.runOnUIThreadSync(crossinline action: () -> Unit) {
    if (thread == Thread.currentThread()) {
        action()
    } else {
        syncExec { action() }
    }
}
