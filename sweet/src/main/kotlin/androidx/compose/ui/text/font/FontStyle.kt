package androidx.compose.ui.text.font

import androidx.compose.runtime.Immutable

/**
 * Defines the font style (normal or italic).
 */
@Immutable
enum class FontStyle {
    /**
     * Use the upright glyphs
     */
    Normal,

    /**
     * Use glyphs designed for slanting
     */
    Italic,
}
