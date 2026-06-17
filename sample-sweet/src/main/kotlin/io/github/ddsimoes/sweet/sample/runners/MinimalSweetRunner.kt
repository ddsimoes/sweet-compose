@file:JvmName("MinimalSweetRunner")

package io.github.ddsimoes.sweet.sample.runners

import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.debug.SwtDebugger.runSWT
import io.github.ddsimoes.sweet.sample.MinimalApp
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell

/**
 * Sweet (SWT-backed) runner for the Minimal sample.
 */
fun main() {
    SweetDebugger.enable()

    val mode = "app"
    // val mode = "embedded"

    if (mode == "app") {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Sweet Counter Example",
            ) {
                MinimalApp()
            }
        }
    } else {
        runSWT { shell: Shell ->
            val composite = Composite(shell, SWT.NONE)
            composite.layout = FillLayout()
            composite.embedCompose {
                MinimalApp()
            }
        }
    }
}

