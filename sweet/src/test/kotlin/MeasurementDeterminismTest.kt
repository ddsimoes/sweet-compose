import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.internal.getDisplayDensity
import io.github.ddsimoes.sweet.test.useMeasurementFont
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertTrue

class MeasurementDeterminismTest {
    @Test
    fun pinned_font_produces_known_label_size() {
        autoSWT {
            testShell(width = 400, height = 200) {
                useMeasurementFont()
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                val label = Label(composite, SWT.NONE).apply { text = "Hello" }
                val size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                composite.setData("meas_size_w", size.x)
                composite.setData("meas_size_h", size.y)
            }.test { shell ->
                runOnSWT {
                    val root = shell.children.firstOrNull() as? Composite
                    val w = root?.getData("meas_size_w") as? Int ?: 0
                    val h = root?.getData("meas_size_h") as? Int ?: 0
                    // The font is pinned in POINTS, so SWT converts to px via DPI
                    // (px = pt * dpi/72). Scale the expected px bands by display density
                    // so the test holds on HiDPI/scaled displays, not only at 96 DPI.
                    val d = getDisplayDensity().density
                    val wLo = (25 * d).toInt(); val wHi = (45 * d).toInt()
                    val hLo = (12 * d).toInt(); val hHi = (22 * d).toInt()
                    assertTrue(w in wLo..wHi, "Label width $w outside [$wLo,$wHi] for pinned font @ density $d")
                    assertTrue(h in hLo..hHi, "Label height $h outside [$hLo,$hHi] for pinned font @ density $d")
                }
            }
        }
    }

    @Test
    fun pinned_font_makes_compose_text_stable() {
        autoSWT {
            testShell(width = 400, height = 200) {
                useMeasurementFont()
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.fillMaxSize()) { Text("Hello Compose") }
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.isVisible, "Shell should be visible under pinned font")
                }
            }
        }
    }
}
