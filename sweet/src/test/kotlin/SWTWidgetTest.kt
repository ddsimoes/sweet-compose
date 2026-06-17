package io.github.ddsimoes.sweet.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.compose.integration.SWTWidget
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Smoke tests for SWTWidget. Full parenting correctness testing is
 * gated on doc 11 (layout engine rewrite) which fixes the current
 * applier-scoping issue noted in QW-10.
 */
class SWTWidgetTest {
    @Test
    fun swtWidget_composes_and_renders_native_label() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val rootComposite = Composite(this, SWT.NONE)
                rootComposite.layout = FillLayout()

                rootComposite.embedCompose {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    ) {
                        Text("Before")

                        SWTWidget(
                            modifier = Modifier.padding(4.dp),
                        ) { parent ->
                            Label(parent, SWT.NONE).apply {
                                text = "SWT Label"
                            }
                        }

                        Text("After")
                    }
                }
            }.test { shell ->
                runOnSWT {
                    val swtLabels = shell.findAll<Label> { it.text == "SWT Label" }
                    assertEquals(1, swtLabels.size, "Should render one SWT Label")

                    val label = swtLabels[0]
                    assertNotNull(label.parent, "Label should have a parent")
                }
            }
        }
    }
}
