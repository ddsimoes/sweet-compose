@file:Suppress("ktlint:standard:function-naming")

package io.github.ddsimoes.sweet.sample.kitchensink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

data class ListItem(
    val id: Int,
    val title: String,
    val description: String,
)

@Composable
fun KitchenSinkApp() {
    var counter by remember { mutableIntStateOf(0) }
    var textFieldValue by remember { mutableStateOf("Type here...") }
    var isChecked by remember { mutableStateOf(false) }
    var isSwitchOn by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(50f) }

    val sampleItems =
        remember {
            (1..20).map { index ->
                ListItem(
                    id = index,
                    title = "Item $index",
                    description = "This is a sample description for item $index",
                )
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header Section
        HeaderSection()

        HorizontalDivider()

        // Text Components Section
        TextComponentsSection()

        HorizontalDivider()

        // Button Components Section
        ButtonComponentsSection(
            counter = counter,
            onIncrement = { counter++ },
            onDecrement = { counter-- },
            onReset = { counter = 0 },
        )

        HorizontalDivider()

        // Input Components Section
        InputComponentsSection(
            textFieldValue = textFieldValue,
            onTextChange = { textFieldValue = it },
            isChecked = isChecked,
            onCheckedChange = { isChecked = it },
            isSwitchOn = isSwitchOn,
            onSwitchChange = { isSwitchOn = it },
            sliderValue = sliderValue,
            onSliderChange = { sliderValue = it },
        )

        HorizontalDivider()

        // Layout Components Section
        LayoutComponentsSection()

        HorizontalDivider()

        // List Components Section
        ListComponentsSection(items = sampleItems)

        HorizontalDivider()

        // State Management Section
        StateManagementSection(
            counter = counter,
            textFieldValue = textFieldValue,
            isChecked = isChecked,
            isSwitchOn = isSwitchOn,
            sliderValue = sliderValue,
        )

        HorizontalDivider()

        // Bottom Section to verify scrolling
        BottomTestSection()
    }
}

@Composable
fun HeaderSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🧪 Compose Kitchen Sink",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Comprehensive component showcase for Sweet Compose compatibility",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
fun TextComponentsSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 Text Components",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Regular text")
            Text(text = "Bold text", fontWeight = FontWeight.Bold)
            Text(text = "Italic text", fontStyle = FontStyle.Italic)
            Text(text = "Large text", fontSize = 20.sp)
            Text(text = "Small text", fontSize = 12.sp)
            Text(text = "Colored text", color = Color.Blue)
            Text(text = "Center aligned text", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ButtonComponentsSection(
    counter: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔘 Button Components",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Counter: $counter")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onIncrement) {
                    Text("Increment")
                }

                OutlinedButton(onClick = onDecrement) {
                    Text("Decrement")
                }

                TextButton(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun InputComponentsSection(
    textFieldValue: String,
    onTextChange: (String) -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isSwitchOn: Boolean,
    onSwitchChange: (Boolean) -> Unit,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📋 Input Components",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = textFieldValue,
                onValueChange = onTextChange,
                label = { Text("Text Field") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
                Text("Checkbox - Checked: $isChecked")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = isSwitchOn,
                    onCheckedChange = onSwitchChange,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch - On: $isSwitchOn")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Text("Slider - Value: ${sliderValue.toInt()}")
                Slider(
                    value = sliderValue,
                    onValueChange = onSliderChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun LayoutComponentsSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📐 Layout Components",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Row Layout:")
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(Color.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("1", color = Color.White)
                }
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(Color.Green),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("2", color = Color.White)
                }
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(Color.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("3", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Column with Weight:")
            Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color.Cyan)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Weight 1")
                }
                Box(
                    modifier =
                        Modifier
                            .weight(2f)
                            .fillMaxSize()
                            .background(Color.Magenta)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Weight 2", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Box with Alignment:")
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.LightGray),
                contentAlignment = Alignment.Center,
            ) {
                Text("Centered in Box")
            }
        }
    }
}

@Composable
fun ListComponentsSection(items: List<ListItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📋 List Components",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("List Items (First 5 items):")

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.take(5).forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StateManagementSection(
    counter: Int,
    textFieldValue: String,
    isChecked: Boolean,
    isSwitchOn: Boolean,
    sliderValue: Float,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔄 State Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Current State Values:")
            Text("• Counter: $counter")
            Text("• Text Field: \"$textFieldValue\"")
            Text("• Checkbox: $isChecked")
            Text("• Switch: $isSwitchOn")
            Text("• Slider: ${sliderValue.toInt()}")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "All values are managed with mutableStateOf and automatically trigger recomposition when changed.",
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
fun BottomTestSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎯 Scroll Test Section",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("If you can see this text, scrolling is working correctly!")
            Spacer(modifier = Modifier.height(8.dp))

            repeat(3) { index ->
                Text(
                    "• Test item ${index + 1}: This section verifies that vertical scrolling works properly",
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "🎉 End of KitchenSink - Scrolling Complete!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Blue,
            )
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Compose Kitchen Sink",
        ) {
            KitchenSinkApp()
        }
    }
}
