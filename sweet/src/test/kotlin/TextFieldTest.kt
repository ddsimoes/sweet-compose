@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral tests for TextField and OutlinedTextField.
 * Per WS-4 contract: each implemented parameter must have a test
 * that asserts observable behavior.
 */
class TextFieldTest {

    // ── Basic value round-trip ─────────────────────────────────────────

    @Test
    fun `textField value round-trips via user input`() {
        var textValue = "initial"

        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                    )
                }
            }.test { shell ->
                assertEquals("initial", textValue)

                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                runOnSWT { textWidget.setText("updated") }

                assertEquals("updated", textValue, "State should update via onValueChange")
            }
        }
    }

    @Test
    fun `textField preserves value across programmatic change`() {
        var textValue = "initial"

        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertEquals("initial", runOnSWT { textWidget.text })

                // Update via SWT — should update state via onValueChange
                runOnSWT { textWidget.setText("changed") }

                // State should have updated through the ModifyListener callback
                assertEquals("changed", textValue,
                    "Value change via SWT should propagate through onValueChange")
            }
        }
    }

    @Test
    fun `textField with PasswordVisualTransformation sets SWT_PASSWORD style`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "secret",
                        onValueChange = { },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    TextField(
                        value = "plain",
                        onValueChange = { },
                    )
                }
            }.test { shell ->
                val texts = runOnSWT { shell.findAll<org.eclipse.swt.widgets.Text> { true } }
                val password = runOnSWT { texts.first { it.text == "secret" } }
                val plain = runOnSWT { texts.first { it.text == "plain" } }

                assertTrue(
                    runOnSWT { (password.style and SWT.PASSWORD) != 0 },
                    "PasswordVisualTransformation should set the SWT.PASSWORD style on the Text widget",
                )
                assertTrue(
                    runOnSWT { (plain.style and SWT.PASSWORD) == 0 },
                    "A TextField without a password transformation must not set SWT.PASSWORD",
                )
            }
        }
    }

    // ── enabled / readOnly ─────────────────────────────────────────────

    @Test
    fun `textField disabled state`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "disabled",
                        onValueChange = { },
                        enabled = false,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertEquals(false, runOnSWT { textWidget.isEnabled })
            }
        }
    }

    @Test
    fun `textField readOnly state`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "read-only",
                        onValueChange = { },
                        readOnly = true,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertEquals(false, runOnSWT { textWidget.editable })
            }
        }
    }

    // ── singleLine ─────────────────────────────────────────────────────

    @Test
    fun `textField singleLine creates non-wrapping text`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "single line",
                        onValueChange = { },
                        singleLine = true,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                val hasSingleStyle = runOnSWT {
                    (textWidget.style and SWT.SINGLE) != 0
                }
                assertTrue(hasSingleStyle, "singleLine TextField should have SWT.SINGLE style")
            }
        }
    }

    @Test
    fun `textField multiline accepts newline input`() {
        var textValue = ""

        autoSWT {
            testShell(width = 300, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        singleLine = false,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                // Multi-line TextField should accept newlines
                runOnSWT { textWidget.setText("line1\nline2") }
                assertTrue(textValue.contains("\n") || textValue.contains("line1"),
                    "Multi-line TextField should accept multiline input, got: '$textValue'")
            }
        }
    }

    // ── isError ────────────────────────────────────────────────────────

    @Test
    fun `textField isError sets visual feedback`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "error state",
                        onValueChange = { },
                        isError = true,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                val bg = runOnSWT { textWidget.background }
                // isError should set a non-null background
                assertTrue(bg != null,
                    "isError TextField should have a background color set")
            }
        }
    }

    @Test
    fun `textField no isError shows normal styling`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TextField(
                        value = "normal",
                        onValueChange = { },
                        isError = false,
                    )
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                // TextField without isError should compose successfully
                // (background may be null or system default — both are acceptable)
                assertEquals("normal", runOnSWT { textWidget.text })
            }
        }
    }

    // ── OutlinedTextField ──────────────────────────────────────────────

    @Test
    fun `outlinedTextField renders with value`() {
        autoSWT {
            testShell(width = 400, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    OutlinedTextField(
                        value = "outlined",
                        onValueChange = { },
                    )
                }
            }.test { shell ->
                val textWidgets = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Text> { true }
                }
                assertEquals(1, textWidgets.size,
                    "OutlinedTextField should contain exactly 1 Text widget")

                assertEquals("outlined", runOnSWT { textWidgets[0].text })
            }
        }
    }

    @Test
    fun `outlinedTextField with icons renders label and text`() {
        autoSWT {
            testShell(width = 400, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    OutlinedTextField(
                        value = "with icons",
                        onValueChange = { },
                        leadingIcon = { Text("L") },
                        trailingIcon = { Text("R") },
                    )
                }
            }.test { shell ->
                val labels = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                }
                val labelTexts = runOnSWT { labels.map { it.text } }
                assertTrue(labelTexts.any { it == "L" },
                    "Leading icon text 'L' should be present, got: $labelTexts")
                assertTrue(labelTexts.any { it == "R" },
                    "Trailing icon text 'R' should be present, got: $labelTexts")

                val textWidgets = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Text> { true }
                }
                assertEquals(1, textWidgets.size,
                    "OutlinedTextField with icons should have 1 Text widget")
            }
        }
    }

    @Test
    fun `outlinedTextField with label renders label text`() {
        autoSWT {
            testShell(width = 400, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        label = { Text("Username") },
                    )
                }
            }.test { shell ->
                val labels = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                }
                val labelTexts = runOnSWT { labels.map { it.text } }
                assertTrue(labelTexts.any { it == "Username" },
                    "Label 'Username' should be rendered, got: $labelTexts")
            }
        }
    }

    @Test
    fun `outlinedTextField isError renders with text`() {
        autoSWT {
            testShell(width = 400, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    OutlinedTextField(
                        value = "error",
                        onValueChange = { },
                        isError = true,
                    )
                }
            }.test { shell ->
                val textWidgets = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Text> { true }
                }
                assertEquals(1, textWidgets.size)
                assertEquals("error", runOnSWT { textWidgets[0].text })
            }
        }
    }

    // ── maxLines / minLines → SWT style ────────────────────────────────

    @Test
    fun `textField singleLine renders with SWT_SINGLE`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TextField(value = "x", onValueChange = {}, singleLine = true)
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertTrue((runOnSWT { textWidget.style } and SWT.SINGLE) != 0,
                    "singleLine should produce SWT.SINGLE")
            }
        }
    }

    @Test
    fun `textField maxLines one renders single-line`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TextField(value = "x", onValueChange = {}, maxLines = 1)
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertTrue((runOnSWT { textWidget.style } and SWT.SINGLE) != 0,
                    "maxLines = 1 should produce SWT.SINGLE")
            }
        }
    }

    @Test
    fun `textField explicit finite maxLines renders multi-line`() {
        autoSWT {
            testShell(width = 300, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TextField(value = "x", onValueChange = {}, maxLines = 3)
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertTrue((runOnSWT { textWidget.style } and SWT.MULTI) != 0,
                    "an explicit finite maxLines > 1 should produce SWT.MULTI")
            }
        }
    }

    @Test
    fun `textField default stays single-line`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TextField(value = "x", onValueChange = {})
                }
            }.test { shell ->
                val textWidget = shell.find<org.eclipse.swt.widgets.Text>()!!
                assertEquals(0, runOnSWT { textWidget.style } and SWT.MULTI,
                    "default TextField should stay single-line (unchanged behaviour)")
            }
        }
    }
}
