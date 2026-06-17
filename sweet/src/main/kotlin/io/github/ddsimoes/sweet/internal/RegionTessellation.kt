package io.github.ddsimoes.sweet.internal

import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SWTPath
import org.eclipse.swt.graphics.Region
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tessellate a [RoundRect] into a polygon and add it to [region].
 *
 * Each corner arc is sampled at [segmentsPerCorner] intervals (default 12);
 * straight edges are single line segments.  The winding is clockwise.
 */
internal fun tessellateRoundRect(
    region: Region,
    rr: RoundRect,
    segmentsPerCorner: Int = 12,
) {
    val l = rr.left
    val t = rr.top
    val r = rr.right
    val b = rr.bottom
    val tl_r = rr.topLeftCornerRadius.coerceAtLeast(0f)
    val tr_r = rr.topRightCornerRadius.coerceAtLeast(0f)
    val br_r = rr.bottomRightCornerRadius.coerceAtLeast(0f)
    val bl_r = rr.bottomLeftCornerRadius.coerceAtLeast(0f)

    val polygon = mutableListOf<Int>()

    fun emit(
        x: Float,
        y: Float,
    ) {
        polygon += x.toInt()
        polygon += y.toInt()
    }

    fun sampleArc(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        startDeg: Float,
        sweepDeg: Float,
    ) {
        for (i in 0 until segmentsPerCorner) {
            val deg = startDeg + sweepDeg * i / segmentsPerCorner
            val rad = deg * PI.toFloat() / 180f
            val px = cx + rx * cos(rad)
            val py = cy - ry * sin(rad)
            emit(px, py)
        }
    }

    // Start at the left end of the top edge.
    emit(l + tl_r, t)

    // Top edge.
    emit(r - tr_r, t)

    // Top-right corner: center (r - tr_r, t + tr_r); start 90° → 0° (CCW, sweep -90°)
    if (tr_r > 0f) {
        sampleArc(r - tr_r, t + tr_r, tr_r, tr_r, 90f, -90f)
    } else {
        emit(r, t)
    }

    // Right edge.
    emit(r, b - br_r)

    // Bottom-right corner: center (r - br_r, b - br_r); start 0° → 270° (CCW, sweep -90°)
    if (br_r > 0f) {
        sampleArc(r - br_r, b - br_r, br_r, br_r, 0f, -90f)
    } else {
        emit(r, b)
    }

    // Bottom edge.
    emit(l + bl_r, b)

    // Bottom-left corner: center (l + bl_r, b - bl_r); start 270° → 180° (CCW, sweep -90°)
    if (bl_r > 0f) {
        sampleArc(l + bl_r, b - bl_r, bl_r, bl_r, 270f, -90f)
    } else {
        emit(l, b)
    }

    // Left edge.
    emit(l, t + tl_r)

    // Top-left corner: center (l + tl_r, t + tl_r); start 180° → 90° (CCW, sweep -90°)
    if (tl_r > 0f) {
        sampleArc(l + tl_r, t + tl_r, tl_r, tl_r, 180f, -90f)
    } else {
        emit(l, t)
    }

    region.add(polygon.toIntArray())
}

/**
 * Tessellate a generic [Path] into line segments and add to [region].
 *
 * Curve ops are approximated by linear subdivision; each Bézier is sampled
 * at [curveSegments] intervals.
 */
internal fun tessellatePath(
    region: Region,
    path: Path,
    curveSegments: Int = 12,
) {
    val swtPath =
        path as? SWTPath ?: run {
            // Fall back to bounding rectangle for non-SWTPath implementations.
            val bounds = path.getBounds()
            region.add(bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            return
        }
    if (swtPath.isEmpty()) return

    val polygon = mutableListOf<Int>()
    var cx = 0f
    var cy = 0f
    var started = false

    fun emit(
        x: Float,
        y: Float,
    ) {
        polygon += x.toInt()
        polygon += y.toInt()
    }

    fun sampleQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        for (i in 1..curveSegments) {
            val t = i.toFloat() / curveSegments
            val u = 1f - t
            val px = u * u * x0 + 2f * u * t * x1 + t * t * x2
            val py = u * u * y0 + 2f * u * t * y1 + t * t * y2
            emit(px, py)
        }
    }

    fun sampleCubic(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        for (i in 1..curveSegments) {
            val t = i.toFloat() / curveSegments
            val u = 1f - t
            val uu = u * u
            val uuu = uu * u
            val tt = t * t
            val ttt = tt * t
            val px = uuu * x0 + 3 * uu * t * x1 + 3 * u * tt * x2 + ttt * x3
            val py = uuu * y0 + 3 * uu * t * y1 + 3 * u * tt * y2 + ttt * y3
            emit(px, py)
        }
    }

    for (op in swtPath.ops) {
        when (op) {
            is SWTPath.Op.MoveTo -> {
                if (started && polygon.size >= 4) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                cx = op.x
                cy = op.y
                emit(cx, cy)
                started = true
            }
            is SWTPath.Op.LineTo -> {
                cx = op.x
                cy = op.y
                emit(cx, cy)
            }
            is SWTPath.Op.QuadTo -> {
                sampleQuad(cx, cy, op.x1, op.y1, op.x2, op.y2)
                cx = op.x2
                cy = op.y2
            }
            is SWTPath.Op.RQuadTo -> {
                val nx = cx + op.dx2
                val ny = cy + op.dy2
                sampleQuad(cx, cy, cx + op.dx1, cy + op.dy1, nx, ny)
                cx = nx
                cy = ny
            }
            is SWTPath.Op.CubicTo -> {
                sampleCubic(cx, cy, op.x1, op.y1, op.x2, op.y2, op.x3, op.y3)
                cx = op.x3
                cy = op.y3
            }
            is SWTPath.Op.RCubicTo -> {
                val nx = cx + op.dx3
                val ny = cy + op.dy3
                sampleCubic(cx, cy, cx + op.dx1, cy + op.dy1, cx + op.dx2, cy + op.dy2, nx, ny)
                cx = nx
                cy = ny
            }
            is SWTPath.Op.Close -> {
                if (polygon.size >= 4) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                started = false
            }
            is SWTPath.Op.ArcTo -> {
                // Approximate arc with a quadratic/cubic for simplicity.
                // For clipping purposes this is fine — exact arc fidelity
                // is handled by the rendering path.
                sampleArcTo(polygon, op, curveSegments)
                cx = op.rect.left + op.rect.width * 0.5f
                cy = op.rect.top + op.rect.height * 0.5f
            }
            is SWTPath.Op.AddRect -> {
                if (polygon.isNotEmpty()) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                val r = op.rect
                region.add(r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
                started = false
            }
            is SWTPath.Op.AddOval -> {
                if (polygon.isNotEmpty()) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                val o = op.oval
                val cx2 = o.left + o.width / 2f
                val cy2 = o.top + o.height / 2f
                val rx = o.width / 2f
                val ry = o.height / 2f
                val ovalPoly = mutableListOf<Int>()
                for (i in 0 until curveSegments * 4) {
                    val deg = i * 360f / (curveSegments * 4)
                    val rad = deg * PI.toFloat() / 180f
                    ovalPoly += (cx2 + rx * cos(rad)).toInt()
                    ovalPoly += (cy2 - ry * sin(rad)).toInt()
                }
                region.add(ovalPoly.toIntArray())
                started = false
            }
            is SWTPath.Op.AddArc -> {
                if (polygon.isNotEmpty()) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                val a = op.oval
                val cx2 = a.left + a.width / 2f
                val cy2 = a.top + a.height / 2f
                val rx = a.width / 2f
                val ry = a.height / 2f
                val arcPoly = mutableListOf<Int>()
                val segs = curveSegments * 4
                for (i in 0..segs) {
                    val deg = op.startAngleDeg + op.sweepAngleDeg * i / segs
                    val rad = deg * PI.toFloat() / 180f
                    arcPoly += (cx2 + rx * cos(rad)).toInt()
                    arcPoly += (cy2 - ry * sin(rad)).toInt()
                }
                region.add(arcPoly.toIntArray())
                started = false
            }
            is SWTPath.Op.AddRoundRect -> {
                if (polygon.isNotEmpty()) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                tessellateRoundRect(region, op.roundRect, curveSegments)
                started = false
            }
            is SWTPath.Op.AddPath -> {
                if (polygon.isNotEmpty()) {
                    region.add(polygon.toIntArray())
                    polygon.clear()
                }
                tessellatePath(region, op.path, curveSegments)
                started = false
            }
        }
    }

    // Flush remaining polygon.
    if (polygon.size >= 4) {
        region.add(polygon.toIntArray())
    }
}

/** Approximate an [SWTPath.Op.ArcTo] as line segments for clip-region tessellation. */
internal fun sampleArcTo(
    polygon: MutableList<Int>,
    op: SWTPath.Op.ArcTo,
    segments: Int,
) {
    val cx = op.rect.left + op.rect.width / 2f
    val cy = op.rect.top + op.rect.height / 2f
    val rx = op.rect.width / 2f
    val ry = op.rect.height / 2f
    for (i in 0..segments) {
        val deg = op.startAngleDeg + op.sweepAngleDeg * i / segments
        val rad = deg * PI.toFloat() / 180f
        polygon += (cx + rx * cos(rad)).toInt()
        polygon += (cy - ry * sin(rad)).toInt()
    }
}
