import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Button as SWTButton

/**
 * Regression test for draw-phase snapshot observation: state that is read
 * ONLY inside a Canvas onDraw lambda (never in composition) must still
 * invalidate the canvas when it changes. Before the fix, clicking the
 * button updated the state but the canvas never repainted, because nothing
 * recomposed and the draw-time read was unobserved.
 */
class CanvasReactiveRedrawTest {
    @Test
    fun drawOnlyStateChangeTriggersRedraw() {
        val drawnValues = CopyOnWriteArrayList<Int>()
        var counter by mutableStateOf(0)

        autoSWT {
            testShell(width = 300, height = 200) {
                layout = FillLayout()
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column {
                        // counter is intentionally NOT read here in composition.
                        Canvas(Modifier.width(100.dp).height(100.dp)) {
                            drawnValues.add(counter)
                            drawRect(
                                color = if (counter % 2 == 0) Color.Red else Color.Blue,
                                size = size,
                            )
                        }
                        Button(onClick = { counter++ }) { Text("inc") }
                    }
                }
            }.test { shell ->
                // Paint events are delivered asynchronously by the running
                // event loop; poll for the expected draw with a deadline.
                fun awaitDrawn(value: Int): Boolean {
                    val deadline = System.currentTimeMillis() + 3000
                    while (System.currentTimeMillis() < deadline) {
                        if (drawnValues.contains(value)) return true
                        Thread.sleep(20)
                    }
                    return drawnValues.contains(value)
                }

                assertTrue(awaitDrawn(0), "Canvas should paint initially with counter=0")

                val button = shell.findAll<SWTButton> { it.text == "inc" }.first()
                button.doSelect()

                assertTrue(
                    awaitDrawn(1),
                    "Canvas should repaint with counter=1 after click; drawn values: $drawnValues",
                )
            }
        }
    }
}
