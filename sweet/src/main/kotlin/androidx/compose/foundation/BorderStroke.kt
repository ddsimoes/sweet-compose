package androidx.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp

/**
 * Represents a border stroke drawn around a shape.
 *
 * **Sweet note:**  Currently not applied by [androidx.compose.material3.Surface]
 * rendering — accepted for API compatibility only.
 */
@Immutable
class BorderStroke(val width: Dp, val brush: Brush) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BorderStroke) return false
        return width == other.width && brush == other.brush
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }

    override fun toString(): String = "BorderStroke(width=$width, brush=$brush)"
}

/** Creates a [BorderStroke] with a solid [color]. */
@Stable
fun BorderStroke(
    width: Dp,
    color: Color,
) = BorderStroke(width, SolidColor(color))
