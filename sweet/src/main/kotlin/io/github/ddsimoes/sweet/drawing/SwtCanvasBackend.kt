package io.github.ddsimoes.sweet.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.graphics.SWTPath
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.ddsimoes.sweet.debug.SweetDebugger
import io.github.ddsimoes.sweet.image.ImageDataImageBitmap
import io.github.ddsimoes.sweet.image.SweetImageBitmap
import io.github.ddsimoes.sweet.image.SwtImageBitmap
import io.github.ddsimoes.sweet.internal.getOrCreateColor
import io.github.ddsimoes.sweet.internal.getOrCreateFont
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.Pattern
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.graphics.Transform
import org.eclipse.swt.widgets.Display

/**
 * SWT GC-backed implementation of [SweetCanvas].
 *
 * Supports solid colors and linear gradients via [Paint].  Radial and sweep
 * gradients degrade to the first colour stop.  Blend modes other than SrcOver
 * are software-composited via [blendOrDirect] when an [offscreenImage] is
 * available (Canvas-backed scopes); without one they degrade to SrcOver.
 *
 * Colours and fonts are resolved through per-display caches so repeated paints
 * reuse native handles.  Paths, transforms, and gradient patterns are
 * short-lived per-operation and disposed immediately.
 */
internal class SwtCanvasBackend(
    gc: GC,
    private val display: Display,
) : SweetCanvas {
    /**
     * The GC all draw operations target. Normally the constructor GC; temporarily swapped
     * to a translated temp-image GC by [blendOrDirect] while rendering a blend source.
     */
    private var gc: GC = gc
    // ---- state stack (save / restore) ----

    /**
     * When set (via [CanvasIntegration] for Canvas-backed draw scopes),
     * enables blend-mode compositing by reading destination pixels from this
     * off-screen buffer.
     */
    var offscreenImage: Image? = null

    // Track which degradation warnings have been emitted for this instance.
    private val loggedDegradations = java.util.BitSet()

    private fun logOnce(
        bit: Int,
        component: String,
        message: String,
    ) {
        if (loggedDegradations.get(bit)) return
        loggedDegradations.set(bit)
        SweetDebugger.log(component, message)
    }

    private data class GCState(
        val foreground: org.eclipse.swt.graphics.Color?,
        val background: org.eclipse.swt.graphics.Color?,
        val foregroundPattern: org.eclipse.swt.graphics.Pattern?,
        val backgroundPattern: org.eclipse.swt.graphics.Pattern?,
        val lineWidth: Int,
        val lineCap: Int,
        val lineJoin: Int,
        val lineDash: IntArray?,
        val alpha: Int,
        val transform: FloatArray?,
        val clipping: Rectangle?,
    )

    private val stateStack = ArrayDeque<GCState>()

    override fun save() {
        val transform = Transform(display)
        gc.getTransform(transform)
        val elements = FloatArray(6)
        transform.getElements(elements)
        transform.dispose()
        val state =
            GCState(
                foreground = gc.foreground,
                background = gc.background,
                foregroundPattern = gc.foregroundPattern,
                backgroundPattern = gc.backgroundPattern,
                lineWidth = gc.lineWidth,
                lineCap = gc.lineCap,
                lineJoin = gc.lineJoin,
                lineDash = gc.lineDash?.copyOf(),
                alpha = gc.alpha,
                transform = elements,
                clipping = gc.clipping?.let { Rectangle(it.x, it.y, it.width, it.height) },
            )
        stateStack.addLast(state)
    }

    override fun restore() {
        val state = stateStack.removeLastOrNull() ?: return
        state.foreground?.let { gc.foreground = it }
        state.background?.let { gc.background = it }
        state.foregroundPattern?.let { if (!it.isDisposed) gc.foregroundPattern = it }
        state.backgroundPattern?.let { if (!it.isDisposed) gc.backgroundPattern = it }
        gc.lineWidth = state.lineWidth
        gc.lineCap = state.lineCap
        gc.lineJoin = state.lineJoin
        if (state.lineDash != null) gc.lineDash = state.lineDash else gc.lineDash = intArrayOf()
        gc.alpha = state.alpha
        state.transform?.let { elements ->
            val t = Transform(display)
            t.setElements(
                elements[0],
                elements[1],
                elements[2],
                elements[3],
                elements[4],
                elements[5],
            )
            gc.setTransform(t)
            t.dispose()
        }
        state.clipping?.let { gc.setClipping(it) }
    }

    // ---- colour helpers ----

    private fun Color.toSwtColor(): org.eclipse.swt.graphics.Color =
        display.getOrCreateColor(toSwtRgb())

    private fun swtColor(color: Color): org.eclipse.swt.graphics.Color =
        display.getOrCreateColor(color.toSwtRgb())

    // ---- paint helpers ----

    /**
     * Apply [Paint.alpha] to [gc.alpha] and emit a degradation warning when
     * [Paint.blendMode] is not [BlendMode.SrcOver].
     */
    private fun configurePaint(paint: Paint) {
        gc.alpha = (paint.alpha * 255.0f).toInt().coerceIn(0, 255)
    }

    /** Extract stroke width from [Paint.style], defaulting to 1. */
    private fun strokeWidth(paint: Paint): Int =
        ((paint.style as? Stroke)?.width ?: 1.0f).toInt().coerceAtLeast(1)

    /**
     * Resolve the base [Color] for a [Paint], degrading gradients to solid
     * when they cannot be rendered natively.
     */
    private fun resolveBaseColor(paint: Paint): Color {
        return when (val brush = paint.brush) {
            null -> paint.color
            is SolidColor -> brush.value
            is RadialGradient -> {
                logOnce(1, "SwtCanvasBackend", "RadialGradient not natively supported; degrading to solid colour")
                brush.colorStops.firstOrNull()?.second ?: paint.color
            }
            is SweepGradient -> {
                logOnce(2, "SwtCanvasBackend", "SweepGradient not natively supported; degrading to solid colour")
                brush.colorStops.firstOrNull()?.second ?: paint.color
            }
            is LinearGradient -> brush.colorStops.firstOrNull()?.second ?: paint.color
        }
    }

    private fun applyColorFilter(
        color: Color,
        filter: ColorFilter?,
    ): Color {
        if (filter == null) return color
        return when (filter) {
            is ColorFilter.Tint -> {
                val dstAlpha = color.alpha.toFloat() / 255f
                val src = filter.color
                when (filter.blendMode) {
                    BlendMode.SrcIn, BlendMode.SrcAtop -> {
                        // SrcIn: result = source * destination-alpha.
                        // Keeps the tint color modulated by destination alpha.
                        Color(
                            red = src.red.toFloat() / 255f * dstAlpha,
                            green = src.green.toFloat() / 255f * dstAlpha,
                            blue = src.blue.toFloat() / 255f * dstAlpha,
                            alpha = src.alpha.toFloat() / 255f * dstAlpha,
                        )
                    }
                    else -> {
                        // Fallback multiplicative blend for other modes.
                        val fr = dstAlpha * (src.red.toFloat() / 255f)
                        val fg = dstAlpha * (src.green.toFloat() / 255f)
                        val fb = dstAlpha * (src.blue.toFloat() / 255f)
                        val fa = dstAlpha * (src.alpha.toFloat() / 255f)
                        Color(red = fr, green = fg, blue = fb, alpha = fa)
                    }
                }
            }
            else -> color // ColorMatrix, Lighting — best-effort ignore
        }
    }

    /** Resolve the effective SWT colour for [paint] (solid / gradient-first-stop). */
    private fun effectiveSwtColor(paint: Paint): org.eclipse.swt.graphics.Color {
        val base = resolveBaseColor(paint)
        val filtered = applyColorFilter(base, paint.colorFilter)
        return swtColor(filtered)
    }

    private fun createLinearGradientPattern(gradient: LinearGradient): Pattern {
        val stops = gradient.colorStops
        if (stops.size > 2) {
            logOnce(3, "SwtCanvasBackend", "LinearGradient with ${stops.size} stops; SWT Pattern is 2-colour, intermediate stops dropped")
        }
        val first = stops.first().second
        val last = stops.last().second

        val x1 = if (gradient.start.x.isFinite()) gradient.start.x else 0.0f
        val y1 = if (gradient.start.y.isFinite()) gradient.start.y else 0.0f
        val x2 = if (gradient.end.x.isFinite()) gradient.end.x else 1.0f
        val y2 = if (gradient.end.y.isFinite()) gradient.end.y else 1.0f

        return Pattern(
            display,
            x1, y1, x2, y2,
            swtColor(first),
            (first.alpha * 255.0f).toInt().coerceIn(0, 255),
            swtColor(last),
            (last.alpha * 255.0f).toInt().coerceIn(0, 255),
        )
    }

    // ---- drawing helpers ----

    /**
     * Stroke a rectangle outline. [gc.drawRectangle] does not apply the line
     * join, so a non-miter join (Round/Bevel) would still produce square
     * corners. Stroke via a closed [org.eclipse.swt.graphics.Path] in that case
     * so the join is honoured, matching Compose.
     */
    private fun strokeRectangle(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        style: DrawStyle,
    ) {
        val join = (style as? Stroke)?.join
        if (join == StrokeJoin.Round || join == StrokeJoin.Bevel) {
            val path = org.eclipse.swt.graphics.Path(display)
            try {
                path.addRectangle(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
                gc.drawPath(path)
            } finally {
                path.dispose()
            }
        } else {
            gc.drawRectangle(x, y, w, h)
        }
    }

    private fun drawSolid(
        paint: Paint,
        swtColor: org.eclipse.swt.graphics.Color,
        fillBlock: () -> Unit,
        strokeBlock: () -> Unit,
    ) {
        when (paint.style) {
            is Fill -> {
                gc.background = swtColor
                fillBlock()
            }
            is Stroke -> {
                gc.foreground = swtColor
                gc.lineWidth = strokeWidth(paint)
                configureStrokeStyle(paint.style)
                strokeBlock()
            }
        }
    }

    /** Map Compose Stroke cap/join/dash to SWT GC line state. */
    private fun configureStrokeStyle(style: DrawStyle) {
        if (style !is Stroke) return
        gc.lineCap =
            when (style.cap) {
                StrokeCap.Butt -> SWT.CAP_FLAT
                StrokeCap.Round -> SWT.CAP_ROUND
                StrokeCap.Square -> SWT.CAP_SQUARE
            }
        gc.lineJoin =
            when (style.join) {
                StrokeJoin.Miter -> SWT.JOIN_MITER
                StrokeJoin.Round -> SWT.JOIN_ROUND
                StrokeJoin.Bevel -> SWT.JOIN_BEVEL
            }
        // Always set the dash state: when there is no dash effect we must
        // clear any dash left over from a previous draw, because the offscreen
        // GC is cached and reused across paints (see CanvasIntegration). Without
        // this reset, a dashed stroke leaks into later solid strokes/frames.
        val pe = style.pathEffect
        if (pe is PathEffect.DashPathEffect) {
            gc.lineDash = pe.intervals.map { it.toInt().coerceAtLeast(1) }.toIntArray()
        } else {
            gc.lineDash = null
        }
    }

    /**
     * Configure fill or stroke for a linear-gradient paint, then execute the
     * appropriate drawing block.  The [Pattern] is always disposed afterward.
     */
    private inline fun drawWithLinearGradient(
        paint: Paint,
        gradient: LinearGradient,
        crossinline fillBlock: () -> Unit,
        crossinline strokeBlock: () -> Unit,
    ) {
        val pattern = createLinearGradientPattern(gradient)
        val oldBgPattern = gc.backgroundPattern
        val oldFgPattern = gc.foregroundPattern
        val oldLineCap = gc.lineCap
        val oldLineJoin = gc.lineJoin
        val oldLineDash = gc.lineDash
        try {
            when (paint.style) {
                is Fill -> {
                    gc.backgroundPattern = pattern
                    fillBlock()
                }
                is Stroke -> {
                    gc.foregroundPattern = pattern
                    gc.lineWidth = strokeWidth(paint)
                    configureStrokeStyle(paint.style)
                    strokeBlock()
                }
            }
        } finally {
            gc.backgroundPattern = oldBgPattern
            gc.foregroundPattern = oldFgPattern
            gc.lineCap = oldLineCap
            gc.lineJoin = oldLineJoin
            gc.lineDash = oldLineDash
            pattern.dispose()
        }
    }

    // ---- blend-mode compositing ----

    /**
     * Composite [sourceBlock] over the off-screen destination using
     * [Paint.blendMode].  Reads destination pixels from [offscreenImage],
     * renders source to a temporary image, composites per-pixel, and
     * writes the result to [gc] at (x, y).
     *
     * Falls back to direct drawing (SrcOver) when [offscreenImage] is
     * `null` or the blend mode is [BlendMode.SrcOver].
     */
    private fun compositeBlend(
        paint: Paint,
        x: Int, y: Int, w: Int, h: Int,
        sourceBlock: (GC) -> Unit,
    ) {
        if (w <= 0 || h <= 0) return

        val dstImage = offscreenImage
        if (dstImage == null) {
            sourceBlock(gc)
            return
        }

        // 1. Read destination area from off-screen image.
        val fullData = dstImage.imageData
        val dstData = org.eclipse.swt.graphics.ImageData(w, h, fullData.depth, fullData.palette)
        for (py in 0 until h) {
            for (px in 0 until w) {
                val sx = x + px; val sy = y + py
                if (sx in 0 until fullData.width && sy in 0 until fullData.height) {
                    dstData.setPixel(px, py, fullData.getPixel(sx, sy))
                    dstData.setAlpha(px, py, fullData.getAlpha(sx, sy))
                }
            }
        }

        // 2. Draw source to temporary image.
        val srcImage = org.eclipse.swt.graphics.Image(display, w, h)
        val srcGc = org.eclipse.swt.graphics.GC(srcImage)
        srcGc.advanced = true
        try {
            srcGc.alpha = 0
            srcGc.fillRectangle(0, 0, w, h)
            srcGc.alpha = 255
            sourceBlock(srcGc)
        } finally {
            srcGc.dispose()
        }
        val srcData = srcImage.imageData

        // 3. Composite.
        val resultData = compositePixels(srcData, dstData, w, h, paint.blendMode, paint.alpha)

        // 4. Write result back.
        val resultImage = org.eclipse.swt.graphics.Image(display, resultData)
        gc.drawImage(resultImage, x, y)
        resultImage.dispose()
        srcImage.dispose()
    }

    /**
     * Shape-agnostic blend dispatch: runs [body] directly against the target [gc] for
     * SrcOver (or when no [offscreenImage] is available), otherwise renders [body] into
     * a temporary source image translated to the shape's bounding box and composites it
     * with [Paint.blendMode] via [compositeBlend].
     *
     * [body] draws in the canvas's normal absolute coordinates — while it runs, the [gc]
     * field is swapped to the temp GC with a (-x, -y) translation, so all existing
     * shape-drawing code (gradients, stroke helpers) works unchanged. The composited
     * region is padded by half the stroke width so centred strokes are not clipped.
     */
    private inline fun blendOrDirect(
        paint: Paint,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        crossinline body: () -> Unit,
    ) {
        if (paint.blendMode == BlendMode.SrcOver || offscreenImage == null || w <= 0 || h <= 0) {
            body()
            return
        }
        val pad = if (paint.style is Stroke) (strokeWidth(paint) + 1) / 2 + 1 else 0
        val bx = x - pad
        val by = y - pad
        compositeBlend(paint, bx, by, w + 2 * pad, h + 2 * pad) { tempGc ->
            val saved = gc
            val translation = Transform(display)
            try {
                translation.translate((-bx).toFloat(), (-by).toFloat())
                tempGc.setTransform(translation)
                gc = tempGc
                body()
            } finally {
                gc = saved
                tempGc.setTransform(null)
                translation.dispose()
            }
        }
    }

    /**
     * Per-pixel Porter-Duff + separable blend mode compositing.
     *
     * For each pixel, normalises 0–255 SWT colour+alpha to 0…1,
     * applies the Porter-Duff formula derived from [mode], then
     * (for separable modes like [BlendMode.Multiply]) applies the
     * blend function in premultiplied space.
     */
    private fun compositePixels(
        src: org.eclipse.swt.graphics.ImageData,
        dst: org.eclipse.swt.graphics.ImageData,
        w: Int,
        h: Int,
        mode: BlendMode,
        globalAlpha: Float,
    ): org.eclipse.swt.graphics.ImageData {
        val result = org.eclipse.swt.graphics.ImageData(w, h, dst.depth, dst.palette)

        fun getR(p: Int) = ((p shr 16) and 0xFF)
        fun getG(p: Int) = ((p shr 8) and 0xFF)
        fun getB(p: Int) = (p and 0xFF)
        fun makePixel(r: Int, g: Int, b: Int) = (r shl 16) or (g shl 8) or b

        // Per-pixel blend output, reused across the whole loop (no per-pixel allocations).
        var rcR = 0f
        var rcG = 0f
        var rcB = 0f
        var rA = 0f

        // Overlay blend function on STRAIGHT (unpremultiplied) colours, per the W3C
        // compositing spec; the premultiplied shortcut used by Multiply/Screen is not
        // valid for Overlay because of its conditional.
        fun overlay(sc: Float, dc: Float) = if (dc <= 0.5f) 2f * sc * dc else 1f - 2f * (1f - sc) * (1f - dc)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val sp = src.getPixel(x, y); val sa = (src.getAlpha(x, y) * globalAlpha).toInt().coerceIn(0, 255)
                val dp = dst.getPixel(x, y); val da = dst.getAlpha(x, y)

                val sA = sa / 255f; val dA = da / 255f
                val sR = getR(sp) / 255f; val sG = getG(sp) / 255f; val sB = getB(sp) / 255f
                val dR = getR(dp) / 255f; val dG = getG(dp) / 255f; val dB = getB(dp) / 255f

                // Premultiplied source and destination
                val scR = sR * sA; val scG = sG * sA; val scB = sB * sA
                val dcR = dR * dA; val dcG = dG * dA; val dcB = dB * dA

                when (mode) {
                    BlendMode.DstOver -> {
                        rA = sA * (1f - dA) + dA
                        rcR = scR * (1f - dA) + dcR; rcG = scG * (1f - dA) + dcG; rcB = scB * (1f - dA) + dcB
                    }
                    BlendMode.SrcIn -> {
                        rA = sA * dA
                        rcR = scR * dA; rcG = scG * dA; rcB = scB * dA
                    }
                    BlendMode.SrcOut -> {
                        rA = sA * (1f - dA)
                        rcR = scR * (1f - dA); rcG = scG * (1f - dA); rcB = scB * (1f - dA)
                    }
                    BlendMode.DstIn -> {
                        rA = dA * sA
                        rcR = dcR * sA; rcG = dcG * sA; rcB = dcB * sA
                    }
                    BlendMode.DstOut -> {
                        rA = dA * (1f - sA)
                        rcR = dcR * (1f - sA); rcG = dcG * (1f - sA); rcB = dcB * (1f - sA)
                    }
                    BlendMode.Plus -> {
                        rA = (sA + dA).coerceAtMost(1f)
                        rcR = (scR + dcR).coerceAtMost(1f)
                        rcG = (scG + dcG).coerceAtMost(1f)
                        rcB = (scB + dcB).coerceAtMost(1f)
                    }
                    BlendMode.Multiply -> {
                        // Multiply in premultiplied space:
                        // result = sc*(1-dA) + dc*(1-sA) + sc*dc
                        rA = sA + dA * (1f - sA)
                        rcR = scR * (1f - dA) + dcR * (1f - sA) + scR * dcR
                        rcG = scG * (1f - dA) + dcG * (1f - sA) + scG * dcG
                        rcB = scB * (1f - dA) + dcB * (1f - sA) + scB * dcB
                    }
                    BlendMode.Screen -> {
                        rA = sA + dA * (1f - sA)
                        rcR = scR + dcR - scR * dcR; rcG = scG + dcG - scG * dcG; rcB = scB + dcB - scB * dcB
                    }
                    BlendMode.Overlay -> {
                        // General separable-blend formula (W3C):
                        // result = sc*(1-dA) + dc*(1-sA) + sA*dA*B(Sc, Dc) with B on straight colours.
                        rA = sA + dA * (1f - sA)
                        rcR = scR * (1f - dA) + dcR * (1f - sA) + sA * dA * overlay(sR, dR)
                        rcG = scG * (1f - dA) + dcG * (1f - sA) + sA * dA * overlay(sG, dG)
                        rcB = scB * (1f - dA) + dcB * (1f - sA) + sA * dA * overlay(sB, dB)
                    }
                    // SrcOver and unsupported modes: Porter-Duff source-over.
                    else -> {
                        rA = sA + dA * (1f - sA)
                        rcR = scR + dcR * (1f - sA); rcG = scG + dcG * (1f - sA); rcB = scB + dcB * (1f - sA)
                    }
                }

                if (rA == 0f) {
                    result.setPixel(x, y, 0)
                    result.setAlpha(x, y, 0)
                    continue
                }

                val inv = 255f / rA.coerceAtLeast(0.0001f)
                val rr = (rcR * inv).toInt().coerceIn(0, 255)
                val rg = (rcG * inv).toInt().coerceIn(0, 255)
                val rb = (rcB * inv).toInt().coerceIn(0, 255)
                val ra = (rA * 255f).toInt().coerceIn(0, 255)

                result.setPixel(x, y, makePixel(rr, rg, rb))
                result.setAlpha(x, y, ra)
            }
        }
        return result
    }


    // ============================================================
    //  drawRect
    // ============================================================

    override fun drawRect(
        topLeft: Offset,
        size: Size,
        paint: Paint,
    ) {
        val x = topLeft.x.toInt()
        val y = topLeft.y.toInt()
        val w = size.width.toInt()
        val h = size.height.toInt()

        blendOrDirect(paint, x, y, w, h) {
            configurePaint(paint)
            when (val brush = paint.brush) {
                is LinearGradient ->
                    drawWithLinearGradient(
                        paint,
                        brush,
                        fillBlock = { gc.fillRectangle(x, y, w, h) },
                        strokeBlock = { strokeRectangle(x, y, w, h, paint.style) },
                    )
                else -> {
                    val color = effectiveSwtColor(paint)
                    drawSolid(
                        paint,
                        color,
                        fillBlock = { gc.fillRectangle(x, y, w, h) },
                        strokeBlock = { strokeRectangle(x, y, w, h, paint.style) },
                    )
                }
            }
        }
    }

    // ============================================================
    //  drawRoundRect
    override fun drawRoundRect(
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        paint: Paint,
    ) {
        val x = topLeft.x.toInt()
        val y = topLeft.y.toInt()
        val w = size.width.toInt()
        val h = size.height.toInt()
        val arcW = (cornerRadius.x * 2.0f).toInt()
        val arcH = (cornerRadius.y * 2.0f).toInt()

        blendOrDirect(paint, x, y, w, h) {
            configurePaint(paint)
            when (val brush = paint.brush) {
                is LinearGradient ->
                    drawWithLinearGradient(
                        paint,
                        brush,
                        fillBlock = { gc.fillRoundRectangle(x, y, w, h, arcW, arcH) },
                        strokeBlock = { gc.drawRoundRectangle(x, y, w, h, arcW, arcH) },
                    )
                else -> {
                    val color = effectiveSwtColor(paint)
                    drawSolid(
                        paint,
                        color,
                        fillBlock = { gc.fillRoundRectangle(x, y, w, h, arcW, arcH) },
                        strokeBlock = { gc.drawRoundRectangle(x, y, w, h, arcW, arcH) },
                    )
                }
            }
        }
    }

    // ============================================================
    //  drawCircle
    // ============================================================

    override fun drawCircle(
        center: Offset,
        radius: Float,
        paint: Paint,
    ) {
        val diameter = (radius * 2.0f).toInt()
        val x = (center.x - radius).toInt()
        val y = (center.y - radius).toInt()

        blendOrDirect(paint, x, y, diameter, diameter) {
            configurePaint(paint)
            when (val brush = paint.brush) {
                is LinearGradient ->
                    drawWithLinearGradient(
                        paint,
                        brush,
                        fillBlock = { gc.fillOval(x, y, diameter, diameter) },
                        strokeBlock = { gc.drawOval(x, y, diameter, diameter) },
                    )
                else -> {
                    val color = effectiveSwtColor(paint)
                    drawSolid(
                        paint,
                        color,
                        fillBlock = { gc.fillOval(x, y, diameter, diameter) },
                        strokeBlock = { gc.drawOval(x, y, diameter, diameter) },
                    )
                }
            }
        }
    }

    // ============================================================
    //  drawOval
    // ============================================================

    override fun drawOval(
        topLeft: Offset,
        size: Size,
        paint: Paint,
    ) {
        val x = topLeft.x.toInt()
        val y = topLeft.y.toInt()
        val w = size.width.toInt()
        val h = size.height.toInt()

        blendOrDirect(paint, x, y, w, h) {
            configurePaint(paint)
            when (val brush = paint.brush) {
                is LinearGradient ->
                    drawWithLinearGradient(
                        paint,
                        brush,
                        fillBlock = { gc.fillOval(x, y, w, h) },
                        strokeBlock = { gc.drawOval(x, y, w, h) },
                    )
                else -> {
                    val color = effectiveSwtColor(paint)
                    drawSolid(
                        paint,
                        color,
                        fillBlock = { gc.fillOval(x, y, w, h) },
                        strokeBlock = { gc.drawOval(x, y, w, h) },
                    )
                }
            }
        }
    }

    // ============================================================
    //  drawArc
    // ============================================================

    override fun drawArc(
        topLeft: Offset,
        size: Size,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    ) {
        val x = topLeft.x.toInt()
        val y = topLeft.y.toInt()
        val w = size.width.toInt()
        val h = size.height.toInt()
        // Compose measures arc angles clockwise (y-down screen): angle a -> (cos a, +sin a).
        // SWT measures them counter-clockwise (Java2D y-up convention): angle a -> (cos a, -sin a).
        // Negating both start and sweep maps the whole arc to the Compose position; without
        // this the arc is mirrored top-to-bottom across the horizontal axis.
        val start = (-startAngle).toInt()
        val sweep = (-sweepAngle).toInt()

        blendOrDirect(paint, x, y, w, h) {
            configurePaint(paint)
            when (val brush = paint.brush) {
                is LinearGradient ->
                    drawWithLinearGradient(
                        paint,
                        brush,
                        fillBlock = {
                            if (useCenter) {
                                gc.fillArc(x, y, w, h, start, sweep)
                            } else {
                                // SWT has no "fill arc without centre" operation.
                                // Fall back to drawing the arc outline with the
                                // paint's stroke width.  For Fill style this
                                // defaults to 1 px (hairline); for Stroke style
                                // the caller's stroke width is preserved.
                                gc.lineWidth = strokeWidth(paint)
                                gc.drawArc(x, y, w, h, start, sweep)
                            }
                        },
                        strokeBlock = { gc.drawArc(x, y, w, h, start, sweep) },
                    )
                else -> {
                    val color = effectiveSwtColor(paint)
                    drawSolid(
                        paint,
                        color,
                        fillBlock = {
                            if (useCenter) {
                                gc.fillArc(x, y, w, h, start, sweep)
                            } else {
                                // Same SWT limitation as above — fall back
                                // to an arc outline with the paint's stroke width.
                                gc.lineWidth = strokeWidth(paint)
                                gc.drawArc(x, y, w, h, start, sweep)
                            }
                        },
                        strokeBlock = { gc.drawArc(x, y, w, h, start, sweep) },
                    )
                }
            }
        }
    }

    // ============================================================
    //  drawLine
    // ============================================================

    override fun drawLine(
        start: Offset,
        end: Offset,
        paint: Paint,
    ) {
        val bx = minOf(start.x, end.x).toInt()
        val by = minOf(start.y, end.y).toInt()
        val bw = (maxOf(start.x, end.x) - minOf(start.x, end.x)).toInt() + 1
        val bh = (maxOf(start.y, end.y) - minOf(start.y, end.y)).toInt() + 1

        blendOrDirect(paint, bx, by, bw, bh) {
            configurePaint(paint)
            // Lines are always stroked; Fill falls back to a 1-pixel stroke.
            val width = if (paint.style is Stroke) strokeWidth(paint) else 1
            val color = effectiveSwtColor(paint)
            gc.foreground = color
            gc.lineWidth = width
            configureStrokeStyle(paint.style)
            gc.drawLine(
                start.x.toInt(),
                start.y.toInt(),
                end.x.toInt(),
                end.y.toInt(),
            )
        }
    }

    // ============================================================
    //  drawPoints
    // ============================================================
    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        paint: Paint,
    ) {
        if (points.isEmpty()) return
        val pointSize = ((paint.style as? Stroke)?.width ?: 1.0f)
        // Bounding box over all points, padded by the point size (dots are centred on
        // their coordinates), for blend-mode compositing.
        val pad = (pointSize / 2.0f).toInt() + 1
        val bx = points.minOf { it.x }.toInt() - pad
        val by = points.minOf { it.y }.toInt() - pad
        val bw = points.maxOf { it.x }.toInt() - bx + pad + 1
        val bh = points.maxOf { it.y }.toInt() - by + pad + 1

        blendOrDirect(paint, bx, by, bw, bh) {
            drawPointsBody(points, pointMode, paint, pointSize)
        }
    }

    private fun drawPointsBody(
        points: List<Offset>,
        pointMode: PointMode,
        paint: Paint,
        pointSize: Float,
    ) {
        configurePaint(paint)
        val color = effectiveSwtColor(paint)

        when (pointMode) {
            PointMode.Points -> {
                when (paint.style) {
                    is Fill -> {
                        gc.background = color
                        val half = (pointSize / 2.0f).toInt().coerceAtLeast(1)
                        val sz = pointSize.toInt().coerceAtLeast(1)
                        for (pt in points) {
                            gc.fillRectangle(
                                (pt.x - half).toInt(),
                                (pt.y - half).toInt(),
                                sz,
                                sz,
                            )
                        }
                    }
                    is Stroke -> {
                        // Compose renders each point as its stroke cap shape with
                        // diameter == strokeWidth. SWT's gc.drawPoint() always
                        // paints a single pixel and ignores line width, so fill a
                        // sized dot instead (round cap -> circle, else square).
                        gc.background = color
                        val sz = pointSize.toInt().coerceAtLeast(1)
                        val half = sz / 2
                        val round = (paint.style as Stroke).cap == StrokeCap.Round
                        for (pt in points) {
                            val px = pt.x.toInt() - half
                            val py = pt.y.toInt() - half
                            if (round) {
                                gc.fillOval(px, py, sz, sz)
                            } else {
                                gc.fillRectangle(px, py, sz, sz)
                            }
                        }
                    }
                }
            }
            PointMode.Lines -> {
                gc.foreground = color
                gc.lineWidth = strokeWidth(paint)
                configureStrokeStyle(paint.style)
                val intPoints = points.map { pt -> pt.x.toInt() to pt.y.toInt() }
                val coords =
                    intArrayOf(
                        *intPoints.flatMap { listOf(it.first, it.second) }.toIntArray(),
                    )
                if (coords.size >= 4) {
                    gc.drawPolyline(coords)
                }
            }
            PointMode.Polygon -> {
                gc.foreground = color
                gc.lineWidth = strokeWidth(paint)
                configureStrokeStyle(paint.style)
                if (paint.style is Fill) {
                    gc.background = color
                }
                val intPoints = points.map { pt -> pt.x.toInt() to pt.y.toInt() }
                val coords =
                    intArrayOf(
                        *intPoints.flatMap { listOf(it.first, it.second) }.toIntArray(),
                    )
                if (coords.size >= 6) {
                    if (paint.style is Fill) {
                        gc.fillPolygon(coords)
                    }
                    gc.drawPolygon(coords)
                }
            }
        }
    }
    //  drawPath
    // ============================================================

    override fun drawPath(
        path: Path,
        paint: Paint,
    ) {
        val bounds = path.getBounds()
        blendOrDirect(
            paint,
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.width.toInt() + 1,
            bounds.height.toInt() + 1,
        ) {
            configurePaint(paint)
            val swtPath = convertToSwtPath(path) ?: return@blendOrDirect
            try {
                when (val brush = paint.brush) {
                    is LinearGradient -> {
                        val pattern = createLinearGradientPattern(brush)
                        try {
                            drawPathWithPaint(paint, path, swtPath, pattern = pattern)
                        } finally {
                            pattern.dispose()
                        }
                    }
                    else -> {
                        val color = effectiveSwtColor(paint)
                        drawPathWithPaint(paint, path, swtPath, solidColor = color)
                    }
                }
            } finally {
                swtPath.dispose()
            }
        }
    }

    private fun drawPathWithPaint(
        paint: Paint,
        path: Path,
        swtPath: org.eclipse.swt.graphics.Path,
        solidColor: org.eclipse.swt.graphics.Color? = null,
        pattern: Pattern? = null,
    ) {
        when (paint.style) {
            is Fill -> {
                if (solidColor != null) gc.background = solidColor
                if (pattern != null) gc.backgroundPattern = pattern
                val swtpPath = path as? SWTPath
                if (swtpPath != null && swtpPath.fillType == PathFillType.EvenOdd) {
                    val oldFillRule = gc.fillRule
                    gc.fillRule = SWT.FILL_EVEN_ODD
                    gc.fillPath(swtPath)
                    gc.fillRule = oldFillRule
                } else {
                    gc.fillPath(swtPath)
                }
            }
            is Stroke -> {
                if (solidColor != null) gc.foreground = solidColor
                if (pattern != null) gc.foregroundPattern = pattern
                gc.lineWidth = strokeWidth(paint)
                configureStrokeStyle(paint.style)
                gc.drawPath(swtPath)
            }
        }
    }

    // ============================================================
    //  drawImage  (simple overload)
    // ============================================================

    /**
     * Resolve the native SWT image for a bitmap, materializing
     * [ImageDataImageBitmap]s on this backend's display.
     */
    private fun resolveSwtImage(image: SweetImageBitmap): org.eclipse.swt.graphics.Image? =
        when (image) {
            is SwtImageBitmap -> image.swtImage
            is ImageDataImageBitmap -> image.swtImageFor(display)
            else -> null
        }

    override fun drawImage(
        image: SweetImageBitmap,
        topLeft: Offset,
        alpha: Float,
        colorFilter: ColorFilter?,
    ) {
        val swtImage = resolveSwtImage(image) ?: return
        val oldAlpha = gc.alpha
        try {
            gc.alpha = (alpha * 255.0f).toInt().coerceIn(0, 255)
            if (colorFilter != null) {
                logOnce(3, "SwtCanvasBackend", "ColorFilter on drawImage not natively supported; ignored")
            }
            gc.drawImage(
                swtImage,
                topLeft.x.toInt(),
                topLeft.y.toInt(),
            )
        } finally {
            gc.alpha = oldAlpha
        }
    }

    // ============================================================
    //  drawImage  (src/dst overload)
    // ============================================================

    override fun drawImage(
        image: SweetImageBitmap,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        alpha: Float,
        colorFilter: ColorFilter?,
    ) {
        val swtImage = resolveSwtImage(image) ?: return
        val oldAlpha = gc.alpha
        try {
            gc.alpha = (alpha * 255.0f).toInt().coerceIn(0, 255)
            if (colorFilter != null) {
                logOnce(3, "SwtCanvasBackend", "ColorFilter on drawImage not natively supported; ignored")
            }
            gc.drawImage(
                swtImage,
                srcOffset.x.toInt(),
                srcOffset.y.toInt(),
                srcSize.width.toInt(),
                srcSize.height.toInt(),
                dstOffset.x.toInt(),
                dstOffset.y.toInt(),
                dstSize.width.toInt(),
                dstSize.height.toInt(),
            )
        } finally {
            gc.alpha = oldAlpha
        }
    }

    // ---- transforms ----

    override fun translate(
        dx: Float,
        dy: Float,
    ) {
        if (dx == 0.0f && dy == 0.0f) return
        val t = Transform(display)
        try {
            gc.getTransform(t)
            t.translate(dx, dy)
            gc.setTransform(t)
        } finally {
            t.dispose()
        }
    }

    override fun scale(
        sx: Float,
        sy: Float,
    ) {
        if (sx == 1.0f && sy == 1.0f) return
        val t = Transform(display)
        try {
            gc.getTransform(t)
            t.scale(sx, sy)
            gc.setTransform(t)
        } finally {
            t.dispose()
        }
    }

    override fun rotate(degrees: Float) {
        if (degrees == 0.0f) return
        val t = Transform(display)
        try {
            gc.getTransform(t)
            t.rotate(degrees)
            gc.setTransform(t)
        } finally {
            t.dispose()
        }
    }

    // ---- clipping ----

    override fun clipRect(rect: Rect) {
        gc.setClipping(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.width.toInt(),
            rect.height.toInt(),
        )
    }

    override fun clipPath(path: Path) {
        val swtPath = convertToSwtPath(path) ?: return
        try {
            gc.setClipping(swtPath)
        } finally {
            swtPath.dispose()
        }
    }

    // ---- text ----

    override fun drawText(
        text: String,
        position: Offset,
        fontSize: Float,
        color: Color,
    ) {
        if (text.isEmpty() || fontSize <= 0.0f) return

        val baseFontData =
            gc.font.fontData.firstOrNull()
                ?: display.systemFont.fontData.firstOrNull()
        val swtColor = color.toSwtColor()

        if (baseFontData == null) {
            // Fallback: draw with current font if we can't determine base data.
            val oldForeground = gc.foreground
            try {
                gc.foreground = swtColor
                gc.drawString(
                    text,
                    position.x.toInt(),
                    position.y.toInt(),
                    true,
                )
            } finally {
                gc.foreground = oldForeground
            }
            return
        }

        val targetHeight = fontSize.toInt().coerceAtLeast(1)
        val derivedFontData =
            baseFontData
                .let {
                    org.eclipse.swt.graphics.FontData(
                        it.name,
                        targetHeight,
                        it.style,
                    )
                }

        val newFont = display.getOrCreateFont(derivedFontData)
        val oldFont = gc.font
        val oldForeground = gc.foreground

        try {
            gc.font = newFont
            gc.foreground = swtColor
            gc.drawString(
                text,
                position.x.toInt(),
                position.y.toInt(),
                true,
            )
        } finally {
            gc.font = oldFont
            gc.foreground = oldForeground
        }
    }

    // ---- capability ----

    override fun supports(feature: String): Boolean =
        when (feature) {
            "radial_gradient" -> false
            "sweep_gradient" -> false
            else -> true
        }

    // ============================================================
    //  Path conversion (unchanged from original)
    // ============================================================

    private fun convertToSwtPath(path: Path): org.eclipse.swt.graphics.Path? {
        val impl = path as? SWTPath ?: return null
        if (impl.isEmpty()) return null

        val swtPath =
            org.eclipse.swt.graphics
                .Path(display)
        var cx = 0f
        var cy = 0f
        impl.ops
            .forEach { op ->
                when (op) {
                    is SWTPath.Op.MoveTo -> {
                        cx = op.x
                        cy = op.y
                        swtPath.moveTo(op.x, op.y)
                    }
                    is SWTPath.Op.LineTo -> {
                        cx = op.x
                        cy = op.y
                        swtPath.lineTo(op.x, op.y)
                    }
                    SWTPath.Op.Close -> swtPath.close()
                    is SWTPath.Op.QuadTo -> {
                        swtPath.quadTo(op.x1, op.y1, op.x2, op.y2)
                        cx = op.x2
                        cy = op.y2
                    }
                    is SWTPath.Op.RQuadTo -> {
                        swtPath.quadTo(cx + op.dx1, cy + op.dy1, cx + op.dx2, cy + op.dy2)
                        cx += op.dx2
                        cy += op.dy2
                    }
                    is SWTPath.Op.CubicTo -> {
                        swtPath.cubicTo(op.x1, op.y1, op.x2, op.y2, op.x3, op.y3)
                        cx = op.x3
                        cy = op.y3
                    }
                    is SWTPath.Op.RCubicTo -> {
                        swtPath.cubicTo(cx + op.dx1, cy + op.dy1, cx + op.dx2, cy + op.dy2, cx + op.dx3, cy + op.dy3)
                        cx += op.dx3
                        cy += op.dy3
                    }
                    // Compose arc angles are clockwise; SWT's are counter-clockwise.
                    // Negate both so path arcs are not mirrored (see drawArc).
                    is SWTPath.Op.ArcTo ->
                        swtPath.addArc(
                            op.rect.left,
                            op.rect.top,
                            op.rect.width,
                            op.rect.height,
                            -op.startAngleDeg,
                            -op.sweepAngleDeg,
                        )

                    is SWTPath.Op.AddRect ->
                        swtPath.addRectangle(
                            op.rect.left,
                            op.rect.top,
                            op.rect.width,
                            op.rect.height,
                        )

                    is SWTPath.Op.AddOval ->
                        swtPath.addArc(
                            op.oval.left,
                            op.oval.top,
                            op.oval.width,
                            op.oval.height,
                            0f,
                            360f,
                        )

                    is SWTPath.Op.AddArc ->
                        swtPath.addArc(
                            op.oval.left,
                            op.oval.top,
                            op.oval.width,
                            op.oval.height,
                            -op.startAngleDeg,
                            -op.sweepAngleDeg,
                        )

                    is SWTPath.Op.AddRoundRect -> addRoundRectToPath(swtPath, op.roundRect)
                    is SWTPath.Op.AddPath -> {
                        val inner = convertToSwtPath(op.path)
                        if (inner != null) {
                            swtPath.addPath(inner)
                            inner.dispose()
                        }
                    }
                }
            }
        return swtPath
    }

    private fun addRoundRectToPath(
        swtPath: org.eclipse.swt.graphics.Path,
        rr: RoundRect,
    ) {
        val l = rr.left
        val t = rr.top
        val r = rr.right
        val b = rr.bottom
        val tl_r = rr.topLeftCornerRadius.coerceAtLeast(0f)
        val tr_r = rr.topRightCornerRadius.coerceAtLeast(0f)
        val br_r = rr.bottomRightCornerRadius.coerceAtLeast(0f)
        val bl_r = rr.bottomLeftCornerRadius.coerceAtLeast(0f)

        swtPath.moveTo(l + tl_r, t)

        // Top edge + top-right corner
        swtPath.lineTo(r - tr_r, t)
        if (tr_r > 0f) {
            swtPath.addArc(r - 2f * tr_r, t, 2f * tr_r, 2f * tr_r, 90f, -90f)
        } else {
            swtPath.lineTo(r, t)
        }

        // Right edge + bottom-right corner
        swtPath.lineTo(r, b - br_r)
        if (br_r > 0f) {
            swtPath.addArc(r - 2f * br_r, b - 2f * br_r, 2f * br_r, 2f * br_r, 0f, -90f)
        } else {
            swtPath.lineTo(r, b)
        }

        // Bottom edge + bottom-left corner
        swtPath.lineTo(l + bl_r, b)
        if (bl_r > 0f) {
            swtPath.addArc(l, b - 2f * bl_r, 2f * bl_r, 2f * bl_r, 270f, -90f)
        } else {
            swtPath.lineTo(l, b)
        }

        // Left edge + top-left corner
        swtPath.lineTo(l, t + tl_r)
        if (tl_r > 0f) {
            swtPath.addArc(l, t, 2f * tl_r, 2f * tl_r, 180f, -90f)
        } else {
            swtPath.lineTo(l, t)
        }

        swtPath.close()
    }
}
