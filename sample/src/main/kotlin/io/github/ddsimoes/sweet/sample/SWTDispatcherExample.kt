package io.github.ddsimoes.sweet.sample

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.Button
import io.github.ddsimoes.sweet.Column
import io.github.ddsimoes.sweet.ComposeSWTTarget
import io.github.ddsimoes.sweet.Row
import io.github.ddsimoes.sweet.SWTModifier
import io.github.ddsimoes.sweet.SWTWindowConfig
import io.github.ddsimoes.sweet.Spacer
import io.github.ddsimoes.sweet.Text
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.padding
import io.github.ddsimoes.sweet.runComposeSWTWithSWTDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.launch

/**
 * Example demonstrating proper SWT coroutine dispatcher usage
 */
@ComposeSWTTarget
fun swtDispatcherExample() {
    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "SWT Dispatcher Example",
            width = 500,
            height = 400
        )
    ) {
        SWTDispatcherApp()
    }
}

@ComposeSWTTarget
@Composable
fun SWTDispatcherApp() {
    var time by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var backgroundTask by remember { mutableStateOf("Idle") }
    
    SweetDebugger.log("APP", "SWTDispatcherApp recomposing with time=$time, isRunning=$isRunning, backgroundTask='$backgroundTask'")

    // Timer using a persistent job that we control manually
    val timerJob = remember { mutableStateOf<Job?>(null) }
    val isRunningState = remember { mutableStateOf(isRunning) }
    
    // When isRunning changes, start or stop the timer
    if (isRunningState.value != isRunning) {
        SweetDebugger.log("TIMER", "isRunning changed from ${isRunningState.value} to $isRunning")
        isRunningState.value = isRunning
        
        // Cancel existing timer
        timerJob.value?.cancel()
        timerJob.value = null
        
        if (isRunning) {
            SweetDebugger.log("TIMER", "Starting timer coroutine")
            timerJob.value = CoroutineScope(Dispatchers.Default).launch {
                SweetDebugger.log("TIMER", "Timer coroutine started successfully")
                var currentTime = time
                while (isActive) {
                    delay(100)
                    currentTime += 100
                    // Update UI state on the Main dispatcher
                    withContext(Dispatchers.Main) {
                        time = currentTime
                    }
                }
                SweetDebugger.log("TIMER", "Timer coroutine ended")
            }
        } else {
            SweetDebugger.log("TIMER", "Timer stopped")
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            SweetDebugger.log("TIMER", "Cleaning up timer job")
            timerJob.value?.cancel()
        }
    }

    // Background task effect
    DisposableEffect(Unit) {
        SweetDebugger.log("BACKGROUND", "Starting background task")
        val job = CoroutineScope(Dispatchers.Main).launch {
            backgroundTask = "Starting background task..."
            delay(1000)
            backgroundTask = "Background task running..."
            delay(2000)
            backgroundTask = "Background task completed!"
        }
        
        onDispose {
            SweetDebugger.log("BACKGROUND", "Background task disposed")
            job.cancel()
        }
    }

    Column(modifier = SWTModifier.padding(20)) {
        Text(
            text = "SWT Dispatcher Demo",
            modifier = SWTModifier.padding(10)
        )

        val timerText = "Timer: ${time}ms"
        SweetDebugger.log("APP", "Rendering timer text='$timerText', time=$time")
        Text(
            text = timerText,
            modifier = SWTModifier.padding(5)
        )

        Row {
            val buttonText = if (isRunning) "Stop" else "Start"
            SweetDebugger.log("APP", "Rendering Start/Stop button with text='$buttonText', isRunning=$isRunning")
            Button(
                text = buttonText,
                onClick = { 
                    SweetDebugger.log("APP", "Start/Stop button clicked, current isRunning=$isRunning")
                    isRunning = !isRunning 
                    SweetDebugger.log("APP", "Start/Stop button handler complete, new isRunning=$isRunning")
                },
                modifier = SWTModifier.padding(5)
            )

            Button(
                text = "Reset",
                onClick = {
                    SweetDebugger.log("APP", "Reset button clicked, current time=$time, isRunning=$isRunning")
                    time = 0L
                    isRunning = false
                    SweetDebugger.log("APP", "Reset button handler complete, new time=$time, isRunning=$isRunning")
                },
                modifier = SWTModifier.padding(5)
            )
        }

        Spacer(modifier = SWTModifier.padding(10))

        Text(
            text = "Background Task Status:",
            modifier = SWTModifier.padding(5)
        )

        Text(
            text = backgroundTask,
            modifier = SWTModifier.padding(5)
        )

        Spacer(modifier = SWTModifier.padding(10))

        Text(
            text = "This example demonstrates proper SWT thread handling with coroutines.\n" +
                    "All UI updates are dispatched correctly to the SWT UI thread.",
            modifier = SWTModifier.padding(5)
        )
    }
}

// Main function for the SWT dispatcher example
fun main() {
    SweetDebugger.enable()

    swtDispatcherExample()
}