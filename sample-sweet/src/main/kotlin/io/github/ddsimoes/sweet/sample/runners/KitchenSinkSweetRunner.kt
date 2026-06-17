@file:JvmName("KitchenSinkSweetRunner")

package io.github.ddsimoes.sweet.sample.runners

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.kitchensink.KitchenSinkApp

/**
 * Sweet (SWT-backed) runner for the Kitchen Sink sample.
 */
fun main() {
    SweetDebugger.enable()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Compose Kitchen Sink",
        ) {
            KitchenSinkApp()
        }
    }
}

