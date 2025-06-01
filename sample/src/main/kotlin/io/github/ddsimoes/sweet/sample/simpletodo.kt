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
import org.eclipse.swt.graphics.RGB

/**
 * Simplified TodoMVC to test layout compatibility
 */
@ComposeSWTTarget
fun simplifiedTodoMVC() {
    SweetDebugger.enable()

    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "Simplified TodoMVC - Layout Test",
            width = 600,
            height = 400
        )
    ) {
        SimplifiedTodoApp()
    }
}

@ComposeSWTTarget
@Composable
fun SimplifiedTodoApp() {
    var newTodoText by remember { mutableStateOf("") }
    var todos by remember {
        mutableStateOf(
            listOf(
                SimpleTodo(1, "Test task 1", false),
                SimpleTodo(2, "Test task 2", true),
                SimpleTodo(3, "Test task 3", false)
            )
        )
    }

    Column(
        modifier = SWTModifier
            .fillMaxWidth()
            .backgroundColor(RGB(248, 250, 252))
            .padding(16)
    ) {
        // Header
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            StyledText(
                text = "🚀 Simplified TodoMVC",
                color = RGB(59, 130, 246),
                modifier = SWTModifier.padding(16)
            )
        }

        Spacer(modifier = SWTModifier.padding(8))

        // Input section
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                StyledText(
                    text = "Add New Todo:",
                    color = RGB(51, 65, 85),
                    modifier = SWTModifier.padding(0, 4)
                )

                Row {
                    TextField(
                        value = newTodoText,
                        onValueChange = { newTodoText = it },
                        placeholder = "What needs to be done?",
                        modifier = SWTModifier.fillMaxWidth().padding(4, 0)
                    )

                    Button(
                        text = "Add",
                        onClick = {
                            if (newTodoText.isNotBlank()) {
                                val newTodo = SimpleTodo(
                                    id = (todos.maxOfOrNull { it.id } ?: 0) + 1,
                                    text = newTodoText.trim()
                                )
                                todos = todos + newTodo
                                newTodoText = ""
                            }
                        },
                        modifier = SWTModifier.backgroundColor(RGB(34, 197, 94))
                    )
                }
            }
        }

        Spacer(modifier = SWTModifier.padding(8))

        // Todo list
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                StyledText(
                    text = "Todo List (${todos.size} items):",
                    color = RGB(51, 65, 85),
                    modifier = SWTModifier.padding(0, 8)
                )

                todos.forEach { todo ->
                    SimpleTodoItem(
                        todo = todo,
                        onToggle = {
                            todos = todos.map {
                                if (it.id == todo.id) it.copy(completed = !it.completed)
                                else it
                            }
                        },
                        onDelete = {
                            todos = todos.filter { it.id != todo.id }
                        }
                    )

                    if (todo != todos.last()) {
                        Divider(modifier = SWTModifier.padding(0, 4))
                    }
                }

                if (todos.isEmpty()) {
                    StyledText(
                        text = "No todos yet! Add one above. 🚀",
                        color = RGB(156, 163, 175),
                        modifier = SWTModifier.padding(16)
                    )
                }
            }
        }

        Spacer(modifier = SWTModifier.padding(8))

        // Stats
        Card(
            backgroundColor = RGB(239, 246, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                val completed = todos.count { it.completed }
                val active = todos.count { !it.completed }

                StyledText(
                    text = "📊 Stats: Total: ${todos.size} | Active: $active | Completed: $completed",
                    color = RGB(59, 130, 246)
                )

                if (completed > 0) {
                    Spacer(modifier = SWTModifier.padding(4))
                    Button(
                        text = "Clear Completed",
                        onClick = {
                            todos = todos.filter { !it.completed }
                        },
                        modifier = SWTModifier.backgroundColor(RGB(239, 68, 68))
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleTodoItem(
    todo: SimpleTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = SWTModifier.fillMaxWidth().padding(0, 4)) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle() },
            modifier = SWTModifier.padding(4, 0)
        )

        StyledText(
            text = todo.text,
            color = if (todo.completed) RGB(156, 163, 175) else RGB(51, 65, 85),
            modifier = SWTModifier.fillMaxWidth().padding(4, 0)
        )

        Button(
            text = "Delete",
            onClick = onDelete,
            modifier = SWTModifier.backgroundColor(RGB(239, 68, 68))
        )
    }
}

data class SimpleTodo(
    val id: Int,
    val text: String,
    val completed: Boolean = false
)

// Main function
fun main() {
    simplifiedTodoMVC()
}