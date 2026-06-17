import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.sample.ImageSampleApp
import io.github.ddsimoes.sweet.sample.imageSampleBitmapSize
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Canvas as SWTCanvas

class ImageSampleTest {
    @Test
    fun imageSample_rendersBitmapIntoCanvas() {
        autoSWT {
            testShell(width = 400, height = 300) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    ImageSampleApp()
                }
            }.test { shell ->
                waitUntil {
                    shell.findAll<SWTCanvas>().isNotEmpty().also {
                        if (!it) {
                            SweetDebugger.log("ImageSampleTest", "Canvas not found. Waiting...")
                        }
                    }
                }
                val canvases = shell.findAll<SWTCanvas>()

                assertTrue(canvases.isNotEmpty(), "Expected at least one SWT Canvas for image drawing")

                val bitmapSize = imageSampleBitmapSize
                assertNotNull(bitmapSize, "Image sample should create an ImageBitmap")
                assertTrue(
                    bitmapSize.first > 0 && bitmapSize.second > 0,
                    "ImageBitmap should have positive dimensions, was $bitmapSize",
                )

                shell.saveScreenshot("image_sample_overview")
            }
        }
    }
}
