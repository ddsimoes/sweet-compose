package androidx.compose.ui.graphics

import androidx.compose.runtime.Stable
import org.eclipse.swt.graphics.RGB
import kotlin.math.pow
import kotlin.math.roundToInt

@Stable
@JvmInline
value class Color(
    val value: ULong,
) {
    constructor(red: Int, green: Int, blue: Int, alpha: Int = 255) : this(
        (
            ((alpha and 0xFF) shl 24) or
                ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                (blue and 0xFF)
        ).toULong(),
    ) {
        require(red in 0..255) { "red must be in 0..255" }
        require(green in 0..255) { "green must be in 0..255" }
        require(blue in 0..255) { "blue must be in 0..255" }
        require(alpha in 0..255) { "alpha must be in 0..255" }
    }

    val luminance: Float
        get() {
            val r = linearize(red.toFloat() / 255f)
            val g = linearize(green.toFloat() / 255f)
            val b = linearize(blue.toFloat() / 255f)
            return 0.2126f * r + 0.7152f * g + 0.0722f * b
        }

    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1f) : this(
        (red * 255).roundToInt().coerceIn(0, 255),
        (green * 255).roundToInt().coerceIn(0, 255),
        (blue * 255).roundToInt().coerceIn(0, 255),
        (alpha * 255).roundToInt().coerceIn(0, 255),
    )

    val red: Int get() = ((value shr 16) and 0xFFu).toInt()
    val green: Int get() = ((value shr 8) and 0xFFu).toInt()
    val blue: Int get() = (value and 0xFFu).toInt()
    val alpha: Int get() = ((value shr 24) and 0xFFu).toInt()

    val isSpecified: Boolean get() = value != Unspecified.value
    val isUnspecified: Boolean get() = value == Unspecified.value

    fun copy(
        red: Int = this.red,
        green: Int = this.green,
        blue: Int = this.blue,
        alpha: Int = this.alpha,
    ): Color =
        Color(red, green, blue, alpha)

    /**
     * Linearly interpolate between two colors in sRGB space.
     */
    fun lerp(
        other: Color,
        fraction: Float,
    ): Color {
        val t = fraction.coerceIn(0f, 1f)
        return Color(
            red = (red + (other.red - red) * t).toInt(),
            green = (green + (other.green - green) * t).toInt(),
            blue = (blue + (other.blue - blue) * t).toInt(),
            alpha = (alpha + (other.alpha - alpha) * t).toInt(),
        )
    }

    /**
     * Composite [other] over this color using the Porter-Duff SrcOver rule.
     */
    fun compositeOver(other: Color): Color {
        val srcA = alpha / 255f
        val dstA = other.alpha / 255f
        val outA = srcA + dstA * (1f - srcA)
        if (outA <= 0f) return Transparent
        val invOutA = 1f / outA
        val outR = (red * srcA + other.red * dstA * (1f - srcA)) * invOutA
        val outG = (green * srcA + other.green * dstA * (1f - srcA)) * invOutA
        val outB = (blue * srcA + other.blue * dstA * (1f - srcA)) * invOutA
        return Color(outR / 255f, outG / 255f, outB / 255f, outA)
    }

    /**
     * Convert to SWT RGB for internal use.
     */
    fun toSwtRgb(): RGB = RGB(red, green, blue)

    companion object {
        /** A special color representing "no color." Not the same as Transparent. */
        val Unspecified: Color = Color(0x0000000100000000UL)

        val Transparent = Color(0x00000000UL)
        val Black = Color(0xFF000000UL)
        val DarkGray = Color(0xFF444444UL)
        val Gray = Color(0xFF888888UL)
        val LightGray = Color(0xFFCCCCCCUL)
        val White = Color(0xFFFFFFFFUL)
        val Red = Color(0xFFFF0000UL)
        val Green = Color(0xFF00FF00UL)
        val Blue = Color(0xFF0000FFUL)
        val Yellow = Color(0xFFFFFF00UL)
        val Cyan = Color(0xFF00FFFFUL)
        val Magenta = Color(0xFFFF00FFUL)
    }
}

/** Top-level factory: `Color(0xFFAARRGGBB)` — matches Compose's Long-based constructor. */
@Stable
fun Color(color: Long): Color = Color(color.toULong())

/** Top-level factory: `Color(0xAARRGGBB)` — matches Compose's Int-based constructor. */
@Stable
fun Color(color: Int): Color = Color(color.toULong())

private fun linearize(channel: Float): Float =
    if (channel <= 0.04045f) {
        channel / 12.92f
    } else {
        ((channel + 0.055f) / 1.055f).pow(2.4f)
    }
