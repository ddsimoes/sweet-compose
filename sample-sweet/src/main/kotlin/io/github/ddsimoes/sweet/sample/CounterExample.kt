package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

fun main() {
    application {
        var windowEnabled by remember { mutableStateOf(true) }
        var alwaysOnTop by remember { mutableStateOf(false) }
        val windowState = rememberWindowState()
        
        Window(
            onCloseRequest = { 
                exitApplication() 
            },
            state = windowState,
            title = "Sweet Feature Test - Counter Example",
            enabled = windowEnabled,
            alwaysOnTop = alwaysOnTop
        ) {
            CounterApp(
                windowEnabled = windowEnabled,
                onToggleEnabled = { 
                    windowEnabled = !windowEnabled
                },
                alwaysOnTop = alwaysOnTop,
                onToggleAlwaysOnTop = { 
                    alwaysOnTop = !alwaysOnTop 
                },
                windowState = windowState
            )
        }
    }
}

@Composable
fun CounterApp(
    windowEnabled: Boolean = true,
    onToggleEnabled: () -> Unit = {},
    alwaysOnTop: Boolean = false,
    onToggleAlwaysOnTop: () -> Unit = {},
    windowState: WindowState? = null
) {
    var counter by remember { mutableStateOf(0) }

    Row {
        Column {
            Text(text = "Window Controls")
            
            Button(
                onClick = onToggleEnabled,
                modifier = Modifier.padding(5.dp)
            ) {
                Text("Toggle Enabled: $windowEnabled")
            }
            
            Button(
                onClick = onToggleAlwaysOnTop,
                modifier = Modifier.padding(5.dp)
            ) {
                Text("Always On Top: $alwaysOnTop")
            }
            
            // Show WindowState info if available
            windowState?.let { state ->
                Text(
                    text = "Window State:",
                    modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)
                )
                Text(
                    text = "Size: ${state.size}",
                    modifier = Modifier.padding(2.dp)
                )
                Text(
                    text = "Position: ${state.position}",
                    modifier = Modifier.padding(2.dp)
                )
                Text(
                    text = "Minimized: ${state.isMinimized}",
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sweet Compose Counter",
                modifier = Modifier.padding(10.dp)
            )

            Text(
                text = "Count: $counter",
                modifier = Modifier.padding(10.dp)
            )

            Button(
                onClick = { counter++ },
                modifier = Modifier.padding(10.dp)
            ) {
                Text("Increment")
            }
        }
    }
}
