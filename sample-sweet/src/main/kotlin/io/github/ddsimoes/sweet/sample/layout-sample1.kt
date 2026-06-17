package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Minimal Row Test"
        ) {
            MinimalRowTest()
        }
    }
}

@Composable
fun MinimalRowTest() {
    Column {
        Text("Before Row")

        // Teste mínimo: apenas 2 elementos em Row
        Row {
            Text("A")
            Text("B")
        }

        Text("After Row")

        // Teste com botões
        Row {
            Button(onClick = {}) { Text("X") }
            Button(onClick = {}) { Text("Y") }
        }

        Text("End")
    }
}

/*
Se os textos A e B aparecerem verticalmente (um embaixo do outro),
o problema está no Row.

Se aparecerem horizontalmente (lado a lado), o Row funciona
e o problema é com layouts mais complexos.

Execute este teste e veja como fica o layout dos elementos A/B e X/Y.
*/
