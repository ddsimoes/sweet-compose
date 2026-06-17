import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button as SWTButton
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DelegatesVisibleBoundsTest {

    @Test
    fun column_children_are_visible() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("A")
                        Text("B")
                        Text("C")
                    }
                }
            }.test { shell ->
                val labels = shell.findAll<Label> { it.text in listOf("A", "B", "C") }
                assertTrue(labels.isNotEmpty(), "Expected at least 3 labels")
                labels.assertLayout().areAllVisible()
            }
        }
    }

    @Test
    fun row_children_stay_within_parent_width() {
        autoSWT {
            testShell(width = 500, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Item 1", modifier = Modifier.weight(1f))
                        Button(onClick = {}) { Text("Delete") }
                    }
                }
            }.test { shell ->
                val composite = runOnSWT { shell.children.first() as Composite }
                val delete = shell.find<SWTButton> { it.text == "Delete" }
                val item = shell.find<Label> { it.text == "Item 1" }
                
                delete.assertLayout().isVisible().isWithin(composite)
                item.assertLayout().isVisible()
                assertTrue(item.visibleBounds().x >= 0, "Item starts outside viewport")
            }
        }
    }

    @Test
    fun box_centered_child_is_visible() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Centered")
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Centered" }
                label.assertLayout().isVisible()
            }
        }
    }
}

