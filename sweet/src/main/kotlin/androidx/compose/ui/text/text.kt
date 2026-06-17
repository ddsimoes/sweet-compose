@file:Suppress("MatchingDeclarationName")

package androidx.compose.ui.text

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

/**
 * Standard TextStyle class
 */
@Stable
data class TextStyle(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontWeight: Any? = null,
    val fontStyle: Any? = null,
    val fontFamily: Any? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val textDecoration: Any? = null,
    val textAlign: Any? = null,
    val lineHeight: TextUnit = TextUnit.Unspecified,
) {
    companion object {
        val Default = TextStyle()
    }
}
