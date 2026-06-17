package io.github.ddsimoes.sweet.layout.delegate

import androidx.compose.foundation.layout.ALIGN_BY_BLOCK_KEY
import androidx.compose.foundation.layout.AlignmentLine
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FirstBaseline
import androidx.compose.foundation.layout.LastBaseline
import androidx.compose.foundation.layout.Measured
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toPxInt
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.internal.chainPaddingBottom
import io.github.ddsimoes.sweet.internal.chainPaddingEnd
import io.github.ddsimoes.sweet.internal.chainPaddingStart
import io.github.ddsimoes.sweet.internal.chainPaddingTop
import io.github.ddsimoes.sweet.internal.getSweetDensity
import io.github.ddsimoes.sweet.layout.Constraints
import io.github.ddsimoes.sweet.layout.LayoutSpec
import io.github.ddsimoes.sweet.layout.Size
import io.github.ddsimoes.sweet.layout.SweetMeasurable
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import kotlin.math.max
import kotlin.math.min

data class ChildPlacement(
    val x: Int,
    val y: Int,
    val size: Size,
    val contentOffsetX: Int = 0,
    val contentOffsetY: Int = 0,
)

data class MeasureBundle(
    val childSizes: List<Size>,
    val parentSize: Size,
    val placements: List<ChildPlacement> = childSizes.map { ChildPlacement(0, 0, it) },
    val aux: Any? = null,
)

interface LayoutDelegate {
    fun measure(
        children: List<SweetMeasurable>,
        constraints: Constraints,
        spec: LayoutSpec,
    ): MeasureBundle

    fun place(
        children: List<SweetMeasurable>,
        width: Int,
        height: Int,
        spec: LayoutSpec,
        bundle: MeasureBundle?,
    )
}

object LayoutDelegateRegistry {
    fun forSpec(spec: LayoutSpec?): LayoutDelegate =
        when (spec) {
            is LayoutSpec.Column -> ColumnDelegate
            is LayoutSpec.Row -> RowDelegate
            is LayoutSpec.Box -> BoxDelegate
            else -> BoxDelegate // default simple stack
        }
}

private fun Control.getWeight(): Float? = (this as? org.eclipse.swt.widgets.Control)?.let { it.getWeightCompat() }

// Lightweight adapter to avoid leaking data package here
private fun Control.getWeightCompat(): Float? = this.getSweetCompositionData().layoutData.weight

private fun Constraints.loose(): Constraints = Constraints(0, maxWidth, 0, maxHeight)

private fun Constraints.constrainSize(
    width: Int,
    height: Int,
): Size = Size(constrainWidth(width), constrainHeight(height))

private fun spacingFor(
    arrangement: Any,
    childCount: Int,
    density: Density,
): Int {
    val spacing =
        when (arrangement) {
            is Arrangement.HorizontalOrVertical -> arrangement.spacing
            else -> Dp.Unspecified
        }
    if (spacing != Dp.Unspecified && childCount > 1) {
        return spacing.toPxInt(density) * (childCount - 1)
    }
    return 0
}

/** Arrangement spacing for a single gap between adjacent children, in px. */
private fun arrangementSpacingPx(
    arrangement: Any,
    density: Density,
): Int {
    val spacing =
        when (arrangement) {
            is Arrangement.HorizontalOrVertical -> arrangement.spacing
            else -> Dp.Unspecified
        }
    return if (spacing != Dp.Unspecified) spacing.toPxInt(density) else 0
}
private fun alignmentBlocks(children: List<SweetMeasurable>): List<((Measured) -> Int)?> =
    children.map { measurable ->
        @Suppress("UNCHECKED_CAST")
        measurable.control.getData(ALIGN_BY_BLOCK_KEY) as? ((Measured) -> Int)
    }

private fun lineOffsets(
    children: List<SweetMeasurable>,
    blocks: List<((Measured) -> Int)?>,
    horizontal: Boolean,
): Pair<List<Int>, List<Boolean>> {
    val offsets = MutableList(children.size) { 0 }
    val specified = MutableList(children.size) { false }
    children.forEachIndexed { index, measurable ->
        val block = blocks[index] ?: return@forEachIndexed
        val alignmentSize = measurable.alignmentSize
        val measured =
            object : Measured {
                override val measuredWidth: Int = alignmentSize.width
                override val measuredHeight: Int = alignmentSize.height
                override val parentData: Any? = null

                override fun get(alignmentLine: AlignmentLine): Int = computeBaselineOffset(measurable.control, alignmentSize, alignmentLine)
            }
        val raw = block(measured)
        val maxLine = if (horizontal) alignmentSize.width else alignmentSize.height
        offsets[index] = raw.coerceIn(0, maxLine)
        specified[index] = true
    }
    return offsets to specified
}

private fun alignmentTarget(
    offsets: List<Int>,
    specified: List<Boolean>,
): Int {
    val indices = offsets.indices.filter { specified[it] }
    return if (indices.isNotEmpty()) indices.maxOf { offsets[it] } else 0
}

private const val BASELINE_CACHE_KEY = "__sweet_baselineOffset"

private data class BaselineCache(
    val font: org.eclipse.swt.graphics.Font?,
    val ascent: Int,
)

private fun SweetMeasurable.placeFrom(placement: ChildPlacement) {
    measuredSize = placement.size
    place(placement.x, placement.y)
    val composite = control as? Composite ?: return
    if (placement.contentOffsetX == 0 && placement.contentOffsetY == 0) {
        return
    }
    composite.children.forEach { child ->
        val bounds = child.bounds
        child.bounds =
            Rectangle(
                bounds.x + placement.contentOffsetX,
                bounds.y + placement.contentOffsetY,
                bounds.width,
                bounds.height,
            )
    }
}

/**
 * Best-effort baseline computation for SWT controls.
 *
 * For text-like controls (Label, Text), we derive the baseline from
 * the current font metrics. For other controls, or if metrics are
 * unavailable, we fall back to a simple proportional heuristic.
 */
private fun computeBaselineOffset(
    control: Control,
    size: Size,
    alignmentLine: AlignmentLine,
): Int {
    if (alignmentLine != FirstBaseline && alignmentLine != LastBaseline) {
        return AlignmentLine.Unspecified
    }

    // Try to derive baseline from font metrics for text-bearing controls
    val isTextLike =
        control is org.eclipse.swt.widgets.Label || control is org.eclipse.swt.widgets.Text
    if (isTextLike && !control.isDisposed) {
        val effectiveFont = io.github.ddsimoes.sweet.layout.SweetLayout.measurementFont ?: control.font
        val cached = control.getData(BASELINE_CACHE_KEY) as? BaselineCache
        if (cached != null && cached.font === effectiveFont) {
            return cached.ascent.coerceIn(0, size.height)
        }

        val gc = GC(control)
        try {
            if (io.github.ddsimoes.sweet.layout.SweetLayout.measurementFont != null) {
                gc.font = io.github.ddsimoes.sweet.layout.SweetLayout.measurementFont
            }
            val ascent = gc.fontMetrics.ascent
            control.setData(BASELINE_CACHE_KEY, BaselineCache(effectiveFont, ascent))
            // Clamp ascent into the measured height; for single-line text
            // this is usually a good approximation of the baseline offset.
            return ascent.coerceIn(0, size.height)
        } finally {
            if (!gc.isDisposed) {
                gc.dispose()
            }
        }
    }

    // Fallback heuristic: treat the baseline as near the bottom of the box.
    return (size.height * 3) / 4
}

object BoxDelegate : LayoutDelegate {
    override fun measure(
        children: List<SweetMeasurable>,
        constraints: Constraints,
        spec: LayoutSpec,
    ): MeasureBundle {
        if (children.isEmpty()) return MeasureBundle(emptyList(), Size(constraints.minWidth, constraints.minHeight))
        val boxSpec = spec as? LayoutSpec.Box
        val fill = boxSpec?.fillChildren == true
        val alignment = boxSpec?.contentAlignment ?: Alignment.TopStart
        var maxW = 0
        var maxH = 0
        var sizes =
            children.map { measurable ->
                val s = measurable.measure(constraints.loose())
                maxW = max(maxW, s.width)
                maxH = max(maxH, s.height)
                s
            }
        val parentSize = constraints.constrainSize(maxW, maxH)
        if (fill) {
            // Fill-layout semantics need the final parent size before measuring children exactly.
            // This is the one deliberate second measurement path; placement still never measures.
            sizes = children.map { it.measure(Constraints.fixed(parentSize.width, parentSize.height)) }
        }
        val placements =
            sizes.map { size ->
                val offset = alignment.align(IntSize(size.width, size.height), IntSize(parentSize.width, parentSize.height), LayoutDirection.Ltr)
                ChildPlacement(offset.x, offset.y, size)
            }
        return MeasureBundle(sizes, parentSize, placements)
    }

    override fun place(
        children: List<SweetMeasurable>,
        width: Int,
        height: Int,
        spec: LayoutSpec,
        bundle: MeasureBundle?,
    ) {
        if (children.isEmpty()) return
        children.forEachIndexed { index, measurable ->
            val placement = bundle?.placements?.getOrNull(index) ?: ChildPlacement(0, 0, measurable.measuredSize)
            measurable.placeFrom(placement)
        }
    }
}

object ColumnDelegate : LayoutDelegate {
    override fun measure(
        children: List<SweetMeasurable>,
        constraints: Constraints,
        spec: LayoutSpec,
    ): MeasureBundle {
        if (children.isEmpty()) return MeasureBundle(emptyList(), Size(constraints.minWidth, constraints.minHeight))
        val density = children.first().control.display.getSweetDensity()
        val colSpec = spec as? LayoutSpec.Column
        val arrangement = colSpec?.verticalArrangement ?: Arrangement.Top
        val weights = children.map { it.control.getWeight() ?: 0f }
        val hasWeights = weights.any { it > 0f } && constraints.hasBoundedHeight

        val childSizes: List<Size>
        if (hasWeights) {
            val fixedSizes = MutableList(children.size) { Size.Zero }
            var fixedHeight = 0
            // Track inter-child padding gaps among fixed children.
            // Each gap = paddingBottom[prev] + paddingTop[current].
            // These gaps consume space that the weight allocation must account for.
            var fixedPaddingGap = 0
            var prevPaddingBottom = 0
            var gapTerminated = false
            children.forEachIndexed { index, child ->
                if (weights[index] <= 0f) {
                    val size = child.measure(Constraints(0, constraints.maxWidth, 0, constraints.maxHeight))
                    fixedSizes[index] = size
                    fixedHeight += size.height
                    if (!gapTerminated) {
                        if (index > 0) {
                            fixedPaddingGap += prevPaddingBottom + child.control.chainPaddingTop
                        }
                        prevPaddingBottom = child.control.chainPaddingBottom
                    }
                } else {
                    // First weighted child: add gap from last fixed child and stop tracking
                    if (!gapTerminated && index > 0 && weights.getOrNull(index - 1)?.let { it <= 0f } == true) {
                        fixedPaddingGap += prevPaddingBottom + child.control.chainPaddingTop
                    }
                    gapTerminated = true
                }
            }
            val remaining = (constraints.maxHeight - fixedHeight - spacingFor(arrangement, children.size, density) - fixedPaddingGap).coerceAtLeast(0)
            val totalWeight = weights.filter { it > 0f }.sum()
            var allocatedSoFar = 0
            childSizes =
                children.mapIndexed { index, child ->
                    if (weights[index] > 0f && totalWeight > 0f) {
                        val allocated =
                            if (index == children.lastIndex) {
                                remaining - allocatedSoFar
                            } else {
                                (remaining * weights[index] / totalWeight).toInt()
                            }.coerceAtLeast(0)
                        allocatedSoFar += allocated
                        child.measure(Constraints(0, constraints.maxWidth, allocated, allocated))
                    } else {
                        fixedSizes[index]
                    }
                }
        } else {
            // MPP RowColumnMeasurePolicy semantics: each non-weighted child is measured
            // against the REMAINING main-axis space (incoming max minus space already
            // consumed by previous children plus arrangement spacing), not the parent's
            // full extent. This is what makes fillMaxHeight on a later sibling resolve
            // to "what is left" instead of overflowing the parent.
            val gapPx = arrangementSpacingPx(arrangement, density)
            var fixedSpace = 0
            childSizes =
                children.map { child ->
                    val remaining = (constraints.maxHeight - fixedSpace).coerceAtLeast(0)
                    val size =
                        child.measure(
                            Constraints(
                                0,
                                constraints.maxWidth,
                                0,
                                if (constraints.hasBoundedHeight) remaining else Int.MAX_VALUE,
                            ),
                        )
                    val spaceAfter =
                        if (constraints.hasBoundedHeight) {
                            min(gapPx, (remaining - size.height).coerceAtLeast(0))
                        } else {
                            gapPx
                        }
                    fixedSpace += size.height + spaceAfter
                    size
                }
        }

        val totalH = childSizes.sumOf { it.height } + spacingFor(arrangement, children.size, density)
        val maxW = childSizes.maxOfOrNull { it.width } ?: 0
        val parentSize = constraints.constrainSize(maxW, totalH)
        val ySizes = childSizes.map { it.height }.toIntArray()
        val yPositions = IntArray(ySizes.size)
        (arrangement as Arrangement.Vertical).run {
            density.run {
                arrange(parentSize.height, ySizes, yPositions)
            }
        }
        val blocks = alignmentBlocks(children)
        val placements =
            if (blocks.none { it != null }) {
                val defaultHAlign = colSpec?.horizontalAlignment ?: Alignment.Start
                children.mapIndexed { index, child ->
                    val size = childSizes[index]
                    val layoutData = child.control.getSweetCompositionData().layoutData
                    val hAlign = layoutData.horizontalAlignmentOverride ?: defaultHAlign
                    val x = hAlign.align(IntSize(size.width, 1), IntSize(parentSize.width, 1), LayoutDirection.Ltr).x
                    ChildPlacement(x, yPositions[index], size)
                }
            } else {
                val (offsets, specified) = lineOffsets(children, blocks, horizontal = true)
                val target = alignmentTarget(offsets, specified)
                childSizes.mapIndexed { index, size ->
                    val x =
                        if (specified[index]) {
                            (target - offsets[index]).coerceAtLeast(0)
                        } else {
                            target.coerceAtLeast(0)
                        }
                    ChildPlacement(x, yPositions[index], size, contentOffsetX = x)
                }
            }
        return MeasureBundle(childSizes, parentSize, placements)
    }

    override fun place(
        children: List<SweetMeasurable>,
        width: Int,
        height: Int,
        spec: LayoutSpec,
        bundle: MeasureBundle?,
    ) {
        if (children.isEmpty()) return
        children.forEachIndexed { index, child ->
            val placement = bundle?.placements?.getOrNull(index) ?: ChildPlacement(0, 0, child.measuredSize)
            child.placeFrom(placement)
        }
    }
}

object RowDelegate : LayoutDelegate {
    override fun measure(
        children: List<SweetMeasurable>,
        constraints: Constraints,
        spec: LayoutSpec,
    ): MeasureBundle {
        if (children.isEmpty()) return MeasureBundle(emptyList(), Size(constraints.minWidth, constraints.minHeight))
        val density = children.first().control.display.getSweetDensity()
        val rowSpec = spec as? LayoutSpec.Row
        val arrangement = rowSpec?.horizontalArrangement ?: Arrangement.Start
        val weights = children.map { it.control.getWeight() ?: 0f }
        val hasWeights = weights.any { it > 0f } && constraints.hasBoundedWidth

        val childSizes: List<Size>
        if (hasWeights) {
            val fixedSizes = MutableList(children.size) { Size.Zero }
            var fixedWidth = 0
            var fixedPaddingGap = 0
            var prevPaddingEnd = 0
            var gapTerminated = false
            children.forEachIndexed { index, child ->
                if (weights[index] <= 0f) {
                    val size = child.measure(Constraints(0, constraints.maxWidth, 0, constraints.maxHeight))
                    fixedSizes[index] = size
                    fixedWidth += size.width
                    if (!gapTerminated) {
                        if (index > 0) {
                            fixedPaddingGap += prevPaddingEnd + child.control.chainPaddingStart
                        }
                        prevPaddingEnd = child.control.chainPaddingEnd
                    }
                } else {
                    if (!gapTerminated && index > 0 && weights.getOrNull(index - 1)?.let { it <= 0f } == true) {
                        fixedPaddingGap += prevPaddingEnd + child.control.chainPaddingStart
                    }
                    gapTerminated = true
                }
            }
            val remaining = (constraints.maxWidth - fixedWidth - spacingFor(arrangement, children.size, density) - fixedPaddingGap).coerceAtLeast(0)
            val totalWeight = weights.filter { it > 0f }.sum()
            var allocatedSoFar = 0
            childSizes =
                children.mapIndexed { index, child ->
                    if (weights[index] > 0f && totalWeight > 0f) {
                        val allocated =
                            if (index == children.lastIndex) {
                                remaining - allocatedSoFar
                            } else {
                                (remaining * weights[index] / totalWeight).toInt()
                            }.coerceAtLeast(0)
                        allocatedSoFar += allocated
                        child.measure(Constraints(allocated, allocated, 0, constraints.maxHeight))
                    } else {
                        fixedSizes[index]
                    }
                }
        } else {
            // MPP RowColumnMeasurePolicy semantics: measure against the REMAINING
            // main-axis space (see ColumnDelegate for details).
            val gapPx = arrangementSpacingPx(arrangement, density)
            var fixedSpace = 0
            childSizes =
                children.map { child ->
                    val remaining = (constraints.maxWidth - fixedSpace).coerceAtLeast(0)
                    val size =
                        child.measure(
                            Constraints(
                                0,
                                if (constraints.hasBoundedWidth) remaining else Int.MAX_VALUE,
                                0,
                                constraints.maxHeight,
                            ),
                        )
                    val spaceAfter =
                        if (constraints.hasBoundedWidth) {
                            min(gapPx, (remaining - size.width).coerceAtLeast(0))
                        } else {
                            gapPx
                        }
                    fixedSpace += size.width + spaceAfter
                    size
                }
        }

        val totalW = childSizes.sumOf { it.width } + spacingFor(arrangement, children.size, density)
        val maxH = childSizes.maxOfOrNull { it.height } ?: 0
        val parentSize = constraints.constrainSize(totalW, maxH)
        val xSizes = childSizes.map { it.width }.toIntArray()
        val xPositions = IntArray(xSizes.size)
        (arrangement as Arrangement.Horizontal).run {
            density.run {
                arrange(parentSize.width, xSizes, LayoutDirection.Ltr, xPositions)
            }
        }
        val blocks = alignmentBlocks(children)
        val placements =
            if (blocks.none { it != null }) {
                val defaultVAlign = rowSpec?.verticalAlignment ?: Alignment.CenterVertically
                children.mapIndexed { index, child ->
                    val size = childSizes[index]
                    val layoutData = child.control.getSweetCompositionData().layoutData
                    val vAlign = layoutData.verticalAlignmentOverride ?: defaultVAlign
                    val y = vAlign.align(IntSize(1, size.height), IntSize(1, parentSize.height), LayoutDirection.Ltr).y
                    ChildPlacement(xPositions[index], y, size)
                }
            } else {
                val (offsets, specified) = lineOffsets(children, blocks, horizontal = false)
                val target = alignmentTarget(offsets, specified)
                childSizes.mapIndexed { index, size ->
                    val y =
                        if (specified[index]) {
                            (target - offsets[index]).coerceAtLeast(0)
                        } else {
                            target.coerceAtLeast(0)
                        }
                    ChildPlacement(xPositions[index], y, size, contentOffsetY = y)
                }
            }
        return MeasureBundle(childSizes, parentSize, placements)
    }

    override fun place(
        children: List<SweetMeasurable>,
        width: Int,
        height: Int,
        spec: LayoutSpec,
        bundle: MeasureBundle?,
    ) {
        if (children.isEmpty()) return
        children.forEachIndexed { index, child ->
            val placement = bundle?.placements?.getOrNull(index) ?: ChildPlacement(0, 0, child.measuredSize)
            child.placeFrom(placement)
        }
    }
}
