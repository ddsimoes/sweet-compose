package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

fun main() {
    println("Testing Sweet Compose MenuBar...")
    
    application {
        var status by remember { mutableStateOf("Ready") }
        var isWordWrap by remember { mutableStateOf(false) }
        var selectedTheme by remember { mutableStateOf("Light") }
        
        Window(onCloseRequest = {
            println("Window close requested!")
            exitApplication()
        }, title = "Sweet MenuBar Example") {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Sweet Compose MenuBar Example",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                Text(
                    text = "Status: $status",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                Text(
                    text = "Settings:",
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                
                Text(
                    text = "• Word Wrap: ${if (isWordWrap) "Enabled" else "Disabled"}",
                    modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
                )
                
                Text(
                    text = "• Theme: $selectedTheme",
                    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
                )
                
                Text(
                    text = "MenuBar implementation is in progress. The APIs are ready but need FrameWindowScope integration.",
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}
