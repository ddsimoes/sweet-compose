package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toPxInt
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.internal.LayoutDataModifier
import kotlin.math.max
import kotlin.math.min

/**
 * Weight modifier for flexible sizing in Row/Column layouts
 */
internal class WeightModifier(
    private val weight: Float,
) : LayoutDataModifier {
    // Weight is last-wins. Capture the value to avoid shadowing by SweetLayoutData.weight.
    override fun applyTo(data: SweetCompositionData): SweetCompositionData {
        val w = weight
        return data.withLayoutData { copy(weight = w) }
    }
}

/**
 * SizeIn modifier for specifying minimum and maximum size constraints.
 *
 * Constraints from multiple `sizeIn` modifiers are **intersected**: the effective minimum is the
 * largest of the mins and the effective maximum is the smallest of the maxes.
 */
internal class SizeInModifier(
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null,
) : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) =
        data.withConstraintsData {
            copy(
                minWidth = maxNullable(minWidth, this@SizeInModifier.minWidth),
                maxWidth = minNullable(maxWidth, this@SizeInModifier.maxWidth),
                minHeight = maxNullable(minHeight, this@SizeInModifier.minHeight),
                maxHeight = minNullable(maxHeight, this@SizeInModifier.maxHeight),
            )
        }
}

/**
 * AspectRatio modifier for maintaining width/height ratio
 */
internal class AspectRatioModifier(
    private val ratio: Float,
) : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) = data.withConstraintsData { copy(aspectRatio = ratio) }
}

/**
 * Offset modifier for positioning adjustments. Offsets are additive across the chain.
 */
internal class OffsetModifier(
    val x: Int,
    val y: Int,
) : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) = data // modeled by OffsetStep
}

/** Intersect helper: largest non-null minimum. */
private fun maxNullable(
    a: Int?,
    b: Int?,
): Int? =
    when {
        a == null -> b
        b == null -> a
        else -> max(a, b)
    }

/** Intersect helper: smallest non-null maximum. */
private fun minNullable(
    a: Int?,
    b: Int?,
): Int? =
    when {
        a == null -> b
        b == null -> a
        else -> min(a, b)
    }

@Stable
fun Modifier.weight(weight: Float): Modifier = this.then(WeightModifier(weight))

@Stable
fun Modifier.sizeIn(
    minWidth: Dp = 0.dp,
    maxWidth: Dp = 10000.dp,
    minHeight: Dp = 0.dp,
    maxHeight: Dp = 10000.dp,
): Modifier =
    this.then(
        SizeInModifier(
            minWidth = if (minWidth > 0.dp) minWidth.toPxInt() else null,
            maxWidth = if (maxWidth != 10000.dp) maxWidth.toPxInt() else null,
            minHeight = if (minHeight > 0.dp) minHeight.toPxInt() else null,
            maxHeight = if (maxHeight != 10000.dp) maxHeight.toPxInt() else null,
        ),
    )

@Stable
fun Modifier.aspectRatio(ratio: Float): Modifier = this.then(AspectRatioModifier(ratio))

@Stable
fun Modifier.offset(
    x: Dp = 0.dp,
    y: Dp = 0.dp,
): Modifier =
    this.then(
        OffsetModifier(x.toPxInt(), y.toPxInt()),
    )
