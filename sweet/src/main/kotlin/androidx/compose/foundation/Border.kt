package androidx.compose.foundation

import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toPx

// ── Border via drawBehind ───────────────────────────────────────────────────

/**
 * Draw a border with [width], [Brush], and [Shape] via [drawBehind] using
 * a stroke style.
 */
@Stable
fun Modifier.border(
    width: Float,
    brush: Brush,
    shape: Shape,
): Modifier =
    this.drawBehind {
        drawBorderShape(brush, shape, width)
    }

/**
 * Draw a border with [width], solid [Color], and [Shape].
 *
 * Delegates to [border] with [SolidColor].
 */
@Stable
fun Modifier.border(
    width: Float,
    color: Color,
    shape: Shape,
): Modifier = this.border(width, brush = SolidColor(color), shape)

/**
 * Draw a border with [width] specified in [Dp], [Brush], and [Shape].
 *
 * Converts [width] to pixels using the current display density at draw time.
 */
@Stable
fun Modifier.border(
    width: Dp,
    brush: Brush,
    shape: Shape,
): Modifier =
    this.drawBehind {
        drawBorderShape(brush, shape, width.toPx())
    }

/**
 * Draw a border with [width] specified in [Dp], solid [Color], and [Shape].
 */
@Stable
fun Modifier.border(
    width: Dp,
    color: Color,
    shape: Shape,
): Modifier = this.border(width, brush = SolidColor(color), shape)

/**
 * Stroke [shape]'s outline so the border lies fully inside the bounds,
 * matching Compose semantics: the stroke is centered on a path inset by
 * half the stroke width (corner radii shrink accordingly), so no part of
 * the border is clipped away at the control edges.
 */
private fun DrawScope.drawBorderShape(
    brush: Brush,
    shape: Shape?,
    widthPx: Float,
) {
    val stroke = Stroke(widthPx)
    val half = widthPx / 2f
    val insetTopLeft = Offset(half, half)
    val insetSize =
        Size(
            (size.width - widthPx).coerceAtLeast(0f),
            (size.height - widthPx).coerceAtLeast(0f),
        )
    if (shape == null || shape == RectangleShape) {
        drawRect(brush = brush, topLeft = insetTopLeft, size = insetSize, style = stroke)
        return
    }
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> {
            drawRect(brush = brush, topLeft = insetTopLeft, size = insetSize, style = stroke)
        }
        is Outline.Rounded -> {
            val rr = outline.roundRect
            val radius = (rr.topLeftCornerRadius - half).coerceAtLeast(0f)
            if (brush is SolidColor) {
                drawRoundRect(
                    color = brush.value,
                    topLeft = insetTopLeft,
                    size = insetSize,
                    cornerRadius = CornerRadius(radius),
                    style = stroke,
                )
            } else {
                val insetRr =
                    RoundRect(
                        Rect(half, half, half + insetSize.width, half + insetSize.height),
                        CornerRadius(radius),
                    )
                drawPath(Path().apply { addRoundRect(insetRr) }, brush = brush, style = stroke)
            }
        }
        is Outline.Generic -> {
            // Best effort for arbitrary paths: stroke the outline as-is
            // (half the stroke may fall outside the bounds).
            drawPath(outline.path, brush = brush, style = stroke)
        }
    }
}
