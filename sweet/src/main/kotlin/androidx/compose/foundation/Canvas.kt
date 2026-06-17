package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.drawing.createSwtCanvas
import io.github.ddsimoes.sweet.drawing.setDrawCallback
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier

/**
 * Canvas composable for custom drawing.
 *
 * Creates an SWT Canvas control that [onDraw] is invoked on. The draw scope
 * is a full [DrawScope] (extends [androidx.compose.ui.unit.Density]) backed
 * by the internal [io.github.ddsimoes.sweet.drawing.SwtCanvasBackend].
 *
 * **Reactive redraw:** snapshot state read inside `onDraw` is observed during
 * the paint pass (display-scoped [androidx.compose.runtime.snapshots.SnapshotStateObserver]);
 * when any observed state changes the canvas is invalidated with `redraw()`.
 * This matches Compose's draw-phase observation — draw-only state never
 * triggers recomposition, so composition alone cannot keep the pixels fresh.
 */
@Composable
fun Canvas(
    modifier: Modifier = Modifier,
    onDraw: DrawScope.() -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = {
            DirectSWTNode(createSwtCanvas(applier))
        },
        update = {
            set(onDraw) { callback ->
                val canvas = this.control as org.eclipse.swt.widgets.Canvas
                canvas.setDrawCallback(callback)
                // Trigger redraw whenever the draw callback or state changes.
                // The global write observer coalesces multiple invalidations
                // into a single frame via SWTMonotonicFrameClock.
                if (!canvas.isDisposed) {
                    canvas.redraw()
                }
            }
            set(modifier) { applySWTModifier(this.control, it) }
        },
    )
}
