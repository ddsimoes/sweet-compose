@file:Suppress("ktlint:standard:function-naming")

package androidx.compose.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

/**
 * Sweet Compose implementation of window positioning for SWT integration.
 * Provides compatibility with Compose Desktop positioning APIs.
 *
 * Sweet implements absolute positioning, a platform-default mode, and an
 * aligned positioning mode compatible with Compose Desktop's APIs.
 */
fun WindowPosition(
    x: Dp,
    y: Dp,
) = WindowPosition.Absolute(x, y)

fun WindowPosition(alignment: Alignment) = WindowPosition.Aligned(alignment)

/**
 * Position of the window on the screen in Dp units.
 * Sweet-specific implementation using SWT coordinates.
 */
@Immutable
sealed class WindowPosition {
    abstract val x: Dp
    abstract val y: Dp
    abstract val isSpecified: Boolean

    /**
     * Platform-default positioning using SWT's cascade behavior
     */
    object PlatformDefault : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false

        @Stable
        override fun toString() = "PlatformDefault"
    }

    /**
     * Aligned positioning within available screen bounds
     */
    @Immutable
    class Aligned(
        val alignment: Alignment,
    ) : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false

        fun copy(alignment: Alignment = this.alignment) = Aligned(alignment)

        @Stable
        override fun toString() = "Aligned($alignment)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Aligned
            return alignment == other.alignment
        }

        override fun hashCode(): Int = alignment.hashCode()
    }

    /**
     * Absolute position in screen coordinates
     */
    @Immutable
    class Absolute(
        override val x: Dp,
        override val y: Dp,
    ) : WindowPosition() {
        override val isSpecified: Boolean get() = true

        @Stable
        operator fun component1(): Dp = x

        @Stable
        operator fun component2(): Dp = y

        fun copy(
            x: Dp = this.x,
            y: Dp = this.y,
        ) = Absolute(x, y)

        @Stable
        override fun toString() = "Absolute($x, $y)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Absolute
            return x == other.x && y == other.y
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            return result
        }
    }
}
