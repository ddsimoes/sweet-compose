@file:Suppress("ktlint:standard:function-naming")

package io.github.ddsimoes.sweet.sample

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

private val InitialData = listOf(3, 7, 4, 6, 2, 5)

// Visible for tests: counts how many times the Canvas draw lambda runs.
var canvasSampleDrawCount: Int = 0

/**
 * Simple Canvas-based sample that draws a bar chart for an integer data series.
 *
 * The UI also shows a textual summary so tests can assert on data changes without
 * needing pixel-level inspection.
 */
@Composable
fun CanvasSampleApp() {
    var data by remember { mutableStateOf(InitialData) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp),
    ) {
        Text("Canvas Sample – Bar Chart", modifier = Modifier.padding(bottom = 8.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                val total = data.size
                val max = data.maxOrNull() ?: 0
                Text(
                    text = "Data points: $total, max=$max",
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                Row {
                    Button(
                        onClick = {
                            val nextValue = (data.maxOrNull() ?: 0).coerceAtLeast(1) + 1
                            data = data + nextValue
                        },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text("Add Point")
                    }

                    Button(
                        onClick = {
                            data = InitialData
                        },
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),
        ) {
            BarChartCanvas(data = data)
        }
    }
}

@Composable
private fun BarChartCanvas(data: List<Int>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        canvasSampleDrawCount++

        if (data.isEmpty()) return@Canvas

        // Clear the canvas area with a distinctive background so it's obvious
        // when drawing is occurring, even in screenshots.
        drawRect(
            color = Color(0xFF263238u), // dark blue-grey
            topLeft = Offset.Zero,
            size =
                androidx.compose.ui.geometry
                    .Size(size.width, size.height),
        )

        val barCount = data.size
        val maxValue = (data.maxOrNull() ?: 0).coerceAtLeast(1)

        val chartWidth = size.width
        val chartHeight = size.height

        val barSpacing = 8f
        val totalSpacing = barSpacing * (barCount + 1)
        val availableWidth = (chartWidth - totalSpacing).coerceAtLeast(0f)
        val barWidth = if (barCount > 0) availableWidth / barCount else 0f

        // Draw baseline
        val baselineY = chartHeight - 20f
        drawLine(
            color = Color.Gray,
            start = Offset(0f, baselineY),
            end = Offset(chartWidth, baselineY),
            strokeWidth = 2f,
        )

        data.forEachIndexed { index, value ->
            val normalized = value.toFloat() / maxValue.toFloat()
            val barHeight = (chartHeight - 40f) * normalized

            val left = barSpacing + index * (barWidth + barSpacing)
            val top = baselineY - barHeight
            val right = left + barWidth

            val barColor =
                when (index % 3) {
                    0 -> Color(0xFF64B5F6u)

                    // blue-ish
                    1 -> Color(0xFF81C784u)

                    // green-ish
                    else -> Color(0xFFFFB74Du) // orange-ish
                }

            drawRect(
                color = barColor,
                topLeft = Offset(left, top),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = (right - left).coerceAtLeast(1f),
                        height = barHeight.coerceAtLeast(1f),
                    ),
            )
        }

        // Draw a border around the chart area using a stroked path.
        val borderPath =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(chartWidth, 0f)
                lineTo(chartWidth, chartHeight)
                lineTo(0f, chartHeight)
                close()
            }

        drawPath(
            path = borderPath,
            color = Color.LightGray,
            style = Stroke(width = 1f),
        )
    }
}

/**
 * Entry point to run the Canvas sample manually.
 */
fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Canvas Sample",
        ) {
            CanvasSampleApp()
        }
    }
}
