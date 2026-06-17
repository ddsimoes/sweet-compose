package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement

/**
 * Comprehensive test of the new Sweet layout system demonstrating:
 * - Column and Row layouts with different arrangements and alignments
 * - Box layout with content alignment
 * - Size modifiers (width, height, size, fillMax*)
 * - Spacing modifiers (padding, offset)
 * - Weight system for flexible layouts
 * - Aspect ratio constraints
 * - Size constraints (sizeIn)
 */
fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            SweetLayoutTest()
        }
    }
}

@Composable
fun SweetLayoutTest() {
    var clickCount by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sweet Layout System Test")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Clicks: $clickCount")
            Button(onClick = { clickCount++ }) { Text("Increment") }
        }

        // Test Row with different arrangements
        Text("Row Arrangements:")
        TestRowArrangements()
    }
}

@Composable
fun TestRowArrangements() {
    Column {
        // SpaceBetween
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {}) { Text("Start") }
            Button(onClick = {}) { Text("Center") }
            Button(onClick = {}) { Text("End") }
        }
        
        // SpaceEvenly
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {}) { Text("A") }
            Button(onClick = {}) { Text("B") }
            Button(onClick = {}) { Text("C") }
        }
        
        // Center
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {}) { Text("Centered") }
        }
    }
}

@Composable
fun TestColumnArrangements() {
    Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        // SpaceBetween - using the new layout system
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {}) { Text("Top") }
            Button(onClick = {}) { Text("Middle") }
            Button(onClick = {}) { Text("Bottom") }
        }
        
        // Center - using the new layout system
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {}) { Text("Centered") }
        }
        
        // SpaceEvenly - using the new layout system
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {}) { Text("A") }
            Button(onClick = {}) { Text("B") }
            Button(onClick = {}) { Text("C") }
        }
    }
}

@Composable
fun TestBoxAlignment() {
    Box(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background button
        Button(
            onClick = {},
            modifier = Modifier.fillMaxSize()
        ) { Text("Background") }
        
        // Centered button  
        Button(onClick = {}) { Text("Center") }
        
        // These would need the sweet alignment system to position correctly
        Button(onClick = {}) { Text("Overlaid") }
    }
}

