@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ddsimoes.sweet.compose.locals.LocalSWTNodeApplier
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.DirectSWTNode
import io.github.ddsimoes.sweet.internal.SWTNodeApplier
import io.github.ddsimoes.sweet.internal.applyForeground
import io.github.ddsimoes.sweet.internal.applySWTModifier
import io.github.ddsimoes.sweet.internal.applyTextFont
import io.github.ddsimoes.sweet.internal.createText
import io.github.ddsimoes.sweet.layout.LayoutCoordinator
import org.eclipse.swt.widgets.Label

/**
 * Displays text using an SWT Label.
 *
 * **Sweet note:** `text`, `modifier`, `color`, `fontSize`, `fontStyle`, and `fontWeight`
 * are applied (fonts are derived from the system font; sizes are converted from sp to
 * points). `textAlign` is accepted for API compatibility but is currently ignored.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    if (SweetDebugger.assertionEnabled && textAlign != null) {
        SweetDebugger.log("Text", "Ignored parameters: textAlign")
    }
    val buttonTextCapture = LocalButtonTextCapture.current
    val tabTextCapture = LocalTabTextCapture.current

    if (buttonTextCapture != null) {
        buttonTextCapture.invoke(text)
    } else if (tabTextCapture != null) {
        tabTextCapture.invoke(text)
    } else {
        val applier = LocalSWTNodeApplier.current
        ComposeNode<DirectSWTNode, SWTNodeApplier>(
            factory = { DirectSWTNode(createText(applier)) },
            update = {
                set(text) {
                    val label = (this.control as Label)
                    if (!label.isDisposed) {
                        label.text = it
                        // Only invalidate cached size on recomposition (when the
                        // label already has non-zero bounds). During initial
                        // composition the font isn't set yet, so computeSize()
                        // would return zero and break layout.
                        if (label.bounds.width > 0 || label.bounds.height > 0) {
                            label.requestLayout()
                        }
                        label.parent?.let { parent ->
                            LayoutCoordinator.forDisplay(label.display)
                                .requestLayout(parent)
                        }
                    }
                }
                set(modifier) { applySWTModifier(this.control, it) }
                set(color) {
                    control.applyForeground(it)
                }
                set(Triple(fontSize, fontStyle, fontWeight)) { (size, style, weight) ->
                    val label = this.control as Label
                    if (!label.isDisposed) {
                        label.applyTextFont(size, style, weight)
                        label.parent?.let { parent ->
                            LayoutCoordinator.forDisplay(label.display)
                                .requestLayout(parent)
                        }
                    }
                }
            },
        )
    }
}
