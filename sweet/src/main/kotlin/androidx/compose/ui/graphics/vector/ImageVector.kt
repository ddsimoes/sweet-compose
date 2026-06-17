@file:Suppress("UnusedParameter")

package androidx.compose.ui.graphics.vector

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp

/**
 * A vector image defined by a tree of [Group]s and [PathNode]s, backed by SVG-like path data.
 *
 * Use [Builder] to construct instances declaratively.
 */
class ImageVector private constructor(
    val name: String,
    val defaultWidth: Dp,
    val defaultHeight: Dp,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val root: Group,
) {
    /**
     * Builder for constructing an [ImageVector] from groups and paths.
     */
    class Builder(
        private val name: String,
        private val defaultWidth: Dp,
        private val defaultHeight: Dp,
        private val viewportWidth: Float,
        private val viewportHeight: Float,
    ) {
        private val groups = mutableListOf<Group>()

        /**
         * Add a group with optional transform. The [block] receives the new group
         * so callers can add paths (and nested groups) inside it.
         */
        fun addGroup(
            name: String,
            rotate: Float = 0f,
            pivotX: Float = 0f,
            pivotY: Float = 0f,
            scaleX: Float = 1f,
            scaleY: Float = 1f,
            translationX: Float = 0f,
            translationY: Float = 0f,
            block: Group.() -> Unit,
        ): Group {
            val group = Group(name, rotate, pivotX, pivotY, scaleX, scaleY, translationX, translationY)
            group.block()
            groups.add(group)
            return group
        }

        /** Build the final [ImageVector] with groups wrapped in a root [Group]. */
        fun build(): ImageVector {
            val rootGroup = Group("", 0f, 0f, 0f, 1f, 1f, 0f, 0f)
            rootGroup.groups.addAll(groups)
            return ImageVector(name, defaultWidth, defaultHeight, viewportWidth, viewportHeight, rootGroup)
        }
    }

    /**
     * A named group of paths and child groups with an optional affine transform.
     */
    class Group(
        val name: String,
        val rotate: Float,
        val pivotX: Float,
        val pivotY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val translationX: Float,
        val translationY: Float,
    ) {
        internal val paths = mutableListOf<PathNode>()
        internal val groups = mutableListOf<Group>()

        /**
         * Add a path node to this group and return it.
         *
         * @param pathData SVG-like path data string (e.g. "M10 10 L20 20 Z").
         */
        fun path(
            pathData: String,
            fill: Color = Color.Black,
            fillAlpha: Float = 1f,
            stroke: Color = Color.Unspecified,
            strokeAlpha: Float = 1f,
            strokeLineWidth: Float = 1f,
            strokeLineCap: StrokeCap = StrokeCap.Butt,
            strokeLineJoin: StrokeJoin = StrokeJoin.Miter,
        ): PathNode {
            val node = PathNode(pathData, fill, fillAlpha, stroke, strokeAlpha, strokeLineWidth, strokeLineCap, strokeLineJoin)
            paths.add(node)
            return node
        }
    }

    /**
     * A single path within a [Group], holding SVG-like path data and rendering properties.
     */
    data class PathNode(
        val pathData: String,
        val fill: Color,
        val fillAlpha: Float,
        val stroke: Color,
        val strokeAlpha: Float,
        val strokeLineWidth: Float,
        val strokeLineCap: StrokeCap,
        val strokeLineJoin: StrokeJoin,
    )
}

/**
 * MPP-compatible pseudo-constructor accepting [Dp] for default dimensions.
 * The class constructor uses [Float] to avoid JVM signature clash;
 * this function bridges the [Dp] call-site convention used in MPP.
 */
@JvmName("Builder")
fun ImageVector.Builder(
    name: String,
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float,
    viewportHeight: Float,
): ImageVector.Builder =
    ImageVector.Builder(name, defaultWidth, defaultHeight, viewportWidth, viewportHeight)

/**
 * Add a path to this [ImageVector.Builder] using a [PathBuilder] DSL.
 *
 * The [pathBuilder] block populates a [PathBuilder] which is serialized
 * to SVG path data and wrapped in a default group.
 */
fun ImageVector.Builder.path(
    fill: Brush = SolidColor(Color.Black),
    fillAlpha: Float = 1.0f,
    stroke: Brush = SolidColor(Color.Transparent),
    strokeAlpha: Float = 1.0f,
    strokeLineWidth: Float = 1.0f,
    strokeLineCap: StrokeCap = StrokeCap.Butt,
    strokeLineJoin: StrokeJoin = StrokeJoin.Miter,
    strokeLineMiter: Float = 4.0f,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector.Builder {
    val pb = PathBuilder()
    pb.pathBuilder()
    val pathData = pb.toPathData()

    addGroup(
        name = "",
        rotate = 0f,
        pivotX = 0f,
        pivotY = 0f,
        scaleX = 1f,
        scaleY = 1f,
        translationX = 0f,
        translationY = 0f,
    ) {
        paths.add(
            ImageVector.PathNode(
                pathData = pathData,
                fill = if (fill is SolidColor) fill.value else Color.Black,
                fillAlpha = fillAlpha,
                stroke = if (stroke is SolidColor) stroke.value else Color.Transparent,
                strokeAlpha = strokeAlpha,
                strokeLineWidth = strokeLineWidth,
                strokeLineCap = strokeLineCap,
                strokeLineJoin = strokeLineJoin,
            ),
        )
    }
    return this
}
