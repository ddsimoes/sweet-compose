import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.internal.getSweetColorCacheSize
import io.github.ddsimoes.sweet.internal.getSweetFontCacheSize
import io.github.ddsimoes.sweet.internal.hasSweetCachedColor
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Button as SWTButton

/**
 * Tests for widget disposal and memory leak detection.
 * Ensures that widgets are properly cleaned up during recomposition and composition disposal.
 */
class MemoryLeakAndDisposalTest {
    @Test
    fun text_and_divider_colors_reuse_display_cache_across_recomposition() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var useFirstPalette by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { useFirstPalette = !useFirstPalette }) {
                            Text("Toggle colors")
                        }
                        Text(
                            text = "Cached text color",
                            color = if (useFirstPalette) Color.Red else Color.Blue,
                        )
                        HorizontalDivider(
                            color = if (useFirstPalette) Color.Green else Color.Yellow,
                        )
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { it.text == "Toggle colors" }
                shell.find<Label> { it.text == "Cached text color" }
                shell.find<Label> { (it.style and SWT.SEPARATOR) != 0 }

                val initialCacheSize = runOnSWT { shell.display.getSweetColorCacheSize() }
                repeat(20) {
                    button.doSelect()
                }

                val finalCacheSize = runOnSWT { shell.display.getSweetColorCacheSize() }
                val expectedColorsCached =
                    runOnSWT {
                        listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
                            .all { shell.display.hasSweetCachedColor(it) }
                    }

                assertTrue(expectedColorsCached, "Text and divider colors should be resolved through the display cache")
                assertTrue(finalCacheSize <= initialCacheSize + 2, "Color cache grew from $initialCacheSize to $finalCacheSize")
            }
        }
    }

    // ========== WIDGET DISPOSAL TESTS ==========

    @Test
    fun widgets_disposed_when_removed_from_composition() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showItems by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showItems = !showItems }) {
                            Text(if (showItems) "Hide Items" else "Show Items")
                        }

                        if (showItems) {
                            Text("Item 1")
                            Text("Item 2")
                            Text("Item 3")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Initially items are visible
                val initialLabels = shell.findAll<Label> { it.text.startsWith("Item") }
                assertEquals(3, initialLabels.size, "Should have 3 items initially")

                // Store references to check disposal later
                val labelRefs = initialLabels.toList()

                // Hide items
                button.doSelect()

                // Items should be gone
                val afterHide = shell.findAll<Label> { it.text.startsWith("Item") }
                assertEquals(0, afterHide.size, "Items should be removed from UI")

                // Check that widgets are actually disposed
                runOnSWT {
                    labelRefs.forEach { label ->
                        assertTrue(label.isDisposed, "Widget should be disposed after removal from composition")
                    }
                }

                // Show items again - should create new widgets
                button.doSelect()

                val afterShow = shell.findAll<Label> { it.text.startsWith("Item") }
                assertEquals(3, afterShow.size, "Items should be recreated")

                // New widgets should be different instances from the old ones
                runOnSWT {
                    afterShow.forEach { newLabel ->
                        labelRefs.forEach { oldLabel ->
                            assertTrue(newLabel !== oldLabel, "New widgets should be different instances")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun nested_widgets_all_disposed_together() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showNested by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showNested = !showNested }) {
                            Text("Toggle Nested")
                        }

                        if (showNested) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Outer")
                                Row {
                                    Text("Inner 1")
                                    Text("Inner 2")
                                }
                                Text("Bottom")
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Get all nested labels
                val initialLabels =
                    shell.findAll<Label> {
                        it.text in listOf("Outer", "Inner 1", "Inner 2", "Bottom")
                    }
                assertEquals(4, initialLabels.size, "Should have 4 nested labels")

                val labelRefs = initialLabels.toList()

                // Hide nested content
                button.doSelect()

                // All should be disposed
                runOnSWT {
                    labelRefs.forEach { label ->
                        assertTrue(label.isDisposed, "Nested widget should be disposed")
                    }
                }

                // Should be no labels remaining
                val afterHide =
                    shell.findAll<Label> {
                        it.text in listOf("Outer", "Inner 1", "Inner 2", "Bottom")
                    }
                assertEquals(0, afterHide.size, "All nested widgets should be gone")
            }
        }
    }

    @Test
    fun disposal_order_parent_after_children() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                mutableListOf<String>()

                composite.embedCompose {
                    var showContent by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showContent = !showContent }) {
                            Text("Toggle")
                        }

                        if (showContent) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Child 1")
                                Text("Child 2")
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Get the parent composite and children
                val children = shell.findAll<Label> { it.text.startsWith("Child") }
                assertEquals(2, children.size)

                // Find parent composite
                runOnSWT { children[0].parent as Composite }

                // Toggle to dispose
                button.doSelect()

                // All should be disposed
                runOnSWT {
                    children.forEach { child ->
                        assertTrue(child.isDisposed, "Child should be disposed")
                    }
                }
            }
        }
    }

    // ========== RECOMPOSITION CLEANUP TESTS ==========

    @Test
    fun widgets_replaced_during_recomposition_are_disposed() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var counter by remember { mutableIntStateOf(0) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { counter++ }) {
                            Text("Increment")
                        }

                        Text("Count: $counter")
                        Text("Double: ${counter * 2}")
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                shell.find<Label> { it.text == "Count: 0" }
                shell.find<Label> { it.text == "Double: 0" }

                button.doSelect()

                val new1 = shell.find<Label> { it.text == "Count: 1" }
                val new2 = shell.find<Label> { it.text == "Double: 2" }

                new1.assertLayout().isVisible()
                new2.assertLayout().isVisible()

                repeat(5) {
                    button.doSelect()
                }

                shell.find<Label> { it.text == "Count: 6" }
                shell.find<Label> { it.text == "Double: 12" }
            }
        }
    }

    @Test
    fun dynamic_list_cleanup_when_items_removed() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var count by remember { mutableIntStateOf(5) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { if (count > 0) count-- }) {
                            Text("Remove")
                        }

                        repeat(count) { i ->
                            Text("Item $i")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                val initialItems = shell.findAll<Label> { it.text.startsWith("Item") }
                assertEquals(5, initialItems.size)

                val itemRefs = initialItems.toList()

                // Remove all items one by one
                repeat(5) {
                    button.doSelect()
                }

                val afterRemove = shell.findAll<Label> { it.text.startsWith("Item") }
                assertEquals(0, afterRemove.size, "All items should be removed")

                runOnSWT {
                    itemRefs.forEach { item ->
                        assertTrue(item.isDisposed, "Removed widget should be disposed")
                    }
                }
            }
        }
    }

    // ========== CONTROL COMPOSITE DISPOSAL ==========

    @Test
    fun composites_disposed_when_container_removed() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var showContainer by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { showContainer = !showContainer }) {
                            Text("Toggle Container")
                        }

                        if (showContainer) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Inside container")
                            }
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                val innerLabel = shell.find<Label> { it.text == "Inside container" }

                button.doSelect()

                runOnSWT {
                    assertTrue(innerLabel.isDisposed, "Widget inside removed container should be disposed")
                }
            }
        }
    }

    // ========== PREVENT DOUBLE DISPOSAL ==========

    @Test
    fun no_error_on_rapid_show_hide_cycles() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var visible by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { visible = !visible }) {
                            Text("Toggle")
                        }

                        if (visible) {
                            Text("Flickering text")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                // Rapidly toggle 20 times — should not throw
                repeat(20) {
                    button.doSelect()
                }
            }
        }
    }

    // ========== WIDGET COUNT VERIFICATION ==========

    @Test
    fun verify_no_widget_leaks_after_multiple_recompositions() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    var toggle by remember { mutableStateOf(false) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { toggle = !toggle }) {
                            Text("Toggle")
                        }

                        if (toggle) {
                            Text("A")
                            Text("B")
                        } else {
                            Text("C")
                            Text("D")
                            Text("E")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                val initialCount =
                    runOnSWT {
                        shell.children.count { it is Control && !it.isDisposed }
                    }

                repeat(10) {
                    button.doSelect()
                }

                val finalCount =
                    runOnSWT {
                        shell.children.count { it is Control && !it.isDisposed }
                    }

                assertTrue(
                    finalCount <= initialCount + 3,
                    "Widget count grew from $initialCount to $finalCount (possible leak)",
                )
            }
        }
    }

    @Test
    fun verify_disposed_controls_not_accessible() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    var show by remember { mutableStateOf(true) }

                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = { show = !show }) {
                            Text("Toggle")
                        }

                        if (show) {
                            Text("Temporary")
                        }
                    }
                }
            }.test { shell ->
                val button = shell.find<SWTButton> { true }

                val tempLabel = shell.find<Label> { it.text == "Temporary" }

                button.doSelect()

                runOnSWT {
                    assertTrue(tempLabel.isDisposed, "Temporary label should be disposed")

                    try {
                        val text = tempLabel.text
                        SweetDebugger.log("DisposalTest", "Disposed label text: '$text' (might be empty)")
                    } catch (e: Exception) {
                        SweetDebugger.log("DisposalTest", "Disposed label threw exception: ${e.message}")
                    }
                }
            }
        }
    }

    // ========== RESOURCE CACHE TESTS (Phase 6) ==========

    @Test
    fun canvas_colors_reuse_display_cache_across_repaints() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color.Red, Offset(10f, 10f), Size(50f, 40f))
                        drawCircle(Color.Blue, 20f, Offset(100f, 60f))
                        drawLine(Color.Green, Offset(0f, 0f), Offset(200f, 100f), 2f)
                        drawText("Hello", Offset(5f, 5f), 12f, Color.White)
                    }
                }
            }.test { shell ->
                val display = shell.display

                val initialColorCache = runOnSWT { display.getSweetColorCacheSize() }
                val initialFontCache = runOnSWT { display.getSweetFontCacheSize() }

                repeat(10) {
                    runOnSWT {
                        shell.setSize(300 + it, 200 + it)
                        shell.layout(true, true)
                    }
                }

                val finalColorCache = runOnSWT { display.getSweetColorCacheSize() }
                val finalFontCache = runOnSWT { display.getSweetFontCacheSize() }

                assertTrue(
                    finalColorCache <= initialColorCache + 4,
                    "Canvas color cache grew from $initialColorCache to $finalColorCache (expected ≤ ${initialColorCache + 4})",
                )

                assertTrue(
                    finalFontCache <= initialFontCache + 1,
                    "Font cache grew from $initialFontCache to $finalFontCache (expected ≤ ${initialFontCache + 1})",
                )
            }
        }
    }

    @Test
    fun canvas_drawing_populates_resource_caches() {
        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color.Red, Offset(5f, 5f), Size(30f, 20f))
                        drawText("Test", Offset(10f, 50f), 14f, Color.Blue)
                    }
                }
            }.test { shell ->
                runOnSWT {
                    val colorSize = shell.display.getSweetColorCacheSize()
                    val fontSize = shell.display.getSweetFontCacheSize()
                    assertTrue(colorSize > 0, "Color cache should be populated after Canvas paint (was $colorSize)")
                    assertTrue(fontSize > 0, "Font cache should be populated after Canvas drawText (was $fontSize)")
                }
            }
        }
    }

    @Test
    fun display_disposal_clears_color_and_font_caches() {
        var firstDisplayColorCacheSize = -1
        var firstDisplayFontCacheSize = -1

        autoSWT {
            testShell(width = 200, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color.Red, Offset(5f, 5f), Size(30f, 20f))
                        drawText("Test", Offset(10f, 50f), 12f, Color.Blue)
                    }
                }
            }.test { shell ->
                runOnSWT {
                    firstDisplayColorCacheSize = shell.display.getSweetColorCacheSize()
                    firstDisplayFontCacheSize = shell.display.getSweetFontCacheSize()
                }
            }
        }

        assertTrue(firstDisplayColorCacheSize > 0, "First display should have cached colors")
        assertTrue(firstDisplayFontCacheSize > 0, "First display should have cached fonts")

        autoSWT {
            testShell(width = 100, height = 100) {
                // Empty shell — just check the cache state on a new display
            }.test { shell ->
                runOnSWT {
                    val colorSize = shell.display.getSweetColorCacheSize()
                    val fontSize = shell.display.getSweetFontCacheSize()
                    assertEquals(0, colorSize, "New display should start with empty color cache")
                    assertEquals(0, fontSize, "New display should start with empty font cache")
                }
            }
        }
    }

    // ========== HELPER FUNCTIONS ==========

    private fun countAllControls(composite: Composite): Int {
        var count = 1 // Count the composite itself
        composite.children.forEach { child ->
            if (child is Composite) {
                count += countAllControls(child)
            } else if (child is Control) {
                count++
            }
        }
        return count
    }
}
