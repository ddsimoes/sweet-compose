package io.github.ddsimoes.sweet

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * Simple counter example with debugging enabled
 */
@ComposeSWTTarget
fun counterExampleWithDebug() {
    // Enable debugging for this example
    SweetDebugger.enable()
    
    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "Sweet Counter (Debug Mode)",
            width = 300,
            height = 200
        )
    ) {
        CounterAppWithDebug()
    }
}

@ComposeSWTTarget
@Composable
fun CounterAppWithDebug() {
    var counter by remember { mutableStateOf(0) }
    
    SweetDebugger.log("COUNTER_APP", "Recomposing with counter=$counter")

    Column(modifier = SWTModifier.padding(20)) {
        Text(
            text = "Sweet Compose Counter (Debug)",
            modifier = SWTModifier.padding(10)
        )
        
        val countText = "Count: $counter"
        SweetDebugger.log("COUNTER_APP", "Rendering count text: '$countText'")
        Text(
            text = countText,
            modifier = SWTModifier.padding(10)
        )
        
        Button(
            text = "Increment",
            onClick = { 
                SweetDebugger.log("COUNTER_APP", "Button clicked, incrementing counter from $counter to ${counter + 1}")
                counter++
            },
            modifier = SWTModifier.padding(10)
        )
    }
}

// Main function for the debug counter example
fun main() {
    counterExampleWithDebug()
}