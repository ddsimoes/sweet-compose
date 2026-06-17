@file:Suppress("ktlint:standard:function-naming")
@file:OptIn(ExperimentalMaterial3Api::class)
package androidx.compose.material3

import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral tests for DropdownMenu — WS-4 contract.
 */
class DropdownMenuTest {

    @Test
    fun `dropdownMenu renders items when expanded`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    DropdownMenu(expanded = true, onDismissRequest = { }) {
                        DropdownMenuItem(text = "Item A", onClick = { })
                        DropdownMenuItem(text = "Item B", onClick = { })
                        DropdownMenuItem(text = "Item C", onClick = { })
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertEquals(3, buttons.size, "DropdownMenu with 3 items should have 3 buttons")

                val buttonTexts = runOnSWT { buttons.map { it.text } }
                assertTrue("Item A" in buttonTexts, "Item A should be present")
                assertTrue("Item B" in buttonTexts, "Item B should be present")
                assertTrue("Item C" in buttonTexts, "Item C should be present")
            }
        }
    }

    @Test
    fun `dropdownMenu renders nothing when not expanded`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    DropdownMenu(expanded = false, onDismissRequest = { }) {
                        DropdownMenuItem(text = "Hidden", onClick = { })
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertEquals(0, buttons.size, "Collapsed DropdownMenu should have no buttons")
            }
        }
    }

    @Test
    fun `dropdownMenuItem onClick fires on click`() {
        var itemClicked = false

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    DropdownMenu(expanded = true, onDismissRequest = { }) {
                        DropdownMenuItem(
                            text = "Click Me",
                            onClick = { itemClicked = true },
                        )
                    }
                }
            }.test { shell ->
                assertTrue(!itemClicked, "Item should not be clicked initially")

                val button = runOnSWT {
                    shell.find<org.eclipse.swt.widgets.Button> { it.text == "Click Me" }
                }!!
                runOnSWT { button.doSelect() }

                assertTrue(itemClicked, "DropdownMenuItem onClick should fire after doSelect")
            }
        }
    }

    @Test
    fun `exposedDropdownMenuBox toggles expanded`() {
        var expanded = false

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        Text("Click to toggle")
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(text = "Option 1", onClick = { })
                        }
                    }
                }
            }.test { shell ->
                assertTrue(!expanded, "Should start collapsed")

                // The trigger label lives inside the ExposedDropdownMenuBox container, which carries
                // the toggle MouseListener. Synthesize a click on that container and assert the toggle.
                val container = runOnSWT {
                    shell.find<org.eclipse.swt.widgets.Label> { it.text == "Click to toggle" }!!.parent
                }
                runOnSWT {
                    container.notifyListeners(
                        SWT.MouseDown,
                        org.eclipse.swt.widgets.Event().apply {
                            type = SWT.MouseDown
                            button = 1
                        },
                    )
                }

                assertTrue(
                    expanded,
                    "Clicking the ExposedDropdownMenuBox container should toggle expanded to true",
                )
            }
        }
    }
}
