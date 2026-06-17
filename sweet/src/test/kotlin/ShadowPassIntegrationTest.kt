package io.github.ddsimoes.sweet.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.layout.SweetLayout
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertTrue

class ShadowPassIntegrationTest {
    @Test fun basic_composition() =
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize().padding(8.dp)) { Text("Hello") }
                }
            }.test { shell -> runOnSWT { assertTrue(shell.isVisible) } }
        }

    @Test fun shell_resize() =
        autoSWT {
            testShell(width = 200, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Row(Modifier.fillMaxSize().padding(8.dp)) {
                        Text("L")
                        Text("R")
                    }
                }
            }.test { shell ->
                runOnSWT {
                    shell.setSize(600, 400)
                    shell.layout()
                    assertTrue(shell.isVisible)
                }
            }
        }

    @Test fun nested_compose_containers_have_null_swt_layout() =
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Column(Modifier.fillMaxSize()) {
                        Row {
                            Text("A")
                            Text("B")
                        }
                    }
                }
            }.test { shell ->
                val composites = shell.findAll<Composite> { true }
                val sweetLayoutCount = runOnSWT { composites.count { it.layout is SweetLayout } }
                val nullLayoutCount = runOnSWT { composites.count { it.layout == null } }

                assertTrue(sweetLayoutCount >= 1, "The top-level Compose container should keep the layout driver")
                assertTrue(nullLayoutCount >= 1, "Nested Compose containers should not install SWT layouts")
            }
        }
}
