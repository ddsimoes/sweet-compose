import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.image.fromSwtImage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertTrue
import org.eclipse.swt.graphics.Image as SWTImage
import org.eclipse.swt.widgets.Canvas as SWTCanvas

class ImageTest {
    @Test
    fun image_composes_with_bitmap() {
        autoSWT {
            testShell(width = 200, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                // Create a simple SWT image and wrap it as ImageBitmap
                val bitmap: ImageBitmap =
                    runOnSWT {
                        val display = this.display
                        val swtImage = SWTImage(display, 16, 16)
                        ImageBitmap.fromSwtImage(swtImage, display)
                    }

                composite.embedCompose {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Test image",
                        modifier = Modifier,
                    )
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Shell should contain a Canvas for Image composable")
            }
        }
    }
}
