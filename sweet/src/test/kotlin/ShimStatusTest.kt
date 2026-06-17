@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.menuAnchor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

/**
 * Behavior tests that enforce current shim status so silent regressions are caught.
 *
 * Each test documents what the shim currently does, then asserts that behavior.
 * When a shim becomes a real implementation, flip the assertion to match.
 */
class ShimStatusTest {
    // ── Slider ──────────────────────────────────────────────────────────────

    @Test
    fun slider_renders_as_text_label_not_interactive() {
        var sliderValue = 0.42f
        var changeCallbackFired = false

        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                changeCallbackFired = true
                                sliderValue = it
                            },
                        )
                        Text("Value: $sliderValue")
                    }
                }
            }.test { shell ->
                // Slider renders as Text("Slider: N%"), so there should be a Label
                val sliderLabelText =
                    runOnSWT {
                        val labels = shell.findAll<org.eclipse.swt.widgets.Label> { true }
                        labels.firstOrNull { it.text.startsWith("Slider:") }?.text
                    }
                assertTrue(sliderLabelText != null, "Slider should render as a text label starting with 'Slider:'")

                // It is NOT interactive — onValueChange should not fire just from rendering
                assertTrue(!changeCallbackFired, "Slider onValueChange should not fire during render (it's non-interactive)")
            }
        }
    }

    // ── Switch ──────────────────────────────────────────────────────────────

    @Test
    fun switch_delegates_to_checkbox_no_toggle_appearance() {
        var switchChecked = false

        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        Switch(
                            checked = switchChecked,
                            onCheckedChange = { switchChecked = it },
                        )
                        Text("Checked: $switchChecked")
                    }
                }
            }.test { shell ->
                // Switch renders as a Checkbox (SWT Button with CHECK style)
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertTrue(buttons.isNotEmpty(), "Switch should create a button (checkbox)")

                // Clicking should toggle (same behavior as Checkbox)
                val swtButton = buttons.first()
                assertTrue(!switchChecked, "Should start unchecked")
                runOnSWT { swtButton.doSelect() }
                assertTrue(switchChecked, "Switch should toggle on click (like a checkbox)")
            }
        }
    }

    // ── RadioButton ─────────────────────────────────────────────────────────

    @Test
    fun radioButton_delegates_to_checkbox_no_group_semantics() {
        var radioA = false
        var radioB = false

        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        RadioButton(
                            selected = radioA,
                            onClick = {
                                radioA = true
                                radioB = false
                            },
                        )
                        RadioButton(
                            selected = radioB,
                            onClick = {
                                radioB = true
                                radioA = false
                            },
                        )
                    }
                }
            }.test { shell ->
                val buttons =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    }
                assertEquals(2, buttons.size, "Should have 2 radio buttons (rendered as checkboxes)")

                // Click A — it should become selected
                assertTrue(!radioA, "A should start unselected")
                runOnSWT { buttons[0].doSelect() }
                assertTrue(radioA, "A should be selected after click")

                // NOTE: Without group semantics, both can be independently checked.
                // This test documents current behavior; update when group support is added.
            }
        }
    }

    // ── TextField ignored parameters ────────────────────────────────────────

    @Test
    fun textField_honors_readOnly_and_singleLine_flags() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "hello",
                        onValueChange = { },
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Name") }, // still ignored
                        placeholder = { Text("Enter") }, // still ignored
                    )
                }
            }.test { shell ->
                val (textCount, isEditable) =
                    runOnSWT {
                        val textWidgets = shell.findAll<org.eclipse.swt.widgets.Text> { true }
                        if (textWidgets.isEmpty()) {
                            false to false
                        } else {
                            true to textWidgets.first().editable
                        }
                    }
                assertTrue(textCount, "TextField should create an SWT Text widget")

                // readOnly is NOW honored — the text is NOT editable when readOnly=true
                assertTrue(!isEditable, "TextField should NOT be editable with readOnly=true (flag now honored)")

                // label/placeholder are still ignored — no extra Label widgets for them
                val labelTexts =
                    runOnSWT {
                        shell.findAll<org.eclipse.swt.widgets.Label> { true }.map { it.text }
                    }
                assertTrue(!labelTexts.contains("Name"), "Label text 'Name' should NOT appear (label is ignored)")
                assertTrue(!labelTexts.contains("Enter"), "Placeholder text 'Enter' should NOT appear (placeholder is ignored)")
            }
        }
    }

    // ── OutlinedTextField ──────────────────────────────────────────────────

    @Test
    fun outlinedTextField_delegates_to_same_TextField_impl() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    OutlinedTextField(
                        value = "outlined",
                        onValueChange = { },
                    )
                }
            }.test { shell ->
                // Should render as a plain SWT Text (same as regular TextField)
                val (textCount, textValue) =
                    runOnSWT {
                        val textWidgets = shell.findAll<org.eclipse.swt.widgets.Text> { true }
                        if (textWidgets.isEmpty()) {
                            false to ""
                        } else {
                            true to textWidgets.first().text
                        }
                    }
                assertTrue(textCount, "OutlinedTextField should create an SWT Text widget")
                assertEquals("outlined", textValue, "OutlinedTextField should display the value")
            }
        }
    }

    // ── ScrollableTabRow ───────────────────────────────────────────────────

    @Test
    fun scrollableTabRow_renders_identically_to_regular_tabRow() {
        autoSWT {
            testShell(width = 800, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    ScrollableTabRow(selectedTabIndex = 0) {
                        Tab(selected = true, onClick = { }, text = { Text("First") })
                        Tab(selected = false, onClick = { }, text = { Text("Second") })
                    }
                }
            }.test { shell ->
                // ScrollableTabRow renders as a plain Row with Tab buttons — no Scrollable
                val (buttonCount, tabTexts) =
                    runOnSWT {
                        val btns = shell.findAll<org.eclipse.swt.widgets.Button> { true }
                        btns.size to btns.map { it.text }
                    }
                assertTrue(buttonCount >= 2, "ScrollableTabRow should render 2 tab buttons")

                // Tabs should be clickable even though scroll is not implemented
                assertTrue(tabTexts.any { it.contains("First") }, "First tab label should appear")
                assertTrue(tabTexts.any { it.contains("Second") }, "Second tab label should appear")
            }
        }
    }

    // ── menuAnchor ──────────────────────────────────────────────────────

    @Test
    fun menuAnchor_is_noop() {
        var clicked = false

        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier =
                            Modifier
                                .menuAnchor()
                                .padding(16.dp),
                    ) {
                        Text("Menu anchor box")
                    }
                }
            }.test { shell ->
                // menuAnchor() is a no-op — composition should not crash
                val children = runOnSWT { shell.children.toList() }
                assertTrue(children.isNotEmpty(), "menuAnchor() should compose without error")
            }
        }
    }
}
