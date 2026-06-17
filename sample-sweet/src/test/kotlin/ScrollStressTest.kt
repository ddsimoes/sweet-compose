import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScrollStressTest {

    @Composable
    private fun StressScrollableColumn() {
        var count by remember { mutableStateOf(50) }
        val items = remember(count) { (1..count).map { "Item $it" } }

        Column(Modifier.fillMaxSize()) {
            Button(onClick = {
                // Alternate between many and few items to force insert/remove/move
                count = if (count <= 10) 100 else 5
            }) {
                Text("Toggle Items")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach {
                    Text(it)
                }
            }
        }
    }

    @Test
    fun scrollable_column_handles_heavy_churn() {
        autoSWT {
            testShell(width = 480, height = 320) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    StressScrollableColumn()
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport>()
                assertNotNull(scroller, "Expected a ScrollViewport wrapping the stress column")

                // Initial items present
                val initialLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertTrue(initialLabels.size >= 20, "Expected many initial items")

                val toggle = shell.find<org.eclipse.swt.widgets.Button> { it.text == "Toggle Items" }

                repeat(5) {
                    // Toggle to few items
                    toggle.doSelect()
                    val fewLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                    assertTrue(fewLabels.size in 1..20, "Expected list to shrink on toggle")

                    // Toggle back to many items
                    toggle.doSelect()
                    val manyLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                    assertTrue(manyLabels.size >= 50, "Expected list to grow on toggle")

                    // Ensure scroller origin remains valid and scroll still works
                    val beforeScroll = runOnSWT { scroller.origin.y }
                    runOnSWT {
                        val bar = scroller.verticalBar
                        val target = if (bar != null) (bar.maximum - bar.thumb).coerceAtLeast(1) else scroller.clientArea.height * 2
                        scroller.setOrigin(0, target)
                    }
                    val afterScroll = runOnSWT { scroller.origin.y }
                    assertTrue(afterScroll >= beforeScroll, "Origin should remain scrollable after heavy churn")
                }
            }
        }
    }
}

