package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Counter Example"
        ) {
            SWTDispatcherApp()
        }
    }
}

@Composable
fun SWTDispatcherApp() {
    var time by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var backgroundTask by remember { mutableStateOf("Idle") }

    // Timer using a persistent job that we control manually
    val timerJob = remember { mutableStateOf<Job?>(null) }
    val isRunningState = remember { mutableStateOf(isRunning) }

    // When isRunning changes, start or stop the timer
    if (isRunningState.value != isRunning) {
        isRunningState.value = isRunning

        // Cancel existing timer
        timerJob.value?.cancel()
        timerJob.value = null

        if (isRunning) {
            timerJob.value = CoroutineScope(Dispatchers.Default).launch {
                var currentTime = time
                while (isActive) {
                    delay(100)
                    currentTime += 100
                    // Update UI state on the Main dispatcher
                    withContext(Dispatchers.Main) {
                        time = currentTime
                    }
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            timerJob.value?.cancel()
        }
    }

    // Background task effect
    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.Main).launch {
            backgroundTask = "Starting background task..."
            delay(1000)
            backgroundTask = "Background task running..."
            delay(2000)
            backgroundTask = "Background task completed!"
        }

        onDispose {
            job.cancel()
        }
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "SWT Dispatcher Demo",
            modifier = Modifier.padding(10.dp)
        )

        Text(
            text = "Timer: ${time}ms",
            modifier = Modifier.padding(5.dp)
        )

        Row {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Button(
                onClick = {
                    time = 0L
                    isRunning = false
                },
                modifier = Modifier.padding(5.dp)
            ) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.padding(10.dp))

        Text(
            text = "Background Task Status:",
            modifier = Modifier.padding(5.dp)
        )

        Text(
            text = backgroundTask,
            modifier = Modifier.padding(5.dp)
        )

        Spacer(modifier = Modifier.padding(10.dp))

        Text(
            text = "This example demonstrates proper SWT thread handling with coroutines.\nAll UI updates are dispatched correctly to the SWT UI thread.",
            modifier = Modifier.padding(5.dp)
        )
    }
}
