package io.github.ddsimoes.sweet.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SWTContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SWTContainer functionality
 * Tests the composition boundary between Compose and SWT
 */
class SWTContainerTest {
    @Test
    fun testBasicSWTContainer() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    testSWTContainerComponent()
                }
            }.test { shell ->
                // Verify the SWT Group was created by SWTContainer
                val groups = shell.findAll<Group> { it.text == "SWT Group Container" }
                assertEquals(1, groups.size, "Should have exactly one SWT Group")

                val group = groups[0]
                assertTrue(group.isDisplayed(), "SWT Group should be displayed")

                // Verify the Label inside the group
                val labels = shell.findAll<Label> { it.text == "This is pure SWT content" }
                assertEquals(1, labels.size, "Should have exactly one SWT Label")

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testSWTContainerWithInteraction() {
        autoSWT {
            testShell(width = 500, height = 350) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    testInteractiveSWTContainer()
                }
            }.test { shell ->
                // Find the button created in SWT
                val buttons = shell.findAll<Button> { it.text == "SWT Button" }
                assertEquals(1, buttons.size, "Should have exactly one SWT Button")

                val swtButton = buttons[0]
                assertTrue(swtButton.isDisplayed(), "SWT Button should be displayed")

                // Find the counter label
                val counterLabels = shell.findAll<Label> { it.text.startsWith("Clicks:") }
                assertEquals(1, counterLabels.size, "Should have exactly one counter label")

                val counterLabel = counterLabels[0]
                assertEquals("Clicks: 0", runOnSWT { counterLabel.text })

                // Click the SWT button
                swtButton.doSelect()

                // Verify counter updated (since SWT is single-threaded, changes are immediate)
                assertEquals("Clicks: 1", runOnSWT { counterLabel.text })

                // Click again
                swtButton.doSelect()
                assertEquals("Clicks: 2", runOnSWT { counterLabel.text })

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testNestedComposition() {
        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    testSimpleNestedComposition()
                }
            }.test { shell ->
                // Verify the SWT content was created
                val labels = shell.findAll<Label> { it.text == "Simple nested SWT content" }
                assertEquals(1, labels.size, "Should have exactly one nested SWT label")
                assertEquals(2, shell.findAll<Label>().size)
            }
        }
    }

    @Test
    fun testSWTContainerModifier() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    testSWTContainerWithModifier()
                }
            }.test { shell ->
                // Verify the SWT Composite was created
                val composites = shell.findAll<Composite> { true }
                // Should have: root composite + SWTContainer composite
                assertTrue(composites.size >= 2, "Should have at least 2 composites")

                // Verify content was created
                val labels = shell.findAll<Label> { it.text == "Content with modifier" }
                assertEquals(1, labels.size, "Should have exactly one label")

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testSWTContainerDynamicContent() {
        autoSWT {
            testShell(width = 500, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    testDynamicSWTContainer()
                }
            }.test { shell ->
                // Find the add button created by Compose
                val addButtons = shell.findAll<Button> { it.text == "Add SWT Widget" }
                assertEquals(1, addButtons.size, "Should have exactly one Add button")

                val addButton = addButtons[0]

                // Find the counter label (created by Compose, not SWT)
                val counterLabels = shell.findAll<Label> { it.text.startsWith("SWT Widgets:") }
                assertEquals(1, counterLabels.size, "Should have exactly one counter label")

                val counterLabel = counterLabels[0]
                assertEquals("SWT Widgets: 0", runOnSWT { counterLabel.text })

                // Click add button to create SWT widgets
                addButton.doSelect()

                // Verify counter updated
                assertEquals("SWT Widgets: 1", runOnSWT { counterLabel.text })

                // The dynamic content is recreated entirely, so we just verify the count matches
                // No need to verify specific dynamic labels since SWTContainer recreates content

                // Add more widgets
                addButton.doSelect()
                addButton.doSelect()

                assertEquals("SWT Widgets: 3", runOnSWT { counterLabel.text })

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testSWTContainerDisposal() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    io.github.ddsimoes.sweet.test
                        .testSWTContainerDisposal()
                }
            }.test { shell ->
                // Find the toggle button
                val toggleButtons = shell.findAll<Button> { it.text == "Toggle SWT Content" }
                assertEquals(1, toggleButtons.size, "Should have exactly one toggle button")

                val toggleButton = toggleButtons[0]

                // Initially should have SWT content
                val initialLabels = shell.findAll<Label> { it.text == "Initial SWT Content" }
                assertEquals(1, initialLabels.size, "Should have initial SWT content")

                // Click to hide content
                toggleButton.doSelect()

                // SWT content should be gone (disposed)
                val hiddenLabels = shell.findAll<Label> { it.text == "Initial SWT Content" }
                assertEquals(0, hiddenLabels.size, "SWT content should be disposed")

                // Click to show content again
                toggleButton.doSelect()

                // SWT content should be recreated
                val recreatedLabels = shell.findAll<Label> { it.text == "Initial SWT Content" }
                assertEquals(1, recreatedLabels.size, "SWT content should be recreated")

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }
}

@Composable
private fun testSWTContainerComponent() {
    SweetDebugger.log("TestSWTContainerComponent", "Rendering SWTContainer test")

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Compose content above", modifier = Modifier.padding(bottom = 8.dp))

        SWTContainer { parent ->
            SweetDebugger.log("SWTContainer", "Creating SWT Group")
            val group = Group(parent, SWT.NONE)
            group.text = "SWT Group Container"
            group.layout = GridLayout(1, false)
            group.layoutData = GridData(GridData.FILL, GridData.FILL, true, true)

            val label = Label(group, SWT.NONE)
            label.text = "This is pure SWT content"
            label.layoutData = GridData(GridData.FILL, GridData.CENTER, true, false)
        }

        Text("Compose content below", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun testInteractiveSWTContainer() {
    var composeCounter by remember { mutableStateOf(0) }

    SweetDebugger.log("TestInteractiveSWTContainer", "Rendering interactive SWTContainer - composeCounter=$composeCounter")

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Compose Counter: $composeCounter", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { composeCounter++ },
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Compose Button")
        }

        SWTContainer { parent ->
            SweetDebugger.log("SWTContainer", "Creating interactive SWT content")
            val composite = Composite(parent, SWT.NONE)
            composite.layout = GridLayout(2, false)
            composite.layoutData = GridData(GridData.FILL, GridData.FILL, true, true)

            var swtCounter = 0

            val counterLabel = Label(composite, SWT.NONE)
            counterLabel.text = "Clicks: $swtCounter"
            counterLabel.layoutData = GridData(GridData.FILL, GridData.CENTER, true, false)

            val swtButton = Button(composite, SWT.PUSH)
            swtButton.text = "SWT Button"
            swtButton.layoutData = GridData(GridData.CENTER, GridData.CENTER, false, false)

            swtButton.addSelectionListener(
                object : org.eclipse.swt.events.SelectionAdapter() {
                    override fun widgetSelected(e: org.eclipse.swt.events.SelectionEvent) {
                        swtCounter++
                        counterLabel.text = "Clicks: $swtCounter"
                    }
                },
            )
        }
    }
}

@Composable
private fun testSimpleNestedComposition() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Simple Nested Composition Test")

        SWTContainer(modifier = Modifier.fillMaxWidth()) { parent ->
            parent.background = parent.display.getSystemColor(SWT.COLOR_GRAY)
            parent.layout =
                RowLayout().apply {
                    this.marginLeft = 0
                }
            val swtLabel = Label(parent, SWT.NONE)
            swtLabel.text = "Simple nested SWT content"
        }
    }
}

@Composable
private fun testSWTContainerWithModifier() {
    SweetDebugger.log("TestSWTContainerWithModifier", "Rendering SWTContainer with modifier")

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("SWTContainer with Modifier", modifier = Modifier.padding(bottom = 8.dp))

        SWTContainer(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { parent ->
            SweetDebugger.log("SWTContainer", "Creating content with modifier")
            val label = Label(parent, SWT.NONE)
            label.text = "Content with modifier"
            label.layoutData = GridData(GridData.FILL, GridData.CENTER, true, false)
        }
    }
}

@Composable
private fun testDynamicSWTContainer() {
    var widgetCount by remember { mutableStateOf(0) }

    SweetDebugger.log("TestDynamicSWTContainer", "Rendering dynamic SWTContainer - widgetCount=$widgetCount")

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Dynamic SWT Content", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { widgetCount++ },
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Add SWT Widget")
        }

        Text("SWT Widgets: $widgetCount", modifier = Modifier.padding(bottom = 8.dp))

        SWTContainer { parent ->
            SweetDebugger.log("SWTContainer", "Creating $widgetCount dynamic widgets")
            val composite = Composite(parent, SWT.NONE)
            composite.layout = GridLayout(1, false)
            composite.layoutData = GridData(GridData.FILL, GridData.FILL, true, true)

            repeat(widgetCount) { index ->
                val label = Label(composite, SWT.NONE)
                label.text = "Dynamic SWT Widget ${index + 1}"
                label.layoutData = GridData(GridData.FILL, GridData.CENTER, true, false)
            }
        }
    }
}

@Composable
private fun testSWTContainerDisposal() {
    var showContent by remember { mutableStateOf(true) }

    SweetDebugger.log("TestSWTContainerDisposal", "Rendering disposal test - showContent=$showContent")

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("SWT Container Disposal Test", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { showContent = !showContent },
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Toggle SWT Content")
        }

        if (showContent) {
            SWTContainer { parent ->
                SweetDebugger.log("SWTContainer", "Creating content to be disposed")
                val label = Label(parent, SWT.NONE)
                label.text = "Initial SWT Content"
                label.layoutData = GridData(GridData.FILL, GridData.CENTER, true, false)
            }
        }
    }
}
