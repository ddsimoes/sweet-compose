@file:Suppress("ktlint:standard:function-naming")

package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberImageBitmap
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.createTempFile

// Visible for tests: last bitmap dimensions produced by the sample.
var imageSampleBitmapSize: Pair<Int, Int>? = null

/**
 * Simple sample that demonstrates drawing an ImageBitmap loaded
 * via [rememberImageBitmap] from a file path, mirroring idiomatic
 * Compose Desktop usage.
 */
@Composable
fun ImageSampleApp() {
    val imagePath =
        remember {
            createSampleImageFile()
        }

    val bitmap: ImageBitmap? = rememberImageBitmap(imagePath)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp),
    ) {
        Text("Image Sample – SWT-backed ImageBitmap", modifier = Modifier.padding(bottom = 8.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text =
                        if (bitmap != null) {
                            "Bitmap size: ${bitmap.width} x ${bitmap.height}"
                        } else {
                            "Bitmap size: (loading...)"
                        },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),
        ) {
            if (bitmap != null) {
                imageSampleBitmapSize = bitmap.width to bitmap.height
                Image(
                    bitmap = bitmap,
                    contentDescription = "Sweet Image sample",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("Loading image...", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

/**
 * Entry point to run the Image sample manually.
 */
fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Image Sample",
        ) {
            ImageSampleApp()
        }
    }
}

private fun createSampleImageFile(): String {
    val tmpPath = createTempFile("sweet-image-sample", ".png")
    val tmpFile = tmpPath.toFile()
    val width = 128
    val height = 96

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()

    // Solid background
    g.color = Color(0x0D, 0x47, 0xA1)
    g.fillRect(0, 0, width, height)

    // White diagonal line
    g.color = Color.WHITE
    g.stroke = java.awt.BasicStroke(3f)
    g.drawLine(0, 0, width - 1, height - 1)

    g.dispose()

    ImageIO.write(image, "png", tmpFile)
    tmpFile.deleteOnExit()

    return tmpFile.absolutePath
}
