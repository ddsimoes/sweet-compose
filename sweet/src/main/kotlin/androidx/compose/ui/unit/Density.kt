package androidx.compose.ui.unit

import androidx.compose.runtime.Stable
import io.github.ddsimoes.sweet.internal.getDisplayDensity
import org.eclipse.swt.widgets.Display

/**
 * Density class for handling DPI-aware conversions
 */
interface Density {
    val density: Float
    val fontScale: Float

    companion object {
        val Default: Density = DensityImpl(1f)

        operator fun invoke(
            density: Float,
            fontScale: Float = 1f,
        ): Density =
            DensityImpl(density, fontScale)

        fun fromDisplay(display: Display): Density {
            val dpi = display.dpi
            val density = dpi.x / 96f
            return DensityImpl(density)
        }
    }
}

@Stable
private data class DensityImpl(
    override val density: Float,
    override val fontScale: Float = 1f,
) : Density

/**
 * Convert Dp to pixels using density
 */
fun Dp.toPx(density: Density): Float = value * density.density

/**
 * Convert Dp to pixels as Int using density
 */
fun Dp.toPxInt(density: Density): Int = (value * density.density).toInt()

/**
 * Convert pixels to Dp using density
 */
fun Float.toDp(density: Density): Dp = Dp(this / density.density)

/**
 * Convert Int pixels to Dp using density
 */
fun Int.toDp(density: Density): Dp = Dp(this / density.density)

/**
 * Convert Dp to pixels using current display density
 */
fun Dp.toPx(): Float = toPx(getDisplayDensity())

/**
 * Convert Dp to pixels as Int using current display density
 */
fun Dp.toPxInt(): Int = toPxInt(getDisplayDensity())
