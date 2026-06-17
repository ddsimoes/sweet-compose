package io.github.ddsimoes.sweet.coroutines

import java.util.concurrent.ConcurrentHashMap

private val loggedDispatcherWarnings = ConcurrentHashMap.newKeySet<String>()

internal fun logDispatcherWarningOnce(
    context: String,
    message: String,
    throwable: Throwable? = null,
) {
    if (!loggedDispatcherWarnings.add(context)) {
        return
    }

    System.err.println("WARNING: $message")
    throwable?.printStackTrace()
}
