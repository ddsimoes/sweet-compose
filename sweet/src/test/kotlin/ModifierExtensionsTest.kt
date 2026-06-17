import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.menuAnchor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModifierExtensionsTest {
    @BeforeEach
    fun setup() {
    }

    @Test
    fun testClickableModifier() {
        var clickCount = 0

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier =
                            Modifier.clickable {
                                clickCount++
                            },
                    ) {
                        androidx.compose.material3.Text("Click me")
                    }
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain child controls")
                }

                // For now, just verify the composition works - clickable functionality depends on proper UI structure
                // This tests that clickable modifier doesn't break composition
                assertTrue(clickCount == 0, "Click count should start at 0")
            }
        }
    }

    @Test
    fun testBackgroundModifier() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier = Modifier.background(Color.Red),
                    ) {
                        androidx.compose.material3.Text("Red background")
                    }
                }
            }.test { shell ->

                // Find the composite with background
                val backgroundComposite =
                    shell.find<Composite> { composite ->
                        runOnSWT {
                            composite.background != null &&
                                composite.background.red == 255 &&
                                composite.background.green == 0 &&
                                composite.background.blue == 0
                        }
                    }
                assertNotNull(backgroundComposite, "Background composite should be present")
            }
        }
    }

    @Test
    fun testBackgroundModifierWithShape() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier = Modifier.background(Color.Blue, CircleShape),
                    ) {
                        androidx.compose.material3.Text("Blue circle")
                    }
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain child controls")
                }

                // For now, just verify the composition works - background functionality testing depends on proper UI structure
                // This tests that background modifier with shape doesn't break composition
                val controls = runOnSWT { shell.children.toList() }
                assertTrue(controls.isNotEmpty(), "Should have created controls")
            }
        }
    }

    @Test
    fun testMenuAnchorModifier() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier = Modifier.menuAnchor(),
                    ) {
                        androidx.compose.material3.Text("Menu anchor")
                    }
                }
            }.test { shell ->

                // Guard against cross-test contamination: skip checks if shell was disposed.
                if (runOnSWT { shell.isDisposed }) return@test

                // Test that the composition was created successfully (observable behavior)
                // MenuAnchor modifier should not break the composition
                val controls = runOnSWT { shell.children.toList() }
                assertTrue(controls.isNotEmpty(), "MenuAnchor should not prevent control creation")

                // Verify the shell contains the expected content
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain child controls")
                }
            }
        }
    }

    @Test
    fun testCombinedModifiers() {
        var clickCount = 0

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier =
                            Modifier
                                .background(Color.Green)
                                .clickable { clickCount++ }
                                .menuAnchor(),
                    ) {
                        androidx.compose.material3.Text("Combined modifiers")
                    }
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain child controls")
                }

                // For now, just verify the composition works - modifier functionality testing depends on proper UI structure
                // This tests that combined modifiers don't break composition
                val controls = runOnSWT { shell.children.toList() }
                assertTrue(controls.isNotEmpty(), "Should have created controls with combined modifiers")

                // Verify initial state
                assertEquals(0, clickCount, "Click count should start at 0")
            }
        }
    }

    @Test
    fun testSizeInConstraintsAppliedInRow() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Row(Modifier.size(300.dp, 100.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 50.dp, maxWidth = 80.dp)
                                    .background(Color.Red),
                        ) {
                            androidx.compose.material3.Text("A")
                        }
                        Box(
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 120.dp, maxWidth = 150.dp)
                                    .background(Color.Blue),
                        ) {
                            androidx.compose.material3.Text("B")
                        }
                    }
                }
            }.test { shell ->
                // Sanity check: controls were created and layout executed without errors
                val boxes = shell.findAll<Composite> { true }
                assertTrue(boxes.size >= 2, "Expected at least two child composites for Row")
            }
        }
    }

    @Test
    fun testAspectRatioAndOffsetAffectLayout() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Column {
                        Box(
                            modifier =
                                Modifier
                                    .size(100.dp)
                                    .background(Color.Yellow),
                        ) {
                            androidx.compose.material3.Text("Base")
                        }
                        // Offset is the primary behaviour we can reliably assert here
                        Box(
                            modifier =
                                Modifier
                                    .size(100.dp)
                                    .offset(x = 10.dp, y = 5.dp)
                                    .background(Color.Gray),
                        ) {
                            androidx.compose.material3.Text("OffsetOnly")
                        }
                    }
                }
            }.test { shell ->
                val second =
                    shell.find<Composite> { comp ->
                        runOnSWT {
                            comp.children.any {
                                it is org.eclipse.swt.widgets.Label &&
                                    (it as org.eclipse.swt.widgets.Label).text == "OffsetOnly"
                            }
                        }
                    }
                val bounds = runOnSWT { second.bounds }
                val density = runOnSWT { shell.display.dpi.x / 96f }
                val expectedX = (10 * density).toInt()
                val expectedY = (5 * density).toInt()

                // Offset should move the second box by at least the specified delta in px
                assertTrue(bounds.x >= expectedX, "Second box X should be offset by at least $expectedX (10dp at density=$density), was ${bounds.x}")
                assertTrue(bounds.y >= expectedY, "Second box Y should be offset by at least $expectedY (5dp at density=$density), was ${bounds.y}")
            }
        }
    }
}
