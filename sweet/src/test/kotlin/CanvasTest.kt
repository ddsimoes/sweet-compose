import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.horizontalGradient
import androidx.compose.ui.integration.embedCompose
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.autoswt.autoSWT
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.eclipse.swt.widgets.Canvas as SWTCanvas

class CanvasTest {
    @Test
    fun canvas_draw_callback_is_invoked() {
        var drawCalled = false

        autoSWT {
            testShell(width = 300, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Canvas(modifier = Modifier) {
                        drawCalled = true

                        drawRect(
                            color = Color.Blue,
                            topLeft = Offset(10f, 10f),
                            size = Size(50f, 40f),
                        )

                        drawCircle(
                            color = Color.Red,
                            radius = 20f,
                            center = Offset(100f, 60f),
                        )

                        drawLine(
                            color = Color.Green,
                            start = Offset(0f, 0f),
                            end = Offset(200f, 100f),
                        )
                    }
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Shell should contain at least one SWT Canvas")
                assertTrue(drawCalled, "Canvas draw callback should be invoked during painting")
            }
        }
    }

    @Test
    fun canvas_resizes_and_redraws() {
        var lastSize: Size? = null

        autoSWT {
            testShell(width = 200, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Canvas(modifier = Modifier) {
                        lastSize = size
                    }
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Shell should contain a Canvas for resize test")

                runOnSWT {
                    shell.setSize(400, 300)
                    shell.layout(true, true)
                }

                val reportedSize = lastSize
                assertTrue(reportedSize != null, "DrawScope.size should be reported at least once")
                assertTrue(reportedSize.width > 0f && reportedSize.height > 0f, "Canvas size should be positive after resize")
            }
        }
    }

    @Test
    fun canvas_supports_paths_and_transforms() {
        var drawCalled = false

        autoSWT {
            testShell(width = 200, height = 150) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Canvas(modifier = Modifier) {
                        drawCalled = true

                        val path =
                            Path().apply {
                                moveTo(10f, 10f)
                                lineTo(60f, 10f)
                                lineTo(35f, 60f)
                                close()
                            }

                        withTransform {
                            translate(left = 20f, top = 15f)
                            clipRect(Rect(0f, 0f, size.width, size.height))
                            clipPath(path)
                            drawPath(path, Color.Green)

                            drawText(
                                text = "Hello",
                                topLeft = Offset(5f, 5f),
                                fontSize = 12f,
                                color = Color.White,
                            )
                        }
                    }
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Shell should contain a Canvas for path test")
                assertTrue(drawCalled, "Draw callback with path and transforms should be invoked")
            }
        }
    }

    // ── Regression tests ────────────────────────────────────────────────────

    /**
     * Regression test for Bug #1: Canvas onDraw content must be visible
     * above Modifier.background() (not covered by background).
     */
    @Test
    fun canvas_onDraw_visible_above_background_modifier() {
        autoSWT {
            testShell(width = 200, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Canvas(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)),
                        onDraw = {
                            drawRect(
                                color = Color.Blue,
                                topLeft = Offset(0f, 0f),
                                size = Size(100f, 100f),
                            )
                        },
                    )
                }
            }.test { shell ->
                val canvases = shell.findAll<SWTCanvas> { true }
                assertTrue(canvases.isNotEmpty(), "Should contain a Canvas")

                var pixel = 0
                runOnSWT {
                    pixel = readPixel(canvases[0], 50, 50)
                }
                val blue = pixel and 0xFF
                val green = (pixel shr 8) and 0xFF
                val red = (pixel shr 16) and 0xFF
                assertTrue(
                    blue > 200 && red < 50 && green < 50,
                    "Center pixel should be blue (onDraw on top), got RGB($red,$green,$blue)",
                )
            }
        }
    }

    /**
     * Regression test for Bug #2: Gradient-filled rounded shapes must NOT
     * produce a self-crossing (bowtie) path.
     *
     * Verifies that drawing a CircleShape with horizontal gradient does not
     * throw or crash — the arc-angle fix ensures the path is well-formed.
     */
    @Test
    fun gradient_filled_circle_no_bowtie() {
        var composed = false

        autoSWT {
            testShell(width = 200, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        content = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .background(
                                            brush =
                                                Brush.horizontalGradient(
                                                    0.0f to Color.Red,
                                                    1.0f to Color.Blue,
                                                ),
                                            shape = CircleShape,
                                        ),
                                content = {},
                            )
                        },
                    )
                    composed = true
                }
            }.test { shell ->
                val composites = shell.findAll<Composite> { true }
                assertTrue(composites.isNotEmpty(), "Should contain Composites")
                assertTrue(composed, "CircleShape with horizontal gradient should compose without error")
            }
        }
    }

    /**
     * Regression test for Bug #3: Modifier.clip(CircleShape) should clip
     * to a circle, not just the bounding rectangle.
     *
     * Asserts the visual outcome, not the clipping mechanism: a pixel at
     * the center (inside the circle) must carry the background fill, while
     * a corner pixel (outside the circle but inside the bounding rect) must
     * not. Note: a clipped composite that paints a background deliberately
     * does NOT use an SWT Region — with a Region set, GTK renders parent
     * painting over child widgets — so clipping happens at draw time
     * (see ClipBackgroundTest for the Region-usage rule).
     */
    @Test
    fun clip_circle_shape_clips_to_circle_not_rectangle() {
        autoSWT {
            testShell(width = 200, height = 200) {
                val composite = Composite(this, SWT.NONE)
                composite.layout = FillLayout()

                composite.embedCompose {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        content = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF9800)),
                                content = {},
                            )
                        },
                    )
                }
            }.test { shell ->
                val composites = shell.findAll<Composite> { true }
                assertTrue(composites.isNotEmpty(), "Should contain Composites")

                val circleBox = composites.lastOrNull() ?: return@test

                fun samplePixels(): Triple<Int, Int, Int> =
                    runOnSWT {
                        val area = circleBox.clientArea
                        val cx = area.width / 2
                        val cy = area.height / 2
                        // Corner: inside the bounding rect, well outside the circle.
                        val corner = readPixel(circleBox, area.width / 16, area.height / 16)
                        // Top-center: inside the circle, near its edge.
                        val topCenter = readPixel(circleBox, cx, area.height / 16)
                        val center = readPixel(circleBox, cx, cy)
                        Triple(corner, topCenter, center)
                    }

                // Paint events are asynchronous; poll until the fill appears.
                val deadline = System.currentTimeMillis() + 3000
                var pixels = samplePixels()
                while (System.currentTimeMillis() < deadline && pixels.third == pixels.first) {
                    Thread.sleep(20)
                    pixels = samplePixels()
                }
                val (corner, topCenter, center) = pixels

                assertEquals(
                    center,
                    topCenter,
                    "Pixel inside the circle near the top edge should carry the fill color",
                )
                assertTrue(
                    corner != center,
                    "Corner pixel (outside the circle) must not carry the fill color — " +
                        "clip degraded to the bounding rectangle",
                )
            }
        }
    }

    companion object {
        /**
         * Read a single pixel from a [Composite] at the given coordinates
         * relative to its client area.
         */
        private fun readPixel(
            control: Composite,
            x: Int,
            y: Int,
        ): Int {
            val clientArea = control.clientArea
            val image = Image(control.display, clientArea.width, clientArea.height)
            val gc = GC(control)
            try {
                gc.copyArea(image, 0, 0)
                val pixel = image.imageData.getPixel(x, y)
                gc.dispose()
                image.dispose()
                return pixel
            } finally {
                if (!gc.isDisposed) gc.dispose()
                if (!image.isDisposed) image.dispose()
            }
        }
    }
}
