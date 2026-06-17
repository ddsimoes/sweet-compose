import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.image.SweetImageBitmapImpl
import io.github.ddsimoes.sweet.image.SwtImageBitmap
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.eclipse.swt.graphics.Image as SWTImage

class SweetImageBitmapDisposalTest {
    @Test
    fun sweetImageBitmap_disposes_underlying_swt_image() {
        var impl: SweetImageBitmapImpl? = null
        var swtImage: SWTImage? = null

        autoSWT {
            testShell(width = 100, height = 100) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                runOnSWT {
                    val display = this.display
                    val img = SWTImage(display, 8, 8)
                    swtImage = img
                    impl = SweetImageBitmapImpl(SwtImageBitmap(img, display))
                }
            }.test { _ ->
                val bitmap = impl
                val image = swtImage

                assertNotNull(bitmap, "SweetImageBitmapImpl should be created")
                assertNotNull(image, "Underlying SWT Image should be created")

                runOnSWT {
                    // Sanity check width/height come from backend image.
                    assertEquals(8, bitmap!!.width, "Bitmap width should match SWT image width")
                    assertEquals(8, bitmap!!.height, "Bitmap height should match SWT image height")

                    assertFalse(image!!.isDisposed, "SWT Image should start undisposed")

                    // Disposing the backend should dispose the SWT Image.
                    bitmap!!.backend.dispose()
                    assertTrue(image!!.isDisposed, "Disposing backend should dispose underlying SWT Image")
                }
            }
        }
    }
}
