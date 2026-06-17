@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applyBackground
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createDivider

/**
 * A horizontal divider line.
 *
 * Backed by an SWT Label styled as a thin line.
 */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    color: Color = Color.Gray,
) {
    val applier = LocalSWTNodeApplier.current
    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createDivider(applier)) },
        update = {
            // Divider should fill available width and respect thickness by default
            set(modifier) { applySWTModifier(this.control, it.fillMaxWidth().height(thickness)) }
            set(color) {
                control.applyBackground(it)
            }
        },
    )
}
