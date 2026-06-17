package androidx.compose.ui.graphics

sealed class ColorFilter {
    /** Tint a source with [color] using [blendMode]. */
    data class Tint(val color: Color, val blendMode: BlendMode = BlendMode.SrcIn) : ColorFilter()

    /** Color matrix filter (4x5 matrix). Best-effort on GC backend. */
    data class ColorMatrix(val colorMatrix: FloatArray) : ColorFilter() {
        init {
            require(colorMatrix.size == 20) { "ColorMatrix must have exactly 20 elements" }
        }

        override fun equals(other: Any?): Boolean =
            other is ColorMatrix && colorMatrix.contentEquals(other.colorMatrix)

        override fun hashCode(): Int = colorMatrix.contentHashCode()
    }

    /** Lighting color filter. Best-effort on GC backend. */
    data class Lighting(val multiply: Color, val add: Color) : ColorFilter()

    companion object {
        /** MPP-compatible factory for [Tint]. */
        fun tint(
            color: Color,
            blendMode: BlendMode = BlendMode.SrcIn,
        ): ColorFilter =
            Tint(color, blendMode)
    }
}
