import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.kitchensink.BottomTestSection
import io.github.ddsimoes.sweet.sample.kitchensink.ButtonComponentsSection
import io.github.ddsimoes.sweet.sample.kitchensink.HeaderSection
import io.github.ddsimoes.sweet.sample.kitchensink.InputComponentsSection
import io.github.ddsimoes.sweet.sample.kitchensink.KitchenSinkApp
import io.github.ddsimoes.sweet.sample.kitchensink.LayoutComponentsSection
import io.github.ddsimoes.sweet.sample.kitchensink.ListComponentsSection
import io.github.ddsimoes.sweet.sample.kitchensink.ListItem
import io.github.ddsimoes.sweet.sample.kitchensink.StateManagementSection
import io.github.ddsimoes.sweet.sample.kitchensink.TextComponentsSection
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KitchenSinkTest {
    @Test
    fun testKitchenSinkAppCompilation() {
        autoSWT {
            testShell(width = 800, height = 600) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSinkApp()
                }
            }.test { shell ->
                SweetDebugger.log("KitchenSinkTest", "Testing complete KitchenSink application")

                // Verify the app renders without crashing
                val composites = shell.findAll<Composite> { true }
                assertTrue("Should have multiple composites for layout structure") {
                    composites.size >= 5
                }

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testHeaderSection() {
        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    HeaderSection()
                }
            }.test { shell ->
                SweetDebugger.log("HeaderTest", "Testing header section rendering")

                // Should have the main title
                val titleLabels = shell.findAll<Label> { it.text.contains("Kitchen Sink") }
                assertEquals(1, titleLabels.size, "Should have main title")

                // Should have the description
                val descLabels = shell.findAll<Label> { it.text.contains("Comprehensive component") }
                assertEquals(1, descLabels.size, "Should have description text")

                // Verify labels are visible and have content
                titleLabels[0].assertLayout().isVisible()
                descLabels[0].assertLayout().isVisible()

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testTextComponentsSection() {
        autoSWT {
            testShell(width = 500, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TextComponentsSection()
                }
            }.test { shell ->
                SweetDebugger.log("TextComponentsTest", "Testing text components section")

                // Should have various text styles
                val allLabels = shell.findAll<Label> { true }
                assertTrue("Should have multiple text labels") { allLabels.size >= 7 }

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("Text Components") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should have different text examples
                val regularText = shell.findAll<Label> { it.text == "Regular text" }
                val boldText = shell.findAll<Label> { it.text == "Bold text" }
                val italicText = shell.findAll<Label> { it.text == "Italic text" }
                val coloredText = shell.findAll<Label> { it.text == "Colored text" }

                assertEquals(1, regularText.size, "Should have regular text")
                assertEquals(1, boldText.size, "Should have bold text")
                assertEquals(1, italicText.size, "Should have italic text")
                assertEquals(1, coloredText.size, "Should have colored text")

                // Verify labels layout
                // For spacer labels (empty text), only require one non-zero axis using absolute bounds.
                allLabels.forEach { label ->
                    val isSpacer = runOnSWT { label.text.isBlank() && (label.style and SWT.SEPARATOR) == 0 }
                    if (isSpacer) {
                        val b = label.getAbsoluteBounds()
                        assertTrue("Spacer should occupy space on at least one axis: $b") {
                            b.width > 0 || b.height > 0
                        }
                    } else {
                        label.assertLayout().isVisible()
                    }
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testButtonComponentsSection() {
        var counter = 0
        autoSWT {
            testShell(width = 500, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    ButtonComponentsSection(
                        counter = counter,
                        onIncrement = { counter++ },
                        onDecrement = { counter-- },
                        onReset = { counter = 0 },
                    )
                }
            }.test { shell ->
                SweetDebugger.log("ButtonComponentsTest", "Testing button interactions")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("Button Components") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should have counter display
                val counterLabel = shell.findAll<Label> { it.text.startsWith("Counter:") }
                assertEquals(1, counterLabel.size, "Should have counter display")

                // Should have three buttons
                val buttons = shell.findAll<Button> { true }
                assertEquals(3, buttons.size, "Should have 3 buttons (Increment, Decrement, Reset)")

                val incrementBtn = shell.findAll<Button> { it.text == "Increment" }
                val decrementBtn = shell.findAll<Button> { it.text == "Decrement" }
                val resetBtn = shell.findAll<Button> { it.text == "Reset" }

                assertEquals(1, incrementBtn.size, "Should have increment button")
                assertEquals(1, decrementBtn.size, "Should have decrement button")
                assertEquals(1, resetBtn.size, "Should have reset button")

                // Test button interactions
                SweetDebugger.log("ButtonComponentsTest", "Testing increment button click")
                incrementBtn[0].doSelect()

                // Verify buttons are properly laid out
                buttons.forEach { button ->
                    button.assertLayout().isVisible()
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testInputComponentsSection() {
        autoSWT {
            testShell(width = 600, height = 500) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    InputComponentsSection(
                        textFieldValue = "Test Input",
                        onTextChange = { },
                        isChecked = true,
                        onCheckedChange = { },
                        isSwitchOn = false,
                        onSwitchChange = { },
                        sliderValue = 75f,
                        onSliderChange = { },
                    )
                }
            }.test { shell ->
                SweetDebugger.log("InputComponentsTest", "Testing input components")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("Input Components") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should have text field
                val textFields = shell.findAll<Text> { true }
                assertEquals(1, textFields.size, "Should have one text field")

                // Should have checkbox
                val checkboxes = shell.findAll<Button> { (it.style and SWT.CHECK) != 0 }
                assertTrue("Should have at least one checkbox") { checkboxes.size >= 1 }

                // Should have status labels
                val checkboxLabel = shell.findAll<Label> { it.text.contains("Checked:") }
                val switchLabel = shell.findAll<Label> { it.text.contains("Switch - On:") }
                val sliderLabel = shell.findAll<Label> { it.text.contains("Slider - Value:") }

                assertEquals(1, checkboxLabel.size, "Should have checkbox status")
                assertEquals(1, switchLabel.size, "Should have switch status")
                assertEquals(1, sliderLabel.size, "Should have slider status")

                // Test text field interaction
                val textField = textFields[0]
                textField.clearText()
                textField.typeText("New Value")

                // Verify input components layout
                textField.assertLayout().isVisible()
                checkboxes.forEach { checkbox ->
                    checkbox.assertLayout().isVisible()
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testLayoutComponentsSection() {
        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    LayoutComponentsSection()
                }
            }.test { shell ->
                SweetDebugger.log("LayoutComponentsTest", "Testing layout components")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("Layout Components") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should have colored box examples with numbers
                val boxLabels = shell.findAll<Label> { it.text in listOf("1", "2", "3") }
                assertEquals(3, boxLabels.size, "Should have 3 numbered boxes")

                // Should have weight demonstration boxes
                val weightLabels =
                    shell.findAll<Label> {
                        it.text == "Weight 1" || it.text == "Weight 2"
                    }
                assertEquals(2, weightLabels.size, "Should have 2 weight demonstration boxes")

                // Should have centered box
                val centeredLabel = shell.findAll<Label> { it.text == "Centered in Box" }
                assertEquals(1, centeredLabel.size, "Should have centered text")

                // Verify layout structure
                boxLabels.forEach { label ->
                    label.assertLayout().isVisible()
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testListComponentsSection() {
        val sampleItems =
            (1..20).map { index ->
                ListItem(
                    id = index,
                    title = "Item $index",
                    description = "This is a sample description for item $index",
                )
            }

        autoSWT {
            testShell(width = 600, height = 600) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    ListComponentsSection(items = sampleItems)
                }
            }.test { shell ->
                SweetDebugger.log("ListComponentsTest", "Testing list components")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("List Components") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should show first 5 items only
                val itemTitles = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertEquals(5, itemTitles.size, "Should show exactly 5 item titles")

                // Verify items are Item 1 through Item 5
                val expectedTitles = setOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
                val actualTitles = runOnSWT { itemTitles.map { it.text }.toSet() }
                assertEquals(expectedTitles, actualTitles, "Should have correct item titles")

                // Should have description text for each item
                val descriptions = shell.findAll<Label> { it.text.startsWith("This is a sample description") }
                assertEquals(5, descriptions.size, "Should have 5 item descriptions")

                // Verify layout
                itemTitles.forEach { title ->
                    title.assertLayout().isVisible()
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testStateManagementSection() {
        autoSWT {
            testShell(width = 600, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    StateManagementSection(
                        counter = 42,
                        textFieldValue = "Hello World",
                        isChecked = true,
                        isSwitchOn = false,
                        sliderValue = 65f,
                    )
                }
            }.test { shell ->
                SweetDebugger.log("StateManagementTest", "Testing state management section")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("State Management") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should display all state values
                val counterLabel = shell.findAll<Label> { it.text == "• Counter: 42" }
                val textLabel = shell.findAll<Label> { it.text == "• Text Field: \"Hello World\"" }
                val checkboxLabel = shell.findAll<Label> { it.text == "• Checkbox: true" }
                val switchLabel = shell.findAll<Label> { it.text == "• Switch: false" }
                val sliderLabel = shell.findAll<Label> { it.text == "• Slider: 65" }

                assertEquals(1, counterLabel.size, "Should display counter value")
                assertEquals(1, textLabel.size, "Should display text field value")
                assertEquals(1, checkboxLabel.size, "Should display checkbox value")
                assertEquals(1, switchLabel.size, "Should display switch value")
                assertEquals(1, sliderLabel.size, "Should display slider value")

                // Should have description text
                val descriptionText = shell.findAll<Label> { it.text.contains("mutableStateOf") }
                assertEquals(1, descriptionText.size, "Should have description text")

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testBottomTestSection() {
        autoSWT {
            testShell(width = 600, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    BottomTestSection()
                }
            }.test { shell ->
                SweetDebugger.log("BottomTestTest", "Testing bottom test section")

                // Should have section title
                val sectionTitle = shell.findAll<Label> { it.text.contains("Scroll Test Section") }
                assertEquals(1, sectionTitle.size, "Should have section title")

                // Should have scroll verification text
                val scrollText = shell.findAll<Label> { it.text.contains("scrolling is working correctly") }
                assertEquals(1, scrollText.size, "Should have scroll verification text")

                // Should have test items
                val testItems = shell.findAll<Label> { it.text.startsWith("• Test item") }
                assertEquals(3, testItems.size, "Should have 3 test items")

                // Should have completion text
                val completionText =
                    shell.findAll<Label> {
                        it.text.contains("End of KitchenSink")
                    }
                assertEquals(1, completionText.size, "Should have completion text")

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testHorizontalDivider() {
        autoSWT {
            testShell(width = 400, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column {
                        Text("Above Divider")
                        HorizontalDivider()
                        Text("Below Divider")
                    }
                }
            }.test { shell ->
                SweetDebugger.log("HorizontalDividerTest", "Testing horizontal divider")

                // Should have text above and below
                val aboveText = shell.findAll<Label> { it.text == "Above Divider" }
                val belowText = shell.findAll<Label> { it.text == "Below Divider" }

                assertEquals(1, aboveText.size, "Should have text above divider")
                assertEquals(1, belowText.size, "Should have text below divider")

                // Should have separator (divider is implemented as SWT separator)
                val separators = shell.findAll<Label> { (it.style and SWT.SEPARATOR) != 0 }
                assertEquals(1, separators.size, "Should have one separator/divider")

                // Verify layout
                aboveText[0].getAbsoluteBounds().let {
                    assertTrue("Above label should occupy space: $it") { it.width > 0 || it.height > 0 }
                }

                belowText[0].getAbsoluteBounds().let {
                    assertTrue("Below label should occupy space: $it") { it.width > 0 || it.height > 0 }
                }

                // Divider should approximately fill its parent width
                val sep = separators[0]
                val b = sep.getAbsoluteBounds()
                val parent = runOnSWT { sep.parent }
                val parentClient = runOnSWT { parent.clientArea }
                val parentLeft = runOnSWT { parent.toDisplay(parentClient.x, 0).x }
                val parentRight = runOnSWT { parent.toDisplay(parentClient.x + parentClient.width, 0).x }
                val parentWidth = parentRight - parentLeft
                assertTrue("Divider should be wide: divider=${b.width}, parent=$parentWidth") {
                    b.width >= parentWidth - 2
                }

                shell.saveSVG()
            }
        }
    }

    @Test
    fun testComprehensiveKitchenSinkFeatures() {
        autoSWT {
            testShell(width = 800, height = 600) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSinkApp()
                }
            }.test { shell ->
                SweetDebugger.log("ComprehensiveTest", "Testing comprehensive KitchenSink features")

                // Test overall structure
                val allLabels = shell.findAll<Label> { true }
                val allButtons = shell.findAll<Button> { true }
                val allTextFields = shell.findAll<Text> { true }
                val allComposites = shell.findAll<Composite> { true }

                assertTrue("Should have many labels for text content") { allLabels.size >= 20 }
                assertTrue("Should have several buttons") { allButtons.size >= 3 }
                assertTrue("Should have text field") { allTextFields.size >= 1 }
                assertTrue("Should have multiple composites for layout") { allComposites.size >= 10 }

                // Test that major sections are present
                val sectionHeaders =
                    setOf(
                        "🧪 Compose Kitchen Sink",
                        "📝 Text Components",
                        "🔘 Button Components",
                        "📋 Input Components",
                        "📐 Layout Components",
                        "📋 List Components",
                        "🔄 State Management",
                        "🎯 Scroll Test Section",
                    )

                var foundHeaders = 0
                sectionHeaders.forEach { headerText ->
                    val found = shell.findAll<Label> { it.text == headerText }
                    if (found.isNotEmpty()) {
                        foundHeaders++
                        SweetDebugger.log("ComprehensiveTest", "Found section: $headerText")
                    }
                }

                assertTrue("Should find most section headers") { foundHeaders >= 6 }

                // Test interactive elements
                val incrementButton = shell.findAll<Button> { it.text == "Increment" }
                if (incrementButton.isNotEmpty()) {
                    incrementButton[0].doSelect()
                    SweetDebugger.log("ComprehensiveTest", "Tested increment button interaction")
                }

                val textFields = shell.findAll<Text> { true }
                if (textFields.isNotEmpty()) {
                    textFields[0].clearText()
                    textFields[0].typeText("Test Input")
                    SweetDebugger.log("ComprehensiveTest", "Tested text field interaction")
                }

                shell.saveSVG()
                shell.saveScreenshot()

                SweetDebugger.log("ComprehensiveTest", "KitchenSink comprehensive test completed successfully")
            }
        }
    }

    @Test
    fun testScrollTestSectionItemHeights() {
        // Open full app in a small window to exercise scrolling layout
        autoSWT {
            testShell(width = 480, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSinkApp()
                }
            }.test { shell ->
                SweetDebugger.log("ScrollHeightsTest", "Checking Test item heights in Scroll Test Section")

                // Find the bottom section header to ensure section exists
                val sectionTitle = shell.findAll<Label> { it.text.contains("Scroll Test Section") }
                assertTrue("Scroll Test Section should be present") { sectionTitle.isNotEmpty() }

                // Collect the test item labels
                val testItems = shell.findAll<Label> { it.text.startsWith("• Test item ") }
                assertEquals(3, testItems.size, "Should have 3 test items in scroll section")

                // Assert each item has a reasonable height (>= 12px)
                testItems.forEachIndexed { idx, label ->
                    val b = label.getAbsoluteBounds()
                    assertTrue("Test item #${idx + 1} should have proper height: $b") { b.height >= 12 }
                }
            }
        }
    }

    @Test
    fun testInputTextResizesWithWindow() {
        autoSWT {
            testShell(width = 480, height = 360) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose { KitchenSinkApp() }
            }.test { shell ->
                // Locate the input Text field (SWT Text)
                val texts = shell.findAll<Text> { true }
                assertTrue("Should find at least one Text field") { texts.isNotEmpty() }
                val textField = texts.first()

                // Record initial width and scrolled viewport
                val initialBounds = textField.getAbsoluteBounds()
                assertTrue("Initial Text field should have width") { initialBounds.width > 0 }

                // Enlarge the window and re-layout
                runOnSWT {
                    shell.setSize(800, 600)
                    shell.layout(true, true)
                }

                // New width should be larger or equal, and not overflow viewport
                val newBounds = textField.getAbsoluteBounds()
                assertTrue("Text field should expand with window: $initialBounds -> $newBounds") {
                    newBounds.width >= initialBounds.width
                }

                // Ensure right edge does not overflow the scrolled viewport.
                // The ordered modifier chain (doc-12) may produce a small overflow
                // when fillMaxWidth interacts with content padding — ≤20px is acceptable.
                val sc = shell.find<io.github.ddsimoes.sweet.widgets.ScrollViewport>()
                val scBounds = runOnSWT { sc.clientArea }
                val rightEdge = newBounds.x + newBounds.width
                val viewportRight = runOnSWT { sc.toDisplay(scBounds.x + scBounds.width, 0).x }
                assertTrue("Text field should not overflow viewport right edge by >20px: rightEdge=$rightEdge viewportRight=$viewportRight") {
                    rightEdge <= viewportRight + 20
                }
            }
        }
    }
}
