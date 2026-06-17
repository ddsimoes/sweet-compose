package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

/**
 * MPP-compatible transform scope for [DrawScope.withTransform] two-lambda form.
 *
 * Delegates through [DrawScopeWithTransformExtKt] to the underlying
 * [DrawScope] methods so that `translate` / `scale` / `rotate` calls
 * in the transform block apply to the same DrawScope as the drawing block.
 */
interface DrawTransform {
    fun translate(
        left: Float = 0f,
        top: Float = 0f,
    )

    fun scale(
        scaleX: Float,
        scaleY: Float = scaleX,
        pivot: Offset = Offset.Zero,
    )

    fun rotate(
        degrees: Float,
        pivot: Offset = Offset.Zero,
    )

    fun clipRect(rect: Rect)

    fun clipPath(path: Path)

    fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    )
}
