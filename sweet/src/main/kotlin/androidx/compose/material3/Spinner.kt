@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createSpinner
import io.github.ddsimoes.sweet.internal.createWidgetCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.widgets.Spinner as SWTSpinner

/**
 * A numeric spinner backed by an SWT Spinner widget.
 *
 * Provides up/down arrows for incrementing/decrementing an integer value
 * within [min]..[max] bounds, stepping by [increment].
 */
@Composable
fun Spinner(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    min: Int = 0,
    max: Int = 100,
    increment: Int = 1,
) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val spinner = createSpinner(applier)
            val scope = spinner.createWidgetCoroutineScope()
            DirectSWTNode(
                spinner.apply {
                    val listener =
                        ModifyListener { e ->
                            val swtSpinner = e.source as SWTSpinner
                            val newValue = swtSpinner.selection

                            @Suppress("UNCHECKED_CAST")
                            val callback = getData("onValueChange") as? ((Int) -> Unit)
                            scope.launch {
                                callback?.invoke(newValue)
                            }
                        }
                    addModifyListener(listener)
                },
            )
        },
        update = {
            set(value) {
                val swtSpinner = this.control as SWTSpinner
                if (!swtSpinner.isDisposed && swtSpinner.selection != it) {
                    swtSpinner.selection = it
                }
            }
            set(onValueChange) { (this.control as SWTSpinner).setData("onValueChange", it) }
            set(modifier) { applySWTModifier(this.control, it) }
            set(enabled) { (this.control as SWTSpinner).enabled = it }
            set(min) {
                val swtSpinner = this.control as SWTSpinner
                if (!swtSpinner.isDisposed) swtSpinner.minimum = it
            }
            set(max) {
                val swtSpinner = this.control as SWTSpinner
                if (!swtSpinner.isDisposed) swtSpinner.maximum = it
            }
            set(increment) {
                val swtSpinner = this.control as SWTSpinner
                if (!swtSpinner.isDisposed) swtSpinner.increment = it
            }
        },
    )
}
