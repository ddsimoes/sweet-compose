package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Counter Example",
        ) {
            MinimalApp()
        }
    }
}

@Composable
fun MinimalApp() {
    var count by mutableStateOf(0)

    Row(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { count++ },
            //modifier = Modifier.padding(10.dp)
        ) {
            Text("Value: $count")
        }
    }
}
