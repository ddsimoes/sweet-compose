@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertTrue

class ShadowTreeMoveTest {
    @Test
    fun keyed_reorder_updates_shadow_layout_order() {
        autoSWT {
            testShell(width = 320, height = 240) {
                val root = Composite(this, SWT.NONE)
                root.layout = FillLayout()
                root.embedCompose {
                    KeyedReorderApp()
                }
            }.test { shell ->
                val aBefore = shell.find<Label> { it.text == "Item A" }
                val cBefore = shell.find<Label> { it.text == "Item C" }
                val initialOrderCorrect = runOnSWT { aBefore.bounds.y < cBefore.bounds.y }
                assertTrue(initialOrderCorrect, "Initial layout should place A above C")

                val reverse = shell.find<org.eclipse.swt.widgets.Button> { it.text == "Reverse" }
                reverse.doSelect()

                val aAfter = shell.find<Label> { it.text == "Item A" }
                val cAfter = shell.find<Label> { it.text == "Item C" }
                val reordered = runOnSWT { cAfter.bounds.y < aAfter.bounds.y }
                assertTrue(reordered, "After keyed reorder, shadow layout should place C above A")
            }
        }
    }

    @Composable
    private fun KeyedReorderApp() {
        var reversed by remember { mutableStateOf(false) }
        val items = if (reversed) listOf("C", "B", "A") else listOf("A", "B", "C")

        Column(Modifier.fillMaxWidth()) {
            Button(onClick = { reversed = !reversed }) {
                Text("Reverse")
            }
            items.forEach { item ->
                key(item) {
                    Text(
                        text = "Item $item",
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                    )
                }
            }
        }
    }
}
