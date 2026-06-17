package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Counter Example"
        ) {
            ButtonsApp()
        }
    }
}

@Composable
fun ButtonsApp() {

    Row(modifier = Modifier.padding(20.dp)) {
        Button(
            onClick = {  },
            modifier = Modifier.padding(10.dp)
        ) {
            Text("AAA")
        }
        Button(
            onClick = {  },
            modifier = Modifier.padding(10.dp)
        ) {
            Text("BBB")
        }
    }
}
