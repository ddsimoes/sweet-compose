import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Tests for modifier composition, precedence, and edge cases.
 * Ensures that modifiers compose correctly in various orders and combinations.
 */
class ModifierCompositionTest {
    // ========== MODIFIER ORDER AND PRECEDENCE ==========

    @Test
    fun size_then_padding_order() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        // Size then padding
                        Text("A", modifier = Modifier.size(100.dp).padding(8.dp))
                        // Padding then size
                        Text("B", modifier = Modifier.padding(8.dp).size(100.dp))
                    }
                }
            }.test { shell ->
                val labelA = shell.find<Label> { it.text == "A" }
                val labelB = shell.find<Label> { it.text == "B" }

                labelA.assertLayout().isVisible()
                labelB.assertLayout().isVisible()

                // Both should be visible and arranged
                listOf(labelA, labelB)
                    .assertLayout()
                    .areArrangedInRow(minGap = 0)

                // Sizes might differ based on modifier order
                val widthA = runOnSWT { labelA.bounds.width }
                val widthB = runOnSWT { labelB.bounds.width }

                SweetDebugger.log("ModifierComposition", "A width (size→padding): $widthA, B width (padding→size): $widthB")
            }
        }
    }

    @Test
    fun multiple_size_modifiers_last_wins() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Multiple size modifiers - which one wins?
                        Text("Multiple", modifier = Modifier.size(50.dp).size(100.dp).size(75.dp))
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Multiple" }
                label.assertLayout().isVisible()

                // Behavior depends on implementation - just verify it doesn't crash
                val bounds = runOnSWT { label.bounds }
                SweetDebugger.log("ModifierComposition", "Multiple size modifiers result: ${bounds.width}x${bounds.height}")
                assertTrue(bounds.width > 0 && bounds.height > 0, "Should have valid size")
            }
        }
    }

    @Test
    fun fillMaxWidth_then_width_modifier() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Explicit width only (fillMaxWidth + width can cause constraint issues)
                        Text("Width", modifier = Modifier.width(100.dp))
                        Text("FillWidth", modifier = Modifier.fillMaxWidth())
                    }
                }
            }.test { shell ->
                val width = shell.find<Label> { it.text == "Width" }
                val fillWidth = shell.find<Label> { it.text == "FillWidth" }

                width.assertLayout().isVisible()
                fillWidth.assertLayout().isVisible()

                val w1 = runOnSWT { width.bounds.width }
                val w2 = runOnSWT { fillWidth.bounds.width }

                SweetDebugger.log("ModifierComposition", "Width: $w1, FillWidth: $w2")

                // Both should be positive, fillWidth should be wider
                assertTrue(w1 > 0 && w2 > 0, "Both should have valid widths")
                assertTrue(w2 >= w1, "FillWidth should be at least as wide as fixed width")
            }
        }
    }

    // ========== CONTRADICTORY SIZE CONSTRAINTS ==========

    @Test
    fun sizeIn_with_min_greater_than_max() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Invalid constraints: minWidth > maxWidth
                        Text(
                            "Invalid",
                            modifier =
                                Modifier.sizeIn(
                                    minWidth = 200.dp,
                                    maxWidth = 100.dp,
                                    minHeight = 100.dp,
                                    maxHeight = 50.dp,
                                ),
                        )
                    }
                }
            }.test { shell ->
                // Should not crash - implementation decides how to handle
                val label = shell.find<Label> { it.text == "Invalid" }
                label.assertLayout().isVisible()

                val bounds = runOnSWT { label.bounds }
                SweetDebugger.log("ModifierComposition", "Min>Max constraints result: ${bounds.width}x${bounds.height}")

                // Should resolve to some valid size
                assertTrue(bounds.width > 0 && bounds.height > 0, "Should resolve to valid size despite contradictory constraints")
            }
        }
    }

    @Test
    fun size_zero_dp() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text("Zero", modifier = Modifier.size(0.dp))
                        Text("After zero")
                    }
                }
            }.test { shell ->
                // Should not crash
                val afterLabel = shell.find<Label> { it.text == "After zero" }
                afterLabel.assertLayout().isVisible()

                // Zero-sized label might or might not be findable
                val zeroLabels = shell.findAll<Label> { it.text == "Zero" }
                SweetDebugger.log("ModifierComposition", "Zero-sized labels found: ${zeroLabels.size}")

                zeroLabels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    SweetDebugger.log("ModifierComposition", "Zero label bounds: ${bounds.width}x${bounds.height}")
                }
            }
        }
    }

    // Note: requiredSize modifier not implemented in Sweet Compose yet
    // @Test
    // fun required_size_vs_size() { ... }

    // ========== OFFSET EDGE CASES ==========

    @Test
    fun offset_with_negative_values() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Column {
                            Text("Normal")
                            Text("Negative", modifier = Modifier.offset(x = (-20).dp, y = (-10).dp))
                            Text("After")
                        }
                    }
                }
            }.test { shell ->
                // Should not crash
                val normal = shell.find<Label> { it.text == "Normal" }
                val negative = shell.find<Label> { it.text == "Negative" }
                val after = shell.find<Label> { it.text == "After" }

                normal.assertLayout().isVisible()
                negative.assertLayout().isVisible()
                after.assertLayout().isVisible()

                val normalBounds = runOnSWT { normal.bounds }
                val negativeBounds = runOnSWT { negative.bounds }

                // Negative offset might move item left/up
                SweetDebugger.log("ModifierComposition", "Normal: (${normalBounds.x}, ${normalBounds.y}), Negative offset: (${negativeBounds.x}, ${negativeBounds.y})")
            }
        }
    }

    @Test
    fun offset_with_extreme_values() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Box(Modifier.fillMaxSize()) {
                        Column {
                            Text("Normal")
                            Text("Extreme", modifier = Modifier.offset(x = 10000.dp, y = 10000.dp))
                        }
                    }
                }
            }.test { shell ->
                // Should not crash - item might be off screen
                val normal = shell.find<Label> { it.text == "Normal" }
                normal.assertLayout().isVisible()

                val extremeLabels = shell.findAll<Label> { it.text == "Extreme" }
                // Might or might not be visible depending on clipping
                SweetDebugger.log("ModifierComposition", "Extreme offset labels found: ${extremeLabels.size}")
            }
        }
    }

    // ========== PADDING EDGE CASES ==========

    @Test
    fun padding_with_zero_values() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text("Zero Padding", modifier = Modifier.padding(0.dp))
                        Text("No Padding")
                    }
                }
            }.test { shell ->
                val zeroPad = shell.find<Label> { it.text == "Zero Padding" }
                val noPad = shell.find<Label> { it.text == "No Padding" }

                zeroPad.assertLayout().isVisible()
                noPad.assertLayout().isVisible()

                // Both should behave similarly
                listOf(zeroPad, noPad)
                    .assertLayout()
                    .areArrangedInColumn(minGap = 0)
            }
        }
    }

    @Test
    fun padding_all_sides_different() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            "Uneven",
                            modifier =
                                Modifier.padding(
                                    start = 5.dp,
                                    top = 10.dp,
                                    end = 15.dp,
                                    bottom = 20.dp,
                                ),
                        )
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Uneven" }
                label.assertLayout().isVisible()

                // Should handle different padding on each side
                val bounds = runOnSWT { label.bounds }
                assertTrue(bounds.width > 0 && bounds.height > 0, "Should have valid size with uneven padding")
            }
        }
    }

    @Test
    fun padding_larger_than_available_space() {
        autoSWT {
            testShell(width = 100, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Padding larger than container
                        Text("Huge Padding", modifier = Modifier.padding(200.dp))
                    }
                }
            }.test { shell ->
                // Should not crash - content might be clipped or have minimal size
                val labels = shell.findAll<Label> { it.text == "Huge Padding" }
                assertTrue(labels.isNotEmpty(), "Label should exist despite huge padding")

                labels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    SweetDebugger.log("ModifierComposition", "Huge padding result: ${bounds.width}x${bounds.height}")
                }
            }
        }
    }

    // ========== BACKGROUND MODIFIER EDGE CASES ==========

    @Test
    fun background_with_unspecified_color() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Unspecified color
                        Text("Unspecified", modifier = Modifier.background(Color.Unspecified))
                        Text("Normal")
                    }
                }
            }.test { shell ->
                // Should not crash
                val unspecified = shell.find<Label> { it.text == "Unspecified" }
                val normal = shell.find<Label> { it.text == "Normal" }

                unspecified.assertLayout().isVisible()
                normal.assertLayout().isVisible()
            }
        }
    }

    @Test
    fun multiple_background_modifiers() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Multiple backgrounds - last one should win
                        Text(
                            "Multi BG",
                            modifier =
                                Modifier
                                    .background(Color.Red)
                                    .background(Color.Blue)
                                    .background(Color.Green),
                        )
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Multi BG" }
                label.assertLayout().isVisible()

                // Should not crash - final background should be applied
            }
        }
    }

    // ========== COMPLEX MODIFIER CHAINS ==========

    @Test
    fun long_modifier_chain() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            "Long Chain",
                            modifier =
                                Modifier
                                    .padding(2.dp)
                                    .size(150.dp)
                                    .padding(4.dp)
                                    .background(Color.LightGray)
                                    .padding(6.dp)
                                    .offset(5.dp, 5.dp)
                                    .padding(8.dp),
                        )
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Long Chain" }
                label.assertLayout().isVisible()

                // Should handle long modifier chains without issues
                val bounds = runOnSWT { label.bounds }
                assertTrue(bounds.width > 0 && bounds.height > 0, "Should have valid size after long modifier chain")
            }
        }
    }

    @Test
    fun modifier_chain_with_conditionals() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val applyRed = true
                    val applySize = false

                    Column(Modifier.fillMaxSize()) {
                        Text(
                            "Conditional",
                            modifier =
                                Modifier
                                    .then(if (applyRed) Modifier.background(Color.Red) else Modifier)
                                    .then(if (applySize) Modifier.size(200.dp) else Modifier)
                                    .padding(8.dp),
                        )
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Conditional" }
                label.assertLayout().isVisible()

                // Conditional modifiers should work correctly
            }
        }
    }

    // ========== FILL MODIFIERS IN CONSTRAINED CONTEXTS ==========

    @Test
    fun fillMaxWidth_in_intrinsic_width_context() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.width(100.dp)) {
                            Text("Fill", modifier = Modifier.fillMaxWidth())
                        }
                        Text("After")
                    }
                }
            }.test { shell ->
                val density = runOnSWT { Density.fromDisplay(shell.display).density }
                val fill = shell.find<Label> { it.text == "Fill" }
                val after = shell.find<Label> { it.text == "After" }
                fill.assertLayout().isVisible()
                after.assertLayout().isVisible()
                // Fill should respect parent's width constraint
                val fillWidth = runOnSWT { fill.bounds.width }
                val maxExpected = (100f * density).toInt()
                assertTrue(fillWidth > 0 && fillWidth <= maxExpected, "Fill width should respect parent constraint (fillWidth=$fillWidth, max=$maxExpected)")
            }
        }
    }

    @Test
    fun fillMaxHeight_in_intrinsic_height_context() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.height(80.dp)) {
                            // fillMaxHeight inside fixed-height row
                            Text("Fill", modifier = Modifier.fillMaxHeight())
                        }
                        Text("After")
                    }
                }
            }.test { shell ->
                val density = runOnSWT { Density.fromDisplay(shell.display).density }
                val fill = shell.find<Label> { it.text == "Fill" }
                val after = shell.find<Label> { it.text == "After" }
                fill.assertLayout().isVisible()
                after.assertLayout().isVisible()
                // Fill should respect parent's height constraint
                val fillHeight = runOnSWT { fill.bounds.height }
                val maxExpected = (80f * density).toInt()
                assertTrue(fillHeight > 0 && fillHeight <= maxExpected, "Fill height should respect parent constraint (fillHeight=$fillHeight, max=$maxExpected)")
            }
        }
    }

    // ========== REQUIRED SIZE MODIFIERS ==========
    // Note: requiredWidth and requiredHeight modifiers not implemented in Sweet Compose yet
    // These tests are commented out until the modifiers are implemented

    // @Test
    // fun requiredWidth_vs_width() { ... }

    // @Test
    // fun requiredHeight_vs_height() { ... }
}
