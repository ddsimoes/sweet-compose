@file:Suppress("MatchingDeclarationName")

package androidx.compose.foundation

import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Outline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import io.github.ddsimoes.sweet.internal.BackgroundColorElement
import io.github.ddsimoes.sweet.internal.BackgroundColorModifier
import io.github.ddsimoes.sweet.internal.applyBackground
import org.eclipse.swt.widgets.Control

// ── Background modifier element ─────────────────────────────────────────────

/**
 * Background modifier that applies background color and (in the future) shape.
 *
 * For now, Sweet applies only the [color] using an internal, cached SWT
 * color; [shape] is accepted for API compatibility but does not affect
 * rendering yet.
 */
internal class BackgroundModifier(
    private val color: Color,
    private val shape: Shape? = null,
) : BackgroundColorElement {
    init {
        // Shape-specific behavior is not yet implemented; log in debug builds when a non-rectangular
        // shape is requested so callers can spot this.
        if (shape != null && shape != RectangleShape &&
            io.github.ddsimoes.sweet.debug.SweetDebugger.assertionEnabled
        ) {
            io.github.ddsimoes.sweet.debug.SweetDebugger.log(
                "BackgroundModifier",
                "Shape parameter is currently ignored; only background color is applied.",
            )
        }
    }

    override val backgroundColor: Color get() = color

    override fun apply(control: Control) {
        // Fallback single-shot application; the reconciling path uses [backgroundColor] instead.
        control.applyBackground(color)
    }
}

// ── Background extension functions ──────────────────────────────────────────

/**
 * Draw a background with a [Brush] and optional [Shape] via [drawBehind].
 *
 * Only works on Composite containers (Box/Row/Column); no-op on leaf widgets.
 */
@Stable
fun Modifier.background(
    brush: Brush,
    shape: Shape? = null,
): Modifier =
    this.drawBehind {
        drawShape(brush, shape, Fill)
    }

/**
 * Draw a background with a solid [Color] and optional [Shape].
 *
 * For simple color backgrounds without a shape, this uses the direct
 * SWT `Control.setBackground()` path for compatibility with all widgets
 * (including leaf controls). When [shape] is non-null, delegates to
 * the [drawBehind]-based [background(brush, shape)].
 */
@Stable
fun Modifier.background(
    color: Color,
    shape: Shape? = null,
): Modifier =
    if (shape == null || shape == RectangleShape) {
        this.then(BackgroundColorModifier(color))
    } else {
        this.background(brush = SolidColor(color), shape = shape)
    }

// ── Shared drawShape helper ─────────────────────────────────────────────────

/**
 * Draw [shape] (or full-size rectangle when null/rectangle) with the given
 * [brush] and [style] (Fill for backgrounds, Stroke for borders).
 *
 * [drawRoundRect] lacks a brush overload, so for gradient brushes on rounded
 * shapes this converts to a path and draws via [DrawScope.drawPath].
 */
internal fun DrawScope.drawShape(
    brush: Brush,
    shape: Shape?,
    style: DrawStyle,
) {
    if (shape == null || shape == RectangleShape) {
        drawRect(brush = brush, size = size, style = style)
        return
    }
    val outline = shape.createOutline(size, layoutDirection, this)
    when (outline) {
        is Outline.Rectangle -> {
            val r = outline.rect
            drawRect(
                brush = brush,
                topLeft = r.topLeft,
                size = r.size,
                style = style,
            )
        }
        is Outline.Rounded -> {
            val rr = outline.roundRect
            if (brush is SolidColor) {
                drawRoundRect(
                    color = brush.value,
                    topLeft = rr.rect.topLeft,
                    size = rr.rect.size,
                    cornerRadius = CornerRadius(rr.topLeftCornerRadius),
                    style = style,
                )
            } else {
                val path = Path().apply { addRoundRect(rr) }
                drawPath(path, brush = brush, style = style)
            }
        }
        is Outline.Generic -> {
            drawPath(outline.path, brush = brush, style = style)
        }
    }
}
