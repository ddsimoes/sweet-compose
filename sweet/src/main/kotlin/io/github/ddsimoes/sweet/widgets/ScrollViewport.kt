package io.github.ddsimoes.sweet.widgets

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Layout

/**
 * A Sweet-owned scroll viewport: a plain [Composite] whose native [org.eclipse.swt.widgets.ScrollBar]s
 * are driven directly by Sweet, replacing `org.eclipse.swt.custom.ScrolledComposite`.
 *
 * Responsibilities:
 * - Hosts a single [content] control, measured with the scroll axis **unbounded**
 *   (Compose scroll semantics) and the cross axis bounded by the viewport.
 * - Scrolling is placement-only: the content is offset by `(-scrollX, -scrollY)` with a
 *   single `setLocation` call — no layout pass runs on scroll.
 * - Keeps the native scrollbars in sync after every layout:
 *   `selection = scroll offset`, `maximum = content extent`, `thumb = viewport extent`
 *   (the same convention `ScrolledComposite` used, so `range = maximum - thumb`).
 *   Bars are hidden when the content fits.
 * - Handles mouse-wheel events explicitly (plain composites do not auto-scroll).
 *
 * Style bits: pass [SWT.V_SCROLL] and/or [SWT.H_SCROLL] at construction (immutable per SWT).
 */
class ScrollViewport(
    parent: Composite,
    style: Int,
) : Composite(parent, style) {
    /** The single scrolled content control. Managed by Sweet; set once after construction. */
    var content: Control? = null
        set(value) {
            field = value
            if (value != null && !isDisposed) layout(true)
        }

    /** Current scroll offset on each axis, in pixels (>= 0). */
    var scrollX: Int = 0
        private set
    var scrollY: Int = 0
        private set

    /**
     * Pixels scrolled per mouse-wheel line / arrow press. Defaults to the widget font's
     * line height so scroll granularity tracks font size and DPI (large-print /
     * accessibility fonts scroll proportionally); falls back to [DEFAULT_LINE_INCREMENT]
     * when font metrics are unavailable.
     */
    var lineIncrement: Int = defaultLineIncrement()

    private fun defaultLineIncrement(): Int =
        runCatching {
            val gc = org.eclipse.swt.graphics.GC(this)
            try {
                gc.fontMetrics.height.coerceAtLeast(1)
            } finally {
                gc.dispose()
            }
        }.getOrDefault(DEFAULT_LINE_INCREMENT)

    /**
     * Current scroll origin as a [Point]; mirrors `ScrolledComposite.getOrigin` so callers
     * (and tests) can port 1:1.
     */
    val origin: Point get() = Point(scrollX, scrollY)

    /** Last laid-out content extent (full content size, before clipping). */
    var contentExtent: Point = Point(0, 0)
        private set

    /**
     * Re-entrancy marker: true while [ViewportLayout.layout] runs. Sweet's layout engine
     * uses it to avoid escalating content invalidations back into a running viewport pass.
     */
    var isLayingOut: Boolean = false
        private set

    /**
     * Invoked after the scroll offset changes (user scroll, wheel, programmatic) or
     * after a layout updated the scroll range. Arguments: (scrollX, scrollY, maxX, maxY,
     * viewportWidth, viewportHeight). Internal wiring point for `ScrollState` sync.
     */
    var onScrollChanged: ((x: Int, y: Int, maxX: Int, maxY: Int, viewportW: Int, viewportH: Int) -> Unit)? = null

    /**
     * Best-effort drag tracking: invoked with `true` while a scrollbar thumb drag is in
     * progress (`SWT.DRAG` detail), `false` on the next non-drag selection event.
     */
    var onScrollDragChanged: ((inProgress: Boolean) -> Unit)? = null

    init {
        super.setLayout(ViewportLayout())
        verticalBar?.let { bar ->
            bar.isVisible = false
            bar.addListener(SWT.Selection) { event ->
                onScrollDragChanged?.invoke(event.detail == SWT.DRAG)
                scrollTo(scrollX, bar.selection)
            }
        }
        horizontalBar?.let { bar ->
            bar.isVisible = false
            bar.addListener(SWT.Selection) { event ->
                onScrollDragChanged?.invoke(event.detail == SWT.DRAG)
                scrollTo(bar.selection, scrollY)
            }
        }
        // Plain composites do not scroll on mouse wheel — handle it explicitly.
        // (Verified on GTK: wheel events over the composite do not move the native bar,
        // so there is no double-handling risk.)
        addListener(SWT.MouseVerticalWheel) { event ->
            if (verticalBar != null && event.count != 0) {
                scrollTo(scrollX, scrollY - event.count * lineIncrement)
                event.doit = false
            }
        }
        addListener(SWT.MouseHorizontalWheel) { event ->
            if (horizontalBar != null && event.count != 0) {
                scrollTo(scrollX - event.count * lineIncrement, scrollY)
                event.doit = false
            }
        }
    }

    /** The viewport owns its layout; external layouts are ignored (as ScrolledComposite did). */
    override fun setLayout(layout: Layout?) {
        // no-op: ViewportLayout installed in init is permanent
    }

    /** Maximum scroll offset on each axis (content extent minus viewport extent, >= 0). */
    val maxScrollX: Int get() = (contentExtent.x - clientArea.width).coerceAtLeast(0)
    val maxScrollY: Int get() = (contentExtent.y - clientArea.height).coerceAtLeast(0)

    /** Mirrors `ScrolledComposite.setOrigin`: scrolls to the given offset (clamped). */
    fun setOrigin(
        x: Int,
        y: Int,
    ) = scrollTo(x, y)

    fun setOrigin(origin: Point) = scrollTo(origin.x, origin.y)

    /**
     * Scrolls to the given offset, clamped to the valid range. Placement-only: moves the
     * content with a single `setLocation`, syncs the bars, and fires [onScrollChanged].
     */
    fun scrollTo(
        x: Int,
        y: Int,
    ) {
        if (isDisposed) return
        val nx = x.coerceIn(0, maxScrollX)
        val ny = y.coerceIn(0, maxScrollY)
        if (nx == scrollX && ny == scrollY) return
        scrollX = nx
        scrollY = ny
        content?.takeIf { !it.isDisposed }?.setLocation(-nx, -ny)
        syncBars()
        fireScrollChanged()
    }

    private fun syncBars() {
        val client = clientArea
        verticalBar?.let { bar ->
            val needed = contentExtent.y > client.height
            bar.setValues(scrollY, 0, contentExtent.y.coerceAtLeast(1), client.height.coerceAtLeast(1), lineIncrement, client.height.coerceAtLeast(1))
            if (bar.isVisible != needed) bar.isVisible = needed
            bar.isEnabled = needed
        }
        horizontalBar?.let { bar ->
            val needed = contentExtent.x > client.width
            bar.setValues(scrollX, 0, contentExtent.x.coerceAtLeast(1), client.width.coerceAtLeast(1), lineIncrement, client.width.coerceAtLeast(1))
            if (bar.isVisible != needed) bar.isVisible = needed
            bar.isEnabled = needed
        }
    }

    private fun fireScrollChanged() {
        val client = clientArea
        onScrollChanged?.invoke(scrollX, scrollY, maxScrollX, maxScrollY, client.width, client.height)
    }

    /**
     * Layout that measures [content] with Compose scroll semantics and keeps scroll state
     * (offset clamping, bar values/visibility) consistent with the new geometry.
     */
    private inner class ViewportLayout : Layout() {
        override fun computeSize(
            composite: Composite,
            wHint: Int,
            hHint: Int,
            flushCache: Boolean,
        ): Point {
            val c = content ?: return Point(wHint.coerceAtLeast(0), hHint.coerceAtLeast(0))
            val vertical = verticalBar != null
            val horizontal = horizontalBar != null
            // Scroll axis unbounded; cross axis follows the hint.
            val contentSize =
                c.computeSize(
                    if (horizontal) SWT.DEFAULT else wHint,
                    if (vertical) SWT.DEFAULT else hHint,
                )
            val trim = computeTrim(0, 0, contentSize.x, contentSize.y)
            return Point(
                if (wHint != SWT.DEFAULT) wHint else trim.width,
                if (hHint != SWT.DEFAULT) hHint else trim.height,
            )
        }

        override fun layout(
            composite: Composite,
            flushCache: Boolean,
        ) {
            val c = content ?: return
            if (c.isDisposed) return
            if (isLayingOut) return
            isLayingOut = true
            try {
                val client = clientArea
                if (client.width <= 0 || client.height <= 0) return
                val vertical = verticalBar != null
                val horizontal = horizontalBar != null
                // Measure content: scroll axis unbounded, cross axis fixed to the viewport.
                val preferred =
                    c.computeSize(
                        if (horizontal) SWT.DEFAULT else client.width,
                        if (vertical) SWT.DEFAULT else client.height,
                    )
                // Content fills the viewport when smaller (ScrolledComposite expand behavior).
                val contentW = if (horizontal) maxOf(preferred.x, client.width) else client.width
                val contentH = if (vertical) maxOf(preferred.y, client.height) else client.height
                contentExtent = Point(contentW, contentH)
                // Clamp the scroll offset to the new range (content may have shrunk).
                scrollX = scrollX.coerceIn(0, maxScrollX)
                scrollY = scrollY.coerceIn(0, maxScrollY)
                val oldBounds = c.bounds
                val boundsChanged =
                    oldBounds.x != -scrollX || oldBounds.y != -scrollY ||
                        oldBounds.width != contentW || oldBounds.height != contentH
                if (boundsChanged) {
                    // Triggers the content's own layout via SWT's resize protocol.
                    c.setBounds(-scrollX, -scrollY, contentW, contentH)
                }
                if (c is Composite && (!boundsChanged || (oldBounds.width == contentW && oldBounds.height == contentH))) {
                    // Size unchanged: SWT will not re-lay the content; do it explicitly so
                    // structural changes inside the content are placed.
                    c.layout(true)
                }
                syncBars()
                fireScrollChanged()
            } finally {
                isLayingOut = false
            }
        }
    }

    private companion object {
        const val DEFAULT_LINE_INCREMENT = 16
    }
}
