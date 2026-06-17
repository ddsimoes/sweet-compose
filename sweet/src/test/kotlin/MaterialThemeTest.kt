import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MaterialThemeTest {
    @BeforeEach
    fun setup() {
    }

    @Test
    fun testDarkColorSchemeCreation() {
        val colorScheme = darkColorScheme()

        // Test default colors
        assertEquals(Color(0xFF6200EAUL), colorScheme.primary)
        assertEquals(Color(0xFF03DAC6UL), colorScheme.secondary)
        assertEquals(Color(0xFF121212UL), colorScheme.background)
        assertEquals(Color.White, colorScheme.onPrimary)
        assertEquals(Color.Black, colorScheme.onSecondary)
    }

    @Test
    fun testDarkColorSchemeWithOverrides() {
        val customPrimary = Color(0xFF0000FFUL)
        val customSecondary = Color(0xFF00FF00UL)

        val colorScheme =
            darkColorScheme(
                primary = customPrimary,
                secondary = customSecondary,
            )

        // Test overridden colors
        assertEquals(customPrimary, colorScheme.primary)
        assertEquals(customSecondary, colorScheme.secondary)

        // Test that non-overridden colors remain default
        assertEquals(Color(0xFF121212UL), colorScheme.background)
        assertEquals(Color.White, colorScheme.onPrimary)
    }

    @Test
    fun testMaterialThemeComposition() {
        val customColorScheme =
            darkColorScheme(
                primary = Color(0xFF0000FFUL),
                background = Color(0xFF000000UL),
            )

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    MaterialTheme(colorScheme = customColorScheme) {
                        // Access MaterialTheme values within the theme
                        val themeColors = MaterialTheme.colorScheme
                        val themeTypography = MaterialTheme.typography

                        // Verify that the theme provides the expected values
                        assertEquals(Color(0xFF0000FFUL), themeColors.primary)
                        assertEquals(Color(0xFF000000UL), themeColors.background)
                        assertNotNull(themeTypography.headlineSmall)

                        Text("Themed content")
                    }
                }
            }.test { shell ->

                // Test that the composition was created successfully
                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain themed content")
                }
            }
        }
    }

    @Test
    fun testTypographyStyles() {
        val typography = Typography()

        // Test that all required typography styles exist
        assertNotNull(typography.headlineSmall)
        assertNotNull(typography.headlineMedium)
        assertNotNull(typography.titleMedium)
        assertNotNull(typography.bodyLarge)
        assertNotNull(typography.bodyMedium)
        assertNotNull(typography.bodySmall)
        assertNotNull(typography.labelSmall)
    }

    @Test
    fun testMaterialThemeWithDefaultValues() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    MaterialTheme {
                        // Test that default values work
                        val colors = MaterialTheme.colorScheme
                        val typography = MaterialTheme.typography

                        assertEquals(Color(0xFF6200EAUL), colors.primary)
                        assertNotNull(typography.headlineSmall)

                        Text("Default themed content")
                    }
                }
            }.test { shell ->

                runOnSWT {
                    assertTrue(shell.children.isNotEmpty(), "Shell should contain default themed content")
                }
            }
        }
    }
}
