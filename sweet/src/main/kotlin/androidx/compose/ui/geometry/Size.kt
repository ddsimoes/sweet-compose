package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

@Stable
@JvmInline
value class Size(val packedValue: Long) {
    val width: Float get() = unpackFloat1(packedValue)
    val height: Float get() = unpackFloat2(packedValue)

    operator fun component1(): Float = width

    operator fun component2(): Float = height

    operator fun times(operand: Float): Size = Size(width * operand, height * operand)

    operator fun div(operand: Float): Size = Size(width / operand, height / operand)

    fun copy(
        width: Float = this.width,
        height: Float = this.height,
    ): Size = Size(width, height)

    val isEmpty: Boolean get() = width <= 0f || height <= 0f
    val isUnspecified: Boolean get() = width.isNaN() || height.isNaN()
    val isFinite: Boolean get() = width.isFinite() && height.isFinite()

    val minDimension: Float get() = kotlin.math.min(width, height)
    val maxDimension: Float get() = kotlin.math.max(width, height)

    override fun toString(): String = "Size($width, $height)"

    companion object {
        val Zero: Size = Size(0L)

        /**
         * A special sentinel used when a size is not specified.
         * Matches `Size(Float.NaN, Float.NaN)`.
         */
        val Unspecified: Size = Size(packFloats(Float.NaN, Float.NaN))
    }
}

@Stable
fun Size(
    width: Float,
    height: Float,
): Size = Size(packFloats(width, height))
