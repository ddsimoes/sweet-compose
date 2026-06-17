package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect

interface Path {
    fun moveTo(
        x: Float,
        y: Float,
    )

    fun lineTo(
        x: Float,
        y: Float,
    )

    fun close()

    /** Add a quadratic Bézier curve from current point to (x2,y2) with control point (x1,y1). */
    fun quadraticBezierTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    )

    /** MPP-compatible alias for [quadraticBezierTo]. */
    fun quadraticTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        quadraticBezierTo(x1, y1, x2, y2)
    }

    fun rQuadraticBezierTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
    )

    /** Add a cubic Bézier curve from current point to (x3,y3) with control points (x1,y1),(x2,y2). */
    fun cubicTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    )

    fun rCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    )

    /** Add an arc. */
    fun arcTo(
        rect: Rect,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        forceMoveTo: Boolean,
    )

    fun addRect(rect: Rect)

    fun addOval(oval: Rect)

    fun addArc(
        oval: Rect,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
    )

    fun addRoundRect(roundRect: RoundRect)

    fun addPath(path: Path)

    /** Reset path to empty state. */
    fun reset()

    /** Reset but keep the internal buffer. */
    fun rewind()

    /** Path fill type. */
    var fillType: PathFillType

    /** Approximate bounding box (may over-estimate for curves). */
    fun getBounds(): Rect

    /** @return true if path contains no operations. */
    fun isEmpty(): Boolean

    /** Combine this path with [path] using [op]. Best-effort on GC backend. */
    fun op(
        path: Path,
        op: PathOperation,
    ): Boolean
}

internal class SWTPath : Path {
    sealed interface Op {
        data class MoveTo(val x: Float, val y: Float) : Op

        data class LineTo(val x: Float, val y: Float) : Op

        object Close : Op

        data class QuadTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Op

        data class RQuadTo(val dx1: Float, val dy1: Float, val dx2: Float, val dy2: Float) : Op

        data class CubicTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float) : Op

        data class RCubicTo(val dx1: Float, val dy1: Float, val dx2: Float, val dy2: Float, val dx3: Float, val dy3: Float) : Op

        data class ArcTo(val rect: Rect, val startAngleDeg: Float, val sweepAngleDeg: Float, val forceMoveTo: Boolean) : Op

        data class AddRect(val rect: Rect) : Op

        data class AddOval(val oval: Rect) : Op

        data class AddArc(val oval: Rect, val startAngleDeg: Float, val sweepAngleDeg: Float) : Op

        data class AddRoundRect(val roundRect: RoundRect) : Op

        data class AddPath(val path: SWTPath) : Op
    }

    internal val ops: MutableList<Op> = mutableListOf()
    override var fillType: PathFillType = PathFillType.NonZero

    private var minX = Float.POSITIVE_INFINITY
    private var minY = Float.POSITIVE_INFINITY
    private var maxX = Float.NEGATIVE_INFINITY
    private var maxY = Float.NEGATIVE_INFINITY

    private fun track(
        x: Float,
        y: Float,
    ) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }

    override fun moveTo(
        x: Float,
        y: Float,
    ) {
        ops += Op.MoveTo(x, y)
        track(x, y)
    }

    override fun lineTo(
        x: Float,
        y: Float,
    ) {
        ops += Op.LineTo(x, y)
        track(x, y)
    }

    override fun close() {
        ops += Op.Close
    }

    override fun quadraticBezierTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        ops += Op.QuadTo(x1, y1, x2, y2)
        track(x1, y1)
        track(x2, y2)
    }

    override fun rQuadraticBezierTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
    ) {
        ops += Op.RQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        ops += Op.CubicTo(x1, y1, x2, y2, x3, y3)
        track(x1, y1)
        track(x2, y2)
        track(x3, y3)
    }

    override fun rCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    ) {
        ops += Op.RCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    override fun arcTo(
        rect: Rect,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        forceMoveTo: Boolean,
    ) {
        ops += Op.ArcTo(rect, startAngleDeg, sweepAngleDeg, forceMoveTo)
        track(rect.left, rect.top)
        track(rect.right, rect.bottom)
    }

    override fun addRect(rect: Rect) {
        if (rect.isEmpty) return
        ops += Op.AddRect(rect)
        track(rect.left, rect.top)
        track(rect.right, rect.bottom)
    }

    override fun addOval(oval: Rect) {
        if (oval.isEmpty) return
        ops += Op.AddOval(oval)
        track(oval.left, oval.top)
        track(oval.right, oval.bottom)
    }

    override fun addArc(
        oval: Rect,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
    ) {
        if (oval.isEmpty) return
        ops += Op.AddArc(oval, startAngleDeg, sweepAngleDeg)
        track(oval.left, oval.top)
        track(oval.right, oval.bottom)
    }

    override fun addRoundRect(roundRect: RoundRect) {
        ops += Op.AddRoundRect(roundRect)
        track(roundRect.left, roundRect.top)
        track(roundRect.right, roundRect.bottom)
    }

    override fun addPath(path: Path) {
        val impl = path as? SWTPath ?: return
        ops += Op.AddPath(impl)
        val b = impl.getBounds()
        track(b.left, b.top)
        track(b.right, b.bottom)
    }

    override fun reset() {
        ops.clear()
        resetBounds()
    }

    override fun rewind() {
        reset() // same in this impl
    }

    override fun getBounds(): Rect =
        if (ops.isEmpty()) {
            Rect.Zero
        } else {
            Rect(minX, minY, maxX, maxY)
        }

    override fun isEmpty(): Boolean = ops.isEmpty()

    override fun op(
        path: Path,
        op: PathOperation,
    ): Boolean {
        // Path.op has no native SWT equivalent; approximate via Region boolean ops.

        // Return false to indicate this is a best-effort approximation.
        val other = path as? SWTPath ?: return false
        if (other.isEmpty()) return false
        when (op) {
            PathOperation.Union -> {
                ops.addAll(other.ops)
                val b = other.getBounds()
                track(b.left, b.top)
                track(b.right, b.bottom)
                return true
            }
            else -> return false // Difference, Intersect, Xor, ReverseDifference unsupported
        }
    }

    private fun resetBounds() {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
    }
}

fun Path(): Path = SWTPath()
