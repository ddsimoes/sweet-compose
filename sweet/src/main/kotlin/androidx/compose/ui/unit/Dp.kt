package androidx.compose.ui.unit

import androidx.compose.runtime.Stable

/**
 * Standard Compose Dp unit
 */
@Stable
@JvmInline
value class Dp(val value: Float) : Comparable<Dp> {
    override fun compareTo(other: Dp): Int = value.compareTo(other.value)

    operator fun plus(other: Dp): Dp = Dp(value + other.value)

    operator fun minus(other: Dp): Dp = Dp(value - other.value)

    operator fun times(other: Float): Dp = Dp(value * other)

    operator fun div(other: Float): Dp = Dp(value / other)

    operator fun unaryMinus(): Dp = Dp(-value)

    override fun toString(): String = "${value}dp"

    companion object {
        /**
         * Unspecified Dp value
         */
        val Unspecified: Dp get() = Dp(Float.NaN)
    }
}

val Int.dp: Dp get() = Dp(this.toFloat())
val Float.dp: Dp get() = Dp(this)
val Double.dp: Dp get() = Dp(this.toFloat())

/**
 * Standard DpSize for representing width/height pairs
 */
@Stable
data class DpSize(
    val width: Dp,
    val height: Dp,
) {
    companion object {
        val Zero = DpSize(0.dp, 0.dp)
        val Unspecified = DpSize(Dp.Unspecified, Dp.Unspecified)
    }
}
