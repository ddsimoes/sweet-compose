import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertNotNull

class SimpleLayoutTest {

    @BeforeEach
    fun setup() {
    }

    @Composable
    fun SimpleTestApp() {
        var text by remember { mutableStateOf("") }
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Simple Layout Test")
            
            Spacer(modifier = Modifier.padding(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Section:")
                    
                    Spacer(modifier = Modifier.padding(4.dp))
                    
                    Row {
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                        
                        Button(onClick = { }) {
                            Text("Test Button")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testSimpleLayout() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    SimpleTestApp()
                }
            }.test { shell ->
                // Wait for UI to be ready

                // Test that components exist
                val textField = shell.find<Text>()
                val testButton = shell.find<Button> { it.text == "Test Button" }
                
                // Verify components exist
                assertNotNull(textField, "Text field should be present")
                assertNotNull(testButton, "Test button should be present")
                
                // Take screenshots for analysis
                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }
}
