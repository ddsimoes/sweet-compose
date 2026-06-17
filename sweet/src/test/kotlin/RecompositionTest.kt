@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class RecompositionTest {
    @Test
    fun testListChanges() {
        val mgr = MyListManager()
        var n = 10
        mgr.myList.addAll(0..n)

        autoSWT {
            testShell(width = 600, height = 900) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    ListViewer(mgr)
                }
            }.test { shell ->
                runBlocking {
//                    delay(1000)
                    mgr.myList.add(++n)
                    mgr.myList.add(++n)
                    delay(200)

                    (2 downTo 0).forEach {
                        mgr.myList.removeAt(it)
                    }

                    delay(200)

                    repeat(5) {
                        mgr.shift++
                        delay(200)
                    }

                    runOnSWT {
                        shell.addListener(SWT.Close) {
                            shell.dispose()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ItemList(
        title: String,
        items: List<Int>,
    ) {
        Column {
            Text(title, modifier = Modifier.background(Color.LightGray))
            items.forEach {
                Row {
                    Checkbox(false, onCheckedChange = {})
                    Text("Item: $it")
                }
            }
        }
    }

    @Composable
    fun ListViewer(manager: MyListManager) {
        val list = manager.myList
        val shift = manager.shift

        val lists =
            listOf(
                "Empty" to emptyList(),
                "All" to list,
                "Odd" to list.filterIndexed { idx, s -> idx % 2 != 0 },
                "Even" to list.filterIndexed { idx, s -> idx % 2 == 0 },
            )
        val weight = 1.0f / lists.size

        val shuffled = shuffleList(lists, shift)

        Row {
            shuffled.forEachIndexed { idx, (title, list) ->
                Column(Modifier.weight(weight)) {
                    ItemList(title, list)
                }
            }
        }
    }

    private fun <T> shuffleList(
        list: List<T>,
        shift: Int,
    ): List<T> {
        var i = 0
        val shuffled = mutableListOf<T>()
        while (i < list.size) {
            val n = (i + shift) % list.size
            print("$n, ")
            shuffled.add(list[n])
            i++
        }
        println()

        return shuffled
    }

    @Composable
    private fun DynamicTextListTest() {
        var textInput by remember { mutableStateOf("Hello") }
        var numberInput by remember { mutableStateOf("3") }

        SweetDebugger.log("DynamicTextListTest", "Recomposing - textInput='$textInput', numberInput='$numberInput'")

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Text:", modifier = Modifier.padding(end = 8.dp))
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Number:", modifier = Modifier.padding(end = 8.dp))
                TextField(
                    value = numberInput,
                    onValueChange = { numberInput = it },
                    modifier = Modifier.weight(1f),
                )
            }

            val count = numberInput.toIntOrNull() ?: 0
            SweetDebugger.log("DynamicTextListTest", "Generating $count text items with base text '$textInput'")

            repeat(count) { index ->
                Text("$textInput ${index + 1}", modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }

    @Test
    fun testDynamicTextList() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    DynamicTextListTest()
                }
            }.test { shell ->

                SweetDebugger.log("DynamicTextList", "=== TESTING DYNAMIC TEXT LIST ===")

                // Find the text input fields
                val textFields = shell.findAll<Text> { true }
                assertEquals(2, textFields.size, "Should have 2 text input fields")

                val textField = textFields[0]
                val numberField = textFields[1]

                // Initial state - should show 3 items with "Hello"
                val initialLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("Hello \\d+")) } }
                assertEquals(3, initialLabels.size, "Should initially show 3 'Hello' items")

                // Verify the initial text content
                val expectedTexts = listOf("Hello 1", "Hello 2", "Hello 3")
                val actualTexts = runOnSWT { initialLabels.map { it.text }.sorted() }
                assertEquals(expectedTexts, actualTexts, "Initial texts should match expected pattern")

                SweetDebugger.log("DynamicTextList", "Initial state verified: ${initialLabels.size} labels")

                // Change the text input to "World"
                textField.clearText()
                textField.typeText("World")

                // Should now show 3 items with "World"
                val worldLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("World \\d+")) } }
                assertEquals(3, worldLabels.size, "Should show 3 'World' items after text change")

                val expectedWorldTexts = listOf("World 1", "World 2", "World 3")
                val actualWorldTexts = runOnSWT { worldLabels.map { it.text }.sorted() }
                assertEquals(expectedWorldTexts, actualWorldTexts, "World texts should match expected pattern")

                SweetDebugger.log("DynamicTextList", "Text change verified: ${worldLabels.size} 'World' labels")

                // The UI is already update with "World", so the layout should reflect that.
                shell.refreshLayout { boundsBefore, boundsAfter ->
                    assertEquals(boundsAfter, boundsBefore, "Incorrect layout after recomposition.")
                }

                // Change the number input to 5
                numberField.clearText()
                numberField.typeText("5")

                // Should now show 5 items with "World"
                val fiveWorldLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("World \\d+")) } }
                assertEquals(5, fiveWorldLabels.size, "Should show 5 'World' items after number change")

                val expectedFiveTexts = listOf("World 1", "World 2", "World 3", "World 4", "World 5")
                val actualFiveTexts = runOnSWT { fiveWorldLabels.map { it.text }.sorted() }
                assertEquals(expectedFiveTexts, actualFiveTexts, "Five World texts should match expected pattern")

                SweetDebugger.log("DynamicTextList", "Number change verified: ${fiveWorldLabels.size} 'World' labels")

                // Change to 0 items
                numberField.clearText()
                numberField.typeText("0")

                // Should now show 0 items
                val zeroLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("World \\d+")) } }
                assertEquals(0, zeroLabels.size, "Should show 0 items when number is 0")

                SweetDebugger.log("DynamicTextList", "Zero items verified: ${zeroLabels.size} labels")

                // Change back to 2 items with different text
                textField.clearText()
                textField.typeText("Test")
                numberField.clearText()
                numberField.typeText("2")

                // Should show 2 items with "Test"
                val testLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("Test \\d+")) } }
                assertEquals(2, testLabels.size, "Should show 2 'Test' items")

                val expectedTestTexts = listOf("Test 1", "Test 2")
                val actualTestTexts = runOnSWT { testLabels.map { it.text }.sorted() }
                assertEquals(expectedTestTexts, actualTestTexts, "Test texts should match expected pattern")

                SweetDebugger.log("DynamicTextList", "Final state verified: ${testLabels.size} 'Test' labels")

                // Test invalid number input
                numberField.clearText()
                numberField.typeText("abc")

                // Should show 0 items for invalid input
                val invalidLabels = shell.findAll<Label> { runOnSWT { it.text.matches(Regex("Test \\d+")) } }
                assertEquals(0, invalidLabels.size, "Should show 0 items for invalid number input")

                SweetDebugger.log("DynamicTextList", "Invalid input handled: ${invalidLabels.size} labels")

                // Take screenshot of the final state
                shell.saveSVG()
                shell.saveScreenshot()

                SweetDebugger.log("DynamicTextList", "Dynamic text list test completed successfully")
            }
        }
    }

    @Test
    fun testBatching() {
        autoSWT {
            val shell =
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        BatchingTest()
                    }
                }

            runTest {
                val changeCount = AtomicInteger(0)
                val txt = shell.find<Text>()
                runOnSWT {
                    txt.addListener(SWT.Modify) {
                        changeCount.incrementAndGet()
                    }
                }

                assertEquals("X - 0", runOnSWT { txt.text })
                assertEquals(0, changeCount.get())

                val button = shell.find<Button>()
                repeat(3) {
                    button.doSelect()
                }

                assertEquals("XXXX - 3", runOnSWT { txt.text })

                assertEquals(3, changeCount.get())
            }
        }
    }

    @Test
    fun testCheckboxState() {
        autoSWT {
            val shell =
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        CheckBoxStateTest()
                    }
                }

            runTest {
                val button = shell.find<Button> { SWT.PUSH in it.style }
                val chkBoxes = shell.findAll<Button> { SWT.CHECK in it.style }

                val (chk1, chk2) = chkBoxes

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })

                button.doSelect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })

                button.doSelect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })

                chk1.doSelect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })

                chk1.doDeselect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })

                chk2.doSelect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })

                button.doSelect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })

                chk2.doDeselect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })
            }
        }
    }

    @Test
    fun testCheckboxStateInverted() {
        autoSWT {
            val shell =
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        CheckBoxStateTest()
                    }
                }

            runTest {
                val button = shell.find<Button> { SWT.PUSH in it.style }
                val chkBoxes = shell.findAll<Button> { SWT.CHECK in it.style }

                val (chk1, chk2, chk1b, chk2b) = chkBoxes

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })
                assertEquals(true, runOnSWT { chk1b.selection })
                assertEquals(true, runOnSWT { chk2b.selection })

                button.doSelect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })
                assertEquals(false, runOnSWT { chk1b.selection })
                assertEquals(false, runOnSWT { chk2b.selection })

                button.doSelect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })
                assertEquals(true, runOnSWT { chk1b.selection })
                assertEquals(true, runOnSWT { chk2b.selection })

                chk1.doSelect()

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })
                assertEquals(false, runOnSWT { chk1b.selection })
                assertEquals(false, runOnSWT { chk2b.selection })

                chk1.doDeselect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })
                assertEquals(true, runOnSWT { chk1b.selection })
                assertEquals(true, runOnSWT { chk2b.selection })

                println("XXXXXX 1")
                chk1b.doDeselect()

                println("XXXXXX 2")

                assertEquals(true, runOnSWT { chk1.selection })
                assertEquals(true, runOnSWT { chk2.selection })
                assertEquals(false, runOnSWT { chk1b.selection })
                assertEquals(false, runOnSWT { chk2b.selection })

                chk1b.doSelect()

                assertEquals(false, runOnSWT { chk1.selection })
                assertEquals(false, runOnSWT { chk2.selection })
                assertEquals(true, runOnSWT { chk1b.selection })
                assertEquals(true, runOnSWT { chk2b.selection })
            }
        }
    }

    @Composable
    fun BatchingTest() {
        var txt by remember { mutableStateOf("X") }
        var number by remember { mutableStateOf(0) }

        Column(Modifier.fillMaxSize()) {
            TextField("$txt - $number", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())

            Button(onClick = {
                txt = txt + "X"
                number++
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Click here")
            }
        }
    }

    @Composable
    fun CheckBoxStateTest() {
        var checked by remember { mutableStateOf(false) }

        Column {
            Button(onClick = {
                checked = !checked
            }) {
                Text("Click here")
            }

            Checkbox(checked, onCheckedChange = {
                checked = it
            })

            Checkbox(checked, onCheckedChange = {
            })

            Checkbox(!checked, onCheckedChange = {
                checked = !it
            })

            Checkbox(!checked, onCheckedChange = {
            })
        }
    }
}

class MyListManager {
    val myList = mutableStateListOf<Int>()
    var shift by mutableStateOf(0)
}
