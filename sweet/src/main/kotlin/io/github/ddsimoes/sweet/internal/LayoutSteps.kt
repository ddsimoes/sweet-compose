package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import io.github.ddsimoes.sweet.layout.Constraints
import kotlin.math.max
import kotlin.math.min

// ── Padding ─────────────────────────────────────────────────────────────────

internal data class PaddingStep(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val horizontal = start + end
        val vertical = top + bottom
        val innerConstraints = constraints.inset(horizontal, vertical)
        val innerSize = inner(innerConstraints)
        val outerWidth = (innerSize.width.toInt() + horizontal).coerceIn(constraints.minWidth, constraints.maxWidth)
        val outerHeight = (innerSize.height.toInt() + vertical).coerceIn(constraints.minHeight, constraints.maxHeight)
        return StepResult(
            outerSize = Size(outerWidth.toFloat(), outerHeight.toFloat()),
            contentOffset = IntOffset(start, top),
        )
    }
}

// ── Size ────────────────────────────────────────────────────────────────────

internal data class SizeStep(
    val width: Int?,
    val height: Int?,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val effectiveConstraints =
            constraints.let { c ->
                var result = c
                if (width != null) {
                    val constrained = width.coerceIn(c.minWidth, c.maxWidth)
                    result = result.copy(minWidth = constrained, maxWidth = constrained)
                }
                if (height != null) {
                    val constrained = height.coerceIn(c.minHeight, c.maxHeight)
                    result = result.copy(minHeight = constrained, maxHeight = constrained)
                }
                result
            }
        val innerSize = inner(effectiveConstraints)
        return StepResult(outerSize = innerSize)
    }
}

// ── FillMax ─────────────────────────────────────────────────────────────────

internal data class FillMaxWidthStep(
    val fraction: Float = 1f,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val fillConstraints =
            if (constraints.hasBoundedWidth) {
                constraints.copy(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth)
            } else {
                constraints
            }
        val innerSize = inner(fillConstraints)
        return StepResult(outerSize = innerSize)
    }
}

internal data class FillMaxHeightStep(
    val fraction: Float = 1f,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val fillConstraints =
            if (constraints.hasBoundedHeight) {
                constraints.copy(minHeight = constraints.maxHeight, maxHeight = constraints.maxHeight)
            } else {
                constraints
            }
        val innerSize = inner(fillConstraints)
        return StepResult(outerSize = innerSize)
    }
}

// ── Offset ──────────────────────────────────────────────────────────────────

internal data class OffsetStep(
    val x: Int,
    val y: Int,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val innerSize = inner(constraints)
        return StepResult(outerSize = innerSize, contentOffset = IntOffset(x, y))
    }
}

// ── SizeIn ───────────────────────────────────────────────────────────────────

internal data class SizeInStep(
    val minWidth: Int?,
    val maxWidth: Int?,
    val minHeight: Int?,
    val maxHeight: Int?,
) : LayoutStep {
    override fun measureWrapped(
        constraints: Constraints,
        inner: (Constraints) -> Size,
    ): StepResult {
        val effectiveConstraints = constraints.let { c ->
            var result = c
            minWidth?.let { mw -> result = result.copy(minWidth = max(result.minWidth, mw)) }
            maxWidth?.let { mw -> result = result.copy(maxWidth = min(result.maxWidth, mw)) }
            minHeight?.let { mh -> result = result.copy(minHeight = max(result.minHeight, mh)) }
            maxHeight?.let { mh -> result = result.copy(maxHeight = min(result.maxHeight, mh)) }
            result
        }
        val innerSize = inner(effectiveConstraints)
        return StepResult(outerSize = innerSize)
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun Constraints.inset(
    horizontal: Int,
    vertical: Int,
): Constraints {
    val dh = horizontal.coerceAtLeast(0)
    val dv = vertical.coerceAtLeast(0)
    // Preserve Int.MAX_VALUE (the sentinel for unbounded) when insetting,
    // so downstream steps like fillMaxWidth/Height see a truly unbounded axis.
    val newMinW = if (minWidth != Int.MAX_VALUE) (minWidth - dh).coerceAtLeast(0) else Int.MAX_VALUE
    val newMaxW = if (maxWidth != Int.MAX_VALUE) (maxWidth - dh).coerceAtLeast(0) else Int.MAX_VALUE
    val newMinH = if (minHeight != Int.MAX_VALUE) (minHeight - dv).coerceAtLeast(0) else Int.MAX_VALUE
    val newMaxH = if (maxHeight != Int.MAX_VALUE) (maxHeight - dv).coerceAtLeast(0) else Int.MAX_VALUE
    return Constraints(
        minWidth = newMinW,
        maxWidth = newMaxW,
        minHeight = newMinH,
        maxHeight = newMaxH,
    )
}
