import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.AutoSWT
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.Todo
import io.github.ddsimoes.sweet.sample.TodoFilter
import io.github.ddsimoes.sweet.sample.TodoItem
import io.github.ddsimoes.sweet.sample.TodoListSection
import io.github.ddsimoes.sweet.sample.TodoMvcApp
import io.github.ddsimoes.sweet.sample.TodoState
import io.github.ddsimoes.sweet.sample.filterTodos
import org.eclipse.swt.SWT
import io.github.ddsimoes.sweet.widgets.ScrollViewport
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoMVCTest {
    @Test
    fun testTodo() {
        autoSWT {
            testShell(width = 600, height = 580) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->

                shell.saveSVG("before")
                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition")
                }

                val scrolled = shell.find<ScrollViewport>()
                assertNotNull(scrolled, "Expected a ScrollViewport wrapping the Column")

                // Width fill: the Column (child of scrolled content) should fill viewport width
                val viewport = runOnSWT { scrolled.clientArea }
                val contentComposite = runOnSWT { scrolled.content as Composite }
                val columnComposite = runOnSWT { contentComposite.children.firstOrNull() as? Composite }
                assertNotNull(columnComposite, "Expected a Column composite inside scroll content")
                val colBounds = columnComposite!!.getAbsoluteBounds()
                val vpLeft = runOnSWT { scrolled.toDisplay(viewport.x, 0).x }
                val vpRight = runOnSWT { scrolled.toDisplay(viewport.x + viewport.width, 0).x }
                val viewportWidth = vpRight - vpLeft
                assertTrue("Column should fill viewport width: column=${colBounds.width}, viewport=$viewportWidth") {
                    colBounds.width >= viewportWidth - 2
                }

                // NOTE: vertical-scroll behaviour is verified at the END of this test (after the
                // pristine 4-item assertions below), where sample todos are added to force the list
                // to overflow the viewport without perturbing the initial item counts.

                // Test that the TodoMVC app loads and has the expected UI components
                val textField = shell.find<Text>()
                val addButton = shell.find<Button> { it.text == "Add" }
                val allButton = shell.find<Button> { it.text.startsWith("All") }
                val activeButton = shell.find<Button> { it.text.startsWith("Active") }
                val completedButton = shell.find<Button> { it.text.startsWith("Completed") }
                val sampleButton = shell.find<Button> { it.text.startsWith("Samples") }

                // Verify all expected components exist
                assertNotNull(textField, "Text field should be present")
                assertNotNull(addButton, "Add button should be present")
                assertNotNull(allButton, "All filter button should be present")
                assertNotNull(activeButton, "Active filter button should be present")
                assertNotNull(completedButton, "Completed filter button should be present")
                assertNotNull(sampleButton, "Add sample todos button should be present")

                // Validate layout visibility: all Delete buttons and Item labels should be inside viewport
                val client = runOnSWT { shell.clientArea }

                val deleteButtons = shell.findAll<Button> { it.text == "Delete" }
                assertEquals(4, deleteButtons.size, "Should have delete buttons for initial todo items")
                runOnSWT {
                    deleteButtons.forEachIndexed { idx, b ->
                        val inViewport = b.bounds.x >= 0 && b.bounds.x + b.bounds.width <= client.width
                        assertTrue(inViewport, "Delete[$idx] out of viewport: bounds=${b.bounds}, client=$client")
                    }
                }

                val itemLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                assertEquals(4, itemLabels.size, "Should have 4 item labels initially")
                runOnSWT {
                    itemLabels.forEachIndexed { idx, l ->
                        val inViewport = l.bounds.x >= 0 && l.bounds.x + l.bounds.width <= client.width
                        assertTrue(inViewport, "ItemLabel[$idx] out of viewport: bounds=${l.bounds}, client=$client")
                    }
                }

                // Test that delete buttons exist for the initial todo items
                // (already asserted above, with visibility checks)

                deleteButtons.first().doSelect()

                shell.find<Button> { it.text.startsWith("Clear ") }.doSelect()

                completedButton.doSelect()

                activeButton.doSelect()

                allButton.doSelect()

                textField.typeText("New item 1")
                addButton.doSelect()

                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition")
                }

                // Verify vertical scrolling works. The list may currently fit within the viewport
                // (content height <= viewport height), in which case there is correctly no scroll
                // range. Add sample todos until it overflows, then confirm setOrigin moves the
                // scrollbar. Mirrors TodoMVCScrollTest.todoList_verticalScroll_works_and_updates_after_add.
                val scroller = shell.find<ScrollViewport>()
                fun scrollRange(): Int = runOnSWT { scroller.verticalBar?.let { it.maximum - it.thumb } ?: 0 }
                var guard = 0
                while (scrollRange() <= 0 && guard++ < 5) {
                    shell.find<Button> { it.text.startsWith("Samples") }.doSelect()
                }
                assertTrue(scrollRange() > 0, "Todo list should overflow the viewport after adding sample todos (range=${scrollRange()})")
                runOnSWT {
                    val bar = scroller.verticalBar!!
                    val initialSel = bar.selection
                    scroller.setOrigin(0, bar.maximum - bar.thumb)
                    assertTrue(bar.selection != initialSel, "Scrollbar selection should change after setOrigin")
                }

                shell.saveSVG()
                shell.saveScreenshot()
            }
        }
    }

    @Test
    fun testTodoLayout() {
        autoSWT {
            testShell(width = 600, height = 800) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->

                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition")
                }

                shell.saveSVG()
                shell.saveScreenshot()

                // Test that the TodoMVC app loads and has the expected UI components
                shell.find<Text>()
                shell.find<Button> { it.text == "Add" }
                shell.find<Button> { it.text.startsWith("All") }
                shell.find<Button> { it.text.startsWith("Active") }
                shell.find<Button> { it.text.startsWith("Completed") }
                shell.find<Button> { it.text.startsWith("Samples") }

                // Validate items and columns: Delete buttons aligned at the right; labels to the left
                val itemLabels = shell.findAll<Label> { it.text.startsWith("Item ") }
                itemLabels.assertLayout().areAllVisible()
                itemLabels.forEach { it.assertLayout().hasMinSize(height = 12) }

                val deleteButtons = shell.findAll<Button> { it.text == "Delete" }
                assertEquals(4, deleteButtons.size, "Should have 4 Delete buttons")

                // Validate Delete buttons are within their rows and right-aligned
                deleteButtons.forEach { btn ->
                    val row = runOnSWT { btn.parent }
                    btn.assertLayout().isWithin(row).isNearRightEdgeOf(row, maxDistance = 5)
                }
                // All Delete buttons should be right-aligned with each other
                deleteButtons.assertLayout().areRightAlignedWith()

                // For each row, label should be left of Delete button
                deleteButtons.forEach { btn ->
                    val row = runOnSWT { btn.parent }
                    val label = runOnSWT { row.find<Label> { it.text.startsWith("Item ") } }
                    label.assertLayout().isLeftOf(btn).doesNotOverlap(btn)
                }
            }
        }
    }

    @Test
    fun testTodoList() {
        val state = TodoState()

        autoSWT {
            testShell(width = 600, height = 900) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoList(state)
                }
            }.test { shell ->
                assertEquals(TodoFilter.ALL, state.filter)
                // Test if the todos on screen match the filtered list in order (currently all todos)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.COMPLETED
                // only completed on screen (1 item)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ALL
                // check if all todos are visible again
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ACTIVE
                // only completed on screen (1 item)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ALL
                // check if all todos are visible again
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ACTIVE
                // only completed on screen (1 item)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.COMPLETED
                // only completed on screen (1 item)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ACTIVE
                // only completed on screen (1 item)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))
            }
        }
    }

    @Test
    fun testTodoToggle() {
        val state = TodoState()

        autoSWT {
            testShell(width = 600, height = 900) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoList(state)
                }
            }.test { shell ->
                assertEquals(TodoFilter.ALL, state.filter)
                // Test if the todos on screen match the filtered list in order (currently all todos)
                assertEquals(filterTodos(state.todos, state.filter), readActualUITodos(shell, state))

                state.filter = TodoFilter.ACTIVE
                val filteredBeforeClick = filterTodos(state.todos, state.filter)
                val actualBeforeClick = readActualUITodos(shell, state)

                println("Before click:")
                println("  Filtered expected: $filteredBeforeClick")
                println("  Actual from UI: $actualBeforeClick")

                assertEquals(filteredBeforeClick, actualBeforeClick)

                val allCheckboxes = shell.findAll<Button> { SWT.CHECK in it.style }
                println("Checkboxes before click:")
                allCheckboxes.forEachIndexed { idx, cb ->
                    println("  [$idx] selection=${runOnSWT { cb.selection }}")
                }

                val firstCheck = shell.find<Button> { SWT.CHECK in it.style }
                firstCheck.doSelect()

                val allCheckboxesAfter = shell.findAll<Button> { SWT.CHECK in it.style }
                println("Checkboxes after click:")
                allCheckboxesAfter.forEachIndexed { idx, cb ->
                    println("  [$idx] selection=${runOnSWT { cb.selection }}")
                }

                val filteredAfterClick = filterTodos(state.todos, state.filter)
                val actualAfterClick = readActualUITodos(shell, state)

                println("After click:")
                println("  State todos: ${state.todos}")
                println("  Filtered expected: $filteredAfterClick")
                println("  Actual from UI: $actualAfterClick")

                assertEquals(filteredAfterClick, actualAfterClick)
            }
        }
    }

    private fun AutoSWT.readActualUITodos(
        shell: Shell,
        state: TodoState,
    ): List<Todo> {
        val checkBoxes = shell.findAll<Button> { it.style and SWT.CHECK != 0 }
        val labels = runOnSWT { checkBoxes.map { it.parent.find<Label> { !it.text.startsWith("Created") } } }
        val actualTodos =
            runOnSWT {
                labels.mapIndexed { idx, label ->
                    val todo = state.todos.find { it.text == label.text }!!
                    todo.copy(completed = checkBoxes[idx].selection)
                }
            }
        return actualTodos
    }

    @Composable
    fun TodoList(state: TodoState) {
        val filtered = filterTodos(state.todos, state.filter)
        TodoListSection(filtered, { id ->
            state.todos =
                state.todos.map { todo ->
                    if (todo.id == id) {
                        todo.copy(completed = !todo.completed)
                    } else {
                        todo
                    }
                }
        }, { id ->
            state.todos =
                state.todos.filter { todo ->
                    todo.id != id
                }
        })
    }

    private fun AutoSWT.checkLayout(composite: Composite) {
        val (bounds1, bounds2) =
            runOnSWT {
                val b1 =
                    composite.findAll<Control> { true }.let { children ->
                        children.map { it.bounds }
                    }

                composite.layout(true, true)

                val b2 =
                    composite.findAll<Control> { true }.let { children ->
                        children.map { it.bounds }
                    }

                listOf(b1, b2)
            }

        assertEquals(bounds2, bounds1, "Incorrect layout after recomposition")
    }

    @Composable
    private fun TestTodoList() {
        var completed by remember {
            mutableStateOf(true)
        }
        var todos by remember {
            mutableStateOf(
                listOf(
                    Todo(1, "Todo 1", true),
                    Todo(2, "Todo 2", false),
                ),
            )
        }

        // Log current state during each recomposition
        SweetDebugger.log("TestTodoList", "Recomposing - completed=$completed, todos=${todos.size} items")
        todos.forEachIndexed { index, todo ->
            SweetDebugger.log("TestTodoList", "  [$index] Todo ${todo.id}: '${todo.text}' completed=${todo.completed}")
        }

        val filteredTodos = todos.filter { it.completed == completed }
        SweetDebugger.log("TestTodoList", "Filtered todos: ${filteredTodos.size} items (showing completed=$completed)")
        filteredTodos.forEachIndexed { index, todo ->
            SweetDebugger.log("TestTodoList", "  [$index] Filtered Todo ${todo.id}: '${todo.text}' completed=${todo.completed}")
        }

        TodoListSection(filteredTodos, { id ->
            SweetDebugger.log("TestTodoList", "Toggle todo $id requested")
            todos = todos.map { todo -> if (todo.id == id) todo.copy(completed = true) else todo }
        }, {
            SweetDebugger.log("TestTodoList", "Button clicked - BEFORE: completed=$completed, todos=${todos.size} items")
            todos = todos.filter { !it.completed }
            SweetDebugger.log("TestTodoList", "Button clicked - AFTER filter: completed=$completed, todos=${todos.size} items")
            completed = !completed
            SweetDebugger.log("TestTodoList", "Button clicked - AFTER toggle: completed=$completed, todos=${todos.size} items")
        })
    }

    @Test
    fun testTodoFilter() {
        autoSWT {
            testShell(width = 600, height = 900) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TestTodoList()
                }
            }.test { shell ->
                // Wait for UI to be ready
                shell.find<Button> { it.style and SWT.PUSH != 0 }.doSelect()
            }
        }
    }

    @Composable
    private fun SimpleRecompositionTest() {
        var state1 by remember { mutableStateOf(1) }
        var state2 by remember { mutableStateOf(true) }

        SweetDebugger.log("SimpleTest", "Recomposing - state1=$state1, state2=$state2")

        TodoListSection(
            todos = if (state2) listOf(Todo(state1, "Item $state1", true)) else emptyList(),
            onToggleTodo = { },
            onDeleteTodo = {
                SweetDebugger.log("SimpleTest", "Button clicked - BEFORE: state1=$state1, state2=$state2")
                state1 = state1 + 1 // State change 1
                SweetDebugger.log("SimpleTest", "Button clicked - AFTER state1: state1=$state1, state2=$state2")
                state2 = !state2 // State change 2
                SweetDebugger.log("SimpleTest", "Button clicked - AFTER state2: state1=$state1, state2=$state2")
            },
        )
    }

    @Test
    fun testSimpleRecomposition() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    SimpleRecompositionTest()
                }
            }.test { shell ->
                // Click the button to trigger dual state change
                shell.find<Button> { it.style and SWT.PUSH != 0 }.doSelect()
            }
        }
    }

    @Composable
    private fun StaleControlTest() {
        var showItems by remember { mutableStateOf(true) }

        SweetDebugger.log("StaleControlTest", "Recomposing - showItems=$showItems")

        TodoListSection(
            todos =
                if (showItems) {
                    listOf(
                        Todo(1, "Item A", false),
                        Todo(2, "Item B", false),
                        Todo(3, "Item C", false),
                    )
                } else {
                    emptyList()
                },
            onToggleTodo = { },
            onDeleteTodo = {
                SweetDebugger.log("StaleControlTest", "Button clicked - toggling showItems from $showItems")
                showItems = !showItems
            },
        )
    }

    @Test
    fun testStaleControls() {
        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    StaleControlTest()
                }
            }.test { shell ->

                // Initial state - should show 3 items
                val initialCheckboxes = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 } }
                SweetDebugger.log("StaleControlTest", "=== INITIAL STATE: ${initialCheckboxes.size} checkboxes ===")

                // Click delete button to toggle to empty list
                shell.find<Button> { it.text == "Delete" }.doSelect()

                // Check for stale controls
                val afterClickCheckboxes = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 } }
                SweetDebugger.log("StaleControlTest", "=== AFTER DELETE CLICK: ${afterClickCheckboxes.size} checkboxes ===")

                // Click again to toggle back to items
                val deleteButtons = shell.findAll<Button> { it.text == "Delete" }
                if (deleteButtons.isNotEmpty()) {
                    deleteButtons.first().doSelect()
                }

                val finalCheckboxes = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 } }
                SweetDebugger.log("StaleControlTest", "=== FINAL STATE: ${finalCheckboxes.size} checkboxes ===")
            }
        }
    }

    @Composable
    private fun ResponsiveLayoutTest() {
        var todos by remember {
            mutableStateOf(
                (1..20).map { Todo(it, "Todo item $it - This is a longer text to test layout", it % 3 == 0) },
            )
        }

        SweetDebugger.log("ResponsiveLayoutTest", "Rendering ${todos.size} todos")

        TodoMvcApp()
    }

    @Test
    fun testResponsiveLayout() {
        autoSWT {
            testShell(width = 400, height = 300) {
                // Small window to test responsiveness
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    ResponsiveLayoutTest()
                }
            }.test { shell ->

                // Add many todos to test scrolling
                val addButton = shell.find<Button> { it.text == "Samples" }
                addButton.doSelect()

                // Add more todos
                addButton.doSelect()

                SweetDebugger.log("ResponsiveLayoutTest", "=== TESTING LAYOUT RESPONSIVENESS ===")

                // Validate header is always visible
                val headerLabel = shell.find<Label> { it.text.contains("TodoMVC") }
                assertNotNull(headerLabel, "Header should always be visible")

                // Validate filter section is accessible
                val allButton = shell.find<Button> { it.text.startsWith("All") }
                val activeButton = shell.find<Button> { it.text.startsWith("Active") }
                val completedButton = shell.find<Button> { it.text.startsWith("Completed") }

                assertNotNull(allButton, "All filter button should be accessible")
                assertNotNull(activeButton, "Active filter button should be accessible")
                assertNotNull(completedButton, "Completed filter button should be accessible")

                // Test filter functionality with many todos
                completedButton.doSelect()

                val completedTodos = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 && it.selection } }
                SweetDebugger.log("ResponsiveLayoutTest", "Completed filter shows ${completedTodos.size} completed todos")

                activeButton.doSelect()

                val activeTodos = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 && !it.selection } }
                SweetDebugger.log("ResponsiveLayoutTest", "Active filter shows ${activeTodos.size} active todos")

                allButton.doSelect()

                // Test layout with different window sizes
                runOnSWT {
                    shell.setSize(800, 600) // Larger window
                    shell.layout()
                }

                // Verify components still work after resize
                val textField = shell.find<Text>()
                val addNewButton = shell.find<Button> { it.text == "Add" }

                textField.typeText("Responsive test todo")
                addNewButton.doSelect()

                // Verify the new todo was added
                val newTodoLabel = shell.find<Label> { it.text == "Responsive test todo" }
                assertNotNull(newTodoLabel, "New todo should be visible after adding")

                // Take screenshot of the responsive layout
                shell.saveSVG()
                shell.saveScreenshot()

                SweetDebugger.log("ResponsiveLayoutTest", "Layout responsiveness test completed successfully")
            }
        }
    }

    @Test
    fun testLazyColumnScrolling() {
        autoSWT {
            testShell(width = 400, height = 200) {
                // Very small window to force scrolling
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    // Create many todos to test scrolling
                    val todos =
                        remember {
                            mutableStateOf((1..30).map { Todo(it, "Todo item number $it", it % 5 == 0) })
                        }

                    TodoListSection(
                        todos = todos.value,
                        onToggleTodo = { },
                        onDeleteTodo = { },
                    )
                }
            }.test { shell ->

                SweetDebugger.log("LazyColumnScrolling", "=== TESTING LAZY COLUMN SCROLLING ===")

                // Check if todos are rendered
                val checkboxes = shell.findAll<Button> { runOnSWT { it.style and SWT.CHECK != 0 } }
                val labels = shell.findAll<Label> { it.text.startsWith("Todo item") }

                SweetDebugger.log("LazyColumnScrolling", "Found ${checkboxes.size} checkboxes and ${labels.size} todo labels")

                // Look for ScrollViewport (LazyColumn implementation)
                val scrolledComposites = shell.findAll<ScrollViewport> { true }
                SweetDebugger.log("LazyColumnScrolling", "Found ${scrolledComposites.size} ScrollViewport widgets")

                scrolledComposites.forEach { scrolled ->
                    runOnSWT {
                        val content = scrolled.content
                        val children = if (content is Composite) content.children.size else 0
                        SweetDebugger.log("LazyColumnScrolling", "ScrollViewport content has $children children")
                        SweetDebugger.log("LazyColumnScrolling", "ScrollViewport bounds: ${scrolled.bounds}")
                        if (content != null) {
                            SweetDebugger.log("LazyColumnScrolling", "Content bounds: ${content.bounds}")
                        }
                    }
                }

                // Take screenshot to visualize the issue
                shell.saveSVG()
                shell.saveScreenshot()

                SweetDebugger.log("LazyColumnScrolling", "ScrollViewport test completed")
            }
        }
    }

    @Test
    fun testColumnVerticalScrollScrolling() {
        autoSWT {
            testShell(width = 400, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    val todos =
                        remember {
                            mutableStateOf((1..30).map { Todo(it, "Todo item number $it", it % 5 == 0) })
                        }
                    // Use Column with verticalScroll to validate behavior without LazyColumn
                    Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                    ) {
                        todos.value.forEach { t ->
                            TodoItem(todo = t, onToggle = { }, onDelete = { })
                        }
                    }
                }
            }.test { shell ->
                // Ensure scroll container exists and fills width
                val scrolled = shell.find<ScrollViewport>()
                val viewport = runOnSWT { scrolled.clientArea }
                val content = runOnSWT { scrolled.content as Composite }
                val child = runOnSWT { content.children.firstOrNull() }
                assertNotNull(child, "Scrollable content should have a child")
                val childBounds = child!!.getAbsoluteBounds()
                val left = runOnSWT { content.toDisplay(viewport.x, 0).x }
                val right = runOnSWT { content.toDisplay(viewport.x + viewport.width, 0).x }
                val viewportWidth = right - left
                assertTrue("Scrollable content should fill viewport width: child=${childBounds.width} viewport=$viewportWidth") {
                    childBounds.width >= viewportWidth - 2
                }
            }
        }
    }

    @Test
    fun testCompactLayout() {
        autoSWT {
            testShell(width = 600, height = 400) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    TodoMvcApp()
                }
            }.test { shell ->

                SweetDebugger.log("CompactLayout", "=== TESTING ULTRA-COMPACT LAYOUT ===")

                // Verify all sections are present and compact
                val headerLabel = shell.find<Label> { it.text.contains("TodoMVC") }
                assertNotNull(headerLabel, "Header should be present")

                val addLabel = shell.find<Label> { it.text == "Add:" }
                assertNotNull(addLabel, "Add section should be present")

                val filterLabel = shell.find<Label> { it.text == "Filter:" }
                assertNotNull(filterLabel, "Filter section should be present")

                val statsLabel = shell.find<Label> { it.text.contains("📊") }
                assertNotNull(statsLabel, "Statistics should be present")

                // Take screenshot of the compact layout
                shell.saveSVG()
                shell.saveScreenshot()

                SweetDebugger.log("CompactLayout", "Ultra-compact layout test completed successfully")
            }
        }
    }
}
