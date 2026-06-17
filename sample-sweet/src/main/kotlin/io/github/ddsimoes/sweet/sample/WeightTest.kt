package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

/**
 * Simple test to verify weight behavior in Row layouts
 */
fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            WeightTestContent()
        }
    }
}

@Composable
fun WeightTestContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Weight Test - All buttons should be in a row with the middle one expanding")
        
        Spacer(modifier = Modifier.padding(8.dp))
        
        // Test 1: Basic weight test
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Test 1: Weight(1f) in middle")
                
                Row {
                    Button(onClick = {}) {
                        Text("Left")
                    }
                    
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Middle (weight=1f)")
                    }
                    
                    Button(onClick = {}) {
                        Text("Right")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.padding(8.dp))
        
        // Test 2: Using Column with weight (like TodoItem)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Test 2: Column with weight(1f) like TodoItem")
                
                Row {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {}
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("This should expand")
                        Text("Pushing the delete button to the right")
                    }
                    
                    Button(onClick = {}) {
                        Text("Delete")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.padding(8.dp))
        
        // Test 3: Different weight ratios
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Test 3: Different weight ratios")
                
                Row {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1f")
                    }
                    
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("2f")
                    }
                    
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1f")
                    }
                }
            }
        }
    }
}
