package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.unit.Density
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates a coroutine scope tied to the widget's lifecycle using [Dispatchers.Main.immediate].
 * The scope is automatically cancelled when the widget is disposed, ensuring event callbacks
 * do not fire after disposal (F5).
 */
internal fun Control.createWidgetCoroutineScope(): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    addDisposeListener { scope.cancel("Widget disposed") }
    return scope
}

private val eventLoopWarningContexts = ConcurrentHashMap.newKeySet<String>()

internal fun Display.dispatchSwtEventsOrSleep(context: String) {
    try {
        if (!readAndDispatch()) {
            sleep()
        }
    } catch (e: Exception) {
        if (eventLoopWarningContexts.add(context)) {
            SweetDebugger.log(
                "SWTEventLoop",
                "WARNING: Exception while dispatching SWT events in $context; suppressing repeated event-loop exception logs",
                e,
            )
        }
    }
}

/**
 * Get the current display density.
 *
 * Prefer passing a [Display] explicitly when one is available. The no-arg
 * overload is a compatibility path for non-composable unit helpers and
 * resolves from the active SWT UI thread display instead of mutable global
 * process state.
 */
fun getDisplayDensity(display: Display? = Display.getCurrent()): Density = display?.getSweetDensity() ?: Density.Default

private const val DISPLAY_DENSITY_KEY = "sweet.display.density"

internal fun Display.getSweetDensity(): Density {
    val existing = getData(DISPLAY_DENSITY_KEY) as? Density
    if (existing != null) return existing

    return Density.fromDisplay(this).also { density ->
        setData(DISPLAY_DENSITY_KEY, density)
    }
}

// Helper function to get weight from a control
internal fun Control.getWeight(): Float? = getSweetCompositionData().layoutData.weight
