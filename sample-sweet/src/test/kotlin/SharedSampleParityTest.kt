import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.AnimationDemoApp
import io.github.ddsimoes.sweet.sample.DrawingKitchenSinkApp
import io.github.ddsimoes.sweet.sample.SweetLayoutTest
import io.github.ddsimoes.sweet.sample.TodoMvcApp
import io.github.ddsimoes.sweet.sample.kitchensink3.KitchenSink3App
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * **Behavioral parity gate** — the mechanical check that closes the Sweet-vs-JetBrains
 * "compiles ≠ works" gap.
 *
 * Every shared composable (hardlinked between `sample-sweet/` and `sample-jetbrains/`)
 * is mounted under Sweet's runtime with AutoSWT and exercised with behavioral assertions:
 * - Mounts without throwing
 * - Expected widgets exist and are laid out
 * - Interactions update observable state
 *
 * The JetBrains side (`:sample-jetbrains`) compiles the identical sources as the
 * compile/reference — this test proves *behavioral* correctness under Sweet.
 *
 * **This is not a smoke test.** Breaking a covered Sweet behavior MUST make at least
 * one assertion here fail. If you add a new shared composable, add a parity test for it.
 *
 * ### Local parity command
 * ```
 * xvfb-run -a ./gradlew :sample-sweet:test --tests "SharedSampleParityTest"
 * ```
 *
 * ### Relationship to other tests
 * - `CompatibilitySurfaceTest` (in `:sweet`) — smoke: "compiles and renders without crashing"
 * - `SharedSampleParityTest` (this file) — behavioral parity: real assertions on shared composables
 * - The A2 signature/shape diff was evaluated and deferred — behavioral parity already catches
 *   the API-drift problems that matter.
 */
class SharedSampleParityTest {

    // ── TodoMVC ────────────────────────────────────────────────────────

    @Test
    fun `todomvc mounts and shows initial items`() {
        autoSWT {
            testShell(width = 600, height = 580) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->
                // Core widgets exist
                assertNotNull(shell.find<Text>(), "Text input for new todos")
                assertNotNull(shell.find<Button> { it.text == "Add" }, "Add button")
                assertNotNull(shell.find<Button> { it.text.startsWith("All") }, "All filter")
                assertNotNull(shell.find<Button> { it.text.startsWith("Active") }, "Active filter")
                assertNotNull(shell.find<Button> { it.text.startsWith("Completed") }, "Completed filter")
                assertNotNull(shell.find<Button> { it.text.startsWith("Samples") }, "Load sample todos")

                // 4 initial todo items
                val deleteButtons = shell.findAll<Button> { it.text == "Delete" }
                assertEquals(4, deleteButtons.size, "Should have 4 initial todo items with Delete buttons")

                val itemLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertEquals(4, itemLabels.size, "Should have 4 initial item labels")
            }
        }
    }

    @Test
    fun `todomvc add todo increases item count`() {
        autoSWT {
            testShell(width = 600, height = 580) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->
                val initialDeleteCount = shell.findAll<Button> { it.text == "Delete" }.size
                assertEquals(4, initialDeleteCount)

                // Type and add a new todo (setText must run on SWT thread)
                val textField = shell.find<Text>()!!
                runOnSWT { textField.setText("New parity todo") }
                shell.find<Button> { it.text == "Add" }!!.doSelect()

                val afterAddDeleteCount = shell.findAll<Button> { it.text == "Delete" }.size
                assertEquals(5, afterAddDeleteCount, "Adding a todo should create a new Delete button")

                val newItemLabels = shell.findAll<Label> { it.text == "New parity todo" }
                assertEquals(1, newItemLabels.size, "New todo label should appear")
            }
        }
    }

    @Test
    fun `todomvc delete removes item`() {
        autoSWT {
            testShell(width = 600, height = 580) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->
                val initialDeleteCount = shell.findAll<Button> { it.text == "Delete" }.size
                assertEquals(4, initialDeleteCount)

                shell.findAll<Button> { it.text == "Delete" }.first().doSelect()

                val afterDeleteCount = shell.findAll<Button> { it.text == "Delete" }.size
                assertEquals(3, afterDeleteCount, "Deleting an item should reduce Delete buttons by 1")
            }
        }
    }

    @Test
    fun `todomvc filter buttons switch view`() {
        autoSWT {
            testShell(width = 600, height = 580) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->
                // Load sample todos (appends 5 items with names like "Review code changes")
                shell.find<Button> { it.text.startsWith("Samples") }!!.doSelect()

                // After adding samples, total Delete buttons should be > 4
                val allDeleteButtons = shell.findAll<Button> { it.text == "Delete" }
                val initialCount = allDeleteButtons.size
                assertTrue(initialCount > 4, "Sample todos should increase delete-button count beyond 4, got $initialCount")

                // Switch to Active filter — should show fewer items
                shell.find<Button> { it.text.startsWith("Active") }!!.doSelect()
                val activeDeleteButtons = shell.findAll<Button> { it.text == "Delete" }
                assertTrue(activeDeleteButtons.size <= initialCount, "Active filter should not increase items")

                // Switch back to All — should restore full list
                shell.find<Button> { it.text.startsWith("All") }!!.doSelect()
                val allAgain = shell.findAll<Button> { it.text == "Delete" }
                assertEquals(initialCount, allAgain.size, "All filter should restore full list")
            }
        }
    }

    // ── KitchenSink3 ───────────────────────────────────────────────────

    @Test
    fun `kitchensink3 mounts and renders all tabs`() {
        autoSWT {
            testShell(width = 900, height = 700) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                val expectedTabs = listOf("Buttons", "TextInputs", "Selection", "Layout", "Lists", "Tabs", "Indicators", "Dialogs")
                val tabButtons = shell.findAll<Button> { expectedTabs.contains(it.text) }
                assertEquals(expectedTabs.size, tabButtons.size, "All tab buttons should be present")
            }
        }
    }

    @Test
    fun `kitchensink3 tab switch shows correct content`() {
        autoSWT {
            testShell(width = 900, height = 700) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    KitchenSink3App()
                }
            }.test { shell ->
                val textInputsTab = shell.find<Button> { it.text == "TextInputs" }!!
                textInputsTab.doSelect()
                val typographyMarker = shell.findAll<Label> { it.text == "Typography" }
                assertTrue(typographyMarker.isNotEmpty(), "TextInputs tab should show 'Typography' marker")

                val layoutTab = shell.find<Button> { it.text == "Layout" }!!
                layoutTab.doSelect()
                val layoutMarker = shell.findAll<Label> { it.text == "Layout Primitives" }
                assertTrue(layoutMarker.isNotEmpty(), "Layout tab should show 'Layout Primitives' marker")
            }
        }
    }

    // ── SweetLayoutTest ─────────────────────────────────────────────────

    @Test
    fun `sweetLayoutTest mounts, renders sections, and counter click updates state`() {
        autoSWT {
            testShell(width = 600, height = 500) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    SweetLayoutTest()
                }
            }.test { shell ->
                val header = shell.findAll<Label> { it.text == "Sweet Layout System Test" }
                assertEquals(1, header.size, "Header label should be present")

                // Behavioral: click the "Increment" button and verify the label updates
                val clicksBefore = runOnSWT {
                    shell.findAll<Label> { it.text.startsWith("Clicks: ") }.first().text
                }
                assertEquals("Clicks: 0", clicksBefore)

                shell.find<Button> { it.text == "Increment" }!!.doSelect()
                val clicksAfter1 = runOnSWT {
                    shell.findAll<Label> { it.text.startsWith("Clicks: ") }.first().text
                }
                assertEquals("Clicks: 1", clicksAfter1)

                shell.find<Button> { it.text == "Increment" }!!.doSelect()
                val clicksAfter2 = runOnSWT {
                    shell.findAll<Label> { it.text.startsWith("Clicks: ") }.first().text
                }
                assertEquals("Clicks: 2", clicksAfter2)

                // Presence checks still valid
                val rowLabel = shell.findAll<Label> { it.text == "Row Arrangements:" }
                assertEquals(1, rowLabel.size, "Row section label should be present")
                val buttons = shell.findAll<Button> { true }
                assertTrue(buttons.size >= 4, "Should have at least 4 buttons (Row items + Increment)")
            }
        }
    }

    // ── AnimationDemo ───────────────────────────────────────────────────

    @Test
    fun `animationDemo renders controls and cycles easing`() {
        autoSWT {
            testShell(width = 500, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    AnimationDemoApp()
                }
            }.test { shell ->
                val titleLabels = shell.findAll<Label> { it.text == "Animation Math Demo" }
                assertEquals(1, titleLabels.size, "AnimationDemo should render its title label")

                // Initial easing reflects the first entry (FastOutSlowIn).
                val easingLabels = shell.findAll<Label> { it.text.startsWith("Easing: ") }
                assertTrue(easingLabels.isNotEmpty(), "Should show current easing")
                val easing0 = runOnSWT { easingLabels.first().text }
                assertTrue(easing0.contains("FastOutSlowIn"),
                    "Initial easing should be FastOutSlowIn, got $easing0")

                // The Easing button cycles the displayed easing to the next entry (Linear).
                shell.find<Button> { it.text == "Easing" }!!.doSelect()
                val easing1 = runOnSWT {
                    shell.findAll<Label> { it.text.startsWith("Easing: ") }.first().text
                }
                assertTrue(easing1.contains("Linear"),
                    "After cycling, easing should be Linear, got $easing1")
            }
        }
    }

    // ── DrawingKitchenSink ──────────────────────────────────────────────

    @Test
    fun `drawingKitchenSink renders sections and PauseResume button toggles state`() {
        autoSWT {
            testShell(width = 900, height = 800) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    // animated = false so the initial spinning state is false → "Resume"
                    DrawingKitchenSinkApp(animated = false)
                }
            }.test { shell ->
                // Presence: section labels exist
                val labelTexts = runOnSWT {
                    shell.findAll<Label> { true }.map { it.text }
                }
                assertTrue(labelTexts.any { it.startsWith("1. Solid Colors") },
                    "Should have Solid Colors section")
                assertTrue(labelTexts.any { it.startsWith("2. Gradient Brushes") },
                    "Should have Gradient Brushes section")
                assertTrue(labelTexts.any { it.startsWith("8. BlendMode") },
                    "Should have BlendMode section")

                // Behavioral: toggle the spinning state via Pause/Resume button
                val btnLabel = runOnSWT {
                    shell.findAll<Button> { true }.first { it.text == "Resume" || it.text == "Pause" }.text
                }
                assertEquals("Resume", btnLabel, "Initial state should be Resume (not spinning)")

                shell.find<Button> { it.text == "Resume" }!!.doSelect()
                val afterClick = runOnSWT {
                    shell.findAll<Button> { true }.first { it.text == "Resume" || it.text == "Pause" }.text
                }
                assertEquals("Pause", afterClick, "After click, button should show Pause (now spinning)")

                shell.find<Button> { it.text == "Pause" }!!.doSelect()
                val afterSecondClick = runOnSWT {
                    shell.findAll<Button> { true }.first { it.text == "Resume" || it.text == "Pause" }.text
                }
                assertEquals("Resume", afterSecondClick, "After second click, should show Resume again")
            }
        }
    }
}
