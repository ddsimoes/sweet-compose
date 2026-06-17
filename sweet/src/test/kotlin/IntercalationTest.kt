import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SWTContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.CompositionHandle
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.compose.locals.LocalDisplayDensity
import io.github.ddsimoes.sweet.internal.getDisplayDensity
import io.github.ddsimoes.sweet.internal.getSweetDensity
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for deep SWT -> Compose -> SWT -> Compose intercalation and scoped runtime context.
 */
class IntercalationTest {
    @Test
    fun nested_embedCompose_inside_swtContainer_parents_controls_to_nested_host() {
        lateinit var nestedHost: Composite

        autoSWT {
            testShell(width = 500, height = 320) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()

                root.embedCompose {
                    Column {
                        Text("Outer before")
                        SWTContainer(modifier = Modifier.fillMaxWidth()) { parent ->
                            nestedHost =
                                Composite(parent, SWT.NONE).apply {
                                    layout = FillLayout()
                                }

                            nestedHost.embedCompose {
                                Text("Nested Compose")
                            }
                        }
                        Text("Outer after")
                    }
                }
            }.test { shell ->
                val nestedLabel = shell.find<Label> { it.text == "Nested Compose" }
                val outerBefore = shell.find<Label> { it.text == "Outer before" }
                val outerAfter = shell.find<Label> { it.text == "Outer after" }

                runOnSWT {
                    assertSame(nestedHost, nestedLabel.parent, "Nested Compose label should be parented to the nested host")

                    val outerColumn = outerBefore.parent
                    assertSame(outerColumn, outerAfter.parent, "Outer siblings should share the same Column parent")
                    assertTrue(
                        outerColumn.children.indexOf(outerAfter) > outerColumn.children.indexOf(outerBefore),
                        "Sibling after nested embedCompose should remain under the outer composition parent",
                    )
                }
            }
        }
    }

    @Test
    fun sibling_after_nested_embedCompose_is_not_parented_to_nested_composition() {
        lateinit var nestedHost: Composite

        autoSWT {
            testShell(width = 500, height = 320) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()

                root.embedCompose {
                    Column {
                        Text("A")
                        SWTContainer { parent ->
                            nestedHost =
                                Composite(parent, SWT.NONE).apply {
                                    layout = FillLayout()
                                }

                            nestedHost.embedCompose {
                                Text("Nested")
                            }
                        }
                        Text("B")
                    }
                }
            }.test { shell ->
                val nested = shell.find<Label> { it.text == "Nested" }
                val a = shell.find<Label> { it.text == "A" }
                val b = shell.find<Label> { it.text == "B" }

                runOnSWT {
                    assertSame(nestedHost, nested.parent, "Nested label should stay inside nested host")
                    assertSame(a.parent, b.parent, "B should be an outer sibling, not a child of the nested composition")
                    assertTrue(
                        a.parent.children.indexOf(b) > a.parent.children.indexOf(a),
                        "B should appear after A in the outer parent children",
                    )
                }
            }
        }
    }

    @Test
    fun two_embedded_compositions_keep_children_under_their_own_hosts() {
        lateinit var hostOne: Composite
        lateinit var hostTwo: Composite

        autoSWT {
            testShell(width = 500, height = 320) {
                layout = FillLayout()
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout(SWT.VERTICAL)

                hostOne =
                    Composite(root, SWT.NONE).apply {
                        layout = FillLayout()
                    }
                hostTwo =
                    Composite(root, SWT.NONE).apply {
                        layout = FillLayout()
                    }

                hostOne.embedCompose {
                    Text("Host one")
                }
                hostTwo.embedCompose {
                    Text("Host two")
                }
            }.test { shell ->
                val one = shell.find<Label> { it.text == "Host one" }
                val two = shell.find<Label> { it.text == "Host two" }

                runOnSWT {
                    assertSame(hostOne, one.parent, "First embedded composition should parent controls to host one")
                    assertSame(hostTwo, two.parent, "Second embedded composition should parent controls to host two")
                }
            }
        }
    }

    @Test
    fun two_shells_keep_controls_and_menus_isolated() {
        lateinit var shellOne: Shell
        lateinit var shellTwo: Shell
        lateinit var handleOne: CompositionHandle
        lateinit var handleTwo: CompositionHandle

        autoSWT {
            testShell(width = 300, height = 220) {
                shellOne = Shell(display, SWT.SHELL_TRIM)
                shellTwo = Shell(display, SWT.SHELL_TRIM)

                shellOne.text = "Runtime context shell one"
                shellTwo.text = "Runtime context shell two"
                shellOne.layout = FillLayout()
                shellTwo.layout = FillLayout()
                shellOne.setSize(260, 180)
                shellTwo.setSize(260, 180)

                handleOne =
                    shellOne.embedCompose {
                        ScopedMenuContent(shellOne, "Shell one label", "One")
                    }
                handleTwo =
                    shellTwo.embedCompose {
                        ScopedMenuContent(shellTwo, "Shell two label", "Two")
                    }

                shellOne.open()
                shellTwo.open()
            }.test {
                try {
                    val oneLabel = shellOne.find<Label> { it.text == "Shell one label" }
                    val twoLabel = shellTwo.find<Label> { it.text == "Shell two label" }

                    runOnSWT {
                        assertSame(shellOne, oneLabel.shell, "Shell one label should stay in shell one")
                        assertSame(shellTwo, twoLabel.shell, "Shell two label should stay in shell two")

                        assertMenuTexts(shellOne.menuBar, listOf("One"))
                        assertMenuTexts(shellTwo.menuBar, listOf("Two"))
                    }
                } finally {
                    runOnSWT {
                        handleOne.dispose()
                        handleTwo.dispose()
                        if (!shellOne.isDisposed) shellOne.dispose()
                        if (!shellTwo.isDisposed) shellTwo.dispose()
                    }
                }
            }
        }
    }

    @Test
    fun composition_density_matches_active_display_context() {
        var localDensity: Density? = null
        var helperDensity: Density? = null
        lateinit var host: Composite

        autoSWT {
            testShell(width = 320, height = 200) {
                host =
                    Composite(this, SWT.NONE).apply {
                        layout = FillLayout()
                    }

                host.embedCompose {
                    val density = LocalDisplayDensity.current
                    SideEffect {
                        localDensity = density
                        helperDensity = getDisplayDensity()
                    }
                    Text("Density")
                }
            }.test {
                val expected = runOnSWT { host.display.getSweetDensity() }

                assertEquals(expected, localDensity, "Composition density should come from the host display")
                assertEquals(expected, helperDensity, "Compatibility density helper should resolve the active display")
            }
        }
    }

    @Composable
    private fun ScopedMenuContent(
        shell: Shell,
        label: String,
        menu: String,
    ) {
        val frameScope =
            object : FrameWindowScope {
                override val window: Shell get() = shell
            }

        frameScope.MenuBar {
            Menu(menu) {
                Item("Item") {
                }
            }
        }
        Text(label)
    }

    private fun assertMenuTexts(
        menuBar: Menu?,
        expected: List<String>,
    ) {
        assertNotNull(menuBar, "Shell should have a menu bar")
        val actual = menuBar.items.map { it.text.replace("&", "") }
        assertEquals(expected, actual)
    }
}
