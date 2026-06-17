@file:Suppress("UnusedParameter")

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Display a bitmap image using the Canvas composable.
 *
 * Computes the scaled size via [contentScale], positions via
 * [alignment], and draws with src/dst rects for faithful
 * Compose semantics.
 */
@Composable
fun Image(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    Canvas(modifier = modifier) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val scaledSize = contentScale.computeSize(srcWidth, srcHeight, size.width, size.height)

        val scaledIntSize = IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt())
        val spaceIntSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        val intOffset = alignment.align(scaledIntSize, spaceIntSize, layoutDirection)

        drawImage(
            image = bitmap,
            srcOffset = Offset.Zero,
            srcSize = Size(srcWidth, srcHeight),
            dstOffset = Offset(intOffset.x.toFloat(), intOffset.y.toFloat()),
            dstSize = scaledSize,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}

/**
 * Display content from a [Painter].
 *
 * Uses the painter's [Painter.intrinsicSize] for scaling
 * computation, then translates and scales the draw scope
 * so the painter renders at the correct position and size.
 */
@Composable
fun Image(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    Canvas(modifier = modifier) {
        val srcSize = painter.intrinsicSize
        val (scaledWidth, scaledHeight) =
            if (srcSize.isUnspecified) {
                // Painter has no intrinsic size — use the destination
                // canvas size so the painter fills the available area.
                size.width to size.height
            } else {
                val s = contentScale.computeSize(srcSize.width, srcSize.height, size.width, size.height)
                s.width to s.height
            }

        val scaledIntSize = IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt())
        val spaceIntSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        val intOffset = alignment.align(scaledIntSize, spaceIntSize, layoutDirection)

        withTransform {
            translate(intOffset.x.toFloat(), intOffset.y.toFloat())
            if (!srcSize.isUnspecified) {
                scale(scaledWidth / srcSize.width, scaledHeight / srcSize.height)
            }
            with(painter) {
                onDraw()
            }
        }
    }
}
