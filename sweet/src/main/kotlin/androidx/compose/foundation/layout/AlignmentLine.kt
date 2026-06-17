@file:Suppress("ktlint:standard:property-naming")

package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.AlignByBlockElement
import io.github.ddsimoes.sweet.internal.LayoutDataModifier
import org.eclipse.swt.widgets.Control
import kotlin.math.max
import kotlin.math.min

@DslMarker annotation class LayoutScopeMarker

/**
 * Defines an offset line that can be used by parent layouts to align and position their children.
 * Text baselines are representative examples of [AlignmentLine]s. For example, they can be used by
 * `Row`, to align its children by baseline, or by `paddingFrom` to achieve a layout with a specific
 * distance from the top to the baseline of the text content. [AlignmentLine]s can be understood as
 * an abstraction over text baselines.
 *
 * When a layout provides a value for a particular [AlignmentLine], this can be read by the parents
 * of the layout after measuring, using the [Placeable.get] operator on the corresponding
 * [Placeable] instance. Based on the position of the [AlignmentLine], the parents can then decide
 * the positioning of the children.
 *
 * Note that when a layout provides a value for an [AlignmentLine], this will be automatically
 * inherited by the layout's parent, which will offset the value by the position of the child within
 * itself. This way, nested layout hierarchies are able to preserve the [AlignmentLine]s defined for
 * deeply nested children, making it possible for non-direct parents to use these for positioning
 * and alignment. When a layout inherits multiple values for the same [AlignmentLine] from different
 * children, the position of the line within the layout will be computed by merging the children
 * values using the provided [merger]. If a layout provides a value for an [AlignmentLine], this
 * will always be the position of the line, regardless of the values provided by children for the
 * same line.
 *
 * [AlignmentLine]s cannot be created directly, please create [VerticalAlignmentLine] or
 * [HorizontalAlignmentLine] instances instead.
 *
 * @sample androidx.compose.ui.samples.AlignmentLineSample
 * @see VerticalAlignmentLine
 * @see HorizontalAlignmentLine
 */
@Immutable
sealed class AlignmentLine(
    internal val merger: (Int, Int) -> Int,
) {
    companion object {
        /** Constant representing that an [AlignmentLine] has not been provided. */
        const val Unspecified = Int.MIN_VALUE
    }
}

/**
 * Merges two values of the current [alignment line][AlignmentLine]. This is used when a layout
 * inherits multiple values for the same [AlignmentLine] from different children, so the position of
 * the line within the layout will be computed by merging the children values using the provided
 * [AlignmentLine.merger].
 */
internal fun AlignmentLine.merge(
    position1: Int,
    position2: Int,
) = merger(position1, position2)

/**
 * A vertical [AlignmentLine]. Defines a vertical offset line that can be used by parent layouts
 * usually to align or position their children horizontally. The positions of the alignment lines
 * will be automatically inherited by parent layouts from their content, and the [merger] will be
 * used to merge multiple line positions when more than one child provides a specific
 * [AlignmentLine]. See [AlignmentLine] for more details.
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class VerticalAlignmentLine(
    merger: (Int, Int) -> Int,
) : AlignmentLine(merger)

/**
 * A horizontal [AlignmentLine]. Defines an horizontal offset line that can be used by parent
 * layouts usually to align or position their children vertically. Text baselines (`FirstBaseline`
 * and `LastBaseline`) are representative examples of [HorizontalAlignmentLine]s. For example, they
 * can be used by `Row`, to align its children by baseline, or by `paddingFrom` to achieve a layout
 * with a specific from the top to the baseline of the text content. The positions of the alignment
 * lines will be automatically inherited by parent layouts from their content, and the [merger] will
 * be used to merge multiple line positions when more than one child provides a specific
 * [HorizontalAlignmentLine]. See [AlignmentLine] for more details.
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class HorizontalAlignmentLine(
    merger: (Int, Int) -> Int,
) : AlignmentLine(merger)

/**
 * [AlignmentLine] defined by the baseline of a first line of a
 * [androidx.compose.foundation.text.BasicText]
 */
val FirstBaseline = HorizontalAlignmentLine(::min)

/**
 * [AlignmentLine] defined by the baseline of the last line of a
 * [androidx.compose.foundation.text.BasicText]
 */
val LastBaseline = HorizontalAlignmentLine(::max)

// Internal key used to attach alignBy-block lambdas to SWT controls
internal const val ALIGN_BY_BLOCK_KEY: String = "__sweet_alignByBlock"

/** A [Measured] corresponds to a layout that has been measured by its parent layout. */
interface Measured {
    /** The measured width of the layout. This might not respect the measurement constraints. */
    val measuredWidth: Int

    /** The measured height of the layout. This might not respect the measurement constraints. */
    val measuredHeight: Int

    /** Data provided by the [ParentDataModifier] applied to the layout. */
    val parentData: Any?
        get() = null

    /**
     * Returns the position of an [alignment line][AlignmentLine], or [AlignmentLine.Unspecified] if
     * the line is not provided.
     */
    operator fun get(alignmentLine: AlignmentLine): Int
}

/**
 * Placeholder modifier for alignment-line blocks.
 *
 * This modifier records the provided [block] on the underlying SWT control
 * so that layout delegates can use it to compute per-child alignment lines.
 */
internal class WithAlignmentLineBlockElement(
    private val block: (Measured) -> Int,
) : AlignByBlockElement {
    // Exposed generically; applySWTModifier records it under ALIGN_BY_BLOCK_KEY (and clears it when
    // the modifier is removed) so Row/Column delegates can retrieve it during placement.
    override val alignByBlockData: Any get() = block

    override fun apply(control: Control) {
        control.setData(ALIGN_BY_BLOCK_KEY, block)
    }
}

/**
 * Legacy placeholder for layout weight.
 *
 * Weight is implemented via [SweetLayoutData.weight]. This element
 * now delegates to the same data path as [WeightModifier] so that
 * any code that still constructs [LayoutWeightElement] explicitly
 * participates in Row/Column weight distribution.
 */
internal class LayoutWeightElement(
    private val weight: Float,
    private val fill: Boolean,
) : LayoutDataModifier {
    // Mirror WeightModifier: record weight in SweetLayoutData so Row/Column delegates can pick it up
    // from SweetLayoutData.weight. The `fill` parameter is not modeled separately yet; weighted
    // children always receive their allocated size.
    override fun applyTo(data: SweetCompositionData): SweetCompositionData {
        if (!fill && SweetDebugger.assertionEnabled) {
            SweetDebugger.log(
                "LayoutModifier",
                "LayoutWeightElement.fill is currently ignored; only weight affects layout.",
            )
        }
        val w = weight
        return data.withLayoutData { copy(weight = w) }
    }
}
