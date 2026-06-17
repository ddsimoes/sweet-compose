package io.github.ddsimoes.sweet.layout

import androidx.compose.foundation.layout.ALIGN_BY_BLOCK_KEY
import androidx.compose.ui.Alignment
import io.github.ddsimoes.sweet.data.aspectRatio
import io.github.ddsimoes.sweet.data.fillMaxHeight
import io.github.ddsimoes.sweet.data.fillMaxWidth
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.data.maxHeight
import io.github.ddsimoes.sweet.data.maxWidth
import io.github.ddsimoes.sweet.data.minHeight
import io.github.ddsimoes.sweet.data.minWidth
import io.github.ddsimoes.sweet.data.offsetX
import io.github.ddsimoes.sweet.data.offsetY
import io.github.ddsimoes.sweet.data.paddingBottom
import io.github.ddsimoes.sweet.data.paddingEnd
import io.github.ddsimoes.sweet.data.paddingStart
import io.github.ddsimoes.sweet.data.paddingTop
import io.github.ddsimoes.sweet.data.sizeHeight
import io.github.ddsimoes.sweet.data.sizeWidth
import io.github.ddsimoes.sweet.data.sweetLayoutSpec
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.LayoutStep
import io.github.ddsimoes.sweet.internal.NodeMeasurable
import io.github.ddsimoes.sweet.internal.ResolvedModifierChain
import io.github.ddsimoes.sweet.internal.chainSizeHeight
import io.github.ddsimoes.sweet.internal.chainSizeWidth
import io.github.ddsimoes.sweet.internal.SweetLayoutNode
import io.github.ddsimoes.sweet.layout.delegate.LayoutDelegateRegistry
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Layout
import kotlin.math.max
import kotlin.math.min

/**
 * Constraints similar to Jetpack Compose, defining min/max width and height
 */
data class Constraints(
    val minWidth: Int = 0,
    val maxWidth: Int = Int.MAX_VALUE,
    val minHeight: Int = 0,
    val maxHeight: Int = Int.MAX_VALUE,
) {
    val hasBoundedWidth: Boolean get() = maxWidth != Int.MAX_VALUE
    val hasBoundedHeight: Boolean get() = maxHeight != Int.MAX_VALUE
    val hasFixedWidth: Boolean get() = minWidth == maxWidth
    val hasFixedHeight: Boolean get() = minHeight == maxHeight

    fun copyWith(
        minWidth: Int = this.minWidth,
        maxWidth: Int = this.maxWidth,
        minHeight: Int = this.minHeight,
        maxHeight: Int = this.maxHeight,
    ) = Constraints(minWidth, maxWidth, minHeight, maxHeight)

    fun constrain(size: Point): Point =
        Point(
            size.x.coerceIn(minWidth, maxOf(minWidth, maxWidth)),
            size.y.coerceIn(minHeight, maxOf(minHeight, maxHeight)),
        )

    // When constraints are contradictory (min > max, e.g. from `sizeIn(min = 200, max = 100)`),
    // the minimum wins. This mirrors Compose, which never produces a max below its min, and keeps
    // `coerceIn` from throwing on an empty range.
    fun constrainWidth(width: Int): Int = width.coerceIn(minWidth, maxOf(minWidth, maxWidth))

    fun constrainHeight(height: Int): Int = height.coerceIn(minHeight, maxOf(minHeight, maxHeight))

    companion object {
        val Infinity = Constraints(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)

        fun fixed(
            width: Int,
            height: Int,
        ) = Constraints(width, width, height, height)
    }
}

/**
 * Represents a measured size with width and height
 */
data class Size(
    val width: Int,
    val height: Int,
) {
    companion object {
        val Zero = Size(0, 0)
        val Unspecified = Size(-1, -1)
    }
}

/**
 * Interface for intrinsic measurements, similar to Compose's Measurable
 */
interface IntrinsicMeasurable {
    fun minIntrinsicWidth(height: Int): Int

    fun maxIntrinsicWidth(height: Int): Int

    fun minIntrinsicHeight(width: Int): Int

    fun maxIntrinsicHeight(width: Int): Int
}

/**
 * Interface for components that can be measured with constraints
 */
interface Measurable : IntrinsicMeasurable {
    fun measure(constraints: Constraints): Size
}

/**
 * Interface for components that can be placed after measurement
 */
interface Placeable {
    var measuredSize: Size

    fun place(
        x: Int,
        y: Int,
    )
}

/**
 * Combined interface for SWT controls that support Sweet layout system
 */
interface SweetMeasurable :
    Measurable,
    Placeable {
    val control: Control
    val alignmentSize: Size
        get() = measuredSize
    override var measuredSize: Size

    override fun place(
        x: Int,
        y: Int,
    ) {
        control.bounds = Rectangle(x, y, measuredSize.width, measuredSize.height)
    }
}

/**
 * Default implementation of SweetMeasurable for SWT controls
 */
open class SWTControlMeasurable(
    override val control: Control,
) : SweetMeasurable {
    override var measuredSize: Size = Size.Zero
    override var alignmentSize: Size = Size.Zero

    /**
     * Temporarily swaps to [SweetLayout.measurementFont] during [block], restoring
     * the original font afterwards. No-op when no measurement font is configured.
     */
    private inline fun <T> withMeasurementFont(block: () -> T): T {
        val measurementFont = SweetLayout.measurementFont ?: return block()
        val originalFont = control.font
        control.font = measurementFont
        try {
            return block()
        } finally {
            control.font = originalFont
        }
    }

    override fun measure(constraints: Constraints): Size =
        withMeasurementFont {
            if (SweetLayout.measureCountEnabled) {
                val id = control.hashCode().toString(16)
                SweetLayout.measureCounts.merge("$id.measure", 1, Int::plus)
            }
            // When an ordered modifier chain is present (doc 12), apply steps in order
            // instead of the flat modifier pipeline.
            val chain = control.getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
            if (chain != null && chain.layoutSteps.isNotEmpty()) {
                return@withMeasurementFont measureWithOrderedChain(chain, constraints)
            }
            measureFlat(constraints)
        }

    /**
     * Legacy flat modifier pipeline: apply fillMax/size/sizeIn → subtract padding →
     * measure content → add padding back → constrain. Used when no ordered chain is present.
     */
    private fun measureFlat(constraints: Constraints): Size {
        val outerConstraints = applyModifiersToConstraints(constraints)
        val paddingStart = control.paddingStart
        val paddingTop = control.paddingTop
        val paddingEnd = control.paddingEnd
        val paddingBottom = control.paddingBottom
        val padW = paddingStart + paddingEnd
        val padH = paddingTop + paddingBottom
        val contentConstraints =
            outerConstraints.copyWith(
                minWidth = (outerConstraints.minWidth - padW).coerceAtLeast(0),
                maxWidth =
                    if (outerConstraints.hasBoundedWidth) {
                        (outerConstraints.maxWidth - padW).coerceAtLeast(0)
                    } else {
                        outerConstraints.maxWidth
                    },
                minHeight = (outerConstraints.minHeight - padH).coerceAtLeast(0),
                maxHeight =
                    if (outerConstraints.hasBoundedHeight) {
                        (outerConstraints.maxHeight - padH).coerceAtLeast(0)
                    } else {
                        outerConstraints.maxHeight
                    },
            )
        val preferredContentSize = getPreferredSizeWithModifiers(contentConstraints)
        val constrainedContentSize = contentConstraints.constrain(preferredContentSize)
        val constrainedOuterSize =
            outerConstraints.constrain(
                Point(
                    constrainedContentSize.x + padW,
                    constrainedContentSize.y + padH,
                ),
            )
        measuredSize = Size(constrainedOuterSize.x, constrainedOuterSize.y)
        alignmentSize = computeAlignmentSize(padW, padH)
        return measuredSize
    }

    /**
     * Ordered chain measurement (doc 12): walk [LayoutStep]s outside-in, calling
     * [getPreferredSizeWithModifiers] at the leaf to get the natural content size.
     */
    private fun measureWithOrderedChain(
        chain: ResolvedModifierChain,
        constraints: Constraints,
    ): Size {
        val outer = applySteps(chain.layoutSteps, 0, constraints)
        val size = Size(outer.width.toInt(), outer.height.toInt())
        measuredSize = size
        // alignmentSize is a niche alignBy feature; use measuredSize as a safe default.
        alignmentSize = size
        return size
    }

    /**
     * Recursively applies chain steps. At the leaf (no more steps), measures the
     * natural content via [getPreferredSizeWithModifiers] and returns its size as a
     * [androidx.compose.ui.geometry.Size].
     */
    private fun applySteps(
        steps: List<LayoutStep>,
        index: Int,
        constraints: Constraints,
    ): androidx.compose.ui.geometry.Size {
        if (index >= steps.size) {
            val coreSize = getPreferredSizeWithModifiers(constraints)
            return androidx.compose.ui.geometry.Size(coreSize.x.toFloat(), coreSize.y.toFloat())
        }
        return steps[index].measureWrapped(constraints) { innerConstraints ->
            applySteps(steps, index + 1, innerConstraints)
        }.outerSize
    }

    private fun computeAlignmentSize(
        padW: Int,
        padH: Int,
    ): Size {
        if (control !is Composite || control.getData(ALIGN_BY_BLOCK_KEY) == null) {
            return measuredSize
        }

        val naturalContentSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        return Size(
            min(measuredSize.width, naturalContentSize.x + padW),
            min(measuredSize.height, naturalContentSize.y + padH),
        )
    }

    private fun applyModifiersToConstraints(constraints: Constraints): Constraints {
        var modifiedConstraints = constraints

        // Apply size modifiers using Sweet data
        control.sizeWidth?.let { width ->
            modifiedConstraints = modifiedConstraints.copyWith(minWidth = width, maxWidth = width)
        }
        control.sizeHeight?.let { height ->
            modifiedConstraints = modifiedConstraints.copyWith(minHeight = height, maxHeight = height)
        }

        // Apply fillMax modifiers using Sweet data
        if (control.fillMaxWidth && constraints.hasBoundedWidth) {
            modifiedConstraints = modifiedConstraints.copyWith(minWidth = constraints.maxWidth)
        }
        if (control.fillMaxHeight && constraints.hasBoundedHeight) {
            modifiedConstraints = modifiedConstraints.copyWith(minHeight = constraints.maxHeight)
        }

        // Apply sizeIn constraints using Sweet data
        control.minWidth?.let { minWidth ->
            modifiedConstraints = modifiedConstraints.copyWith(minWidth = max(modifiedConstraints.minWidth, minWidth))
        }
        control.maxWidth?.let { maxWidth ->
            modifiedConstraints = modifiedConstraints.copyWith(maxWidth = min(modifiedConstraints.maxWidth, maxWidth))
        }
        control.minHeight?.let { minHeight ->
            modifiedConstraints =
                modifiedConstraints.copyWith(minHeight = max(modifiedConstraints.minHeight, minHeight))
        }
        control.maxHeight?.let { maxHeight ->
            modifiedConstraints =
                modifiedConstraints.copyWith(maxHeight = min(modifiedConstraints.maxHeight, maxHeight))
        }

        return modifiedConstraints
    }

    /**
     * Computes the raw natural CONTENT size of this control against the given
     * (already padding-subtracted) constraints, before checkbox/aspect adjustments.
     *
     * Default implementation queries SWT's `computeSize`. Subclasses that own their
     * own measurement (e.g. the shadow-tree [NodeMeasurable], which measures container
     * children via a layout delegate) override this to bypass SWT entirely.
     */
    protected open fun measureNaturalContent(constraints: Constraints): Point {
        return control.computeSize(
            constraints.toWidthHint(),
            constraints.toHeightHint(),
        )
    }

    private fun getPreferredSizeWithModifiers(constraints: Constraints): Point {
        // Start with control's natural preferred size.
        var naturalSize = measureNaturalContent(constraints)

        // Special-case: Checkbox often needs square sizing; ensure a reasonable square
        val buttonStyle = (control as? org.eclipse.swt.widgets.Button)?.style
        if (buttonStyle != null && (buttonStyle and SWT.CHECK) != 0) {
            val side = max(16, max(naturalSize.x, naturalSize.y))
            naturalSize = Point(side, side)
        }

        // Apply aspect ratio if specified using Sweet data
        control.aspectRatio?.let { ratio ->
            return when {
                constraints.hasFixedWidth -> {
                    val height = (constraints.maxWidth / ratio).toInt()
                    Point(constraints.maxWidth, height)
                }

                constraints.hasFixedHeight -> {
                    val width = (constraints.maxHeight * ratio).toInt()
                    Point(width, constraints.maxHeight)
                }

                else -> {
                    // Use natural width and calculate height
                    val height = (naturalSize.x / ratio).toInt()
                    Point(naturalSize.x, height)
                }
            }
        }

        return naturalSize
    }

    private fun Constraints.toWidthHint(): Int =
        if (hasFixedWidth && maxWidth != Int.MAX_VALUE) {
            maxWidth
        } else {
            SWT.DEFAULT
        }

    private fun Constraints.toHeightHint(): Int =
        if (hasFixedHeight && maxHeight != Int.MAX_VALUE) {
            maxHeight
        } else {
            SWT.DEFAULT
        }


    override fun place(
        x: Int,
        y: Int,
    ) {
        // Read placement values from the ordered modifier chain when present (doc 12),
        // falling back to legacy flat fields for controls that predate chain building.
        val chain = control.getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        val offsetX: Int
        val offsetY: Int
        val padStart: Int
        val padTop: Int
        val padEnd: Int
        val padBottom: Int
        if (chain != null) {
            offsetX = chain.totalOffsetX
            offsetY = chain.totalOffsetY
            padStart = chain.totalPaddingStart
            padTop = chain.totalPaddingTop
            padEnd = chain.totalPaddingEnd
            padBottom = chain.totalPaddingBottom
        } else {
            // Legacy flat-field fallback — kept for controls built outside compose.
            offsetX = control.offsetX
            offsetY = control.offsetY
            padStart = control.paddingStart
            padTop = control.paddingTop
            padEnd = control.paddingEnd
            padBottom = control.paddingBottom
        }

        val finalX = x + offsetX
        val finalY = y + offsetY

        val contentWidth = (measuredSize.width - padStart - padEnd).coerceAtLeast(0)
        val contentHeight = (measuredSize.height - padTop - padBottom).coerceAtLeast(0)

        control.bounds =
            Rectangle(
                finalX + padStart,
                finalY + padTop,
                contentWidth,
                contentHeight,
            )
    }
    // Intrinsics read the fixed size chain-first (with flat fallback) like every other
    // chain-aware path; min/max bounds come from sizeIn, which stays in the flat bag.
    override fun minIntrinsicWidth(height: Int): Int =
        withMeasurementFont {
            val naturalWidth = control.computeSize(SWT.DEFAULT, height).x
            val sizeWidth = control.chainSizeWidth
            val minWidth = control.minWidth
            when {
                sizeWidth != null -> sizeWidth
                minWidth != null -> max(naturalWidth, minWidth)
                else -> naturalWidth
            }
        }

    override fun maxIntrinsicWidth(height: Int): Int =
        withMeasurementFont {
            val naturalWidth = control.computeSize(SWT.DEFAULT, height).x
            val sizeWidth = control.chainSizeWidth
            val maxWidth = control.maxWidth
            when {
                sizeWidth != null -> sizeWidth
                maxWidth != null -> min(naturalWidth, maxWidth)
                else -> naturalWidth
            }
        }

    override fun minIntrinsicHeight(width: Int): Int =
        withMeasurementFont {
            val naturalHeight = control.computeSize(width, SWT.DEFAULT).y
            val sizeHeight = control.chainSizeHeight
            val minHeight = control.minHeight
            when {
                sizeHeight != null -> sizeHeight
                minHeight != null -> max(naturalHeight, minHeight)
                else -> naturalHeight
            }
        }

    override fun maxIntrinsicHeight(width: Int): Int =
        withMeasurementFont {
            val naturalHeight = control.computeSize(width, SWT.DEFAULT).y
            val sizeHeight = control.chainSizeHeight
            val maxHeight = control.maxHeight
            when {
                sizeHeight != null -> sizeHeight
                maxHeight != null -> min(naturalHeight, maxHeight)
                else -> naturalHeight
            }
        }
}

/**
 * Unified SweetLayout that delegates measurement and placement based on the
 * parent's LayoutSpec stored in SweetCompositionData.
 */
class SweetLayout : Layout() {
    // NOTE: The measure-count instrumentation below backs LayoutContractTest's Phase 4 guard
    // against placement-time remeasurement. It is default-off outside tests.
    companion object {
        /** Enable counting for testing. */
        @Volatile var measureCountEnabled: Boolean = false
        val measureCounts: MutableMap<String, Int> = LinkedHashMap()

        /** Reset counters (call before each test). */
        fun resetMeasureCounts() {
            measureCounts.clear()
        }

        /**
         * A pinned font used for layout measurement, independent of the host system's default font.
         *
         * When set, all [SweetMeasurable.measure] calls temporarily swap this font onto the control
         * before calling [Control.computeSize], and restore the original font after. The control's
         * visible font is unchanged — only layout measurement uses the pinned font.
         *
         * When `null` (default), the control's current font is used for measurement as before.
         */
        @Volatile var measurementFont: org.eclipse.swt.graphics.Font? = null
    }

    /** Optional identifier set by tests for per-node counting. */
    var countId: String? = null
    private val measurables = mutableListOf<SweetMeasurable>()

    override fun computeSize(
        composite: Composite,
        wHint: Int,
        hHint: Int,
        flushCache: Boolean,
    ): Point {
        if (measureCountEnabled) {
            val id = countId ?: composite.hashCode().toString(16)
            measureCounts.merge("$id.computeSize", 1, Int::plus)
        }
        val constraints = createConstraints(wHint, hHint)
        val node = composite.getData("sweet.m1.layoutNode") as? SweetLayoutNode
        if (node != null) {
            val size = NodeMeasurable(node, isRoot = true).measure(constraints)
            if (SweetDebugger.assertionEnabled && composite.parent is io.github.ddsimoes.sweet.widgets.ScrollViewport) {
                SweetDebugger.log(
                    "SweetLayout",
                    "computeSize for scroll content: hints=($wHint,$hHint) constraints=$constraints result=$size children=${node.children.size}",
                )
            }
            return Point(size.width, size.height)
        }

        updateMeasurables(composite)
        val spec = composite.sweetLayoutSpec
        val delegate = LayoutDelegateRegistry.forSpec(spec)
        val bundle = delegate.measure(measurables, constraints, spec ?: LayoutSpec.Box(Alignment.TopStart))
        if (SweetDebugger.assertionEnabled && composite.parent is io.github.ddsimoes.sweet.widgets.ScrollViewport) {
            SweetDebugger.log(
                "SweetLayout",
                "computeSize for scroll content: hints=($wHint,$hHint) constraints=$constraints result=${bundle.parentSize} children=${measurables.size}",
            )
        }
        return Point(bundle.parentSize.width, bundle.parentSize.height)
    }

    override fun layout(
        composite: Composite,
        flushCache: Boolean,
    ) {
        // When the M1 shadow pass is active, a single pass drives the whole contiguous
        // Sweet subtree. The pass root is the *topmost shadow-managed* Sweet composite — one
        // whose SWT parent is not itself a shadow-managed SweetLayout composite (it sits under
        // an embedCompose+FillLayout boundary, a foreign GridLayout island, or a window Shell).
        // Interior Sweet composites are inert: the ancestor pass already measured and placed
        // them, and re-driving here would re-apply their own padding (double count).
        //
        // A SweetLayout composite that is NOT in any shadow tree (e.g. the window Shell, which
        // hosts the boundary composite) has no node here — it falls through to the legacy path
        val node = composite.getData("sweet.m1.layoutNode") as? io.github.ddsimoes.sweet.internal.SweetLayoutNode
        if (node == null) {
            // No shadow node (e.g. window Shell): position the single boundary child that
            // will itself be shadow-managed. Without this, the Shell's initial size won't
            // propagate to the Compose content before the first screenshot/test assertion.
            val client = composite.clientArea
            if (client.width <= 0 || client.height <= 0) return
            composite.children.forEach { child ->
                child.setBounds(client.x, client.y, client.width, client.height)
            }
            return
        }

        val parent = composite.parent
        val parentIsShadowSweet =
            parent != null &&
                parent.layout is SweetLayout &&
                parent.getData("sweet.m1.layoutNode") != null
        if (parentIsShadowSweet) {
            // Interior: escalate to the driving ancestor.
            var driver: Composite = composite
            var p = composite.parent
            while (p != null && p.layout is SweetLayout && p.getData("sweet.m1.layoutNode") != null) {
                driver = p
                p = p.parent
            }
            val driverNode = driver.getData("sweet.m1.layoutNode") as? io.github.ddsimoes.sweet.internal.SweetLayoutNode
            if (driverNode != null && driverNode.inPass) return
            if (!driver.isDisposed && !escalateThroughScrollViewport(driver)) driver.layout(true)
            return
        }
        // Content of a scroll viewport: the viewport owns the content's extent (it measures
        // the scroll axis unbounded), so invalidations must escalate to the viewport, which
        // re-measures, resizes the content, and re-drives this layout.
        if (escalateThroughScrollViewport(composite)) return
        val client = composite.clientArea
        if (client.width > 0 && client.height > 0) {
            io.github.ddsimoes.sweet.internal.layoutPass(node, io.github.ddsimoes.sweet.layout.Constraints.fixed(client.width, client.height))
        }
    }

    /**
     * If [composite] is the content of a [io.github.ddsimoes.sweet.widgets.ScrollViewport]
     * and the viewport is not already laying out, escalates the invalidation to the
     * viewport and returns true. The viewport pass re-measures the content (scroll axis
     * unbounded), resizes it, and triggers this layout again with fresh geometry.
     */
    private fun escalateThroughScrollViewport(composite: Composite): Boolean {
        val viewport = composite.parent as? io.github.ddsimoes.sweet.widgets.ScrollViewport ?: return false
        if (viewport.isLayingOut || viewport.isDisposed) return false
        viewport.layout(true)
        return true
    }

    private fun updateMeasurables(composite: Composite) {
        measurables.clear()
        composite.children.forEach { control ->
            measurables.add(SWTControlMeasurable(control))
        }
    }

    private fun createConstraints(
        wHint: Int,
        hHint: Int,
    ): Constraints {
        val minWidth = if (wHint == -1) 0 else wHint
        val maxWidth = if (wHint == -1) Int.MAX_VALUE else wHint
        val minHeight = if (hHint == -1) 0 else hHint
        val maxHeight = if (hHint == -1) Int.MAX_VALUE else hHint
        return Constraints(minWidth, maxWidth, minHeight, maxHeight)
    }
}
