package io.github.ddsimoes.sweet.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import org.eclipse.swt.widgets.Display

@InternalCoroutinesApi
class SWTMainDispatcherFactory : MainDispatcherFactory {

    override val loadPriority: Int = 100

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        return try {
            val display = Display.getCurrent() ?: Display.getDefault()
            SWTMainCoroutineDispatcher(display)
        } catch (e: Exception) {
            throw IllegalStateException("SWT Display is not available", e)
        }
    }

    override fun hintOnError(): String {
        return "Make sure SWT is available and Display is initialized"
    }
}