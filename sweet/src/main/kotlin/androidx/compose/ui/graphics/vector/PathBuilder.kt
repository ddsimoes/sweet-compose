package androidx.compose.ui.graphics.vector

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * DSL builder for constructing path data incrementally.
 *
 * Each method appends the corresponding SVG path command to the internal
 * data string and returns `this` for chaining.
 */
class PathBuilder {
    private val sb = StringBuilder()

    fun moveTo(
        x: Float,
        y: Float,
    ): PathBuilder {
        sb.append("M$x,$y ")
        return this
    }

    fun lineTo(
        x: Float,
        y: Float,
    ): PathBuilder {
        sb.append("L$x,$y ")
        return this
    }

    fun horizontalLineTo(x: Float): PathBuilder {
        sb.append("H$x ")
        return this
    }

    fun verticalLineTo(y: Float): PathBuilder {
        sb.append("V$y ")
        return this
    }

    fun close(): PathBuilder {
        sb.append("Z ")
        return this
    }

    fun relativeMoveTo(
        dx: Float,
        dy: Float,
    ): PathBuilder {
        sb.append("m$dx,$dy ")
        return this
    }

    fun relativeLineTo(
        dx: Float,
        dy: Float,
    ): PathBuilder {
        sb.append("l$dx,$dy ")
        return this
    }

    fun relativeHorizontalLineTo(dx: Float): PathBuilder {
        sb.append("h$dx ")
        return this
    }

    fun relativeVerticalLineTo(dy: Float): PathBuilder {
        sb.append("v$dy ")
        return this
    }

    fun relativeQuadraticBezierTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
    ): PathBuilder {
        sb.append("q$dx1,$dy1,$dx2,$dy2 ")
        return this
    }

    fun quadraticBezierTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): PathBuilder {
        sb.append("Q$x1,$y1,$x2,$y2 ")
        return this
    }

    fun cubicTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ): PathBuilder {
        sb.append("C$x1,$y1,$x2,$y2,$x3,$y3 ")
        return this
    }

    fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    ): PathBuilder {
        sb.append("c$dx1,$dy1,$dx2,$dy2,$dx3,$dy3 ")
        return this
    }

    fun arcTo(
        rect: androidx.compose.ui.geometry.Rect,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        forceMoveTo: Boolean,
    ): PathBuilder {
        // Approximate the elliptical arc with cubic beziers,
        // splitting into ≤ 90° segments for accuracy.
        val cx = rect.center.x
        val cy = rect.center.y
        val rx = rect.width / 2f
        val ry = rect.height / 2f

        val sweepRad = Math.toRadians(sweepAngleDeg.toDouble()).toFloat()
        if (abs(sweepRad) < 1e-6f) return this

        val startRad = Math.toRadians(startAngleDeg.toDouble()).toFloat()
        val maxSegmentRad = (Math.PI / 2.0).toFloat() // 90°
        val numSegments =
            maxOf(1, ceil(abs(sweepRad) / maxSegmentRad).toInt())
        val segmentRad = sweepRad / numSegments

        // k = (4/3) * tan(θ/4) — the standard cubic-bezier
        // magic constant for a circular arc segment of angle θ.
        val k = (4.0f / 3.0f) * tan(segmentRad / 4.0f)

        for (i in 0 until numSegments) {
            val a1 = startRad + i * segmentRad
            val a2 = a1 + segmentRad

            val cos1 = cos(a1)
            val sin1 = sin(a1)
            val cos2 = cos(a2)
            val sin2 = sin(a2)

            val x1 = cx + rx * cos1
            val y1 = cy + ry * sin1
            val x2 = cx + rx * cos2
            val y2 = cy + ry * sin2

            if (i == 0 && forceMoveTo) sb.append("M$x1,$y1 ")

            val cpx1 = x1 + k * (-rx * sin1)
            val cpy1 = y1 + k * (ry * cos1)
            val cpx2 = x2 - k * (-rx * sin2)
            val cpy2 = y2 - k * (ry * cos2)

            sb.append("C$cpx1,$cpy1,$cpx2,$cpy2,$x2,$y2 ")
        }
        return this
    }

    /** Serialize the path data to an SVG-compatible string. */
    internal fun toPathData(): String = sb.toString().trim()
}
