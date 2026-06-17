@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:no-wildcard-imports")

package io.github.ddsimoes.sweet.sample.kitchensink3

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay


// ── Constants ─────────────────────────────────────────────────────────────

private val SectionSpacing = 12.dp
private val ElementSpacing = 8.dp
private val TightSpacing = 4.dp
private val CardPadding = 16.dp
private val SectionTitleSize = 20.sp
private val BodySize = 14.sp
private val TitleSize = 18.sp
private val LargeTextSize = 24.sp
private val SmallTextSize = 12.sp
private val LargeBoxSize = 60.dp
private val MediumBoxSize = 48.dp
private val ProgressAnimStep = 0.01f
private val ProgressAnimMod = 1.01f

private val MaterialBlue = Color(0xFF2196F3)
private val MaterialGreen = Color(0xFF4CAF50)
private val MaterialBlueGrey = Color(0xFF607D8B)
private val MaterialPink = Color(0xFFE91E63)
private val MaterialLightGrey = Color(0xFFF5F5F5)
private val MaterialOrange = Color(0xFFFF9800)
// ── Entry Point ───────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KitchenSink3App() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf(
        "Buttons", "TextInputs", "Selection", "Layout",
        "Lists", "Tabs", "Indicators", "Dialogs",
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sweet Compose Kitchen Sink") })
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabLabels.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CardPadding),
            ) {
                when (selectedTab) {
                    0 -> ButtonsTab()
                    1 -> TextInputsTab()
                    2 -> SelectionTab()
                    3 -> LayoutTab()
                    4 -> ListsTab()
                    5 -> TabsTab()
                    6 -> IndicatorsTab()
                    7 -> DialogsTab()
                }
            }
        }
    }
}

// ── Tab 1: Buttons ───────────────────────────────────────────────────────

@Composable
fun ButtonsTab() {
    var clickCount by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("Button Variants", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(ElementSpacing)) {
            Button(onClick = { clickCount++ }) { Text("Button") }
            OutlinedButton(onClick = { clickCount++ }) { Text("Outlined") }
            TextButton(onClick = { clickCount++ }) { Text("Text") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(ElementSpacing)) {
            ElevatedButton(onClick = { clickCount++ }) { Text("Elevated") }
            FilledTonalButton(onClick = { clickCount++ }) { Text("FilledTonal") }
        }

        FloatingActionButton(onClick = { clickCount++ }) {
            Text("FAB")
        }

        HorizontalDivider()

        Text("Clicks: $clickCount", fontSize = TitleSize)

        HorizontalDivider()

        Text("IconButton", fontWeight = FontWeight.Bold)
        IconButton(onClick = { clickCount++ }) {
            Text("\u2605") // ★
        }
    }
}

// ── Tab 2: Text & Inputs ────────────────────────────────────────────────

@Composable
fun TextInputsTab() {
    var textValue by remember { mutableStateOf("Edit me") }


    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("Typography", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Text("Headline (20sp bold)", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)
        Text("Title (18sp)", fontSize = TitleSize)
        Text("Body (14sp)", fontSize = BodySize)
        Text("Italic body", fontStyle = FontStyle.Italic)
        Text("Bold label", fontWeight = FontWeight.Bold)
        Text("Custom 24sp red", fontSize = LargeTextSize, color = Color.Red)

        HorizontalDivider()

        Text("TextField", fontWeight = FontWeight.Bold)
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("You typed: $textValue")

        Text("TextField (readOnly)", fontWeight = FontWeight.Bold)
        TextField(
            value = "Read-only text",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text("OutlinedTextField", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = "outlined",
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
        )

    }
}

// ── Tab 3: Selection ─────────────────────────────────────────────────────

@Composable
fun SelectionTab() {
    var checked by remember { mutableStateOf(false) }
    var switched by remember { mutableStateOf(false) }
    var radio by remember { mutableIntStateOf(1) }
    var sliderValue by remember { mutableFloatStateOf(0.6f) }

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("Selection Controls", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Spacer(Modifier.width(ElementSpacing))
            Text(if (checked) "Checked" else "Unchecked")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = switched, onCheckedChange = { switched = it })
            Spacer(Modifier.width(ElementSpacing))
            Text(if (switched) "ON" else "OFF")
        }

        HorizontalDivider()

        Text("RadioButton Group", fontWeight = FontWeight.Bold)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = radio == 1, onClick = { radio = 1 })
                Text("Option A  ")
                RadioButton(selected = radio == 2, onClick = { radio = 2 })
                Text("Option B  ")
                RadioButton(selected = radio == 3, onClick = { radio = 3 })
                Text("Option C")
            }
            Text("Selected: $radio")
        }

        HorizontalDivider()

        Text("Slider (text shim)", fontWeight = FontWeight.Bold)
        Slider(value = sliderValue, onValueChange = { sliderValue = it })
        Text("Slider value: ${(sliderValue * 100).toInt()}%")
    }
}

// ── Tab 4: Layout ────────────────────────────────────────────────────────

@Composable
fun LayoutTab() {
    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("Layout Primitives", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Text("Row", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(ElementSpacing)) {
            Surface(color = MaterialBlue, modifier = Modifier.size(LargeBoxSize)) {
                Box(contentAlignment = Alignment.Center) { Text("A", color = Color.White) }
            }
            Surface(color = MaterialGreen, modifier = Modifier.size(LargeBoxSize)) {
                Box(contentAlignment = Alignment.Center) { Text("B", color = Color.White) }
            }
            Spacer(Modifier.width(CardPadding))
            Surface(color = MaterialOrange, modifier = Modifier.size(LargeBoxSize)) {
                Box(contentAlignment = Alignment.Center) { Text("C", color = Color.White) }
            }
        }

        Text("Column", fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(TightSpacing)) {
            repeat(3) { i ->
                Surface(
                    color = MaterialBlueGrey,
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Row $i", color = Color.White, fontSize = SmallTextSize)
                    }
                }
            }
        }

        HorizontalDivider()
        Text("Card", fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(CardPadding)) {
                Text("Card Title", fontWeight = FontWeight.Bold)
                Text("Card body content with supporting text.")
            }
        }

        HorizontalDivider()

        Text("Surface", fontWeight = FontWeight.Bold)
        Surface(
            color = MaterialPink,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("S", color = Color.White, fontSize = LargeTextSize)
            }
        }

        HorizontalDivider()

        Text("HorizontalDivider", fontWeight = FontWeight.Bold)
        HorizontalDivider()
        HorizontalDivider()
    }
}

// ── Tab 5: Lists ─────────────────────────────────────────────────────────

@Composable
fun ListsTab() {
    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("LazyColumn", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            LazyColumn {
                items(50) { index ->
                    Text(
                        "Item ${index + 1}",
                        modifier = Modifier.padding(horizontal = SectionSpacing, vertical = TightSpacing),
                    )
                }
            }
        }

        HorizontalDivider()

        Text("LazyRow", fontWeight = FontWeight.Bold)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(ElementSpacing)) {
            items(20) { index ->
                Card(modifier = Modifier.width(80.dp)) {
                    Box(
                        modifier = Modifier.height(LargeBoxSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$index", fontSize = TitleSize)
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Scrollable Column", fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                repeat(20) { i ->
                    Text(
                        "Scrollable line $i",
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ── Tab 6: Tabs ──────────────────────────────────────────────────────────

@Composable
fun TabsTab() {
    var innerSelected by remember { mutableIntStateOf(0) }
    val longLabels = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta")

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("TabRow (3 tabs)", fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = innerSelected.coerceAtMost(2)) {
            Tab(
                selected = innerSelected == 0,
                onClick = { innerSelected = 0 },
                text = { Text("First") },
            )
            Tab(
                selected = innerSelected == 1,
                onClick = { innerSelected = 1 },
                text = { Text("Second") },
            )
            Tab(
                selected = innerSelected == 2,
                onClick = { innerSelected = 2 },
                text = { Text("Third") },
            )
        }
        Text("Inner tab: ${innerSelected + 1}")

        HorizontalDivider()

        Text("ScrollableTabRow (6 tabs)", fontWeight = FontWeight.Bold)
        ScrollableTabRow(selectedTabIndex = 0) {
            longLabels.forEachIndexed { index, label ->
                Tab(
                    selected = index == 0,
                    onClick = { },
                    text = { Text(label) },
                )
            }
        }
    }
}

// ── Tab 7: Indicators & Canvas ──────────────────────────────────────────

@Composable
fun IndicatorsTab() {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            progress = (progress + ProgressAnimStep) % ProgressAnimMod
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("ProgressIndicator", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Text("LinearProgressIndicator (determinate)")
        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${(progress * 100).toInt()}%")

        HorizontalDivider()

        Text("CircularProgressIndicator (indeterminate shim)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(MediumBoxSize))
            Spacer(Modifier.width(CardPadding))
            Text("SWT ProgressBar | INDETERMINATE")
        }

        HorizontalDivider()

        Text("Canvas", fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            drawRect(Color.Red, topLeft = androidx.compose.ui.geometry.Offset(20f, 20f), size = androidx.compose.ui.geometry.Size(60f, 40f))
            drawCircle(Color.Blue, radius = 25f, center = androidx.compose.ui.geometry.Offset(130f, 50f))
            drawLine(Color.Green, start = androidx.compose.ui.geometry.Offset(20f, 100f), end = androidx.compose.ui.geometry.Offset(200f, 100f), strokeWidth = 4f)
        }
    }
}

// ── Tab 8: Dialogs & Menus ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsTab() {
    var showAlert by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        Text("Dialogs", fontSize = SectionTitleSize, fontWeight = FontWeight.Bold)

        Button(onClick = { showAlert = true }) {
            Text("Show AlertDialog")
        }
        if (showAlert) {
            AlertDialog(
                onDismissRequest = { showAlert = false },
                title = { Text("Kitchen Sink Alert") },
                text = { Text("This is a native SWT MessageBox dialog.") },
                confirmButton = { Text("OK") },
                dismissButton = { Text("Cancel") },
            )
        }

        HorizontalDivider()
        Text("ExposedDropdownMenuBox", fontWeight = FontWeight.Bold)
        ExposedDropdownMenuBox(
            expanded = showDropdown,
            onExpandedChange = { showDropdown = !showDropdown },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialLightGrey)
                    .padding(SectionSpacing),
            ) {
                Text(if (showDropdown) "\u25B2 Choose an item" else "\u25BC Choose an item")
            }

            ExposedDropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                DropdownMenuItem(text = { Text("Option 1") }, onClick = { showDropdown = false })
                DropdownMenuItem(text = { Text("Option 2") }, onClick = { showDropdown = false })
                DropdownMenuItem(text = { Text("Option 3") }, onClick = { showDropdown = false })
            }
        }

        HorizontalDivider()

        Text("MenuBar \u2014 check the window menu bar for File > Exit")
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Compose Kitchen Sink",
        ) {
            KitchenSink3App()
        }
    }
}
