@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:no-wildcard-imports", "MagicNumber")

// NOTE: This file is a HARDLINK shared between sample-sweet/ and sample-jetbrains/.
// Both paths point to the same inode — the identical source is compiled against two
// different Compose runtimes (Sweet in sample-sweet, JetBrains Compose Desktop in
// sample-jetbrains) so we validate API parity from a single source of truth.
// See docs/development/sample-compatibility.md for the module layout.

package io.github.ddsimoes.sweet.sample

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin




fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sweet Compose - Drawing Kitchen Sink",
            state = rememberWindowState(width = 800.dp, height = 1000.dp),
        ) {
            DrawingKitchenSinkApp()
        }
    }
}

@Composable
fun DrawingKitchenSinkApp(animated: Boolean = true) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            TitleSection("Sweet Compose - Drawing Kitchen Sink")
            Spacer(Modifier.height(4.dp))
            Subtitle("Drawing features in one scrollable Compose Multiplatform view.")
            Spacer(Modifier.height(16.dp)); SolidColorsSection()
            Spacer(Modifier.height(16.dp)); GradientBrushesSection()
            Spacer(Modifier.height(16.dp)); StrokeAndDashSection()
            Spacer(Modifier.height(16.dp)); ShapesSection()
            Spacer(Modifier.height(16.dp)); PathBezierSection()
            Spacer(Modifier.height(16.dp)); TransformsSection()
            Spacer(Modifier.height(16.dp)); ClipSection()
            Spacer(Modifier.height(16.dp)); BlendModeSection()
            Spacer(Modifier.height(16.dp)); ColorFilterSection()
            Spacer(Modifier.height(16.dp)); BitmapImageSection()
            Spacer(Modifier.height(16.dp)); VectorIconSection()
            Spacer(Modifier.height(16.dp)); ContainerDecorationSection()
            Spacer(Modifier.height(16.dp)); ReactiveRedrawSection(animated)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable private fun TitleSection(title: String) { Text(title, fontSize = 22.sp, modifier = Modifier.padding(bottom = 2.dp)) }
@Composable private fun Subtitle(subtitle: String) { Text(subtitle, fontSize = 11.sp, color = Color.Gray) }
@Composable private fun SectionLabel(label: String) { Text(label, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp)) }
@Composable private fun Label(text: String) { Text(text, fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp)) }

@Composable
private fun SectionCanvas(modifier: Modifier = Modifier, onDraw: DrawScope.() -> Unit) {
    Canvas(
        modifier = modifier.fillMaxWidth().height(100.dp)
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)),
        onDraw = onDraw,
    )
}

// -- 1. Solid Colors ------------------------------------------------

@Composable
private fun SolidColorsSection() {
    SectionLabel("1. Solid Colors - drawRect, drawCircle, drawLine, drawPoints")
    SectionCanvas {
        drawRect(Color.Red, topLeft = Offset(8f, 8f), size = Size(80f, 40f))
        drawRect(Color.Blue, topLeft = Offset(100f, 8f), size = Size(80f, 40f), style = Stroke(2f))
        drawRect(Color.Green, topLeft = Offset(16f, 56f), size = Size(60f, 36f), alpha = 0.5f)
        drawCircle(Color.Magenta, radius = 16f, center = Offset(240f, 30f))
        drawCircle(Color.Cyan, radius = 16f, center = Offset(280f, 30f), style = Stroke(2f))
        drawLine(Color.Black, Offset(220f, 60f), Offset(300f, 90f), strokeWidth = 3f)
        drawPoints(
            points = listOf(Offset(320f, 16f), Offset(340f, 40f), Offset(330f, 70f), Offset(310f, 60f)),
            pointMode = PointMode.Points,
            color = Color(0xFFE91E63),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
    }
    Label("Red filled rect | Blue stroked rect | Green 50% alpha | Circles | Line | Scatter points")
}

// -- 2. Gradient Brushes -------------------------------------------

@Composable
private fun GradientBrushesSection() {
    SectionLabel("2. Gradient Brushes")
    SectionCanvas(Modifier.height(120.dp)) {
        // SWT Pattern repeats (does not clamp) outside the gradient axis, so each
        // gradient's start/end must span the full extent of the rect it fills —
        // otherwise the off-axis region wraps instead of holding the edge colour.
        drawRect(
            Brush.linearGradient(0f to Color.Red, 1f to Color.Blue,
                start = Offset(8f, 20f), end = Offset(158f, 20f)),
            topLeft = Offset(8f, 8f), size = Size(150f, 30f))
        drawRect(
            Brush.horizontalGradient(0f to Color.Cyan, 1f to Color.Magenta, startX = 8f, endX = 158f),
            topLeft = Offset(8f, 42f), size = Size(150f, 30f))
        drawRect(
            Brush.verticalGradient(0f to Color(0xFF4CAF50), 1f to Color(0xFF1B5E20),
                startY = 8f, endY = 72f),
            topLeft = Offset(200f, 8f), size = Size(60f, 64f))
        drawRect(SolidColor(Color(0xFFFF5722)), topLeft = Offset(280f, 8f), size = Size(50f, 30f))
        val rr = RoundRect(Rect(280f, 42f, 330f, 72f), CornerRadius(12f, 12f))
        drawPath(Path().apply { addRoundRect(rr) }, SolidColor(Color(0xFF3F51B5)))
    }
    Label("Linear gradient | Horizontal | Vertical | SolidColor brush | Round via path")
}

// -- 3. Stroke styles ----------------------------------------------

@Composable
private fun StrokeAndDashSection() {
    SectionLabel("3. Stroke styles - cap, join, dash (PathEffect.dashPathEffect)")
    SectionCanvas(Modifier.height(120.dp)) {
        val yOff = 20f
        drawLine(Color.Black, Offset(8f, yOff), Offset(100f, yOff), strokeWidth = 4f)
        drawLine(Color(0xFF4CAF50), Offset(8f, yOff + 16f), Offset(100f, yOff + 16f), strokeWidth = 4f)
        drawLine(Color(0xFF2196F3), Offset(8f, yOff + 32f), Offset(100f, yOff + 32f), strokeWidth = 4f)
        drawLine(Color(0xFFFF5722), Offset(120f, yOff), Offset(220f, yOff), strokeWidth = 3f)
        drawRect(Color(0xFF3F51B5), topLeft = Offset(120f, yOff + 16f), size = Size(100f, 50f),
            style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)))
        drawRect(Color(0xFF009688), topLeft = Offset(280f, yOff), size = Size(80f, 60f),
            style = Stroke(3f, join = StrokeJoin.Round))
    }
    Label("Caps | Dashed line | Dashed rect | Round-join stroked rect")
}

// -- 4. Shapes ------------------------------------------------------

@Composable
private fun ShapesSection() {
    SectionLabel("4. Shapes - drawRoundRect, drawOval, drawArc")
    SectionCanvas(Modifier.height(120.dp)) {
        drawRoundRect(Color(0xFF673AB7), Offset(8f, 8f), Size(60f, 30f), cornerRadius = CornerRadius(12f, 12f))
        drawOval(Color(0xFFFF9800), Offset(80f, 8f), Size(60f, 30f))
        drawArc(Color(0xFFE91E63), 30f, 120f, true, Offset(8f, 50f), Size(60f, 50f))
        drawArc(Color(0xFF2196F3), 0f, 270f, false, Offset(80f, 50f), Size(60f, 50f), style = Stroke(3f))
        drawRoundRect(Color(0xFF4CAF50), Offset(160f, 8f), Size(80f, 92f), CornerRadius(16f, 16f), style = Stroke(2f))
        drawCircle(Color(0xFFFF5722), 36f, Offset(280f, 54f))
    }
    Label("Round-rect | Oval | Pie arc | Stroked arc | Stroked round-rect | Circle")
}

// -- 5. Path - beziers ---------------------------------------------

@Composable
private fun PathBezierSection() {
    SectionLabel("5. Path - cubic Bezier, quadratic, arcTo, close")
    SectionCanvas(Modifier.height(120.dp)) {
        drawPath(Path().apply { moveTo(8f, 90f); cubicTo(30f, 10f, 100f, 10f, 120f, 90f) },
            Color(0xFF673AB7), style = Stroke(2f))
        drawPath(Path().apply { moveTo(130f, 90f); quadraticTo(175f, 10f, 220f, 90f) },
            Color(0xFF2196F3), style = Stroke(2f))
        drawPath(Path().apply { arcTo(Rect(230f, 30f, 290f, 90f), 0f, 180f, true) },
            Color(0xFF4CAF50), style = Stroke(2f))
        drawPath(Path().apply {
            moveTo(300f, 90f); lineTo(320f, 30f); lineTo(360f, 60f)
            lineTo(380f, 20f); lineTo(400f, 90f); close()
        }, Color(0xFFE91E63), alpha = 0.7f)
    }
    Label("Cubic Bezier | Quadratic Bezier | Arc path | Filled polygon")
}

// -- 6. Transforms --------------------------------------------------

@Composable
private fun TransformsSection() {
    SectionLabel("6. Transforms - translate, scale, rotate, withTransform, inset")
    SectionCanvas(Modifier.height(120.dp)) {
        withTransform({
            translate(left = 20f, top = 20f)
            scale(scaleX = 1.5f, scaleY = 1.5f, pivot = Offset.Zero)
        }) {
            drawRect(Color(0xFF2196F3), size = Size(30f, 20f))
        }
        withTransform({
            translate(left = 120f, top = 50f)
            rotate(degrees = 30f, pivot = Offset.Zero)
        }) {
            drawRect(Color(0xFF4CAF50), Offset(-20f, -15f), Size(40f, 30f))
        }
        withTransform({
            translate(left = 240f, top = 40f)
        }) {
            drawRect(Color(0xFF9E9E9E), size = Size(60f, 40f))
            withTransform({
                translate(left = 10f, top = 10f)
                scale(scaleX = 0.5f, scaleY = 0.5f, pivot = Offset.Zero)
            }) {
                drawRect(Color(0xFFFF5722), size = Size(40f, 20f))
            }
        }
        inset(20f, 60f, 20f, 10f) { drawRect(Color(0xFF009688), Offset(290f, 0f), Size(80f, 40f)) }
    }
    Label("withTransform+translate+scale | rotate | nested | inset scoped block")
}

// -- 7. Clipping ----------------------------------------------------

@Composable
private fun ClipSection() {
    SectionLabel("7. Clipping - clipRect, clipPath")
    SectionCanvas(Modifier.height(120.dp)) {
        clipRect(left = 8f, top = 8f, right = 80f, bottom = 60f) {
            drawRect(Color(0xFFE91E63), size = Size(120f, 80f))
            drawCircle(Color(0xFF2196F3), 20f, Offset(60f, 40f))
        }
        withTransform({
            translate(left = 110f, top = 0f)
        }) {
            val roundedClip = Path().apply {
                addRoundRect(RoundRect(Rect(0f, 8f, 80f, 88f), CornerRadius(16f, 16f)))
            }
            clipPath(roundedClip) {
                drawRect(Brush.verticalGradient(0f to Color(0xFFFF5722), 1f to Color(0xFF3F51B5)), size = Size(80f, 80f))
            }
        }
        withTransform({
            translate(left = 220f, top = 0f)
        }) {
            val ovalClip = Path().apply { addOval(Rect(0f, 8f, 80f, 88f)) }
            clipPath(ovalClip) {
                drawRect(Brush.horizontalGradient(0f to Color(0xFF4CAF50), 1f to Color(0xFF009688)), size = Size(80f, 80f))
            }
        }
    }
    Label("Rect clip | Rounded-rect clip + gradient | Oval clip + gradient")
}

// -- 8. BlendMode ---------------------------------------------------

@Composable
private fun BlendModeSection() {
    SectionLabel("8. BlendMode - SrcOver, SrcIn, SrcOut, DstOver, Plus, Multiply")
    SectionCanvas(Modifier.height(64.dp)) {
        val red = Color(0xFFE91E63); val blue = Color(0xFF2196F3)
        fun pair(x: Float, m: BlendMode) {
            drawRect(blue, Offset(x, 8f), Size(36f, 30f)); drawRect(red, Offset(x + 8f, 16f), Size(36f, 30f), blendMode = m)
        }
        pair(4f, BlendMode.SrcOver); pair(60f, BlendMode.SrcIn); pair(116f, BlendMode.SrcOut)
        pair(172f, BlendMode.DstOver); pair(228f, BlendMode.Plus); pair(284f, BlendMode.Multiply)
    }
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("SrcOver", "SrcIn", "SrcOut", "DstOver", "Plus", "Multiply").forEach {
            Text(it, fontSize = 8.sp, color = Color.DarkGray, modifier = Modifier.width(48.dp))
        }
    }
    Label("Overlapping red/blue rects - non-SrcOver modes degrade gracefully on GC backend")
}

// -- 9. ColorFilter -------------------------------------------------

@Composable
private fun ColorFilterSection() {
    SectionLabel("9. ColorFilter - tint, alpha modulation")
    SectionCanvas {
        drawRect(Color.White, Offset(8f, 8f), Size(70f, 50f), colorFilter = ColorFilter.tint(Color(0xFFE91E63)))
        drawRect(Color.White, Offset(92f, 8f), Size(70f, 50f), colorFilter = ColorFilter.tint(Color(0xFF2196F3)))
        drawRect(Color(0xFF4CAF50), Offset(180f, 8f), Size(70f, 50f), alpha = 0.5f, colorFilter = ColorFilter.tint(Color(0xFFFF5722)))
        drawRect(Color(0xFFFF9800), Offset(268f, 8f), Size(70f, 50f))
    }
    Label("Tint pink | Tint blue | 50% alpha + tint | Baseline")
}

// -- 10. Bitmap Images ----------------------------------------------

private const val CLASSPATH_IMAGE_RESOURCE = "images/sweet-classpath.png"
private const val FILESYSTEM_IMAGE_RESOURCE = "images/sweet-filesystem.png"

// loadImageBitmap/useResource are deprecated in MPP in favor of the Compose
// resources library (`Res`), which Sweet does not support yet; suppressed so
// the hardlinked source compiles warning-free against the JetBrains runtime.
@Suppress("DEPRECATION")
@Composable
private fun BitmapImageSection() {
    val classpathBitmap = remember { useResource(CLASSPATH_IMAGE_RESOURCE, ::loadImageBitmap) }
    val filesystemBitmap = remember {
        materializeFilesystemSamplePng().inputStream().use(::loadImageBitmap)
    }
    SectionLabel("10. Bitmap Images - loadImageBitmap (classpath + filesystem), Image, drawImage")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(classpathBitmap, "Classpath PNG", Modifier.size(72.dp))
            Label("classpath ${classpathBitmap.width}x${classpathBitmap.height}")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(filesystemBitmap, "Filesystem PNG", Modifier.size(72.dp))
            Label("filesystem ${filesystemBitmap.width}x${filesystemBitmap.height}")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(filesystemBitmap, "Crop", Modifier.size(96.dp, 48.dp), contentScale = ContentScale.Crop)
            Label("Crop")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(classpathBitmap, "FillBounds", Modifier.size(96.dp, 48.dp), contentScale = ContentScale.FillBounds)
            Label("FillBounds")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(96.dp)) {
                drawImage(classpathBitmap, topLeft = Offset.Zero)
                drawImage(filesystemBitmap, topLeft = Offset(24f, 24f), alpha = 0.7f)
            }
            Label("drawImage + alpha")
        }
    }
    Label("Image from classpath resource | from filesystem file | ContentScale variants | Canvas drawImage overlay")
}

/**
 * Materialize the bundled sample PNG as a real file in the system temp directory so
 * the section demonstrates loading an ImageBitmap from the filesystem (as opposed
 * to the classpath).
 */
@Suppress("DEPRECATION")
private fun materializeFilesystemSamplePng(): File {
    val file = File(System.getProperty("java.io.tmpdir"), "sweet-drawing-kitchensink-fs.png")
    if (!file.isFile || file.length() == 0L) {
        useResource(FILESYSTEM_IMAGE_RESOURCE) { input ->
            file.outputStream().use { input.copyTo(it) }
        }
    }
    return file
}

// -- 11. Vector Icons ----------------------------------------------

@Composable
private fun VectorIconSection() {
    val icons = remember { KitchenSinkIcons.create() }
    SectionLabel("11. Vector Icons - ImageVector + real Icon composable")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icons.add, "Add", tint = Color(0xFF4CAF50))
        Icon(icons.arrowBack, "Back", tint = Color(0xFF2196F3))
        Icon(icons.check, "Check", tint = Color(0xFF4CAF50))
        Icon(icons.close, "Close", tint = Color(0xFFE91E63))
        Icon(icons.menu, "Menu", tint = Color.DarkGray)
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icons.add, "16", Modifier.size(16.dp), tint = Color.Unspecified)
        Icon(icons.add, "24", Modifier.size(24.dp), tint = Color(0xFFFF5722))
        Icon(icons.add, "32", Modifier.size(32.dp), tint = Color(0xFF3F51B5))
        Icon(icons.add, "40", Modifier.size(40.dp), tint = Color(0xFF673AB7))
    }
    Label("5 custom ImageVectors (tinted) | Size variants (16-40dp)")
}

@Composable
private fun ContainerDecorationSection() {
    SectionLabel("12. Container decoration - background(shape/brush), border, clip")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).height(60.dp).background(Color(0xFFE91E63), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Text("Rounded", color = Color.White, fontSize = 10.sp)
        }
        Box(Modifier.weight(1f).height(60.dp)
            .background(Brush.horizontalGradient(0f to Color(0xFF3F51B5), 1f to Color(0xFF2196F3)), CircleShape),
            contentAlignment = Alignment.Center) {
            Text("Circle", color = Color.White, fontSize = 10.sp)
        }
        Box(Modifier.weight(1f).height(60.dp)
            .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp)).padding(8.dp),
            contentAlignment = Alignment.Center) {
            Text("Border", fontSize = 10.sp)
        }
        Box(Modifier.weight(1f).height(60.dp).clip(CircleShape).background(Color(0xFFFF9800)),
            contentAlignment = Alignment.Center) {
            Text("Clip", color = Color.White, fontSize = 10.sp)
        }
    }
    Label("RoundedCornerShape bg | CircleShape + gradient | Border | Clip to CircleShape")
}

// -- 13. Reactive redraw -------------------------------------------

@Composable
private fun ReactiveRedrawSection(animated: Boolean = true) {
    var hue by remember { mutableStateOf(0f) }
    var spinning by remember { mutableStateOf(animated) }
    LaunchedEffect(spinning) {
        while (spinning) {
            hue = (hue + 0.5f) % 360f
            delay(16)
        }
    }
    SectionLabel("13. Reactive Redraw - mutableState triggers canvas redraw")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.weight(1f).height(100.dp)) {
            for (i in 0 until 12) {
                val angle = (i.toFloat() / 12f) * 360f + hue
                val rad = angle * PI.toFloat() / 180f
                drawCircle(Color.fromDegrees((hue + i * 30f) % 360f), 10f,
                    Offset(50f + cos(rad) * 30f, 50f + sin(rad) * 30f))
            }
            }
        Box(Modifier.weight(1f).height(100.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val r = if (((hue / 30f).toInt() % 2 == 0)) 24f else 18f
                drawCircle(Color(0xFFE91E63), r, Offset(50f, 50f))
            }
            Text("t=${hue.toInt()} deg", fontSize = 8.sp, color = Color.DarkGray,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp))
        }
        val barData = remember { mutableStateListOf(3, 7, 4, 6, 2, 5) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val bw = 20f; val gap = 6f
                val maxV = barData.maxOrNull()?.toFloat() ?: 1f
                barData.forEachIndexed { i, v ->
                    drawRect(Color.fromDegrees((hue + i * 50f) % 360f),
                        Offset(i * (bw + gap) + 4f, 70f - (v / maxV) * 60f),
                        Size(bw - 2f, (v / maxV) * 60f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { barData.shuffle() }, Modifier.height(28.dp)) { Text("Shuffle", fontSize = 9.sp) }
                Button(onClick = { spinning = !spinning }, Modifier.height(28.dp)) { Text(if (spinning) "Pause" else "Resume", fontSize = 9.sp) }
            }
        }
    }
    Label("HSL color wheel (spinning) | Heartbeat pulse | Reactive bar chart (Shuffle/Pause)")
}

private data class KitchenSinkIcons(
    val add: ImageVector,
    val arrowBack: ImageVector,
    val check: ImageVector,
    val close: ImageVector,
    val menu: ImageVector,
) {
    companion object {
        fun create() = KitchenSinkIcons(
            add = strokeIcon("Add") {
                moveTo(12f, 5f); verticalLineTo(19f)
                moveTo(5f, 12f); horizontalLineTo(19f)
            },
            arrowBack = strokeIcon("ArrowBack") {
                moveTo(19f, 12f); horizontalLineTo(5f)
                moveTo(12f, 5f); lineTo(5f, 12f); lineTo(12f, 19f)
            },
            check = strokeIcon("Check") {
                moveTo(5f, 13f); lineTo(10f, 18f); lineTo(19f, 7f)
            },
            close = strokeIcon("Close") {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            },
            menu = strokeIcon("Menu") {
                moveTo(5f, 7f); horizontalLineTo(19f)
                moveTo(5f, 12f); horizontalLineTo(19f)
                moveTo(5f, 17f); horizontalLineTo(19f)
            },
        )

        private fun strokeIcon(name: String, pathBlock: PathBuilder.() -> Unit): ImageVector {
            return ImageVector.Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Transparent),
                    fillAlpha = 0f,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2.25f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    pathBlock()
                }
            }.build()
        }
    }
}

// -- Helpers --------------------------------------------------------

private fun Color.Companion.fromDegrees(h: Float): Color {
    val s = 0.8f; val l = 0.6f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f); h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x); h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
