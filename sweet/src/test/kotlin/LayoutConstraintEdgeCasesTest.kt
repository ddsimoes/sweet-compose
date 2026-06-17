@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for layout constraint edge cases that could cause crashes or incorrect rendering.
 * Focuses on boundary conditions, zero values, and extreme cases.
 */
class LayoutConstraintEdgeCasesTest {
    // ========== ZERO WEIGHT TESTS ==========

    @Test
    fun row_single_child_with_zero_weight() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        Text("Zero", modifier = Modifier.weight(0f))
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Zero" }

                // Should still be visible (even with zero weight, item should get minimal space)
                label.assertLayout().isVisible()

                // Width should be minimal (based on intrinsic size)
                val width = runOnSWT { label.bounds.width }
                assertTrue(width >= 0, "Width should be non-negative, was $width")
            }
        }
    }

    @Test
    fun row_all_children_with_zero_weight() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        Text("A", modifier = Modifier.weight(0f))
                        Text("B", modifier = Modifier.weight(0f))
                        Text("C", modifier = Modifier.weight(0f))
                    }
                }
            }.test { shell ->
                val labels =
                    listOf(
                        shell.find<Label> { it.text == "A" },
                        shell.find<Label> { it.text == "B" },
                        shell.find<Label> { it.text == "C" },
                    )

                // All should be visible
                labels.assertLayout().areAllVisible()

                // Should be arranged in a row
                labels
                    .assertLayout()
                    .areArrangedInRow(minGap = 0)
                    .doNotOverlap()

                // All should have minimal (intrinsic) widths
                labels.forEach { label ->
                    val width = runOnSWT { label.bounds.width }
                    assertTrue(width >= 0, "Width should be non-negative for ${runOnSWT { label.text }}")
                }
            }
        }
    }

    @Test
    fun row_mixed_zero_and_positive_weights() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        Text("Zero1", modifier = Modifier.weight(0f))
                        Text("One", modifier = Modifier.weight(1f))
                        Text("Zero2", modifier = Modifier.weight(0f))
                        Text("Two", modifier = Modifier.weight(2f))
                    }
                }
            }.test { shell ->
                val zero1 = shell.find<Label> { it.text == "Zero1" }
                val one = shell.find<Label> { it.text == "One" }
                val zero2 = shell.find<Label> { it.text == "Zero2" }
                val two = shell.find<Label> { it.text == "Two" }

                // All visible
                listOf(zero1, one, zero2, two).assertLayout().areAllVisible()

                // Arranged left to right
                listOf(zero1, one, zero2, two)
                    .assertLayout()
                    .areArrangedInRow(minGap = 0)

                // Zero-weight items should be smaller than weighted items
                val zero1Width = runOnSWT { zero1.bounds.width }
                val oneWidth = runOnSWT { one.bounds.width }
                val zero2Width = runOnSWT { zero2.bounds.width }
                val twoWidth = runOnSWT { two.bounds.width }

                assertTrue(oneWidth > zero1Width, "Weighted item should be wider than zero-weight")
                assertTrue(twoWidth > zero2Width, "Weighted item should be wider than zero-weight")

                // Two should be approximately twice as wide as One
                val ratio = twoWidth.toDouble() / oneWidth.toDouble()
                assertTrue(ratio >= 1.5 && ratio <= 2.5, "Weight 2/1 ratio should be ~2, was $ratio")
            }
        }
    }

    @Test
    fun column_all_children_with_zero_weight() {
        autoSWT {
            testShell(width = 300, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text("A", modifier = Modifier.weight(0f))
                        Text("B", modifier = Modifier.weight(0f))
                        Text("C", modifier = Modifier.weight(0f))
                    }
                }
            }.test { shell ->
                val labels =
                    listOf(
                        shell.find<Label> { it.text == "A" },
                        shell.find<Label> { it.text == "B" },
                        shell.find<Label> { it.text == "C" },
                    )

                // All should be visible
                labels.assertLayout().areAllVisible()

                // Should be arranged in a column
                labels
                    .assertLayout()
                    .areArrangedInColumn(minGap = 0)
                    .doNotOverlap()
            }
        }
    }

    // ========== EXTREME CONSTRAINT TESTS ==========

    @Test
    fun row_with_very_small_container() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Very small container
                        Row(Modifier.width(10.dp).height(10.dp)) {
                            Text("A", modifier = Modifier.weight(1f))
                            Text("B", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }.test { shell ->
                val labels = shell.findAll<Label> { it.text == "A" || it.text == "B" }

                // Should handle gracefully - items might be clipped but should exist
                assertTrue(labels.size == 2, "Should have 2 labels even in tiny container")

                labels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    assertTrue(bounds.width >= 0, "Width should be non-negative")
                    assertTrue(bounds.height >= 0, "Height should be non-negative")
                }
            }
        }
    }

    @Test
    fun row_with_zero_size_container() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Zero-sized container
                        Row(Modifier.size(0.dp)) {
                            Text("A", modifier = Modifier.weight(1f))
                            Text("B", modifier = Modifier.weight(1f))
                        }
                        Text("Outside", modifier = Modifier.padding(8.dp))
                    }
                }
            }.test { shell ->
                // Should not crash - the zero-sized row might have invisible children
                val outside = shell.find<Label> { it.text == "Outside" }
                outside.assertLayout().isVisible()

                // Labels in zero-sized container might not be visible or might have zero bounds
                val labels = shell.findAll<Label> { it.text == "A" || it.text == "B" }
                labels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    // Just verify non-negative (might be zero)
                    assertTrue(bounds.width >= 0, "Width should be non-negative")
                    assertTrue(bounds.height >= 0, "Height should be non-negative")
                }
            }
        }
    }

    @Test
    fun column_with_extremely_large_content() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // Extremely large children
                        Text("Huge1", modifier = Modifier.height(10000.dp))
                        Text("Huge2", modifier = Modifier.height(10000.dp))
                    }
                }
            }.test { shell ->
                // Should not crash - items will be clipped
                val labels = shell.findAll<Label> { it.text.startsWith("Huge") }
                assertTrue(labels.size == 2, "Should have 2 huge labels")

                // Verify they exist and have bounds (even if clipped)
                labels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    assertTrue(bounds.height >= 0, "Height should be non-negative")
                }
            }
        }
    }

    // ========== FILL MODIFIER WITH EDGE CASES ==========

    @Test
    fun fillMaxWidth_with_zero_available_width() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.width(0.dp)) {
                        Text("Fill", modifier = Modifier.fillMaxWidth())
                    }
                }
            }.test { shell ->
                // Should not crash
                val labels = shell.findAll<Label> { it.text == "Fill" }
                assertTrue(labels.isNotEmpty(), "Label should exist")

                labels.forEach { label ->
                    val width = runOnSWT { label.bounds.width }
                    assertTrue(width >= 0, "Width should be non-negative")
                }
            }
        }
    }

    @Test
    fun fillMaxHeight_with_zero_available_height() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.height(0.dp)) {
                        Text("Fill", modifier = Modifier.fillMaxHeight())
                    }
                }
            }.test { shell ->
                // Should not crash
                val labels = shell.findAll<Label> { it.text == "Fill" }
                assertTrue(labels.isNotEmpty(), "Label should exist")

                labels.forEach { label ->
                    val height = runOnSWT { label.bounds.height }
                    assertTrue(height >= 0, "Height should be non-negative")
                }
            }
        }
    }

    // ========== ARRANGEMENT EDGE CASES ==========

    @Test
    fun row_spaceBetween_with_single_child() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Single")
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Single" }
                label.assertLayout().isVisible()

                // Single item in SpaceBetween - positioning is implementation-dependent
                // Main goal is to verify it doesn't crash
                val x = runOnSWT { label.bounds.x }
                assertTrue(x >= 0, "X position should be non-negative, was $x")
            }
        }
    }

    @Test
    fun row_spaceEvenly_with_single_child() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text("Single")
                    }
                }
            }.test { shell ->
                val label = shell.find<Label> { it.text == "Single" }
                label.assertLayout().isVisible()

                // Single item in SpaceEvenly might be centered
                val x = runOnSWT { label.bounds.x }
                assertTrue(x >= 0, "X position should be non-negative")
            }
        }
    }

    @Test
    fun column_spaceBetween_with_two_children() {
        autoSWT {
            testShell(width = 300, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Top")
                        Text("Bottom")
                    }
                }
            }.test { shell ->
                val top = shell.find<Label> { it.text == "Top" }
                val bottom = shell.find<Label> { it.text == "Bottom" }

                val composite = runOnSWT { shell.children.first() as Composite }

                // Top should be near top edge
                top.assertLayout().isVisible().isNearTopEdgeOf(composite, maxDistance = 10)

                // Bottom should be near bottom edge
                bottom.assertLayout().isVisible().isNearBottomEdgeOf(composite, maxDistance = 10)

                // Should not overlap
                top.assertLayout().doesNotOverlap(bottom)
            }
        }
    }

    @Test
    fun row_spacedBy_with_zero_spacing() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text("A")
                        Text("B")
                        Text("C")
                    }
                }
            }.test { shell ->
                val labels =
                    listOf(
                        shell.find<Label> { it.text == "A" },
                        shell.find<Label> { it.text == "B" },
                        shell.find<Label> { it.text == "C" },
                    )

                labels
                    .assertLayout()
                    .areAllVisible()
                    .areArrangedInRow(minGap = 0)
                    .doNotOverlap()

                // With zero spacing, items should be adjacent (right edge of A touches left edge of B)
                val aRight = runOnSWT { labels[0].bounds.x + labels[0].bounds.width }
                val bLeft = runOnSWT { labels[1].bounds.x }
                val gap = bLeft - aRight
                assertTrue(gap >= 0, "Gap should be non-negative, was $gap")
            }
        }
    }

    @Test
    fun row_spaceEvenly_with_items_too_large_to_fit() {
        autoSWT {
            testShell(width = 100, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text("VeryLongText1", modifier = Modifier.width(80.dp))
                        Text("VeryLongText2", modifier = Modifier.width(80.dp))
                        Text("VeryLongText3", modifier = Modifier.width(80.dp))
                    }
                }
            }.test { shell ->
                // Should not crash even when items don't fit
                val labels = shell.findAll<Label> { it.text.startsWith("VeryLongText") }
                assertEquals(3, labels.size, "Should have 3 labels")

                // MPP RowColumnMeasurePolicy semantics: each non-weighted child is
                // measured against the REMAINING main-axis space, and Modifier.width
                // enforces incoming constraints. In a 100px Row, three width(80.dp)
                // children must not overflow — each later child gets less space.

                // No child may extend past the Row's right edge.
                labels.forEach { label ->
                    val bounds = runOnSWT { label.bounds }
                    assertTrue(bounds.x + bounds.width <= 100, "Child must not overflow the Row: $bounds")
                }

                // Each child's width must be ≤ the previous child's (remaining-space shrinkage).
                val widths = labels.map { label -> runOnSWT { label.bounds.width } }
                for (i in 1 until widths.size) {
                    assertTrue(widths[i] <= widths[i - 1], "Each child must not be wider than the previous: $widths")
                }

                // The last child must be at most the arrangement spacing (0 for SpaceEvenly) —
                // if any space remains after previous children, the last gets it; otherwise 0.
                assertTrue(widths.last() == 0, "Last child in overloaded Row must be zero-width: $widths")
            }
        }
    }

    // ========== EMPTY CONTAINER TESTS ==========

    @Test
    fun row_with_no_children() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().height(50.dp)) {
                            // Empty row
                        }
                        Text("After empty row")
                    }
                }
            }.test { shell ->
                // Should not crash
                val afterLabel = shell.find<Label> { it.text == "After empty row" }
                afterLabel.assertLayout().isVisible()
            }
        }
    }

    @Test
    fun column_with_no_children() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxHeight().width(50.dp)) {
                            // Empty column
                        }
                        Text("After empty column")
                    }
                }
            }.test { shell ->
                // Should not crash
                val afterLabel = shell.find<Label> { it.text == "After empty column" }
                afterLabel.assertLayout().isVisible()
            }
        }
    }

    @Test
    fun box_with_no_children() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(50.dp)) {
                            // Empty box
                        }
                        Text("After empty box")
                    }
                }
            }.test { shell ->
                // Should not crash
                val afterLabel = shell.find<Label> { it.text == "After empty box" }
                afterLabel.assertLayout().isVisible()
            }
        }
    }

    // ========== DEEPLY NESTED LAYOUTS ==========

    @Test
    fun deeply_nested_row_column_box() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    DeepNesting()
                }
            }.test { shell ->
                // Should not crash or cause stack overflow
                val deepLabel = shell.find<Label> { it.text == "Deep" }
                deepLabel.assertLayout().isVisible()
            }
        }
    }

    @Composable
    private fun DeepNesting() {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) {
                                Column(Modifier.fillMaxSize()) {
                                    Row(Modifier.fillMaxWidth()) {
                                        Box(Modifier.weight(1f)) {
                                            Text("Deep")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== WEIGHT WITH FIXED SIZE EDGE CASES ==========

    @Test
    fun row_weight_with_explicit_width() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize()) {
                        // weight and explicit width - width should win
                        Text("Fixed", modifier = Modifier.weight(1f).width(100.dp))
                        Text("Flexible", modifier = Modifier.weight(1f))
                    }
                }
            }.test { shell ->
                val fixed = shell.find<Label> { it.text == "Fixed" }
                val flexible = shell.find<Label> { it.text == "Flexible" }

                fixed.assertLayout().isVisible()
                flexible.assertLayout().isVisible()

                // Both should be visible and arranged
                listOf(fixed, flexible)
                    .assertLayout()
                    .areArrangedInRow(minGap = 0)
            }
        }
    }

    @Test
    fun column_weight_with_explicit_height() {
        autoSWT {
            testShell(width = 300, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        // weight and explicit height - height should win
                        Text("Fixed", modifier = Modifier.weight(1f).height(50.dp))
                        Text("Flexible", modifier = Modifier.weight(1f))
                    }
                }
            }.test { shell ->
                val fixed = shell.find<Label> { it.text == "Fixed" }
                val flexible = shell.find<Label> { it.text == "Flexible" }

                fixed.assertLayout().isVisible()
                flexible.assertLayout().isVisible()

                // Both should be visible and arranged
                listOf(fixed, flexible)
                    .assertLayout()
                    .areArrangedInColumn(minGap = 0)
            }
        }
    }
}
