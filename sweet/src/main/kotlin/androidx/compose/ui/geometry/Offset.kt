package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.sqrt

@Stable
@JvmInline
value class Offset(val packedValue: Long) {
    val x: Float get() = unpackFloat1(packedValue)
    val y: Float get() = unpackFloat2(packedValue)

    operator fun component1(): Float = x

    operator fun component2(): Float = y

    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

    operator fun times(operand: Float): Offset = Offset(x * operand, y * operand)

    operator fun div(operand: Float): Offset = Offset(x / operand, y / operand)

    operator fun unaryMinus(): Offset = Offset(-x, -y)

    fun copy(
        x: Float = this.x,
        y: Float = this.y,
    ): Offset = Offset(x, y)

    val isFinite: Boolean get() = x.isFinite() && y.isFinite()
    val isUnspecified: Boolean get() = x.isNaN() && y.isNaN()

    fun getDistance(): Float = sqrt(x * x + y * y)

    fun getDistanceSquared(): Float = x * x + y * y

    fun min(other: Offset): Offset = Offset(kotlin.math.min(x, other.x), kotlin.math.min(y, other.y))

    fun max(other: Offset): Offset = Offset(kotlin.math.max(x, other.x), kotlin.math.max(y, other.y))

    override fun toString(): String = "Offset($x, $y)"

    companion object {
        val Zero: Offset = Offset(0L)

        /**
         * A special sentinel used when an offset is not specified.
         * Matches `Offset(Float.NaN, Float.NaN)`.
         */
        val Unspecified: Offset = Offset(packFloats(Float.NaN, Float.NaN))

        /**
         * An offset used for infinite bounds.
         */
        val Infinite: Offset =
            Offset(
                packFloats(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            )
    }
}

@Stable
fun Offset(
    x: Float,
    y: Float,
): Offset = Offset(packFloats(x, y))
