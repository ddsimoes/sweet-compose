@file:Suppress("MatchingDeclarationName")

package io.github.ddsimoes.sweet.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.layout.SweetLayout
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite

/**
 * Node representing a scrollable region backed by Sweet's own [ScrollViewport].
 *
 * The viewport itself is the `control`, and its content is hosted in [contentComposite],
 * which always uses [SweetLayout] and forms its own shadow-tree pass root. Vertical /
 * horizontal scrolling is configured via the [vertical] / [horizontal] flags and
 * coordinated with [ScrollState] and [LazyListState] when provided.
 */
internal class SWTScrollableNode(
    parentApplier: SWTNodeApplier,
    private val vertical: Boolean = true,
    private val horizontal: Boolean = false,
    internal val scrollState: ScrollState? = null,
    internal val lazyListState: LazyListState? = null,
    internal val reverseScrolling: Boolean = false,
) : SweetNode {
    override var layoutNode: SweetLayoutNode? = null
    override val control: ScrollViewport
    val contentComposite: Composite

    init {
        val parent = parentApplier.requireCurrentParent("SWTScrollableNode")

        val scrollStyle =
            when {
                vertical && horizontal -> SWT.V_SCROLL or SWT.H_SCROLL
                vertical -> SWT.V_SCROLL
                horizontal -> SWT.H_SCROLL
                else -> SWT.NONE
            }

        control =
            ScrollViewport(parent, scrollStyle).apply {
                // Use SweetLayoutData on this control to fill available space in parent.
                // Only fill the cross axis for vertical scrollers: a horizontal-only scroller
                // (LazyRow, Row+horizontalScroll) must hug its content height, otherwise it
                // stretches to the parent's max height and pushes siblings out of view.
                updateSweetCompositionData {
                    withLayoutData { copy(fillMaxWidth = true, fillMaxHeight = vertical) }
                }

                if (SweetDebugger.assertionEnabled) {
                    SweetDebugger.log(
                        "SWTScrollableNode",
                        "Created scroll viewport [${hashCode().toString(16)}] with scroll: v=$vertical, h=$horizontal",
                    )
                }
            }

        contentComposite =
            Composite(control, SWT.NONE).apply {
                // SweetLayout so the content subtree forms its own shadow pass root and can
                // report preferred size beyond the viewport.
                layout = SweetLayout()
            }
        control.content = contentComposite

        // 1. ScrollState wiring: keep value/maxValue/viewportSize in sync after every
        //    scroll or viewport layout.
        val state = scrollState
        if (state != null) {
            control.onScrollChanged = { x, y, maxX, maxY, viewportW, viewportH ->
                if (vertical) {
                    state.updateFromScrollable(mapToState(y, maxY), maxY, viewportH)
                } else if (horizontal) {
                    state.updateFromScrollable(mapToState(x, maxX), maxX, viewportW)
                }
            }
            control.onScrollDragChanged = { inProgress ->
                state.isScrollInProgress = inProgress
            }
        }

        // 2. LazyListState wiring: give it references to viewport + content composite,
        //    and chain onto onScrollChanged so it recomputes visible items.
        val listState = lazyListState
        if (listState != null) {
            listState.contentComposite = contentComposite
            listState.viewport = control
            listState.orientation = if (vertical) Orientation.Vertical else Orientation.Horizontal

            val prevOnScrollChanged = control.onScrollChanged
            control.onScrollChanged = { x, y, maxX, maxY, viewportW, viewportH ->
                prevOnScrollChanged?.invoke(x, y, maxX, maxY, viewportW, viewportH)
                listState.recomputeVisibleItems()
            }
        }
    }

    /** Maps a viewport offset to a state value, honoring [reverseScrolling]. */
    private fun mapToState(
        offset: Int,
        max: Int,
    ): Int = if (reverseScrolling) (max - offset).coerceAtLeast(0) else offset

    override fun dispose() {
        if (!contentComposite.isDisposed) {
            contentComposite.dispose()
        }
        if (!control.isDisposed) {
            control.dispose()
        }
    }
}
