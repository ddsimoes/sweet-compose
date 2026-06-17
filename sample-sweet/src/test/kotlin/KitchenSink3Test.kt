import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.kitchensink3.KitchenSink3App
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the KitchenSink3 sample, switches through every tab via real tab clicks,
 * and asserts each tab's representative content is present and laid out sanely.
 *
 * Also guards the layout regressions found during the 2026 visual review:
 * - Scaffold fills the window instead of wrapping content.
 * - fillMaxWidth resolves against real bounds deep in the tree (TextField spans the window).
 * - Horizontal scrollers (LazyRow) hug content height instead of stretching, so
 *   siblings below them stay visible.
 */
class KitchenSink3Test {
    @Test
    fun allTabsRenderAndSwitch() {
        // Representative marker label for each tab's content
        val tabMarkers = listOf(
            "Buttons" to "Button Variants",
            "TextInputs" to "Typography",
            "Selection" to "Selection Controls",
            "Layout" to "Layout Primitives",
            "Lists" to "LazyColumn",
            "Tabs" to "TabRow (3 tabs)",
            "Indicators" to "ProgressIndicator",
            "Dialogs" to "Dialogs",
        )
        autoSWT {
            testShell(width = 900, height = 700) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                tabMarkers.forEach { (tab, marker) ->
                    val tabButtons = shell.findAll<Button> { it.text == tab }
                    assertEquals(1, tabButtons.size, "Should have one '$tab' tab button")
                    tabButtons[0].doSelect()

                    val markerLabels = shell.findAll<Label> { it.text == marker }
                    assertEquals(1, markerLabels.size, "Tab '$tab' should show '$marker'")
                    markerLabels[0].assertLayout().isVisible()

                    shell.saveScreenshot("tab_$tab")
                }

                // Tab row spans the window: tabs are not squeezed into wrap-content width
                val allTabs = tabMarkers.map { (tab, _) -> shell.findAll<Button> { it.text == tab }[0] }
                allTabs.assertLayout().areArrangedInRow()
                val firstTab = allTabs.first().getAbsoluteBounds()
                val lastTab = allTabs.last().getAbsoluteBounds()
                assertTrue("Tab row should span most of the 900px window") {
                    (lastTab.x + lastTab.width) - firstTab.x > 800
                }
            }
        }
    }

    @Test
    fun textInputsTabFieldsFillWidth() {
        autoSWT {
            testShell(width = 900, height = 700) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                shell.findAll<Button> { it.text == "TextInputs" }[0].doSelect()

                // fillMaxWidth must resolve against the real window width, not wrap content
                val textFields = shell.findAll<Text> { true }
                assertTrue("Should find text fields in TextInputs tab") { textFields.isNotEmpty() }
                textFields.forEach { field ->
                    val bounds = field.getAbsoluteBounds()
                    assertTrue("TextField should span most of the 900px window: $bounds") {
                        bounds.width > 600
                    }
                }
            }
        }
    }

    @Test
    fun listsTabContentBelowLazyRowStaysVisible() {
        autoSWT {
            testShell(width = 900, height = 700) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                shell.findAll<Button> { it.text == "Lists" }[0].doSelect()

                // The LazyRow scroller must hug its content height; if it stretches, the
                // "Scrollable Column" section is pushed out of the clipped content area.
                val sectionLabels = shell.findAll<Label> { it.text == "Scrollable Column" }
                assertEquals(1, sectionLabels.size, "Lists tab should show 'Scrollable Column'")
                sectionLabels[0].assertLayout().isVisible()

                val scrollLines = shell.findAll<Label> { it.text == "Scrollable line 0" }
                assertEquals(1, scrollLines.size, "Inner scrollable column should render")
                scrollLines[0].assertLayout().isVisible()
            }
        }
    }
}
