import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.sample.Todo
import io.github.ddsimoes.sweet.sample.TodoItem
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoMVCScrollTest {

    @Composable
    private fun TodoListOnly(initialCount: Int = 40) {
        var count by remember { mutableStateOf(initialCount) }
        val todos = remember(count) { (1..count).map { Todo(it, "Item $it", completed = (it % 5 == 0)) } }

        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Button(onClick = { count += 1 }) { Text("Add One Item") }

            // Focus the test on the list section used by TodoMVC
            TodoListSection(
                todos = todos,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }

    /**
     * Content for the resize stability test: a vertically scrollable column with items
     * that have moderately long text so width changes affect text wrapping.
     */
    @Composable
    private fun ResizeCyclesContent() {
        val items = remember {
            listOf(
                "Review pull request #142 and provide feedback on the approach",
                "Write integration tests for the new layout system",
                "Update documentation with architecture diagrams",
                "Fix memory leak in the composition disposal path",
                "Refactor measurement to use single-pass approach",
                "Investigate SWT scrollbar assertion failures",
                "Benchmark layout performance with many children",
                "Add support for horizontal scrolling in LazyColumn",
                "Fix padding gap accounting in weight allocation",
                "Port TodoMVC sample to use Material3 components",
                "Write a design doc for the next feature",
                "Debug the resize height growth issue",
            )
        }

        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Text("Resize Test:", modifier = Modifier.padding(bottom = 4.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEachIndexed { index, text ->
                    Text("$index. $text", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    fun TodoListSection(
        todos: List<Todo>,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier.fillMaxSize().background(Color.Gray)) {
            Column(modifier = Modifier.padding(8.dp).fillMaxSize().background(Color.DarkGray)) {
                Text("Header")

                Spacer(modifier = Modifier.padding(2.dp))

                if (todos.isEmpty()) {
                    Text("No todos found! 🎉", color = Color.Gray)
                } else {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        todos.forEach { todo ->
                            key(todo.id) {
                                TodoItem(
                                    todo = todo,
                                    onToggle = {  },
                                    onDelete = {  }
                                )
                            }
                        }
                    }
                }
                Text("Footer")
            }
        }
    }


    @Composable
    fun TodoListSection2(
        todos: List<Todo>,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier.fillMaxSize().background(Color.Gray)) {
            Column(modifier = Modifier.padding(8.dp).fillMaxSize().background(Color.DarkGray)) {
                Text("Header")

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    todos.forEach { todo ->
                        key(todo.id) {
                            TodoItem(
                                todo = todo,
                                onToggle = {  },
                                onDelete = {  }
                            )
                        }
                    }
                }

                Text("Footer")
            }
        }
    }

    @Test
    fun todoList_test_layout_1() {
        autoSWT {
            testShell(width = 520, height = 400) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    TodoListSection2((1 .. 5).map { Todo(it, "Todo item $it") })
                }
            }.test { shell ->
                val header = shell.find<Label> { it.text == "Header" }
                val footer = shell.find<Label> { it.text == "Footer" }

                // Header should be visible
                header.assertLayout().isVisible()

                footer.assertLayout().isVisible()
            }
        }
    }

    @Test
    fun todoList_test_layout_2() {
        autoSWT {
            testShell(width = 520, height = 400) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    TodoListOnly(initialCount = 5)
                }
            }.test { shell ->
                val header = shell.find<Label> { it.text == "Header" }
                val footer = shell.find<Label> { it.text == "Footer" }

                // Header should be visible
                header.assertLayout().isVisible()

                footer.assertLayout().isVisible()
            }
        }
    }

    @Test
    fun todoList_verticalScroll_works_and_updates_after_add() {
        autoSWT {
            testShell(width = 520, height = 300) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    // Use many initial items so scrolling is required
                    TodoListOnly(initialCount = 5)
                }
            }.test { shell ->

                // Verify scroller exists
                val scroller = shell.find<ScrollViewport>()
                assertNotNull(scroller, "Expected a ScrollViewport wrapping the Todo list")

                // Verify we have many items so scrolling is needed
                val labels = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertTrue(labels.size >= 5, "Expected many list items to require scrolling")

                // Initial scroll should be possible: programmatically scroll down and verify origin changes
                val initialOrigin = runOnSWT { scroller.origin }
                runOnSWT {
                    val bar = scroller.verticalBar
                    val target = if (bar != null) (bar.maximum - bar.thumb).coerceAtLeast(1) else scroller.clientArea.height * 2
                    scroller.setOrigin(0, target)
                }
                val originAfter = runOnSWT { scroller.origin }
                assertTrue(originAfter.y > initialOrigin.y, "Scroller origin.y should increase after setOrigin")

                // Add another item and verify scroller size updates (range increases or stays scrolled)
                shell.find<org.eclipse.swt.widgets.Button> { it.text == "Add One Item" }.doSelect()

                // After add, origin remains valid and items increased
                val labelsAfter = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertTrue(labelsAfter.size > labels.size, "Labels count should increase after adding one item")

                // Attempt to scroll further; origin should still be adjustable (minSize recomputed)
                val beforeSecondScroll = runOnSWT { scroller.origin.y }
                runOnSWT {
                    val bar = scroller.verticalBar
                    val target = if (bar != null) (bar.maximum - bar.thumb).coerceAtLeast(1) else scroller.clientArea.height * 2
                    scroller.setOrigin(0, target)
                }
                val afterSecondScroll = runOnSWT { scroller.origin.y }
                assertTrue(afterSecondScroll >= beforeSecondScroll, "Origin should remain scrollable after adding an item")
            }
        }
    }

    /**
     * Regression test: rapid width resize cycles with text-wrapping content should
     * not cause unbounded content height growth. The test creates many items with
     * moderately long text (so width changes affect wrapping), then cycles the
     * window width through narrow → wide → narrow → … repeatedly, measuring the
     * ScrollViewport's vertical scroll range (max - thumb) after each cycle.
     *
     * History: with the old ScrolledComposite-based scroller (pre workstream 13),
     * `updateScrollerMinSize` would call `ScrolledComposite.setMinSize`, which triggered
     * `layout(false)` → `Composite.updateLayout()` ancestor walk, causing re-entrant
     * ancestor re-layout with stale measurements. During interactive drag resize this
     * compounded across cycles, producing monotonic growth. The Sweet-owned
     * [ScrollViewport] measures content directly; this test still guards range stability
     * across wrap-affecting resizes.
     */
    @Test
    fun scrollContent_height_stable_across_resize_cycles() {
        autoSWT {
            testShell(width = 480, height = 400) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    ResizeCyclesContent()
                }
            }.test { shell ->
                val scroller = shell.find<ScrollViewport>()
                assertNotNull(scroller, "Expected a ScrollViewport")

                // Capture scroll range metric: max - thumb is the scrollable extent
                fun captureScrollRange(): Int {
                    return runOnSWT {
                        val vb = scroller.verticalBar ?: return@runOnSWT 0
                        // Scrollable extent = how many pixels you can scroll
                        (vb.maximum - vb.thumb).coerceAtLeast(0)
                    }
                }

                // Do multiple width cycles: narrow ↔ wide
                val widths = intArrayOf(480, 250, 480, 300, 480, 220, 480, 350, 480)
                val ranges = mutableListOf<Int>()

                for (w in widths) {
                    runOnSWT { shell.setSize(w, 400) }
                    ranges.add(captureScrollRange())
                }

                // After returning to wide (480), the scroll range should be similar
                // to the initial wide range.  Narrow widths may produce larger ranges
                // (more text wrapping), but should not grow unboundedly.
                val wideRanges = ranges.filterIndexed { i, _ -> widths[i] == 480 }
                if (wideRanges.size >= 2) {
                    val first = wideRanges.first()
                    val last = wideRanges.last()
                    // The last wide-scrollable-extent should not exceed the first by
                    // more than a small tolerance (text wrapping can vary slightly
                    // between cold/warm measurements, but not unboundedly).
                    assertTrue(
                        last <= first + 20,
                        "Scroll range should not grow across cycles: first wide=$first, last wide=$last, all=$ranges"
                    )
                }

                // Verify that narrow widths produce larger scroll ranges (wrapped text)
                val narrowRange = ranges[1] // width=250
                val wideRange = ranges[0]  // width=480
                assertTrue(
                    narrowRange >= wideRange,
                    "Narrower window should require more vertical scrolling (text wraps); narrow=$narrowRange, wide=$wideRange"
                )
            }
        }
    }
 }
