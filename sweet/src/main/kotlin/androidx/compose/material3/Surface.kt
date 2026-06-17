@file:Suppress("ktlint:standard:filename", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ddsimoes.sweet.debug.SweetDebugger

/**
 * Material surface is the central metaphor in material design. Each surface exists at a given
 * elevation, which influences how that piece of surface visually relates to other surfaces.
 *
 * **Sweet note:** Currently only applies background color via [Modifier.background] and,
 * like the MPP implementation, propagates incoming min constraints to its content
 * (`propagateMinConstraints = true`), so content fills a sized Surface. Shape, elevation,
 * shadow, border, and content color propagation are accepted for API compatibility but
 * are not yet applied to rendering.
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape (currently ignored)
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color (currently ignored)
 * @param tonalElevation Tonal elevation (currently ignored)
 * @param shadowElevation Shadow elevation (currently ignored)
 * @param border Optional border (currently ignored)
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    if (SweetDebugger.assertionEnabled) {
        if (shape != RectangleShape) {
            SweetDebugger.log(
                "Surface",
                "Shape parameter is currently ignored; only background color is applied.",
            )
        }
        if (tonalElevation != 0.dp || shadowElevation != 0.dp) {
            SweetDebugger.log(
                "Surface",
                "Elevation parameters are currently ignored.",
            )
        }
        if (border != null) {
            SweetDebugger.log(
                "Surface",
                "Border parameter is currently ignored.",
            )
        }
        if (contentColor != Color.Unspecified) {
            SweetDebugger.log(
                "Surface",
                "contentColor is accepted but content color propagation is not yet implemented.",
            )
        }
    }
    Box(
        modifier = modifier.background(color),
        propagateMinConstraints = true,
    ) {
        content()
    }
}
