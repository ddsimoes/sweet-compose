package androidx.compose.ui.unit

import androidx.compose.runtime.Stable

/**
 * Standard TextUnit class for font sizes
 */
@Stable
@JvmInline
value class TextUnit(val value: Float) {
    companion object {
        val Unspecified = TextUnit(Float.NaN)
    }
}

val Int.sp: TextUnit get() = TextUnit(this.toFloat())
val Float.sp: TextUnit get() = TextUnit(this)
val Double.sp: TextUnit get() = TextUnit(this.toFloat())
