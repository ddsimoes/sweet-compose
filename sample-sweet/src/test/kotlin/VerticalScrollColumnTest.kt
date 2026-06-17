import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VerticalScrollColumnTest {

    @Composable
    fun ScrollColumnContent(itemCount: Int = 50) {
        Card(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                repeat(itemCount) { i ->
                    Text(text = "Item #${i + 1}")
                }
            }
        }
    }

    @Composable
    fun ScrollColumnContent2(itemCount: Int = 50) {
        Card(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    repeat(itemCount) { i ->
                        Text(text = "Item #${i + 1}")
                    }
                }
            }
        }
    }

    @Test
    fun columnInsideCard_verticalScroll_scrollsAndFillsWidth() {
        autoSWT {
            testShell(width = 420, height = 240) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ScrollColumnContent(itemCount = 60)
                }
            }.test { shell ->
                SweetDebugger.log("VScrollColumnTest", "Verifying Column.verticalScroll inside Card")

                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition")
                }

                // Find scrolled composite
                val scrolled = shell.find<ScrollViewport>()
                assertNotNull(scrolled, "Expected a ScrollViewport wrapping the Column")

                // Content present
                val labels = shell.findAll<Label> { it.text.startsWith("Item #") }
                assertTrue("Should have many items composed") { labels.size >= 20 }

                // Width fill: the Column (child of scrolled content) should fill viewport width
                val viewport = runOnSWT { scrolled.clientArea }
                val contentComposite = runOnSWT { scrolled.content as Composite }
                val columnComposite = runOnSWT { contentComposite.children.firstOrNull() as? Composite }
                assertNotNull(columnComposite, "Expected a Column composite inside scroll content")
                val colBounds = columnComposite!!.getAbsoluteBounds()
                val vpLeft = runOnSWT { scrolled.toDisplay(viewport.x, 0).x }
                val vpRight = runOnSWT { scrolled.toDisplay(viewport.x + viewport.width, 0).x }
                val viewportWidth = vpRight - vpLeft
                assertTrue("Column should fill viewport width: column=${colBounds.width}, viewport=$viewportWidth") {
                    colBounds.width >= viewportWidth - 2
                }

                // Programmatically scroll down roughly half content height and verify origin changes
                runOnSWT {
                    val bar = scrolled.verticalBar
                    val initialSel = bar?.selection ?: 0
                    val range = if (bar != null) (bar.maximum - bar.thumb) else viewport.height * 2
                    val target = (range / 2).coerceAtLeast(1)
                    scrolled.setOrigin(0, target)
                    // Ensure bar selection reflects origin
                    if (bar != null) {
                        assertTrue(bar.selection != initialSel, "Scrollbar selection should change after setOrigin")
                    }
                }
                // Verify origin updated
                val originAfter = runOnSWT { scrolled.origin }
                assertTrue("Scrolled origin.y should be > 0 after scrolling") { originAfter.y > 0 }
            }
        }
    }

    @Test
    fun columnInsideColumnInsideCard_verticalScroll_scrollsAndFillsWidth() {
        autoSWT {
            testShell(width = 420, height = 240) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ScrollColumnContent2(itemCount = 60)
                }
            }.test { shell ->
                SweetDebugger.log("VScrollColumnTest", "Verifying Column.verticalScroll inside Card")

                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition")
                }

                // Find scrolled composite
                val scrolled = shell.find<ScrollViewport>()
                assertNotNull(scrolled, "Expected a ScrollViewport wrapping the Column")

                // Content present
                val labels = shell.findAll<Label> { it.text.startsWith("Item #") }
                assertTrue("Should have many items composed") { labels.size >= 20 }

                // Width fill: the Column (child of scrolled content) should fill viewport width
                val viewport = runOnSWT { scrolled.clientArea }
                val contentComposite = runOnSWT { scrolled.content as Composite }
                val columnComposite = runOnSWT { contentComposite.children.firstOrNull() as? Composite }
                assertNotNull(columnComposite, "Expected a Column composite inside scroll content")
                val colBounds = columnComposite!!.getAbsoluteBounds()
                val vpLeft = runOnSWT { scrolled.toDisplay(viewport.x, 0).x }
                val vpRight = runOnSWT { scrolled.toDisplay(viewport.x + viewport.width, 0).x }
                val viewportWidth = vpRight - vpLeft
                assertTrue("Column should fill viewport width: column=${colBounds.width}, viewport=$viewportWidth") {
                    colBounds.width >= viewportWidth - 2
                }

                // Programmatically scroll down roughly half content height and verify origin changes
                runOnSWT {
                    val bar = scrolled.verticalBar
                    val initialSel = bar?.selection ?: 0
                    val range = if (bar != null) (bar.maximum - bar.thumb) else viewport.height * 2
                    val target = (range / 2).coerceAtLeast(1)
                    scrolled.setOrigin(0, target)
                    // Ensure bar selection reflects origin
                    if (bar != null) {
                        assertTrue(bar.selection != initialSel, "Scrollbar selection should change after setOrigin")
                    }
                }
                // Verify origin updated
                val originAfter = runOnSWT { scrolled.origin }
                assertTrue("Scrolled origin.y should be > 0 after scrolling") { originAfter.y > 0 }
            }
        }
    }

}

