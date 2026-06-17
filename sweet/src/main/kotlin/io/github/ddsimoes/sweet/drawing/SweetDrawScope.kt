package io.github.ddsimoes.sweet.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.ddsimoes.sweet.image.SweetImageBitmapImpl

/**
 * DrawScope implementation that delegates to [SweetCanvas].
 *
 * Builds [Paint] objects from draw parameters and translates
 * the full DrawScope API into [SweetCanvas] operations.
 */
internal class SweetDrawScope(
    private val canvas: SweetCanvas,
    initialSize: Size,
    override val layoutDirection: LayoutDirection,
    override val density: Float = 1f,
    override val fontScale: Float = 1f,
) : DrawScope, Density {
    private var _size: Size = initialSize
    override val size: Size get() = _size
    override val center: Offset get() = Offset(_size.width / 2f, _size.height / 2f)

    // ── Rectangles ──────────────────────────────────────────────

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawRect(topLeft, size, paint)
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        val color = brushColor(brush)
        val paint = Paint(color, brush, style, alpha, colorFilter, blendMode)
        canvas.drawRect(topLeft, size, paint)
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawRoundRect(topLeft, size, cornerRadius, paint)
    }

    // ── Circle / Oval ───────────────────────────────────────────

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (radius <= 0f) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawCircle(center, radius, paint)
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (radius <= 0f) return
        val color = brushColor(brush)
        val paint = Paint(color, brush, style, alpha, colorFilter, blendMode)
        canvas.drawCircle(center, radius, paint)
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawOval(topLeft, size, paint)
    }

    // ── Arc ─────────────────────────────────────────────────────

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        if (sweepAngle == 0f) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawArc(topLeft, size, startAngle, sweepAngle, useCenter, paint)
    }

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (size.width <= 0f || size.height <= 0f) return
        if (sweepAngle == 0f) return
        val color = brushColor(brush)
        val paint = Paint(color, brush, style, alpha, colorFilter, blendMode)
        canvas.drawArc(topLeft, size, startAngle, sweepAngle, useCenter, paint)
    }

    // ── Line ────────────────────────────────────────────────────

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val paint =
            Paint(
                color = color,
                style = Stroke(width = strokeWidth),
                alpha = alpha,
                colorFilter = colorFilter,
                blendMode = blendMode,
            )
        canvas.drawLine(start, end, paint)
    }

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val color = brushColor(brush)
        val paint =
            Paint(
                color = color,
                brush = brush,
                style = Stroke(width = strokeWidth),
                alpha = alpha,
                colorFilter = colorFilter,
                blendMode = blendMode,
            )
        canvas.drawLine(start, end, paint)
    }
    // ── Points ──────────────────────────────────────────────────

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (points.isEmpty()) return
        val paint =
            Paint(
                color = color,
                style = Stroke(width = strokeWidth, cap = cap, pathEffect = pathEffect),
                alpha = alpha,
                colorFilter = colorFilter,
                blendMode = blendMode,
            )
        canvas.drawPoints(points, pointMode, paint)
    }

    // ── Image ───────────────────────────────────────────────────

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        colorFilter: ColorFilter?,
    ) {
        val backend = (image as? SweetImageBitmapImpl)?.backend ?: return
        canvas.drawImage(backend, topLeft, alpha, colorFilter)
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        alpha: Float,
        colorFilter: ColorFilter?,
    ) {
        val backend = (image as? SweetImageBitmapImpl)?.backend ?: return
        canvas.drawImage(backend, srcOffset, srcSize, dstOffset, dstSize, alpha, colorFilter)
    }

    // ── Path ────────────────────────────────────────────────────

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (path.isEmpty()) return
        val paint = Paint(color, null, style, alpha, colorFilter, blendMode)
        canvas.drawPath(path, paint)
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (path.isEmpty()) return
        val color = brushColor(brush)
        val paint = Paint(color, brush, style, alpha, colorFilter, blendMode)
        canvas.drawPath(path, paint)
    }

    // ── Text ────────────────────────────────────────────────────

    override fun drawText(
        text: String,
        topLeft: Offset,
        fontSize: Float,
        color: Color,
        alpha: Float,
    ) {
        if (text.isEmpty() || fontSize <= 0f) return
        canvas.drawText(text, topLeft, fontSize, color)
    }

    // ── Transforms ──────────────────────────────────────────────

    override fun translate(
        left: Float,
        top: Float,
    ) {
        if (left == 0f && top == 0f) return
        canvas.translate(left, top)
    }

    override fun scale(
        scaleX: Float,
        scaleY: Float,
        pivot: Offset,
    ) {
        if (pivot == Offset.Zero) {
            canvas.scale(scaleX, scaleY)
        } else {
            canvas.translate(pivot.x, pivot.y)
            canvas.scale(scaleX, scaleY)
            canvas.translate(-pivot.x, -pivot.y)
        }
    }

    override fun rotate(
        degrees: Float,
        pivot: Offset,
    ) {
        if (pivot == Offset.Zero) {
            canvas.rotate(degrees)
        } else {
            canvas.translate(pivot.x, pivot.y)
            canvas.rotate(degrees)
            canvas.translate(-pivot.x, -pivot.y)
        }
    }

    override fun clipRect(rect: Rect) {
        if (rect.isEmpty) return
        canvas.clipRect(rect)
    }

    override fun clipPath(path: Path) {
        if (path.isEmpty()) return
        canvas.clipPath(path)
    }

    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        clipRect(Rect(left, top, right, bottom))
    }

    // ── Scoped operations ───────────────────────────────────────

    override fun withTransform(block: DrawScope.() -> Unit) {
        canvas.withSaveRestore { this.block() }
    }

    override fun inset(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        block: DrawScope.() -> Unit,
    ) {
        withTransform {
            translate(left, top)
            val savedSize = _size
            _size =
                Size(
                    (savedSize.width - left - right).coerceAtLeast(0f),
                    (savedSize.height - top - bottom).coerceAtLeast(0f),
                )
            try {
                block()
            } finally {
                _size = savedSize
            }
        }
    }

    /**
     * Convenience helper for scoped transforms.
     * Delegates to [SweetCanvas.withSaveRestore].
     */
    inline fun withSaveRestore(crossinline block: SweetDrawScope.() -> Unit) {
        canvas.withSaveRestore { this.block() }
    }

    // ── Helpers ─────────────────────────────────────────────────

    companion object {
        /** Extract the first color from a [Brush] for building [Paint]. */
        private fun brushColor(brush: Brush): Color =
            when (brush) {
                is SolidColor -> brush.value
                is LinearGradient -> brush.colorStops.firstOrNull()?.second ?: Color.Black
                is RadialGradient -> brush.colorStops.firstOrNull()?.second ?: Color.Black
                is SweepGradient -> brush.colorStops.firstOrNull()?.second ?: Color.Black
            }
    }
}
