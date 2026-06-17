package androidx.compose.foundation.shape

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * A shape that represents a perfect circle
 */
object CircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = minOf(size.width, size.height) / 2f
        val rect = Rect(0f, 0f, size.width, size.height)
        val roundRect = RoundRect(rect, radius)
        return Outline.Rounded(roundRect)
    }
    
    override fun toString(): String = "CircleShape"
}

/**
 * A shape with rounded corners
 */
class RoundedCornerShape internal constructor(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp
) : Shape {
    
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val topStartPx = topStart.value * density.density
        val topEndPx = topEnd.value * density.density
        val bottomEndPx = bottomEnd.value * density.density
        val bottomStartPx = bottomStart.value * density.density
        
        val rect = Rect(0f, 0f, size.width, size.height)
        
        return if (topStartPx == topEndPx && topEndPx == bottomEndPx && bottomEndPx == bottomStartPx) {
            // All corners are the same - use simple RoundRect constructor
            val roundRect = RoundRect(rect, topStartPx)
            Outline.Rounded(roundRect)
        } else {
            // Different corner radii
            val roundRect = RoundRect(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
                topLeftCornerRadius = if (layoutDirection == LayoutDirection.Ltr) topStartPx else topEndPx,
                topRightCornerRadius = if (layoutDirection == LayoutDirection.Ltr) topEndPx else topStartPx,
                bottomRightCornerRadius = if (layoutDirection == LayoutDirection.Ltr) bottomEndPx else bottomStartPx,
                bottomLeftCornerRadius = if (layoutDirection == LayoutDirection.Ltr) bottomStartPx else bottomEndPx
            )
            Outline.Rounded(roundRect)
        }
    }
    
    // Getters for the corner radii
    val topStartCorner: Dp get() = topStart
    val topEndCorner: Dp get() = topEnd
    val bottomEndCorner: Dp get() = bottomEnd
    val bottomStartCorner: Dp get() = bottomStart
    
    override fun toString(): String = 
        "RoundedCornerShape(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, bottomStart=$bottomStart)"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoundedCornerShape) return false
        
        return topStart == other.topStart &&
               topEnd == other.topEnd &&
               bottomEnd == other.bottomEnd &&
               bottomStart == other.bottomStart
    }
    
    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }
}

/**
 * A shape with no rounded corners (rectangle)
 */
object RectangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rect = Rect(0f, 0f, size.width, size.height)
        return Outline.Rectangle(rect)
    }
    
    override fun toString(): String = "RectangleShape"
}

/**
 * Creates a rounded corner shape with the specified corner radius for all corners
 */
fun RoundedCornerShape(corner: Dp): RoundedCornerShape = 
    RoundedCornerShape(corner, corner, corner, corner)

/**
 * Creates a rounded corner shape with different radii for horizontal and vertical corners
 */
fun RoundedCornerShape(horizontal: Dp, vertical: Dp): RoundedCornerShape =
    RoundedCornerShape(horizontal, vertical, horizontal, vertical)

/**
 * Creates a rounded corner shape with different radii for each corner
 */
fun createRoundedCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp
): RoundedCornerShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
