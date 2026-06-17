@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applyBackground
import io.github.ddsimoes.sweet.internal.applyForeground
import io.github.ddsimoes.sweet.internal.applySWTModifier
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.ProgressBar

/**
 * Creates an SWT ProgressBar parented via [applier].
 *
 * @param applier the composition-scoped applier providing the parent composite
 * @param style the SWT style bits (e.g. [SWT.HORIZONTAL], [SWT.INDETERMINATE])
 */
internal fun createProgressBar(
    applier: SWTNodeApplier,
    style: Int,
): ProgressBar {
    val parent = applier.requireCurrentParent("ProgressBar")
    return ProgressBar(parent, style)
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Determinate Material Design linear progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * **Sweet note:** Backed by an SWT [ProgressBar] with [SWT.HORIZONTAL] style. Progress is
 * mapped from the 0..1 range to the SWT selection range 0..100.
 *
 * @param progress the progress of this indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of the progress bar fill
 * @param trackColor color of the track behind the indicator
 */
@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6750A4),
    trackColor: Color = Color(0xFFE7E0EC),
) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val bar = createProgressBar(applier, SWT.HORIZONTAL)
            DirectSWTNode(
                bar.apply {
                    minimum = 0
                    maximum = 100
                },
            )
        },
        update = {
            set(progress) {
                val bar = this.control as ProgressBar
                if (!bar.isDisposed) {
                    val coerced = it.coerceIn(0f, 1f)
                    bar.selection = (coerced * 100).toInt()
                }
            }
            set(modifier) { if (!this.control.isDisposed) applySWTModifier(this.control, it) }
            set(color) {
                val bar = this.control as ProgressBar
                bar.applyForeground(it)
            }
            set(trackColor) {
                val bar = this.control as ProgressBar
                bar.applyBackground(it)
            }
        },
    )
}

/**
 * <a href="https://m3.material.io/components/progress-indicators/overview" class="external"
 * target="_blank">Indeterminate Material Design circular progress indicator</a>.
 *
 * Progress indicators express an unspecified wait time or display the duration of a process.
 *
 * **Sweet note:** Currently rendered as an indeterminate horizontal SWT [ProgressBar]
 * ([SWT.HORIZONTAL] | [SWT.INDETERMINATE]) — this is a shim. A true circular appearance
 * is not yet implemented.
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of the progress indicator
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6750A4),
) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            val bar = createProgressBar(applier, SWT.HORIZONTAL or SWT.INDETERMINATE)
            DirectSWTNode(bar)
        },
        update = {
            set(modifier) { if (!this.control.isDisposed) applySWTModifier(this.control, it) }
            set(color) {
                val bar = this.control as ProgressBar
                bar.applyForeground(it)
            }
        },
    )
}
