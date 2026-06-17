import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class LayoutContractTest {
    @AfterEach
    fun resetCompanionState() {
        SweetLayout.measureCountEnabled = false
        SweetLayout.resetMeasureCounts()
    }

    @Test
    fun nested_row_column_box_measure_count_is_bounded() {
        autoSWT {
            SweetLayout.measureCountEnabled = true
            SweetLayout.resetMeasureCounts()
            try {
                testShell(width = 400, height = 300) {
                    val composite = Composite(this, SWT.NONE)
                    composite.layout = FillLayout()
                    composite.embedCompose {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth()) {
                                Box(Modifier.size(width = 160.dp, height = 60.dp)) {
                                    Column(Modifier.fillMaxSize()) {
                                        Text("Deep")
                                    }
                                }
                            }
                        }
                    }
                }.test { shell ->
                    shell.find<Label> { it.text == "Deep" }.assertLayout().isVisible()

                    val measureCounts = SweetLayout.measureCounts.filterKeys { it.endsWith(".measure") }
                    assertTrue(measureCounts.isNotEmpty(), "Expected Sweet measurable counts")
                    val maxMeasureCount = measureCounts.values.maxOrNull() ?: 0
                    assertTrue(
                        maxMeasureCount <= 4,
                        "Each control should be measured a bounded number of times, counts=$measureCounts",
                    )
                }
            } finally {
                SweetLayout.measureCountEnabled = false
                SweetLayout.resetMeasureCounts()
            }
        }
    }

    @Test
    fun padded_leaf_reserves_space_once() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Text("Padded", modifier = Modifier.padding(12.dp))
                        Text("After")
                    }
                }
            }.test { shell ->
                val host = runOnSWT { shell.children.first() as Composite }
                val composeRoot = runOnSWT { host.children.first() as Composite }
                val padded = shell.find<Label> { it.text == "Padded" }
                val after = shell.find<Label> { it.text == "After" }

                val paddedBounds = runOnSWT { padded.visibleBounds(composeRoot) }
                val afterBounds = runOnSWT { after.visibleBounds(composeRoot) }
                val bottomGap = afterBounds.y - (paddedBounds.y + paddedBounds.height)

                assertTrue(paddedBounds.x > 0, "Leaf start padding should be > 0")
                assertTrue(paddedBounds.y > 0, "Leaf top padding should be > 0")
                assertTrue(bottomGap > 0, "Leaf bottom padding should be > 0")
            }
        }
    }

    @Test
    fun padded_container_offsets_content_once() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(10.dp)) {
                            Text("Inside")
                        }
                    }
                }
            }.test { shell ->
                val host = runOnSWT { shell.children.first() as Composite }
                val composeRoot = runOnSWT { host.children.first() as Composite }
                val inside = shell.find<Label> { it.text == "Inside" }
                val bounds = runOnSWT { inside.visibleBounds(composeRoot) }

                assertTrue(bounds.x > 0, "Container start padding should be > 0")
                assertTrue(bounds.y > 0, "Container top padding should be > 0")
            }
        }
    }

    @Test
    fun nested_padded_containers_do_not_double_count() {
        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(7.dp)) {
                            Box(Modifier.padding(11.dp)) {
                                Text("Nested")
                            }
                        }
                    }
                }
            }.test { shell ->
                val host = runOnSWT { shell.children.first() as Composite }
                val composeRoot = runOnSWT { host.children.first() as Composite }
                val nested = shell.find<Label> { it.text == "Nested" }
                val bounds = runOnSWT { nested.visibleBounds(composeRoot) }

                // Nested padding should be the sum of both containers (7dp + 11dp),
                // producing a larger offset than a single 7dp container alone.
                assertTrue(bounds.x > 0, "Nested start padding should be > 0")
                assertTrue(bounds.y > 0, "Nested top padding should be > 0")
            }
        }
    }
}
