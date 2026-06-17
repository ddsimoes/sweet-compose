import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ButtonVariantsTest {

    // ── Button variant shims (all alias Button) — smoke tests ──────────
    // Per WS-4 contract: smoke tests allowed only for shim/partial components.

    @Test
    fun `elevatedButton composes without crashing (shim smoke)`() {
        var buttonClicked = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    ElevatedButton(onClick = { buttonClicked = true }) {
                        Text("Elevated Button")
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { it.text == "Elevated Button" }
                }
                assertEquals(1, buttons.size, "Should have 1 elevated button")
                assertTrue(!buttonClicked, "Button should not be clicked initially")
            }
        }
    }

    @Test
    fun `filledTonalButton composes without crashing (shim smoke)`() {
        var buttonClicked = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    FilledTonalButton(onClick = { buttonClicked = true }) {
                        Text("Tonal Button")
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { it.text == "Tonal Button" }
                }
                assertEquals(1, buttons.size, "Should have 1 tonal button")
                assertTrue(!buttonClicked, "Button should not be clicked initially")
            }
        }
    }

    @Test
    fun `floatingActionButton composes without crashing (shim smoke)`() {
        var fabClicked = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    FloatingActionButton(onClick = { fabClicked = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }.test { shell ->
                assertTrue(!fabClicked, "FAB should not be clicked initially")
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertTrue(buttons.isNotEmpty(), "FAB should create at least one button")
            }
        }
    }

    @Test
    fun `button variants with icons compose without crashing (shim smoke)`() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        ElevatedButton(onClick = {}) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elevated")
                        }

                        FilledTonalButton(onClick = {}) {
                            Text("Tonal")
                        }

                        FloatingActionButton(onClick = {}) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }.test { shell ->
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertTrue(buttons.size >= 3, "Should have at least 3 buttons for 3 variants, got ${buttons.size}")
            }
        }
    }

    // ── Implemented components — behavioral tests ──────────────────────

    @Test
    fun radioButton_toggles_selected_state() {
        var radioSelected = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    RadioButton(
                        selected = radioSelected,
                        onClick = { radioSelected = true },
                    )
                }
            }.test { shell ->
                assertEquals(false, radioSelected, "Radio should be unselected initially")

                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { true }
                }
                assertTrue(buttons.isNotEmpty(), "RadioButton should create an SWT button")

                // Click the radio button and verify state changes
                runOnSWT { buttons[0].doSelect() }
                assertEquals(true, radioSelected, "Radio should be selected after click")
            }
        }
    }

    // ── Layout / misc ──────────────────────────────────────────────────

    @Test
    fun spacer_creates_spacing_between_text() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Row {
                        Text("Before")
                        Spacer(Modifier.width(16.dp))
                        Text("After")
                    }
                }
            }.test { shell ->
                val before =
                    runOnSWT { shell.find<org.eclipse.swt.widgets.Label> { it.text == "Before" }!!.bounds }
                val after =
                    runOnSWT { shell.find<org.eclipse.swt.widgets.Label> { it.text == "After" }!!.bounds }
                val gap = after.x - (before.x + before.width)
                assertTrue(
                    gap >= 8,
                    "Spacer(16.dp) should insert horizontal space between the labels (gap=$gap px)",
                )
            }
        }
    }
}
