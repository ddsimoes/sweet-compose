import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

class DebugLayoutTest {

    @BeforeEach
    fun setup() {
    }

    @Composable
    fun DebugRowApp() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Debug Row Test")
            
            // Direct Row test without Card wrapper
            Row {
                Text("Direct row text")
                Button(onClick = { }) {
                    Text("Direct Button")
                }
            }
            
            // Row with fillMaxWidth TextField
            Row {
                TextField(
                    value = "test",
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                )
                Button(onClick = { }) {
                    Text("With Fill")
                }
            }
            
            // Simple Row without modifiers
            Row {
                TextField(
                    value = "simple",
                    onValueChange = { }
                )
                Button(onClick = { }) {
                    Text("Simple")
                }
            }
        }
    }

    @Test
    fun testDebugRow() {
        autoSWT {
            testShell(width = 500, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    DebugRowApp()
                }
            }.test { shell ->
                // Wait for UI to be ready

                // Count all text fields and buttons
                val textFields = shell.findAll<Text> { runOnSWT { 
                    it.style and SWT.MULTI == 0 && it.style and SWT.READ_ONLY == 0 
                }}
                val buttons = shell.findAll<Button> { runOnSWT { 
                    it.style and SWT.CHECK == 0 && it.style and SWT.RADIO == 0 
                }}
                
                println("Found ${textFields.size} text fields")
                println("Found ${buttons.size} buttons")
                
                // Debug: Check parent-child relationships
                runOnSWT {
                    shell.children.forEach { child ->
                        if (child is Composite) {
                            println("Top level composite: ${child.hashCode()}")
                            printComposite(child, 1)
                        }
                    }
                }
                
                // Verify components exist
                assertNotNull(textFields.find { runOnSWT { it.text == "test" } }, "fillMaxWidth TextField should be present")
                assertNotNull(textFields.find { runOnSWT { it.text == "simple" } }, "simple TextField should be present")
                assertNotNull(buttons.find { runOnSWT { it.text == "Direct Button" } }, "Direct button should be present")
                assertNotNull(buttons.find { runOnSWT { it.text == "With Fill" } }, "With Fill button should be present")
                assertNotNull(buttons.find { runOnSWT { it.text == "Simple" } }, "Simple button should be present")
            }
        }
    }
    
    private fun printComposite(composite: Composite, depth: Int) {
        val indent = "  ".repeat(depth)
        println("${indent}Composite ${composite.hashCode()} (isRow: ${composite.getData("isRow")})")
        composite.children.forEach { child ->
            when (child) {
                is Composite -> {
                    printComposite(child, depth + 1)
                }
                is Text -> {
                    println("$indent  Text: '${child.text}' (fillMaxWidth: ${child.getData("fillMaxWidth")})")
                }
                is Button -> {
                    println("$indent  Button: '${child.text}' (fillMaxWidth: ${child.getData("fillMaxWidth")})")
                }
                else -> {
                    println("$indent  ${child.javaClass.simpleName}: ${child.hashCode()}")
                }
            }
        }
    }
}
