import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.CanvasSampleApp
import io.github.ddsimoes.sweet.sample.canvasSampleDrawCount
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Canvas as SWTCanvas

class CanvasSampleTest {
    @BeforeEach
    fun setup() {
    }

    @Test
    fun canvasSample_rendersCanvasAndSummary() {
        autoSWT {
            testShell(width = 500, height = 350) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    CanvasSampleApp()
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Expected at least one SWT Canvas for chart drawing")

                val bounds = runOnSWT { canvases.first().bounds }
                assertTrue(bounds.width > 0 && bounds.height > 0, "Canvas should have positive size, was $bounds")

                // Verify that the draw callback has been associated with the Canvas.
                val hasCallback =
                    runOnSWT {
                        canvases.first().getData("__sweet_canvas_drawCallback") != null
                    }
                assertTrue(hasCallback, "Canvas should have a draw callback set")

                // Ensure our composable draw lambda has executed at least once.
                assertTrue(canvasSampleDrawCount > 0, "Canvas draw lambda should have been invoked")

                val summaryLabel =
                    shell.find<Label> {
                        it.text.startsWith("Data points:")
                    }
                assertNotNull(summaryLabel, "Summary label should be present")

                val summaryText = runOnSWT { summaryLabel.text }
                assertTrue(
                    summaryText.contains("Data points"),
                    "Summary text should mention data points, was '$summaryText'",
                )

                // Save artifacts for visual inspection
                shell.saveSVG("canvas_sample_overview")
                shell.saveScreenshot("canvas_sample_overview")
            }
        }
    }

    @Test
    fun canvasSample_updatesDataOnButtonClick() {
        autoSWT {
            testShell(width = 500, height = 350) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    CanvasSampleApp()
                }
            }.test { shell ->
                val summaryLabel =
                    shell.find<Label> {
                        it.text.startsWith("Data points:")
                    }
                assertNotNull(summaryLabel, "Summary label should be present")

                val addButton =
                    shell.find<Button> {
                        it.text == "Add Point"
                    }
                assertNotNull(addButton, "Add Point button should be present")

                val initialSummary = runOnSWT { summaryLabel.text }
                assertTrue(
                    initialSummary.contains("Data points: 6"),
                    "Initial summary should reflect 6 data points, was '$initialSummary'",
                )

                // Trigger state change via button click
                addButton.doSelect()

                val updatedSummary = runOnSWT { summaryLabel.text }
                assertTrue(
                    updatedSummary.contains("Data points: 7"),
                    "After clicking Add Point, expected 7 data points, was '$updatedSummary'",
                )

                // Sanity: Canvas should still be present after state change
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Canvas should remain present after state changes")

                // Save artifacts for visual inspection after interaction
                shell.saveSVG("canvas_sample_after_add")
                shell.saveScreenshot("canvas_sample_after_add")
            }
        }
    }
}
