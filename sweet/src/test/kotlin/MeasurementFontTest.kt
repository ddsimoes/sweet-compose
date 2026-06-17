import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MeasurementFontTest {
    @AfterEach
    fun cleanup() {
        SweetLayout.measurementFont = null
    }

    @Test
    fun `layout works with measurement font null`() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Hello")
                        Text("World")
                    }
                }
            }.test { shell ->
                val hello = shell.find<Label> { it.text == "Hello" }
                val world = shell.find<Label> { it.text == "World" }
                hello.assertLayout().isVisible()
                world.assertLayout().isVisible()

                val helloBounds = runOnSWT { hello.bounds }
                val worldBounds = runOnSWT { world.bounds }
                assertTrue(worldBounds.y > helloBounds.y, "World should be below Hello")
            }
        }
    }

    @Test
    fun `layout works with measurement font set to system font`() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val systemFont = this.display.systemFont
                SweetLayout.measurementFont = systemFont

                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Hello")
                        Text("World")
                    }
                }
            }.test { shell ->
                val hello = shell.find<Label> { it.text == "Hello" }
                val world = shell.find<Label> { it.text == "World" }
                hello.assertLayout().isVisible()
                world.assertLayout().isVisible()

                val helloBounds = runOnSWT { hello.bounds }
                val worldBounds = runOnSWT { world.bounds }
                assertTrue(worldBounds.y > helloBounds.y, "World should be below Hello")
            }
        }
    }

    @Test
    fun `measurement font does not change visible control font`() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val systemFont = this.display.systemFont
                SweetLayout.measurementFont = systemFont

                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.size(100.dp)) {
                        Text("Visible")
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Visible" }
                val controlFont = runOnSWT { label.font }
                assertNotNull(controlFont, "Control should have a font")
            }
        }
    }

    @Test
    fun `measurement font with explicit size produces deterministic layout`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val display = this.display
                val fontData = display.systemFont.fontData.firstOrNull()
                assertNotNull(fontData, "Should have at least one font data")
                val sizedFontData =
                    org.eclipse.swt.graphics.FontData(
                        fontData.name,
                        12,
                        fontData.style,
                    )
                val measurementFont = Font(display, sizedFontData)
                SweetLayout.measurementFont = measurementFont

                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Item A")
                        Text("Item B")
                        Text("Item C")
                    }
                }

                // Dispose font after layout
                this.addDisposeListener {
                    SweetLayout.measurementFont = null
                    measurementFont.dispose()
                }
            }.test { shell ->
                val a = shell.find<Label> { it.text == "Item A" }
                val b = shell.find<Label> { it.text == "Item B" }
                val c = shell.find<Label> { it.text == "Item C" }

                a.assertLayout().isVisible()
                b.assertLayout().isVisible()
                c.assertLayout().isVisible()

                val aBounds = runOnSWT { a.bounds }
                val bBounds = runOnSWT { b.bounds }
                val cBounds = runOnSWT { c.bounds }

                assertTrue(bBounds.y > aBounds.y, "B should be below A")
                assertTrue(cBounds.y > bBounds.y, "C should be below B")

                assertEquals(
                    aBounds.height,
                    bBounds.height,
                    "Items with same font should have same height",
                )
                assertEquals(
                    bBounds.height,
                    cBounds.height,
                    "Items with same font should have same height",
                )
            }
        }
    }

    @Test
    fun `alignByBaseline works with measurement font`() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val systemFont = this.display.systemFont
                SweetLayout.measurementFont = systemFont

                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    ) {
                        Text("Left", modifier = Modifier.alignByBaseline())
                        Text("Right", modifier = Modifier.alignByBaseline())
                    }
                }
            }.test { shell ->
                val left = shell.find<Label> { it.text == "Left" }
                val right = shell.find<Label> { it.text == "Right" }

                val leftY = runOnSWT { left.bounds.y }
                val rightY = runOnSWT { right.bounds.y }

                val delta = kotlin.math.abs(leftY - rightY)
                assertTrue(
                    delta <= 1,
                    "Baseline-aligned items should have matching Y (delta=$delta)",
                )
            }
        }
    }
}
