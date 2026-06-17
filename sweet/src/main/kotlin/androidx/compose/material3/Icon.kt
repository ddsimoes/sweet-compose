@file:Suppress("ktlint:standard:function-naming", "UnusedParameter")

package androidx.compose.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * An icon component for Material Design icons.
 *
 * Delegates to [androidx.compose.foundation.Icon] for real vector rendering.
 *
 * @param imageVector The vector icon to display
 * @param contentDescription Accessibility description for the icon
 * @param modifier The modifier to be applied to the icon
 * @param tint The color to tint the icon
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    androidx.compose.foundation.Icon(imageVector, contentDescription, modifier, tint)
}

/**
 * A clickable icon button with Material Design touch target size (48dp).
 *
 * @param onClick Callback for when the button is clicked
 * @param modifier The modifier to be applied to the button
 * @param content The icon content, typically an Icon composable
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(48.dp) // Material Design touch target
                .clickable { onClick() }
                .padding(12.dp),
        // 12dp padding around 24dp icon = 48dp total
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
