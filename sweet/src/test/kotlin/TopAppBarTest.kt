@file:Suppress("ktlint:standard:no-wildcard-imports")
@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.material3.*
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TopAppBarTest {

    @Test
    fun topAppBar_renders_title_text() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TopAppBar(
                        title = { Text("Test Title") },
                    )
                }
            }.test { shell ->
                // TopAppBar should render the title as a Label
                val labels = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                }
                val labelTexts = runOnSWT { labels.map { it.text } }
                assertTrue(
                    labelTexts.any { it == "Test Title" },
                    "TopAppBar should render 'Test Title' as a label, got: $labelTexts",
                )
            }
        }
    }

    @Test
    fun topAppBar_with_navigationIcon_renders_icon_text() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TopAppBar(
                        title = { Text("App Title") },
                        navigationIcon = { Text("←") },
                    )
                }
            }.test { shell ->
                val labels = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                }
                val labelTexts = runOnSWT { labels.map { it.text } }
                assertTrue(
                    labelTexts.any { it == "App Title" },
                    "TopAppBar should render title, got: $labelTexts",
                )
                assertTrue(
                    labelTexts.any { it == "←" },
                    "TopAppBar should render navigation icon text '←', got: $labelTexts",
                )
            }
        }
    }

    @Test
    fun topAppBar_actions_are_clickable() {
        var actionClicked = false

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    TopAppBar(
                        title = { Text("App Title") },
                        actions = {
                            Button(
                                onClick = { actionClicked = true },
                            ) {
                                Text("Action")
                            }
                        },
                    )
                }
            }.test { shell ->
                assertTrue(!actionClicked, "Action should not be clicked initially")

                // Find the action button and click it
                val buttons = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Button> { it.text == "Action" }
                }
                assertEquals(1, buttons.size, "Should have exactly 1 action button")
                runOnSWT { buttons[0].doSelect() }

                assertTrue(actionClicked, "Action should be clicked after doSelect")
            }
        }
    }

    @Test
    fun topAppBar_default_colors_compose_without_crash() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    val colors = TopAppBarDefaults.topAppBarColors()
                    assertNotNull(colors, "TopAppBar colors should not be null")

                    TopAppBar(
                        title = { Text("Default Colors") },
                        colors = colors,
                    )
                }
            }.test { shell ->
                val labels = runOnSWT {
                    shell.findAll<org.eclipse.swt.widgets.Label> { true }
                }
                val labelTexts = runOnSWT { labels.map { it.text } }
                assertTrue(
                    labelTexts.any { it == "Default Colors" },
                    "TopAppBar with default colors should render title, got: $labelTexts",
                )
            }
        }
    }

    @Test
    fun topAppBar_colors_data_class_constructs_correctly() {
        val colors = TopAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Blue,
            titleContentColor = androidx.compose.ui.graphics.Color.White,
            navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
            actionIconContentColor = androidx.compose.ui.graphics.Color.Gray,
        )

        assertNotNull(colors, "TopAppBarColors should be constructable")
        assertEquals(
            androidx.compose.ui.graphics.Color.Blue, colors.containerColor,
            "Container color should be Blue",
        )
        assertEquals(
            androidx.compose.ui.graphics.Color.White, colors.titleContentColor,
            "Title color should be White",
        )
    }
}
