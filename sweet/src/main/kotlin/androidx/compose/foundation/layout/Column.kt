package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
import androidx.compose.foundation.VerticalScrollConfig
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
import io.github.ddsimoes.sweet.internal.createSweetColumn
import io.github.ddsimoes.sweet.layout.LayoutSpec
import org.eclipse.swt.widgets.Composite

internal class HorizontalAlignElement(
    val horizontal: Alignment.Horizontal,
) : LayoutDataModifier {
    // Store per-child horizontal alignment override so ColumnDelegate can respect it.
    override fun applyTo(data: SweetCompositionData) = data.withLayoutData { copy(horizontalAlignmentOverride = horizontal) }
}

/** Scope for the children of [Column]. */
@LayoutScopeMarker
@Immutable
interface ColumnScope {
    /**
     * Size the element's height proportional to its [weight] relative to other weighted sibling
     * elements in the [Column]. The parent will divide the vertical space remaining after measuring
     * unweighted child elements and distribute it according to this weight. When [fill] is true,
     * the element will be forced to occupy the whole height allocated to it. Otherwise, the element
     * is allowed to be smaller - this will result in [Column] being smaller, as the unused
     * allocated height will not be redistributed to other siblings.
     *
     * In a [FlowColumn], when a weight is applied to an item, the item is scaled based on the
     * number of weighted items that fall on the column it was placed in.
     *
     * @param weight The proportional height to give to this element, as related to the total of all
     *   weighted siblings. Must be positive.
     * @param fill When `true`, the element will occupy the whole height allocated.
     * @sample androidx.compose.foundation.layout.samples.SimpleColumn
     */
    @Stable
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
        fill: Boolean = true,
    ): Modifier

    /**
     * Align the element horizontally within the [Column]. This alignment will have priority over
     * the [Column]'s `horizontalAlignment` parameter.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleAlignInColumn
     */
    @Stable fun Modifier.align(alignment: Alignment.Horizontal): Modifier

    /**
     * Position the element horizontally such that its [alignmentLine] aligns with sibling elements
     * also configured to [alignBy]. [alignBy] is a form of [align], so both modifiers will not work
     * together if specified for the same layout. Within a [Column], all components with [alignBy]
     * will align horizontally using the specified [VerticalAlignmentLine]s or values provided using
     * the other [alignBy] overload, forming a sibling group. At least one element of the sibling
     * group will be placed as it had [Alignment.Start] align in [Column], and the alignment of the
     * other siblings will be then determined such that the alignment lines coincide. Note that if
     * only one element in a [Column] has the [alignBy] modifier specified the element will be
     * positioned as if it had [Alignment.Start] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleRelativeToSiblingsInColumn
     */
    @Stable fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine): Modifier

    /**
     * Position the element horizontally such that the alignment line for the content as determined
     * by [alignmentLineBlock] aligns with sibling elements also configured to [alignBy]. [alignBy]
     * is a form of [align], so both modifiers will not work together if specified for the same
     * layout. Within a [Column], all components with [alignBy] will align horizontally using the
     * specified [VerticalAlignmentLine]s or values obtained from [alignmentLineBlock], forming a
     * sibling group. At least one element of the sibling group will be placed as it had
     * [Alignment.Start] align in [Column], and the alignment of the other siblings will be then
     * determined such that the alignment lines coincide. Note that if only one element in a
     * [Column] has the [alignBy] modifier specified the element will be positioned as if it had
     * [Alignment.Start] align.
     *
     * Example usage:
     *
     * @sample androidx.compose.foundation.layout.samples.SimpleRelativeToSiblings
     */
    @Stable fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier
}

internal object ColumnScopeInstance : ColumnScope {
    @Stable
    override fun Modifier.weight(
        weight: Float,
        fill: Boolean,
    ): Modifier = this.then(WeightModifier(weight))

    @Stable
    override fun Modifier.align(alignment: Alignment.Horizontal) = this.then(HorizontalAlignElement(horizontal = alignment))

    @Stable
    override fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine) =
        this.then(
            WithAlignmentLineBlockElement { measured ->
                measured[alignmentLine]
            },
        )

    @Stable
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int) = this.then(WithAlignmentLineBlockElement(block = alignmentLineBlock))
}

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    // Detect and strip verticalScroll from modifier so we can wrap with a scroll viewport
    var scrollConfig: VerticalScrollConfig? = null
    var strippedModifier: Modifier = Modifier
    modifier.foldIn(Unit) { _, element ->
        when (element) {
            is VerticalScrollConfig -> scrollConfig = element
            else -> strippedModifier = strippedModifier.then(element)
        }
    }

    if (scrollConfig != null) {
        // Wrap content in a SWTScrollableNode; apply non-scroll modifiers to the scroller
        ScrollContainer(
            verticalConfig = scrollConfig,
            horizontalConfig = null,
            strippedModifier = strippedModifier,
            debugLabel = "Column",
        ) {
            // Inner Column holds the scrollable content. Fill width, grow height.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
        }
        return
    }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createSweetColumn(applier, verticalArrangement, horizontalAlignment)) },
        update = {
            set(modifier) { applySWTModifier(this.control, it) }
            set(verticalArrangement) {
                val composite = this.control as Composite
                val prev = composite.sweetLayoutSpec as? LayoutSpec.Column
                composite.updateSweetCompositionData {
                    withLayoutSpec(LayoutSpec.Column(it, prev?.horizontalAlignment ?: Alignment.Start))
                }
            }
            set(horizontalAlignment) {
                val composite = this.control as Composite
                val prev = composite.sweetLayoutSpec as? LayoutSpec.Column
                composite.updateSweetCompositionData {
                    withLayoutSpec(LayoutSpec.Column(prev?.verticalArrangement ?: Arrangement.Top, it))
                }
            }
        },
        content = { ColumnScopeInstance.content() },
    )
}
