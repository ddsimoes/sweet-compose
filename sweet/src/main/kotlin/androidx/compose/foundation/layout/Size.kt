package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toPxInt
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.internal.LayoutDataModifier

internal object FillMaxWidthModifier : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) = data // modeled by FillMaxWidthStep
}

internal object FillMaxHeightModifier : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) = data // modeled by FillMaxHeightStep
}

internal class SizeModifier(
    val width: Int,
    val height: Int,
) : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) = data // modeled by SizeStep
}

@Stable
fun Modifier.fillMaxWidth(): Modifier = this.then(FillMaxWidthModifier)

@Stable
fun Modifier.fillMaxHeight(): Modifier = this.then(FillMaxHeightModifier)

@Stable
fun Modifier.fillMaxSize(): Modifier = this.fillMaxWidth().fillMaxHeight()

@Stable
fun Modifier.size(size: Dp): Modifier = this.size(size, size)

@Stable
fun Modifier.size(
    width: Dp,
    height: Dp,
): Modifier = this.then(SizeModifier(width.toPxInt(), height.toPxInt()))

@Stable
fun Modifier.width(width: Dp): Modifier = this.size(width, 0.dp)

@Stable
fun Modifier.height(height: Dp): Modifier = this.size(0.dp, height)
