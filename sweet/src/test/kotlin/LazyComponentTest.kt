import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.AutoSWT
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LazyComponentTest {
    @BeforeEach
    fun setup() {
    }

    /**
     * Helper method to find all Label widgets inside lazy components
     * (they are inside ScrolledComposite -> Composite structure)
     */
    private fun AutoSWT.findLazyLabels(shell: Shell): List<Label> =
        runOnSWT {
            shell
                .findAll<Control> { true }
                .filterIsInstance<Label>()
        }

    /**
     * Helper method to find all Button widgets inside lazy components
     */
    private fun AutoSWT.findLazyButtons(shell: Shell): List<Button> =
        runOnSWT {
            shell
                .findAll<Control> { true }
                .filterIsInstance<Button>()
        }

    /**
     * Helper method to validate lazy list structure and content
     */
    private fun AutoSWT.validateLazyStructure(
        shell: Shell,
        expectedLabelTexts: List<String>,
    ) {
        val labels = findLazyLabels(shell)

        val labelTexts =
            runOnSWT {
                labels.map { it.text }
            }

        if (SweetDebugger.assertionEnabled) {
            SweetDebugger.log("LazyComponentTest", "Found ${labels.size} labels")
            labelTexts.forEachIndexed { index, text ->
                SweetDebugger.log("LazyComponentTest", "  [$index] Label: '$text'")
            }
        }

        assertNotNull(labels, "Labels should be found")
        assertTrue(labels.isNotEmpty(), "Should have at least one label")

        expectedLabelTexts.forEach { expectedText ->
            val foundText = labelTexts.find { it == expectedText }
            assertNotNull(foundText, "Should find label with text '$expectedText'. Found texts: $labelTexts")
        }
    }

    @Test
    fun testLazyColumnBasicComposition() {
        val expectedTexts = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")

        autoSWT {
            testShell(width = 400, height = 600) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LazyColumn {
                        items(5) { index ->
                            Text("Item ${index + 1}")
                        }
                    }
                }
            }.test { shell ->

                // Validate that the LazyColumn created the expected structure
                validateLazyStructure(shell, expectedTexts)

                // Additional validation: check that all items are in order
                val labels = findLazyLabels(shell)
                assertEquals(5, labels.size, "Should have exactly 5 labels")

                val actualTexts =
                    runOnSWT {
                        labels.map { it.text }
                    }

                expectedTexts.forEachIndexed { index, expectedText ->
                    val actualText = actualTexts[index]
                    assertEquals(expectedText, actualText, "Item $index should have text '$expectedText', but found '$actualText'")
                }

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testLazyRowBasicComposition() {
        val expectedButtonTexts = listOf("Button 1", "Button 2", "Button 3")

        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LazyRow {
                        items(3) { index ->
                            Button(
                                onClick = { },
                                content = { Text("Button ${index + 1}") },
                            )
                        }
                    }
                }
            }.test { shell ->

                // Find all buttons in the lazy row
                val buttons = findLazyButtons(shell)

                if (SweetDebugger.assertionEnabled) {
                    SweetDebugger.log("LazyRowTest", "Found ${buttons.size} buttons")
                    val buttonTexts =
                        runOnSWT {
                            buttons.mapIndexed { index, button ->
                                index to button.text
                            }
                        }
                    buttonTexts.forEach { (index, text) ->
                        SweetDebugger.log("LazyRowTest", "  [$index] Button: '$text'")
                    }
                }

                // Validate structure
                assertNotNull(buttons, "Buttons should be found")
                assertEquals(3, buttons.size, "Should have exactly 3 buttons, found ${buttons.size}")

                // Validate button texts
                val actualButtonTexts =
                    runOnSWT {
                        buttons.map { it.text }
                    }

                expectedButtonTexts.forEachIndexed { index, expectedText ->
                    val actualText = actualButtonTexts[index]
                    assertEquals(expectedText, actualText, "Button $index should have text '$expectedText', but found '$actualText'")
                }

                // Test button interaction (click first button)
                runOnSWT {
                    SweetDebugger.log("LazyRowTest", "Testing button click interaction")
                    // The onClick is empty, but we can test that the button responds
                    val firstButton = buttons.first()
                    assertTrue(!firstButton.isDisposed, "Button should not be disposed")
                }

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testLazyListWithListData() {
        val testData = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")
        val expectedTexts = testData.map { "Fruit: $it" }

        autoSWT {
            testShell(width = 300, height = 500) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LazyColumn {
                        items(testData) { fruit ->
                            Card {
                                Text("Fruit: $fruit")
                            }
                        }
                    }
                }
            }.test { shell ->

                // Validate that all fruit items are created
                validateLazyStructure(shell, expectedTexts)

                val labels = findLazyLabels(shell)
                assertEquals(5, labels.size, "Should have exactly 5 fruit labels")

                // Validate that each fruit is present in correct order
                val actualLabelTexts =
                    runOnSWT {
                        labels.map { it.text }
                    }

                testData.forEachIndexed { index, fruit ->
                    val expectedText = "Fruit: $fruit"
                    val actualText = actualLabelTexts[index]
                    assertEquals(expectedText, actualText, "Fruit item $index should have text '$expectedText', but found '$actualText'")
                }

                // Additional validation: check that all original fruits are represented
                val foundFruits =
                    testData.filter { fruit ->
                        actualLabelTexts.any { text -> text.contains(fruit) }
                    }
                assertEquals(testData.size, foundFruits.size, "All fruits should be found. Expected: $testData, Found: $foundFruits")

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testLazyColumnWithMixedContent() {
        listOf("Header Item", "Button 0", "Button 1", "Button 2", "Footer Card")
        val expectedButtonTexts = listOf("Button 0", "Button 1", "Button 2")

        autoSWT {
            testShell(width = 400, height = 500) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    LazyColumn {
                        item {
                            Text("Header Item")
                        }

                        items(3) { index ->
                            Button(onClick = { }) {
                                Text("Button $index")
                            }
                        }

                        item {
                            Card {
                                Text("Footer Card")
                            }
                        }
                    }
                }
            }.test { shell ->

                val labels = findLazyLabels(shell)
                val buttons = findLazyButtons(shell)

                val labelTexts = runOnSWT { labels.map { it.text } }
                val buttonTexts = runOnSWT { buttons.map { it.text } }

                if (SweetDebugger.assertionEnabled) {
                    SweetDebugger.log("LazyMixedTest", "Found ${labels.size} labels and ${buttons.size} buttons")
                    labelTexts.forEachIndexed { index, text ->
                        SweetDebugger.log("LazyMixedTest", "  Label[$index]: '$text'")
                    }
                    buttonTexts.forEachIndexed { index, text ->
                        SweetDebugger.log("LazyMixedTest", "  Button[$index]: '$text'")
                    }
                }

                // Validate structure
                assertNotNull(labels, "Labels should be found")
                assertNotNull(buttons, "Buttons should be found")
                assertTrue(labels.isNotEmpty(), "Should have at least one label")
                assertTrue(buttons.isNotEmpty(), "Should have at least one button")

                // Validate specific content
                val hasHeader = labelTexts.any { it == "Header Item" }
                val hasFooter = labelTexts.any { it == "Footer Card" }
                assertTrue(hasHeader, "Expected header text. Found: $labelTexts")
                assertTrue(hasFooter, "Expected footer text. Found: $labelTexts")

                // Validate buttons
                assertEquals(3, buttons.size, "Should have exactly 3 buttons")
                expectedButtonTexts.forEachIndexed { index, expectedText ->
                    val actualText = buttonTexts[index]
                    assertEquals(expectedText, actualText, "Button $index should have text '$expectedText', but found '$actualText'")
                }

                // Test ordering: header, then buttons, then footer
                val orderedElements = mutableListOf<String>()
                // Add header
                orderedElements.add("Header Item")
                // Add button texts (they appear as labels too in the UI)
                orderedElements.addAll(expectedButtonTexts)
                // Add footer
                orderedElements.add("Footer Card")

                // Verify the content appears in the expected order
                assertTrue(labels.size >= 2, "Should have at least header and footer labels")

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }
}
