@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createCard

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val applier = LocalSWTNodeApplier.current
    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createCard(applier)) },
        update = {
            set(modifier) { applySWTModifier(this.control, it) }
        },
        content = content,
    )
}
