package androidx.compose.foundation.lazy

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyListState = remember { LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset) }

/**
 * A state object that can be hoisted to control and observe scrolling of a [LazyColumn] or
 * [LazyRow].
 *
 * Sweet's implementation keeps eager composition but makes the state real:
 * - [firstVisibleItemIndex] and [firstVisibleItemScrollOffset] are derived from scroll offset
 *   vs. the item controls' bounds (authoritative from the shadow tree after layout).
 * - [layoutInfo] provides [visibleItemsInfo] computed after each scroll/layout.
 * - [scrollToItem] maps an item index to a pixel offset and delegates to the internal
 *   [ScrollState.scrollTo].
 *
 * In most cases, this will be created via [rememberLazyListState].
 *
 * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 */
@Stable
class LazyListState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
) {
    /** Internal [ScrollState] used for scroll mechanics and wired to the viewport. */
    internal val scrollState: ScrollState = ScrollState(0)

    /** Set by [LazyColumn]/[LazyRow] after the content [Composite] is created. */
    internal var contentComposite: Composite? = null

    /** Set by [LazyColumn]/[LazyRow] after the viewport is created. */
    internal var viewport: ScrollViewport? = null

    /** Orientation: set by [LazyColumn] (Vertical) or [LazyRow] (Horizontal). */
    internal var orientation: Orientation = Orientation.Vertical

    // -- Derived snapshot state (populated by recomputeVisibleItems) --

    private var _firstVisibleItemIndex by mutableStateOf(firstVisibleItemIndex)
    private var _firstVisibleItemScrollOffset by mutableStateOf(firstVisibleItemScrollOffset)
    private var _layoutInfo by mutableStateOf<LazyListLayoutInfo>(emptyLayoutInfo())

    /**
     * The index of the first item that is visible within the scrollable viewport area not including
     * items in the content padding region.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     */
    val firstVisibleItemIndex: Int
        get() = _firstVisibleItemIndex

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the amount
     * that the item is offset backwards.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     */
    val firstVisibleItemScrollOffset: Int
        get() = _firstVisibleItemScrollOffset

    /**
     * The object of [LazyListLayoutInfo] calculated during the last scroll/layout pass. For example,
     * you can use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure. Avoid
     * using it directly in composition to prevent performance issues.
     */
    val layoutInfo: LazyListLayoutInfo
        get() = _layoutInfo

    /**
     * Instantly scroll to the item at [index] with the given [scrollOffset] (pixels into the item).
     *
     * The scroll position is computed from the item control's current bounds after layout. If the
     * item has not been laid out yet (bounds = 0), this call is a no-op.
     *
     * @param index the item index
     * @param scrollOffset the scroll offset into the item, in pixels
     */
    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0) {
        val content = contentComposite ?: return
        val children = content.children ?: return
        if (index >= children.size) return
        val item = children[index] ?: return
        val targetOffset = when (orientation) {
            Orientation.Vertical -> item.bounds.y + scrollOffset
            Orientation.Horizontal -> item.bounds.x + scrollOffset
        }
        scrollState.scrollTo(targetOffset)
    }

    /**
     * Recomputes [firstVisibleItemIndex], [firstVisibleItemScrollOffset], and [layoutInfo]
     * from the current scroll offset and child control bounds. Called from onScrollChanged.
     */
    internal fun recomputeVisibleItems() {
        val content = contentComposite ?: return
        val vp = viewport ?: return
        val children = getItemControls(content)
        val scrollOffset = when (orientation) {
            Orientation.Vertical -> vp.scrollY
            Orientation.Horizontal -> vp.scrollX
        }
        val viewportExtent = when (orientation) {
            Orientation.Vertical -> vp.clientArea.height
            Orientation.Horizontal -> vp.clientArea.width
        }

        val visibleItems = mutableListOf<LazyListItemInfo>()
        var firstIndex = -1
        var firstOffset = 0

        for ((index, control) in children.withIndex()) {
            val itemOffset = when (orientation) {
                Orientation.Vertical -> control.bounds.y
                Orientation.Horizontal -> control.bounds.x
            }
            val itemSize = when (orientation) {
                Orientation.Vertical -> control.bounds.height
                Orientation.Horizontal -> control.bounds.width
            }
            if (itemSize <= 0) continue

            // Item intersects the visible viewport?
            val itemEnd = itemOffset + itemSize
            val viewportEnd = scrollOffset + viewportExtent
            if (itemEnd > scrollOffset && itemOffset < viewportEnd) {
                if (firstIndex < 0) {
                    firstIndex = index
                    firstOffset = itemOffset
                }
                visibleItems.add(
                    object : LazyListItemInfo {
                        override val index: Int = index
                        override val key: Any = index // keys not tracked in eager mode
                        override val offset: Int = itemOffset
                        override val size: Int = itemSize
                    }
                )
            } else if (visibleItems.isNotEmpty()) {
                // Past the visible window — children are laid out in order
                break
            }
        }

        _firstVisibleItemIndex = firstIndex.coerceAtLeast(0)
        _firstVisibleItemScrollOffset = if (firstIndex >= 0) {
            (scrollOffset - firstOffset).coerceAtLeast(0)
        } else {
            0
        }
        _layoutInfo = object : LazyListLayoutInfo {
            override val visibleItemsInfo: List<LazyListItemInfo> = visibleItems
            override val viewportStartOffset: Int = scrollOffset
            override val viewportEndOffset: Int = scrollOffset + viewportExtent
            override val totalItemsCount: Int = children.size
            override val viewportSize: IntSize =
                IntSize(vp.clientArea.width, vp.clientArea.height)
            override val orientation: Orientation = this@LazyListState.orientation
        }
    }

    /**
     * Walks the content composite's children, filtering out non-item controls (e.g. the
     * Column/Row layout composite). Returns the ordered list of item controls.
     */
    private fun getItemControls(content: Composite): List<Control> {
        val children = content.children ?: return emptyList()
        return children.filter { child -> child is Composite && !child.isDisposed }
    }

    private companion object {
        fun emptyLayoutInfo(): LazyListLayoutInfo = object : LazyListLayoutInfo {
            override val visibleItemsInfo: List<LazyListItemInfo> = emptyList()
            override val viewportStartOffset: Int = 0
            override val viewportEndOffset: Int = 0
            override val totalItemsCount: Int = 0
        }
    }
}
