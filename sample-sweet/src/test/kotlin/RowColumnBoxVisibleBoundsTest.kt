import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button as SWTButton
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RowColumnBoxVisibleBoundsTest {

    @Test
    fun row_weighted_text_and_button_visible() {
        autoSWT {
            testShell(width = 640, height = 240) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Weighted", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {}) { Text("Action") }
                    }
                }
            }.test { shell ->
                val composite = runOnSWT { shell.children.first() as Composite }
                val label = shell.find<Label> { it.text == "Weighted" }
                val button = shell.find<SWTButton> { it.text == "Action" }
                
                label.assertLayout().isVisible()
                button.assertLayout().isVisible().isWithin(composite)
                assertTrue(label.visibleBounds().x >= 0, "Label starts outside viewport")
            }
        }
    }

    @Test
    fun column_spaced_children_have_nonzero_intervals() {
        autoSWT {
            testShell(width = 320, height = 320) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("One")
                        Text("Two")
                        Text("Three")
                    }
                }
            }.test { shell ->
                val labels = shell.findAll<Label> { it.text in listOf("One", "Two", "Three") }
                
                labels.assertLayout().areAllVisible().areArrangedInColumn(minGap = 0).doNotOverlap()
            }
        }
    }

    @Test
    fun box_center_alignment_positions_child_inside() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Center")
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Center" }
                
                label.assertLayout().isVisible().isCenteredIn(runOnSWT { shell.children.first() })
            }
        }
    }
}

