package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable

@Stable
data class RoundRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val topLeftCornerRadius: Float = 0f,
    val topRightCornerRadius: Float = 0f,
    val bottomRightCornerRadius: Float = 0f,
    val bottomLeftCornerRadius: Float = 0f,
) {
    constructor(
        rect: Rect,
        cornerRadius: Float,
    ) : this(
        rect.left,
        rect.top,
        rect.right,
        rect.bottom,
        cornerRadius,
        cornerRadius,
        cornerRadius,
        cornerRadius,
    )

    constructor(
        rect: Rect,
        cornerRadius: CornerRadius,
    ) : this(
        rect.left,
        rect.top,
        rect.right,
        rect.bottom,
        cornerRadius.x,
        cornerRadius.y,
        cornerRadius.x,
        cornerRadius.y,
    )

    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val rect: Rect get() = Rect(left, top, right, bottom)

    companion object {
        val Zero: RoundRect = RoundRect(0f, 0f, 0f, 0f)
    }
}
