import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Canvas as SWTCanvas

/**
 * Tests for the MPP-compatible loaders in androidx.compose.ui.res:
 * [loadImageBitmap] and [useResource].
 */
class ImageResourcesTest {
    private fun pngBytes(
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(0x21, 0x96, 0xF3)
        g.fillRect(0, 0, width, height)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    @Test
    fun loadImageBitmap_decodes_stream_without_a_display() {
        // No SWT Display exists in this test: decoding must succeed anyway,
        // since the native Image is only materialized at draw time.
        val bitmap = ByteArrayInputStream(pngBytes(20, 10)).use(::loadImageBitmap)
        assertEquals(20, bitmap.width, "Decoded bitmap width")
        assertEquals(10, bitmap.height, "Decoded bitmap height")
    }

    @Test
    fun useResource_loads_classpath_png() {
        val bitmap = useResource("images/test-image.png", ::loadImageBitmap)
        assertEquals(20, bitmap.width, "Classpath bitmap width")
        assertEquals(10, bitmap.height, "Classpath bitmap height")
    }

    @Test
    fun useResource_throws_for_missing_resource() {
        assertFailsWith<IllegalArgumentException> {
            useResource("images/does-not-exist.png") { it.readBytes() }
        }
    }

    @Test
    fun image_renders_bitmap_loaded_via_loadImageBitmap() {
        val bitmap: ImageBitmap = ByteArrayInputStream(pngBytes(16, 16)).use(::loadImageBitmap)
        autoSWT {
            testShell(width = 200, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Loaded image",
                        modifier = Modifier,
                    )
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Shell should contain a Canvas for the Image composable")
            }
        }
    }
}
