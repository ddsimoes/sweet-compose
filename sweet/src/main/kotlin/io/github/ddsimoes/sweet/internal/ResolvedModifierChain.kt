package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import io.github.ddsimoes.sweet.data.getSweetCompositionData
import io.github.ddsimoes.sweet.data.offsetX as flatOffsetX
import io.github.ddsimoes.sweet.data.offsetY as flatOffsetY
import io.github.ddsimoes.sweet.data.paddingStart as flatPaddingStart
import io.github.ddsimoes.sweet.data.paddingTop as flatPaddingTop
import io.github.ddsimoes.sweet.data.paddingEnd as flatPaddingEnd
import io.github.ddsimoes.sweet.data.paddingBottom as flatPaddingBottom
import io.github.ddsimoes.sweet.data.sizeWidth as flatSizeWidth
import io.github.ddsimoes.sweet.data.sizeHeight as flatSizeHeight
import io.github.ddsimoes.sweet.layout.Constraints
import org.eclipse.swt.widgets.Control

/**
 * An ordered chain of layout modifier steps, built once per `applySWTModifier`.
 *
 * Compose modifiers are an ordered chain: `padding(10).size(50)` means the padding
 * wraps the size (outer = 70, inner = 50), while `size(50).padding(10)` means the
 * size wraps the padding (outer = 50, inner = 30). Sweet's previous flat bag lost this.
 *
 * sizes flow up (inside-out). The accumulated content offsets feed placement.
 */
internal data class ResolvedModifierChain(
    /** Layout-wrapper steps in outside-in order (index 0 = outermost). */
    val layoutSteps: List<LayoutStep>,
) {
    // Accumulated placement values from all steps. Computed eagerly in a single traversal
    // (a chain instance is rebuilt on every applySWTModifier, so per-property lazy caches
    // would still re-traverse once each per recomposition — six times instead of one).
    // These replace the old flat SweetLayoutData/SweetSpacingData fields for placement.

    /** Total x-offset from all [OffsetStep]s. */
    val totalOffsetX: Int
    /** Total y-offset from all [OffsetStep]s. */
    val totalOffsetY: Int
    /** Total start padding from all [PaddingStep]s. */
    val totalPaddingStart: Int
    /** Total top padding from all [PaddingStep]s. */
    val totalPaddingTop: Int
    /** Total end padding from all [PaddingStep]s. */
    val totalPaddingEnd: Int
    /** Total bottom padding from all [PaddingStep]s. */
    val totalPaddingBottom: Int

    /**
     * Effective fixed width/height from the outermost [SizeStep] that sets the axis.
     * The outermost step wins, matching chain measurement semantics (an outer fixed
     * constraint bounds everything inside it).
     */
    val sizeWidth: Int?
    val sizeHeight: Int?

    init {
        var offsetX = 0
        var offsetY = 0
        var padStart = 0
        var padTop = 0
        var padEnd = 0
        var padBottom = 0
        var width: Int? = null
        var height: Int? = null
        // layoutSteps is outside-in (index 0 = outermost), so the first SizeStep
        // seen for an axis is the outermost and wins.
        for (step in layoutSteps) {
            when (step) {
                is OffsetStep -> {
                    offsetX += step.x
                    offsetY += step.y
                }
                is PaddingStep -> {
                    padStart += step.start
                    padTop += step.top
                    padEnd += step.end
                    padBottom += step.bottom
                }
                is SizeStep -> {
                    if (width == null) width = step.width
                    if (height == null) height = step.height
                }
                else -> {}
            }
        }
        totalOffsetX = offsetX
        totalOffsetY = offsetY
        totalPaddingStart = padStart
        totalPaddingTop = padTop
        totalPaddingEnd = padEnd
        totalPaddingBottom = padBottom
        sizeWidth = width
        sizeHeight = height
    }
}

/**
 * A single modifier step in the ordered chain.
 * Implementations model individual Compose layout modifiers.
 */
internal sealed interface LayoutStep {
    fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult
}

data class StepResult(
    val outerSize: Size,
    val contentOffset: IntOffset = IntOffset.Zero,
)

// ── Chain-first Control extensions for placement ──────────────────────────────
// These read from the ordered modifier chain when present, falling back to legacy
// flat fields for controls built outside compose. Replace direct reads of the flat
// extension properties (control.paddingTop, etc.) with these everywhere except
// measureFlat / applyModifiersToConstraints (which will be deleted).

internal val Control.chainOffsetX: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalOffsetX ?: flatOffsetX
    }

internal val Control.chainOffsetY: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalOffsetY ?: flatOffsetY
    }

internal val Control.chainPaddingStart: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalPaddingStart ?: flatPaddingStart
    }

internal val Control.chainPaddingTop: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalPaddingTop ?: flatPaddingTop
    }

internal val Control.chainPaddingEnd: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalPaddingEnd ?: flatPaddingEnd
    }

internal val Control.chainPaddingBottom: Int
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return chain?.totalPaddingBottom ?: flatPaddingBottom
    }

internal val Control.chainSizeWidth: Int?
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return if (chain != null) chain.sizeWidth else flatSizeWidth
    }

internal val Control.chainSizeHeight: Int?
    get() {
        val chain = getSweetCompositionData()?.modifierChain as? ResolvedModifierChain
        return if (chain != null) chain.sizeHeight else flatSizeHeight
    }
