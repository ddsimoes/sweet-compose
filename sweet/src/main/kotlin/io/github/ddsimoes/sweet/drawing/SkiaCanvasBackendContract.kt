@file:Suppress("unused")

package io.github.ddsimoes.sweet.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode

/**
 * **Skia backend sketch — no implementation.**
 *
 * Defines the contract a Skia-based [SweetCanvas] would fulfill.
 * Exists as an interface seam so a Skia backend can be plugged in later
 * without changing the public API.
 *
 * The current production backend is [SwtCanvasBackend] (SWT GC).
 */
internal sealed interface SkiaCanvasBackendContract {
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

    fun drawOval(
        topLeft: Offset,
        size: Size,
        paint: Paint,
    )

    fun drawCircle(
        center: Offset,
        radius: Float,
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

    fun drawText(
        text: String,
        topLeft: Offset,
        fontSize: Float,
        color: Color,
    )

    fun drawImage(
        image: io.github.ddsimoes.sweet.image.SweetImageBitmap,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        alpha: Float,
        colorFilter: ColorFilter?,
    )

    fun translate(
        dx: Float,
        dy: Float,
    )

    fun scale(
        sx: Float,
        sy: Float,
    )

    fun rotate(degrees: Float)

    fun clipRect(rect: Rect)

    fun clipPath(path: Path)

    fun save()

    fun restore()

    fun supports(feature: String): Boolean

    companion object {
        const val FEATURE_LINEAR_GRADIENT = "linear_gradient"
        const val FEATURE_RADIAL_GRADIENT = "radial_gradient"
        const val FEATURE_SWEEP_GRADIENT = "sweep_gradient"
        const val FEATURE_COLOR_MATRIX = "color_matrix"
        const val FEATURE_LIGHTING = "lighting"
        const val FEATURE_BLEND_MODES = "blend_modes"
    }
}
