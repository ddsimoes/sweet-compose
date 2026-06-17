package androidx.compose.ui.graphics.drawscope

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * DrawScope provides a scoped drawing environment that is a [Density] so
 * `dp.toPx()` and `sp.toPx()` work naturally inside draw lambdas.
 *
 * This is a faithful subset of the real Compose [DrawScope] API:
 * supported operations route to the internal [SweetCanvas] backend;
 * unsupported parameters are documented and degrade gracefully.
 */
interface DrawScope : Density {
    /** Size of the current drawing area in pixels. */
    val size: Size

    /** Layout direction of the current composition. */
    val layoutDirection: LayoutDirection

    /** Center of the drawing area (convenience). */
    val center: Offset get() = Offset(size.width / 2f, size.height / 2f)

    // ── Rectangles ──────────────────────────────────────────────

    @Stable
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawRect(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawRoundRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        cornerRadius: CornerRadius = CornerRadius.Zero,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Circle / Oval ───────────────────────────────────────────

    @Stable
    fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawOval(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Arc ─────────────────────────────────────────────────────

    @Stable
    fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Line ────────────────────────────────────────────────────

    @Stable
    fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float = 1.0f,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float = 1.0f,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Points ──────────────────────────────────────────────────

    @Stable
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float = 1.0f,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: PathEffect? = null,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Image ───────────────────────────────────────────────────

    @Stable
    fun drawImage(
        image: ImageBitmap,
        topLeft: Offset = Offset.Zero,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
    )

    @Stable
    fun drawImage(
        image: ImageBitmap,
        srcOffset: Offset = Offset.Zero,
        srcSize: Size = image.run { Size(width.toFloat(), height.toFloat()) },
        dstOffset: Offset = Offset.Zero,
        dstSize: Size = srcSize,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
    )

    // ── Path ────────────────────────────────────────────────────

    @Stable
    fun drawPath(
        path: Path,
        color: Color,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    @Stable
    fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
    )

    // ── Text ────────────────────────────────────────────────────

    @Stable
    fun drawText(
        text: String,
        topLeft: Offset,
        fontSize: Float,
        color: Color,
        alpha: Float = 1.0f,
    )

    // ── Transforms ──────────────────────────────────────────────

    @Stable
    fun translate(
        left: Float = 0.0f,
        top: Float = 0.0f,
    )

    @Stable
    fun scale(
        scaleX: Float,
        scaleY: Float = scaleX,
        pivot: Offset = center,
    )

    @Stable
    fun rotate(
        degrees: Float,
        pivot: Offset = center,
    )

    @Stable
    fun clipRect(rect: Rect)

    @Stable
    fun clipPath(path: Path)

    @Stable
    fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        clipRect(Rect(left, top, right, bottom))
    }

    // ── Scoped operations ───────────────────────────────────────

    /** Execute [block] with the current transform/clip saved and restored. */
    @Stable
    fun withTransform(block: DrawScope.() -> Unit)

    /** Translate by [offset], execute [block], then restore. */
    fun withTranslation(
        offset: Offset,
        block: DrawScope.() -> Unit,
    ) {
        withTransform {
            translate(offset.x, offset.y)
            block()
        }
    }

    /** Inset the drawing area by [inset], execute [block], then restore. */
    fun inset(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        block: DrawScope.() -> Unit,
    ) {
        withTransform {
            translate(left, top)
            clipRect(Rect(0f, 0f, size.width - left - right, size.height - top - bottom))
            block()
        }
    }

    /** Clip to a rectangle, execute [block], then restore. */
    fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        block: DrawScope.() -> Unit,
    ) {
        withTransform {
            clipRect(left, top, right, bottom)
            block()
        }
    }

    /** Clip to a [path], execute [block], then restore. */
    fun clipPath(
        path: Path,
        block: DrawScope.() -> Unit,
    ) {
        withTransform {
            clipPath(path)
            block()
        }
    }
}

/**
 * Receiver scope for [Modifier.drawWithContent]. Provides all [DrawScope] methods
 * plus [drawContent] which conceptually draws the composable's content (children).
 *
 * In the bounded Sweet implementation, [drawContent] is a no-op — SWT paints
 * children natively between the behind and in-front draw passes.
 */
interface ContentDrawScope : DrawScope {
    /** Draw the composable's content (children). No-op in bounded implementation. */
    fun drawContent()
}

/**
 * Scope for caching drawing operations across recompositions.
 *
 * Currently a stub that captures an [onDrawBehind] callback.
 * Full caching support is deferred (Tier-3).
 */
class CacheDrawScope {
    /** Callback invoked on each draw pass. */
    var onDrawBehind: (DrawScope.() -> Unit)? = null

    /** Register a callback to draw behind the content. */
    fun onDrawBehind(block: DrawScope.() -> Unit) {
        this.onDrawBehind = block
    }
}

sealed class DrawStyle

object Fill : DrawStyle()

@Stable
data class Stroke(
    val width: Float = 1f,
    val miter: Float = 4f,
    val cap: StrokeCap = StrokeCap.Butt,
    val join: StrokeJoin = StrokeJoin.Miter,
    val pathEffect: PathEffect? = null,
) : DrawStyle()

/**
 * Execute [transformBlock] to set up transforms, then [drawBlock] to draw,
 * with the current transform/clip state saved and restored.
 *
 * MPP signature; on Sweet this is implemented by wrapping the single-lambda
 * member [DrawScope.withTransform].
 */
fun DrawScope.withTransform(
    transformBlock: DrawTransform.() -> Unit,
    drawBlock: DrawScope.() -> Unit,
) {
    val transform = DrawTransformDelegate(this)
    // Calls the *member* DrawScope.withTransform(block) — members beat extensions.
    this.withTransform {
        transform.transformBlock()
        this.drawBlock()
    }
}

// ─────────────────────────────────────────────────────────────
// MPP-compatible scoped clip / inset
// ─────────────────────────────────────────────────────────────

/**
 * Clip to a rectangle, execute [block], then restore the clip.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun DrawScope.clipRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit,
) {
    withTransform({
        clipRect(left, top, right, bottom) // non-scoped member via DrawTransform
    }) {
        block()
    }
}

/** Clip to a [path], execute [block], then restore. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun DrawScope.clipPath(
    path: Path,
    block: DrawScope.() -> Unit,
) {
    withTransform({
        clipPath(path) // non-scoped member via DrawTransform
    }) {
        block()
    }
}

/** Inset the drawing area, execute [block], then restore. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun DrawScope.inset(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit,
) {
    val w = size.width
    val h = size.height
    withTransform({
        translate(left, top)
        clipRect(0f, 0f, w - left - right, h - top - bottom)
    }) {
        block()
    }
}

internal class DrawTransformDelegate(
    private val scope: DrawScope,
) : DrawTransform {
    override fun translate(
        left: Float,
        top: Float,
    ) {
        scope.translate(left, top)
    }

    override fun scale(
        scaleX: Float,
        scaleY: Float,
        pivot: Offset,
    ) {
        scope.scale(scaleX, scaleY, pivot)
    }

    override fun rotate(
        degrees: Float,
        pivot: Offset,
    ) {
        scope.rotate(degrees, pivot)
    }

    override fun clipRect(rect: Rect) {
        scope.clipRect(rect)
    }

    override fun clipPath(path: Path) {
        scope.clipPath(path)
    }

    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        scope.clipRect(left, top, right, bottom)
    }
}
