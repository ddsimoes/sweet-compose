@file:Suppress("UnusedParameter")

package androidx.compose.ui.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection

/** Default alpha value used when [Painter.draw] is called without explicit alpha. */
const val DefaultAlpha: Float = 1.0f

/**
 * Painter abstraction for drawing content inside a [DrawScope].
 *
 * Subclasses override [onDraw] to provide rendering logic and
 * [intrinsicSize] to declare natural dimensions. Callers invoke
 * the final [draw] method, which applies alpha / color-filter /
 * layout-direction configuration before delegating to [onDraw].
 */
abstract class Painter {
    /**
     * Intrinsic (natural) size of the content, in pixels.
     * Return [Size.Unspecified] when the painter has no
     * preferred dimensions (e.g. a solid colour or gradient).
     */
    abstract val intrinsicSize: Size

    /**
     * Configure alpha for the next draw pass.
     * @return `true` when the painter consumed the value and
     *         the caller does not need to apply it separately.
     */
    open fun applyAlpha(alpha: Float): Boolean = true

    /**
     * Configure a [ColorFilter] for the next draw pass.
     * @return `true` when the painter consumed the value.
     */
    open fun applyColorFilter(colorFilter: ColorFilter?): Boolean = true

    /**
     * Configure [LayoutDirection] for the next draw pass.
     * @return `true` when the painter consumed the value.
     */
    open fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean = true

    /**
     * Draw this painter's content into the provided [DrawScope].
     * Subclasses override this; callers use [draw].
     */
    open fun DrawScope.onDraw() {}

    /**
     * Final entry point for drawing this painter.
     *
     * Applies [alpha], [colorFilter], and the [DrawScope]'s
     * [layoutDirection][DrawScope.layoutDirection] before
     * calling [onDraw]. Subclasses override [onDraw] instead.
     */
    fun DrawScope.draw(
        size: Size,
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
    ) {
        applyAlpha(alpha)
        applyColorFilter(colorFilter)
        applyLayoutDirection(layoutDirection)
        onDraw()
    }
}

// ── Concrete painters ──────────────────────────────────────────

/**
 * [Painter] that draws a single [Color].
 */
class ColorPainter(
    val color: Color,
) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(color = color, size = size)
    }
}

/**
 * [Painter] that draws with a [Brush] (solid colour, gradient, etc.).
 */
class BrushPainter(
    val brush: Brush,
) : Painter() {
    override val intrinsicSize: Size = brush.intrinsicSize

    override fun DrawScope.onDraw() {
        drawRect(brush = brush, size = size)
    }
}

/**
 * [Painter] that draws an [ImageBitmap].
 */
class BitmapPainter(
    private val image: ImageBitmap,
) : Painter() {
    override val intrinsicSize: Size
        get() = Size(image.width.toFloat(), image.height.toFloat())

    override fun DrawScope.onDraw() {
        drawImage(image)
    }
}

// ── Modifier.paint extension ───────────────────────────────────

/** Marker [Modifier.Element] that carries a [Painter] into the draw phase. */
private class PaintModifier(
    val painter: Painter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
) : Modifier.Element

/**
 * Paint [painter] onto this modifier's content area.
 *
 * Stub — registers a marker modifier; actual drawing will be
 * performed via `drawBehind` when the draw-phase integration lands.
 */
@Stable
fun Modifier.paint(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
): Modifier =
    this.then(
        PaintModifier(painter, sizeToIntrinsics, alignment, contentScale, alpha, colorFilter),
    )
