import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.kitchensink3.KitchenSink3App
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Text
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test: on the KitchenSink3 TextInputs tab, scrolling to the end must bring
 * the last component (the OutlinedTextField with value "outlined") fully into view,
 * matching MPP Compose behavior.
 *
 * The original bug: ColumnDelegate measured non-weighted children against the column's
 * FULL height instead of the remaining space (MPP RowColumnMeasurePolicy semantics), so
 * the scroll viewport's injected fillMaxHeight resolved to the whole column, the viewport
 * overflowed past its parent's bottom edge, and the overflowed strip could never be
 * scrolled into view.
 */
class ScrollEndDebugTest {
    @Test
    fun outlinedFieldFullyVisibleAtScrollEnd() {
        autoSWT {
            testShell(width = 900, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                shell.findAll<Button> { it.text == "TextInputs" }[0].doSelect()

                val outlined = shell.findAll<Text> { it.text == "outlined" }[0]
                val vp = shell.findAll<ScrollViewport> { true }[0]

                // The viewport must not overflow its parent (the Scaffold content column).
                runOnSWT {
                    val vpBounds = vp.bounds
                    val parentClient = vp.parent.clientArea
                    assertTrue(
                        vpBounds.y + vpBounds.height <= parentClient.y + parentClient.height,
                        "Scroll viewport must fit within its parent: viewport=$vpBounds parentClient=$parentClient",
                    )
                }

                runOnSWT { vp.setOrigin(0, vp.maxScrollY) }

                // At max scroll the outlined field must lie fully inside the viewport's
                // client area. Compute its top relative to the viewport via widget bounds
                // (toDisplay-based helpers are skewed by the Text widget's inner trim).
                runOnSWT {
                    var top = 0
                    var c: Control = outlined
                    while (c !== vp) {
                        top += c.bounds.y
                        c = c.parent ?: error("outlined field is not inside the scroll viewport")
                    }
                    val height = outlined.bounds.height
                    val client = vp.clientArea
                    assertTrue(
                        top >= 0 && top + height <= client.height,
                        "Outlined field must be fully visible at scroll end: " +
                            "top=$top height=$height viewportClientHeight=${client.height} " +
                            "scrollY=${vp.scrollY} maxScrollY=${vp.maxScrollY} contentExtent=${vp.contentExtent}",
                    )
                }

                shell.saveScreenshot("scroll_end")
            }
        }
    }
}
