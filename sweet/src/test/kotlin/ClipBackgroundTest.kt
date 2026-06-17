import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Regression test for the SWT/GTK Region quirk: when a Region is set on a
 * Composite, anything the parent paints (background color or PaintListener
 * output) renders OVER its child widgets, hiding them (reproduced with pure
 * SWT). Sweet therefore must not install a Region on a clipped composite
 * that paints — it clips the painted content at draw time instead — and may
 * only use the Region when the composite paints nothing itself.
 */
class ClipBackgroundTest {
    @Test
    fun clippedPaintingCompositeUsesDrawTimeClipNotRegion() {
        autoSWT {
            testShell(width = 400, height = 120) {
                layout = FillLayout()
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TestClipContent()
                }
            }.test { shell ->
                val clipBgLabel = shell.findAll<Label> { it.text == "ClipBg" }.first()
                val clipOnlyLabel = shell.findAll<Label> { it.text == "ClipOnly" }.first()

                runOnSWT {
                    // clip + background paints → Region must NOT be set,
                    // otherwise the child label would be hidden by the
                    // parent's painting (GTK quirk).
                    assertNull(
                        clipBgLabel.parent.region,
                        "Clipped composite that paints a background must not use a Region",
                    )
                    // clip without any parent painting → Region clipping is
                    // safe and should be used.
                    assertNotNull(
                        clipOnlyLabel.parent.region,
                        "Clipped composite that paints nothing should use a Region",
                    )
                }
            }
        }
    }
}

@Composable
private fun TestClipContent() {
    Row {
        Box(
            Modifier.width(120.dp).height(60.dp).clip(CircleShape).background(Color(0xFFFF9800)),
            contentAlignment = Alignment.Center,
        ) {
            Text("ClipBg", color = Color.White, fontSize = 10.sp)
        }
        Box(
            Modifier.width(120.dp).height(60.dp).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("ClipOnly", fontSize = 10.sp)
        }
    }
}
