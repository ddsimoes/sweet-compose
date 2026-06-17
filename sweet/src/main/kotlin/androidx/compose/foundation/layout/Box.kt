package androidx.compose.foundation.layout

import androidx.compose.foundation.HorizontalScrollConfig
import androidx.compose.foundation.VerticalScrollConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.data.SweetCompositionData
import io.github.ddsimoes.sweet.data.updateSweetCompositionData
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.LayoutDataModifier
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.ScrollContainer
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.createSpacer
import io.github.ddsimoes.sweet.internal.createSweetBox
import io.github.ddsimoes.sweet.layout.LayoutSpec

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val applier = LocalSWTNodeApplier.current

    // Detect and strip scroll modifiers so we can wrap with a scroll viewport
    var vScrollConfig: VerticalScrollConfig? = null
    var hScrollConfig: HorizontalScrollConfig? = null
    var strippedModifier: Modifier = Modifier
    modifier.foldIn(Unit) { _, element ->
        when (element) {
            is VerticalScrollConfig -> vScrollConfig = element
            is HorizontalScrollConfig -> hScrollConfig = element
            else -> strippedModifier = strippedModifier.then(element)
        }
    }

    if (vScrollConfig != null || hScrollConfig != null) {
        ScrollContainer(
            verticalConfig = vScrollConfig,
            horizontalConfig = hScrollConfig,
            strippedModifier = strippedModifier,
            debugLabel = "Box",
        ) {
            // Inner Box holds the scrollable content; fill the viewport on non-scrolling axes.
            val fillModifier =
                when {
                    vScrollConfig != null && hScrollConfig != null -> Modifier
                    vScrollConfig != null -> Modifier.fillMaxWidth()
                    else -> Modifier.fillMaxHeight()
                }
            Box(
                modifier = fillModifier,
                contentAlignment = contentAlignment,
                propagateMinConstraints = propagateMinConstraints,
                content = content,
            )
        }
        return
    }

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createSweetBox(applier, contentAlignment, propagateMinConstraints)) },
        update = {
            set(modifier) { applySWTModifier(this.control, it) }
            set(contentAlignment to propagateMinConstraints) { (alignment, propagateMin) ->
                val composite = this.control as org.eclipse.swt.widgets.Composite
                composite.updateSweetCompositionData {
                    withLayoutSpec(LayoutSpec.Box(alignment, fillChildren = propagateMin))
                }
            }
        },
        content = { BoxScopeInstance.content() },
    )
}

// Currently implemented as Label with empty text — a real native widget per Spacer.
// Prime candidate for container collapsing (doc 14).
@Composable
fun Spacer(modifier: Modifier = Modifier) {
    val applier = LocalSWTNodeApplier.current

    ComposeNode<DirectSWTNode, SWTNodeApplier>(
        factory = { DirectSWTNode(createSpacer(applier)) },
        update = {
            set(modifier) { applySWTModifier(this.control, it) }
        },
    )
}

/** Scope for the children of [Box]. */
@LayoutScopeMarker
@Immutable
interface BoxScope {
    /**
     * Align the child element within the [Box]. This alignment will have priority over
     * the [Box]'s `contentAlignment` parameter.
     */
    @Stable
    fun Modifier.align(alignment: Alignment): Modifier
}

internal object BoxScopeInstance : BoxScope {
    @Stable
    override fun Modifier.align(alignment: Alignment): Modifier =
        this.then(BoxAlignElement(alignment))
}

internal class BoxAlignElement(
    val alignment: Alignment,
) : LayoutDataModifier {
    override fun applyTo(data: SweetCompositionData) =
        data.withLayoutData { copy(boxAlignment = alignment) }
}
