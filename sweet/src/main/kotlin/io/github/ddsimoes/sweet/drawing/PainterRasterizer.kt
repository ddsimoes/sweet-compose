package io.github.ddsimoes.sweet.drawing

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.unit.LayoutDirection
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display

/**
 * Rasterize a [Painter] into an SWT [Image] by drawing it onto an offscreen buffer through
 * the SWT draw backend. This bridges Compose [Painter]s (e.g. a window icon supplied as a
 * [androidx.compose.ui.graphics.BitmapPainter]) to native SWT handles that require an [Image].
 *
 * Returns `null` for painters without a concrete intrinsic size (e.g. solid colours / gradients),
 * since there is nothing meaningful to rasterize. The caller owns the returned [Image] and must
 * dispose it (typically via a `DisposeListener` on the widget that consumes it).
 */
internal fun Painter.rasterizeToImage(display: Display): Image? {
    val intrinsic = intrinsicSize
    if (intrinsic.width.isNaN() || intrinsic.height.isNaN() ||
        intrinsic.width <= 0f || intrinsic.height <= 0f
    ) {
        return null
    }
    val width = intrinsic.width.toInt().coerceAtLeast(1)
    val height = intrinsic.height.toInt().coerceAtLeast(1)

    val image = Image(display, width, height)
    val gc = GC(image)
    try {
        val backend = SwtCanvasBackend(gc, display)
        val scope = SweetDrawScope(
            canvas = backend,
            initialSize = Size(width.toFloat(), height.toFloat()),
            layoutDirection = LayoutDirection.Ltr,
            density = 1f,
            fontScale = 1f,
        )
        with(this) {
            scope.draw(Size(width.toFloat(), height.toFloat()), DefaultAlpha, null)
        }
    } finally {
        gc.dispose()
    }
    return image
}
