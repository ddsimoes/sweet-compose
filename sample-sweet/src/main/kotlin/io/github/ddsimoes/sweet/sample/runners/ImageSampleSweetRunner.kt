@file:JvmName("ImageSampleSweetRunner")

package io.github.ddsimoes.sweet.sample.runners

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.ImageSampleApp

/**
 * Sweet (SWT-backed) runner for the Image sample.
 */
fun main() {
    SweetDebugger.enable()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet – Image Sample",
        ) {
            ImageSampleApp()
        }
    }
}

