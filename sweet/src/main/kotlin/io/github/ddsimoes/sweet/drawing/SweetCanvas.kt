package io.github.ddsimoes.sweet.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import io.github.ddsimoes.sweet.image.SweetImageBitmap

/**
 * Bundles all drawing parameters for [SweetCanvas] operations.
 */
internal data class Paint(
    val color: Color = Color.Black,
    val brush: Brush? = null,
    val style: DrawStyle = Fill,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
)

/**
 * Internal abstraction over the underlying drawing backend.
 *
 * Implementations translate these high-level operations to SWT GC
 * calls (or other backends in the future).
 */
internal interface SweetCanvas {
    fun drawRect(
        topLeft: Offset,
        size: Size,
        paint: Paint,
    )

    fun drawRoundRect(
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        paint: Paint,
    )

    fun drawCircle(
        center: Offset,
        radius: Float,
        paint: Paint,
    )

    fun drawOval(
        topLeft: Offset,
        size: Size,
        paint: Paint,
    )

    fun drawArc(
        topLeft: Offset,
        size: Size,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    )

    fun drawLine(
        start: Offset,
        end: Offset,
        paint: Paint,
    )

    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        paint: Paint,
    )

    fun drawPath(
        path: Path,
        paint: Paint,
    )

    fun drawImage(
        image: SweetImageBitmap,
        topLeft: Offset,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
    )

    fun drawImage(
        image: SweetImageBitmap,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
    )

    fun translate(
        dx: Float,
        dy: Float,
    )

    fun scale(
        sx: Float,
        sy: Float,
    )

    fun rotate(
        degrees: Float,
    )

    fun clipRect(rect: Rect)

    fun clipPath(path: Path)

    fun drawText(
        text: String,
        position: Offset,
        fontSize: Float,
        color: Color,
    )

    fun save()

    fun restore()

    fun withSaveRestore(block: () -> Unit) {
        save()
        try {
            block()
        } finally {
            restore()
        }
    }

    /**
     * Report whether this canvas backend supports the named feature.
     *
     * Currently, most features return true; radial and sweep gradients
     * may be probed via feature name (e.g. "RadialGradient", "SweepGradient").
     */
    fun supports(feature: String): Boolean
}
