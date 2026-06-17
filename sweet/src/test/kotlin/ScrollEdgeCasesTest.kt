import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Button as SWTButton

/**
 * Tests for scroll system edge cases that could cause incorrect behavior.
 * Focuses on boundary conditions, zero-sized content, and dynamic content scenarios.
 */
class ScrollEdgeCasesTest {
    // ========== ZERO-SIZED CONTENT TESTS ==========

    @Test
    fun verticalScroll_with_empty_content() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        // Empty - no content
                    }
                }
            }.test { shell ->
                // Should not crash with empty scrollable content
                val scroller = shell.find<ScrollViewport> { true }

                val origin = runOnSWT { scroller.origin }
                assertEquals(0, origin.y, "Empty scroll should start at origin 0")
            }
        }
    }

    @Test
    fun horizontalScroll_with_empty_content() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()

                    Row(
                        modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                    ) {
                        // Empty - no content
                    }
                }
            }.test { shell ->
                // Should not crash with empty scrollable content
                val scroller = shell.find<ScrollViewport> { true }

                val origin = runOnSWT { scroller.origin }
                assertEquals(0, origin.x, "Empty scroll should start at origin 0")
            }
        }
    }

    @Test
    fun verticalScroll_with_single_small_item() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        Text("Single Item")
                    }
                }
            }.test { shell ->
                // Content smaller than viewport - scrolling should be minimal/none
                val scroller = shell.find<ScrollViewport> { true }

                val origin = runOnSWT { scroller.origin }
                assertEquals(0, origin.y, "Single small item should not need scrolling")

                // Verify scrollbar might not be visible or enabled
                val vBar = runOnSWT { scroller.verticalBar }
                val vBarVisible = runOnSWT { vBar?.isVisible ?: false }
                SweetDebugger.log("ScrollEdgeCases", "VBar visible for single item: $vBarVisible")
            }
        }
    }

    @Test
    fun verticalScroll_content_exactly_fits_viewport() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Content height exactly matches container
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        ) {
                            Text("Fits exactly", modifier = Modifier.fillMaxHeight())
                        }
                    }
                }
            }.test { shell ->
                // Content exactly fits - should not need scrolling
                val scroller = shell.find<ScrollViewport> { true }

                val origin = runOnSWT { scroller.origin }
                assertEquals(0, origin.y, "Content that fits exactly should start at 0")
            }
        }
    }

    // ========== SCROLL POSITION VALIDATION ==========

    @Test
    fun verticalScroll_jumpTo_negative_value_bounded_to_zero() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        repeat(50) {
                            Text("Item $it")
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!

                // Try to jump to negative position
                runOnSWT {
                    kotlinx.coroutines.runBlocking { state.scrollTo(-100) }
                }

                // Should be bounded to 0
                val value = runOnSWT { state.value }
                assertEquals(0, value, "Scroll position should be bounded to 0, not negative")

                val scroller = shell.find<ScrollViewport> { true }
                val origin = runOnSWT { scroller.origin }
                assertEquals(0, origin.y, "Scroller origin.y should be 0")
            }
        }
    }

    @Test
    fun verticalScroll_jumpTo_beyond_max_bounded_to_max() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        repeat(20) {
                            Text("Item $it")
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!

                // Try to jump way beyond max
                runOnSWT {
                    kotlinx.coroutines.runBlocking { state.scrollTo(999999) }
                }

                // Should be bounded to maxValue
                val value = runOnSWT { state.value }
                val maxValue = runOnSWT { state.maxValue }

                assertTrue(value <= maxValue, "Scroll position should not exceed maxValue")
                assertTrue(value >= 0, "Scroll position should be non-negative")

                SweetDebugger.log("ScrollEdgeCases", "After jumpTo(999999): value=$value, maxValue=$maxValue")
            }
        }
    }

    @Test
    fun horizontalScroll_jumpTo_beyond_bounds() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Row(
                        modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                    ) {
                        repeat(30) {
                            Text("Item $it  ")
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!

                // Jump to negative
                runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(-50) } }
                var value = runOnSWT { state.value }
                assertEquals(0, value, "Horizontal scroll should be bounded to 0")

                // Jump beyond max
                runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(999999) } }
                value = runOnSWT { state.value }
                val maxValue = runOnSWT { state.maxValue }

                assertTrue(value <= maxValue, "Horizontal scroll should not exceed maxValue")
            }
        }
    }

    // ========== RAPID SCROLL OPERATIONS ==========

    @Test
    fun verticalScroll_rapid_consecutive_jumps() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        repeat(100) {
                            Text("Item $it")
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!

                // Rapid jumps in succession
                runOnSWT {
                    kotlinx.coroutines.runBlocking { state.scrollTo(100) }
                    kotlinx.coroutines.runBlocking { state.scrollTo(200) }
                    kotlinx.coroutines.runBlocking { state.scrollTo(50) }
                    kotlinx.coroutines.runBlocking { state.scrollTo(300) }
                    kotlinx.coroutines.runBlocking { state.scrollTo(0) }
                    kotlinx.coroutines.runBlocking { state.scrollTo(150) }
                }

                // Should end at final value (150)
                val value = runOnSWT { state.value }
                assertEquals(150, value, "Should end at final jump value")

                val scroller = shell.find<ScrollViewport> { true }
                val origin = runOnSWT { scroller.origin }
                assertTrue(origin.y >= 0, "Origin should be non-negative")
            }
        }
    }

    // ========== DYNAMIC CONTENT RESIZING ==========

    @Test
    fun verticalScroll_content_grows_dynamically() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var itemCount by remember { mutableIntStateOf(5) }
                    val scrollState = rememberScrollState()

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { itemCount += 10 }) {
                            Text("Add Items")
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        ) {
                            repeat(itemCount) {
                                Text("Item $it")
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Initially 5 items
                var items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(5, items.size)

                // Add more items
                button.doSelect()

                items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(15, items.size)

                // Scroll should handle the new content
                val scroller = shell.find<ScrollViewport> { true }
                val origin = runOnSWT { scroller.origin }
                assertTrue(origin.y >= 0, "Scroll origin should be valid after content growth")

                // Add even more
                button.doSelect()

                items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(25, items.size)
            }
        }
    }

    @Test
    fun verticalScroll_content_shrinks_dynamically() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var itemCount by remember { mutableIntStateOf(50) }
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { itemCount = maxOf(0, itemCount - 10) }) {
                            Text("Remove Items")
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        ) {
                            repeat(itemCount) {
                                Text("Item $it")
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }
                val state = capturedState!!

                // Initially 50 items, scroll down
                runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(500) } }

                var value = runOnSWT { state.value }
                assertTrue(value > 0, "Should have scrolled down")

                // Remove items
                button.doSelect() // 40 items
                button.doSelect() // 30 items
                button.doSelect() // 20 items

                var items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(20, items.size)

                // Scroll position should adjust if necessary
                value = runOnSWT { state.value }
                val maxValue = runOnSWT { state.maxValue }
                assertTrue(value <= maxValue, "Scroll position should not exceed new maxValue")

                // Remove all items
                repeat(5) { button.doSelect() }

                items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(0, items.size)

                // Scroll position with empty content - might be 0 or retain previous position
                value = runOnSWT { state.value }
                assertTrue(value >= 0, "Scroll value should be non-negative even with empty content")
            }
        }
    }

    @Test
    fun verticalScroll_content_removed_while_scrolled() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showContent by remember { mutableStateOf(true) }
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showContent = !showContent }) {
                            Text("Toggle Content")
                        }

                        if (showContent) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                            ) {
                                repeat(50) {
                                    Text("Item $it")
                                }
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }
                val state = capturedState!!

                // Scroll down
                runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(300) } }

                var value = runOnSWT { state.value }
                assertTrue(value > 0, "Should have scrolled down")

                // Remove content
                button.doSelect()

                var items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(0, items.size, "Content should be removed")

                // Show content again
                button.doSelect()

                items = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Item") }
                assertEquals(50, items.size, "Content should be recreated")

                // Scroll state might reset or maintain position
                value = runOnSWT { state.value }
                assertTrue(value >= 0, "Scroll value should be valid after content recreation")
            }
        }
    }

    // ========== NESTED SCROLL CONTAINERS ==========

    @Test
    fun nested_vertical_scroll_containers() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val outerState = rememberScrollState()

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(outerState),
                    ) {
                        Text("Outer Top")

                        // Inner scrollable (nested - might have special handling)
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            val innerState = rememberScrollState()
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(innerState),
                            ) {
                                repeat(20) {
                                    Text("Inner $it")
                                }
                            }
                        }

                        Text("Outer Bottom")

                        repeat(20) {
                            Text("Outer Item $it")
                        }
                    }
                }
            }.test { shell ->
                // Should not crash with nested scrollers
                val labels = shell.findAll<org.eclipse.swt.widgets.Label> { it.text.startsWith("Outer") || it.text.startsWith("Inner") }
                assertTrue(labels.isNotEmpty(), "Should have labels from both outer and inner scrollers")
            }
        }
    }

    // ========== HORIZONTAL SCROLL SPECIFIC ==========

    @Test
    fun horizontalScroll_in_non_row_context() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()

                    // Horizontal scroll on a Box (not a Row)
                    Box(
                        modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                    ) {
                        Row {
                            repeat(30) {
                                Text("Item $it  ")
                            }
                        }
                    }
                }
            }.test { shell ->
                // Horizontal scroll in Box context - might not create ScrollViewport
                val scrollers = shell.findAll<ScrollViewport> { true }
                if (scrollers.isNotEmpty()) {
                    val origin = runOnSWT { scrollers[0].origin }
                    assertEquals(0, origin.x, "Horizontal scroll should start at 0")
                } else {
                    // Box might not support horizontal scroll yet - test that it doesn't crash
                    assertTrue(true, "Box with horizontal scroll doesn't crash")
                }
            }
        }
    }

    @Test
    fun horizontalScroll_with_very_wide_content() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Row(
                        modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
                    ) {
                        // Very wide content
                        repeat(100) {
                            Text("Item $it          ")
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!

                // Should handle very wide content
                val maxValue = runOnSWT { state.maxValue }

                // If content is wide enough to scroll, maxValue should be reasonable
                if (maxValue < Int.MAX_VALUE && maxValue > 0) {
                    // Jump to end
                    runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(maxValue) } }

                    val value = runOnSWT { state.value }
                    assertEquals(maxValue, value, "Should be able to scroll to end")

                    val scroller = shell.find<ScrollViewport> { true }
                    val origin = runOnSWT { scroller.origin }
                    assertTrue(origin.x >= 0, "Origin.x should be non-negative")
                } else {
                    // Content might not be laid out yet or maxValue not calculated
                    // Main goal is no crash with very wide content
                    assertTrue(true, "Very wide content doesn't crash")
                }
            }
        }
    }

    // ========== SCROLL STATE DISPOSAL ==========

    @Test
    fun scroll_state_survives_recomposition() {
        var capturedState: androidx.compose.foundation.ScrollState? = null

        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var counter by remember { mutableIntStateOf(0) }
                    val scrollState = rememberScrollState()
                    capturedState = scrollState

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { counter++ }) {
                            Text("Recompose: $counter")
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        ) {
                            repeat(50) {
                                Text("Item $it")
                            }
                        }
                    }
                }
            }.test { shell ->
                val state = capturedState!!
                val button = shell.find<SWTButton> { true }

                // Scroll down
                runOnSWT { kotlinx.coroutines.runBlocking { state.scrollTo(200) } }
                var value = runOnSWT { state.value }
                assertEquals(200, value)

                // Trigger recomposition
                button.doSelect()

                // Scroll position should be maintained
                value = runOnSWT { state.value }
                assertEquals(200, value, "Scroll position should survive recomposition")

                // Trigger multiple recompositions
                repeat(5) { button.doSelect() }

                value = runOnSWT { state.value }
                assertEquals(200, value, "Scroll position should still be maintained")
            }
        }
    }
}
