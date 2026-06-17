@file:Suppress("ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.foundation.lazy

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.SWTScrollableNode
import io.github.ddsimoes.sweet.internal.applySWTModifier

/**
 * LazyColumn is a vertically scrolling list backed by a `Column` inside a Sweet scroll viewport.
 *
 * Sweet's current implementation is intentionally simple:
 * - All items described in [content] are composed eagerly.
 * - Scrolling is provided by SWT's scrollbars; there is no item virtualization.
 * - Large lists are supported functionally, but without the memory/perf benefits of a true lazy list yet.
 *
 * @param modifier the modifier to apply to this layout
 * @param contentPadding a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is allowed
 * @param content the LazyListScope which describes the content
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val scope = LazyListScope()
    scope.content()
    val items = scope.build()
    val applier = LocalSWTNodeApplier.current

    // Create a scrollable viewport (Sweet ScrollViewport). Pass the LazyListState's
    // internal ScrollState for scroll mechanics, and the LazyListState itself so the
    // node can wire content+viewport references for visible-item tracking.
    ComposeNode<SWTScrollableNode, SWTNodeApplier>(
        factory = {
            SWTScrollableNode(
                parentApplier = applier,
                vertical = true,
                horizontal = false,
                scrollState = state.scrollState,
                lazyListState = state,
            )
        },
        update = {
            // Fill is injected into the chain (rather than relying on the node's constructor-set
            // layout data) so modifier reconciliation, which rebuilds layout data from the chain,
            // preserves the scroll container's fill. Matches the Column/Row verticalScroll path.
            set(modifier) { applySWTModifier(this.control, Modifier.fillMaxWidth().fillMaxHeight().then(it)) }
        },
    ) {
        // Use Column layout for the items
        Column(
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            if (reverseLayout) {
                items.reversed().forEach { item ->
                    item()
                }
            } else {
                items.forEach { item ->
                    item()
                }
            }
        }
    }
}
