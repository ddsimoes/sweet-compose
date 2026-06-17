@file:Suppress("ktlint:standard:function-naming")

// NOTE: This file is a HARDLINK shared between sample-sweet/ and sample-jetbrains/.
// Both paths point to the same inode — the identical source is compiled against two
// different Compose runtimes (Sweet in sample-sweet, JetBrains Compose Desktop in
// sample-jetbrains) so we validate API parity from a single source of truth.
// See docs/development/sample-compatibility.md for the module layout.

package io.github.ddsimoes.sweet.sample
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.time.LocalDateTime

/**
 * Data model for Todo items
 */
data class Todo(
    val id: Int,
    val text: String,
    val completed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Filter states for the todo list
 */
enum class TodoFilter(
    val displayName: String,
) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
}

/**
 * Statistics for the todo list
 */
data class TodoStats(
    val total: Int,
    val active: Int,
    val completed: Int,
)

class TodoState {
    var todos by mutableStateOf(
        listOf(
            Todo(1, "Item One", false),
            Todo(2, "Item Two", false),
            Todo(3, "Item Three", true),
            Todo(4, "Item Four", false),
        ),
    )

    var filter by mutableStateOf(TodoFilter.ALL)
}

/**
 * Main TodoMVC application composable
 */
@Composable
fun TodoMvcApp(todoState: TodoState = remember { TodoState() }) {
    var newTodoText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Header - fixed at top
        HeaderSection()

        Spacer(modifier = Modifier.padding(4.dp))

        // Add new todo section - fixed below header
        AddTodoSection(
            newTodoText = newTodoText,
            onTextChange = { newTodoText = it },
            onAddTodo = {
                if (newTodoText.isNotBlank()) {
                    val newTodo =
                        Todo(
                            id = (todoState.todos.maxOfOrNull { it.id } ?: 0) + 1,
                            text = newTodoText.trim(),
                        )
                    todoState.todos += newTodo
                    newTodoText = ""
                }
            },
        )

        Spacer(modifier = Modifier.padding(4.dp))

        // Filter controls - fixed below add section
        FilterSection(
            onFilterChange = {
                todoState.filter = it
            },
            stats = calculateStats(todoState.todos),
        )

        Spacer(modifier = Modifier.padding(4.dp))

        // Todo list - takes remaining space and scrolls
        val filteredTodos = filterTodos(todoState.todos, todoState.filter)

        TodoListSection(
            todos = filteredTodos,
            modifier = Modifier.weight(1f), // Takes remaining space
            onToggleTodo = { todoId ->
                todoState.todos =
                    todoState.todos.map { todo ->
                        if (todo.id == todoId) {
                            todo.copy(completed = !todo.completed)
                        } else {
                            todo
                        }
                    }
            },
            onDeleteTodo = { todoId ->
                todoState.todos = todoState.todos.filter { it.id != todoId }
            },
        )

        Spacer(modifier = Modifier.padding(4.dp))

        // Footer - fixed at bottom
        FooterSection(
            stats = calculateStats(todoState.todos),
            onClearCompleted = {
                todoState.todos = todoState.todos.filter { !it.completed }
            },
            onAddSampleTodos = {
                val sampleTodos =
                    listOf(
                        "Review code changes",
                        "Write unit tests",
                        "Update documentation",
                        "Fix reported bugs",
                        "Plan next sprint",
                    )
                val maxId = todoState.todos.maxOfOrNull { it.id } ?: 0
                val newTodos =
                    sampleTodos.mapIndexed { index, text ->
                        Todo(id = maxId + index + 1, text = text)
                    }
                todoState.todos += newTodos
            },
        )
    }
}

/**
 * Header section with title
 */
@Composable
fun HeaderSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp)) {
            Text("🚀 TodoMVC", modifier = Modifier.weight(1f))
            Text("Sweet Compose SWT", color = Color.Gray)
        }
    }
}

/**
 * Section for adding new todos
 */
@Composable
fun AddTodoSection(
    newTodoText: String,
    onTextChange: (String) -> Unit,
    onAddTodo: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp)) {
            Text("Add:", modifier = Modifier.padding(end = 4.dp))
            TextField(
                value = newTodoText,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).padding(end = 4.dp),
            )
            Button(onClick = onAddTodo) {
                Text("Add")
            }
        }
    }
}

/**
 * Filter controls section
 */
@Composable
fun FilterSection(
    onFilterChange: (TodoFilter) -> Unit,
    stats: TodoStats,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp)) {
            Text("Filter:", modifier = Modifier.padding(end = 4.dp))
            TodoFilter.entries.forEach { filter ->
                val count =
                    when (filter) {
                        TodoFilter.ALL -> stats.total
                        TodoFilter.ACTIVE -> stats.active
                        TodoFilter.COMPLETED -> stats.completed
                    }

                Button(
                    onClick = { onFilterChange(filter) },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text("${filter.displayName} ($count)")
                }
            }
        }
    }
}

/**
 * Todo list section with efficient scrolling
 */
@Composable
fun TodoListSection(
    todos: List<Todo>,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
            Text("Todo List (${todos.size} items):")

            Spacer(modifier = Modifier.padding(2.dp))

            if (todos.isEmpty()) {
                Text("No todos found! 🎉", color = Color.Gray)
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    todos.forEach { todo ->
                        key(todo.id) {
                            TodoItem(
                                todo = todo,
                                onToggle = { onToggleTodo(todo.id) },
                                onDelete = { onDeleteTodo(todo.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Todo list section with efficient scrolling
 */
@Composable
fun TodoListSectionFail(
    todos: List<Todo>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                todos.forEach { todo ->
                    key(todo.id) {
                        TodoItem(
                            todo = todo,
                            onToggle = { },
                            onDelete = { },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoListSectionOk(
    todos: List<Todo>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            todos.forEach { todo ->
                key(todo.id) {
                    TodoItem(
                        todo = todo,
                        onToggle = { },
                        onDelete = { },
                    )
                }
            }
        }
    }
}

/**
 * Individual todo item
 */
@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { newValue ->
                onToggle()
            },
            modifier = Modifier.padding(end = 4.dp),
        )

        Text(
            text = todo.text,
            color = if (todo.completed) Color.Gray else Color.Black,
            modifier = Modifier.weight(1f),
        )

        Button(onClick = onDelete) {
            Text("Delete")
        }
    }
}

/**
 * Footer section with statistics and actions
 */
@Composable
fun FooterSection(
    stats: TodoStats,
    onClearCompleted: () -> Unit,
    onAddSampleTodos: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp)) {
            val completionPercentage = if (stats.total > 0) (stats.completed * 100) / stats.total else 0
            Text(
                "📊 ${stats.total}|${stats.active}|${stats.completed} ($completionPercentage%)",
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = onAddSampleTodos,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Text("Samples")
            }

            if (stats.completed > 0) {
                Button(onClick = onClearCompleted) {
                    Text("Clear (${stats.completed})")
                }
            }
        }
    }
}

/**
 * Calculate statistics for the todo list
 */
fun calculateStats(todos: List<Todo>): TodoStats =
    TodoStats(
        total = todos.size,
        active = todos.count { !it.completed },
        completed = todos.count { it.completed },
    )

/**
 * Filter todos based on the current filter
 */
fun filterTodos(
    todos: List<Todo>,
    filter: TodoFilter,
): List<Todo> =
    when (filter) {
        TodoFilter.ALL -> todos
        TodoFilter.ACTIVE -> todos.filter { !it.completed }
        TodoFilter.COMPLETED -> todos.filter { it.completed }
    }

/**
 * TodoMVC application entry point
 */
fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            TodoMvcApp()
        }
    }
}
