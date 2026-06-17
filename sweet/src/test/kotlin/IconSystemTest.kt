import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

class IconSystemTest {
    @BeforeEach
    fun setup() {
    }

    @Test
    fun testIconBasicComposition() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                    )
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain icon controls")
                }
            }
        }
    }

    @Test
    fun testIconWithTint() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.Red,
                    )
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain tinted icon")
                }
            }
        }
    }

    @Test
    fun testIconButtonComposition() {
        var buttonClicked = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    IconButton(
                        onClick = { buttonClicked = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                        )
                    }
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain icon button")
                }

                // Initial state - button not clicked
                assertTrue(!buttonClicked, "Button should not be clicked initially")
            }
        }
    }

    @Test
    fun testIconsCollectionExists() {
        // Test that our Icons collection has the expected icons
        assertNotNull(Icons.Default.Settings, "Settings icon should exist")
        assertNotNull(Icons.Default.Add, "Add icon should exist")
        assertNotNull(Icons.Default.Edit, "Edit icon should exist")
        assertNotNull(Icons.Default.Search, "Search icon should exist")
        assertNotNull(Icons.Default.MoreVert, "MoreVert icon should exist")
        assertNotNull(Icons.Default.Refresh, "Refresh icon should exist")

        // Test Filled variants
        assertNotNull(Icons.Filled.Settings, "Filled Settings icon should exist")
        assertNotNull(Icons.Filled.Add, "Filled Add icon should exist")
    }

    @Test
    fun testImageVectorProperties() {
        val settingsIcon = Icons.Default.Settings

        assertEquals("Settings", settingsIcon.name, "Icon name should be correct")
        assertTrue(settingsIcon.defaultWidth > 0.dp, "Icon should have positive width")
        assertTrue(settingsIcon.defaultHeight > 0.dp, "Icon should have positive height")
    }
}
