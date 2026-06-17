package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Defines the padding values for all four sides of a component
 */
@Stable
interface PaddingValues {
    /**
     * The padding value for the start edge (left in LTR, right in RTL)
     */
    fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp
    
    /**
     * The padding value for the top edge
     */
    fun calculateTopPadding(): Dp
    
    /**
     * The padding value for the end edge (right in LTR, left in RTL)
     */
    fun calculateRightPadding(layoutDirection: LayoutDirection): Dp
    
    /**
     * The padding value for the bottom edge
     */
    fun calculateBottomPadding(): Dp
}

/**
 * Implementation of PaddingValues with absolute values for all sides
 */
@Stable
private class AbsolutePaddingValues(
    private val left: Dp = 0.dp,
    private val top: Dp = 0.dp,
    private val right: Dp = 0.dp,
    private val bottom: Dp = 0.dp
) : PaddingValues {
    
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = left
    override fun calculateTopPadding(): Dp = top
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = right
    override fun calculateBottomPadding(): Dp = bottom
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbsolutePaddingValues) return false
        
        return left == other.left &&
               top == other.top &&
               right == other.right &&
               bottom == other.bottom
    }
    
    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }
    
    override fun toString(): String =
        "PaddingValues(left=$left, top=$top, right=$right, bottom=$bottom)"
}

/**
 * Creates PaddingValues with the same value for all sides
 */
fun PaddingValues(all: Dp): PaddingValues = 
    AbsolutePaddingValues(all, all, all, all)

/**
 * Creates PaddingValues with different values for horizontal and vertical sides
 */
fun PaddingValues(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues = 
    AbsolutePaddingValues(horizontal, vertical, horizontal, vertical)

/**
 * Creates PaddingValues with individual values for each side
 */
fun PaddingValues(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
): PaddingValues = 
    AbsolutePaddingValues(start, top, end, bottom)

/**
 * No padding values - convenience constant
 */
val NoPadding: PaddingValues = PaddingValues(0.dp)

/**
 * Standard small padding values
 */
val SmallPadding: PaddingValues = PaddingValues(8.dp)

/**
 * Standard medium padding values  
 */
val MediumPadding: PaddingValues = PaddingValues(16.dp)

/**
 * Standard large padding values
 */
val LargePadding: PaddingValues = PaddingValues(24.dp)
