import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.ddsimoes.autoswt.AutoSWT
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.MinimalApp
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalTest {
    @Test
    fun testMinimal() {
        autoSWT {
            val shell =
                testShell(width = 600, height = 900) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        MinimalApp()
                    }
                }

            runTest {
                testApp(shell.display)
            }
        }
    }

    @Test
    fun testMinimalApp() {
        autoSWT {
            runOnSWT {
                application(display) {
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "Sweet Counter Example",
                    ) {
                        MinimalApp()
                    }
                }
            }

            runTest {
                testApp(display)
            }
        }
    }

    private fun AutoSWT.testApp(display: Display) {
        val shell =
            runOnSWT {
                // I DO care about extra Windows (visible or not)

                assertEquals(1, display.shells.size, "Expected exactly 1 visible shell")

                display.shells.first()
            }
        val button = shell.find<Button>()
        assertEquals("Value: 0", runOnSWT { button.text })

        button.doSelect()
        assertEquals("Value: 1", runOnSWT { button.text })

        button.doSelect()
        assertEquals("Value: 2", runOnSWT { button.text })

        shell.saveSVG()
        shell.saveScreenshot()
    }
}
