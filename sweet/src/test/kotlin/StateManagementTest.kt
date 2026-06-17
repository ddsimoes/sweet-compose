import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StateManagementTest {
    @BeforeEach
    fun setup() {
    }

    @Test
    fun testMutableIntStateOf() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var intValue by remember { mutableIntStateOf(42) }

                    Column {
                        androidx.compose.material3.Text("Value: $intValue")
                        androidx.compose.material3.Button(onClick = { intValue = 99 }) {
                            androidx.compose.material3.Text("Change")
                        }
                    }
                }
            }.test { shell ->
                // Initial value
                val label = shell.find<Label>()
                assertNotNull(label, "Label should be present")
                val labelText = runOnSWT { label.text }
                assertEquals("Value: 42", labelText, "Initial value should be 42")

                // Mutate state via button click and verify UI updates
                val button = shell.find<org.eclipse.swt.widgets.Button> { it.text == "Change" }
                runOnSWT { button.doSelect() }

                val updatedText = runOnSWT { label.text }
                assertEquals("Value: 99", updatedText, "After mutation, label should show Value: 99")
            }
        }
    }

    @Test
    fun testMutableFloatStateOf() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var floatValue by remember { mutableFloatStateOf(3.14f) }

                    Column {
                        androidx.compose.material3.Text("Value: $floatValue")
                        androidx.compose.material3.Button(onClick = { floatValue = 2.71f }) {
                            androidx.compose.material3.Text("Change")
                        }
                    }
                }
            }.test { shell ->
                val label = shell.find<Label>()
                assertNotNull(label, "Label should be present")
                assertEquals("Value: 3.14", runOnSWT { label.text }, "Initial value should be 3.14")

                val button = shell.find<org.eclipse.swt.widgets.Button> { it.text == "Change" }
                runOnSWT { button.doSelect() }

                assertEquals("Value: 2.71", runOnSWT { label.text }, "After mutation, label should show Value: 2.71")
            }
        }
    }
}
