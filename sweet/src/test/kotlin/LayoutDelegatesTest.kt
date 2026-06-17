import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.internal.getDisplayDensity
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Button as SWTButton

/**
 * Comprehensive tests for the new unified layout delegates
 */
class LayoutDelegatesTest {
    // ========== ROW DELEGATE TESTS ==========

    @Test
    fun row_weight_distribution_equal_weights() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("A", modifier = Modifier.weight(1f))
                        Text("B", modifier = Modifier.weight(1f))
                        Text("C", modifier = Modifier.weight(1f))
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

                // Should be arranged in a row with roughly equal widths
                labels
                    .assertLayout()
                    .areArrangedInRow(minGap = 0)
                    .doNotOverlap()

                // Check that they're positioned left-to-right
                val positions = labels.map { runOnSWT { it.bounds.x } }
                assertTrue(positions[1] > positions[0], "B should be right of A")
                assertTrue(positions[2] > positions[1], "C should be right of B")
            }
        }
    }

    @Test
    fun row_weight_distribution_different_weights() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Small", modifier = Modifier.weight(1f))
                        Text("Large", modifier = Modifier.weight(3f))
                    }
                }
            }.test { shell ->
                val small = shell.find<Label> { it.text == "Small" }
                val large = shell.find<Label> { it.text == "Large" }

                small.assertLayout().isVisible().isLeftOf(large)
                large.assertLayout().isVisible()

                // Large should be approximately 3x wider than small
                val smallWidth = runOnSWT { small.bounds.width }
                val largeWidth = runOnSWT { large.bounds.width }
                val ratio = largeWidth.toDouble() / smallWidth.toDouble()
                assertTrue(ratio >= 2.5 && ratio <= 3.5, "Large/Small width ratio should be ~3, was $ratio")
            }
        }
    }

    @Test
    fun row_mixed_weighted_and_fixed_children() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Fixed") // No weight = fixed size
                        Text("Weighted", modifier = Modifier.weight(1f))
                        Button(onClick = {}) { Text("Button") } // No weight = fixed size
                    }
                }
            }.test { shell ->
                val fixed = shell.find<Label> { it.text == "Fixed" }
                val weighted = shell.find<Label> { it.text == "Weighted" }
                val button = shell.find<SWTButton> { it.text == "Button" }

                // All visible and arranged left-to-right
                listOf(fixed, weighted, button)
                    .assertLayout()
                    .areAllVisible()
                    .areArrangedInRow(minGap = 0)

                // Weighted should be larger than others
                val fixedWidth = runOnSWT { fixed.bounds.width }
                val weightedWidth = runOnSWT { weighted.bounds.width }
                assertTrue(weightedWidth > fixedWidth, "Weighted should be wider than fixed")
            }
        }
    }

    @Test
    fun row_horizontal_arrangements() {
        data class TestCase(
            val arrangement: Arrangement.Horizontal,
            val description: String,
        )

        val testCases =
            listOf(
                TestCase(Arrangement.Start, "Start alignment"),
                TestCase(Arrangement.End, "End alignment"),
                TestCase(Arrangement.Center, "Center alignment"),
                TestCase(Arrangement.SpaceEvenly, "Space evenly"),
                TestCase(Arrangement.SpaceBetween, "Space between"),
            )
        testCases.forEach { testCase ->
            autoSWT {
                testShell(width = 400, height = 200) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalArrangement = testCase.arrangement,
                        ) {
                            Text("A")
                            Text("B")
                        }
                    }
                }.test { shell ->
                    val containerComposite = runOnSWT { shell.children.first() as Composite }
                    val labelA = shell.find<Label> { it.text == "A" }
                    val labelB = shell.find<Label> { it.text == "B" }

                    // Basic sanity checks
                    labelA.assertLayout().isVisible().isWithin(containerComposite)
                    labelB.assertLayout().isVisible().isWithin(containerComposite)
                    labelA.assertLayout().isLeftOf(labelB)

                    // Arrangement-specific checks
                    when (testCase.arrangement) {
                        Arrangement.Start -> {
                            // Should be near left edge
                            labelA.assertLayout().isNearLeftEdgeOf(containerComposite, maxDistance = 20)
                        }

                        Arrangement.End -> {
                            // B should be near right edge
                            labelB.assertLayout().isNearRightEdgeOf(containerComposite, maxDistance = 20)
                        }

                        Arrangement.Center -> {
                            // Both should be roughly centered
                            val containerBounds = runOnSWT { containerComposite.visibleBounds() }
                            val centerX = containerBounds.x + containerBounds.width / 2
                            val labelAX = runOnSWT { labelA.bounds.x + labelA.bounds.width / 2 }
                            val labelBX = runOnSWT { labelB.bounds.x + labelB.bounds.width / 2 }
                            val midPoint = (labelAX + labelBX) / 2
                            assertTrue(kotlin.math.abs(midPoint - centerX) <= 30, "Labels should be centered")
                        }

                        else -> {
                            // For other arrangements (SpaceEvenly, etc.), just verify basic layout
                            // This is a general test, so we don't need specific assertions for all arrangements
                        }
                    }
                }
            }
        }
    }

    // ========== COLUMN DELEGATE TESTS ==========

    @Test
    fun column_weight_distribution_equal_weights() {
        autoSWT {
            testShell(width = 200, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Top", modifier = Modifier.weight(1f))
                        Text("Middle", modifier = Modifier.weight(1f))
                        Text("Bottom", modifier = Modifier.weight(1f))
                    }
                }
            }.test { shell ->
                val labels =
                    listOf(
                        shell.find<Label> { it.text == "Top" },
                        shell.find<Label> { it.text == "Middle" },
                        shell.find<Label> { it.text == "Bottom" },
                    )

                // All should be visible and arranged vertically
                labels
                    .assertLayout()
                    .areAllVisible()
                    .areArrangedInColumn(minGap = 0)

                // Check vertical ordering
                val positions = labels.map { runOnSWT { it.bounds.y } }
                assertTrue(positions[1] > positions[0], "Middle should be below Top")
                assertTrue(positions[2] > positions[1], "Bottom should be below Middle")
            }
        }
    }

    @Test
    fun column_vertical_arrangements() {
        data class TestCase(
            val arrangement: Arrangement.Vertical,
            val description: String,
        )

        val testCases =
            listOf(
                TestCase(Arrangement.Top, "Top alignment"),
                TestCase(Arrangement.Bottom, "Bottom alignment"),
                TestCase(Arrangement.Center, "Center alignment"),
            )

        testCases.forEach { testCase ->
            autoSWT {
                testShell(width = 200, height = 400) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = testCase.arrangement,
                        ) {
                            Text("First")
                            Text("Second")
                        }
                    }
                }.test { shell ->
                    val containerComposite = runOnSWT { shell.children.first() as Composite }
                    val first = shell.find<Label> { it.text == "First" }
                    val second = shell.find<Label> { it.text == "Second" }

                    first.assertLayout().isVisible().isWithin(containerComposite)
                    second.assertLayout().isVisible().isWithin(containerComposite)
                    first.assertLayout().isAbove(second)

                    when (testCase.arrangement) {
                        Arrangement.Top -> {
                            first.assertLayout().isNearTopEdgeOf(containerComposite, maxDistance = 20)
                        }

                        Arrangement.Bottom -> {
                            second.assertLayout().isNearBottomEdgeOf(containerComposite, maxDistance = 20)
                        }

                        Arrangement.Center -> {
                            val containerBounds = runOnSWT { containerComposite.visibleBounds() }
                            val centerY = containerBounds.y + containerBounds.height / 2
                            val firstY = runOnSWT { first.bounds.y + first.bounds.height / 2 }
                            val secondY = runOnSWT { second.bounds.y + second.bounds.height / 2 }
                            val midPoint = (firstY + secondY) / 2
                            val distance = kotlin.math.abs(midPoint - centerY)
                            assertTrue(
                                distance <= 60,
                                "Labels should be centered vertically: distance=$distance, centerY=$centerY, midPoint=$midPoint",
                            )
                        }

                        else -> { }
                    }
                }
            }
        }

        @Test
        fun column_spacedby_arrangement() {
            autoSWT {
                testShell(width = 200, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("A")
                            Text("B")
                            Text("C")
                        }
                    }
                }.test { shell ->
                    val labelA = shell.find<Label> { it.text == "A" }
                    val labelB = shell.find<Label> { it.text == "B" }
                    val labelC = shell.find<Label> { it.text == "C" }

                    // spacedBy(16.dp) → gap is 16dp in px, which scales with display density.
                    val d = runOnSWT { getDisplayDensity().density }
                    val gap = (16 * d).toInt()
                    val tol = (6 * d).toInt()
                    labelA.assertLayout().isVisible().isAbove(labelB, gap = gap, tolerance = tol)
                    labelB.assertLayout().isVisible().isAbove(labelC, gap = gap, tolerance = tol)
                }
            }
        }

        // ========== BOX DELEGATE TESTS ==========

        @Test
        fun box_alignment_variants() {
            data class TestCase(
                val alignment: Alignment,
                val description: String,
            )

            val testCases =
                listOf(
                    TestCase(Alignment.TopStart, "TopStart"),
                    TestCase(Alignment.TopEnd, "TopEnd"),
                    TestCase(Alignment.BottomStart, "BottomStart"),
                    TestCase(Alignment.BottomEnd, "BottomEnd"),
                    TestCase(Alignment.Center, "Center"),
                )

            testCases.forEach { testCase ->
                autoSWT {
                    testShell(width = 300, height = 200) {
                        val composite = Composite(this, SWT.NONE)
                        composite.layout = FillLayout()
                        composite.embedCompose {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentAlignment = testCase.alignment,
                            ) {
                                Text("Aligned")
                            }
                        }
                    }.test { shell ->
                        val containerComposite = runOnSWT { shell.children.first() as Composite }
                        val label = shell.find<Label> { it.text == "Aligned" }

                        label.assertLayout().isVisible().isWithin(containerComposite)

                        when (testCase.alignment) {
                            Alignment.TopStart -> {
                                label
                                    .assertLayout()
                                    .isNearTopEdgeOf(containerComposite, maxDistance = 10)
                                    .isNearLeftEdgeOf(containerComposite, maxDistance = 10)
                            }

                            Alignment.TopEnd -> {
                                label
                                    .assertLayout()
                                    .isNearTopEdgeOf(containerComposite, maxDistance = 10)
                                    .isNearRightEdgeOf(containerComposite, maxDistance = 10)
                            }

                            Alignment.BottomStart -> {
                                label
                                    .assertLayout()
                                    .isNearBottomEdgeOf(containerComposite, maxDistance = 10)
                                    .isNearLeftEdgeOf(containerComposite, maxDistance = 10)
                            }

                            Alignment.BottomEnd -> {
                                label
                                    .assertLayout()
                                    .isNearBottomEdgeOf(containerComposite, maxDistance = 10)
                                    .isNearRightEdgeOf(containerComposite, maxDistance = 10)
                            }

                            Alignment.Center -> {
                                label.assertLayout().isCenteredIn(containerComposite, tolerance = 10)
                            }
                        }
                    }
                }
            }
        }

        @Test
        fun box_stacked_children_all_visible() {
            autoSWT {
                testShell(width = 200, height = 150) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Box(Modifier.fillMaxSize().padding(8.dp)) {
                            Text("Stacked1")
                            Text("Stacked2")
                        }
                    }
                }.test { shell ->
                    val stacked1 = shell.find<Label> { it.text == "Stacked1" }
                    val stacked2 = shell.find<Label> { it.text == "Stacked2" }

                    // Both should be visible even though they're stacked
                    stacked1.assertLayout().isVisible()
                    stacked2.assertLayout().isVisible()

                    // They should be at the same position (both at TopStart by default)
                    val bounds1 = runOnSWT { stacked1.bounds }
                    val bounds2 = runOnSWT { stacked2.bounds }
                    // In Box layout, children can overlap, so just check they're both visible
                    assertTrue(bounds1.width > 0 && bounds1.height > 0, "Stacked1 should have positive dimensions")
                    assertTrue(bounds2.width > 0 && bounds2.height > 0, "Stacked2 should have positive dimensions")
                }
            }
        }

        // ========== EDGE CASES AND ERROR CONDITIONS ==========

        @Test
        fun empty_containers_handle_gracefully() {
            autoSWT {
                testShell(width = 200, height = 150) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(Modifier.fillMaxSize().padding(8.dp)) {
                            // Empty column
                        }
                    }
                }.test { shell ->
                    // Should not crash and shell should be visible
                    val containerComposite = runOnSWT { shell.children.first() as Composite }
                    assertTrue(runOnSWT { containerComposite.visible }, "Container should be visible even when empty")
                }
            }
        }

        @Test
        fun single_child_layouts_work_correctly() {
            autoSWT {
                testShell(width = 200, height = 150) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Row(Modifier.fillMaxSize().padding(8.dp)) {
                            Text("Only Child")
                        }
                    }
                }.test { shell ->
                    val label = shell.find<Label> { it.text == "Only Child" }
                    label.assertLayout().isVisible()
                }
            }
        }

        @Test
        fun nested_layouts_work_correctly() {
            autoSWT {
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(Modifier.fillMaxSize().padding(8.dp)) {
                            Text("Header")
                            Row(Modifier.fillMaxWidth()) {
                                Text("Left", modifier = Modifier.weight(1f))
                                Text("Right", modifier = Modifier.weight(1f))
                            }
                            Text("Footer")
                        }
                    }
                }.test { shell ->
                    val header = shell.find<Label> { it.text == "Header" }
                    val left = shell.find<Label> { it.text == "Left" }
                    val right = shell.find<Label> { it.text == "Right" }
                    val footer = shell.find<Label> { it.text == "Footer" }

                    // All should be visible
                    listOf(header, left, right, footer).assertLayout().areAllVisible()

                    // Check structure: Header above row, row items side-by-side, footer below
                    header.assertLayout().isAbove(left).isAbove(right)
                    left.assertLayout().isLeftOf(right).isAbove(footer)
                    right.assertLayout().isAbove(footer)
                }
            }
        }

        @Test
        fun column_per_child_horizontal_alignment_overrides_parent() {
            autoSWT {
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Start", modifier = Modifier.align(Alignment.Start))
                            Text("Center")
                            Text("End", modifier = Modifier.align(Alignment.End))
                        }
                    }
                }.test { shell ->
                    val start = shell.find<Label> { it.text == "Start" }
                    val center = shell.find<Label> { it.text == "Center" }
                    val end = shell.find<Label> { it.text == "End" }

                    val startX = runOnSWT { start.bounds.x }
                    val centerX = runOnSWT { center.bounds.x }
                    val endX = runOnSWT { end.bounds.x }

                    assertTrue(startX < centerX, "Start should be left of Center")
                    assertTrue(centerX < endX, "Center should be left of End")
                }
            }
        }

        @Test
        fun row_per_child_vertical_alignment_overrides_parent() {
            autoSWT {
                testShell(width = 400, height = 200) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Top", modifier = Modifier.align(Alignment.Top))
                            Text("Center")
                            Text("Bottom", modifier = Modifier.align(Alignment.Bottom))
                        }
                    }
                }.test { shell ->
                    val top = shell.find<Label> { it.text == "Top" }
                    val center = shell.find<Label> { it.text == "Center" }
                    val bottom = shell.find<Label> { it.text == "Bottom" }

                    val topY = runOnSWT { top.bounds.y }
                    val centerY = runOnSWT { center.bounds.y }
                    val bottomY = runOnSWT { bottom.bounds.y }

                    assertTrue(topY <= centerY, "Top should be at or above Center")
                    assertTrue(centerY <= bottomY, "Center should be at or above Bottom")
                }
            }
        }

        @Test
        fun row_alignByBlock_aligns_vertical_centers() {
            autoSWT {
                testShell(width = 400, height = 200) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 60.dp, height = 40.dp)
                                        .alignBy { it.measuredHeight / 2 },
                            ) {
                                Text("Short")
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 60.dp, height = 80.dp)
                                        .alignBy { it.measuredHeight / 2 },
                            ) {
                                Text("Tall")
                            }
                        }
                    }
                }.test { shell ->
                    val short = shell.find<Label> { it.text == "Short" }
                    val tall = shell.find<Label> { it.text == "Tall" }
                    val shortBounds = runOnSWT { short.bounds }
                    val tallBounds = runOnSWT { tall.bounds }
                    val firstCenterY = shortBounds.y + shortBounds.height / 2
                    val secondCenterY = tallBounds.y + tallBounds.height / 2
                    val deltaY = kotlin.math.abs(firstCenterY - secondCenterY)
                    val minHeight = minOf(shortBounds.height, tallBounds.height)
                    val tolerance = maxOf(3, (minHeight * 5) / 100)
                    assertTrue(
                        deltaY <= tolerance,
                        "Boxes aligned with alignBy block should have matching vertical centers (delta=$deltaY, tolerance=$tolerance)",
                    )
                }
            }
        }

        @Test
        fun column_alignByBlock_aligns_horizontal_centers() {
            autoSWT {
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 40.dp, height = 40.dp)
                                        .alignBy { it.measuredWidth / 2 },
                            ) {
                                Text("Narrow")
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 80.dp, height = 40.dp)
                                        .alignBy { it.measuredWidth / 2 },
                            ) {
                                Text("Wide")
                            }
                        }
                    }
                }.test { shell ->
                    val narrow = shell.find<Label> { it.text == "Narrow" }
                    val wide = shell.find<Label> { it.text == "Wide" }
                    val narrowBounds = runOnSWT { narrow.bounds }
                    val wideBounds = runOnSWT { wide.bounds }
                    val firstCenterX = narrowBounds.x + narrowBounds.width / 2
                    val secondCenterX = wideBounds.x + wideBounds.width / 2
                    val deltaX = kotlin.math.abs(firstCenterX - secondCenterX)
                    val minWidth = minOf(narrowBounds.width, wideBounds.width)
                    val tolerance = maxOf(3, (minWidth * 5) / 100)
                    assertTrue(
                        deltaX <= tolerance,
                        "Boxes aligned with alignBy block should have matching horizontal centers (delta=$deltaX, tolerance=$tolerance)",
                    )
                }
            }
        }

        @Test
        fun row_alignByBaseline_matches_alignByFirstBaseline() {
            autoSWT {
                testShell(width = 400, height = 200) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            // Both children participate in the same baseline group, but
                            // one uses alignByBaseline() and the other alignBy(FirstBaseline).
                            Text("Left", modifier = Modifier.alignByBaseline())
                            Text("Right", modifier = Modifier.alignBy(androidx.compose.foundation.layout.FirstBaseline))
                        }
                    }
                }.test { shell ->
                    val left = shell.find<Label> { it.text == "Left" }
                    val right = shell.find<Label> { it.text == "Right" }

                    val leftY = runOnSWT { left.bounds.y }
                    val rightY = runOnSWT { right.bounds.y }

                    // Both modifiers should map to the same baseline alignment behavior.
                    val delta = kotlin.math.abs(leftY - rightY)
                    assertTrue(
                        delta <= 1,
                        "alignByBaseline() and alignBy(FirstBaseline) should produce matching vertical positions (delta=$delta)",
                    )
                }
            }
        }
    }
}
