package androidx.compose.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * 2D alignment interface matching Compose's [Alignment].
 *
 * Aligns a 2D item of [size] within a 2D [space], producing an [IntOffset].
 * Used for both drawing (Image/Painter placement) and layout (Box/Column/Row).
 */
@Stable
fun interface Alignment {
    fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset

    companion object {
        // ── 2D alignment values ──

        val TopStart: Alignment = BiasAlignment(-1f, -1f)
        val TopCenter: Alignment = BiasAlignment(0f, -1f)
        val TopEnd: Alignment = BiasAlignment(1f, -1f)
        val CenterStart: Alignment = BiasAlignment(-1f, 0f)
        val Center: Alignment = BiasAlignment(0f, 0f)
        val CenterEnd: Alignment = BiasAlignment(1f, 0f)
        val BottomStart: Alignment = BiasAlignment(-1f, 1f)
        val BottomCenter: Alignment = BiasAlignment(0f, 1f)
        val BottomEnd: Alignment = BiasAlignment(1f, 1f)

        // ── 1D alignment subtypes for Column/Row ──

        val Start: Horizontal = BiasAlignment.Horizontal(-1f)
        val CenterHorizontally: Horizontal = BiasAlignment.Horizontal(0f)
        val End: Horizontal = BiasAlignment.Horizontal(1f)

        val Top: Vertical = BiasAlignment.Vertical(-1f)
        val CenterVertically: Vertical = BiasAlignment.Vertical(0f)
        val Bottom: Vertical = BiasAlignment.Vertical(1f)
    }

    /** 1D horizontal alignment for Row/Column cross-axis. */
    @Immutable
    fun interface Horizontal : Alignment

    /** 1D vertical alignment for Row/Column cross-axis. */
    @Immutable
    fun interface Vertical : Alignment
}

/**
 * 2D alignment parameterized by horizontal and vertical bias.
 *
 * Bias values range from -1f (start) to 1f (end). 0f = center.
 */
@Immutable
internal data class BiasAlignment(
    val horizontalBias: Float,
    val verticalBias: Float,
) : Alignment {
    override fun align(
        size: IntSize,
        space: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset {
        val centerX = (space.width - size.width) / 2f
        val centerY = (space.height - size.height) / 2f
        val resolvedHorizontalBias = if (layoutDirection == LayoutDirection.Ltr) horizontalBias else -horizontalBias
        val x = centerX * (1 + resolvedHorizontalBias)
        val y = centerY * (1 + verticalBias)
        return IntOffset(x.roundToInt(), y.roundToInt())
    }

    @Immutable
    internal data class Horizontal(val bias: Float) : Alignment.Horizontal {
        override fun align(
            size: IntSize,
            space: IntSize,
            layoutDirection: LayoutDirection,
        ): IntOffset {
            val centerX = (space.width - size.width) / 2f
            val resolvedBias = if (layoutDirection == LayoutDirection.Ltr) bias else -bias
            return IntOffset((centerX * (1 + resolvedBias)).roundToInt(), 0)
        }
    }

    @Immutable
    internal data class Vertical(val bias: Float) : Alignment.Vertical {
        override fun align(
            size: IntSize,
            space: IntSize,
            layoutDirection: LayoutDirection,
        ): IntOffset {
            val centerY = (space.height - size.height) / 2f
            return IntOffset(0, (centerY * (1 + bias)).roundToInt())
        }
    }
}
