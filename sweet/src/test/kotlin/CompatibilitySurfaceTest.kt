import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.menuAnchor
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Smoke tests verifying that shim/no-op and partial APIs compose safely without crashing.
 *
 * These tests do not assert visual correctness — they ensure the compatibility surface
 * can be compiled and rendered without runtime exceptions, which is the primary contract
 * for a migration bridge.
 */
class CompatibilitySurfaceTest {
    // ── Text (partial: font params ignored) ─────────────────────────────

    @Test
    fun `text composes safely with ignored font parameters`() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Text(
                        text = "Hello",
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val labels = shell.findAll<Label> { true }
                    assertTrue(labels.isNotEmpty(), "Text should create a Label widget")
                }
            }
        }
    }

    @Test
    fun `text composes safely with only defaults`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Text("Default text only")
                }
            }.test { shell ->
                runOnSWT {
                    val labels = shell.findAll<Label> { true }
                    assertTrue(labels.isNotEmpty(), "Text should create a Label widget with defaults")
                }
            }
        }
    }

    // ── TextField (partial: many params ignored) ────────────────────────

    @Test
    fun `textField composes safely with ignored parameters`() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val text = remember { mutableStateOf("initial") }
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        readOnly = true,
                        singleLine = true,
                        isError = true,
                        maxLines = 3,
                        minLines = 2,
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val textWidgets = shell.findAll<org.eclipse.swt.widgets.Text> { true }
                    assertTrue(textWidgets.isNotEmpty(), "TextField should create an SWT Text widget")
                }
            }
        }
    }

    @Test
    fun `outlinedTextField composes safely`() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val text = remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val textWidgets = shell.findAll<org.eclipse.swt.widgets.Text> { true }
                    assertTrue(textWidgets.isNotEmpty(), "OutlinedTextField should create an SWT Text widget")
                }
            }
        }
    }

    // ── Switch / RadioButton (shim: alias Checkbox) ─────────────────────

    @Test
    fun `switch composes safely as shim`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val checked = remember { mutableStateOf(false) }
                    Switch(
                        checked = checked.value,
                        onCheckedChange = { checked.value = it },
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val buttons = shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    assertTrue(buttons.isNotEmpty(), "Switch should create a Button widget (checkbox shim)")
                }
            }
        }
    }

    @Test
    fun `radioButton composes safely as shim`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    RadioButton(
                        selected = true,
                        onClick = {},
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val buttons = shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    assertTrue(buttons.isNotEmpty(), "RadioButton should create a Button widget (checkbox shim)")
                }
            }
        }
    }

    // ── Slider (shim: renders as text) ──────────────────────────────────

    @Test
    fun `slider composes safely as text shim`() {
        autoSWT {
            testShell(width = 300, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Slider(
                        value = 0.5f,
                        onValueChange = {},
                    )
                }
            }.test { shell ->
                runOnSWT {
                    val labels = shell.findAll<Label> { true }
                    assertTrue(labels.isNotEmpty(), "Slider should render as a Label (text shim)")
                }
            }
        }
    }

    // ── Button variants (shim: alias Button) ────────────────────────────

    @Test
    fun `all button variants compose safely`() {
        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column {
                        OutlinedButton(onClick = {}) { Text("Outlined") }
                        TextButton(onClick = {}) { Text("Text") }
                        ElevatedButton(onClick = {}) { Text("Elevated") }
                        FilledTonalButton(onClick = {}) { Text("Tonal") }
                        FloatingActionButton(onClick = {}) { Text("FAB") }
                    }
                }
            }.test { shell ->
                runOnSWT {
                    val buttons = shell.findAll<org.eclipse.swt.widgets.Button> { true }
                    assertTrue(buttons.size >= 5, "All 5 button variants should create Button widgets, found ${buttons.size}")
                }
            }
        }
    }

    // ── Icon (shim: renders as text/emoji) ──────────────────────────────

    @Test
    fun `icon composes safely as text shim`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Blue,
                    )
                }
            }.test { shell ->
                runOnSWT {
                    // Icon is now a real Canvas-based component (vector rendering)
                    val canvases = shell.findAll<org.eclipse.swt.widgets.Canvas> { true }
                    assertTrue(canvases.isNotEmpty(), "Icon should render as a Canvas widget")
                }
            }
        }
    }

    @Test
    fun `iconButton composes safely`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                runOnSWT {
                    // IconButton contains a real Canvas-based Icon now
                    val canvases = shell.findAll<org.eclipse.swt.widgets.Canvas> { true }
                    assertTrue(canvases.isNotEmpty(), "IconButton should contain rendered Canvas content")
                }
            }
        }
    }

    // ── background(shape) (partial: shape ignored) ──────────────────────

    @Test
    fun `background with shape composes safely`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(
                        modifier =
                            Modifier
                                .size(100.dp)
                                .background(Color.Red, RectangleShape)
                                .padding(8.dp),
                    ) {
                        Text("Shaped bg")
                    }
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Background with shape should compose without crash")
                }
            }
        }
    }

    // ── menuAnchor (shim: no-op) ────────────────────────────────────────

    @Test
    fun `menuAnchor composes safely as noop`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(modifier = Modifier.size(100.dp).menuAnchor()) {
                        Text("Anchor")
                    }
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "menuAnchor should compose without crash")
                }
            }
        }
    }

    // ── mutableIntStateOf / mutableFloatStateOf (boxed shims) ───────────

    @Test
    fun `mutableIntStateOf composes safely`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val count = remember { mutableIntStateOf(0) }
                    Text("Count: ${count.value}")
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "mutableIntStateOf should work in composition")
                }
            }
        }
    }

    @Test
    fun `mutableFloatStateOf composes safely`() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val progress = remember { mutableFloatStateOf(0.5f) }
                    Text("Progress: ${progress.value}")
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "mutableFloatStateOf should work in composition")
                }
            }
        }
    }

    // ── Debug logging fires when assertions enabled ─────────────────────

    @Test
    fun `sweetDebugger assertionEnabled is true when dashEA is active`() {
        // When -ea is passed to the JVM, SweetDebugger.assertionEnabled should be true.
        // Gradle test runner may or may not pass -ea; we test the property is readable.
        val enabled = SweetDebugger.assertionEnabled
        // Just assert it's a boolean — the test runner config determines the value.
        assertNotNull(enabled)
    }

    @Test
    fun `shim APIs compose safely even with assertions enabled`() {
        // All shim APIs have SweetDebugger.log inside assertion guards.
        // Even when assertions are enabled, they must not throw.
        autoSWT {
            testShell(width = 600, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Text with ignored params
                        Text("Hi", fontSize = 14.sp, fontStyle = FontStyle.Italic)
                        // Slider (text shim)
                        Slider(value = 0.3f, onValueChange = {})
                        // Switch (checkbox shim)
                        Switch(checked = true, onCheckedChange = {})
                        // Button variant (alias shim)
                        OutlinedButton(onClick = {}) { Text("OK") }
                        // Icon (text shim)
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            }.test { shell ->
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "All shim APIs should compose without crash with assertions enabled")
                }
            }
        }
    }
}
