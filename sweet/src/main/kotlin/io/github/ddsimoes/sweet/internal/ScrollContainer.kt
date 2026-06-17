@file:Suppress("ktlint:standard:function-naming")

package io.github.ddsimoes.sweet.internal

import androidx.compose.foundation.HorizontalScrollConfig
import androidx.compose.foundation.VerticalScrollConfig
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * Shared scroll wrapper used by [androidx.compose.foundation.layout.Column],
 * [androidx.compose.foundation.layout.Row] and [androidx.compose.foundation.layout.Box]
 * when a `verticalScroll`/`horizontalScroll` modifier is detected in the chain.
 *
 * Hosts [content] inside an [SWTScrollableNode] (Sweet [io.github.ddsimoes.sweet.widgets.ScrollViewport])
 * and keeps the viewport offset in sync with the [androidx.compose.foundation.ScrollState] of
 * each configured axis.
 */
@Composable
internal fun ScrollContainer(
    verticalConfig: VerticalScrollConfig?,
    horizontalConfig: HorizontalScrollConfig?,
    strippedModifier: Modifier,
    debugLabel: String,
    content: @Composable () -> Unit,
) {
    val applier = LocalSWTNodeApplier.current
    if (SweetDebugger.assertionEnabled) {
        SweetDebugger.log(
            "ScrollWrap",
            "Wrapping $debugLabel with scroller (v=${verticalConfig != null}, h=${horizontalConfig != null}); " +
                "applying stripped modifiers to scroller",
        )
    }
    // The viewport's state sync supports a single state; prefer the vertical one.
    val primaryState = verticalConfig?.state ?: horizontalConfig?.state
    val primaryReverse = verticalConfig?.reverseScrolling ?: horizontalConfig?.reverseScrolling ?: false
    ComposeNode<SWTScrollableNode, SWTNodeApplier>(
        factory = {
            SWTScrollableNode(
                parentApplier = applier,
                vertical = verticalConfig != null,
                horizontal = horizontalConfig != null,
                scrollState = primaryState,
                reverseScrolling = primaryReverse,
            )
        },
        update = {
            // Apply the original modifiers (minus scroll) to the scroll container so it fills
            // width/height. Fill is injected into the chain (rather than set on the node in
            // isolation) so modifier reconciliation, which rebuilds layout data from the chain,
            // preserves it. Horizontal-only scrollers hug their content height instead of
            // filling, so they don't stretch across the parent's max height.
            val injectedFill =
                if (verticalConfig != null) {
                    Modifier.fillMaxWidth().fillMaxHeight()
                } else {
                    Modifier.fillMaxWidth()
                }
            set(strippedModifier) { applySWTModifier(this.control, injectedFill.then(it)) }
            // Keep the viewport offset in sync with ScrollState when it changes programmatically.
            set(verticalConfig?.state?.value) { scrollValue ->
                val viewport = this.control
                if (scrollValue != null && verticalConfig != null && !viewport.isDisposed) {
                    val state = verticalConfig.state
                    val clamped = scrollValue.coerceIn(0, state.maxValue)
                    val targetY =
                        if (!verticalConfig.reverseScrolling) {
                            clamped
                        } else {
                            (state.maxValue - clamped).coerceAtLeast(0)
                        }
                    if (viewport.scrollY != targetY) {
                        viewport.scrollTo(viewport.scrollX, targetY)
                    }
                }
            }
            set(horizontalConfig?.state?.value) { scrollValue ->
                val viewport = this.control
                if (scrollValue != null && horizontalConfig != null && !viewport.isDisposed) {
                    val state = horizontalConfig.state
                    val clamped = scrollValue.coerceIn(0, state.maxValue)
                    val targetX =
                        if (!horizontalConfig.reverseScrolling) {
                            clamped
                        } else {
                            (state.maxValue - clamped).coerceAtLeast(0)
                        }
                    if (viewport.scrollX != targetX) {
                        viewport.scrollTo(targetX, viewport.scrollY)
                    }
                }
            }
        },
        content = content,
    )
}
