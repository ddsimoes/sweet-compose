@file:Suppress("UnusedParameter")

package androidx.compose.foundation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon component that renders an [ImageVector] on a [Canvas].
 *
 * Traverses the vector's group tree, applying affine transforms,
 * and draws each path using the parsed SVG-like [PathNode.pathData].
 *
 * @param imageVector The vector icon to display.
 * @param contentDescription Accessibility description for the icon.
 * @param modifier The modifier to be applied to the icon.
 * @param tint Tint color applied to filled paths. [Color.Unspecified] leaves
 *   the original fill color intact; any other color overrides all fill colors.
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Canvas(
        modifier = modifier.size(imageVector.defaultWidth, imageVector.defaultHeight),
    ) {
        val scaleX = size.width / imageVector.viewportWidth
        val scaleY = size.height / imageVector.viewportHeight
        drawGroup(imageVector.root, scaleX, scaleY, tint)
    }
}

/**
 * Icon component that renders a [Painter] inside an [Image].
 *
 * @param painter The painter to draw.
 * @param contentDescription Accessibility description for the icon.
 * @param modifier The modifier to be applied to the icon.
 * @param tint Currently unused for painter-based icons.
 */
@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        alpha = 1f,
    )
}

// ── Drawing helpers ─────────────────────────────────────────────

/** Recursively draw a [ImageVector.Group] and its children. */
private fun DrawScope.drawGroup(
    group: ImageVector.Group,
    scaleX: Float,
    scaleY: Float,
    tint: Color,
) {
    withTransform {
        // Apply group transform
        if (group.translationX != 0f || group.translationY != 0f) {
            translate(group.translationX * scaleX, group.translationY * scaleY)
        }
        if (group.scaleX != 1f || group.scaleY != 1f) {
            scale(group.scaleX, group.scaleY)
        }
        if (group.rotate != 0f) {
            // Pivot rotation: translate to pivot, rotate, translate back
            val px = group.pivotX * scaleX
            val py = group.pivotY * scaleY
            translate(px, py)
            rotate(group.rotate)
            translate(-px, -py)
        }

        // Draw paths in this group
        for (pathNode in group.paths) {
            drawPathNode(pathNode, scaleX, scaleY, tint)
        }

        // Draw child groups
        for (childGroup in group.groups) {
            drawGroup(childGroup, scaleX, scaleY, tint)
        }
    }
}

/** Draw a single [ImageVector.PathNode]. */
private fun DrawScope.drawPathNode(
    node: ImageVector.PathNode,
    scaleX: Float,
    scaleY: Float,
    tint: Color,
) {
    val path = parseSvgPathData(node.pathData, scaleX, scaleY)

    // Determine fill and stroke: tint overrides when specified
    val fillColor = if (tint.isSpecified && node.fill.isSpecified) tint else node.fill
    val strokeColor = if (tint.isSpecified && node.stroke.isSpecified) tint else node.stroke

    if (fillColor.isSpecified) {
        drawPath(
            path = path,
            color = fillColor,
            alpha = node.fillAlpha,
            style = Fill,
        )
    }

    if (node.stroke.isSpecified) {
        // Scale stroke width by viewport-to-canvas ratio so
        // size variants (16/24/32/40dp) have proportional thickness.
        val scaledStrokeWidth = node.strokeLineWidth * minOf(scaleX, scaleY)
        drawPath(
            path = path,
            color = strokeColor,
            alpha = node.strokeAlpha,
            style =
                Stroke(
                    width = scaledStrokeWidth.coerceAtLeast(0.5f),
                    cap = node.strokeLineCap,
                    join = node.strokeLineJoin,
                ),
        )
    }
}

// ── SVG path data parser ────────────────────────────────────────

/**
 * Parse an SVG-like path data string into a [Path] object, applying
 * the given scale factors to all coordinates.
 *
 * Supports: M/m, L/l, H/h, V/v, C/c, S/s, Q/q, T/t, A/a, Z/z.
 * Relative commands are converted to absolute using the current point.
 */
private fun parseSvgPathData(
    data: String,
    scaleX: Float,
    scaleY: Float,
): Path {
    val path = Path()
    var cx = 0f // current x
    var cy = 0f // current y
    var i = 0

    fun skipWhitespace() {
        while (i < data.length && (data[i] == ' ' || data[i] == ',' || data[i] == '\t' || data[i] == '\n' || data[i] == '\r')) i++
    }

    fun readFloat(): Float? {
        skipWhitespace()
        if (i >= data.length) return null
        // Check for sign prefix as part of the number
        var j = i
        if (j < data.length && (data[j] == '+' || data[j] == '-')) j++
        if (j >= data.length || !data[j].isDigit() && data[j] != '.') return null
        while (j < data.length && (data[j].isDigit() || data[j] == '.' || data[j] == 'e' || data[j] == 'E' || data[j] == '+' || data[j] == '-')) {
            // sign inside exponent
            if ((data[j] == '+' || data[j] == '-') && j > i && data[j - 1] != 'e' && data[j - 1] != 'E') break
            j++
        }
        val num = data.substring(i, j).toFloat()
        i = j
        return num
    }

    fun readFloats(count: Int): List<Float>? {
        val result = mutableListOf<Float>()
        repeat(count) {
            val f = readFloat() ?: return null
            result.add(f)
        }
        return result
    }

    fun readOptionalFloat(): Float = readFloat() ?: 0f

    fun handleArc(
        rx: Float,
        ry: Float,
        xAxisRotation: Float,
        largeArcFlag: Float,
        sweepFlag: Float,
        x: Float,
        y: Float,
        relative: Boolean,
    ) {
        val absX = if (relative) cx + x else x
        val absY = if (relative) cy + y else y
        val sr = rx * scaleX
        val sry = ry * scaleY
        if (sr <= 0f || sry <= 0f) {
            // Degenerate arc → line
            path.lineTo(absX * scaleX, absY * scaleY)
        } else {
            val oval =
                Rect(
                    (cx - sr) * scaleX,
                    (cy - sry) * scaleY,
                    (cx + sr) * scaleX,
                    (cy + sry) * scaleY,
                )
            path.arcTo(oval, xAxisRotation, if (largeArcFlag != 0f) 180f else 90f, forceMoveTo = false)
            // Approximation: just lineTo final point
            path.lineTo(absX * scaleX, absY * scaleY)
        }
        cx = absX
        cy = absY
    }

    while (i < data.length) {
        skipWhitespace()
        if (i >= data.length) break

        val cmd = data[i]
        i++ // consume command char

        when (cmd) {
            'M' -> {
                val coords = readFloats(2) ?: break
                val x = coords[0]
                val y = coords[1]
                cx = x
                cy = y
                path.moveTo(x * scaleX, y * scaleY)
                // Implicit L for subsequent coordinate pairs
                while (true) {
                    val next = readFloats(2) ?: break
                    cx = next[0]
                    cy = next[1]
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'm' -> {
                val coords = readFloats(2) ?: break
                cx += coords[0]
                cy += coords[1]
                path.moveTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloats(2) ?: break
                    cx += next[0]
                    cy += next[1]
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'L' -> {
                val coords = readFloats(2) ?: break
                cx = coords[0]
                cy = coords[1]
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloats(2) ?: break
                    cx = next[0]
                    cy = next[1]
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'l' -> {
                val coords = readFloats(2) ?: break
                cx += coords[0]
                cy += coords[1]
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloats(2) ?: break
                    cx += next[0]
                    cy += next[1]
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'H' -> {
                cx = readOptionalFloat()
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloat() ?: break
                    cx = next
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'h' -> {
                cx += readOptionalFloat()
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloat() ?: break
                    cx += next
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'V' -> {
                cy = readOptionalFloat()
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloat() ?: break
                    cy = next
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'v' -> {
                cy += readOptionalFloat()
                path.lineTo(cx * scaleX, cy * scaleY)
                while (true) {
                    val next = readFloat() ?: break
                    cy += next
                    path.lineTo(cx * scaleX, cy * scaleY)
                }
            }
            'C' -> {
                val coords = readFloats(6) ?: break
                val x1 = coords[0]
                val y1 = coords[1]
                val x2 = coords[2]
                val y2 = coords[3]
                val x = coords[4]
                val y = coords[5]
                path.cubicTo(
                    x1 * scaleX,
                    y1 * scaleY,
                    x2 * scaleX,
                    y2 * scaleY,
                    x * scaleX,
                    y * scaleY,
                )
                cx = x
                cy = y
                while (true) {
                    val next = readFloats(6) ?: break
                    path.cubicTo(
                        next[0] * scaleX,
                        next[1] * scaleY,
                        next[2] * scaleX,
                        next[3] * scaleY,
                        next[4] * scaleX,
                        next[5] * scaleY,
                    )
                    cx = next[4]
                    cy = next[5]
                }
            }
            'c' -> {
                val coords = readFloats(6) ?: break
                path.rCubicTo(
                    coords[0] * scaleX,
                    coords[1] * scaleY,
                    coords[2] * scaleX,
                    coords[3] * scaleY,
                    coords[4] * scaleX,
                    coords[5] * scaleY,
                )
                cx += coords[4]
                cy += coords[5]
                while (true) {
                    val next = readFloats(6) ?: break
                    path.rCubicTo(
                        next[0] * scaleX,
                        next[1] * scaleY,
                        next[2] * scaleX,
                        next[3] * scaleY,
                        next[4] * scaleX,
                        next[5] * scaleY,
                    )
                    cx += next[4]
                    cy += next[5]
                }
            }
            'S', 's' -> {
                val coords = readFloats(4) ?: break
                if (cmd == 'S') {
                    path.cubicTo(
                        cx * scaleX,
                        cy * scaleY,
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                        coords[2] * scaleX,
                        coords[3] * scaleY,
                    )
                    cx = coords[2]
                    cy = coords[3]
                } else {
                    path.rCubicTo(
                        0f,
                        0f,
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                        coords[2] * scaleX,
                        coords[3] * scaleY,
                    )
                    cx += coords[2]
                    cy += coords[3]
                }
            }
            'Q', 'q' -> {
                val coords = readFloats(4) ?: break
                if (cmd == 'Q') {
                    path.quadraticBezierTo(
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                        coords[2] * scaleX,
                        coords[3] * scaleY,
                    )
                    cx = coords[2]
                    cy = coords[3]
                } else {
                    path.rQuadraticBezierTo(
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                        coords[2] * scaleX,
                        coords[3] * scaleY,
                    )
                    cx += coords[2]
                    cy += coords[3]
                }
                while (true) {
                    val next = readFloats(4) ?: break
                    if (cmd == 'Q') {
                        path.quadraticBezierTo(
                            next[0] * scaleX,
                            next[1] * scaleY,
                            next[2] * scaleX,
                            next[3] * scaleY,
                        )
                        cx = next[2]
                        cy = next[3]
                    } else {
                        path.rQuadraticBezierTo(
                            next[0] * scaleX,
                            next[1] * scaleY,
                            next[2] * scaleX,
                            next[3] * scaleY,
                        )
                        cx += next[2]
                        cy += next[3]
                    }
                }
            }
            'T', 't' -> {
                val coords = readFloats(2) ?: break
                if (cmd == 'T') {
                    path.quadraticBezierTo(
                        cx * scaleX,
                        cy * scaleY,
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                    )
                    cx = coords[0]
                    cy = coords[1]
                } else {
                    path.rQuadraticBezierTo(
                        0f,
                        0f,
                        coords[0] * scaleX,
                        coords[1] * scaleY,
                    )
                    cx += coords[0]
                    cy += coords[1]
                }
            }
            'A' -> {
                val coords = readFloats(7) ?: break
                handleArc(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], coords[6], relative = false)
            }
            'a' -> {
                val coords = readFloats(7) ?: break
                handleArc(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], coords[6], relative = true)
            }
            'Z', 'z' -> {
                path.close()
            }
            else -> {
                // Unknown command — skip it
            }
        }
    }

    return path
}
