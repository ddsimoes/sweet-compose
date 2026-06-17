import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test

/**
 * Test Sweet's actual Canvas composable to isolate if Compose integration prevents painting.
 */
class SweetComposeCanvasTest {

    @Test
    fun testSweetComposeCanvas() {
        autoSWT {
            testShell(width = 500, height = 300) {
                layout = FillLayout()

                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TestCanvasApp()
                }

            }.test { shell ->
                println("\n=== Sweet Compose Canvas Test ===")
                println("If canvas shows GREY, Compose integration prevents Canvas painting")
                println("If canvas shows RED, the issue is specific to the BarChart sample")

                shell.saveScreenshot("sweet_compose_canvas_test")
            }
        }
    }
}

@Composable
private fun TestCanvasApp() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Sweet Compose Canvas Test (should be RED below):")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                SweetDebugger.log("TestCanvas", "Drawing RED rectangle: size=$size")
                SweetDebugger.log("TestCanvas", "Color.Red components: r=${Color.Red.red}, g=${Color.Red.green}, b=${Color.Red.blue}, a=${Color.Red.alpha}")

                // Fill with RED
                drawRect(
                    color = Color.Red,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
            }
        }
    }
}
