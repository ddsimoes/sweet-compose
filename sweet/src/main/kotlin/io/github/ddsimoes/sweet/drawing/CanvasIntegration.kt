package io.github.ddsimoes.sweet.drawing

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.getSweetDensity
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ControlAdapter
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display

internal const val CANVAS_DRAW_CALLBACK_KEY = "__sweet_canvas_drawCallback"
internal const val CANVAS_DRAW_CALLBACKS_KEY = "sweet.modifier.draw.callbacks"

/** Key set on a Canvas [Control] to signal ownership of the offscreen buffer. */
internal const val CANVAS_OFFSCREEN_KEY = "__sweet_canvas_offscreen"

private const val CANVAS_SNAPSHOT_OBSERVER_KEY = "sweet.canvas.snapshotObserver"

/**
 * Display-scoped [SnapshotStateObserver] for the canvas draw phase.
 *
 * Snapshot state read inside a draw callback is observed per-canvas; when any
 * observed state changes, the canvas is invalidated with `redraw()` so the
 * pixel content stays in sync. This mirrors Compose's draw-phase observation:
 * state used only at draw time never triggers recomposition, so composition
 * alone cannot keep the canvas fresh.
 */
private fun Display.getOrCreateCanvasSnapshotObserver(): SnapshotStateObserver {
    val existing = getData(CANVAS_SNAPSHOT_OBSERVER_KEY) as? SnapshotStateObserver
    if (existing != null) return existing
    val observer =
        SnapshotStateObserver { command ->
            // Apply notifications may arrive from any thread; marshal
            // invalidations to the SWT UI thread.
            if (isDisposed) return@SnapshotStateObserver
            if (Display.getCurrent() === this) {
                command()
            } else {
                try {
                    asyncExec(command)
                } catch (_: org.eclipse.swt.SWTException) {
                    // Display disposed concurrently; nothing to invalidate.
                }
            }
        }
    observer.start()
    setData(CANVAS_SNAPSHOT_OBSERVER_KEY, observer)
    disposeExec {
        observer.stop()
        observer.clear()
        setData(CANVAS_SNAPSHOT_OBSERVER_KEY, null)
    }
    return observer
}

/**
 * Invalidation callback for [SnapshotStateObserver.observeReads]. Kept as a
 * single instance so the observer can merge scopes across paints.
 */
private val invalidateCanvasOnStateChange: (Canvas) -> Unit = { canvas ->
    if (!canvas.isDisposed) {
        canvas.redraw()
    }
}

/**
 * Create an SWT Canvas attached to the current Compose parent.
 *
 * This helper relies on the composition-scoped [SWTNodeApplier] to discover
 * the current container composite, mirroring how other Sweet nodes allocate
 * their underlying SWT controls.
 */
internal fun createSwtCanvas(applier: SWTNodeApplier): Canvas {
    val parent: Composite = applier.requireCurrentParent("Canvas")

    val canvas = Canvas(parent, SWT.NONE)

    installCanvasListeners(canvas)

    return canvas
}

/**
 * Associate the current draw callback with this Canvas.
 */
internal fun Canvas.setDrawCallback(callback: DrawScope.() -> Unit) {
    setData(CANVAS_DRAW_CALLBACK_KEY, callback)
}

@Suppress("UNCHECKED_CAST")
private fun Canvas.getDrawCallback(): (DrawScope.() -> Unit)? =
    getData(CANVAS_DRAW_CALLBACK_KEY) as? (DrawScope.() -> Unit)

private fun installCanvasListeners(canvas: Canvas) {
    // Paint listener invokes the current DrawScope callback
    canvas.addPaintListener(
        object : PaintListener {
            override fun paintControl(e: PaintEvent) {
                // Use clientArea for correct drawable dimensions
                val clientArea = canvas.clientArea
                if (clientArea.width <= 0 || clientArea.height <= 0) return

                val callback = canvas.getDrawCallback() ?: return

                // Cached offscreen buffer: reuse if size matches, reallocate on resize.
                val offscreenImage: Image
                val offscreenGc: GC

                @Suppress("UNCHECKED_CAST")
                val cached = canvas.getData(CANVAS_OFFSCREEN_KEY) as? Pair<Image, GC>
                if (cached != null) {
                    val (cachedImage, cachedGc) = cached
                    val cachedBounds = cachedImage.bounds
                    if (cachedBounds.width == clientArea.width && cachedBounds.height == clientArea.height) {
                        offscreenImage = cachedImage
                        offscreenGc = cachedGc
                    } else {
                        cachedGc.dispose()
                        cachedImage.dispose()
                        offscreenImage = Image(canvas.display, clientArea.width, clientArea.height)
                        offscreenGc = GC(offscreenImage)
                        canvas.setData(CANVAS_OFFSCREEN_KEY, Pair(offscreenImage, offscreenGc))
                    }
                } else {
                    offscreenImage = Image(canvas.display, clientArea.width, clientArea.height)
                    offscreenGc = GC(offscreenImage)
                    canvas.setData(CANVAS_OFFSCREEN_KEY, Pair(offscreenImage, offscreenGc))
                }

                try {
                    // Clear the offscreen buffer before each paint so that
                    // animated / non-opaque content does not accumulate
                    // across frames (composite on top of stale pixels).
                    offscreenGc.background = canvas.background
                    offscreenGc.fillRectangle(0, 0, clientArea.width, clientArea.height)
                    offscreenGc.advanced = true

                    val backend = SwtCanvasBackend(offscreenGc, canvas.display).also { it.offscreenImage = offscreenImage }
                    val size = Size(clientArea.width.toFloat(), clientArea.height.toFloat())
                    val density = canvas.display.getSweetDensity()

                    val scope =
                        SweetDrawScope(
                            canvas = backend,
                            initialSize = size,
                            layoutDirection = LayoutDirection.Ltr,
                            density = density.density,
                            fontScale = density.fontScale,
                        )

                    if (SweetDebugger.assertionEnabled) {
                        SweetDebugger.log(
                            "SWTCanvas",
                            "Invoking Canvas draw callback with size=${size.width}x${size.height}",
                        )
                    }

                    // Execute all draw callbacks under snapshot read observation
                    // so state read only at draw time (not in composition)
                    // still invalidates the canvas when it changes.
                    val observer = canvas.display.getOrCreateCanvasSnapshotObserver()
                    observer.observeReads(canvas, invalidateCanvasOnStateChange) {
                        // Draw behind-content modifiers first so they sit behind
                        // the Canvas onDraw content (matching Compose semantics:
                        // drawBehind is behind content).
                        @Suppress("UNCHECKED_CAST")
                        val drawCallbacks = canvas.getData(CANVAS_DRAW_CALLBACKS_KEY) as? List<DrawScope.() -> Unit>
                        if (!drawCallbacks.isNullOrEmpty()) {
                            for (cb in drawCallbacks) {
                                cb.invoke(scope)
                            }
                        }

                        callback.invoke(scope)
                    }

                    // Blit the offscreen buffer to the real GC.
                    e.gc.drawImage(offscreenImage, 0, 0)
                } finally {
                    // Offscreen buffer is cached — do NOT dispose here.
                }
            }
        },
    )

    // Redraw the canvas when resized so callers don't need to
    // manage explicit invalidation for basic scenarios.
    canvas.addControlListener(
        object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                if (!canvas.isDisposed) {
                    canvas.redraw()
                }
            }
        },
    )

    // Clean up the cached offscreen buffer and snapshot observation
    // when the Canvas is disposed.
    canvas.addDisposeListener {
        @Suppress("UNCHECKED_CAST")
        val cached = canvas.getData(CANVAS_OFFSCREEN_KEY) as? Pair<Image, GC>
        if (cached != null) {
            cached.second.dispose()
            cached.first.dispose()
            canvas.setData(CANVAS_OFFSCREEN_KEY, null)
        }
        val observer = canvas.display.getData(CANVAS_SNAPSHOT_OBSERVER_KEY) as? SnapshotStateObserver
        observer?.clear(canvas)
    }
}
