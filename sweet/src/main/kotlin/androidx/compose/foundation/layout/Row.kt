package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
import androidx.compose.foundation.HorizontalScrollConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.data.sweetLayoutSpec
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.LayoutDataModifier
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.ScrollContainer
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createSweetRow
import io.github.ddsimoes.sweet.layout.LayoutSpec

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    // Detect and strip horizontalScroll from modifier so we can wrap with a scroll viewport
    var hScrollConfig: HorizontalScrollConfig? = null
    var strippedModifier: Modifier = Modifier
    modifier.foldIn(Unit) { _, element ->
        when (element) {
            is HorizontalScrollConfig -> hScrollConfig = element
            else -> strippedModifier = strippedModifier.then(element)
        }
    }

    if (hScrollConfig != null) {
        // Wrap content in a SWTScrollableNode; apply non-scroll modifiers to the scroller
        ScrollContainer(
            verticalConfig = null,
            horizontalConfig = hScrollConfig,
            strippedModifier = strippedModifier,
            debugLabel = "Row",
        ) {
            // Inner Row holds the scrollable content.
            Row(
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = verticalAlignment,
                content = content,
            )
        }
        return
    }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createSweetRow(applier, horizontalArrangement, verticalAlignment)) },
        update = {
            set(modifier) { applySWTModifier(this.control, it) }
            set(horizontalArrangement) {
                val composite = this.control as org.eclipse.swt.widgets.Composite
                val prev = composite.sweetLayoutSpec as? LayoutSpec.Row
                composite.updateSweetCompositionData {
                    withLayoutSpec(LayoutSpec.Row(it, prev?.verticalAlignment ?: Alignment.CenterVertically))
                }
            }
            set(verticalAlignment) {
                val composite = this.control as org.eclipse.swt.widgets.Composite
                val prev = composite.sweetLayoutSpec as? LayoutSpec.Row
                composite.updateSweetCompositionData {
                    withLayoutSpec(LayoutSpec.Row(prev?.horizontalArrangement ?: Arrangement.Start, it))
                }
            }
        },
        content = { RowScopeInstance.content() },
    )
}

@LayoutScopeMarker
@Immutable
interface RowScope {
    /**
     * Size the element's width proportional to its [weight] relative to other weighted sibling
     * elements in the [Row]. The parent will divide the horizontal space remaining after measuring
     * unweighted child elements and distribute it according to this weight. When [fill] is true,
     * the element will be forced to occupy the whole width allocated to it. Otherwise, the element
     * is allowed to be smaller - this will result in [Row] being smaller, as the unused allocated
     * width will not be redistributed to other siblings.
     *
     * @param weight The proportional width to give to this element, as related to the total of all
     *   weighted siblings. Must be positive.
     * @param fill When `true`, the element will occupy the whole width allocated.
     */
    @Stable
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): Modifier

    /**
     * Align the element vertically within the [Row]. This alignment will have priority over the
     * [Row]'s `verticalAlignment` parameter.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignInRow
     */
    @Stable fun Modifier.align(alignment: Alignment.Vertical): Modifier

    /**
     * Position the element vertically such that its [alignmentLine] aligns with sibling elements
     * also configured to `alignBy`. `alignBy` is a form of [align], so both modifiers will not work
     * together if specified for the same layout. `alignBy` can be used to align two layouts by
     * baseline inside a [Row], using `alignBy(FirstBaseline)`. Within a [Row], all components with
     * `alignBy` will align vertically using the specified [HorizontalAlignmentLine]s or values
     * provided using the other `alignBy` overload, forming a sibling group. At least one element of
     * the sibling group will be placed as it had [Alignment.Top] align in [Row], and the alignment
     * of the other siblings will be then determined such that the alignment lines coincide. Note
     * that if only one element in a [Row] has the `alignBy` modifier specified the element will be
     * positioned as if it had [Alignment.Top] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     * @see alignByBaseline
     */
    @Stable fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier

    /**
     * Position the element vertically such that its first baseline aligns with sibling elements
     * also configured to `alignByBaseline` or `alignBy`. This modifier is a form of [align], so
     * both modifiers will not work together if specified for the same layout. `alignByBaseline` is
     * a particular case of `alignBy`. See `alignBy` for more details.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     * @see alignBy
     */
    @Stable fun Modifier.alignByBaseline(): Modifier

    /**
     * Position the element vertically such that the alignment line for the content as determined by
     * [alignmentLineBlock] aligns with sibling elements also configured to `alignBy`. `alignBy` is
     * a form of [align], so both modifiers will not work together if specified for the same layout.
     * Within a [Row], all components with `alignBy` will align vertically using the specified
     * [HorizontalAlignmentLine]s or values obtained from [alignmentLineBlock], forming a sibling
     * group. At least one element of the sibling group will be placed as it had [Alignment.Top]
     * align in [Row], and the alignment of the other siblings will be then determined such that the
     * alignment lines coincide. Note that if only one element in a [Row] has the `alignBy` modifier
     * specified the element will be positioned as if it had [Alignment.Top] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignByInRow
     */
    @Stable fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier
}

internal object RowScopeInstance : RowScope {
    @Stable
    override fun Modifier.weight(
        weight: Float,
        fill: Boolean,
    ): Modifier = this.then(WeightModifier(weight))

    @Stable
    override fun Modifier.align(alignment: Alignment.Vertical) = this.then(VerticalAlignElement(alignment))

    @Stable
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine) =
        this.then(
            WithAlignmentLineBlockElement { measured ->
                measured[alignmentLine]
            },
        )

    @Stable override fun Modifier.alignByBaseline() = alignBy(FirstBaseline)

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int) = this.then(WithAlignmentLineBlockElement(block = alignmentLineBlock))
}

internal class VerticalAlignElement(
    val vertical: Alignment.Vertical,
) : LayoutDataModifier {
    // Store per-child vertical alignment override so RowDelegate can respect it.
    override fun applyTo(data: SweetCompositionData) = data.withLayoutData { copy(verticalAlignmentOverride = vertical) }
}
