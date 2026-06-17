import androidx.compose.ui.integration.embedCompose
import io.github.ddsimoes.autoswt.autoSWT
import io.github.ddsimoes.sweet.sample.DrawingKitchenSinkApp
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for the bitmap-image section of the Drawing Kitchen Sink:
 * verifies that loadImageBitmap works for both classpath resources
 * (useResource) and filesystem files, by checking the size labels the
 * section renders from the decoded bitmaps.
 */
class DrawingKitchenSinkImageTest {
    @Test
    fun bitmapSectionLoadsClasspathAndFilesystemImages() {
        autoSWT {
            testShell(width = 800, height = 700) {
                layout = FillLayout()
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()
                composite.embedCompose {
                    DrawingKitchenSinkApp(animated = false)
                }
            }.test { shell ->
                val classpathLabels = shell.findAll<Label> { it.text == "classpath 96x96" }
                assertEquals(
                    1,
                    classpathLabels.size,
                    "Classpath PNG should decode to 96x96 and render its size label",
                )

                val filesystemLabels = shell.findAll<Label> { it.text == "filesystem 96x96" }
                assertEquals(
                    1,
                    filesystemLabels.size,
                    "Filesystem PNG should decode to 96x96 and render its size label",
                )

                shell.saveScreenshot("drawing_kitchensink_bitmap_images")
            }
        }
    }
}
