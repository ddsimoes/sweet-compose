@file:Suppress("UnusedParameter")

package androidx.compose.foundation

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.internal.SWTModifierElement
import org.eclipse.swt.widgets.Control

/**
 * Create and [remember] the [ScrollState] based on the currently appropriate scroll
 * configuration to allow changing scroll position or observing scroll behavior.
 *
 * @param initial initial scroller position to start with
 */
@Composable
fun rememberScrollState(initial: Int = 0): ScrollState = remember { ScrollState(initial) }

/**
 * State of the scroll. Allows the developer to change the scroll position or get current state
 * by calling methods on this object. To be hosted and passed to [Modifier.verticalScroll] or
 * [Modifier.horizontalScroll].
 *
 * Sweet implementation notes (vs. MPP `androidx.compose.foundation.ScrollState`):
 * - [scrollTo] is immediate (no scroll mutation priority/cancellation semantics yet).
 * - `animateScrollTo` is not available yet — it is gated on the animation foundation
 *   (`AnimationSpec`/`SpringSpec`, doc 21).
 * - [isScrollInProgress] is best-effort: `true` while a native scrollbar thumb drag is in
 *   progress; gesture/fling tracking is not implemented.
 *
 * @param initial value of the scroll
 */
@Stable
class ScrollState(
    initial: Int = 0,
) {
    /**
     * current scroll position value in pixels
     */
    var value: Int by mutableIntStateOf(initial)
        private set

    private var maxValueState = mutableIntStateOf(0)

    /**
     * maximum bound for [value]. Starts at 0 and is updated by the scroll container on every
     * viewport layout (`contentExtent - viewportExtent`, clamped to >= 0).
     */
    var maxValue: Int
        get() = maxValueState.value
        internal set(newMax) {
            maxValueState.value = newMax
            Snapshot.withoutReadObservation {
                if (value > newMax) {
                    value = newMax
                }
            }
        }

    /**
     * Size of the viewport on the scrollable axis, or 0 if still unknown. Note that this value
     * is only populated after the first layout pass.
     */
    var viewportSize: Int by mutableIntStateOf(0)
        internal set

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or smooth scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * Whether this [ScrollState] is currently scrolling by gesture, fling or programmatically.
     * Best-effort in Sweet: tracks native scrollbar thumb drags only.
     */
    var isScrollInProgress: Boolean by androidx.compose.runtime.mutableStateOf(false)
        internal set

    /** Whether it is allowed to scroll forward (`value < maxValue`). */
    val canScrollForward: Boolean by derivedStateOf { value < maxValue }

    /** Whether it is allowed to scroll backward (`value > 0`). */
    val canScrollBackward: Boolean by derivedStateOf { value > 0 }

    /**
     * Instantly jump to the given position in pixels.
     *
     * Sweet executes the jump synchronously (there is no ongoing-scroll cancellation yet).
     * The associated scroll container reacts to this change and updates its viewport.
     *
     * @param value number of pixels to scroll by
     * @return the amount of scroll consumed
     */
    suspend fun scrollTo(value: Int): Float {
        val target = value.coerceIn(0, maxValue)
        val consumed = (target - this.value).toFloat()
        this.value = target
        return consumed
    }

    /**
     * Internal helper used by scroll containers to keep this state in sync with the
     * underlying native scrollbars/viewport geometry.
     */
    internal fun updateFromScrollable(
        position: Int,
        max: Int,
        viewport: Int,
    ) {
        maxValue = max.coerceAtLeast(0)
        viewportSize = viewport.coerceAtLeast(0)
        value = position.coerceIn(0, maxValue)
    }
}

/**
 * Configure component to be vertically scrollable.
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean bottom, when `false`, 0 [ScrollState.value] will mean top
 */
fun Modifier.verticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
): Modifier = this.then(VerticalScrollModifier(state, enabled, reverseScrolling))

/**
 * Configure component to be horizontally scrollable.
 *
 * @param state state of the scroll
 * @param enabled whether or not scrolling via touch input is enabled
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param reverseScrolling reverse the direction of scrolling, when `true`, 0 [ScrollState.value]
 * will mean right, when `false`, 0 [ScrollState.value] will mean left
 */
fun Modifier.horizontalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
): Modifier = this.then(HorizontalScrollModifier(state, enabled, reverseScrolling))

// Marker interface so layout composables can detect vertical scroll intent in the modifier chain
internal interface VerticalScrollConfig {
    val state: ScrollState
    val enabled: Boolean
    val reverseScrolling: Boolean
}

// Marker interface so layout composables can detect horizontal scroll intent in the modifier chain
internal interface HorizontalScrollConfig {
    val state: ScrollState
    val enabled: Boolean
    val reverseScrolling: Boolean
}

private class VerticalScrollModifier(
    override val state: ScrollState,
    override val enabled: Boolean,
    override val reverseScrolling: Boolean,
) : SWTModifierElement,
    VerticalScrollConfig {
    override fun apply(control: Control) {
        // Marker only: actual scroll behavior is provided by layout wrappers
        // (e.g., Column detecting VerticalScrollConfig and creating SWTScrollableNode).
        // The ScrollState is wired to the ScrollViewport there.
    }
}

private class HorizontalScrollModifier(
    override val state: ScrollState,
    override val enabled: Boolean,
    override val reverseScrolling: Boolean,
) : SWTModifierElement,
    HorizontalScrollConfig {
    override fun apply(control: Control) {
        // Marker only: actual scroll behavior is provided by layout wrappers
        // (e.g., Row detecting HorizontalScrollConfig and creating SWTScrollableNode).
        // The ScrollState is wired to the ScrollViewport there.
    }
}
