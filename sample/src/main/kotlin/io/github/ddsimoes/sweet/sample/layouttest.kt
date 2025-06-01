package io.github.ddsimoes.sweet.sample

import androidx.compose.runtime.*
import io.github.ddsimoes.sweet.Button
import io.github.ddsimoes.sweet.Card
import io.github.ddsimoes.sweet.Column
import io.github.ddsimoes.sweet.ComposeSWTTarget
import io.github.ddsimoes.sweet.Row
import io.github.ddsimoes.sweet.SWTModifier
import io.github.ddsimoes.sweet.SWTWindowConfig
import io.github.ddsimoes.sweet.Spacer
import io.github.ddsimoes.sweet.Text
import io.github.ddsimoes.sweet.backgroundColor
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.fillMaxWidth
import io.github.ddsimoes.sweet.padding
import io.github.ddsimoes.sweet.runComposeSWTWithSWTDispatcher
import org.eclipse.swt.graphics.RGB

/**
 * Simple layout test to verify Row/Column layout data compatibility
 */
@ComposeSWTTarget
fun layoutTest() {
    SweetDebugger.enable()

    runComposeSWTWithSWTDispatcher(
        config = SWTWindowConfig(
            title = "Layout Test - Row/Column Compatibility",
            width = 500,
            height = 300
        )
    ) {
        LayoutTestContent()
    }
}

@ComposeSWTTarget
@Composable
fun LayoutTestContent() {
    Column(
        modifier = SWTModifier
            .fillMaxWidth()
            .backgroundColor(RGB(248, 250, 252))
            .padding(16)
    ) {
        Text(
            text = "Layout Compatibility Test",
            modifier = SWTModifier.padding(8)
        )

        // Test Row layout with fillMaxWidth elements
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                Text("Testing Row Layout:")

                Spacer(modifier = SWTModifier.padding(4))

                Row {
                    Button(
                        text = "Button 1",
                        onClick = {},
                        modifier = SWTModifier.padding(4, 0)
                    )
                    Button(
                        text = "Button 2",
                        onClick = {},
                        modifier = SWTModifier.padding(4, 0)
                    )
                    Button(
                        text = "Button 3",
                        onClick = {}
                    )
                }
            }
        }

        Spacer(modifier = SWTModifier.padding(8))

        // Test Column layout with fillMaxWidth elements
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                Text("Testing Column Layout:")

                Spacer(modifier = SWTModifier.padding(4))

                Text(
                    text = "Item 1",
                    modifier = SWTModifier.fillMaxWidth().padding(0, 2)
                )
                Text(
                    text = "Item 2",
                    modifier = SWTModifier.fillMaxWidth().padding(0, 2)
                )
                Text(
                    text = "Item 3",
                    modifier = SWTModifier.fillMaxWidth().padding(0, 2)
                )
            }
        }

        Spacer(modifier = SWTModifier.padding(8))

        // Test mixed layouts
        Card(
            backgroundColor = RGB(255, 255, 255),
            modifier = SWTModifier.fillMaxWidth().padding(8)
        ) {
            Column(modifier = SWTModifier.padding(16)) {
                Text("Mixed Layout Test:")

                Spacer(modifier = SWTModifier.padding(4))

                Row {
                    Column {
                        Text("Left Column")
                        Button(
                            text = "Action A",
                            onClick = {},
                            modifier = SWTModifier.padding(0, 2)
                        )
                        Button(
                            text = "Action B",
                            onClick = {},
                            modifier = SWTModifier.padding(0, 2)
                        )
                    }

                    Column {
                        Text("Right Column")
                        Button(
                            text = "Action C",
                            onClick = {},
                            modifier = SWTModifier.padding(0, 2)
                        )
                        Button(
                            text = "Action D",
                            onClick = {},
                            modifier = SWTModifier.padding(0, 2)
                        )
                    }
                }
            }
        }

        Spacer(modifier = SWTModifier.padding(8))

        Text(
            text = "✅ If you can see this, the layout compatibility is working!",
            modifier = SWTModifier.padding(8)
        )
    }
}

// Main function
fun main() {
    layoutTest()
}