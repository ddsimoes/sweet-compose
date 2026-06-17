package io.github.ddsimoes.sweet.sample

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Animation Demo",
        ) {
            AnimationDemoApp()
        }
    }
}

@Composable
fun AnimationDemoApp() {
    var selectedEasing by remember { mutableIntStateOf(0) }
    val easingNames = arrayOf("FastOutSlowIn", "Linear", "EaseInOutCubic")
    val easings = arrayOf<Easing>(
        FastOutSlowInEasing,
        LinearEasing,
        EaseInOutCubic,
    )

    var running by remember { mutableIntStateOf(1) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(running) {
        if (running == 0) return@LaunchedEffect
        var t = 0f
        while (true) {
            delay(16) // ~60fps
            t += 0.016f / 2f
            if (t > 1f) t -= 1f
            progress = t
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Animation Math Demo")
        Spacer(Modifier.height(8.dp))
        Text("Easing: ${easingNames[selectedEasing]}")

        Spacer(Modifier.height(8.dp))

        val easing = easings[selectedEasing]
        val easedProgress = easing.transform(progress)
        val boxOffset = (easedProgress * 200).toInt()

        Box(
            Modifier
                .size(200.dp, 100.dp)
                .background(Color.LightGray),
        ) {
            Box(
                Modifier
                    .padding(start = boxOffset.dp, top = 30.dp)
                    .size(40.dp)
                    .background(Color.Blue),
            ) { }
        }

        Spacer(Modifier.height(16.dp))

        Row {
            Button(
                onClick = { running = if (running == 0) 1 else 0 },
            ) {
                Text(if (running != 0) "Pause" else "Resume")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { selectedEasing = (selectedEasing + 1) % easings.size },
            ) {
                Text("Easing")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Easing values at 0, 0.25, 0.5, 0.75, 1.0:\n" +
                "  " + listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    .joinToString("  ") { "%.3f".format(easing.transform(it)) },
        )
    }
}
