package io.github.ddsimoes.sweet.sample

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.Button
import io.github.ddsimoes.sweet.Card
import io.github.ddsimoes.sweet.Checkbox
import io.github.ddsimoes.sweet.Column
import io.github.ddsimoes.sweet.ComposeSWTTarget
import io.github.ddsimoes.sweet.Divider
import io.github.ddsimoes.sweet.Row
import io.github.ddsimoes.sweet.SWTModifier
import io.github.ddsimoes.sweet.SWTWindowConfig
import io.github.ddsimoes.sweet.Spacer
import io.github.ddsimoes.sweet.StyledText
import io.github.ddsimoes.sweet.TextField
import io.github.ddsimoes.sweet.backgroundColor
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.fillMaxWidth
import io.github.ddsimoes.sweet.padding
import io.github.ddsimoes.sweet.runComposeSWTWithSWTDispatcher
import kotlinx.coroutines.delay
import org.eclipse.swt.graphics.RGB
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Data model
 */
data class Todo(
    val id: Int,
    val text: String,
    val completed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * App state
 */
data class TodoAppState(
    val todos: List<Todo> = emptyList(),
    val filter: TodoFilter = TodoFilter.ALL,
    val theme: AppTheme = AppTheme.LIGHT,
    val layout: AppLayout = AppLayout.LIST,
    val newTodoText: String = "",
    val sessionTimeSeconds: Long = 0,
    val isTimerRunning: Boolean = true
)

enum class TodoFilter(val displayName: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

enum class AppTheme(val displayName: String, val background: RGB, val surface: RGB, val primary: RGB, val text: RGB) {
    LIGHT("Light", RGB(255, 255, 255), RGB(248, 249, 250), RGB(59, 130, 246), RGB(0, 0, 0)),
    DARK("Dark", RGB(17, 24, 39), RGB(31, 41, 55), RGB(99, 102, 241), RGB(243, 244, 246)),
    BLUE("Ocean Blue", RGB(219, 234, 254), RGB(191, 219, 254), RGB(29, 78, 216), RGB(30, 58, 138)),
    GREEN("Forest", RGB(220, 252, 231), RGB(187, 247, 208), RGB(34, 197, 94), RGB(22, 101, 52))
}

enum class AppLayout(val displayName: String) {
    LIST("List View"),
    GRID("Grid View")
}

/**
 * Complete TodoMVC Application with timer and themes
 */
@ComposeSWTTarget
fun todoMvcComplete() {
    SweetDebugger.enable()

    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "Sweet TodoMVC - Complete Demo",
            width = 800,
            height = 600
        )
    ) {
        TodoMvcApp()
    }
}

@ComposeSWTTarget
@Composable
fun TodoMvcApp() {
    var appState by remember {
        mutableStateOf(
            TodoAppState().copy(
                todos = listOf(
                    Todo(1, "Learn Jetpack Compose for SWT", false),
                    Todo(2, "Build an awesome TODO app", false),
                    Todo(3, "Test timer functionality", true),
                    Todo(4, "Try different themes", false)
                )
            )
        )
    }

    // Timer effect
    LaunchedEffect(appState.isTimerRunning) {
        if (appState.isTimerRunning) {
            while (true) {
                delay(1000)
                appState = appState.copy(sessionTimeSeconds = appState.sessionTimeSeconds + 1)
            }
        }
    }

    // Apply theme
    val theme = appState.theme

    Column(
        modifier = SWTModifier
            .fillMaxWidth()
            .backgroundColor(theme.background)
            .padding(16)
    ) {
        // Header with timer and theme controls
        HeaderSection(
            sessionTime = appState.sessionTimeSeconds,
            isTimerRunning = appState.isTimerRunning,
            currentTheme = appState.theme,
            currentLayout = appState.layout,
            onThemeChange = { appState = appState.copy(theme = it) },
            onLayoutChange = { appState = appState.copy(layout = it) },
            onTimerToggle = { appState = appState.copy(isTimerRunning = !appState.isTimerRunning) }
        )

        Divider(modifier = SWTModifier.padding(vertical = 8))

        // Todo input
        TodoInput(
            text = appState.newTodoText,
            onTextChange = { appState = appState.copy(newTodoText = it) },
            onAddTodo = {
                if (appState.newTodoText.isNotBlank()) {
                    val newTodo = Todo(
                        id = (appState.todos.maxOfOrNull { it.id } ?: 0) + 1,
                        text = appState.newTodoText.trim()
                    )
                    appState = appState.copy(
                        todos = appState.todos + newTodo,
                        newTodoText = ""
                    )
                }
            },
            theme = theme
        )

        Spacer(modifier = SWTModifier.padding(8))

        // Filter controls
        FilterControls(
            currentFilter = appState.filter,
            onFilterChange = { appState = appState.copy(filter = it) },
            todoStats = calculateStats(appState.todos),
            theme = theme
        )

        Spacer(modifier = SWTModifier.padding(8))

        // Todo list
        TodoList(
            todos = filterTodos(appState.todos, appState.filter),
            layout = appState.layout,
            theme = theme,
            onToggleTodo = { todoId ->
                appState = appState.copy(
                    todos = appState.todos.map { todo ->
                        if (todo.id == todoId) todo.copy(completed = !todo.completed)
                        else todo
                    }
                )
            },
            onDeleteTodo = { todoId ->
                appState = appState.copy(
                    todos = appState.todos.filter { it.id != todoId }
                )
            }
        )

        Spacer(modifier = SWTModifier.padding(8))

        // Footer with statistics
        FooterSection(
            stats = calculateStats(appState.todos),
            theme = theme,
            onClearCompleted = {
                appState = appState.copy(
                    todos = appState.todos.filter { !it.completed }
                )
            },
            onAddRandomTodo = {
                val randomTasks = listOf(
                    "Review code", "Write documentation", "Fix bug #${Random.nextInt(100, 999)}",
                    "Update dependencies", "Optimize performance", "Add unit tests",
                    "Refactor component", "Design new feature", "Plan next sprint"
                )
                val newTodo = Todo(
                    id = (appState.todos.maxOfOrNull { it.id } ?: 0) + 1,
                    text = randomTasks.random()
                )
                appState = appState.copy(todos = appState.todos + newTodo)
            }
        )
    }
}

@Composable
fun HeaderSection(
    sessionTime: Long,
    isTimerRunning: Boolean,
    currentTheme: AppTheme,
    currentLayout: AppLayout,
    onThemeChange: (AppTheme) -> Unit,
    onLayoutChange: (AppLayout) -> Unit,
    onTimerToggle: () -> Unit
) {
    val hours = sessionTime / 3600
    val minutes = (sessionTime % 3600) / 60
    val seconds = sessionTime % 60
    val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        backgroundColor = currentTheme.surface,
        modifier = SWTModifier.fillMaxWidth().padding(8)
    ) {
        Column(modifier = SWTModifier.padding(16)) {
            Row {
                StyledText(
                    text = "🚀 Sweet TodoMVC",
                    color = currentTheme.primary,
                    modifier = SWTModifier.padding(10, 0)
                )

                Spacer(modifier = SWTModifier.fillMaxWidth())

                StyledText(
                    text = "Session Time: $timeText",
                    color = if (isTimerRunning) currentTheme.primary else currentTheme.text,
                    modifier = SWTModifier.padding(5, 0)
                )

                Button(
                    text = if (isTimerRunning) "⏸️ Pause" else "▶️ Start",
                    onClick = onTimerToggle,
                    modifier = SWTModifier.padding(5, 0)
                )
            }

            Spacer(modifier = SWTModifier.padding(4))

            Row {
                StyledText("Theme:", color = currentTheme.text)
                AppTheme.values().forEach { theme ->
                    Button(
                        text = theme.displayName,
                        onClick = { onThemeChange(theme) },
                        modifier = SWTModifier
                            .padding(2, 0)
                            .backgroundColor(if (theme == currentTheme) currentTheme.primary else currentTheme.surface)
                    )
                }

                Spacer(modifier = SWTModifier.padding(8, 0))

                StyledText("Layout:", color = currentTheme.text)
                AppLayout.values().forEach { layout ->
                    Button(
                        text = layout.displayName,
                        onClick = { onLayoutChange(layout) },
                        modifier = SWTModifier
                            .padding(2, 0)
                            .backgroundColor(if (layout == currentLayout) currentTheme.primary else currentTheme.surface)
                    )
                }
            }
        }
    }
}

@Composable
fun TodoInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAddTodo: () -> Unit,
    theme: AppTheme
) {
    Card(
        backgroundColor = theme.surface,
        modifier = SWTModifier.fillMaxWidth().padding(8)
    ) {
        Row(modifier = SWTModifier.padding(16)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = "What needs to be done?",
                modifier = SWTModifier.fillMaxWidth().padding(4, 0)
            )

            Button(
                text = "➕ Add",
                onClick = onAddTodo,
                modifier = SWTModifier.backgroundColor(theme.primary)
            )
        }
    }
}

@Composable
fun FilterControls(
    currentFilter: TodoFilter,
    onFilterChange: (TodoFilter) -> Unit,
    todoStats: TodoStats,
    theme: AppTheme
) {
    Row(modifier = SWTModifier.fillMaxWidth()) {
        TodoFilter.values().forEach { filter ->
            Button(
                text = "${filter.displayName} (${getFilterCount(filter, todoStats)})",
                onClick = { onFilterChange(filter) },
                modifier = SWTModifier
                    .padding(4, 0)
                    .backgroundColor(if (filter == currentFilter) theme.primary else theme.surface)
            )
        }
    }
}

@Composable
fun TodoList(
    todos: List<Todo>,
    layout: AppLayout,
    theme: AppTheme,
    onToggleTodo: (Int) -> Unit,
    onDeleteTodo: (Int) -> Unit
) {
    Card(
        backgroundColor = theme.surface,
        modifier = SWTModifier.fillMaxWidth().padding(8)
    ) {
        Column(modifier = SWTModifier.padding(16)) {
            if (todos.isEmpty()) {
                StyledText(
                    text = "No todos found! 🎉",
                    color = theme.text,
                    modifier = SWTModifier.padding(32)
                )
            } else {
                todos.forEach { todo ->
                    TodoItem(
                        todo = todo,
                        theme = theme,
                        onToggle = { onToggleTodo(todo.id) },
                        onDelete = { onDeleteTodo(todo.id) }
                    )

                    if (todo != todos.last()) {
                        Divider(modifier = SWTModifier.padding(vertical = 4))
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    theme: AppTheme,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = SWTModifier.fillMaxWidth().padding(0, 4)) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle() },
            modifier = SWTModifier.padding(4, 0)
        )

        Column(modifier = SWTModifier.fillMaxWidth()) {
            StyledText(
                text = todo.text,
                color = if (todo.completed) RGB(156, 163, 175) else theme.text,
                modifier = SWTModifier.padding(0, 2)
            )

            StyledText(
                text = "Created: ${todo.createdAt.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}",
                color = RGB(156, 163, 175),
                modifier = SWTModifier.padding(4, 0)
            )
        }

        Button(
            text = "🗑️",
            onClick = onDelete,
            modifier = SWTModifier.backgroundColor(RGB(239, 68, 68))
        )
    }
}

@Composable
fun FooterSection(
    stats: TodoStats,
    theme: AppTheme,
    onClearCompleted: () -> Unit,
    onAddRandomTodo: () -> Unit
) {
    Card(
        backgroundColor = theme.surface,
        modifier = SWTModifier.fillMaxWidth().padding(8)
    ) {
        Column(modifier = SWTModifier.padding(16)) {
            Row {
                StyledText(
                    text = "📊 Statistics:",
                    color = theme.primary,
                    modifier = SWTModifier.padding(8, 0)
                )

                StyledText(
                    text = "Total: ${stats.total} | Active: ${stats.active} | Completed: ${stats.completed}",
                    color = theme.text
                )
            }

            Spacer(modifier = SWTModifier.padding(4))

            Row {
                Button(
                    text = "🎲 Add Random Todo",
                    onClick = onAddRandomTodo,
                    modifier = SWTModifier.padding(4, 0).backgroundColor(theme.primary)
                )

                if (stats.completed > 0) {
                    Button(
                        text = "🧹 Clear Completed (${stats.completed})",
                        onClick = onClearCompleted,
                        modifier = SWTModifier.backgroundColor(RGB(239, 68, 68))
                    )
                }
            }

            Spacer(modifier = SWTModifier.padding(4))

            if (stats.total > 0) {
                val completionPercentage = (stats.completed * 100) / stats.total
                StyledText(
                    text = "Progress: $completionPercentage% complete 📈",
                    color = theme.primary
                )
            }
        }
    }
}

// Helper functions
data class TodoStats(val total: Int, val active: Int, val completed: Int)

fun calculateStats(todos: List<Todo>): TodoStats {
    return TodoStats(
        total = todos.size,
        active = todos.count { !it.completed },
        completed = todos.count { it.completed }
    )
}

fun filterTodos(todos: List<Todo>, filter: TodoFilter): List<Todo> {
    return when (filter) {
        TodoFilter.ALL -> todos
        TodoFilter.ACTIVE -> todos.filter { !it.completed }
        TodoFilter.COMPLETED -> todos.filter { it.completed }
    }
}

fun getFilterCount(filter: TodoFilter, stats: TodoStats): Int {
    return when (filter) {
        TodoFilter.ALL -> stats.total
        TodoFilter.ACTIVE -> stats.active
        TodoFilter.COMPLETED -> stats.completed
    }
}

// Main function
fun main() {
    todoMvcComplete()
}