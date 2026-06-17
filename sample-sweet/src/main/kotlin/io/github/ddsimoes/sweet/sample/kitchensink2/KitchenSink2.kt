package io.github.ddsimoes.sweet.sample.kitchensink2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement

/**
 * Kitchen Sink - Comprehensive demonstration of Sweet Compose components
 * 
 * This application showcases all major components and features of Sweet Compose,
 * providing a complete reference implementation for developers.
 */

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Compose Kitchen Sink"
        ) {
            KitchenSinkApp2()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenSinkApp2() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Basic", "Lists", "Dialogs", "Animations", "Custom")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Real TopAppBar implementation
        TopAppBar(title = { Text("Sweet Compose Kitchen Sink") })
        
        // TabRow placeholder - will be implemented next  
        PlaceholderTabRow(
            selectedIndex = selectedTab,
            tabs = tabs,
            onTabSelected = { selectedTab = it }
        )
        
        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> BasicComponentsTab()
                1 -> ListsTab()
                2 -> DialogsTab()
                3 -> AnimationsTab()
                4 -> CustomTab()
                else -> BasicComponentsTab()
            }
        }
    }
}

// PlaceholderTopAppBar removed - using real TopAppBar now

@Composable
private fun PlaceholderTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    // Temporary implementation until TabRow is ready
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            Button(
                onClick = { onTabSelected(index) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(title)
            }
        }
    }
}

// Tab Content Composables

@Composable
fun BasicComponentsTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Basic Components")
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { }) { Text("Button") }
            OutlinedButton(onClick = { }) { Text("Outlined") }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var checked by remember { mutableStateOf(false) }
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it }
            )
            Text("Checkbox")
        }
        
        TextField(
            value = "Sample Text",
            onValueChange = { }
        )
    }
}

@Composable
fun ListsTab() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lists & Collections")
        Spacer(modifier = Modifier.height(16.dp))
        
        // Real LazyColumn implementation
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn {
                item {
                    Text(
                        text = "LazyColumn Header",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                items(10) { index ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Item ${index + 1}")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Action")
                        }
                    }
                }
                
                item {
                    Text(
                        text = "LazyColumn Footer",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // LazyRow example
        Text("Horizontal Scrolling")
        LazyRow {
            items(8) { index ->
                Card(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Card ${index + 1}")
                        Button(onClick = { }) {
                            Text("Click")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogsTab() {
    var showDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dialogs & Overlays")
        
        Button(
            onClick = { showDialog = true }
        ) {
            Text("Show Dialog")
        }
        
        if (showDialog) {
            // Placeholder for AlertDialog
            Card {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("This will be an AlertDialog!")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                        Button(onClick = { showDialog = false }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationsTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Animations & Transitions")
        
        Text("Animation stubs will be implemented in Phase 6")
        
        // Placeholder for animated components
        Card {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🎭")
            }
        }
    }
}

@Composable
fun CustomTab() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Custom Components")
        
        Text("Custom layouts and advanced components")
        
        // Placeholder for custom layout examples
        Card {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🛠️")
            }
        }
    }
}
