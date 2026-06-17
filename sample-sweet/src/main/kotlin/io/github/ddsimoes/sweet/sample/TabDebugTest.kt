package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

/**
 * Debug program to manually verify Tab components render correctly
 */
fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            println("Starting Tab Debug Test...")
            
            Column {
                Text("=== Tab Debug Test ===")
                
                Text("1. Simple text test")
                
                Text("2. Card test:")
                Card {
                    Text("Content inside card")
                }
                
                Text("3. Row test:")
                Row {
                    Text("Item A")
                    Text("Item B")
                }
                
                Text("4. Individual Tab test:")
                Tab(
                    selected = true,
                    onClick = { println("Tab clicked!") },
                    text = { Text("Single Tab") }
                )
                
                Text("5. TabRow test:")
                TabRow(selectedTabIndex = 0) {
                    Tab(
                        selected = true,
                        onClick = { println("Tab 1 clicked!") },
                        text = { Text("Tab 1") }
                    )
                    Tab(
                        selected = false,
                        onClick = { println("Tab 2 clicked!") },
                        text = { Text("Tab 2") }
                    )
                }
                
                Text("6. ScrollableTabRow test:")
                ScrollableTabRow(selectedTabIndex = 1) {
                    repeat(5) { index ->
                        Tab(
                            selected = index == 1,
                            onClick = { println("Scrollable tab $index clicked!") },
                            text = { Text("ST $index") }
                        )
                    }
                }
                
                Text("=== End Test ===")
            }
        }
    }
}
