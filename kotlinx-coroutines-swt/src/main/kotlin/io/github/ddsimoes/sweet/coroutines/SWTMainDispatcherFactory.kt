package io.github.ddsimoes.sweet.coroutines

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import org.eclipse.swt.widgets.Display

@InternalCoroutinesApi
@Suppress("MagicNumber")
class SWTMainDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int = 100
    private val displayKey = MainCoroutineDispatcher::class.qualifiedName

    @Suppress("TooGenericExceptionCaught")
    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        return try {
            val display = Display.getCurrent() ?: Display.getDefault()

            val dispatcher = display.getData(displayKey) as MainCoroutineDispatcher?
            if (dispatcher != null) {
                return dispatcher
            }

            SWTMainCoroutineDispatcher(display).also { dispatcher ->
                display.setData(displayKey, dispatcher)
            }
        } catch (e: Exception) {
            throw IllegalStateException("SWT Display is not available", e)
        }
    }

    override fun hintOnError(): String {
        return "Make sure SWT is available and Display is initialized"
    }
}
