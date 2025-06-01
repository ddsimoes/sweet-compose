package io.github.ddsimoes.sweet.sample

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.Button
import io.github.ddsimoes.sweet.Column
import io.github.ddsimoes.sweet.ComposeSWTTarget
import io.github.ddsimoes.sweet.SWTModifier
import io.github.ddsimoes.sweet.SWTWindowConfig
import io.github.ddsimoes.sweet.Text
import io.github.ddsimoes.sweet.padding
import io.github.ddsimoes.sweet.runComposeSWTWithSWTDispatcher

/**
 * Simple counter example demonstrating Sweet Compose SWT functionality
 */
@ComposeSWTTarget
fun counterExample() {
    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "Sweet Counter Example",
            width = 300,
            height = 200
        )
    ) {
        CounterApp()
    }
}

@ComposeSWTTarget
@Composable
fun CounterApp() {
    var counter by remember { mutableStateOf(0) }

    Column(modifier = SWTModifier.padding(20)) {
        Text(
            text = "Sweet Compose Counter",
            modifier = SWTModifier.padding(10)
        )
        
        Text(
            text = "Count: $counter",
            modifier = SWTModifier.padding(10)
        )
        
        Button(
            text = "Increment",
            onClick = { counter++ },
            modifier = SWTModifier.padding(10)
        )
    }
}

// Main function for the counter example
fun main() {
    counterExample()
}