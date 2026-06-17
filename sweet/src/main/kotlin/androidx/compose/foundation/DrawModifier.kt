package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.CacheDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.ddsimoes.sweet.internal.DrawModifierElement
import org.eclipse.swt.widgets.Control

// ── Draw modifier elements ──────────────────────────────────────────────────

/**
 * Installs a [drawCallback] via [DrawModifierElement] so the reconciler wires
 * a PaintListener on the owning [Composite].
 */
internal class DrawBehindModifier(
    private val onDraw: DrawScope.() -> Unit,
) : DrawModifierElement {
    override val drawCallback: DrawScope.() -> Unit get() = onDraw

    override fun apply(control: Control) {
        // Handled by [reconcileDraw]; no legacy single-shot path needed.
    }
}

/**
 * Draw-with-content modifier that wraps the user callback in a [ContentDrawScope].
 *
 * In the bounded implementation [ContentDrawScope.drawContent] is a no-op,
 * so the entire callback draws behind children. The overlay Canvas sibling for
 * in-front drawing is deferred (Tier-3).
 */
internal class DrawWithContentModifier(
    private val onDraw: ContentDrawScope.() -> Unit,
) : DrawModifierElement {
    override val drawCallback: DrawScope.() -> Unit = {
        val contentScope =
            object : ContentDrawScope, DrawScope by this {
                override fun drawContent() {
                    // No-op for bounded implementation: SWT paints children natively.
                }
            }
        contentScope.onDraw()
    }

    override fun apply(control: Control) {
        // Handled by [reconcileDraw].
    }
}

// ── Draw modifier extension functions ───────────────────────────────────────

/**
 * Install a draw callback that paints behind the composable's content (children).
 *
 * On [Composite] containers this adds a SWT PaintListener. On leaf widgets
 * the callback is silently ignored (Tier-3 policy).
 */
@Stable
fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit): Modifier {
    return this.then(DrawBehindModifier(onDraw))
}

/**
 * Draw with caching scope. Currently a stub that aliases [drawBehind].
 *
 * The [onBuildDrawCache] lambda receives a [CacheDrawScope] where cached
 * draw operations can be registered via [CacheDrawScope.onDrawBehind].
 * For the bounded implementation, the returned callback is forwarded
 * directly to [drawBehind].
 */
@Stable
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> CacheDrawScope): Modifier {
    val scope = CacheDrawScope()
    onBuildDrawCache(scope)
    return this.drawBehind(scope.onDrawBehind ?: {})
}

/**
 * Draw with access to [ContentDrawScope], which conceptually splits the draw
 * pass into behind and in-front phases via [ContentDrawScope.drawContent].
 *
 * Bounded implementation: [ContentDrawScope.drawContent] is a no-op and the
 * entire callback draws behind children via a SWT PaintListener. In-front
 * drawing via overlay Canvas sibling is deferred (Tier-3).
 */
@Stable
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit): Modifier {
    return this.then(DrawWithContentModifier(onDraw))
}
