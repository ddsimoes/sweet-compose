@file:Suppress("MatchingDeclarationName")

package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toPxInt
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.internal.LayoutDataModifier

internal class PaddingModifier(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) : LayoutDataModifier {
    // Padding is additive: `padding(8).padding(4)` insets by 12 on each affected edge.
    override fun applyTo(data: SweetCompositionData) = data // modeled by PaddingStep
}

@Stable
fun Modifier.padding(all: Dp): Modifier = this.padding(all, all, all, all)

@Stable
fun Modifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
): Modifier = this.padding(horizontal, vertical, horizontal, vertical)

@Stable
fun Modifier.padding(paddingValues: PaddingValues): Modifier =
    this.padding(
        // Sweet's layout pipeline is currently LTR-only; resolve start/end as left/right.
        start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
        top = paddingValues.calculateTopPadding(),
        end = paddingValues.calculateRightPadding(LayoutDirection.Ltr),
        bottom = paddingValues.calculateBottomPadding(),
    )

@Stable
fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): Modifier =
    this.then(
        PaddingModifier(
            start.toPxInt(),
            top.toPxInt(),
            end.toPxInt(),
            bottom.toPxInt(),
        ),
    )
