@file:Suppress("ktlint:standard:function-naming")

import androidx.compose.ui.graphics.BitmapPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorPainter
import androidx.compose.ui.graphics.ImageBitmap
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.drawing.rasterizeToImage
import io.github.ddsimoes.sweet.image.ImageDataImageBitmap
import io.github.ddsimoes.sweet.image.SweetImageBitmapImpl
import org.eclipse.swt.graphics.PaletteData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for the Painter → SWT Image rasterizer used to apply a window icon ([WindowConfig.icon]).
 */
class WindowIconTest {

    @Test
    fun `rasterizeToImage returns null for a sizeless painter`() {
        autoSWT {
            testShell(width = 100, height = 100) {
                // no content — we only need a Display
            }.test { shell ->
                runOnSWT {
                    assertNull(ColorPainter(Color.Red).rasterizeToImage(shell.display))
                }
            }
        }
    }

    @Test
    fun `rasterizeToImage renders a bitmap painter at its intrinsic size`() {
        autoSWT {
            testShell(width = 100, height = 100) {
                // no content
            }.test { shell ->
                // Image/GC allocation must happen on the SWT UI thread.
                val image = runOnSWT {
                    val palette = PaletteData(0xFF, 0xFF00, 0xFF0000)
                    val imageData = org.eclipse.swt.graphics.ImageData(16, 16, 24, palette)
                    val bitmap: ImageBitmap = SweetImageBitmapImpl(ImageDataImageBitmap(imageData))
                    BitmapPainter(bitmap).rasterizeToImage(shell.display)
                }

                assertNotNull(image)
                assertEquals(16, image!!.bounds.width)
                assertEquals(16, image.bounds.height)
                runOnSWT { image.dispose() }
            }
        }
    }
}
