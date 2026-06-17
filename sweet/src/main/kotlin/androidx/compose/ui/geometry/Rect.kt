package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable

@Stable
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isEmpty: Boolean get() = left >= right || top >= bottom
    val isFinite: Boolean get() =
        left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

    val center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)
    val topLeft: Offset get() = Offset(left, top)
    val topRight: Offset get() = Offset(right, top)
    val bottomLeft: Offset get() = Offset(left, bottom)
    val bottomRight: Offset get() = Offset(right, bottom)
    val size: Size get() = Size(width, height)

    operator fun contains(offset: Offset): Boolean =
        offset.x in left..right && offset.y in top..bottom

    fun translate(offset: Offset): Rect =
        Rect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)

    fun inflate(delta: Float): Rect =
        Rect(left - delta, top - delta, right + delta, bottom + delta)

    fun deflate(delta: Float): Rect =
        inflate(-delta)

    fun intersect(other: Rect): Rect {
        if (isEmpty || other.isEmpty || left >= other.right || other.left >= right ||
            top >= other.bottom || other.top >= bottom
        ) {
            return Zero
        }
        return Rect(
            kotlin.math.max(left, other.left),
            kotlin.math.max(top, other.top),
            kotlin.math.min(right, other.right),
            kotlin.math.min(bottom, other.bottom),
        )
    }

    fun overlaps(other: Rect): Boolean =
        !isEmpty && !other.isEmpty &&
            left < other.right && other.left < right &&
            top < other.bottom && other.top < bottom

    override fun toString(): String = "Rect.fromLTRB($left, $top, $right, $bottom)"

    companion object {
        val Zero: Rect = Rect(0f, 0f, 0f, 0f)

        /** Create a Rect from left, top, right, bottom. */
        fun fromLTRB(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
        ): Rect =
            Rect(left, top, right, bottom)

        /** Create a Rect from an origin and size. */
        fun fromLTWH(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
        ): Rect =
            Rect(left, top, left + width, top + height)

        /** Create a Rect centered at [center] with [size]. */
        fun center(
            center: Offset,
            radius: Float,
        ): Rect =
            fromLTRB(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    }
}
