package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Layout Test"
        ) {
            LayoutTestApp()
        }
    }
}

@Composable
fun LayoutTestApp() {
    Column(modifier = Modifier.padding(16.dp)) {

        // Test 1: Simple Text
        Text("=== LAYOUT TESTS ===")

        Spacer(modifier = Modifier.padding(8.dp))

        // Test 2: Card with content
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Card Title")
                Text("Card content goes here")
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // Test 3: Row with buttons
        Text("Row Test:")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { }) {
                Text("Btn 1")
            }

            Button(onClick = { }) {
                Text("Btn 2")
            }

            Button(onClick = { }) {
                Text("Btn 3")
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // Test 4: Nested layouts
        Text("Nested Layout Test:")
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Outer Column")

                Row {
                    Column {
                        Text("Col A")
                        Text("Item 1")
                        Text("Item 2")
                    }

                    Spacer(modifier = Modifier.padding(16.dp))

                    Column {
                        Text("Col B")
                        Button(onClick = {}) { Text("Action") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // Test 5: fillMaxWidth test
        Text("Fill Width Test:")
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Full Width Button")
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // Test 6: Padding variations
        Text("Padding Test:")
        Column {
            Text("No padding", modifier = Modifier)
            Text("Small padding", modifier = Modifier.padding(4.dp))
            Text("Medium padding", modifier = Modifier.padding(8.dp))
            Text("Large padding", modifier = Modifier.padding(16.dp))
        }
    }
}

/*
LAYOUT DEBUGGING:

Este sample testa:
✅ Column básico
✅ Row básico
✅ Card container
✅ Spacer para espaçamento
✅ Modifier.fillMaxWidth()
✅ Modifier.padding()
✅ Layouts aninhados (Column dentro de Row dentro de Column)
✅ Botões em diferentes containers

Se algo não estiver funcionando, o problema pode estar em:
1. GridLayout configuration (numColumns, spacing)
2. GridData application (fillMaxWidth, padding)
3. Parent-child relationships no applier
4. Modifier application order
*/
